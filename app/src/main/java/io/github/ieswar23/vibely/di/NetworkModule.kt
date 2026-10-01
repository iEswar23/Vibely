package io.github.ieswar23.vibely.di

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.ieswar23.vibely.BuildConfig
import io.github.ieswar23.vibely.data.remote.VibelyApi
import io.github.ieswar23.vibely.data.remote.mock.AndroidAssetSource
import io.github.ieswar23.vibely.data.remote.mock.MockInterceptor
import io.github.ieswar23.vibely.util.Clock
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().create()

    @Provides
    @Singleton
    fun provideMockInterceptor(@ApplicationContext context: Context, clock: Clock): MockInterceptor =
        MockInterceptor(assets = AndroidAssetSource(context), clock = clock)

    @Provides
    @Singleton
    fun provideOkHttpClient(mockInterceptor: MockInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
                },
            )
            // Must be last: it short-circuits the chain and serves fixtures from assets.
            .addInterceptor(mockInterceptor)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl(VibelyApi.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    fun provideVibelyApi(retrofit: Retrofit): VibelyApi = retrofit.create(VibelyApi::class.java)
}
