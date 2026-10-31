package com.example.herbhopper_v1.data.network

import com.example.herbhopper_v1.BuildConfig
import com.example.herbhopper_v1.data.Order
import com.example.herbhopper_v1.data.Product
import com.example.herbhopper_v1.data.SessionManager
import com.google.gson.Gson
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface ApiService {
    // ─── Autenticación y perfil ───────────────────────────────────────────
    @POST("users")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @POST("users/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("users/refresh")
    suspend fun refresh(@Body request: RefreshRequest): AuthResponse

    @POST("users/logout")
    suspend fun logout(@Body request: RefreshRequest): MessageResponse

    @POST("users/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): MessageResponse

    @GET("users/me")
    suspend fun getMe(): UserResponse

    @PUT("users/me")
    suspend fun updateMe(@Body request: UpdateMeRequest): UserResponse

    @DELETE("users/me")
    suspend fun deleteMe(): MessageResponse

    @POST("users/device-token")
    suspend fun issueDeviceToken(): DeviceTokenResponse

    // ─── Administración de usuarios (ADMIN) ───────────────────────────────
    @GET("users")
    suspend fun getAllUsers(): List<UserResponse>

    @POST("users/admin-create")
    suspend fun adminCreateUser(@Body request: AdminCreateUserRequest): MessageResponse

    @GET("users/password-resets")
    suspend fun getPasswordResets(): List<PasswordResetResponse>

    @POST("users/password-resets/{id}/resolve")
    suspend fun resolvePasswordReset(@Path("id") id: Int): MessageResponse

    @PUT("users/{id}")
    suspend fun updateUser(@Path("id") id: String, @Body request: UpdateUserRequest): MessageResponse

    @PATCH("users/{id}/status")
    suspend fun setUserActive(@Path("id") id: String, @Body request: UserStatusRequest): MessageResponse

    @DELETE("users/{id}")
    suspend fun deleteUser(@Path("id") id: String): MessageResponse

    // ─── Productos / inventario ───────────────────────────────────────────
    @GET("products")
    suspend fun getProducts(): List<Product>

    @POST("products")
    suspend fun createProduct(@Body product: Product): Product

    @PUT("products/{id}")
    suspend fun updateProduct(@Path("id") id: Int, @Body product: Product): Product

    @DELETE("products/{id}")
    suspend fun deleteProduct(@Path("id") id: Int): MessageResponse

    @POST("products/{id}/reorder")
    suspend fun reorderProduct(@Path("id") id: Int, @Body request: ReorderRequest): MessageResponse

    @Multipart
    @POST("upload")
    suspend fun uploadImage(@Part image: MultipartBody.Part): UploadResponse

    // ─── Pedidos ──────────────────────────────────────────────────────────
    @GET("orders")
    suspend fun getOrders(): List<Order>

    @GET("orders/{orderId}")
    suspend fun getOrder(@Path("orderId") orderId: String): Order

    @POST("orders")
    suspend fun createOrder(@Body request: CreateOrderRequest): Order

    @PUT("orders/{orderId}/status")
    suspend fun updateOrderStatus(@Path("orderId") orderId: String, @Body request: OrderStatusRequest): MessageResponse

    @PUT("orders/{orderId}/cancel")
    suspend fun cancelMyOrder(@Path("orderId") orderId: String): MessageResponse

    @GET("orders/stats/today")
    suspend fun getSellerStats(): SellerStatsResponse

    @GET("payment/status/{orderId}")
    suspend fun getPaymentStatus(@Path("orderId") orderId: String): PaymentStatusResponse

    // ─── Fórmulas médicas ─────────────────────────────────────────────────
    @Multipart
    @POST("prescriptions")
    suspend fun uploadPrescription(
        @Part file: MultipartBody.Part,
        @Part("doctorName") doctorName: RequestBody,
        @Part("doctorRegistry") doctorRegistry: RequestBody
    ): PrescriptionResponse

    @GET("prescriptions")
    suspend fun getPrescriptions(): List<PrescriptionResponse>

    @PUT("prescriptions/{id}/review")
    suspend fun reviewPrescription(@Path("id") id: Int, @Body request: PrescriptionReviewRequest): PrescriptionResponse

    // ─── Administración del sistema ───────────────────────────────────────
    @GET("admin/health")
    suspend fun getSystemHealth(): SystemHealthResponse

    @POST("admin/services/restart")
    suspend fun restartServices(): RestartResponse

    @GET("admin/analytics")
    suspend fun getAnalytics(): AnalyticsResponse

    @GET("admin/logs")
    suspend fun getAuditLogs(@Query("limit") limit: Int = 100, @Query("type") type: String? = null): List<AuditLogResponse>

    @GET("admin/restock-requests")
    suspend fun getRestockRequests(): List<RestockRequestResponse>

    @POST("admin/restock-requests/{id}/fulfill")
    suspend fun fulfillRestock(@Path("id") id: Int): MessageResponse

    // ─── Sucursales ───────────────────────────────────────────────────────
    @GET("stores")
    suspend fun getStores(): List<StoreDto>

    @POST("stores")
    suspend fun createStore(@Body store: StoreDto): StoreDto

    @PUT("stores/{id}")
    suspend fun updateStore(@Path("id") id: Int, @Body store: StoreDto): StoreDto

    @DELETE("stores/{id}")
    suspend fun deleteStore(@Path("id") id: Int): MessageResponse

    // ─── Soporte ──────────────────────────────────────────────────────────
    @GET("support/messages")
    suspend fun getSupportMessages(): List<SupportMessageDto>

    @POST("support/messages")
    suspend fun sendSupportMessage(@Body request: SupportMessageRequest): SupportMessageDto

    @GET("support/conversations")
    suspend fun getSupportConversations(): List<SupportConversationDto>

    @GET("support/conversations/{userId}")
    suspend fun getSupportConversation(@Path("userId") userId: String): List<SupportMessageDto>

    @POST("support/conversations/{userId}/reply")
    suspend fun replySupport(@Path("userId") userId: String, @Body request: SupportMessageRequest): SupportMessageDto

    companion object {
        const val BASE_URL = "https://herb-hopper-v2.onrender.com/api/"
        const val PAYMENT_RESPONSE_URL = "https://herb-hopper-v2.onrender.com/api/payment/response"
        const val PAYMENT_CONFIRMATION_URL = "https://herb-hopper-v2.onrender.com/api/payment/confirmation"

        /** Cliente sin autenticación, usado solo para renovar el token. */
        private val refreshApi: ApiService by lazy {
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).build())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }

        val instance: ApiService by lazy {
            val clientBuilder = OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor())
                .authenticator(TokenAuthenticator(refreshApi))
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS) // Render puede tardar en "despertar"

            // Solo en depuración y solo encabezados: nunca se registran cuerpos (contraseñas, tokens)
            if (BuildConfig.DEBUG) {
                clientBuilder.addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.HEADERS
                    redactHeader("Authorization")
                })
            }

            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(clientBuilder.build())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }
    }
}

/** Agrega "Authorization: Bearer <JWT>" a cada petición cuando hay sesión. */
private class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = SessionManager.getAccessToken()
        val request = if (token != null) {
            chain.request().newBuilder().header("Authorization", "Bearer $token").build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}

/** Si el JWT venció (401), usa el token de renovación una vez y reintenta la petición. */
private class TokenAuthenticator(private val refreshApi: ApiService) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.priorResponse != null) return null // ya se reintentó
        val refreshToken = SessionManager.getRefreshToken() ?: return null
        return synchronized(this) {
            try {
                val auth = kotlinx.coroutines.runBlocking { refreshApi.refresh(RefreshRequest(refreshToken)) }
                SessionManager.updateTokens(auth.token, auth.refreshToken)
                response.request.newBuilder().header("Authorization", "Bearer ${auth.token}").build()
            } catch (e: Exception) {
                null
            }
        }
    }
}

/** Extrae el mensaje "error" que devuelve el backend en respuestas 4xx/5xx. */
fun Throwable.apiErrorMessage(fallback: String = "Error de conexión con el servidor"): String {
    if (this is HttpException) {
        val body = response()?.errorBody()?.string()
        val parsed = try { Gson().fromJson(body, ErrorResponse::class.java)?.error } catch (e: Exception) { null }
        if (!parsed.isNullOrBlank()) return parsed
    }
    return fallback
}

// ─── DTOs ─────────────────────────────────────────────────────────────────

data class ErrorResponse(val error: String?)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val role: String = "PATIENT",
    val phone: String? = null
)

data class LoginRequest(val email: String, val password: String)

data class RefreshRequest(val refreshToken: String, val device: Boolean = false)

data class DeviceTokenResponse(val deviceToken: String)

data class UserResponse(
    val id: String,
    val name: String,
    val email: String,
    val role: String,
    val phone: String? = null,
    val active: Boolean = true
)

data class AuthResponse(
    val message: String,
    val user: UserResponse,
    val token: String,
    val refreshToken: String,
    /** Solo en el acceso con huella: token nuevo para la bóveda biométrica. */
    val deviceToken: String? = null
)

data class UpdateMeRequest(val name: String? = null, val phone: String? = null, val password: String? = null)

data class AdminCreateUserRequest(val name: String, val email: String, val password: String, val role: String)

data class UploadResponse(val message: String, val url: String, val public_id: String)

data class ForgotPasswordRequest(val email: String)

data class MessageResponse(val message: String)

data class PasswordResetResponse(val id: Int, val email: String, val timestamp: Long, val status: String)

data class UpdateUserRequest(val name: String, val email: String, val password: String?, val role: String)

data class UserStatusRequest(val active: Boolean)

data class ReorderRequest(val quantity: Int)

data class OrderItemRequest(val productId: Int, val quantity: Int)

data class CreateOrderRequest(
    val orderId: String,
    val items: List<OrderItemRequest>,
    val address: String,
    val latitude: Double?,
    val longitude: Double?,
    val paymentMethod: String,
    val prescriptionId: Int?
)

data class OrderStatusRequest(val status: String)

data class SellerStatsResponse(val salesToday: Double, val paidOrdersToday: Int, val newOrders: Int)

data class PaymentStatusResponse(val orderId: String, val paymentStatus: String, val paymentRef: String?, val paymentMethod: String?)

data class PrescriptionResponse(
    val id: Int,
    val userId: String,
    val patientName: String?,
    val doctorName: String,
    val doctorRegistry: String,
    val status: String, // PENDING, APPROVED, REJECTED
    val reviewNote: String?,
    val createdAt: Long,
    val reviewedAt: Long?,
    val reviewDeadline: Long,
    val fileFormat: String?,
    val fileUrl: String?
)

data class PrescriptionReviewRequest(val status: String, val note: String?)

data class IntegrityCheck(val name: String, val issues: Int, val ok: Boolean)

data class SystemHealthResponse(
    val database: String,
    val latencyMs: Long,
    val integrityPercent: Int,
    val checks: List<IntegrityCheck>,
    val counts: Map<String, Int>,
    val uptimeSeconds: Long,
    val memoryMb: Int,
    val checkedAt: Long
)

data class RestartResponse(val message: String, val health: SystemHealthResponse)

data class BreakdownItem(val key: String?, val count: Int, val amount: Double?)

data class AnalyticsResponse(
    val approvedSales: Double,
    val totalOrders: Int,
    val approvedPayments: Int,
    val activeUsers: Int,
    val ordersByStatus: List<BreakdownItem>,
    val salesByPaymentMethod: List<BreakdownItem>,
    val usersByRole: List<BreakdownItem>
)

data class AuditLogResponse(
    val id: Int,
    val type: String,
    val message: String,
    val userId: String?,
    val level: String,
    val timestamp: Long
)

data class RestockRequestResponse(
    val id: Int,
    val productId: Int,
    val productName: String,
    val stock: Int,
    val quantity: Int,
    val status: String,
    val timestamp: Long
)

data class StoreDto(
    val id: Int = 0,
    val name: String,
    val location: String,
    val licenseNumber: String,
    val status: String = "PENDING"
)

data class SupportMessageDto(val id: Int, val userId: String, val text: String, val fromSupport: Boolean, val timestamp: Long)

data class SupportMessageRequest(val text: String)

data class SupportConversationDto(
    val userId: String,
    val name: String,
    val email: String,
    val text: String,
    val fromSupport: Boolean,
    val timestamp: Long,
    val awaitingReply: Boolean
)
