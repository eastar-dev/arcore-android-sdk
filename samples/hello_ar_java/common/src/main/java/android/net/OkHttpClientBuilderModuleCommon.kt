package android.net

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(ApplicationComponent::class)
object OkHttpClientBuilderModuleCommon {
    private const val connectTimeout: Long = 10                // ConnectTimeout Default 180
    private const val writeTimeout: Long = 120                 // WriteTimeout Default 180
    private const val readTimeout: Long = 120                  // ReadTimeout Default 180

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class OkHttpClientBuilder

    @OkHttpClientBuilder
    @Singleton
    @Provides
    fun provideOkHttpClientBuilderCommon(
        @OkHttpClientBuilderModuleCommonBase.OkHttpClientBuilder okHttpClientBuilderModuleCommonBase: OkHttpClient.Builder
    ): OkHttpClient.Builder {
        return okHttpClientBuilderModuleCommonBase.apply {
            connectTimeout(connectTimeout, TimeUnit.SECONDS)
            writeTimeout(writeTimeout, TimeUnit.SECONDS)
            readTimeout(readTimeout, TimeUnit.SECONDS)
            cookieJar(NetCookie())
        }
    }
}
