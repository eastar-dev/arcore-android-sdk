package android.net

import android.os.SystemClock
import com.google.firebase.crashlytics.FirebaseCrashlytics
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import okio.GzipSource
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.experimental.and

@Suppress("LocalVariableName")
class OkHttp3Logger : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        if (!LOG)
            return chain.proceed(chain.request())

        if (chain.request().headers.firstOrNull { "${it.first}:${it.second}" == NO_LOG_HEADER } != null)
            return chain.proceed(chain.request())


        val request = chain.request()
        val _out_1 = request.method + ":" + request.url
        val _out_2 = request.bodyString()
        val _out_c = request.headers.cookieString()
        val _out_h = request.headers.headerString()

        if (_OUT_C) e(_out_c).run { flog(_out_c) }
        if (_OUT_H) e(_out_h).run { flog(_out_h) }
        if (_OUT_1 && !_OUT_2) {
            e(_out_1, _out_2.take(3600).trim().replace('\n', '↙'))
        } else {
            e(_out_1)
            e(_out_2)
        }
        if (_IN_1 || _IN_2) flog(_out_1)
        if (_IN_2) flog(_out_2)


        val startNs = System.nanoTime()
        return kotlin.runCatching {
            chain.proceed(request)
        }.onSuccess { response ->
            val tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs)

            val _in_c = request.headers.cookieString()
            val _in_h = request.headers.headerString()
            val _in_1 = "${response.code} ${response.message} (${tookMs}ms) ${response.request.url}  ${response.request.bodyString().replace('\n', '↙')}"
            val _in_2 = response.bodyString()

            val priority = if (response.isSuccessful) android.util.Log.INFO else android.util.Log.WARN
            if (_IN_C) p(priority, _in_c).run { flog(_in_c) }
            if (_IN_H) p(priority, _in_h).run { flog(_in_h) }
            if (_IN_1 && !_IN_2) {
                p(priority, (_in_1 + _in_2.take(1000)).trim().replace('\n', '↙'))
            } else {
                p(priority, _in_1)
                p(priority, _in_2)
            }
            if (_IN_1 || _IN_2) flog(_in_1)
            if (_IN_2) flog(_in_2)
        }.onFailure {
            FirebaseCrashlytics.getInstance().recordException(it)
            w(it.stackTraceToString())
            w(_out_1)
            w(_out_2)
            flog(it)
            flog(_out_1)
            flog(_out_2)
        }.getOrThrow()
    }

    companion object {
        var LOG = true
        var _FLOG: File? = null

        var _OUT_1 = true
        var _OUT_2 = false
        var _OUT_H = false
        var _OUT_C = false
        var _IN_1 = true
        var _IN_2 = false
        var _IN_H = false
        var _IN_C = false

        const val COOKIE = "Cookie"
        const val SET_COOKIE = "Set-Cookie"
        const val NO_LOG_HEADER = "NoLog:NoLog"

        private val UTF8 = Charset.forName("UTF-8")

        private const val LF = "\n"
        private const val MAX_LOG_LINE_BYTE_SIZE = 3600
        private const val PREFIX = "``"
    }


    private fun e(vararg args: Any?) {
        if (!LOG) return
        val msg: String = makeMessage(*args)
        println(android.util.Log.ERROR, msg)
    }

    private fun i(vararg args: Any?) {
        val msg: String = makeMessage(*args)
        println(android.util.Log.INFO, msg)
    }

    private fun w(vararg args: Any?) {
        val msg: String = makeMessage(*args)
        println(android.util.Log.WARN, msg)
    }

    private fun p(priority: Int, vararg args: Any?) {
        val msg: String = makeMessage(*args)
        println(priority, msg)
    }

    private fun flog(vararg args: Any?) {
        _FLOG ?: return
        runCatching {
            val log: String = makeMessage(*args)
            val st = StringTokenizer(log, LF, false)

            val tag = "%-40s%-40d %-100s ``".format(Date().toString(), SystemClock.elapsedRealtime(), "")
            if (st.hasMoreTokens()) {
                val token = st.nextToken()
                _FLOG!!.appendText(tag + token + LF)
            }

            val space = "%-40s%-40s %-100s ``".format("", "", "")
            while (st.hasMoreTokens()) {
                val token = st.nextToken()
                _FLOG!!.appendText(space + token + LF)
            }
        }
    }

    private fun println(priority: Int, msg: String?) {
        val sa = ArrayList<String>(100)
        val st = StringTokenizer(msg, LF, false)
        while (st.hasMoreTokens()) {
            val byteText = st.nextToken().toByteArray()
            var offset = 0
            while (offset < byteText.size) {
                val count: Int = safeCut(byteText, offset)
                sa.add(PREFIX + String(byteText, offset, count))
                offset += count
            }
        }
        when (sa.size) {
            0 -> android.util.Log.println(priority, "okHttp", PREFIX)
            else -> sa.forEach { android.util.Log.println(priority, "okHttp", it) }
        }
    }

    private fun safeCut(byteArray: ByteArray, startOffset: Int): Int {
        val byteLength = byteArray.size
        if (byteLength <= startOffset) throw ArrayIndexOutOfBoundsException("!!text_length <= start_byte_index")
        if (byteArray[startOffset] and 0xc0.toByte() == 0x80.toByte()) throw java.lang.UnsupportedOperationException("!!start_byte_index must splited index")

        var position = startOffset + MAX_LOG_LINE_BYTE_SIZE
        if (byteLength <= position) return byteLength - startOffset

        while (startOffset <= position) {
            if (byteArray[position] and 0xc0.toByte() != 0x80.toByte()) break
            position--
        }
        if (position <= startOffset) throw UnsupportedOperationException("!!byte_length too small")
        return position - startOffset
    }

    private fun makeMessage(vararg args: Any?): String = args.map {
        when (it) {
            null -> "null"
            is JSONObject -> it.toString(2)
            is JSONArray -> it.toString(2)
            else -> dump(it.toString())
        }
    }.joinToString()

    private fun dump(text: String): String? = StringBuilder().runCatching {
        val s = text[0]
        val e = text[text.length - 1]
        val formattedText = when {
            s == '[' && e == ']' -> JSONArray(text).toString(2)
            s == '{' && e == '}' -> JSONObject(text).toString(2)
            else -> text
        }
        append(formattedText).toString()
    }.getOrDefault(text)


    private fun Headers.headerString(): String {
        val _in_h = StringBuilder()
        for (i in 0 until size)
            if (name(i) != SET_COOKIE)
                _in_h.divide.append(name(i) + ": " + value(i))
        return _in_h.toString()
    }

    private fun Headers.cookieString(): String {
        val _in_c = StringBuilder()
        for (i in 0 until size)
            if (name(i) == SET_COOKIE)
                _in_c.divide.append(name(i) + ": " + value(i))
        return _in_c.toString()
    }

    private fun Request.bodyString(): String {
        if (bodyHasUnknownEncoding(headers))
            return ""
        body ?: return ""

        val body = body!!
        val buffer = Buffer()
        body.writeTo(buffer)
        val contentType = body.contentType()
        var charset: Charset? = UTF8
        if (contentType != null)
            charset = contentType.charset(UTF8)

        return if (isPlaintext(buffer))
            buffer.clone().readString(charset!!)
        else
            "BODY_BINARY:[${body.contentLength()}]"
    }


    private fun Response.bodyString(): String {
        body ?: return ""

        if (bodyHasUnknownEncoding(headers))
            return ""

        val body = body!!

        val source = body.source()
        source.request(java.lang.Long.MAX_VALUE) // Buffer the entire body.
        var buffer = source.buffer

        if ("gzip".equals(headers["Content-Encoding"], ignoreCase = true)) {
            var gzippedResponseBody: GzipSource? = null
            try {
                gzippedResponseBody = GzipSource(buffer.clone())
                buffer = Buffer()
                buffer.writeAll(gzippedResponseBody)
            } finally {
                gzippedResponseBody?.close()
            }
        }

        val charset = body.contentType()?.charset(UTF8) ?: UTF8

        return if (body.contentLength() != 0L)
            buffer.clone().readString(charset)
        else
            ""
    }

    private fun bodyHasUnknownEncoding(headers: Headers): Boolean {
        val contentEncoding = headers.get("Content-Encoding")
        return (contentEncoding != null
            && !contentEncoding.equals("identity", ignoreCase = true)
            && !contentEncoding.equals("gzip", ignoreCase = true))
    }

    private val StringBuilder.divide: StringBuilder
        get() = if (isNotEmpty())
            append("\n")
        else
            this


    private fun isPlaintext(buffer: Buffer): Boolean = kotlin.runCatching {
        val prefix = Buffer()
        val byteCount = if (buffer.size < 64) buffer.size else 64
        buffer.copyTo(prefix, 0, byteCount)
        for (i in 0..15) {
            if (prefix.exhausted()) {
                break
            }
            val codePoint = prefix.readUtf8CodePoint()
            if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                return false
            }
        }
        return true
    }.getOrDefault(false)
}

