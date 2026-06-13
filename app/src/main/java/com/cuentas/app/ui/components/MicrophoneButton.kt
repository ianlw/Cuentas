package com.cuentas.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun MicrophoneButton(
    isRecording: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 68.dp
) {
    val transition = updateTransition(targetState = isRecording, label = "mic_transition")

    val scale by transition.animateFloat(
        transitionSpec = { spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow) },
        label = "scale"
    ) { rec -> if (rec) 1.2f else 1.0f }

    // Ripple animation when recording
    val infiniteTransition = rememberInfiniteTransition(label = "ripple")

    val ripple1Scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple1Scale"
    )
    val ripple1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple1Alpha"
    )

    val ripple2Scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, 300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple2Scale"
    )
    val ripple2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, 300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple2Alpha"
    )

    // Pulse glow
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val primary = MaterialTheme.colorScheme.primary

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        // Ripple rings (only when recording)
        if (isRecording) {
            Box(
                modifier = Modifier
                    .size(size * ripple1Scale)
                    .alpha(ripple1Alpha)
                    .background(primary.copy(alpha = 0.15f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(size * ripple2Scale)
                    .alpha(ripple2Alpha)
                    .background(primary.copy(alpha = 0.2f), CircleShape)
            )
            // Inner glow ring
            Box(
                modifier = Modifier
                    .size(size * 1.35f)
                    .alpha(glowAlpha)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(primary.copy(alpha = 0.4f), Color.Transparent)
                        ),
                        CircleShape
                    )
            )
        }

        // Main button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size)
                .scale(scale)
                .background(
                    brush = if (isRecording) {
                        Brush.radialGradient(
                            colors = listOf(Color(0xFFFF4D6D), Color(0xFFB5002B))
                        )
                    } else {
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    },
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onToggle() }
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = if (isRecording) "Detener grabación" else "Grabar",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
