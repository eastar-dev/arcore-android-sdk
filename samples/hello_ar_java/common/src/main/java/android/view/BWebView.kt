@file:Suppress("unused", "UNUSED_PARAMETER")

package android.view

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.log.Log
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Looper
import android.os.Message
import android.provider.Browser
import android.text.InputType
import android.util.AttributeSet
import android.webkit.*
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import java.net.URISyntaxException
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

open class BWebView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : WebView(context, attrs, defStyleAttr) {
    interface WebActivityInterface {
        fun loadUrl(url: String?)
        fun sendJavascript(script: String?)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setWebSettings()
        setWebViewClient()
        setWebChromeClient()
        addJavascriptInterface()
    }

    @SuppressLint("SetJavaScriptEnabled")
    @TargetApi(Build.VERSION_CODES.KITKAT)
    protected fun setWebSettings() {
        onWebSettings(settings)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun onWebSettings(webSettings: WebSettings) {
        isFocusable = true
        isFocusableInTouchMode = true
        isVerticalScrollBarEnabled = true
        isHorizontalScrollBarEnabled = false
        webSettings.javaScriptEnabled = true
        webSettings.setSupportMultipleWindows(true)
        webSettings.domStorageEnabled = true
        webSettings.databaseEnabled = true
        mOnWebSettingsListener?.onWebSettings(webSettings)
    }

    open fun setWebViewClient() {
        webViewClient = BWebViewClient()
        mOnWebViewClientListener?.onWebViewClient(this)
    }

    open fun setWebChromeClient() {
        webChromeClient = BChromeClient()
        mOnWebChromeClientListener?.onWebChromeClient(this)
    }

    open fun addJavascriptInterface() {
        addViewSourceJavascriptInterface()
        addJsBackJavascriptInterface()
        mOnAddJavascriptListener?.onAddJavascript(this)
    }

    override fun loadUrl(url: String?) {
        Log.e(url)
        super.loadUrl(url)
    }

    override fun loadUrl(url: String?, additionalHttpHeaders: MutableMap<String, String>?) {
        Log.e(url, additionalHttpHeaders.toString())
        super.loadUrl(url, additionalHttpHeaders)
//        additionalHttpHeaders?.put("Referer", getUrl())
//        super.loadUrl(url, additionalHttpHeaders ?: mapOf<String, String?>("Referer" to getUrl()))
    }

    override fun postUrl(url: String?, postData: ByteArray?) {
        Log.e("===========================================================================")
        Log.e("== POST URL == ", url, postData.toString())
        Log.e("===========================================================================")
        super.postUrl(url, postData)
    }

    fun toggleSource() {
        if (findViewWithTag<View?>(SCROLL_SOURCE_VIEW) == null) {
            val context = context
            val tv = TextView(context)
            tv.tag = SOURCE_VIEW
            tv.setTextColor(Color.RED)
            val sv = ScrollView(context)
            sv.tag = SCROLL_SOURCE_VIEW
            sv.addView(tv)
            addView(sv)
        } else {
            val sv = findViewWithTag<View>(SCROLL_SOURCE_VIEW)
            sv.visibility = GONE - sv.visibility
        }
        if (findViewWithTag<View>(SCROLL_SOURCE_VIEW).visibility == VISIBLE) sendJavascript("$SOURCE.viewSource(document.documentElement.outerHTML);")
    }

    @SuppressLint("AddJavascriptInterface")
    private fun addViewSourceJavascriptInterface() {
        addJavascriptInterface(object : Any() {
            @JavascriptInterface
            fun viewSource(source: String?) {
                //NOT MAIN THREAD
                post { (findViewWithTag<View>(SOURCE_VIEW) as TextView).text = source }
            }
        }, SOURCE)
    }

    private val builder = StringBuilder()
    fun toggleConsoleLog() {
        if (findViewWithTag<View?>(SCROLL_LOG_VIEW) == null) {
            val context = context
            val tv = TextView(context)
            tv.tag = LOG_VIEW
            tv.setTextColor(Color.BLUE)
            val sv = ScrollView(context)
            sv.isFillViewport = true
            sv.tag = SCROLL_LOG_VIEW
            sv.addView(tv)
            sv.setBackgroundColor(0x55ff0000)
            addView(sv, -1, -1)
        } else {
            val sv = findViewWithTag<View>(SCROLL_LOG_VIEW)
            sv.visibility = GONE - sv.visibility
        }
        if (findViewWithTag<View>(SCROLL_LOG_VIEW).visibility == VISIBLE) {
            val millis = System.currentTimeMillis()
            consoleLog(">>SHOW{$millis}")
        }
    }

    private fun consoleLog(text: CharSequence?) {
        builder.insert(0, '\n')
        builder.insert(0, text)
        builder.setLength(MAX_LOG_LENGTH)
        val sv = findViewWithTag<ScrollView>(SCROLL_LOG_VIEW)
        sv?.post { (findViewWithTag<View>(LOG_VIEW) as TextView).text = builder.toString() }
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////
    private var mSemaphore: Semaphore? = null
    private var mConsumeJsBack = false
    private var mJavascriptFunctionNameForHWBackkey: String? = null
    fun setJavascriptFunctionNameForHWBackkey(javascriptFunctionNameForHWBackkey: String) {
        mJavascriptFunctionNameForHWBackkey = javascriptFunctionNameForHWBackkey
    }

    fun onBackPressedJavascriptFunction(): Boolean {
        Log.e("onBackPressedEx", mJavascriptFunctionNameForHWBackkey)
        if (mJavascriptFunctionNameForHWBackkey.isNullOrEmpty()) {
            return false
        }
        mSemaphore = Semaphore(0)
        val javascriptBack = String.format(
            "" //
                + "var b = false;" //
                + "try { b = (typeof %1\$s == 'function'); } catch (e) { b = false; }" //
                + HWBACK + ".setJsBackResult(b);" //
                + "if(b){" //
                + "   %1\$s();" //
                + "}" //
            , mJavascriptFunctionNameForHWBackkey
        )
        //        Log.e(javascriptBack);
        sendJavascript(javascriptBack)
        try {
            mSemaphore?.tryAcquire(1, TimeUnit.SECONDS)
            Log.i("consumeJsBack", mConsumeJsBack)
            return mConsumeJsBack
        } catch (e: InterruptedException) {
            Log.printStackTrace(e)
        }
        return false
    }

    @SuppressLint("AddJavascriptInterface")
    private fun addJsBackJavascriptInterface() {
        Log.e("addJsBackJavascriptInterface", HWBACK)
        addJavascriptInterface(object : Any() {
            @JavascriptInterface
            fun setJsBackResult(consumeJsBack: Boolean) {
//                Log.i("consumeJsBack", consumeJsBack);
                mConsumeJsBack = consumeJsBack
                mSemaphore!!.release()
            }
        }, HWBACK)
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////
    fun sendJavascript(script: String) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            post { sendJavascript(script) }
            return
        }
        Log.pc(Log.ERROR, "evaluateJavascript", script)
        evaluateJavascript(script, null)
    }

    fun onBackPressedEx(): Boolean {
        if (findViewWithTag<View>(SCROLL_SOURCE_VIEW) != null && findViewWithTag<View>(SCROLL_SOURCE_VIEW).visibility == VISIBLE) {
            toggleSource()
            return true
        }
        if (findViewWithTag<View>(SCROLL_LOG_VIEW) != null && findViewWithTag<View>(SCROLL_LOG_VIEW).visibility == VISIBLE) {
            toggleConsoleLog()
            return true
        }

//        if (onBackPressedJavascriptFunction()) {
//            return true
//        }
//
//        if (historyBack()) {
//            return true;
//        }
        return false
    }

    fun historyBack(): Boolean {
//        copyBackForwardList()
        val canGoBack = canGoBack()
        if (canGoBack) goBack()
        return canGoBack
    }


    //////////////////////////////////////////////////////////////////////////////////////////////
    open fun isInternal(view: WebView, url: String): Boolean {
        return url.matches("^(https?)://.*".toRegex())
    }

    fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
        consoleLog("shouldOverrideUrlLoading$url")
        //        Log.e(view.getTitle(), url);
        if (url.isEmpty())
            return false
        if (url.startsWith("android-app:") || url.startsWith("intent:") || url.startsWith("#Intent;")) {
            var intent: Intent? = null
            try {
                intent = Intent.parseUri(url, 0)
                val browserId = intent.getStringExtra(Browser.EXTRA_APPLICATION_ID)
                if (intent.action != null && intent.action == Intent.ACTION_VIEW && browserId.isNullOrEmpty())
                    intent.putExtra(Browser.EXTRA_APPLICATION_ID, APPLICATION_ID)
                view.context.startActivity(intent)
                return true
            } catch (e: URISyntaxException) {
                Log.printStackTrace(e)
            } catch (e: ActivityNotFoundException) {
                Log.w(e.message, intent)
                if (intent != null) {
                    val packageName = intent.getPackage()
                    if (packageName.isNullOrEmpty()) return false
                    view.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
                    Log.i("!this package intent is auto move to market ")
                }
                return true
            }
        }
        if (isInternal(view, url)) {
            view.loadUrl(url)
            return true
        }

        //!ERROR_UNSUPPORTED_SCHEME
        try {
            view.context.startActivity(Intent.parseUri(url, 0))
            return true
        } catch (e: URISyntaxException) {
            Log.printStackTrace(e)
        } catch (e: ActivityNotFoundException) {
            Log.w(e.message)
            return true
        }
        return false
    }

    fun onReceivedError(view: WebView, errorCode: Int, description: String, failingUrl: String) {
        when (errorCode) {
            WebViewClient.ERROR_AUTHENTICATION -> Log.w("!ERROR_AUTHENTICATION")
            WebViewClient.ERROR_BAD_URL -> Log.w("!ERROR_BAD_URL")
            WebViewClient.ERROR_CONNECT -> Log.w("!ERROR_CONNECT")
            WebViewClient.ERROR_FAILED_SSL_HANDSHAKE -> Log.w("!ERROR_FAILED_SSL_HANDSHAKE")
            WebViewClient.ERROR_FILE -> Log.w("!ERROR_FILE")
            WebViewClient.ERROR_FILE_NOT_FOUND -> Log.w("!ERROR_FILE_NOT_FOUND")
            WebViewClient.ERROR_HOST_LOOKUP -> Log.w("!ERROR_HOST_LOOKUP")
            WebViewClient.ERROR_IO -> Log.w("!ERROR_IO")
            WebViewClient.ERROR_PROXY_AUTHENTICATION -> Log.w("!ERROR_PROXY_AUTHENTICATION")
            WebViewClient.ERROR_REDIRECT_LOOP -> Log.w("!ERROR_REDIRECT_LOOP")
            WebViewClient.ERROR_TOO_MANY_REQUESTS -> Log.w("!ERROR_TOO_MANY_REQUESTS")
            WebViewClient.ERROR_UNKNOWN -> Log.w("!ERROR_UNKNOWN")
            WebViewClient.ERROR_UNSUPPORTED_AUTH_SCHEME -> Log.w("!ERROR_UNSUPPORTED_AUTH_SCHEME")
            WebViewClient.ERROR_UNSUPPORTED_SCHEME -> Log.w("!ERROR_UNSUPPORTED_SCHEME")
            WebViewClient.ERROR_TIMEOUT -> Log.w("!ERROR_TIMEOUT")
            else -> Log.w("OK")
        }
    }

    fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
//        Log.pm(Log.ERROR, "onPageStarted", ">WEB:S>", view.getTitle(), url);
    }

    fun onPageFinished(view: WebView, url: String) {
//        Log.pm(Log.WARN, "onPageFinished", ">WEB:E>", view.getTitle(), url);
    }

    fun onLoadResource(view: WebView, url: String) {
//		Log.v(url);
    }

    /////////////////////////////////////////////////////////////////////////
    fun onProgressChanged(view: WebView, newProgress: Int) {}

    /////////////////////////////////////////////////////////////////////////
    fun onJsAlert(view: WebView, url: String, message: String, result: JsResult): Boolean {
        Log.e(view.title, url)
        Log.e(message)
        AlertDialog.Builder(view.context)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { _, _ -> result.confirm() }
            .setOnCancelListener { result.cancel() }
            .create()
            .show()
        return true
    }

    fun onJsConfirm(view: WebView, url: String, message: String, result: JsResult): Boolean {
        Log.e(view.title, url)
        Log.e(message)
        AlertDialog.Builder(view.context)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { _, _ -> result.confirm() }
            .setNegativeButton(android.R.string.cancel) { _, _ -> result.cancel() }
            .setOnCancelListener { result.cancel() }
            .create()
            .show()
        return true
    }

    private fun onJsPrompt(view: WebView, url: String, message: String, defaultValue: String, result: JsPromptResult): Boolean {
        Log.e(view.title, url)
        Log.e(message)
        val input = EditText(view.context)
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.setText(defaultValue)
        AlertDialog.Builder(view.context)
            .setView(input)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { _, _ -> result.confirm(input.text.toString()) }
            .setNegativeButton(android.R.string.cancel) { _, _ -> result.cancel() }
            .setOnCancelListener { result.cancel() }
            .create()
            .show()
        return false
    }

    fun onReceivedTitle(view: WebView, title: String) {
//        if (!(view.getContext() instanceof Activity))
//            ((Activity)view.getContext()).setTitle(title);
    }

    private fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {}
    private fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
        val log = consoleMessage.message()
        val tag = consoleMessage.sourceId() + "#" + consoleMessage.lineNumber()
//        val level = consoleMessage.messageLevel()
        consoleLog("$log::$tag")
        return true
    }

    interface OnWebSettingsListener {
        fun onWebSettings(settings: WebSettings)
    }

    interface OnWebViewClientListener {
        fun onWebViewClient(webview: WebView)
    }

    interface OnWebChromeClientListener {
        fun onWebChromeClient(webview: WebView)
    }

    interface OnAddJavascriptListener {
        fun onAddJavascript(webview: WebView)
    }

    fun setOnWebSettingsListener(onWebSettingsListener: OnWebSettingsListener) {
        mOnWebSettingsListener = onWebSettingsListener
    }

    fun setOnWebViewClientListener(onWebViewClientListener: OnWebViewClientListener) {
        mOnWebViewClientListener = onWebViewClientListener
    }

    fun setOnWebChromeClientListener(onWebChromeClientListener: OnWebChromeClientListener) {
        mOnWebChromeClientListener = onWebChromeClientListener
    }

    fun setOnAddJavascriptListener(onAddJavascriptListener: OnAddJavascriptListener) {
        mOnAddJavascriptListener = onAddJavascriptListener
    }

    private var mOnWebSettingsListener: OnWebSettingsListener? = null
    private var mOnWebViewClientListener: OnWebViewClientListener? = null
    private var mOnWebChromeClientListener: OnWebChromeClientListener? = null
    private var mOnAddJavascriptListener: OnAddJavascriptListener? = null

    fun setShouldOverrideUrlLoading(shouldOverrideUrlLoading: ShouldOverrideUrlLoading) {
        mShouldOverrideUrlLoading = shouldOverrideUrlLoading
    }

    fun setOnLoadResource(onLoadResource: OnLoadResource) {
        mOnLoadResource = onLoadResource
    }

    fun setOnPageStarted(onPageStarted: OnPageStarted) {
        mOnPageStarted = onPageStarted
    }

    fun setOnPageFinished(onPageFinished: OnPageFinished) {
        mOnPageFinished = onPageFinished
    }

    fun setOnReceivedSslError(onReceivedSslError: OnReceivedSslError) {
        mOnReceivedSslError = onReceivedSslError
    }

    fun setOnReceivedError(onReceivedError: OnReceivedError) {
        mOnReceivedError = onReceivedError
    }

    fun setOnConsoleMessage(onConsoleMessage: OnConsoleMessage) {
        mOnConsoleMessage = onConsoleMessage
    }

    fun setOnProgressChanged(onProgressChanged: OnProgressChanged) {
        mOnProgressChanged = onProgressChanged
    }

    fun setOnReceivedTitle(onReceivedTitle: OnReceivedTitle) {
        mOnReceivedTitle = onReceivedTitle
    }

    fun setOnJsAlert(onJsAlert: OnJsAlert) {
        mOnJsAlert = onJsAlert
    }

    fun setOnJsConfirm(onJsConfirm: OnJsConfirm) {
        mOnJsConfirm = onJsConfirm
    }

    fun setOnJsPrompt(onJsPrompt: OnJsPrompt) {
        mOnJsPrompt = onJsPrompt
    }

    interface ShouldOverrideUrlLoading {
        fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean
    }

    interface OnLoadResource {
        fun onLoadResource(view: WebView, url: String)
    }

    interface OnPageStarted {
        fun onPageStarted(view: WebView, url: String, favicon: Bitmap?)
    }

    interface OnPageFinished {
        fun onPageFinished(view: WebView, url: String)
    }

    interface OnReceivedSslError {
        fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError)
    }

    interface OnReceivedError {
        fun onReceivedError(view: WebView, errorCode: Int, description: String, failingUrl: String)
    }

    interface OnConsoleMessage {
        fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean
    }

    interface OnProgressChanged {
        fun onProgressChanged(view: WebView, newProgress: Int)
    }

    interface OnReceivedTitle {
        fun onReceivedTitle(view: WebView, title: String)
    }

    interface OnJsAlert {
        fun onJsAlert(view: WebView, url: String, message: String, result: JsResult): Boolean
    }

    interface OnJsConfirm {
        fun onJsConfirm(view: WebView, url: String, message: String, result: JsResult): Boolean
    }

    interface OnJsPrompt {
        fun onJsPrompt(view: WebView, url: String, message: String, defaultValue: String, result: JsPromptResult): Boolean
    }

    private var mShouldOverrideUrlLoading: ShouldOverrideUrlLoading? = null
    private var mOnLoadResource: OnLoadResource? = null
    private var mOnPageStarted: OnPageStarted? = null
    private var mOnPageFinished: OnPageFinished? = null
    private var mOnReceivedSslError: OnReceivedSslError? = null
    private var mOnReceivedError: OnReceivedError? = null
    private var mOnConsoleMessage: OnConsoleMessage? = null
    private var mOnProgressChanged: OnProgressChanged? = null
    private var mOnReceivedTitle: OnReceivedTitle? = null
    private var mOnJsAlert: OnJsAlert? = null
    private var mOnJsConfirm: OnJsConfirm? = null
    private var mOnJsPrompt: OnJsPrompt? = null

    inner class BWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            return mShouldOverrideUrlLoading?.shouldOverrideUrlLoading(view, url) ?: this@BWebView.shouldOverrideUrlLoading(view, url)
        }

        override fun onLoadResource(view: WebView, url: String) {
            mOnLoadResource?.onLoadResource(view, url) ?: this@BWebView.onLoadResource(view, url)
        }

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            mOnPageStarted?.onPageStarted(view, url, favicon) ?: this@BWebView.onPageStarted(view, url, favicon)
        }

        override fun onPageFinished(view: WebView, url: String) {
            mOnPageFinished?.onPageFinished(view, url) ?: this@BWebView.onPageFinished(view, url)
        }

        override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
            mOnReceivedSslError?.onReceivedSslError(view, handler, error) ?: this@BWebView.onReceivedSslError(view, handler, error)
        }

        override fun onReceivedError(view: WebView, errorCode: Int, description: String, failingUrl: String) {
            mOnReceivedError?.onReceivedError(view, errorCode, description, failingUrl) ?: this@BWebView.onReceivedError(view, errorCode, description, failingUrl)
        }
    }

    inner class BChromeClient : WebChromeClient() {
        override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
            return mOnConsoleMessage?.onConsoleMessage(consoleMessage) ?: this@BWebView.onConsoleMessage(consoleMessage)
        }

        override fun onProgressChanged(view: WebView, newProgress: Int) {
            mOnProgressChanged?.onProgressChanged(view, newProgress) ?: this@BWebView.onProgressChanged(view, newProgress)
        }

        override fun onReceivedTitle(view: WebView, title: String) {
            mOnReceivedTitle?.onReceivedTitle(view, title) ?: this@BWebView.onReceivedTitle(view, title)
        }

        override fun onJsAlert(view: WebView, url: String, message: String, result: JsResult): Boolean {
            return mOnJsAlert?.onJsAlert(view, url, message, result) ?: this@BWebView.onJsAlert(view, url, message, result)
        }

        override fun onJsConfirm(view: WebView, url: String, message: String, result: JsResult): Boolean {
            return mOnJsConfirm?.onJsConfirm(view, url, message, result) ?: this@BWebView.onJsConfirm(view, url, message, result)
        }

        override fun onJsPrompt(view: WebView, url: String, message: String, defaultValue: String, result: JsPromptResult): Boolean {
            return mOnJsPrompt?.onJsPrompt(view, url, message, defaultValue, result) ?: this@BWebView.onJsPrompt(view, url, message, defaultValue, result)
        }

        /////////////////////////////////////////////////////////////////////////////////////////////////////////
        //etc////////////////////////////////////////////////////////////////////////////////////////////////////
        /////////////////////////////////////////////////////////////////////////////////////////////////////////
        override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message): Boolean {
            return this@BWebView.onCreateWindow(view, isDialog, isUserGesture, resultMsg)
        }

        override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
            this@BWebView.onGeolocationPermissionsShowPrompt(origin, callback)
        }
    }

    fun onGeolocationPermissionsShowPrompt(origin: String?, callback: GeolocationPermissions.Callback) {
        //https://play.google.com/apps/publish/?account=8841149513553108353#AndroidMetricsErrorsPlace:p=com.kebhana.hanapush&appid=4976086679178587985&appVersion=PRODUCTION&clusterName=apps/com.kebhana.hanapush/clusters/bffa9a37&detailsAppVersion=PRODUCTION&detailsSpan=7
        callback.invoke(origin, false, false)
        //        if (getContext() instanceof BActivity) {
//            BActivity ba = (BActivity) getContext();
//            ba.showDialog(origin + " 에서 위치정보를 사용하려 합니다." //
//                    , "승인", (dialog, which) -> callback.invoke(origin, true, true)//
//                    , "이번만", (dialog, which) -> callback.invoke(origin, true, false)//
//                    , "불가", (dialog, which) -> callback.invoke(origin, false, false)//
//            );
//        }
    }

    private fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message): Boolean {
        Log.e(view, isDialog, isUserGesture, resultMsg)
        val newWebView = WebView(view.context)
        val transport = resultMsg.obj as WebViewTransport
        transport.webView = newWebView
        resultMsg.sendToTarget()
        return true
    }


    companion object {
        protected const val APPLICATION_ID = "webapp"

        /////////////////////////////////////////////////////////////////////////////////////////////////
        //source----------------------------------------------------------------------------
        private const val SOURCE = "SOURCE"
        private const val SOURCE_VIEW = "SOURCE_VIEW"
        private const val SCROLL_SOURCE_VIEW = "SCROLL_SOURCE_VIEW"

        //colsoleLog-----------------------------------------------------------------------
        private const val LOG_VIEW = "LOG_VIEW"
        private const val SCROLL_LOG_VIEW = "SCROLL_LOG_VIEW"
        private const val MAX_LOG_LENGTH = 300000
        private const val HWBACK = "HWBACK"
        const val EXTRA_URL = "url"
    }
}
