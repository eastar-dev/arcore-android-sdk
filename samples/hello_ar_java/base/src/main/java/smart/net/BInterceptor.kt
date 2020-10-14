package smart.net

import android.log.Log
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject
import smart.auth.Session

class BInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response =
        kotlin.runCatching {
            val chainRequest = chain.request()
            val request = chainRequest.newBuilder().apply {
                addHeader(BUserAgent.USER_AGENT_KEY, BUserAgent.USER_AGENT)
                addHeader("Accept", "application/json")
            }.build()

            val response = intercept(chain.proceed(request))
            Session.startSessionTimeout()
            response
        }.getOrThrow()


    private fun intercept(response: Response): Response {
        val body = response.body?.string()
        Log.e(body)
        body?.let {
            var rowJson = JSONObject(it)
            rowJson = rowJson.optJSONObject("resultData") ?: rowJson
            val status = rowJson.optInt("status", 5000)
            val message = rowJson.optString("message", "")

            rowJson = rowJson.optJSONObject("result") ?: rowJson
            Log.w(rowJson)
            val mediaType = response.body?.contentType()
            return response.newBuilder()
                .body(rowJson.toString().toResponseBody(mediaType))
                .code(status)
                .message(message)
                .build()
        } ?: run {
            return response
        }
    }
}
