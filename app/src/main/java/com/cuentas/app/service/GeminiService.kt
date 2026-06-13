package com.cuentas.app.service

import android.util.Base64
import com.cuentas.app.BuildConfig
import com.cuentas.app.domain.model.GeminiExpenseResponse
import com.cuentas.app.domain.model.ParsedExpense
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class GeminiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val apiKey = BuildConfig.GEMINI_API_KEY
    private val baseUrl =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-lite:generateContent"

    // ───────── API pública ─────────

    suspend fun parseExpenseFromText(userInput: String): GeminiExpenseResponse =
        withContext(Dispatchers.IO) {
            try {
                val body = buildTextRequest(userInput)
                val raw = callApi(body)
                parseGeminiResponse(raw)
            } catch (e: Exception) {
                GeminiExpenseResponse(error = "Error: ${e.message}")
            }
        }

    suspend fun parseExpenseFromImage(imageFile: File): GeminiExpenseResponse =
        withContext(Dispatchers.IO) {
            try {
                val bytes = imageFile.readBytes()
                val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                val mimeType = when {
                    imageFile.name.endsWith(".png", true) -> "image/png"
                    imageFile.name.endsWith(".webp", true) -> "image/webp"
                    else -> "image/jpeg"
                }
                val body = buildImageRequest(b64, mimeType)
                val raw = callApi(body)
                parseGeminiResponse(raw)
            } catch (e: Exception) {
                GeminiExpenseResponse(error = "Error procesando imagen: ${e.message}")
            }
        }

    // ───────── Builders de request ─────────

    private fun buildTextRequest(userInput: String): String {
        val prompt = """
            Eres un asistente de control de gastos personales en Perú.
            Analiza el siguiente texto y extrae TODOS los gastos mencionados.
            
            REGLAS CRÍTICAS para el monto:
            - "15 90" → 15.90  (dos números donde el segundo tiene 1-2 dígitos = son los céntimos)
            - "14 soles" → 14.0
            - "4 soles" → 4.0
            - "4" al final de una frase de precio → 4.0
            - Si hay múltiples productos en una misma frase → crear un registro por cada uno
            - Separadores: coma, "y", "también", "además"
            
            Devuelve ÚNICAMENTE JSON válido, sin texto extra:
            {
              "expenses": [
                {
                  "description": "nombre del producto/servicio",
                  "amount": 15.90,
                  "store_name": "tienda si se menciona, vacío si no",
                  "category": "Comida"
                }
              ],
              "clarification_needed": null
            }
            
            Categorías válidas: Comida, Transporte, Entretenimiento, Ropa, Hogar, Salud, Educación, Tecnología, Otro
            
            Texto: "$userInput"
        """.trimIndent()

        return JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("response_mime_type", "application/json")
                put("temperature", 0.1)
            })
        }.toString()
    }

    private fun buildImageRequest(base64Image: String, mimeType: String): String {
        val prompt = """
            Eres un asistente de control de gastos. Analiza esta imagen de una boleta, ticket o recibo.
            Extrae TODOS los productos/servicios y datos que puedas ver.
            
            Devuelve ÚNICAMENTE JSON válido, sin texto extra:
            {
              "expenses": [
                {
                  "description": "nombre del producto o servicio",
                  "amount": 15.90,
                  "store_name": "nombre de la tienda",
                  "category": "Comida"
                }
              ],
              "store_name": "nombre principal del establecimiento",
              "total": 50.00,
              "date": "YYYY-MM-DD o null",
              "clarification_needed": "pregunta específica si algo no es legible, o null"
            }
            
            Categorías válidas: Comida, Transporte, Entretenimiento, Ropa, Hogar, Salud, Educación, Tecnología, Otro
            Si un valor no se puede leer claramente, indícalo en clarification_needed.
        """.trimIndent()

        return JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("inline_data", JSONObject().apply {
                                put("mime_type", mimeType)
                                put("data", base64Image)
                            })
                        })
                        put(JSONObject().apply { put("text", prompt) })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("response_mime_type", "application/json")
                put("temperature", 0.1)
            })
        }.toString()
    }

    // ───────── HTTP call ─────────

    private fun callApi(requestBody: String): String {
        val request = Request.Builder()
            .url("$baseUrl?key=$apiKey")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: throw Exception("Respuesta vacía de Gemini")
            if (!response.isSuccessful) {
                throw Exception("Gemini API error ${response.code}: $body")
            }
            return body
        }
    }

    // ───────── Parser de respuesta ─────────

    private fun parseGeminiResponse(raw: String): GeminiExpenseResponse {
        return try {
            val root = JSONObject(raw)
            val candidates = root.getJSONArray("candidates")
            val text = candidates
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
                .trim()

            // Extraer el JSON del texto (a veces Gemini añade markdown fences)
            val jsonText = if (text.contains("```")) {
                text.substringAfter("```json\n", text.substringAfter("```\n"))
                    .substringBefore("```")
            } else text

            val result = JSONObject(jsonText)
            val expenses = mutableListOf<ParsedExpense>()

            result.optJSONArray("expenses")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    expenses.add(
                        ParsedExpense(
                            description = obj.optString("description", ""),
                            amount = obj.optDouble("amount", 0.0),
                            storeName = obj.optString("store_name", ""),
                            category = obj.optString("category", "Otro")
                        )
                    )
                }
            }

            val clarification = result.optString("clarification_needed").let {
                if (it.isBlank() || it == "null") null else it
            }
            val total = if (result.has("total") && !result.isNull("total"))
                result.optDouble("total") else null

            GeminiExpenseResponse(
                expenses = expenses,
                storeName = result.optString("store_name", ""),
                total = total,
                date = result.optString("date").let { if (it == "null" || it.isBlank()) null else it },
                clarificationNeeded = clarification
            )
        } catch (e: Exception) {
            GeminiExpenseResponse(error = "No pude procesar la respuesta: ${e.message}")
        }
    }
}
