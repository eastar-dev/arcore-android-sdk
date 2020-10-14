package android.net

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import okhttp3.OkHttpClient
import javax.inject.Qualifier

@Module
@InstallIn(ApplicationComponent::class)
object OkHttpClientBuilderModuleCommonBase {

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class OkHttpClientBuilder

    @OkHttpClientBuilder
    @Provides
    fun provideOkHttpClientBuilderCommonBase() = OkHttpClient().newBuilder()
}
