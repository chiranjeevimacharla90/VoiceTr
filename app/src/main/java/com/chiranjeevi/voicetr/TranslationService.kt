package com.chiranjeevi.voicetr

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

private data class TranslationResponse(
    @SerializedName("responseData") val responseData: ResponseData?
)

private data class ResponseData(@SerializedName("translatedText") val translatedText: String?)

private interface MyMemoryApi {
    @GET("get")
    suspend fun translate(
        @Query("q") text: String,
        @Query("langpair") langPair: String
    ): TranslationResponse
}

object TranslationService {
    private val api: MyMemoryApi by lazy {
        val logger = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logger)
            .build()

        Retrofit.Builder()
            .baseUrl("https://api.mymemory.translated.net/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MyMemoryApi::class.java)
    }

    suspend fun translate(text: String, source: String, target: String): String {
        if (source == target) return text
        val response = api.translate(text, "$source|$target")
        return response.responseData?.translatedText?.takeIf { it.isNotBlank() }
            ?: error("No translation was returned by the service")
    }
}
