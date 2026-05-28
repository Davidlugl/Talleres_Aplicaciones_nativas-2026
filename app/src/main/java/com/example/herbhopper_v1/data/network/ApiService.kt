package com.example.herbhopper_v1.data.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("users")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @POST("users/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @retrofit2.http.GET("products")
    suspend fun getProducts(): List<com.example.herbhopper_v1.data.Product>

    @POST("products")
    suspend fun createProduct(@Body product: com.example.herbhopper_v1.data.Product): com.example.herbhopper_v1.data.Product

    @retrofit2.http.Multipart
    @POST("upload")
    suspend fun uploadImage(
        @retrofit2.http.Part image: okhttp3.MultipartBody.Part
    ): UploadResponse

    @POST("users/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): MessageResponse

    @retrofit2.http.GET("users")
    suspend fun getAllUsers(): List<UserResponse>

    @retrofit2.http.GET("users/password-resets")
    suspend fun getPasswordResets(): List<PasswordResetResponse>

    @POST("users/password-resets/{id}/resolve")
    suspend fun resolvePasswordReset(@retrofit2.http.Path("id") id: Int): MessageResponse

    @retrofit2.http.PUT("users/{id}")
    suspend fun updateUser(@retrofit2.http.Path("id") id: String, @Body request: UpdateUserRequest): MessageResponse

    @retrofit2.http.DELETE("users/{id}")
    suspend fun deleteUser(@retrofit2.http.Path("id") id: String): MessageResponse

    companion object {
        private const val BASE_URL = "https://herb-hopper-v2.onrender.com/api/"

        val instance: ApiService by lazy {
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }
    }
}

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val role: String = "PATIENT"
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class UserResponse(
    val id: String,
    val name: String,
    val email: String,
    val role: String
)

data class AuthResponse(
    val message: String,
    val user: UserResponse
)

data class UploadResponse(
    val message: String,
    val url: String,
    val public_id: String
)

data class ForgotPasswordRequest(val email: String)
data class MessageResponse(val message: String)
data class PasswordResetResponse(
    val id: Int,
    val email: String,
    val timestamp: Long,
    val status: String
)
data class UpdateUserRequest(
    val name: String,
    val email: String,
    val password: String?,
    val role: String
)
