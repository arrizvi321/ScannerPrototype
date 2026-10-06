package com.example.scannerprototype.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scannerprototype.R
import com.example.scannerprototype.data.CheckInUiState

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

private enum class ActiveScreen {
    Home,
    ManualSignIn,
    ManualSignOut
}

/**
 * Home screen and Navigation host for Greeter Application displaying:
 * - Home Screen with centered title, manual sign in/out buttons, QR instruction, QR image, and scanned QR code field.
 * - Manual Sign-In Screen with a complete Check-In form submitting to /api/sign-in.
 * - Manual Sign-Out Screen with a Phone Number form submitting to /api/sign-out.
 * - Result dialog with a 10-second countdown line.
 */
@Composable
fun ScannerTestScreen(
    scannedCodeText: String,
    onScannedCodeChanged: (String) -> Unit,
    checkInState: CheckInUiState,
    onDismissDialog: () -> Unit,
    onManualCheckInSubmit: (firstName: String, lastName: String, phone: String, purpose: String, hasSymptoms: Boolean) -> Unit,
    onManualSignOutSubmit: (phone: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var activeScreen by remember { mutableStateOf(ActiveScreen.Home) }

    when (activeScreen) {
        ActiveScreen.Home -> {
            HomeScreenContent(
                scannedCodeText = scannedCodeText,
                onScannedCodeChanged = onScannedCodeChanged,
                checkInState = checkInState,
                onDismiss = onDismissDialog,
                onOpenManualSignIn = { activeScreen = ActiveScreen.ManualSignIn },
                onOpenManualSignOut = { activeScreen = ActiveScreen.ManualSignOut },
                modifier = modifier
            )
        }
        ActiveScreen.ManualSignIn -> {
            ManualSignInForm(
                checkInState = checkInState,
                onDismiss = onDismissDialog,
                onBack = { activeScreen = ActiveScreen.Home },
                onSubmit = { firstName, lastName, phone, purpose, hasSymptoms ->
                    onManualCheckInSubmit(firstName, lastName, phone, purpose, hasSymptoms)
                },
                modifier = modifier
            )
        }
        ActiveScreen.ManualSignOut -> {
            ManualSignOutForm(
                checkInState = checkInState,
                onDismiss = onDismissDialog,
                onBack = { activeScreen = ActiveScreen.Home },
                onSubmit = { phone ->
                    onManualSignOutSubmit(phone)
                },
                modifier = modifier
            )
        }
    }
}

/**
 * Shared Response Card displaying check-in/out result details with a 10-second animated line bar at the bottom.
 */
@Composable
private fun CheckInResponseCard(
    checkInState: CheckInUiState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progressAnim = remember(checkInState) { Animatable(1f) }

    // Automatically animate the 10-second countdown visual line and clear response message on completion
    LaunchedEffect(checkInState) {
        if (checkInState is CheckInUiState.Success || checkInState is CheckInUiState.Error) {
            progressAnim.snapTo(1f)
            progressAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 10000, easing = LinearEasing)
            )
            onDismiss()
        }
    }

    if (checkInState !is CheckInUiState.Idle) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (checkInState is CheckInUiState.Error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = modifier.fillMaxWidth(0.92f)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (checkInState) {
                        is CheckInUiState.Idle -> { /* Handled by outer guard */ }
                        is CheckInUiState.Loading -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(36.dp),
                                    strokeWidth = 4.dp
                                )
                                Text(
                                    text = "Checking in...",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        is CheckInUiState.Success -> {
                            val response = checkInState.response
                            val formattedCheckedIn = formatIsoTimestamp(response.checkedInAt)
                            val formattedCheckedOut = formatIsoTimestamp(response.checkedOutAt)

                            if (response.status == "already_checked_in") {
                                val name = response.visitorName.takeIf { !it.isNullOrBlank() }
                                    ?: "${response.firstName.orEmpty()} ${response.lastName.orEmpty()}".trim().ifBlank { "Visitor" }
                                val msg = response.message.takeIf { !it.isNullOrBlank() }
                                    ?: "You are already checked in. Please check out."
                                val checkInTimeStr = formattedCheckedIn ?: response.checkedInAt.orEmpty()

                                Text(
                                    text = "$name, $msg You checked in at $checkInTimeStr",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 34.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "ALREADY CHECKED IN",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }

                                if (!response.purpose.isNullOrBlank()) {
                                    Text(
                                        text = "Purpose of Visit: ${response.purpose}",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            } else if (response.status == "not_checked_in") {
                                val msg = response.message.takeIf { !it.isNullOrBlank() }
                                    ?: "You are not signed in. Please sign in first."

                                Text(
                                    text = msg,
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 34.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "NOT SIGNED IN",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }

                                if (!response.purpose.isNullOrBlank()) {
                                    Text(
                                        text = "Purpose of Visit: ${response.purpose}",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            } else {
                                val mainDisplayMessage = response.message.takeIf { !it.isNullOrBlank() }
                                    ?: if (response.checkedOutAt.isNullOrBlank()) "Visitor checked in." else "Check-out Successful"

                                Text(
                                    text = mainDisplayMessage,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 40.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                if (!response.status.isNullOrBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = response.status.replace("_", " ").uppercase(),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }

                                if (!response.checkedOutAt.isNullOrBlank()) {
                                    if (formattedCheckedOut != null) {
                                        Text(
                                            text = "You checked out at: $formattedCheckedOut",
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                    if (formattedCheckedIn != null) {
                                        Text(
                                            text = "Checked in at: $formattedCheckedIn",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Medium,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    if (formattedCheckedIn != null) {
                                        Text(
                                            text = "You checked in at: $formattedCheckedIn",
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                }

                                if (!response.purpose.isNullOrBlank()) {
                                    Text(
                                        text = "Purpose of Visit: ${response.purpose}",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }
                        is CheckInUiState.Error -> {
                            Text(
                                text = "Check-in Failed",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.error
                            )
                            val displayError = if (checkInState.errorMessage.contains("invalid", ignoreCase = true) || checkInState.errorMessage.contains("pass", ignoreCase = true)) {
                                "Please try again."
                            } else {
                                checkInState.errorMessage
                            }
                            Text(
                                text = displayError,
                                fontSize = 20.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // Visual line bar indicating 10 second countdown timer
                if (checkInState is CheckInUiState.Success || checkInState is CheckInUiState.Error) {
                    LinearProgressIndicator(
                        progress = { progressAnim.value },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp),
                        color = if (checkInState is CheckInUiState.Error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeScreenContent(
    scannedCodeText: String,
    onScannedCodeChanged: (String) -> Unit,
    checkInState: CheckInUiState,
    onDismiss: () -> Unit,
    onOpenManualSignIn: () -> Unit,
    onOpenManualSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                })
            }
            .padding(32.dp)
    ) {
        // Center Content: Response Card + Title "Greeter Application" + Buttons directly under it
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            // Rich Response Card displayed directly above "Greeter Application" title
            CheckInResponseCard(
                checkInState = checkInState,
                onDismiss = onDismiss
            )

            Text(
                text = "Greeter Application",
                fontSize = 44.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                lineHeight = 52.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onOpenManualSignIn,
                    contentPadding = PaddingValues(horizontal = 28.dp, vertical = 18.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Manual Sign in",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onOpenManualSignOut,
                    contentPadding = PaddingValues(horizontal = 28.dp, vertical = 18.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Manual Sign out",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = "If you have a QR code wallet pass, simply scan it on the code scanner for quick sign in/sign out.",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Scan this to sign in from your phone and create wallet pass",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Image(
                    painter = painterResource(id = R.drawable.qr),
                    contentDescription = "Scan this to sign in from your phone and create wallet pass",
                    modifier = Modifier.size(170.dp)
                )
            }
        }

        // Bottom Content: Scanned QR code text field visible at the bottom
        Column(
            modifier = Modifier.align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = scannedCodeText,
                onValueChange = onScannedCodeChanged,
                label = { Text("Scanned QR Code", fontSize = 18.sp) },
                placeholder = { Text("Scanned code will appear here", fontSize = 20.sp) },
                textStyle = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Medium),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

/**
 * Manual Sign-In Check-In Form Screen submitting to /api/sign-in
 */
@Composable
private fun ManualSignInForm(
    checkInState: CheckInUiState,
    onDismiss: () -> Unit,
    onBack: () -> Unit,
    onSubmit: (firstName: String, lastName: String, phone: String, purpose: String, hasSymptoms: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var rawPhoneDigits by remember { mutableStateOf("") }
    var selectedPurpose by remember { mutableStateOf("Visiting a resident") }
    var hasSymptoms by remember { mutableStateOf(false) }

    var isDropdownExpanded by remember { mutableStateOf(false) }
    var firstNameError by remember { mutableStateOf<String?>(null) }
    var lastNameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val purposeOptions = listOf("Visiting a resident", "Appointment", "Delivery", "Other")

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                })
            }
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header with Back arrow & "Check-in form" title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(onClick = onBack) {
                    Text(
                        text = "←",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B4980)
                    )
                }
                Text(
                    text = "Check-in form",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B4980)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // First name & Last name side-by-side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // First name field
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "First name",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B4980),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = {
                            firstName = it
                            if (it.isNotBlank()) firstNameError = null
                        },
                        isError = firstNameError != null,
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 18.sp),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (firstNameError != null) {
                        Text(
                            text = firstNameError!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // Last name field
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Last name",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B4980),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = lastName,
                        onValueChange = {
                            lastName = it
                            if (it.isNotBlank()) lastNameError = null
                        },
                        isError = lastNameError != null,
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 18.sp),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (lastNameError != null) {
                        Text(
                            text = lastNameError!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // Phone number field
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Phone number",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B4980),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                OutlinedTextField(
                    value = rawPhoneDigits,
                    onValueChange = { newText ->
                        val digits = newText.filter { it.isDigit() }
                        rawPhoneDigits = if (digits.length > 10 && digits.startsWith("1")) digits.substring(1) else digits.take(10)
                        if (isValidCanadianPhoneNumber(rawPhoneDigits)) {
                            phoneError = null
                        }
                    },
                    visualTransformation = CanadianPhoneVisualTransformation(),
                    placeholder = { Text("(416) 555-0123", fontSize = 18.sp) },
                    isError = phoneError != null,
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 18.sp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    leadingIcon = {
                        Text(
                            text = "🇨🇦 ▾  +1",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 12.dp, end = 8.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                if (phoneError != null) {
                    Text(
                        text = phoneError!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Purpose of visit dropdown
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Purpose of visit",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B4980),
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedPurpose,
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                        trailingIcon = {
                            Text(
                                text = if (isDropdownExpanded) "▴" else "▾",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Transparent clickable overlay covering the entire text field area
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { isDropdownExpanded = !isDropdownExpanded }
                    )

                    DropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        purposeOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(text = option, fontSize = 18.sp) },
                                onClick = {
                                    selectedPurpose = option
                                    isDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Do you currently have symptoms of illness? Box
            OutlinedCard(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Do you currently have symptoms of illness?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B4980)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.pointerInput(Unit) {
                                detectTapGestures { hasSymptoms = false }
                            }
                        ) {
                            RadioButton(
                                selected = !hasSymptoms,
                                onClick = { hasSymptoms = false }
                            )
                            Text(
                                text = "No",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.pointerInput(Unit) {
                                detectTapGestures { hasSymptoms = true }
                            }
                        ) {
                            RadioButton(
                                selected = hasSymptoms,
                                onClick = { hasSymptoms = true }
                            )
                            Text(
                                text = "Yes",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Submit form Button
            Button(
                onClick = {
                    var isValid = true

                    if (firstName.isBlank()) {
                        firstNameError = "First name is required"
                        isValid = false
                    } else {
                        firstNameError = null
                    }

                    if (lastName.isBlank()) {
                        lastNameError = "Last name is required"
                        isValid = false
                    } else {
                        lastNameError = null
                    }

                    if (!isValidCanadianPhoneNumber(rawPhoneDigits)) {
                        phoneError = "Please enter a valid 10-digit Canadian phone number"
                        isValid = false
                    } else {
                        phoneError = null
                    }

                    if (isValid) {
                        onSubmit(firstName.trim(), lastName.trim(), rawPhoneDigits, selectedPurpose, hasSymptoms)
                    }
                },
                enabled = checkInState is CheckInUiState.Idle,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1B4980)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
            ) {
                Text(
                    text = "Submit form",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Response Card displayed near the bottom of the Manual Sign In form screen
            if (checkInState !is CheckInUiState.Idle) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CheckInResponseCard(
                        checkInState = checkInState,
                        onDismiss = onDismiss
                    )
                }
            }
        }
    }
}

/**
 * Manual Sign-Out Form Screen submitting to /api/sign-out
 */
@Composable
private fun ManualSignOutForm(
    checkInState: CheckInUiState,
    onDismiss: () -> Unit,
    onBack: () -> Unit,
    onSubmit: (phone: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var rawPhoneDigits by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                })
            }
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header with Back arrow & "Check-out form" title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(onClick = onBack) {
                    Text(
                        text = "←",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B4980)
                    )
                }
                Text(
                    text = "Check-out form",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B4980)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Phone number field
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Phone number",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B4980),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                OutlinedTextField(
                    value = rawPhoneDigits,
                    onValueChange = { newText ->
                        val digits = newText.filter { it.isDigit() }
                        rawPhoneDigits = if (digits.length > 10 && digits.startsWith("1")) digits.substring(1) else digits.take(10)
                        if (isValidCanadianPhoneNumber(rawPhoneDigits)) {
                            phoneError = null
                        }
                    },
                    visualTransformation = CanadianPhoneVisualTransformation(),
                    placeholder = { Text("(416) 555-0123", fontSize = 18.sp) },
                    isError = phoneError != null,
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 18.sp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    leadingIcon = {
                        Text(
                            text = "🇨🇦 ▾  +1",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 12.dp, end = 8.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                if (phoneError != null) {
                    Text(
                        text = phoneError!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Submit form Button
            Button(
                onClick = {
                    if (!isValidCanadianPhoneNumber(rawPhoneDigits)) {
                        phoneError = "Please enter a valid 10-digit Canadian phone number"
                    } else {
                        phoneError = null
                        onSubmit(rawPhoneDigits)
                    }
                },
                enabled = checkInState is CheckInUiState.Idle,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1B4980)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
            ) {
                Text(
                    text = "Submit form",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Response Card displayed near the bottom of the Manual Sign Out form screen
            if (checkInState !is CheckInUiState.Idle) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CheckInResponseCard(
                        checkInState = checkInState,
                        onDismiss = onDismiss
                    )
                }
            }
        }
    }
}

/**
 * Visual Transformation that formats unformatted raw phone digits (e.g. "4165551234")
 * into Canadian display format "(416) 555-1234" while preserving exact cursor position.
 */
private class CanadianPhoneVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text.filter { it.isDigit() }.take(10)

        val formatted = StringBuilder()
        for (i in raw.indices) {
            when (i) {
                0 -> formatted.append("(").append(raw[i])
                3 -> formatted.append(") ").append(raw[i])
                6 -> formatted.append("-").append(raw[i])
                else -> formatted.append(raw[i])
            }
        }

        val transformedString = formatted.toString()

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val clamped = offset.coerceIn(0, raw.length)
                return when {
                    clamped <= 0 -> 0
                    clamped <= 3 -> clamped + 1
                    clamped <= 6 -> clamped + 3
                    else -> clamped + 4
                }.coerceAtMost(transformedString.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                val clamped = offset.coerceIn(0, transformedString.length)
                return when {
                    clamped <= 1 -> 0
                    clamped <= 4 -> clamped - 1
                    clamped <= 8 -> (clamped - 3).coerceIn(0, raw.length)
                    else -> (clamped - 4).coerceIn(0, raw.length)
                }
            }
        }

        return TransformedText(AnnotatedString(transformedString), offsetMapping)
    }
}

/**
 * Validates whether a given string is a valid Canadian phone number (10 digits with a valid Canadian area code).
 */
private fun isValidCanadianPhoneNumber(phoneInput: String): Boolean {
    val digits = phoneInput.filter { it.isDigit() }
    val national = if (digits.length == 11 && digits.startsWith("1")) {
        digits.substring(1)
    } else {
        digits
    }

    if (national.length != 10) return false

    val exchangeCodeFirstDigit = national[3]
    if (exchangeCodeFirstDigit < '2') return false

    val canadianAreaCodes = setOf(
        "204", "226", "236", "249", "250", "289", "306", "343", "354", "365", "367", "382", "387",
        "403", "416", "418", "431", "437", "438", "450", "474", "506", "514", "519", "537", "548",
        "579", "581", "584", "587", "604", "613", "639", "647", "672", "683", "705", "709", "742",
        "778", "780", "782", "807", "819", "825", "867", "873", "902", "905"
    )

    return canadianAreaCodes.contains(national.substring(0, 3))
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
