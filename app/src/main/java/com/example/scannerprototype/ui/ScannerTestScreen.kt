package com.example.scannerprototype.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scannerprototype.data.CheckInUiState
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * UI Component for rendering the manual entry field
 * and prominent check-in/check-out result message in the center of the screen.
 */
@Composable
fun ScannerTestScreen(
    manualInputText: String,
    onManualInputChanged: (String) -> Unit,
    checkInState: CheckInUiState,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(
        modifier = modifier
            .fillMaxSize()
            /*
             * TAP-TO-DISMISS KEYBOARD:
             * Dismisses focus and hides soft keyboard on background taps.
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
            // Top Section: Title & Manual Entry Field
            Column {
                Text(
                    text = "Scanner Check-In Prototype for Greeter Application",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                TextField(
                    value = manualInputText,
                    onValueChange = onManualInputChanged,
                    placeholder = { Text("Type anything here") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "The scanner shouldn't affect this manual text box so visitors can safely manual check in on robot screen while someone else comes and scans their code at the same time.",
                    fontSize = 20.sp,
                    color = Color.Black,
                    lineHeight = 35.sp
                )
            }

            // Center Section: Displaying check-in/out response message & timestamps in large text
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                when (checkInState) {
                    is CheckInUiState.Idle -> {
                        Text(
                            text = "Scan a QR code to check in",
                            fontSize = 24.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    is CheckInUiState.Loading -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(48.dp))
                            Text(
                                text = "Checking in...",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    is CheckInUiState.Success -> {
                        val response = checkInState.response
                        val mainDisplayMessage = response.message.takeIf { !it.isNullOrBlank() }
                            ?: if (response.checkedOutAt.isNullOrBlank()) "Check-in Successful" else "Check-out Successful"

                        val formattedCheckedIn = formatIsoTimestamp(response.checkedInAt)
                        val formattedCheckedOut = formatIsoTimestamp(response.checkedOutAt)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = mainDisplayMessage,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                lineHeight = 44.sp,
                                color = MaterialTheme.colorScheme.primary
                            )

                            if (!response.status.isNullOrBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = response.status.replace("_", " ").uppercase(),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Timestamp Displays
                            if (!response.checkedOutAt.isNullOrBlank()) {
                                // USER IS CHECKING OUT (checkoutat is not null)
                                if (formattedCheckedOut != null) {
                                    Text(
                                        text = "You checked out at: $formattedCheckedOut",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                                if (formattedCheckedIn != null) {
                                    Text(
                                        text = "Checked in at: $formattedCheckedIn",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                // USER IS CHECKING IN (checkoutat is null)
                                if (formattedCheckedIn != null) {
                                    Text(
                                        text = "You checked in at: $formattedCheckedIn",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }
                    }
                    is CheckInUiState.Error -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Check-in Failed",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = checkInState.errorMessage,
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Parses ISO 8601 timestamps (e.g. "2026-09-29T16:45:00.000Z") and formats them
 * into a user-friendly string localized to system time zone.
 */
private fun formatIsoTimestamp(isoString: String?): String? {
    if (isoString.isNullOrBlank()) return null
    val patterns = arrayOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSX",
        "yyyy-MM-dd'T'HH:mm:ssX",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'"
    )
    for (pattern in patterns) {
        try {
            val parseFormat = SimpleDateFormat(pattern, Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = parseFormat.parse(isoString)
            if (date != null) {
                val outputFormat = DateFormat.getDateTimeInstance(
                    DateFormat.MEDIUM,
                    DateFormat.SHORT,
                    Locale.getDefault()
                )
                return outputFormat.format(date)
            }
        } catch (_: Exception) {
            // Try next pattern
        }
    }
    return isoString
}
