@file:Suppress("DEPRECATION")

package android.volley

import android.content.Context
import android.log.Log
import com.google.gson.GsonBuilder
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.components.ApplicationComponent
import dev.eastar.ktx.urlEncode
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.lang.reflect.Field
import java.util.*

abstract class NetEnty {
    var _url: String = ""
    val params = HashMap<String, Any?>()
    var bodyContentType = "text/plain; charset=" + Charsets.UTF_8
    private fun getBody(): ByteArray {
        this.bodyContentType = "application/x-www-form-urlencoded; charset=" + Charsets.UTF_8
        return params.entries.joinToString("&") {
            it.key.urlEncode + "=" + it.value?.toString()?.urlEncode
        }.toByteArray(Charsets.UTF_8)
    }

    protected var errorCode = "-1"
    protected var errorMessage = "error"

    protected fun setParams(vararg key_value: Any?) = putParamsInternal(key_value.toPair())
    private fun putParamsInternal(params: Iterable<Pair<String, Any?>>) {
        params.forEach {
            val key = it.first
            val value = it.second
            when {
                value == null -> this.params[key] = null
                value.javaClass.isArray -> (value as Array<*>).forEachIndexed { index, any -> this.params["$key[$index]"] = any }
                value.javaClass.isAssignableFrom(List::class.java) -> (value as List<*>).forEachIndexed { index, any -> this.params["$key[$index]"] = any }
                value is Boolean -> this.params[key] = if (value) "Y" else "N"
                else -> this.params[key] = value
            }
        }
    }

    open fun parseData(json: String) {
        val gson = GsonBuilder().create()
        try {
            val field = getDataField(javaClass)
            val type = field.type
            field.isAccessible = true
            field.set(this, gson.fromJson(json, type))
        } catch (e: Exception) {
            Log.printStackTrace(e)
        }
    }

    @Throws(NoSuchFieldException::class)
    private fun getDataField(clz: Class<*>?): Field {
        if (!NetEnty::class.java.isAssignableFrom(clz!!))
            throw NoSuchFieldException()
        return try {
            clz.getDeclaredField("data")
        } catch (e: NoSuchFieldException) {
            getDataField(clz.superclass)
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    fun call() {
        Log.e(javaClass)

        val body = getBody().toRequestBody(bodyContentType.toMediaTypeOrNull())

        val request = Request.Builder()
            .url(_url)
            .post(body)
            .build()

        val okHttpClient = hiltEntryPoint.okHttpClient()

        val response = okHttpClient.newCall(request).execute()
        try {
            val json = response.body?.string() ?: JSONObject.NULL.toString()
            parseData(json)
        } catch (e: Exception) {
            Log.printStackTrace(e)
        }
    }
}

inline fun <reified R> Array<*>.toPair(): List<R> = toList().toPair()
inline fun <reified R> Iterable<*>.toPair(): List<R> {
    if (count() % 2 == 1)
        throw IllegalArgumentException("!!key value must pair")

    return zipWithNext { a, b ->
        (a to b) as R
    }.filterIndexed { index, _ ->
        index % 2 == 0
    }
}

@EntryPoint
@InstallIn(ApplicationComponent::class)
interface NetEntryPoint {
    fun okHttpClient(): OkHttpClient
}

lateinit var hiltEntryPoint: NetEntryPoint
fun entryPoint(context: Context) {
    hiltEntryPoint = EntryPointAccessors.fromApplication(context, NetEntryPoint::class.java)
}
