package smart.base

import android.app.NotificationManager
import android.base.CD
import android.base.CN
import android.content.Context
import android.content.pm.PackageManager
import android.log.Log
import android.net.OkHttp3Logger
import android.os.Build
import android.provider.Settings
import android.webkit.WebView
import androidx.core.app.NotificationCompat
import dev.eastar.operaxwebextansion.OperaXLog
import java.io.File
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*

object DD {
    fun attachBaseContext() {
        Log.LOG = true

        CD.DEVELOP = true
        CD.PASS = true
        OperaXLog.LOG = true
        OperaXLog._IN_1 = true
        OperaXLog._OUT_1 = true

        OkHttp3Logger.LOG = true
        OkHttp3Logger._IN_1 = true
        OkHttp3Logger._OUT_1 = true
        OkHttp3Logger._IN_2 = true
        OkHttp3Logger._OUT_2 = true
//        OkHttp3Logger._IN_C = true
//        OkHttp3Logger._IN_H = true
//        OkHttp3Logger._OUT_C = true
//        OkHttp3Logger._OUT_H = true


        OperaXLog.LOG = true
    }

    fun onCreate(context: Context) {
        CD.CONTEXT = context
        Log.FILE_LOG = File(context.getExternalFilesDir(null), "_log.log")
        OperaXLog.FILE_LOG = File(context.getExternalFilesDir(null), "_log.log")
        NN.host(CN.getLastServer(context));
        logInfo(context)
        displayInfo(context)
        setWebContentsDebuggingEnabled()
        uuid(context)

//        Session.addRef()
//        hashkeyForDaumMap(context)
//        sacnFolder(context.filesDir.parentFile, "")
//        sacnFolder(File(context.getDatabasePath("0000").parent), "")
//        uncaughtExceptionHandler()
//        setNotification(context)
//        Pref.CREATE(context)
//        Log.e(Pref.get(Pref.__PUSH_TOKEN))
    }

    private fun uuid(context: Context) {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        Log.e("ANDROID_ID : ", androidId)
        Log.e("UUID : ", UUID(androidId.hashCode().toLong(), Build.MODEL.hashCode().toLong()).toString())

        Log.e("ANDROID_ID : ", PP.androidId)
        Log.e("UUID : ", PP.appUuid)
    }

    private fun logInfo(context: Context) {
        Log.e("Log.LOG       ", Log.LOG)
        Log.e("Log.FILE_LOG  ", Log.FILE_LOG)
        Log.e("BD.DEVELOP    ", CD.DEVELOP)
        Log.e("BD.PASS       ", CD.PASS)

        Log.e("NET.LOG       ", OkHttp3Logger.LOG)
        Log.e("NET._FLOG     ", OkHttp3Logger._FLOG)
        Log.e("_OUT_1        ", OkHttp3Logger._OUT_1)
        Log.e("_OUT_2        ", OkHttp3Logger._OUT_2)
        Log.e("_OUT_C        ", OkHttp3Logger._OUT_C)
        Log.e("_OUT_H        ", OkHttp3Logger._OUT_H)
        Log.e("_IN_1         ", OkHttp3Logger._IN_1)
        Log.e("_IN_2         ", OkHttp3Logger._IN_2)
        Log.e("_IN_C         ", OkHttp3Logger._IN_C)
        Log.e("_IN_H         ", OkHttp3Logger._IN_H)
        Log.e("SERVER        ", CN.getLastServer(context))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.w("WebView version: " + WebView.getCurrentWebViewPackage()?.versionName)
        }
    }

    private fun displayInfo(context: Context) {
        with(context.resources.displayMetrics) {
            Log.e("DENSITYDPI    ", densityDpi)
            Log.e("DENSITY       ", density)
            Log.e("WIDTHPIXELS   ", widthPixels)
            Log.e("HEIGHTPIXELS  ", heightPixels)
            Log.e("SCALEDDENSITY ", scaledDensity)
        }
    }

    private fun uncaughtExceptionHandler() {
        val dueHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.flog(android.util.Log.getStackTraceString(throwable))
            dueHandler.uncaughtException(thread, throwable)
        }
    }

    fun sacnFolder(dir: File, prefix: String) {
        if (!dir.isDirectory) {
            Log.i(prefix, dir.absolutePath, dir.length(), dir.exists())
            return
        }

        Log.e(prefix, dir.absolutePath)

        val fs = dir.listFiles()
        for (file in fs) {
            if (!file.isDirectory) {
                Log.i(prefix, file.absolutePath, file.length())
            }
        }

        for (file in fs) {
            if (file.isDirectory) {
                sacnFolder(file, "$prefix▷")
            }
        }
    }

    fun setNotification(context: Context) {
        try {
            val pm = context.packageManager
            val applicationInfo = pm.getApplicationInfo(context.packageName, 0)
            val name = pm.getApplicationLabel(applicationInfo).toString()

            val notificationBuilder = NotificationCompat.Builder(context, "COMMON")//
                .setSmallIcon(android.R.drawable.ic_notification_overlay)//
                .setAutoCancel(false)//
                .setContentTitle(name)//
                .setContentText("$name 실행중")//
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(0 /* ID of notification */, notificationBuilder.build())
        } catch (e: Exception) {
            Log.printStackTrace(e)
        }

    }

    private fun setWebContentsDebuggingEnabled() {
        runCatching { WebView.setWebContentsDebuggingEnabled(true) }.onFailure { it.printStackTrace() }
    }

    fun hashkeyForDaumMap(context: Context) {
        try {
            val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            for (signature in info.signatures) {
                val md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                Log.d(context.packageName, android.util.Base64.encodeToString(md.digest(), android.util.Base64.NO_WRAP))
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.printStackTrace(e)
        } catch (e: NoSuchAlgorithmException) {
            Log.printStackTrace(e)
        }
    }

}
