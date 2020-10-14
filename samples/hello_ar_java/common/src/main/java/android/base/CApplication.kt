package android.base

import android.app.Application
import android.content.Context
import android.log.Log
import android.volley.entryPoint

abstract class CApplication : Application() {
    override fun attachBaseContext(base: Context) {
        Log.e("=============================================================================")
        Log.e("=== 시작됨 ==================================================================")
        Log.e("=============================================================================")
        super.attachBaseContext(base)
    }

    override fun onCreate() {
        super.onCreate()
        BV.create(this)
        entryPoint(this)
    }

}


