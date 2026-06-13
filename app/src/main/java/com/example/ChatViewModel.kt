package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val isError: Boolean = false
)

class ChatViewModel : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Maintain conversational history for API context
    private val conversationHistory = mutableListOf<Content>()

    init {
        // Initial greeting
        _messages.value = listOf(
            ChatMessage(text = "Hello! I'm your AI assistant powered by Gemini. How can I help you today?", isUser = false)
        )
    }

    fun sendMessage(messageText: String) {
        if (messageText.isBlank()) return

        val userMsg = ChatMessage(text = messageText, isUser = true)
        _messages.value = _messages.value + userMsg

        // Add to API context
        conversationHistory.add(Content(role = "user", parts = listOf(Part(text = messageText))))

        _isLoading.value = true

        viewModelScope.launch {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                    addErrorMessage("API Key is missing. Please add it via the Settings menu.")
                    return@launch
                }

                val request = GenerateContentRequest(
                    // Send up to last 20 messages for context
                    contents = conversationHistory.takeLast(20)
                )

                val response = RetrofitClient.service.generateContent(apiKey, request)

                if (response.error != null) {
                    addErrorMessage(response.error.message ?: "Unknown API Error")
                } else {
                    val replyText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No response from AI."
                    
                    // Add AI reply to UI
                    val aiMsg = ChatMessage(text = replyText, isUser = false)
                    _messages.value = _messages.value + aiMsg
                    
                    // Add AI reply to history
                    conversationHistory.add(Content(role = "model", parts = listOf(Part(text = replyText))))
                }

            } catch (e: Exception) {
                e.printStackTrace()
                addErrorMessage("Network Error: ${e.localizedMessage}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun addErrorMessage(error: String) {
        _messages.value = _messages.value + ChatMessage(text = error, isUser = false, isError = true)
        // Ensure failed attempts or errors don't permanently break the conversation queue by keeping the model and user out of sync.
        // Actually, just leaving it is fine, it will resend next time. But realistically we might want to pop the last user query.
    }
}
