package com.example.tts_app.data.remote

object SourceFactory {
    fun getSource(): NovelSource {
        return try {
            val clazz = Class.forName("com.example.tts_app.data.remote.Source")
            clazz.getDeclaredConstructor().newInstance() as NovelSource
        } catch (e: Exception) {
            StubSource()
        }
    }
}