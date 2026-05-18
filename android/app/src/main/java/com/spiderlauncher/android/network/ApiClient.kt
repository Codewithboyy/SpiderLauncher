package com.spiderlauncher.android.network

import com.spiderlauncher.android.model.VersionDetail
import com.spiderlauncher.android.model.VersionManifest
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

interface MojangApi {

    @GET("mc/game/version_manifest_v2.json")
    suspend fun getVersionManifest(): Response<VersionManifest>

    @GET
    suspend fun getVersionDetail(@Url url: String): Response<VersionDetail>
}

object ApiClient {

    private const val BASE_URL = "https://piston-meta.mojang.com/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    val mojangApi: MojangApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MojangApi::class.java)
    }

    fun buildDownloadClient(): OkHttpClient = okHttpClient
}
