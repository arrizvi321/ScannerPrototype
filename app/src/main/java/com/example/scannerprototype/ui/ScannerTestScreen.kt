package com.example.scannerprototype.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * UI Component for rendering the manual entry field and scanned barcode display.
 *
 * FOCUS MANAGEMENT:
 * - Uses [pointerInput] with [detectTapGestures] on the root container so tapping outside an active
 *   TextField automatically clears focus and hides the soft keyboard.
 * - Displays the captured barcode state passed from [MainActivity] independently of active input fields.
 */
@Composable
fun ScannerTestScreen(
    scannedCode: String,
    manualInputText: String,
    onManualInputChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(
        modifier = modifier
            .fillMaxSize()
            /*
             * TAP-TO-DISMISS KEYBOARD:
             * Intercepts background tap gestures to dismiss focus and hide the software IME keyboard.
             */
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                })
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Scanner Test",
                    fontSize = 28.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Manual Entry Field:",
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                /*
                 * VISIBLE MANUAL INPUT FIELD:
                 * Standard TextField for user typing. Physical USB scanner hardware input targeting
                 * this field is intercepted upstream in MainActivity.dispatchKeyEvent.
                 */
                TextField(
                    value = manualInputText,
                    onValueChange = onManualInputChanged,
                    placeholder = { Text("Type manually here...") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            /*
             * SCANNED RESULT DISPLAY CARD:
             * Displays the scanned barcode received from the Activity key interceptor.
             */
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Last Scanned Code:",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = scannedCode.ifEmpty { "Nothing scanned yet" },
                        fontSize = 20.sp,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}