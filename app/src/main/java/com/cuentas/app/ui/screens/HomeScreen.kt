package com.cuentas.app.ui.screens

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.cuentas.app.data.model.InputType
import com.cuentas.app.domain.model.ParsedExpense
import com.cuentas.app.service.LocationData
import com.cuentas.app.service.VoiceState
import com.cuentas.app.ui.components.ExpenseListItem
import com.cuentas.app.ui.components.GlassCard
import com.cuentas.app.ui.components.GradientGlassCard
import com.cuentas.app.ui.components.MicrophoneButton
import com.cuentas.app.ui.theme.GradientPurpleCyan
import com.cuentas.app.viewmodel.MainViewModel
import com.cuentas.app.viewmodel.ProcessingState
import kotlinx.coroutines.launch
import java.io.File
import java.text.NumberFormat
import java.util.Locale

enum class InputMode { NONE, KEYBOARD }

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onOpenSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val processingState by viewModel.processingState.collectAsState()
    val voiceState by viewModel.voiceState.collectAsState()
    val balance by viewModel.balance.collectAsState()
    val recentExpenses by viewModel.repository.getRecentExpenses().collectAsState(initial = emptyList())
    val totalSpent by viewModel.repository.getTotalSpent().collectAsState(initial = null)

    val isRecording = voiceState is VoiceState.Listening || voiceState is VoiceState.Processing
    var inputMode by remember { mutableStateOf(InputMode.NONE) }
    var textInput by remember { mutableStateOf(TextFieldValue("")) }
    var showCameraOptions by remember { mutableStateOf(false) }
    var showBalanceDialog by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    // Permissions
    val micPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    // Camera launchers
    var cameraImageFile by remember { mutableStateOf<File?>(null) }
    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraImageFile?.let { file ->
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                viewModel.processImageUri(uri)
            }
        }
    }
    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.processImageUri(it) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // ─── Top Bar ─────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cuentas",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Configuración",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            }

            // ─── Balance + Total Cards ────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Balance card
                GradientGlassCard(
                    modifier = Modifier.weight(1f),
                    gradient = GradientPurpleCyan
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Mi saldo",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Spacer(Modifier.weight(1f))
                            IconButton(
                                onClick = { showBalanceDialog = true },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Editar saldo",
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "S/ %.2f".format(balance),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Gastos card
                GradientGlassCard(
                    modifier = Modifier.weight(1f),
                    gradient = listOf(Color(0xFFFF6B6B), Color(0xFFFF9F43))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = Color(0xFFFF6B6B),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Gastos",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "S/ %.2f".format(totalSpent ?: 0.0),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ─── Recent expenses list ─────────────────────────────────────────
            if (voiceState is VoiceState.Processing || voiceState is VoiceState.Listening) {
                // Voice feedback area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (voiceState is VoiceState.Processing) {
                                (voiceState as? VoiceState.Processing)?.partialText?.ifBlank { "Escuchando..." }
                                    ?: "Escuchando..."
                            } else "Listo para escuchar...",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Toca el micrófono para detener",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (recentExpenses.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("💸", fontSize = 48.sp)
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        "Sin gastos aún",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                    )
                                    Text(
                                        "Usa el micrófono, cámara o teclado",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f)
                                    )
                                }
                            }
                        }
                    }
                    items(recentExpenses) { expense ->
                        ExpenseListItem(expense = expense)
                    }
                }
            }

            // ─── Keyboard input ───────────────────────────────────────────────
            AnimatedVisibility(
                visible = inputMode == InputMode.KEYBOARD,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                LaunchedEffect(Unit) { focusRequester.requestFocus() }
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    elevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { inner ->
                                if (textInput.text.isEmpty()) {
                                    Text(
                                        "Ej: helado 3 soles, pan 4...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    )
                                }
                                inner()
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    CircleShape
                                )
                                .clickable {
                                    if (textInput.text.isNotBlank()) {
                                        viewModel.processTextInput(textInput.text, InputType.TEXT)
                                        textInput = TextFieldValue("")
                                        inputMode = InputMode.NONE
                                    }
                                }
                        ) {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = "Enviar",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // ─── Input buttons ────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp, top = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Camera button
                AnimatedVisibility(
                    visible = !isRecording,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Box {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(52.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    CircleShape
                                )
                                .clip(CircleShape)
                                .clickable { showCameraOptions = true }
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Cámara",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showCameraOptions,
                            onDismissRequest = { showCameraOptions = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Tomar foto") },
                                leadingIcon = { Icon(Icons.Default.CameraAlt, null) },
                                onClick = {
                                    showCameraOptions = false
                                    if (cameraPermission.status.isGranted) {
                                        val file = File(context.cacheDir, "photo_${System.currentTimeMillis()}.jpg")
                                        cameraImageFile = file
                                        val uri = FileProvider.getUriForFile(
                                            context, "${context.packageName}.fileprovider", file
                                        )
                                        takePictureLauncher.launch(uri)
                                    } else {
                                        cameraPermission.launchPermissionRequest()
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Subir imagen") },
                                leadingIcon = { Icon(Icons.Default.Image, null) },
                                onClick = {
                                    showCameraOptions = false
                                    pickImageLauncher.launch("image/*")
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.width(if (isRecording) 0.dp else 24.dp))

                // Mic button (center, main)
                MicrophoneButton(
                    isRecording = isRecording,
                    onToggle = {
                        if (!micPermission.status.isGranted) {
                            micPermission.launchPermissionRequest()
                        } else {
                            if (locationPermission.status.isGranted.not()) {
                                locationPermission.launchPermissionRequest()
                            }
                            inputMode = InputMode.NONE
                            viewModel.toggleVoice()
                        }
                    },
                    size = 72.dp
                )

                Spacer(Modifier.width(if (isRecording) 0.dp else 24.dp))

                // Keyboard button
                AnimatedVisibility(
                    visible = !isRecording,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                if (inputMode == InputMode.KEYBOARD)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                CircleShape
                            )
                            .clip(CircleShape)
                            .clickable {
                                inputMode = if (inputMode == InputMode.KEYBOARD) InputMode.NONE else InputMode.KEYBOARD
                            }
                    ) {
                        Icon(
                            Icons.Default.Keyboard,
                            contentDescription = "Teclado",
                            tint = if (inputMode == InputMode.KEYBOARD)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // ─── Loading overlay ──────────────────────────────────────────────────
        AnimatedVisibility(
            visible = processingState is ProcessingState.Loading,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                GlassCard(modifier = Modifier.padding(32.dp)) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Analizando con IA...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // ─── Confirm Expenses Dialog ──────────────────────────────────────────
        val confirmState = processingState as? ProcessingState.ConfirmExpenses
        if (confirmState != null) {
            ConfirmExpensesDialog(
                expenses = confirmState.expenses,
                clarification = confirmState.clarification,
                onConfirm = { finalExpenses ->
                    viewModel.confirmExpenses(
                        finalExpenses,
                        confirmState.location,
                        confirmState.imageUri,
                        confirmState.inputType,
                        confirmState.rawInput
                    )
                },
                onDismiss = { viewModel.dismissProcessingState() }
            )
        }

        // ─── Error ────────────────────────────────────────────────────────────
        val errorState = processingState as? ProcessingState.Error
        if (errorState != null) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissProcessingState() },
                title = { Text("Error") },
                text = { Text(errorState.message) },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissProcessingState() }) {
                        Text("Entendido")
                    }
                }
            )
        }
    }

    // Balance edit dialog
    if (showBalanceDialog) {
        var balanceText by remember { mutableStateOf("%.2f".format(balance)) }
        AlertDialog(
            onDismissRequest = { showBalanceDialog = false },
            title = { Text("Editar saldo") },
            text = {
                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it },
                    label = { Text("Saldo (S/)") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    balanceText.toFloatOrNull()?.let { viewModel.setBalance(it) }
                    showBalanceDialog = false
                }) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBalanceDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

// ─── Confirm Expenses Dialog ──────────────────────────────────────────────────

@Composable
fun ConfirmExpensesDialog(
    expenses: List<ParsedExpense>,
    clarification: String?,
    onConfirm: (List<ParsedExpense>) -> Unit,
    onDismiss: () -> Unit
) {
    val editableExpenses = remember(expenses) { expenses.map { mutableStateOf(it) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (expenses.size == 1) "Confirmar gasto" else "Confirmar ${expenses.size} gastos",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!clarification.isNullOrBlank()) {
                    GlassCard(
                        shape = RoundedCornerShape(12.dp),
                        accentColor = MaterialTheme.colorScheme.tertiary
                    ) {
                        Text(
                            text = "❓ $clarification",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                editableExpenses.forEach { state ->
                    val exp = state.value
                    GlassCard(shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(exp.description, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("S/ %.2f".format(exp.amount), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            if (exp.storeName.isNotBlank()) Text("Tienda: ${exp.storeName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Text("Categoría: ${exp.category}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(editableExpenses.map { it.value }) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
