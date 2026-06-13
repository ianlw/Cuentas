package com.cuentas.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cuentas.app.data.db.CategorySummary
import com.cuentas.app.data.db.StoreSummary
import com.cuentas.app.ui.components.BarChart
import com.cuentas.app.ui.components.BarEntry
import com.cuentas.app.ui.components.ChartLegendItem
import com.cuentas.app.ui.components.DonutChart
import com.cuentas.app.ui.components.DonutSlice
import com.cuentas.app.ui.components.GlassCard
import com.cuentas.app.ui.components.GradientGlassCard
import com.cuentas.app.ui.theme.GradientPurpleCyan
import com.cuentas.app.ui.theme.categoryColor
import com.cuentas.app.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class DashboardPeriod(val label: String) {
    WEEK("Semana"), MONTH("Mes"), ALL("Total")
}

@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    var selectedPeriod by remember { mutableStateOf(DashboardPeriod.MONTH) }

    val repo = viewModel.repository
    val (startTime, endTime) = when (selectedPeriod) {
        DashboardPeriod.WEEK  -> repo.getLastNDaysRange(7)
        DashboardPeriod.MONTH -> repo.getCurrentMonthRange()
        DashboardPeriod.ALL   -> Pair(0L, System.currentTimeMillis())
    }

    val categoryData by repo.getCategorySummary(startTime, endTime).collectAsState(initial = emptyList())
    val topStores by repo.getTopStores(startTime, endTime).collectAsState(initial = emptyList())
    val totalInPeriod by repo.getTotalByDateRange(startTime, endTime).collectAsState(initial = null)

    // Build daily bar data (last 7 days)
    val allExpenses by repo.getFilteredExpenses(
        repo.getLastNDaysRange(7).first, System.currentTimeMillis()
    ).collectAsState(initial = emptyList())

    val barEntries = remember(allExpenses) {
        val sdf = SimpleDateFormat("EEE", Locale("es", "PE"))
        val grouped = allExpenses.groupBy { expense ->
            val cal = Calendar.getInstance().apply { timeInMillis = expense.timestamp }
            cal.get(Calendar.DAY_OF_YEAR)
        }
        (6 downTo 0).map { daysAgo ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
            val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
            val label = if (daysAgo == 0) "Hoy" else sdf.format(cal.time).replaceFirstChar { it.uppercase() }
            val total = grouped[dayOfYear]?.sumOf { it.amount }?.toFloat() ?: 0f
            BarEntry(label, total)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            Text(
                text = "Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Period selector
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DashboardPeriod.entries.forEach { period ->
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = { selectedPeriod = period },
                        label = { Text(period.label) }
                    )
                }
            }

            // Total summary card
            GradientGlassCard(gradient = GradientPurpleCyan, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Total gastado",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        "S/ %.2f".format(totalInPeriod ?: 0.0),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        when (selectedPeriod) {
                            DashboardPeriod.WEEK  -> "Esta semana"
                            DashboardPeriod.MONTH -> "Este mes"
                            DashboardPeriod.ALL   -> "Todos los tiempos"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            // Donut chart by category
            if (categoryData.isNotEmpty()) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "Por categoría",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val slices = categoryData.map { cs ->
                                DonutSlice(cs.category, cs.total.toFloat(), categoryColor(cs.category))
                            }
                            DonutChart(
                                slices = slices,
                                modifier = Modifier.size(140.dp),
                                centerLabel = "S/ %.0f".format(totalInPeriod ?: 0.0)
                            )
                            Spacer(Modifier.width(16.dp))
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                categoryData.take(6).forEach { cs ->
                                    ChartLegendItem(
                                        color = categoryColor(cs.category),
                                        label = cs.category,
                                        value = "S/ %.2f".format(cs.total)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bar chart — gastos por día
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Últimos 7 días",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(16.dp))
                    BarChart(
                        entries = barEntries,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    )
                }
            }

            // Top stores
            if (topStores.isNotEmpty()) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "Top tiendas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(12.dp))
                        topStores.forEachIndexed { idx, store ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${idx + 1}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(24.dp)
                                )
                                Text(
                                    store.storeName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    "S/ %.2f".format(store.total),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
