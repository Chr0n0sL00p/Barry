package com.example.nfpet.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.InputStream

@Composable
fun PetImageRenderer(
    photoUri: String?,
    name: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Try decoding local URIs first
    if (photoUri != null && !photoUri.startsWith("mock_")) {
        val bitmap = remember(photoUri) {
            try {
                val uri = Uri.parse(photoUri)
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                BitmapFactory.decodeStream(inputStream)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Foto de $name",
                contentScale = ContentScale.Crop,
                modifier = modifier.clip(RoundedCornerShape(16.dp))
            )
            return
        }
    }

    // High-fidelity fallback for mock pets using stunning gradients and cute illustrations
    val (gradientColors, icon, typeLabel) = remember(photoUri) {
        when (photoUri) {
            "mock_golden" -> Triple(
                listOf(Color(0xFFFFE082), Color(0xFFFFB300)),
                "🦮",
                "Golden Retriever"
            )
            "mock_siamese" -> Triple(
                listOf(Color(0xFFCFD8DC), Color(0xFF546E7A)),
                "🐈",
                "Siamesa"
            )
            "mock_pug" -> Triple(
                listOf(Color(0xFFD7CCC8), Color(0xFF8D6E63)),
                "🐕",
                "Pug"
            )
            "mock_poodle" -> Triple(
                listOf(Color(0xFFF5F5F5), Color(0xFFBDBDBD)),
                "🐩",
                "Caniche"
            )
            else -> Triple(
                listOf(Color(0xFFFFAB91), Color(0xFFFF7043)),
                "🐾",
                "Mascota"
            )
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(gradientColors)),
        contentAlignment = Alignment.Center
    ) {
        // Subtle background pattern
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.15f),
                radius = size.minDimension * 0.4f,
                center = center.copy(y = center.y - 20f)
            )
        }
        
        Text(
            text = icon,
            fontSize = 54.sp,
            modifier = Modifier.align(Alignment.Center)
        )
        
        // Premium tag at the bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.4f))
                .fillMaxSize()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Text(
                text = typeLabel,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
    }
}
