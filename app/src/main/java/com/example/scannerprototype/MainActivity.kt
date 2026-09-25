package com.example.scannerprototype

import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.example.scannerprototype.ui.ScannerTestScreen

/**
 * Main Activity handling window-level hardware barcode scanner input interception.
 *
 * ARCHITECTURAL OVERVIEW:
 * USB HID Barcode Scanners act as physical keyboards. When a scan occurs, characters arrive
 * in rapid succession (<50ms apart) followed by an ENTER terminator.
 *
 * INTERCEPTION MECHANISM:
 * Standard Compose/View key listeners on TextFields cannot block printable characters once
 * a text field is focused. We override [dispatchKeyEvent] at the Activity root window level.
 * Because window key dispatching occurs BEFORE key events reach focused UI elements:
 * 1. We detect rapid key timing and physical HID input devices.
 * 2. We buffer character stream into an in-memory [scanBuffer].
 * 3. We return 'true' to consume events, preventing characters from typing into active TextFields.
 * 4. We clean up any initial character leaks once the scan stream completes.
 */
class MainActivity : ComponentActivity() {

    // States passed down to Compose UI
    val scannedCodeState = mutableStateOf("")
    val manualInputState = mutableStateOf("")

    private val scanBuffer = StringBuilder()
    private var lastKeyTime = 0L
    private val SCANNER_THRESHOLD_MS = 50L // Keypress interval threshold (scanners fire <30ms)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ScannerTestScreen(
                        scannedCode = scannedCodeState.value,
                        manualInputText = manualInputState.value,
                        onManualInputChanged = { manualInputState.value = it }
                    )
                }
            }
        }
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

                    scanBuffer.setLength(0) // Reset buffer for next scan
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