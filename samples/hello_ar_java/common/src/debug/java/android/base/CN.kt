package android.base

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager

enum class CN {
    REAL, STG, DEV, DEV2;

    companion object {
        fun getLastServer(context: Context) = valueOf(PreferenceManager.getDefaultSharedPreferences(context).getString(CN::class.java.name, REAL.name)!!)
        fun setLastServer(context: Context, server: CN) = PreferenceManager.getDefaultSharedPreferences(context).edit(true) { putString(CN::class.java.name, server.name) }
    }
}


