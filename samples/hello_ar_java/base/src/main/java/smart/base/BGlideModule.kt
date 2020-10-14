package smart.base

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions

@GlideModule
class BGlideModule : AppGlideModule() {
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        super.applyOptions(context, builder)
        builder.setDefaultRequestOptions(RequestOptions().format(DecodeFormat.PREFER_ARGB_8888))
        //        builder.setLogLevel(Log.DEBUG);
//        final GlideExecutor.UncaughtThrowableStrategy myUncaughtThrowableStrategy = new GlideExecutor.UncaughtThrowableStrategy() {
//            @Override
//            public void handle(Throwable t) {
//
//            }
//        };
//        builder.setDiskCacheExecutor(newDiskCacheExecutor(myUncaughtThrowableStrategy));
        val diskCacheSizeBytes = 1024 * 1024 * 100 // 100 MB
        builder.setDiskCache(InternalCacheDiskCacheFactory(context, diskCacheSizeBytes.toLong()))
    }

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
//        registry.append(Photo.class, InputStream.class, new FlickrModelLoader.Factory());
    }

    // Disable manifest parsing to avoid adding similar modules twice.
    override fun isManifestParsingEnabled(): Boolean {
        return false
    }
}