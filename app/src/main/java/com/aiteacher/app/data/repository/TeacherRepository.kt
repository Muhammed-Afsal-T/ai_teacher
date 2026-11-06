package com.aiteacher.app.data.repository

import com.aiteacher.app.data.api.RetrofitClient
import com.aiteacher.app.data.model.InteractResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class TeacherRepository {
    private val apiService get() = RetrofitClient.getService()

    suspend fun sendAudioInteraction(audioFile: File, userId: String = "user_1"): Result<InteractResponse> {
        return try {
            val requestFile = audioFile.asRequestBody("audio/m4a".toMediaTypeOrNull())
            val audioPart = MultipartBody.Part.createFormData("audio", audioFile.name, requestFile)
            val userPart = userId.toRequestBody("text/plain".toMediaTypeOrNull())

            val response = apiService.interact(audioPart, userPart)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Server returned error: ${response.code()}"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
