package com.example.nudgev1

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

// --- DATA CLASSES FOR GROQ API ---
data class GroqMessage(val role: String, val content: String)
data class GroqRequest(val model: String, val messages: List<GroqMessage>)
data class GroqChoice(val message: GroqMessage)
data class GroqResponse(val choices: List<GroqChoice>)

interface GroqApiService {
    @POST("chat/completions")
    suspend fun chat(
        @Header("Authorization") auth: String,
        @Body request: GroqRequest
    ): GroqResponse
}

data class ChatMessage(
    val text: String,
    val isUser: Boolean
)

class AIViewModel : ViewModel() {

    private val apiKey = "YOUR_GROQ_API_KEY"
    private val modelName = "openai/gpt-oss-120b"

    private val apiService: GroqApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.groq.com/openai/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GroqApiService::class.java)
    }

    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(
        listOf(ChatMessage("Hi! I'm your Nudge study buddy ✨ Ask me anything — I'll explain it simply, step by step.", false))
    )
    val chatHistory: StateFlow<List<ChatMessage>> = _chatHistory.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun sendMessage(userMessage: String) {
        if (userMessage.isBlank()) return

        // Add user message to UI
        _chatHistory.value = _chatHistory.value + ChatMessage(userMessage, true)
        _isLoading.value = true

        viewModelScope.launch {
            try {
                // Build message history for context
                val historyMessages = _chatHistory.value.map {
                    GroqMessage(
                        role = if (it.isUser) "user" else "assistant",
                        content = it.text
                    )
                }

                val systemPrompt = GroqMessage(
                    role = "system",
                    content = "You are a helpful college study buddy named Nudge. Keep responses concise, clear, and easy to read inside a mobile chat screen. Avoid using Markdown tables or raw HTML. Use simple bullet points, bold text for key terms, and short paragraphs. Explain concepts step-by-step."
                )

                val request = GroqRequest(
                    model = modelName,
                    messages = listOf(systemPrompt) + historyMessages
                )

                val response = apiService.chat("Bearer $apiKey", request)
                val responseText = response.choices.firstOrNull()?.message?.content ?: "Sorry, I couldn't generate a response."

                _chatHistory.value = _chatHistory.value + ChatMessage(responseText, false)
            } catch (e: Exception) {
                _chatHistory.value = _chatHistory.value + ChatMessage("Error: ${e.message}. Please check your internet or API key.", false)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
