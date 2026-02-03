package com.example.tts_app.ui

sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    object Success : UiState()
    data class Error(val message: String) : UiState()
}

enum class ConnectionState {
    None, Testing, Success, Failed
}