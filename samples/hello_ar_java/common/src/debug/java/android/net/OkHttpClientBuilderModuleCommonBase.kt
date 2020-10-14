package android.net

import android.content.Context
import com.readystatesoftware.chuck.ChuckInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(ApplicationComponent::class)
object OkHttpClientBuilderModuleCommonBase {

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class OkHttpClientBuilder

    @OkHttpClientBuilder
    @Singleton
    @Provides
    fun provideOkHttpClientBuilderCommonBase(
        @ApplicationContext context: Context
    ): OkHttpClient.Builder {
        return OkHttpClient().newBuilder()
            .addInterceptor(ChuckInterceptor(context))
            .addNetworkInterceptor(OkHttp3Logger())
    }
}
