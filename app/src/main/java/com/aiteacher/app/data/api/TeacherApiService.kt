package com.aiteacher.app.data.api

import com.aiteacher.app.data.model.InteractResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface TeacherApiService {
    @Multipart
    @POST("api/v1/interact")
    suspend fun interact(
        @Part audio: MultipartBody.Part,
        @Part("user_id") userId: RequestBody
    ): Response<InteractResponse>

    @GET("api/v1/profile/{user_id}")
    suspend fun getProfile(
        @Path("user_id") userId: String
    ): Response<ResponseBody>

    @GET("api/v1/health")
    suspend fun checkHealth(): Response<ResponseBody>
}
