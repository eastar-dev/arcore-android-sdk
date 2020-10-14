package smart.util

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.log.Log
import android.text.TextUtils
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import dev.eastar.operaxwebextansion.OperaXRequest
import dev.eastar.operaxwebextansion.OperaXResponse
import org.json.JSONException
import org.json.JSONObject
import smart.base.PP
import java.net.URISyntaxException
import java.util.*

object UU {
    @JvmStatic
    fun hideKeyboard(activity: Activity) {
        val view = activity.currentFocus
        if (view != null) {
            val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    @JvmStatic
    fun opt_parseUri(intent: String?): Intent? {
        return try {
            Intent.parseUri(intent, Intent.URI_INTENT_SCHEME)
        } catch (e: URISyntaxException) {
            e.printStackTrace()
            null
        }
    }

    fun copy(context: Context, text: String?) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("label", text)
        clipboard.setPrimaryClip(clip)
    }

    @JvmStatic
    fun isEmpty(cs: CharSequence?) = cs.isNullOrEmpty()

    @JvmStatic
    fun dummy(len: Int) = "".padEnd(len, '●')
}


@Suppress("unused")
fun ComponentActivity.localeLifecycleObserver() {
    lifecycle.addObserver(
        object : LifecycleObserver {
            private lateinit var mLocale: Locale

            @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
            fun createLocal() {
                Log.e("``ll", this@localeLifecycleObserver.javaClass.simpleName, "저장언어", Locale.getDefault())
                mLocale = Locale.getDefault()
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_START)
            fun startLocal() {
                if (mLocale != Locale.getDefault()) {
                    Log.w("mLocale != Locale.getDefault() 화면갱신")
                    startActivity(intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                } else if (PP.appLanguage.isNotBlank() && Locale(PP.appLanguage) != Locale.getDefault()) {
                    Log.w("PP.appLanguage != Locale.getDefault() 화면갱신")
                    startActivity(intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                } else {
                    Log.i("언어변경 없음")
                }
            }
        }
    )
}