package com.example.tts_app.api

import okhttp3.ResponseBody
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Streaming

interface AllTalkApi {
    @FormUrlEncoded
    @POST("api/tts-generate")
    suspend fun generateAudio(
        @Field("text_input") text: String,
        @Field("character_voice_gen") voice: String,
        @Field("language") language: String = "en",
        @Field("text_filtering") textFiltering: String = "standard",
        @Field("narrator_enabled") narratorEnabled: String = "false",
        @Field("narrator_voice_gen") narratorVoice: String,
        @Field("text_not_inside") textNotInside: String = "character",

        @Field("output_file_name") outputFileName: String = "android_output",

        @Field("output_file_timestamp") outputFileTimestamp: String = "false",

        @Field("autoplay") autoplay: String = "false",
        @Field("autoplay_volume") autoplayVolume: String = "0.8"

    ): ResponseBody
    @Streaming
    @GET("audio/{filename}")
    suspend fun downloadAudio(@Path("filename") filename: String): ResponseBody
}