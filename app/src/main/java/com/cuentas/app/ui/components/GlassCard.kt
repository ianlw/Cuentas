package com.cuentas.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cuentas.app.ui.theme.LocalGlass

/**
 * Componente de "Liquid Glass" que implementa el efecto de glassmorphism:
 * - Fondo semi-transparente con gradiente
 * - Borde con gradiente luminoso
 * - Sombra coloreada
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    elevation: Dp = 8.dp,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable BoxScope.() -> Unit
) {
    val glass = LocalGlass.current

    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = accentColor.copy(alpha = 0.2f),
                spotColor = accentColor.copy(alpha = 0.15f)
            )
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        glass.highlight,
                        glass.background
                    )
                ),
                shape = shape
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        glass.border,
                        glass.border.copy(alpha = 0.1f)
                    )
                ),
                shape = shape
            ),
        content = content
    )
}

/** Versión con gradiente de acento visible (para cards de estadísticas) */
@Composable
fun GradientGlassCard(
    modifier: Modifier = Modifier,
    gradient: List<Color>,
    shape: Shape = RoundedCornerShape(24.dp),
    elevation: Dp = 12.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val glass = LocalGlass.current

    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = gradient.first().copy(alpha = 0.3f),
                spotColor = gradient.last().copy(alpha = 0.2f)
            )
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        gradient.first().copy(alpha = 0.25f),
                        gradient.last().copy(alpha = 0.15f)
                    )
                ),
                shape = shape
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        gradient.first().copy(alpha = 0.5f),
                        gradient.last().copy(alpha = 0.2f)
                    )
                ),
                shape = shape
            ),
        content = content
    )
}
