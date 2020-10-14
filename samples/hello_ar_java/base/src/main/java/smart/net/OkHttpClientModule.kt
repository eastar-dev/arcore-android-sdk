package smart.net

import android.log.Log
import android.net.OkHttpClientBuilderModuleCommon
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(ApplicationComponent::class)
object OkHttpClientModule {
    @Singleton
    @Provides
    fun provideOkHttpClientBuilder(
        @OkHttpClientBuilderModuleCommon.OkHttpClientBuilder okHttpClientCommon: OkHttpClient.Builder
    ): OkHttpClient {
//        Log.e(okHttpClientCommon)
        return okHttpClientCommon
            .addInterceptor(BInterceptor())
            .build()
    }
}