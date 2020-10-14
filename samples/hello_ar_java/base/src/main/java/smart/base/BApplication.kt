package smart.base;

import android.graphics.Bitmap
import smart.auth.session
import smart.net.BUserAgent

abstract class BApplication : BDApplication() {
    override fun onCreate() {
        PPSharedPreferences.CREATE(applicationContext)
        super.onCreate()
        Pref.CREATE(applicationContext)
        BUserAgent.CREATE(applicationContext)
        session()
    }

    private var screenBitmap: Bitmap? = null
    fun setScreenBitmap(bitmap: Bitmap?) {
        screenBitmap?.takeUnless { it.isRecycled }?.recycle()
        screenBitmap = bitmap
    }

    fun getScreenBitmap(): Bitmap? = screenBitmap

    fun recycleCapture() {
        screenBitmap?.takeUnless { it.isRecycled }?.recycle()
        screenBitmap = null
    }
}

