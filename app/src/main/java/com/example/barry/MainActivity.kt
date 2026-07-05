package com.example.barry

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.barry.data.model.PetReport
import com.example.barry.nfc.NfcParser
import com.example.barry.ui.BarryViewModel
import com.example.barry.ui.components.NfcScanDialog
import com.example.barry.ui.screens.FeedScreen
import com.example.barry.ui.screens.MapScreen
import com.example.barry.ui.screens.ReportScreen
import com.example.barry.ui.screens.SimulatorScreen
import com.example.barry.ui.theme.BarryTheme

class MainActivity : ComponentActivity() {
    private val viewModel: BarryViewModel by viewModels()
    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize NFC Adapter
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        val intent = Intent(this, javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        enableEdgeToEdge()
        setContent {
            BarryTheme {
                var showSplash by rememberSaveable { mutableStateOf(true) }
                
                if (showSplash) {
                    SplashScreen(onLoadingComplete = { showSplash = false })
                } else {
                    BarryApp(viewModel = viewModel)
                }
            }
        }

        // Process starting NFC intent if the app was launched by an NFC tag scan
        processNfcIntent(getIntent())
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        processNfcIntent(intent)
    }

    private fun processNfcIntent(intent: Intent) {
        val petId = NfcParser.parseNdefIntent(intent)
        if (petId != null) {
            viewModel.onNfcTagScanned(petId)
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun BarryApp(viewModel: BarryViewModel) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.FEED) }
    
    // Hold a reference to a pet that was clicked in the feed to show it in the map
    var selectedPetForMap by remember { mutableStateOf<PetReport?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                AppDestinations.entries.forEach { dest ->
                    val isSelected = currentDestination == dest
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = dest.icon,
                                contentDescription = dest.label,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        label = {
                            Text(
                                text = dest.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                            )
                        },
                        selected = isSelected,
                        onClick = {
                            currentDestination = dest
                            // Clear selected pet when navigating normally
                            if (dest != AppDestinations.MAP) {
                                selectedPetForMap = null
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        // Animated Crossfade transition between screens for a premium feel
        AnimatedContent(
            targetState = currentDestination,
            transitionSpec = {
                fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) togetherWith
                        fadeOut(animationSpec = androidx.compose.animation.core.tween(300))
            },
            modifier = Modifier.padding(innerPadding)
        ) { targetDest ->
            when (targetDest) {
                AppDestinations.FEED -> {
                    FeedScreen(
                        viewModel = viewModel,
                        onNavigateToMap = { pet ->
                            selectedPetForMap = pet
                            currentDestination = AppDestinations.MAP
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                AppDestinations.MAP -> {
                    MapScreen(
                        viewModel = viewModel,
                        selectedPetFromFeed = selectedPetForMap,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                AppDestinations.REPORT -> {
                    ReportScreen(
                        viewModel = viewModel,
                        onReportSubmitted = {
                            currentDestination = AppDestinations.FEED
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                AppDestinations.SIMULATOR -> {
                    SimulatorScreen(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
        
        // NFC Scan overlay Dialog (automatically shows itself based on viewmodel state)
        NfcScanDialog(viewModel = viewModel)
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    FEED("Avisos", Icons.Default.Pets),
    MAP("Mapa", Icons.Default.Map),
    REPORT("Reportar", Icons.Default.AddCircle),
    SIMULATOR("Simulador", Icons.Default.Build),
}

@Composable
fun SplashScreen(onLoadingComplete: () -> Unit) {
    var progress by remember { mutableStateOf(0.0f) }
    
    // Animate progress from 0.0 to 1.0 over 2.5 seconds for interactive loading feel
    LaunchedEffect(Unit) {
        val duration = 2500L
        val step = 25L
        val increments = duration / step
        for (i in 1..increments) {
            kotlinx.coroutines.delay(step)
            progress = i.toFloat() / increments
        }
        onLoadingComplete()
    }
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Fullscreen Splash background (Barry cover image)
        Image(
            painter = painterResource(id = R.drawable.splash_bg),
            contentDescription = "Pantalla de carga",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        // Interactive linear progress bar positioned near the bottom to fit the image layout
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp) // Perfect padding offset for portrait progress alignment
                .width(260.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = Color(0xFF81C784), // Matching brand green
                trackColor = Color(0xFF81C784).copy(alpha = 0.2f)
            )
            
            Spacer(modifier = Modifier.height(14.dp))
            
            Text(
                text = "Buscando a Barry... ${(progress * 100).toInt()}%",
                color = Color(0xFF556B2F), // Dark olive green text
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}