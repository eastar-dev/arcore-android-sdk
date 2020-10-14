@file:Suppress("unused")
package android.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.preference.PreferenceManager
import java.io.File
import java.util.*

class KK

val Context.isKorean
    get() = (Locale.getDefault() == Locale.KOREAN)

/** 시작시 변경하는 언어설정 */
fun activityAttachBaseContext(context: Context?, locale: Locale): Context? {
//    Log.d("``ll", context?.javaClass?.simpleName, locale)
    context ?: return null
    Locale.setDefault(locale)
    val configuration = context.resources.configuration
    configuration.setLocale(locale)
//    Log.e("``ll language", locale)
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
        context.createConfigurationContext(configuration)
    } else {
        context.resources.updateConfiguration(configuration, context.resources.displayMetrics)
    }
    return context
}

@SuppressLint("MissingPermission")
fun Context.isDeviceOnline(): Boolean {
    return try {
//        if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE)) {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = cm.activeNetworkInfo
        networkInfo != null && networkInfo.isConnected
//        }
//        return false
    } catch (e: Exception) {
        false
    }
}

//////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////

fun Bitmap.toFile(file: File) = runCatching {
    file.run {
        parentFile?.let {
            if (!it.exists())
                it.mkdirs()
        }
        outputStream().use { output ->
            this@runCatching.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
    }
}.getOrDefault(false)

