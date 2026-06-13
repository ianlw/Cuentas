package com.cuentas.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cuentas.app.ui.screens.DashboardScreen
import com.cuentas.app.ui.screens.HistoryScreen
import com.cuentas.app.ui.screens.HomeScreen
import com.cuentas.app.ui.theme.CuentasTheme
import com.cuentas.app.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: MainViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsState()
            val customColors by viewModel.customColors.collectAsState()

            CuentasTheme(themeMode = themeMode, customColors = customColors) {
                // Pager con 3 páginas. Página inicial es la 1 (Home).
                val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> HistoryScreen(viewModel)
                        1 -> HomeScreen(viewModel)
                        2 -> DashboardScreen(viewModel)
                    }
                }
            }
        }
    }
}
