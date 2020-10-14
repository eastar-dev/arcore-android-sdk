@file:Suppress("unused", "SpellCheckingInspection", "FunctionName")

package smart.base

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import dev.eastar.pref.annotation.Pref
import java.util.*

@Pref(defaultSharedPreferences = true)
data class PPSharedPreferences(
//@formatter:off
    //PP
    val androidId             : String  ,
    val appUuid               : String  ,
    val PUBLIC_KEY            : String  ,
    val AUTOLOGIN_ERROR_COUNT : Int     ,

    //AppPref
    val isPermissionChecked   : Boolean , //Y,N
//    val isGuide               : Boolean , //Y,N
    val isAccountNotiHide     : Boolean , //N,Y
    val appLanguage           : String  , //ConfigurationCompat.getLocales(context.resources.configuration)[0].language
//@formatter:on
) {
    companion object {
        @Suppress("LocalVariableName")
        @SuppressLint("HardwareIds")
        fun CREATE(context: Context) {
            val __ANDROID_ID = "__ANDROID_ID"
            val __APP_UUID = "__APP_UUID"
            val androidId = "androidId"
            val appUuid = "appUuid"
            PreferenceManager.getDefaultSharedPreferences(context).let { pref ->
                if (!PP.preferences.contains(androidId)) {
                    PP.androidId = pref.getString(__ANDROID_ID, UUID.randomUUID().toString())!!
                    pref.edit(true) { this.remove(__ANDROID_ID) }
                }

                if (!PP.preferences.contains(appUuid)) {
                    PP.appUuid = pref.getString(__APP_UUID, UUID(PP.androidId.hashCode().toLong(), Build.MODEL.hashCode().toLong()).toString())!!
                    pref.edit(true) { this.remove(__APP_UUID) }
                }
            }

            val NAME = "__APP_VAR"
            val AppLocale_KEY = "AppLocale_KEY"
            val CHECK_PERMISSION = "CHECK_PERMISSION"
            val CALL_GUIDE = "CALL_GUIDE"
            val ACCOUNT_NOTI_SHOW = "ACCOUNT_NOTI_SHOW"
            context.getSharedPreferences(NAME, Activity.MODE_PRIVATE).let { pref ->
                if (!PP.preferences.contains("appLanguage")) {
                    PP.appLanguage = pref.getString(AppLocale_KEY, Locale.KOREA.language)!!
                    pref.edit(true) { this.remove(AppLocale_KEY) }
                }
                if (!PP.preferences.contains("isPermissionChecked")) {
                    PP.isPermissionChecked = pref.getString(CHECK_PERMISSION, "") == "Y"
                    pref.edit(true) { this.remove(CHECK_PERMISSION) }
                }

                if (pref.contains(ACCOUNT_NOTI_SHOW)) {
                    PP.isAccountNotiHide = pref.getString(ACCOUNT_NOTI_SHOW, "") != "N"
                    pref.edit(true) { this.remove(ACCOUNT_NOTI_SHOW) }
                }
            }
        }
    }
}
