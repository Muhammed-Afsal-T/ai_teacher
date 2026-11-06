package com.aiteacher.app.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    // 10.0.2.2 routes from the Android emulator to the host machine's localhost.
    // For a physical phone connected over Wi-Fi, change this to your PC's LAN IP (e.g. "http://192.168.1.100:8000/")
    var baseUrl: String = "http://10.0.2.2:8000/"
        set(value) {
            field = if (!value.endsWith("/")) "$value/" else value
            retrofitInstance = null
        }

    private var retrofitInstance: Retrofit? = null

    private fun getOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    fun getService(): TeacherApiService {
        val current = retrofitInstance
        if (current != null) return current.create(TeacherApiService::class.java)

        val newRetrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(getOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofitInstance = newRetrofit
        return newRetrofit.create(TeacherApiService::class.java)
    }

    fun resolveAudioUrl(relativeOrAbsoluteUrl: String): String {
        return if (relativeOrAbsoluteUrl.startsWith("http://") || relativeOrAbsoluteUrl.startsWith("https://")) {
            relativeOrAbsoluteUrl
        } else {
            val cleanRelative = relativeOrAbsoluteUrl.removePrefix("/")
            "$baseUrl$cleanRelative"
        }
    }
}
