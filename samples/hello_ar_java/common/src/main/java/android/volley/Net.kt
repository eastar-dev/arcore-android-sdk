package android.volley

import android.log.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * <pre>
 * &lt;uses-permission android:name="android.permission.INTERNET" />
</pre> *
 */

@Suppress("ObjectPropertyName", "SpellCheckingInspection")
object Net {
    suspend fun <R : NetEnty> block(enty: R): R = withContext(Dispatchers.IO) {
        kotlin.runCatching {
            enty.call()
        }.getOrElse {
            Log.w(it)
            throw it
        }
        enty
    }

    fun <R : NetEnty> asyncKt(enty: R, successCallback: ((R) -> Unit)? = null, errorCallback: ((Throwable) -> Unit)? = null) {
        CoroutineScope(Dispatchers.Main).launch {
            kotlin.runCatching {
                val result = block(enty)
                successCallback?.invoke(result)
            }.getOrElse {
                errorCallback?.invoke(it)
            }
        }
    }
}

