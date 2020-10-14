@file:Suppress("FunctionName")

package smart.net

import android.content.Context
import android.log.Log
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import android.webkit.WebSettings
import dev.eastar.ktx.*
import org.json.JSONObject
import smart.base.PP
import java.net.URLEncoder
import java.util.*

object BUserAgent {
    const val USER_AGENT_KEY = "User-Agent"
    lateinit var USER_AGENT: String

    @JvmStatic
    fun CREATE(context: Context) {
        USER_AGENT = getUserAgent(context)
        Log.w(USER_AGENT)
    }

    private fun getUserAgent(context: Context): String {
        val defaultUserAgent = runCatching { WebSettings.getDefaultUserAgent(context) }.onFailure { it.printStackTrace() }.getOrDefault("")
        val jo = JSONObject().apply {
            put("platform", "Android")
            put("brand", Build.BRAND)
            put("model", Build.MODEL)
            put("version", Build.VERSION.RELEASE)
            put("deviceId", "null")
            put("phoneNumber", "")
            put("countryIso", "null")
            put("telecom", context.networkOperatorName)
            put("simSerialNumber", "null")
            put("subscriberId", "null")
            put("appVersion", context.versionName)
            put("phoneName", "")
            put("appName", context.appName.toString().urlEncodeEuckr)
            put("deviceWidth", context.resources.displayMetrics.widthPixels.toString())
            put("deviceHeight", context.resources.displayMetrics.heightPixels.toString())
            put("uid", PP.androidId)
            put("hUid", PP.appUuid)
            put("etcStr", "")
            put("User-Agent", defaultUserAgent)
        }
        return "$jo;$defaultUserAgent"
    }
}