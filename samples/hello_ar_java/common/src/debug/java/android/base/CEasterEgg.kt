@file:Suppress("NonAsciiCharacters", "NonAsciiCharacters")

package android.base

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.log.Log
import android.view.BWebView
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import androidx.core.content.pm.PackageInfoCompat
import com.google.firebase.iid.FirebaseInstanceId
import dev.eastar.ktx.*
import java.lang.reflect.InvocationTargetException
import kotlin.system.exitProcess


var systemWindowInsetTop: Int = 0


fun Application.easterEgg() = registerActivityStartedLifecycleCallbacks { easterEgg() }

@SuppressLint("SetTextI18n")
private fun Activity.easterEgg() {
    val versionTag = "show_me_the_money"
    val appEasterEgg = "android.etc.AppEasterEgg"
    val bEasterEgg = "smart.base.BEasterEgg"
    val cEasterEgg = CEasterEgg::class.java.name

    val parent = findViewById<ViewGroup>(android.R.id.content)
    if (parent.findViewWithTag<View>(versionTag) != null)
        return
    val ver = TextView(this)
    ver.setOnApplyWindowInsetsListener { _, insets ->
//        systemWindowInsetTop = insets.getInsets(WindowInsets.Type.STATUS_BARS)
        systemWindowInsetTop = insets.systemWindowInsetTop
        insets
    }
    ver.viewTreeObserver.addOnGlobalLayoutListener(
        object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                ver.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val locationInWindow = IntArray(2)
                ver.getLocationInWindow(locationInWindow)

                if (locationInWindow[1] == 0) {
                    Log.e(" ver.y", locationInWindow[1], "->", systemWindowInsetTop)
                    ver.y = systemWindowInsetTop.toFloat()
                }
            }
        }
    )

    val server = CN.getLastServer(this)
    val versionCode = PackageInfoCompat.getLongVersionCode(packageManager.getPackageInfo(packageName, 0))
    val versionName = packageManager.getPackageInfo(packageName, 0).versionName

    ver.text = "[$versionCode][$server][v$versionName]"
    ver.tag = versionTag
    ver.setTextColor(0x55ff0000)
    ver.setBackgroundColor(0x5500ff00)
    ver.textSize = 14f//sp
    val height = -2// TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 35f, resources.displayMetrics).toInt()
    parent.addView(ver, ViewGroup.LayoutParams.WRAP_CONTENT, height)

    val appFunc = getFunc(appEasterEgg)
    val bFunc = getFunc(bEasterEgg)
    val cFunc = getFunc(cEasterEgg)
    val func = (appFunc + bFunc + cFunc).toTypedArray()

    ver.setOnClickListener {
        AlertDialog.Builder(this)
            .setItems(func) { dialog, which ->
                val funcName = (dialog as AlertDialog).listView.getItemAtPosition(which) as String
                kotlin.runCatching {
                    runFunc(appEasterEgg, funcName, this)
                }.recoverCatching {
                    if (it is NoSuchMethodException)
                        runFunc(bEasterEgg, funcName, this)
                }.recoverCatching {
                    if (it is NoSuchMethodException)
                        runFunc(cEasterEgg, funcName, this)
                }
            }
            .show()
    }
}

private fun getFunc(clz: String): List<String> = runCatching {
    Class.forName(clz).run {
        methods.filter { it.declaringClass == this }
            .filter { it.returnType == Void.TYPE }
            .filterNot { it.name.contains("$") }
            .map { it.name }
    }
}.getOrDefault(emptyList())

fun runFunc(clz: String, funcName: String, activity: Activity) = runCatching {
    val clazz = Class.forName(clz)
    val method = clazz.getMethod(funcName)
    val constructor = clazz.getConstructor(Activity::class.java)
    val receiver = constructor.newInstance(activity)
    method.invoke(receiver)
    true
}.onFailure {
    if (it is InvocationTargetException)
        it.printStackTrace()
}.getOrThrow()

@Suppress("SpellCheckingInspection", "FunctionName", "unused")
class CEasterEgg(val activity: Activity) {

    fun _TEST() {
        activity.runCatching { javaClass.getMethod("test").invoke(this) }
        (activity as AppCompatActivity).supportFragmentManager.fragments
            .forEach {
                it.runCatching { javaClass.getMethod("test").invoke(this) }
            }
    }


    fun A_DEV_SERVER() = CHANGE_SERVER(CN.DEV)
    fun A_DEV2_SERVER() = CHANGE_SERVER(CN.DEV2)
    fun A_STG_SERVER() = CHANGE_SERVER(CN.STG)
    fun A_REAL_SERVER() = CHANGE_SERVER(CN.REAL)

    fun APPLICATION_DETAILS_SETTINGS() = setting()

    fun PUSH_TOKEN() {
        activity.runCatching {
            FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener(this) { instanceIdResult ->
                val token = instanceIdResult.token
                val clipboardManager = getSystemService<ClipboardManager>()
                val clip = ClipData.newPlainText("label", token)
                clipboardManager?.setPrimaryClip(clip)
                toast(clip.getItemAt(0).text.toString() + " 복사되었습니다.")
            }
        }
    }

    fun 다시_보지않음_초기화() = NoMore.clear(activity)

    fun _MAIN() = (activity as CActivity).main()

    fun 소스보기() {
        val resId = activity.getResId("webview", "id", activity.packageName)
        val webview = activity.findViewById<BWebView>(resId)
        webview.toggleSource()
    }

    fun 콘솔보기() {
        val resId = activity.getResId("webview", "id", activity.packageName)
        val webview = activity.findViewById<BWebView>(resId)
        webview.toggleConsoleLog()
    }

    fun AllActivity() {
        val list = activity.packageManager.getPackageInfo(activity.packageName, PackageManager.GET_ACTIVITIES).activities
        val items = list
            .filter { it.name.startsWith("com.hana.") || it.name.startsWith("com.opera.") }
            .filterNot { it.name == javaClass.name }
            .map { it.name }
            .toTypedArray()

        AlertDialog.Builder(activity)
            .setItems(items) { dialog, which ->
                try {
                    val item = (dialog as AlertDialog).listView.getItemAtPosition(which) as String
                    activity.startActivity(Intent().setClassName(activity, item))
                } catch (e: Exception) {
                    Log.printStackTrace(e)
                }
            }
            .show()
    }

    private fun toast(text: CharSequence) = Toast.makeText(activity, text, Toast.LENGTH_LONG).show()
    private fun setting() = activity.startSetting()
    private fun CHANGE_SERVER(server: CN) {
        val before = CN.getLastServer(activity)
        CN.setLastServer(activity, server)
        val after = CN.getLastServer(activity)
        toast("$before->$after")

        activity.finishAffinity() // Finishes all activities.
        activity.startActivity(activity.packageManager.getLaunchIntentForPackage(activity.packageName)) // Start the launch activity
        exitProcess(0) // System finishes and automatically relaunches us.
    }
}
