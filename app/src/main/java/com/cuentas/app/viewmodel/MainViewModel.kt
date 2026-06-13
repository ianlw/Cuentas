package com.cuentas.app.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cuentas.app.data.model.Expense
import com.cuentas.app.data.model.InputType
import com.cuentas.app.data.preferences.ThemeMode
import com.cuentas.app.data.preferences.ThemePreferences
import com.cuentas.app.data.preferences.dataStore
import com.cuentas.app.data.repository.ExpenseRepository
import com.cuentas.app.domain.model.GeminiExpenseResponse
import com.cuentas.app.domain.model.ParsedExpense
import com.cuentas.app.service.GeminiService
import com.cuentas.app.service.LocationData
import com.cuentas.app.service.LocationService
import com.cuentas.app.service.VoiceService
import com.cuentas.app.service.VoiceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

/** Estado de procesamiento de la entrada */
sealed class ProcessingState {
    object Idle : ProcessingState()
    object Loading : ProcessingState()
    data class ConfirmExpenses(
        val expenses: List<ParsedExpense>,
        val clarification: String?,
        val location: LocationData?,
        val imageUri: String?,
        val inputType: InputType,
        val rawInput: String
    ) : ProcessingState()
    data class Success(val count: Int) : ProcessingState()
    data class Error(val message: String) : ProcessingState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    // Services
    val voiceService = VoiceService(context)
    private val geminiService = GeminiService()
    private val locationService = LocationService(context)

    // Preferences
    private val themePrefs = ThemePreferences(context.dataStore)

    // Repository
    val repository: ExpenseRepository by lazy {
        val db = com.cuentas.app.data.db.AppDatabase.getInstance(context)
        ExpenseRepository(db.expenseDao())
    }

    // ─── State flows ──────────────────────────────────────────────────────────

    private val _processingState = MutableStateFlow<ProcessingState>(ProcessingState.Idle)
    val processingState: StateFlow<ProcessingState> = _processingState.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.DARK)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _customColors = MutableStateFlow(Triple(0xFF7C3AED.toInt(), 0xFF06B6D4.toInt(), 0xFFF59E0B.toInt()))
    val customColors: StateFlow<Triple<Int, Int, Int>> = _customColors.asStateFlow()

    private val _balance = MutableStateFlow(0f)
    val balance: StateFlow<Float> = _balance.asStateFlow()

    val voiceState: StateFlow<VoiceState> = voiceService.state

    init {
        // Cargar preferencias guardadas
        viewModelScope.launch {
            themePrefs.themeMode.collectLatest { _themeMode.value = it }
        }
        viewModelScope.launch {
            themePrefs.balance.collectLatest { _balance.value = it }
        }
        viewModelScope.launch {
            themePrefs.customColors.collectLatest { _customColors.value = it }
        }
        // Reaccionar a resultados de voz
        viewModelScope.launch {
            voiceService.state.collectLatest { state ->
                if (state is VoiceState.Result) {
                    processTextInput(state.text, InputType.VOICE)
                }
            }
        }
    }

    // ─── Voz ──────────────────────────────────────────────────────────────────

    fun toggleVoice() {
        when (voiceState.value) {
            is VoiceState.Listening,
            is VoiceState.Processing -> voiceService.stopListening()
            else -> voiceService.startListening()
        }
    }

    // ─── Texto ────────────────────────────────────────────────────────────────

    fun processTextInput(text: String, inputType: InputType = InputType.TEXT) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _processingState.value = ProcessingState.Loading
            val location = tryGetLocation()
            val response = geminiService.parseExpenseFromText(text)
            handleGeminiResponse(response, inputType, text, location, null)
        }
    }

    // ─── Imagen ───────────────────────────────────────────────────────────────

    fun processImageUri(uri: Uri) {
        viewModelScope.launch {
            _processingState.value = ProcessingState.Loading
            try {
                val location = tryGetLocation()
                val tmpFile = copyUriToTempFile(uri)
                val response = geminiService.parseExpenseFromImage(tmpFile)
                handleGeminiResponse(response, InputType.CAMERA, "", location, uri.toString())
                tmpFile.delete()
            } catch (e: Exception) {
                _processingState.value = ProcessingState.Error("No se pudo procesar la imagen: ${e.message}")
            }
        }
    }

    private fun copyUriToTempFile(uri: Uri): File {
        val tmp = File(context.cacheDir, "img_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tmp).use { output -> input.copyTo(output) }
        }
        return tmp
    }

    // ─── Manejo de respuesta Gemini ───────────────────────────────────────────

    private fun handleGeminiResponse(
        response: GeminiExpenseResponse,
        inputType: InputType,
        rawInput: String,
        location: LocationData?,
        imageUri: String?
    ) {
        if (response.error != null) {
            _processingState.value = ProcessingState.Error(response.error)
            return
        }
        if (response.expenses.isEmpty()) {
            _processingState.value = ProcessingState.Error("No pude identificar ningún gasto en tu mensaje.")
            return
        }
        _processingState.value = ProcessingState.ConfirmExpenses(
            expenses = response.expenses,
            clarification = response.clarificationNeeded,
            location = location,
            imageUri = imageUri,
            inputType = inputType,
            rawInput = rawInput
        )
    }

    // ─── Confirmar y guardar gastos ───────────────────────────────────────────

    fun confirmExpenses(
        expenses: List<ParsedExpense>,
        location: LocationData?,
        imageUri: String?,
        inputType: InputType,
        rawInput: String
    ) {
        viewModelScope.launch {
            expenses.forEach { parsed ->
                val expense = Expense(
                    amount = parsed.amount,
                    description = parsed.description,
                    storeName = parsed.storeName,
                    category = parsed.category,
                    imageUri = imageUri,
                    latitude = location?.latitude,
                    longitude = location?.longitude,
                    address = location?.address,
                    timestamp = System.currentTimeMillis(),
                    rawInput = rawInput,
                    inputType = inputType
                )
                repository.insertExpense(expense)
                // Actualizar balance: restar el gasto
                val current = _balance.value
                val newBalance = current - parsed.amount.toFloat()
                setBalance(newBalance)
            }
            _processingState.value = ProcessingState.Success(expenses.size)
        }
    }

    fun dismissProcessingState() {
        _processingState.value = ProcessingState.Idle
    }

    // ─── Ubicación ────────────────────────────────────────────────────────────

    private suspend fun tryGetLocation(): LocationData? = try {
        locationService.getCurrentLocation()
    } catch (e: Exception) { null }

    // ─── Preferencias ─────────────────────────────────────────────────────────

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            themePrefs.setThemeMode(mode)
            _themeMode.value = mode
        }
    }

    fun setBalance(amount: Float) {
        viewModelScope.launch {
            themePrefs.setBalance(amount)
            _balance.value = amount
        }
    }

    fun setCustomColors(c1: Int, c2: Int, c3: Int) {
        viewModelScope.launch {
            themePrefs.setCustomColors(c1, c2, c3)
            _customColors.value = Triple(c1, c2, c3)
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceService.destroy()
    }
}
