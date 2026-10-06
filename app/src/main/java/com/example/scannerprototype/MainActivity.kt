package com.example.scannerprototype

import android.media.AudioManager
import android.media.ToneGenerator
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
 * manual sign-in and sign-out form submissions, triggering check-in requests,
 * enforcing cooldown delays, playing a scanner beep sound upon valid scan requests,
 * displaying live cooldown warning sub-banners without replacing active dialogs,
 * and reading out messages via Text-To-Speech (TTS).
 */
class MainActivity : ComponentActivity() {

    // States passed down to Compose UI
    val scannedCodeState = mutableStateOf("")
    val manualInputState = mutableStateOf("")
    val checkInUiState = mutableStateOf<CheckInUiState>(CheckInUiState.Idle)

    private val scanBuffer = StringBuilder()
    private var lastKeyTime = 0L

    private val SCANNER_THRESHOLD_MS = 50L // Keypress interval threshold (scanners fire <30ms)
    private val API_COOLDOWN_MS = 10000L   // 10 second minimum delay between API calls
    private var lastApiCallTime = 0L

    // Text To Speech & Audio Beep Engines
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false
    private var toneGenerator: ToneGenerator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Android ToneGenerator for scanner feedback sound
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        } catch (_: Exception) {}

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
                        scannedCodeText = scannedCodeState.value,
                        onScannedCodeChanged = { scannedCodeState.value = it },
                        checkInState = checkInUiState.value,
                        onDismissDialog = {
                            checkInUiState.value = CheckInUiState.Idle
                            scanBuffer.setLength(0)
                        },
                        onManualCheckInSubmit = { firstName, lastName, phone, purpose, hasSymptoms ->
                            performManualCheckIn(firstName, lastName, phone, purpose, hasSymptoms)
                        },
                        onManualSignOutSubmit = { phone ->
                            performManualSignOut(phone)
                        }
                    )
                }
            }
        }
    }

    private fun playBeepSound() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        } catch (_: Exception) {}
    }

    private fun speakText(text: String) {
        if (isTtsInitialized && text.isNotBlank()) {
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "CHECK_IN_TTS_ID")
        }
    }

    private fun getApiCooldownRemainingSeconds(): Long {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastCall = currentTime - lastApiCallTime
        return if (timeSinceLastCall < API_COOLDOWN_MS) {
            val remainingMs = API_COOLDOWN_MS - timeSinceLastCall
            (remainingMs + 999) / 1000
        } else {
            0L
        }
    }

    private fun performCheckIn(tokenValue: String) {
        if (checkInUiState.value !is CheckInUiState.Idle) {
            return
        }

        val remainingSeconds = getApiCooldownRemainingSeconds()
        if (remainingSeconds > 0) {
            speakText("Please wait $remainingSeconds seconds before trying again.")
            return
        }

        playBeepSound()
        lastApiCallTime = System.currentTimeMillis()
        checkInUiState.value = CheckInUiState.Loading
        lifecycleScope.launch {
            val result = CheckInApi.checkInWithPass(tokenValue)
            result.onSuccess { response ->
                val rawMsg = response.message
                val isInvalidPassMsg = rawMsg != null && (rawMsg.contains("invalid", ignoreCase = true) || rawMsg.contains("pass", ignoreCase = true))
                if (!response.ok || isInvalidPassMsg) {
                    val displayMessage = "Please try again."
                    checkInUiState.value = CheckInUiState.Error(displayMessage)
                    speakText(displayMessage)
                } else {
                    checkInUiState.value = CheckInUiState.Success(response)
                    val messageToSpeak = rawMsg.takeIf { !it.isNullOrBlank() }
                        ?: if (response.checkedOutAt.isNullOrBlank()) "Check-in Successful" else "Check-out Successful"
                    speakText(messageToSpeak)
                }
            }.onFailure { error ->
                val rawMsg = error.message ?: "Check-in request failed"
                val displayMessage = if (rawMsg.contains("invalid", ignoreCase = true) || rawMsg.contains("pass", ignoreCase = true)) {
                    "Please try again."
                } else {
                    rawMsg
                }
                checkInUiState.value = CheckInUiState.Error(displayMessage)
                speakText(displayMessage)
            }
        }
    }

    private fun performManualCheckIn(
        firstName: String,
        lastName: String,
        phone: String,
        purpose: String,
        hasSymptoms: Boolean
    ) {
        if (checkInUiState.value !is CheckInUiState.Idle) {
            return
        }

        val remainingSeconds = getApiCooldownRemainingSeconds()
        if (remainingSeconds > 0) {
            speakText("Please wait $remainingSeconds seconds before trying again.")
            return
        }

        lastApiCallTime = System.currentTimeMillis()
        checkInUiState.value = CheckInUiState.Loading
        lifecycleScope.launch {
            val symptomsStr = if (hasSymptoms) "Yes" else "No"
            val result = CheckInApi.signInManual(
                firstName = firstName,
                lastName = lastName,
                phoneNumber = phone,
                purpose = purpose,
                symptoms = symptomsStr
            )

            result.onSuccess { response ->
                checkInUiState.value = CheckInUiState.Success(response)
                val messageToSpeak = if (response.status == "already_checked_in") {
                    val name = response.visitorName.takeIf { !it.isNullOrBlank() }
                        ?: "$firstName $lastName"
                    val msg = response.message.takeIf { !it.isNullOrBlank() }
                        ?: "You are already checked in."
                    "$name, $msg"
                } else {
                    response.message.takeIf { !it.isNullOrBlank() } ?: "Visitor checked in."
                }
                speakText(messageToSpeak)
            }.onFailure { error ->
                val errorMessage = error.message ?: "Check-in request failed"
                checkInUiState.value = CheckInUiState.Error(errorMessage)
                speakText(errorMessage)
            }
        }
    }

    private fun performManualSignOut(
        phone: String
    ) {
        if (checkInUiState.value !is CheckInUiState.Idle) {
            return
        }

        val remainingSeconds = getApiCooldownRemainingSeconds()
        if (remainingSeconds > 0) {
            speakText("Please wait $remainingSeconds seconds before trying again.")
            return
        }

        lastApiCallTime = System.currentTimeMillis()
        checkInUiState.value = CheckInUiState.Loading
        lifecycleScope.launch {
            val result = CheckInApi.signOutManual(phoneNumber = phone)

            result.onSuccess { response ->
                checkInUiState.value = CheckInUiState.Success(response)
                val messageToSpeak = response.message.takeIf { !it.isNullOrBlank() }
                    ?: if (response.status == "not_checked_in") "You are not signed in. Please sign in first." else "Goodbye."
                speakText(messageToSpeak)
            }.onFailure { error ->
                val errorMessage = error.message ?: "Sign-out request failed"
                checkInUiState.value = CheckInUiState.Error(errorMessage)
                speakText(errorMessage)
            }
        }
    }

    override fun onDestroy() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (_: Exception) {}
        super.onDestroy()
    }

    /**
     * Intercepts OS-level key events at the Window root before they are routed
     * to Jetpack Compose UI elements or active TextFields.
     */
    @Suppress("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // When a message box/dialog is active on screen (checkInUiState is NOT Idle),
        // completely consume and ignore all key events from the scanner/keyboard (except volume control).
        // The message box must remain visible and not get dismissed, and no action/TTS should occur.
        if (checkInUiState.value !is CheckInUiState.Idle) {
            scanBuffer.setLength(0)
            if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP || event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                return super.dispatchKeyEvent(event)
            }
            return true // CONSUME EVENT: Ignore scanner/keyboard input and prevent message box dismissal
        }

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
             * 1. SCAN COMPLETION / TERMINATOR HANDLING (ENTER & TAB KEYS)
             * Scanners emit an ENTER key (\n / \r) or TAB at the end of a scan payload.
             */
            if (event.keyCode == KeyEvent.KEYCODE_ENTER || event.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER || event.keyCode == KeyEvent.KEYCODE_TAB) {
                if (scanBuffer.isNotEmpty()) {
                    val fullCode = scanBuffer.toString().trim()
                    scanBuffer.setLength(0) // Reset buffer for next scan

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
                } else {
                    // CONSUME TRAILING TERMINATOR KEYS (e.g. \n after \r in CRLF scanner suffix)
                    return true
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
