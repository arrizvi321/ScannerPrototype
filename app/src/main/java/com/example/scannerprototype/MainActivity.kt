package com.example.scannerprototype

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.InputDevice
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.scannerprototype.data.CheckInApi
import com.example.scannerprototype.data.CheckInUiState
import com.example.scannerprototype.ui.ScannerTestScreen
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Main Activity handling window-level hardware barcode scanner input interception,
 * triggering check-in requests upon code scan with a 5-second cooldown delay,
 * and reading out check-in messages using Text-To-Speech (TTS).
 */
class MainActivity : ComponentActivity() {

    // States passed down to Compose UI
    val scannedCodeState = mutableStateOf("")
    val manualInputState = mutableStateOf("")
    val checkInUiState = mutableStateOf<CheckInUiState>(CheckInUiState.Idle)

    private val scanBuffer = StringBuilder()
    private var lastKeyTime = 0L
    private var lastScanProcessTime = 0L
    private val SCANNER_THRESHOLD_MS = 50L // Keypress interval threshold (scanners fire <30ms)
    private val SCAN_COOLDOWN_MS = 5000L // 5 second delay between scans

    // Text To Speech Engine
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Android Text To Speech Engine
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech?.setLanguage(Locale.US)
                if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    isTtsInitialized = true
                }
            }
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ScannerTestScreen(
                        manualInputText = manualInputState.value,
                        onManualInputChanged = { manualInputState.value = it },
                        checkInState = checkInUiState.value
                    )
                }
            }
        }
    }

    private fun speakText(text: String) {
        if (isTtsInitialized && text.isNotBlank()) {
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "CHECK_IN_TTS_ID")
        }
    }

    private fun performCheckIn(tokenValue: String) {
        checkInUiState.value = CheckInUiState.Loading
        lifecycleScope.launch {
            val result = CheckInApi.checkInWithPass(tokenValue)
            result.onSuccess { response ->
                checkInUiState.value = CheckInUiState.Success(response)
                val messageToSpeak = response.message.takeIf { !it.isNullOrBlank() }
                    ?: if (response.checkedOutAt.isNullOrBlank()) "Check-in Successful" else "Check-out Successful"
                speakText(messageToSpeak)
            }.onFailure { error ->
                val errorMessage = error.message ?: "Check-in request failed"
                checkInUiState.value = CheckInUiState.Error(errorMessage)
                speakText(errorMessage)
            }
        }
    }

    override fun onDestroy() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        super.onDestroy()
    }

    /**
     * Intercepts OS-level key events at the Window root before they are routed
     * to Jetpack Compose UI elements or active TextFields.
     */
    @Suppress("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            val currentTime = System.currentTimeMillis()
            val timeDiff = currentTime - lastKeyTime
            lastKeyTime = currentTime

            /*
             * HARDWARE DEVICE IDENTIFICATION:
             * Identifies if the event originates from a physical USB HID device (scanner/keyboard)
             * rather than a virtual software IME keyboard.
             */
            val isPhysicalHardwareKey = event.device != null &&
                    !event.device.isVirtual &&
                    event.device.keyboardType != InputDevice.KEYBOARD_TYPE_NONE

            /*
             * 1. SCAN COMPLETION / TERMINATOR HANDLING (ENTER KEY)
             * Scanners emit an ENTER key (\n / \r) at the end of a scan payload.
             */
            if (event.keyCode == KeyEvent.KEYCODE_ENTER || event.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                if (scanBuffer.isNotEmpty()) {
                    val fullCode = scanBuffer.toString().trim()
                    scanBuffer.setLength(0) // Reset buffer for next scan

                    // 5-SECOND COOLDOWN: Ignore scans that occur within 5 seconds of the last processed scan
                    if (currentTime - lastScanProcessTime < SCAN_COOLDOWN_MS) {
                        return true // CONSUME EVENT: Keeps screen text unchanged and prevents duplicate requests
                    }

                    lastScanProcessTime = currentTime
                    scannedCodeState.value = fullCode

                    /*
                     * RETROACTIVE SANITIZATION (LEAK CLEANUP):
                     * Before the high-speed timing threshold locks in, the first 1-2 digits of a scan
                     * may pass to super.dispatchKeyEvent() before timeDiff registers as fast.
                     * Here, we check if the active text field ends with those initial digits and strip them.
                     */
                    val currentText = manualInputState.value
                    if (currentText.isNotEmpty()) {
                        for (length in minOf(currentText.length, 3) downTo 1) {
                            val leakedPrefix = fullCode.take(length)
                            if (currentText.endsWith(leakedPrefix)) {
                                manualInputState.value = currentText.dropLast(length)
                                break
                            }
                        }
                    }

                    // Trigger API check-in request with scanned code token
                    performCheckIn(fullCode)

                    return true // CONSUME EVENT: Stops ENTER from submitting forms or adding line breaks
                }
            } else {
                /*
                 * 2. HARDWARE CHARACTER INTERCEPTION & BUFFERING
                 */
                val unicodeChar = event.getUnicodeChar(event.metaState)
                if (unicodeChar != 0) {
                    val char = unicodeChar.toChar()

                    /*
                     * SCAN STREAM FILTERING:
                     * If keystrokes arrive in rapid bursts (<50ms interval), if a scan buffer is already active,
                     * or if the device is a physical USB HID input:
                     * - Append character to scanBuffer.
                     * - Return 'true' to CONSUME the event so active TextFields never receive it.
                     */
                    if (timeDiff < SCANNER_THRESHOLD_MS || scanBuffer.isNotEmpty() || isPhysicalHardwareKey) {
                        if (char != '\u0000' && !char.isISOControl()) {
                            scanBuffer.append(char)
                            return true // CONSUME EVENT: Blocks typing into focused text fields
                        }
                    } else {
                        /*
                         * MANUAL HUMAN TYPING:
                         * Keystrokes arriving at standard typing speeds (>50ms interval) clear the buffer
                         * and fall through to super.dispatchKeyEvent(event) to allow regular input.
                         */
                        scanBuffer.setLength(0)
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }
}
