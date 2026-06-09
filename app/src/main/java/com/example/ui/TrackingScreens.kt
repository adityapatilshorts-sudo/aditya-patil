package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.LiveLocationEntity
import com.example.data.TripHistoryEntity
import com.example.data.UserProfileEntity
import com.example.data.RoutePoint
import com.example.ui.theme.*

@Composable
fun MainAppNavigation(
    viewModel: TrackingViewModel,
    modifier: Modifier = Modifier
) {
    val currentProfile by viewModel.currentProfile.collectAsState()
    val allTrips by viewModel.allTrips.collectAsState()
    val liveLocation by viewModel.liveLocation.collectAsState()
    val isGoogleChooserOpen by viewModel.isGoogleAccountChooserOpen.collectAsState()
    val simulatedRoleOverride by viewModel.simulatedScreenRole.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            currentProfile == null || !currentProfile!!.isLoggedIn -> {
                SignInScreen(
                    onLoginClick = { viewModel.openGoogleChooser() }
                )
            }
            simulatedRoleOverride == null && currentProfile!!.selectedRole == null -> {
                RoleSelectionScreen(
                    onSelectRole = { role -> viewModel.selectRole(role) }
                )
            }
            else -> {
                // Main active dashboard based on role (or override simulator)
                val activeRole = simulatedRoleOverride ?: currentProfile!!.selectedRole ?: "STUDENT"
                DashboardWrapper(
                    role = activeRole,
                    profile = currentProfile!!,
                    trips = allTrips,
                    liveLocation = liveLocation,
                    viewModel = viewModel
                )
            }
        }

        // Animated Google Sign-In Selector
        if (isGoogleChooserOpen) {
            GoogleChooserDialog(
                onDismiss = { viewModel.closeGoogleChooser() },
                onSelectAccount = { email, name ->
                    viewModel.loginWithGoogle(email, name, "https://api.dicebear.com/7.x/bottts/svg?seed=Transit")
                }
            )
        }

        // Live Simulation Floating Control Center on top (extremely convenient for verification!) Only visible if logged in
        if (currentProfile?.isLoggedIn == true) {
            SimulationConsoleFloatingBadge(
                currentSimRole = simulatedRoleOverride ?: currentProfile?.selectedRole,
                onOverrideRole = { role -> viewModel.overrideRoleForSimulation(role) },
                onLogout = { viewModel.logout() }
            )
        }
    }
}

// 1. Google Account Chooser Simulator Dialog
@Composable
fun GoogleChooserDialog(
    onDismiss: () -> Unit,
    onSelectAccount: (String, String) -> Unit
) {
    var isCustomAccountMode by remember { mutableStateOf(false) }
    var customName by remember { mutableStateOf("") }
    var customEmail by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("google_chooser_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .animateContentSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Simulated Google Colored 'G'
                Row(
                    modifier = Modifier.padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "G", fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, color = Color(0xFF4285F4), fontFamily = FontFamily.SansSerif)
                    Text(text = "o", fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, color = Color(0xFFEA4335), fontFamily = FontFamily.SansSerif)
                    Text(text = "o", fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, color = Color(0xFFFBBC05), fontFamily = FontFamily.SansSerif)
                    Text(text = "g", fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, color = Color(0xFF4285F4), fontFamily = FontFamily.SansSerif)
                    Text(text = "l", fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, color = Color(0xFF34A853), fontFamily = FontFamily.SansSerif)
                    Text(text = "e", fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, color = Color(0xFFEA4335), fontFamily = FontFamily.SansSerif)
                }

                Text(
                    text = "Sign in to Bus & Van Tracker",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (isCustomAccountMode) "Enter account details" else "Choose an account to continue",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )

                HorizontalDivider()

                if (!isCustomAccountMode) {
                    // Account Option 1 (Matches user credentials exactly)
                    ListItem(
                        modifier = Modifier
                            .clickable { onSelectAccount("rameshwarpatil82@gmail.com", "Rameshwar Patil") }
                            .testTag("account_primary"),
                        headlineContent = { Text("Rameshwar Patil", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("rameshwarpatil82@gmail.com") },
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(SchoolBusGold),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("R", color = SlateDarkNavy, fontWeight = FontWeight.Bold)
                            }
                        }
                    )

                    HorizontalDivider()

                    // Account Option 2 (Demo Guest)
                    ListItem(
                        modifier = Modifier.clickable { onSelectAccount("guest.parent@gmail.com", "Simulated Parent Guest") },
                        headlineContent = { Text("Simulated Parent Guest", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("guest.parent@gmail.com") },
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(StatusBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("P", color = PureWhite, fontWeight = FontWeight.Bold)
                            }
                        }
                    )

                    HorizontalDivider()

                    // Account Option 3 (Demo Student)
                    ListItem(
                        modifier = Modifier.clickable { onSelectAccount("alex.student@gmail.com", "Alex Mercer") },
                        headlineContent = { Text("Alex Mercer (Student)", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("alex.student@gmail.com") },
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(GreenOnline),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("S", color = SlateDarkNavy, fontWeight = FontWeight.Bold)
                            }
                        }
                    )

                    HorizontalDivider()

                    // Account Option 4: CUSTOM INPUT TRIGGER
                    ListItem(
                        modifier = Modifier
                            .clickable { isCustomAccountMode = true }
                            .testTag("account_custom_trigger"),
                        headlineContent = { Text("Use another account", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary) },
                        supportingContent = { Text("Sign in with any custom name & email") },
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add account",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    )
                } else {
                    // Render Custom Text Fields beautifully matching High Density forms
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = customName,
                            onValueChange = { customName = it },
                            label = { Text("Your Full Name") },
                            placeholder = { Text("e.g. John Doe") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_login_name_input"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = customEmail,
                            onValueChange = { customEmail = it },
                            label = { Text("Your Email Address") },
                            placeholder = { Text("e.g. john.doe@email.com") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_login_email_input"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { isCustomAccountMode = false },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Back")
                            }

                            Button(
                                onClick = {
                                    if (customName.isNotBlank() && customEmail.isNotBlank()) {
                                        onSelectAccount(customEmail.trim(), customName.trim())
                                    }
                                },
                                modifier = Modifier.weight(1.5f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                enabled = customName.isNotBlank() && customEmail.isNotBlank()
                            ) {
                                Text("Sign In")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f))
                }
            }
        }
    }
}

// 2. Beautiful SignIn Landing Screen
@Composable
fun SignInScreen(onLoginClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "bus_pulse")
    val scalePulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Branding Circle Panel
        Box(
            modifier = Modifier
                .size(170.dp)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                SchoolBusGold.copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        ),
                        radius = size.width * 0.75f * scalePulse
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(SchoolBusGold)
                    .border(2.dp, PureWhite, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsBus,
                    contentDescription = "Bus Tracker Logo",
                    tint = SlateDarkNavy,
                    modifier = Modifier.size(60.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Titles
        Text(
            text = "Transit Tracker",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = SchoolBusGold,
            textAlign = TextAlign.Center
        )

        Text(
            text = "School Bus & Van Live Tracking",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(36.dp))

        // Features Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                FeatureRow(Icons.Default.PinDrop, "Real-Time Tracking", "Watch vehicles update live on high-contrast layouts.")
                FeatureRow(Icons.Default.VerifiedUser, "Three Specialized Roles", "Tailored interfaces for Drivers, Parents, and Students.")
                FeatureRow(Icons.Default.History, "Instant Trip Statistics", "Log trip timings and duration history seamlessly.")
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Custom Styled Google OAuth Button
        Button(
            onClick = onLoginClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("google_login_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = PureWhite,
                contentColor = SlateDarkNavy
            ),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(horizontal = 24.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "G ",
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    color = Color(0xFF4285F4)
                )
                Text(
                    text = "Sign in with Google",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateDarkNavy
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun FeatureRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(SchoolBusGold.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SchoolBusGold,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f))
        }
    }
}

// 3. Role Selection Screen ("Who are you?")
@Composable
fun RoleSelectionScreen(onSelectRole: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Welcome aboard!",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = SchoolBusGold,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Tell us: Who are you?",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, bottom = 28.dp)
        )

        // Driver Selection Card
        RoleCard(
            title = "Driver",
            description = "Start trips, broadcast live GPS location updates, and access your personal driving ledger.",
            icon = Icons.Default.DirectionsBus,
            accentColor = SchoolBusGold,
            tag = "role_card_driver",
            onClick = { onSelectRole("DRIVER") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Parent Selection Card
        RoleCard(
            title = "Parent",
            description = "Track your child's vehicle coordinates live on the map and view active run sheets.",
            icon = Icons.Default.Face,
            accentColor = StatusBlue,
            tag = "role_card_parent",
            onClick = { onSelectRole("PARENT") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Student Selection Card
        RoleCard(
            title = "Student",
            description = "Stay updated with scheduled academic route stops and view moving shuttles dynamically.",
            icon = Icons.Default.School,
            accentColor = GreenOnline,
            tag = "role_card_student",
            onClick = { onSelectRole("STUDENT") }
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun RoleCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    tag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(tag),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = "$title badge icon",
                    tint = accentColor,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Icon(
                imageVector = Icons.Default.ArrowForwardIos,
                contentDescription = "Forward to Role Dashboard",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// 4. Shared Vector Blueprint School Map Composable
@Composable
fun SchoolBusMap(
    liveLocation: LiveLocationEntity?,
    routePoints: List<RoutePoint>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .background(SlateDarkNavy)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val columns = 12
            val rows = 12
            val widthStep = size.width / columns
            val heightStep = size.height / rows

            // Draw Blueprint GridLines
            for (i in 0..columns) {
                drawLine(
                    color = SubtleGray.copy(alpha = 0.08f),
                    start = Offset(i * widthStep, 0f),
                    end = Offset(i * widthStep, size.height),
                    strokeWidth = 1f
                )
            }
            for (j in 0..rows) {
                drawLine(
                    color = SubtleGray.copy(alpha = 0.08f),
                    start = Offset(0f, j * heightStep),
                    end = Offset(size.width, j * heightStep),
                    strokeWidth = 1f
                )
            }

            // Map boundaries coordinates projection
            val minLat = 37.7700
            val maxLat = 37.7790
            val minLng = -122.4230
            val maxLng = -122.4130

            fun projectToCanvas(lat: Double, lng: Double): Offset {
                val x = ((lng - minLng) / (maxLng - minLng)) * size.width
                val y = size.height - (((lat - minLat) / (maxLat - minLat)) * size.height)
                return Offset(x.toFloat(), y.toFloat())
            }

            // Draw Roads path segments
            val pointsCount = routePoints.size
            for (i in 0 until pointsCount) {
                val startPoint = routePoints[i]
                val endPoint = routePoints[(i + 1) % pointsCount]
                val startOffset = projectToCanvas(startPoint.latitude, startPoint.longitude)
                val endOffset = projectToCanvas(endPoint.latitude, endPoint.longitude)

                drawLine(
                    color = SlateDarkNavy,
                    start = startOffset,
                    end = endOffset,
                    strokeWidth = 16f
                )
                drawLine(
                    color = LightSlateSurface.copy(alpha = 0.8f),
                    start = startOffset,
                    end = endOffset,
                    strokeWidth = 12f
                )
                drawLine(
                    color = SchoolBusGold.copy(alpha = 0.8f),
                    start = startOffset,
                    end = endOffset,
                    strokeWidth = 1.6f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            }

            // Draw Landmark Stops Pins
            routePoints.forEach { stop ->
                if (stop.isStop) {
                    val stopOffset = projectToCanvas(stop.latitude, stop.longitude)

                    drawCircle(
                        color = StatusBlue.copy(alpha = 0.25f),
                        radius = 16f,
                        center = stopOffset
                    )
                    drawCircle(
                        color = PureWhite,
                        radius = 8f,
                        center = stopOffset
                    )
                    drawCircle(
                        color = StatusBlue,
                        radius = 5.5f,
                        center = stopOffset
                    )
                }
            }

            // Draw Bus Position Pin
            if (liveLocation != null) {
                val busOffset = projectToCanvas(liveLocation.latitude, liveLocation.longitude)
                val pulsingRadius = 24f + (if (liveLocation.isActive) (System.currentTimeMillis() % 1000) / 1000f * 14f else 0f)

                drawCircle(
                    color = if (liveLocation.isActive) SchoolBusGold.copy(alpha = 0.35f) else SubtleGray.copy(alpha = 0.3f),
                    radius = pulsingRadius,
                    center = busOffset
                )
                drawCircle(
                    color = SlateDarkNavy,
                    radius = 14f,
                    center = busOffset
                )
                drawCircle(
                    color = if (liveLocation.isActive) SchoolBusGold else SubtleGray,
                    radius = 11f,
                    center = busOffset
                )
                drawCircle(
                    color = SlateDarkNavy,
                    radius = 4f,
                    center = busOffset
                )
            }
        }

        // Overlay text info panel
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .background(DeepCharcoal.copy(alpha = 0.90f), RoundedCornerShape(8.dp))
                .border(1.dp, PureWhite.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (liveLocation?.isActive == true) GreenOnline else RedOffline)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (liveLocation?.isActive == true) "VEHICLE LIVE" else "VEHICLE OFFLINE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (liveLocation?.isActive == true) GreenOnline else SubtleGray
                )
            }
        }

        // Speedometer overlay
        if (liveLocation?.isActive == true) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .background(DeepCharcoal.copy(alpha = 0.90f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Speed: 32 km/h",
                    fontSize = 11.sp,
                    color = SchoolBusGold,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// 5. Unified Dashboard Wrapper Screen
@Composable
fun DashboardWrapper(
    role: String,
    profile: UserProfileEntity,
    trips: List<TripHistoryEntity>,
    liveLocation: LiveLocationEntity?,
    viewModel: TrackingViewModel
) {
    Scaffold(
        bottomBar = {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(SchoolBusGold.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = SchoolBusGold,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = profile.fullName.ifEmpty { "Rameshwar Patil" },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Active Mode: $role",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    TextButton(
                        onClick = { viewModel.logout() },
                        modifier = Modifier.testTag("logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Logout current user session",
                            modifier = Modifier.size(16.dp),
                            tint = Color.Red
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Log Out", color = Color.Red, fontSize = 12.sp)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (role) {
                "DRIVER" -> DriverDashboard(profile, trips, liveLocation, viewModel)
                "PARENT" -> ParentDashboard(profile, liveLocation, viewModel)
                "STUDENT" -> StudentDashboard(profile, liveLocation, viewModel)
            }
        }
    }
}

@Composable
fun DashboardHeader(
    title: String,
    subtitle: String,
    avatarName: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsBus,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = subtitle.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    style = androidx.compose.ui.text.TextStyle(
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
        }

        // Action controls & Initial bubble matching High Density style card JD
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .clickable { /* Active button hint */ },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notification Center",
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (avatarName.isNotEmpty()) avatarName.take(2).uppercase() else "TR",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// A. DRIVER MODULE DASHBOARD
@Composable
fun DriverDashboard(
    profile: UserProfileEntity,
    trips: List<TripHistoryEntity>,
    liveLocation: LiveLocationEntity?,
    viewModel: TrackingViewModel
) {
    var nameInput by remember { mutableStateOf(profile.fullName) }
    var mobileInput by remember { mutableStateOf(profile.mobileNumber) }
    var vehicleInput by remember { mutableStateOf(profile.vehicleNumber) }
    var vehicleTypeSelected by remember { mutableStateOf(profile.vehicleType) }

    LaunchedEffect(profile) {
        nameInput = profile.fullName
        mobileInput = profile.mobileNumber
        vehicleInput = profile.vehicleNumber
        vehicleTypeSelected = profile.vehicleType
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        DashboardHeader(
            title = "SchoolTrack",
            subtitle = "Driver Console Hub",
            avatarName = profile.fullName.ifEmpty { "Driver" }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Driver Console",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Track route, dispatch updates",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f)
                )
            }

            Box(
                modifier = Modifier
                    .background(
                        if (liveLocation?.isActive == true) GreenOnline.copy(alpha = 0.2f) else RedOffline.copy(alpha = 0.2f),
                        CircleShape
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (liveLocation?.isActive == true) "ON TRIP" else "OFFLINE",
                    color = if (liveLocation?.isActive == true) GreenOnline else RedOffline,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // START / STOP TRIP BUTTONS
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (liveLocation?.isActive == true) "Active Trip is Live Streaming" else "Ready to Start Trip",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = if (liveLocation?.isActive == true) "Parents and school nodes can watch you crawling on map." else "Tapping below triggers automatic starting parameters.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )

                if (liveLocation?.isActive == true) {
                    Button(
                        onClick = { viewModel.stopTrip() },
                        colors = ButtonDefaults.buttonColors(containerColor = RedOffline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("stop_trip_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Stop Trip & Save History", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                } else {
                    Button(
                        onClick = { viewModel.startTrip() },
                        colors = ButtonDefaults.buttonColors(containerColor = SchoolBusGold),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("start_trip_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = SlateDarkNavy)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Live Trip", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SlateDarkNavy)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Live Trace Map
        Text(
            text = "Your Router Map Tracking",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 4.dp),
            fontSize = 15.sp
        )
        SchoolBusMap(
            liveLocation = liveLocation,
            routePoints = viewModel.schoolRoute,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Profile Configuration Settings
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Text(
                    text = "Vehicle & Profile Preferences",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = SchoolBusGold
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Driver Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("driver_name_input"),
                    shape = RoundedCornerShape(10.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = mobileInput,
                    onValueChange = { mobileInput = it },
                    label = { Text("Mobile Phone Number") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("driver_mobile_input"),
                    shape = RoundedCornerShape(10.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = vehicleInput,
                    onValueChange = { vehicleInput = it },
                    label = { Text("Vehicle ID / Plate No") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("driver_vehicle_input"),
                    shape = RoundedCornerShape(10.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text("Vehicle Transit Type", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { vehicleTypeSelected = "Bus" }
                            .testTag("vehicle_type_bus"),
                        border = BorderStroke(
                            2.dp,
                            if (vehicleTypeSelected == "Bus") SchoolBusGold else Color.Transparent
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (vehicleTypeSelected == "Bus") SchoolBusGold.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(imageVector = Icons.Default.DirectionsBus, contentDescription = null, tint = if (vehicleTypeSelected == "Bus") SchoolBusGold else MaterialTheme.colorScheme.onSurface)
                            Text("School Bus", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { vehicleTypeSelected = "Van" }
                            .testTag("vehicle_type_van"),
                        border = BorderStroke(
                            2.dp,
                            if (vehicleTypeSelected == "Van") SchoolBusGold else Color.Transparent
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (vehicleTypeSelected == "Van") SchoolBusGold.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(imageVector = Icons.Default.AirportShuttle, contentDescription = null, tint = if (vehicleTypeSelected == "Van") SchoolBusGold else MaterialTheme.colorScheme.onSurface)
                            Text("Private Van", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        viewModel.updateDriverInfo(
                            name = nameInput,
                            mobile = mobileInput,
                            vehicleNo = vehicleInput,
                            vehicleType = vehicleTypeSelected,
                            photoUri = null
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_driver_profile_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save Preference Profile", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // History logs
        Text(
            text = "Completed Trip Records",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 4.dp),
            fontSize = 15.sp
        )

        if (trips.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.History, contentDescription = null, tint = SubtleGray, modifier = Modifier.size(32.dp))
                        Text(
                            "No trips logged yet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        } else {
            trips.forEach { trip ->
                TripHistoryItem(trip)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
fun TripHistoryItem(trip: TripHistoryEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (trip.vehicleType == "Bus") Icons.Default.DirectionsBus else Icons.Default.AirportShuttle,
                        contentDescription = null,
                        tint = SchoolBusGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Trip ID #${trip.id}", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                }

                Box(
                    modifier = Modifier
                        .background(SchoolBusGold.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(text = trip.duration, color = SchoolBusGold, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Date", fontSize = 10.sp, color = SubtleGray)
                    Text(trip.date, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Column {
                    Text("Start Time", fontSize = 10.sp, color = SubtleGray)
                    Text(trip.startTime, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Column {
                    Text("End Time", fontSize = 10.sp, color = SubtleGray)
                    Text(trip.endTime, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Column {
                    Text("Vehicle No", fontSize = 10.sp, color = SubtleGray)
                    Text(trip.vehicleNumber, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}


// B. PARENT DASHBOARD SCREEN
@Composable
fun ParentDashboard(
    profile: UserProfileEntity,
    liveLocation: LiveLocationEntity?,
    viewModel: TrackingViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        DashboardHeader(
            title = "SchoolTrack",
            subtitle = "Parent Tracking Hub",
            avatarName = profile.fullName.ifEmpty { "Parent" }
        )

        Text(
            text = "Parent Dashboard",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "View student transport tracking updates",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Child's Bus Router Map", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
        SchoolBusMap(
            liveLocation = liveLocation,
            routePoints = viewModel.schoolRoute,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Profile details
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (liveLocation != null) "Assigned Driver" else "Waiting for Active Run",
                            fontSize = 11.sp,
                            color = SubtleGray
                        )
                        Text(
                            text = liveLocation?.driverName ?: "Golden Academy Hub",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                if (liveLocation?.isActive == true) GreenOnline.copy(alpha = 0.2f) else RedOffline.copy(alpha = 0.2f),
                                CircleShape
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (liveLocation?.isActive == true) "ONLINE" else "OFFLINE",
                            color = if (liveLocation?.isActive == true) GreenOnline else RedOffline,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Vehicle Number", fontSize = 11.sp, color = SubtleGray)
                        Text(liveLocation?.vehicleNumber ?: "SCH-2026-BUS", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Vehicle Type", fontSize = 11.sp, color = SubtleGray)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (liveLocation?.vehicleType == "Van") Icons.Default.AirportShuttle else Icons.Default.DirectionsBus,
                                contentDescription = null,
                                tint = SchoolBusGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(liveLocation?.vehicleType ?: "Bus", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Driver Mobile", fontSize = 11.sp, color = SubtleGray)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp), tint = SubtleGray)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                if (liveLocation != null) "+91 98765 43210" else "Unavailable",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Last Ping Updates", fontSize = 11.sp, color = SubtleGray)
                        Text(
                            text = if (liveLocation?.isActive == true) "Just now" else "Offline",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (liveLocation?.isActive == true) StatusBlue else RedOffline
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { /* Simulated Call Trigger */ },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Phone, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Call Bus Driver Sim", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}


// C. STUDENT DASHBOARD SCREEN
@Composable
fun StudentDashboard(
    profile: UserProfileEntity,
    liveLocation: LiveLocationEntity?,
    viewModel: TrackingViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        DashboardHeader(
            title = "SchoolTrack",
            subtitle = "Student Commute Hub",
            avatarName = profile.fullName.ifEmpty { "Student" }
        )

        Text(
            text = "Student Transit Hub",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Academic Commute Routes & Stations",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        SchoolBusMap(
            liveLocation = liveLocation,
            routePoints = viewModel.schoolRoute,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Your Scheduled Academic Walk",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = SchoolBusGold
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(StatusBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.School, contentDescription = null, tint = StatusBlue)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("St. Mary's Private Academy", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Arrives Daily before 09:00 AM", fontSize = 11.sp, color = SubtleGray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Selected Shuttle", fontSize = 10.sp, color = SubtleGray)
                        Text(
                            text = if (liveLocation != null) "Shuttle: ${liveLocation.vehicleType}" else "Default Bus Route",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Column {
                        Text("Plate Identifier", fontSize = 10.sp, color = SubtleGray)
                        Text(
                            text = liveLocation?.vehicleNumber ?: "SCH-2026-N",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Column {
                        Text("Next Transit Stop", fontSize = 10.sp, color = SubtleGray)
                        Text(
                            text = "Avenue Crossing 2",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = StatusBlue
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}


// D. GLOBAL SIMULATOR CONSOLE BADGE - SPLIT SCREEN RUN SIMULATOR
@Composable
fun BoxScope.SimulationConsoleFloatingBadge(
    currentSimRole: String?,
    onOverrideRole: (String?) -> Unit,
    onLogout: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(16.dp)
            .padding(top = 40.dp)
    ) {
        if (!isExpanded) {
            FloatingActionButton(
                onClick = { isExpanded = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("simulator_badge")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Open Transit Role Simulation Console",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        } else {
            Card(
                modifier = Modifier
                    .width(220.dp)
                    .animateContentSize()
                    .testTag("simulator_panel"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DeepCharcoal),
                border = BorderStroke(1.5.dp, SchoolBusGold.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Simulator Switch",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = SchoolBusGold
                        )
                        IconButton(
                            onClick = { isExpanded = false },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Instant Switching Hub (Perfect for testing!)", fontSize = 10.sp, color = SubtleGray)
                    Spacer(modifier = Modifier.height(8.dp))

                    RoleSimButton("Choose Driver Mode", currentSimRole == "DRIVER") {
                        onOverrideRole("DRIVER")
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    RoleSimButton("Choose Parent Mode", currentSimRole == "PARENT") {
                        onOverrideRole("PARENT")
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    RoleSimButton("Choose Student Mode", currentSimRole == "STUDENT") {
                        onOverrideRole("STUDENT")
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = PureWhite.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            onLogout()
                            isExpanded = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RedOffline),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Reset Simulation", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun RoleSimButton(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) SchoolBusGold else LightSlateSurface,
            contentColor = if (isSelected) SlateDarkNavy else PureWhite
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) SlateDarkNavy else PureWhite.copy(alpha = 0.3f))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
