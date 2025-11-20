package com.example.myapplication.feature.chat.viewmodel

sealed class ChatSideEffect {
    data class ShowError(val message: String) : ChatSideEffect()
    data class ShowSuccess(val message: String) : ChatSideEffect()
    object NavigateToSomewhere : ChatSideEffect()
}