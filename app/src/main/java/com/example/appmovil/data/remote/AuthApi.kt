package com.example.appmovil.data.remote

import retrofit2.Response
import retrofit2.http.*
import com.example.appmovil.data.remote.LoginRequest
import com.example.appmovil.data.remote.RegisterRequest
import com.example.appmovil.data.remote.UpdateUserStatusRequest
import com.example.appmovil.data.remote.AuthResponse
import com.example.appmovil.data.remote.SatelliteStatusItem
import com.example.appmovil.data.remote.UserContact
import com.example.appmovil.data.remote.GenericResponse
import com.example.appmovil.data.remote.MessageItem

interface AuthApi {
    @POST("api/login.php")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/register.php")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("api/update_status.php")
    suspend fun updateStatus(@Body request: UpdateUserStatusRequest): Response<AuthResponse>

    @POST("api/reset_lunch_status.php")
    suspend fun resetLunchStatus(@Body request: Map<String, Int>): Response<AuthResponse>

    @POST("api/get_satellites_status.php")
    suspend fun getSatellitesStatus(): Response<List<SatelliteStatusItem>>

    @GET("api/get_contacts.php")
    suspend fun getContacts(@Query("mi_id") miId: Int, @Query("rol") rol: String): Response<List<UserContact>>

    @GET("api/mark_messages_read.php")
    suspend fun markMessagesRead(
        @Query("mi_id") miId: Int,
        @Query("otro_id") otroId: Int
    ): GenericResponse

    @POST("api/get_private_messages.php")
    suspend fun getMessages(@Query("user1") user1: Int, @Query("user2") user2: Int): Response<List<MessageItem>>

    @FormUrlEncoded
    @POST("api/send_private_message.php")
    suspend fun sendMessage(
        @Field("emisor_id") emisor: Int,
        @Field("receptor_id") receptor: Int,
        @Field("mensaje") mensaje: String
    ): Response<AuthResponse>

    @GET("api/get_zones.php")
    suspend fun getZones(): Response<List<String>>

    @GET("api/get_user_info.php")
    suspend fun getUserInfo(@Query("id") id: Int): Response<UserProfileResponse>

    @POST("api/update_profile.php")
    suspend fun updateProfile(@Body profile: UserProfile): Response<DefaultResponse>

    @POST("api/change_password.php")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<DefaultResponse>

    @POST("api/update_user_zone.php")
    suspend fun updateUserZone(@Body request: UpdateUserZoneRequest): Response<AuthResponse>

    @FormUrlEncoded
    @POST("api/update_location.php")
    suspend fun updateLocation(
        @Field("userId") userId: Int,
        @Field("lat") lat: Double,
        @Field("lng") lng: Double
    ): Response<GenericResponse>

    @GET("api/get_users_locations.php")
    suspend fun getUsersLocations(): Response<List<UserLocationItem>>

    @FormUrlEncoded
    @POST("api/attendance.php")
    suspend fun markAttendance(
        @Field("userId") userId: Int,
        @Field("evento") evento: String // "INGRESO" o "SALIDA"
    ): Response<AuthResponse>

    @GET("api/get_attendance.php")
    suspend fun getAttendance(@Query("fecha") fecha: String? = null): Response<List<AttendanceItem>>
}
