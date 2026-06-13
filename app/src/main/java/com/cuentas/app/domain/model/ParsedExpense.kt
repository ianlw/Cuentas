package com.cuentas.app.domain.model

/** Resultado de parseo de Gemini para un gasto individual */
data class ParsedExpense(
    val description: String,
    val amount: Double,
    val storeName: String = "",
    val category: String = "Otro",
    val clarificationNeeded: String? = null
)

/** Respuesta completa de Gemini — puede incluir múltiples gastos (ej: entrada de voz con varios ítems) */
data class GeminiExpenseResponse(
    val expenses: List<ParsedExpense> = emptyList(),
    val storeName: String = "",
    val total: Double? = null,
    val date: String? = null,
    val clarificationNeeded: String? = null,
    val error: String? = null
)
