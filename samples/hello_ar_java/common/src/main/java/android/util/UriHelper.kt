@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package android.util

import android.net.Uri

class UriHelper(url: String?) {
    private val mScheme: String?
    private var mUserInfo: String?
    private val mHost: String?
    private val mPort: Int
    private var mPath: String?
    private var mQuery: String?
    private var mFragment: String?

    init {
        with(Uri.parse(url)) {
            mScheme = scheme
            mUserInfo = encodedUserInfo
            mHost = host
            mPort = port
            mPath = encodedPath
            mQuery = encodedQuery
            mFragment = encodedFragment
            if (mUserInfo.isNullOrBlank()) mUserInfo = ""
            if (mQuery.isNullOrBlank()) mQuery = ""
            if (mPath.isNullOrBlank()) mPath = ""
            if (mFragment.isNullOrBlank()) mFragment = ""
        }
    }

    fun toAuthority(): String {
        val sb = StringBuilder()
        sb.append(mScheme).append("://")
        if (!mUserInfo.isNullOrBlank()) sb.append(mUserInfo).append('@')
        if (!mHost.isNullOrBlank()) sb.append(mHost)
        if (mPort > 0) sb.append(':').append(mPort)
        return sb.toString()
    }

    fun toPath(): String {
        val sb = StringBuilder()
        if (!mPath.isNullOrBlank()) sb.append(mPath)
        if (!mQuery.isNullOrBlank()) sb.append('?').append(mQuery)
        if (!mFragment.isNullOrBlank()) sb.append('#').append(mFragment)
        return sb.toString()
    }

    fun toUrl(): String = toAuthority() + toPath()

    fun toUri(): Uri = Uri.parse(toUrl())

    fun setEncodedPath(path: String?) {
        mPath = path
    }

    fun setEncodedQuery(query: String?) {
        mQuery = query
    }

    fun setEncodedFragment(fragment: String?) {
        mFragment = fragment
    }

    /////////////////////////////////////////
    fun addEncodedQuery(query: String) {
        mQuery += "$query&"
    }

    fun addQuery(key: String, value: String) {
        addEncodedQuery(Uri.encode(key) + "=" + Uri.encode(value))
    }

    companion object {
        fun urlEncode(url: String, charset: String?): String {
            val bytes = url.toByteArray(charset(charset!!))
            val sb = StringBuilder(bytes.size)
            for (i in bytes.indices) {
                val cp = if (bytes[i] < 0) bytes[i] + 256 else bytes[i].toInt()
                if (cp <= 0x20 || cp >= 0x7F || cp == 0x22 || cp == 0x25 || cp == 0x3C || cp == 0x3E || cp == 0x20 || cp == 0x5B || cp == 0x5C || cp == 0x5D || cp == 0x5E || cp == 0x60 || cp == 0x7b || cp == 0x7c || cp == 0x7d) {
                    sb.append(String.format("%%%02X", cp))
                } else {
                    sb.append(cp.toChar())
                }
            }
            return sb.toString()
        }

        fun urlEncode(url: String): String {
            val s = url.indexOf("?")
            var e = url.indexOf("#")
            if (e < 0) e = url.length
            val sb = StringBuilder()
            val queryText = url.substring(s, e)
            val query = queryText.split("&").toTypedArray()
            for (query in query) {
                var d = query.indexOf("=")
                if (d < 0) d = query.length
                val key = query.substring(0, d)
                val value = query.substring(d, query.length)
                sb.append(Uri.encode(key)).append("=").append(Uri.encode(value)).append("&")
            }
            return url.substring(0, s) + sb.toString() + url.substring(e)
        }
    }
}

val Uri?.afterPath: String
    get() = if (this == null) "" else "$encodedPath?$encodedQuery#$encodedFragment"

