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

class MainActivity : ComponentActivity() {

    // States passed down to Compose UI
    val scannedCodeState = mutableStateOf("")
    val manualInputState = mutableStateOf("")

    private val scanBuffer = StringBuilder()
    private var lastKeyTime = 0L
    private val SCANNER_THRESHOLD_MS = 50L

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

    @Suppress("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            val currentTime = System.currentTimeMillis()
            val timeDiff = currentTime - lastKeyTime
            lastKeyTime = currentTime

            // Check if input originates from a physical USB HID device (scanner)
            val isPhysicalHardwareKey = event.device != null &&
                    !event.device.isVirtual &&
                    event.device.keyboardType != InputDevice.KEYBOARD_TYPE_NONE

            // 1. Handle ENTER key (End of Scan)
            if (event.keyCode == KeyEvent.KEYCODE_ENTER || event.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                if (scanBuffer.isNotEmpty()) {
                    val fullCode = scanBuffer.toString().trim()
                    scannedCodeState.value = fullCode

                    // CLEANUP: If the first 1-2 digits leaked into the manual input state during the scan, trim them out
                    val currentText = manualInputState.value
                    if (currentText.isNotEmpty()) {
                        // Check if the current manual text ends with the leaked prefix of the scan
                        for (length in minOf(currentText.length, 3) downTo 1) {
                            val leakedPrefix = fullCode.take(length)
                            if (currentText.endsWith(leakedPrefix)) {
                                manualInputState.value = currentText.dropLast(length)
                                break
                            }
                        }
                    }

                    scanBuffer.setLength(0)
                    return true // Consume ENTER
                }
            } else {
                // 2. Intercept hardware key character
                val unicodeChar = event.getUnicodeChar(event.metaState)
                if (unicodeChar != 0) {
                    val char = unicodeChar.toChar()

                    // If keystrokes are rapid OR coming from an external physical keyboard/scanner
                    if (timeDiff < SCANNER_THRESHOLD_MS || scanBuffer.isNotEmpty() || isPhysicalHardwareKey) {
                        if (char != '\u0000' && !char.isISOControl()) {
                            scanBuffer.append(char)
                            return true // CONSUME EVENT: Blocks character from reaching active TextField
                        }
                    } else {
                        // Slow keypress from software keyboard -> Clear scanner buffer
                        scanBuffer.setLength(0)
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }
}