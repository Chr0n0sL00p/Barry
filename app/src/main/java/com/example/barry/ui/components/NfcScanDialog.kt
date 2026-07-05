package com.example.barry.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.barry.data.model.PetReport
import com.example.barry.ui.BarryViewModel
import com.example.barry.ui.theme.BrandCoral
import com.example.barry.ui.theme.StatusFound
import com.example.barry.ui.theme.StatusLost
import com.example.barry.ui.theme.Typography

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun NfcScanDialog(
    viewModel: BarryViewModel,
    modifier: Modifier = Modifier
) {
    val showDialog by viewModel.showNfcDialog.collectAsState()
    val scannedPet by viewModel.nfcScannedPet.collectAsState()
    val userLocation by viewModel.userLocation.collectAsState()

    if (!showDialog) return

    val context = LocalContext.current

    // Dialog layout with dark overlay background
    Dialog(
        onDismissRequest = { viewModel.closeNfcDialog() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .clickable { viewModel.closeNfcDialog() },
            contentAlignment = Alignment.Center
        ) {
            // Main dialog card container (prevents clicks from dismissing)
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .shadow(16.dp, RoundedCornerShape(28.dp))
                    .clickable(enabled = false) {}
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Close button at top-right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = { viewModel.closeNfcDialog() },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), CircleShape)
                                .size(32.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    AnimatedContent(
                        targetState = scannedPet != null,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
                        }
                    ) { isMatched ->
                        if (!isMatched) {
                            // --- STATE 1: WAITING FOR SCAN (Radar animation) ---
                            NfcWaitingScanView(
                                onSimulateScan = { petId -> viewModel.onNfcTagScanned(petId) }
                            )
                        } else {
                            // --- STATE 2: SCAN SUCCESSFUL (Pet details profile card) ---
                            scannedPet?.let { pet ->
                                NfcScanSuccessView(
                                    pet = pet,
                                    currentLatitude = userLocation?.latitude,
                                    currentLongitude = userLocation?.longitude,
                                    onClose = { viewModel.closeNfcDialog() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NfcWaitingScanView(
    onSimulateScan: (String) -> Unit
) {
    // Pulse animation state
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "🐾 Lector de Collar NFC",
            style = Typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Aproxima el collar NFC de la mascota al reverso de tu teléfono para leer su ID de rescate instantáneo.",
            style = Typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Pulse Radar graphics
        Box(
            modifier = Modifier.size(160.dp),
            contentAlignment = Alignment.Center
        ) {
            // Concentric pulsing rings
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = BrandCoral.copy(alpha = pulseAlpha),
                    radius = (size.minDimension / 2f) * pulseScale
                )
                drawCircle(
                    color = BrandCoral.copy(alpha = (pulseAlpha + 0.3f).coerceAtMost(0.4f) * (1f - pulseScale / 2f)),
                    radius = (size.minDimension / 2.5f) * (pulseScale * 0.8f).coerceAtLeast(0.4f)
                )
            }

            // Central icon bubble
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(BrandCoral, BrandCoral.copy(alpha = 0.8f))))
                    .shadow(4.dp, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Nfc,
                    contentDescription = "Aproximar NFC",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Simulation shortcuts helper (specifically for emulators!)
        Text(
            text = "🔌 Emulador / Simulación rápida:",
            style = Typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onSimulateScan("Max") },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Max 🦮", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Button(
                onClick = { onSimulateScan("Luna") },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Luna 🐈", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun NfcScanSuccessView(
    pet: PetReport,
    currentLatitude: Double?,
    currentLongitude: Double?,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Pulse Success Glow Icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(StatusFound.copy(alpha = 0.15f))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusFound, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "MASCOTA IDENTIFICADA CON ÉXITO",
                color = StatusFound,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Pet Image Avatar
        PetImageRenderer(
            photoUri = pet.photoUri,
            name = pet.name,
            modifier = Modifier
                .size(110.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(2.dp, StatusFound, RoundedCornerShape(20.dp))
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = pet.name,
            style = Typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(2.dp))

        val badgeColor = if (pet.status == "LOST") StatusLost else StatusFound
        val badgeLabel = if (pet.status == "LOST") "ALERTA: SE BUSCA URGENTEMENTE 🚨" else "REGISTRADO COMO HALLADO 💚"
        Text(
            text = badgeLabel,
            color = badgeColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = pet.description,
            style = Typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Owner Detail Area
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Dueño / Contacto de Rescate",
                    style = Typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${pet.reporterName} (${pet.phoneNumber})",
                    style = Typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Primary Call to Action buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Call Owner Button
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:${pet.phoneNumber}")
                    }
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Default.Phone, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Llamar al Dueño", color = Color.White, fontWeight = FontWeight.Bold)
            }

            // Share current location via SMS button (Google Maps integration!)
            Button(
                onClick = {
                    val lat = currentLatitude ?: -33.4489
                    val lng = currentLongitude ?: -70.6693
                    
                    val mapsLink = "https://maps.google.com/?q=$lat,$lng"
                    val message = "¡Hola! He encontrado a tu mascota **${pet.name}** utilizando el collar NFC y la app Barry. Mi ubicación de encuentro actual es: $mapsLink"
                    
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("smsto:${pet.phoneNumber}")
                        putExtra("sms_body", message)
                    }
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Default.ShareLocation, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Compartir mi Ubicación", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(6.dp))

            TextButton(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cerrar Ficha", fontWeight = FontWeight.Bold)
            }
        }
    }
}
