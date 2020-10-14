package smart.base

import android.base.CApplication
import android.base.easterEgg
import android.content.Context
import android.log.logActivity

abstract class BDApplication : CApplication() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        DD.attachBaseContext()
    }

    override fun onCreate() {
        super.onCreate()
        easterEgg()
        logActivity()
        DD.onCreate(this)
    }
}
