package com.aiteacher.app.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    // Deployed backend base URL
    var baseUrl: String = "http://gambzjbzxw3vkwkmuv0qezy9.140.245.6.31.sslip.io/"
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
