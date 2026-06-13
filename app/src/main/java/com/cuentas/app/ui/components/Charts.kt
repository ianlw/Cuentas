package com.cuentas.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ──────────────────────────────────────────────────────────────────────────────
// Donut Chart
// ──────────────────────────────────────────────────────────────────────────────

data class DonutSlice(val label: String, val value: Float, val color: Color)

@Composable
fun DonutChart(
    slices: List<DonutSlice>,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 28.dp,
    centerLabel: String = ""
) {
    var animated by remember { mutableStateOf(false) }
    LaunchedEffect(slices) { animated = true }

    val totalValue = slices.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(0.001f)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sw = strokeWidth.toPx()
            val diameter = size.minDimension - sw
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)

            var startAngle = -90f

            slices.forEach { slice ->
                val sweepAngle = (slice.value / totalValue) * 360f
                // Background track
                drawArc(
                    color = slice.color.copy(alpha = 0.15f),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle - 2f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = sw, cap = StrokeCap.Round)
                )
                // Colored arc
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(slice.color, slice.color.copy(alpha = 0.7f)),
                        center = Offset(size.width / 2f, size.height / 2f)
                    ),
                    startAngle = startAngle,
                    sweepAngle = (sweepAngle - 3f).coerceAtLeast(0f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = sw, cap = StrokeCap.Round)
                )
                startAngle += sweepAngle
            }
        }

        // Center text
        if (centerLabel.isNotEmpty()) {
            Text(
                text = centerLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Bar Chart
// ──────────────────────────────────────────────────────────────────────────────

data class BarEntry(val label: String, val value: Float)

@Composable
fun BarChart(
    entries: List<BarEntry>,
    modifier: Modifier = Modifier,
    barColor: List<Color> = listOf(Color(0xFF9D5CFF), Color(0xFF22D3EE)),
    maxValue: Float = entries.maxOfOrNull { it.value }?.coerceAtLeast(1f) ?: 1f
) {
    var animated by remember { mutableStateOf(false) }
    LaunchedEffect(entries) { animated = true }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        entries.forEach { entry ->
            val fraction = (entry.value / maxValue).coerceIn(0f, 1f)
            val animatedFraction by animateFloatAsState(
                targetValue = if (animated) fraction else 0f,
                animationSpec = tween(800),
                label = "bar"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f)
            ) {
                if (entry.value > 0f) {
                    Text(
                        text = "S/%.0f".format(entry.value),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontSize = 9.sp
                    )
                }
                Spacer(Modifier.height(2.dp))
                Canvas(
                    modifier = Modifier
                        .height(120.dp * animatedFraction + 4.dp)
                        .fillMaxWidth(0.65f)
                ) {
                    val radius = size.width / 2f
                    drawRoundRect(
                        brush = Brush.verticalGradient(barColor),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = entry.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 9.sp
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Legend item
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun ChartLegendItem(
    color: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Canvas(modifier = Modifier.size(10.dp)) {
            drawCircle(color = color)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
