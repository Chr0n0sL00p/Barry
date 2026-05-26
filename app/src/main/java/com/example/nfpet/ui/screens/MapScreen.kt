package com.example.nfpet.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nfpet.data.model.PetReport
import com.example.nfpet.ui.NFPetViewModel
import com.example.nfpet.ui.components.PetImageRenderer
import com.example.nfpet.ui.theme.StatusFound
import com.example.nfpet.ui.theme.StatusLost
import com.example.nfpet.ui.theme.Typography
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MapScreen(
    viewModel: NFPetViewModel,
    selectedPetFromFeed: PetReport? = null,
    modifier: Modifier = Modifier
) {
    val reports by viewModel.reports.collectAsState()
    val userLocation by viewModel.userLocation.collectAsState()
    val currentCity by viewModel.currentCity.collectAsState()

    var selectedPet by remember { mutableStateOf<PetReport?>(null) }
    
    // Track selected pet from feed navigation
    LaunchedEffect(selectedPetFromFeed) {
        if (selectedPetFromFeed != null) {
            selectedPet = selectedPetFromFeed
        }
    }

    // State for interactive map simulation: pan offsets and zoom
    var mapOffset by remember { mutableStateOf(Offset.Zero) }
    var zoomLevel by remember { mutableStateOf(1f) }

    val context = LocalContext.current

    Scaffold(
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // --- High-Fidelity Custom Radar/Map Canvas ---
            // Captures drag gestures for panning around the city
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFE8F5E9)) // Soft landscape green background
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            mapOffset = Offset(
                                x = mapOffset.x + dragAmount.x,
                                y = mapOffset.y + dragAmount.y
                            )
                        }
                    }
            ) {
                // Background grid and concentric radar circles to show premium radar scanning
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val center = Offset(canvasWidth / 2f + mapOffset.x, canvasHeight / 2f + mapOffset.y)

                    // Draw grid lines
                    val gridSize = 80f * zoomLevel
                    val startX = (mapOffset.x % gridSize) - gridSize
                    val startY = (mapOffset.y % gridSize) - gridSize

                    var x = startX
                    while (x < canvasWidth + gridSize) {
                        drawLine(
                            color = Color(0xFFC8E6C9),
                            start = Offset(x, 0f),
                            end = Offset(x, canvasHeight),
                            strokeWidth = 1f
                        )
                        x += gridSize
                    }

                    var y = startY
                    while (y < canvasHeight + gridSize) {
                        drawLine(
                            color = Color(0xFFC8E6C9),
                            start = Offset(0f, y),
                            end = Offset(canvasWidth, y),
                            strokeWidth = 1f
                        )
                        y += gridSize
                    }

                    // Draw scan circles centered on the user
                    drawCircle(
                        color = Color(0xFF4CAF50).copy(alpha = 0.04f),
                        radius = 200f * zoomLevel,
                        center = center
                    )
                    drawCircle(
                        color = Color(0xFF4CAF50).copy(alpha = 0.08f),
                        radius = 400f * zoomLevel,
                        center = center
                    )
                    drawCircle(
                        color = Color(0xFF4CAF50).copy(alpha = 0.02f),
                        radius = 600f * zoomLevel,
                        center = center
                    )
                }

                // User Location (Center Glow dot) using BoxWithConstraints for rock-solid sizing
                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val canvasWidth = constraints.maxWidth.toFloat()
                    val canvasHeight = constraints.maxHeight.toFloat()
                    
                    val centerOffset = Offset(
                        x = canvasWidth / 2f + mapOffset.x,
                        y = canvasHeight / 2f + mapOffset.y
                    )

                    // User Pin Glow Effect
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    x = (centerOffset.x - 12.dp.toPx()).roundToInt(),
                                    y = (centerOffset.y - 12.dp.toPx()).roundToInt()
                                )
                            }
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }

                    // Lost Pets Pins (positioned relative to user coordinates)
                    val baseLat = userLocation?.latitude ?: -33.4489
                    val baseLng = userLocation?.longitude ?: -70.6693

                    reports.forEach { report ->
                        // Calculate offset from base user coordinate (1 degree is approx 111km)
                        // Scaling factor for display
                        val scale = 30000f * zoomLevel
                        val dx = (report.longitude - baseLng) * scale
                        val dy = -(report.latitude - baseLat) * scale // Y is inverted in screen coords

                        val pinOffset = Offset(
                            x = centerOffset.x + dx.toFloat(),
                            y = centerOffset.y + dy.toFloat()
                        )

                        // Render pin if it's on screen
                        if (pinOffset.x >= 0 && pinOffset.x <= canvasWidth &&
                            pinOffset.y >= 0 && pinOffset.y <= canvasHeight
                        ) {
                            val isSelected = selectedPet?.id == report.id
                            val pinColor = if (report.status == "LOST") StatusLost else StatusFound
                            val sizeMultiplier = if (isSelected) 1.25f else 1f

                            Box(
                                modifier = Modifier
                                    .offset {
                                        IntOffset(
                                            x = (pinOffset.x - (20.dp.toPx() * sizeMultiplier)).roundToInt(),
                                            y = (pinOffset.y - (20.dp.toPx() * sizeMultiplier)).roundToInt()
                                        )
                                    }
                                    .size(40.dp * sizeMultiplier)
                                    .shadow(6.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(2.dp, pinColor, CircleShape)
                                    .clickable { selectedPet = report },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (report.status == "LOST") "🚨" else "🐾",
                                    fontSize = (16.sp * sizeMultiplier)
                                )
                            }
                        }
                    }
                }
            }

            // Top Status Panel (Header)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.5f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Radar de Búsqueda Activo",
                                style = Typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Mostrando mascotas perdidas en $currentCity",
                                style = Typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Floating Map Controls (Right Side)
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Recenter GPS Button
                IconButton(
                    onClick = {
                        mapOffset = Offset.Zero
                        viewModel.detectLocation()
                    },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .shadow(4.dp, CircleShape)
                        .size(54.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationSearching,
                        contentDescription = "Recalibrar GPS",
                        tint = Color.White
                    )
                }

                // Zoom In
                IconButton(
                    onClick = { if (zoomLevel < 3f) zoomLevel += 0.25f },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                        .shadow(3.dp, CircleShape)
                        .size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Zoom In",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Zoom Out
                IconButton(
                    onClick = { if (zoomLevel > 0.5f) zoomLevel -= 0.25f },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                        .shadow(3.dp, CircleShape)
                        .size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Zoom Out",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Pet Detail Sliding Bottom Card
            AnimatedVisibility(
                visible = selectedPet != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                selectedPet?.let { pet ->
                    val formattedDate = remember(pet.timestamp) {
                        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                        sdf.format(Date(pet.timestamp))
                    }

                    Card(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(12.dp, RoundedCornerShape(28.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Drag handle
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(bottom = 12.dp)
                                    .size(width = 36.dp, height = 4.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PetImageRenderer(
                                    photoUri = pet.photoUri,
                                    name = pet.name,
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                )

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        val badgeColor = if (pet.status == "LOST") StatusLost else StatusFound
                                        val badgeLabel = if (pet.status == "LOST") "PERDIDO 🚨" else "ENCONTRADO 💚"
                                        
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(badgeColor.copy(alpha = 0.15f))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = badgeLabel,
                                                color = badgeColor,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }

                                        IconButton(
                                            onClick = { selectedPet = null },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Cerrar",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = pet.name,
                                        style = Typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = "Perdido el $formattedDate en ${pet.city}",
                                        style = Typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = pet.description,
                                style = Typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Contact info & call button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Reportado por:",
                                        style = Typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = pet.reporterName,
                                        style = Typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Button(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL).apply {
                                            data = Uri.parse("tel:${pet.phoneNumber}")
                                        }
                                        context.startActivity(intent)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.height(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = "Llamar",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Llamar",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
