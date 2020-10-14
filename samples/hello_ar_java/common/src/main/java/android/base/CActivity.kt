package android.base

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.drawable.Drawable
import android.log.Log
import android.log.LogActivity
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.lifecycleScope
import dev.eastar.ktx.startMain
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

abstract class CActivity : LogActivity() {
    val mContext: Context by lazy { this }
    val mActivity: CActivity by lazy { this }

    @SuppressLint("SourceLockedOrientationActivity")
    protected open fun setRequestedOrientation() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
    }

    override fun setRequestedOrientation(requestedOrientation: Int) {
        kotlin.runCatching { super.setRequestedOrientation(requestedOrientation) }
    }

    protected open fun setSoftInputMode() {
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    /** onParseExtra() -> onLoadOnce() -> onReload() -> onClear() -> onLoad() -> onUpdateUI() */
    override fun onCreate(savedInstanceState: Bundle?) {
        setRequestedOrientation()
        setSoftInputMode()
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun autoDisposable() {
                disposables
                    .filterNot { it.isDisposed }
                    .forEach { it.dispose() }
            }
        })
    }

    var disposables = mutableSetOf<Disposable>()
    fun Disposable.autoDispose() = this.also { disposables.add(it) }

    //progress/////////////////////////////////////////////////////////////////////////////
    val mProgressDlg: DialogFragment by lazy { createProgress() }
    open fun createProgress(): DialogFragment = CProgressDialogFragment()

    class CProgressDialogFragment : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            return AlertDialog.Builder(requireContext())
                .setView(ProgressBar(context, null, android.R.attr.progressBarStyleLarge))
                .create().apply {
                    window?.apply {
                        clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                        setBackgroundDrawableResource(android.R.color.transparent)
                    }
                    setCanceledOnTouchOutside(false)
                    setCancelable(true)
                }
        }
    }

    private var s = 0L
    private val interval: Long
        get() = (if (s == 0L) 0L else System.currentTimeMillis() - s).also { s = System.currentTimeMillis() }
    private var mProgress = 0
    fun showProgress() {
        Log.pc(Log.ERROR, "showProgress", "~~", mProgress + 1, interval)
        if (mProgress++ > 0) {
            return
        }

        if (mProgressDlg.isAdded)
            return

        mProgressDlg.showNow(supportFragmentManager, "progress")
    }

    fun dismissProgress() {
        lifecycleScope.launch {
            Log.pc(Log.WARN, "dismissProgress", "~~", mProgress - 1, interval)
            delay(500)
            Log.w("~~", mProgress - 1, interval)
            if (--mProgress > 0) {
                return@launch
            }
            mProgressDlg.dismissAllowingStateLoss()
        }
    }

    //main exit/////////////////////////////////////////////////////////////////
    open fun main() {
        Log.e(javaClass, "public void main()")
        startMain()
    }

    open fun exit() {
        Log.e(javaClass, "public void exit()")
        finish()
    }


    //clicked////////////////////////////////////////////////////////////////////
    protected open fun onBackPressedEx(): Boolean {
        for (fragment in supportFragmentManager.fragments) {
            if (fragment is CFragment) {
                if (fragment.onBackPressedEx()) return true
            }
        }
        return false
    }

    override fun onBackPressed() {
        if (onBackPressedEx()) return
        super.onBackPressed()
    }

//    open fun onHeaderBack(v: View?) {
//        Log.d(javaClass, "onHeaderBack")
//        onBackPressed()
//    }
//
//    open fun onHeaderMain(v: View?) {
//        Log.d(javaClass, "onHeaderMain")
//        main()
//    }
//
//    open fun onHeaderTitle(v: View?) = Log.d(javaClass, "onHeaderTitle")
//    open fun onHeaderLogin(v: View?) = Log.d(javaClass, "onHeaderLogin")
//    open fun onHeaderMenu(v: View?) = Log.d(javaClass, "onHeaderMenu")
//    open fun onHeaderLeft(v: View?) = Log.d(javaClass, "onHeaderLeft")
//    open fun onHeaderRight(v: View?) = Log.d(javaClass, "onHeaderRight")

    //fragment/////////////////////////////////////////////////////////////////////////////////////////
//    var mFragmentManager = supportFragmentManager
//    protected fun setFragmentVisible(resid_fragment: Int, b: Boolean) {
//        val fr = mFragmentManager.findFragmentById(resid_fragment)
//        setFragmentVisible(fr, b)
//    }
//
//    protected fun setFragmentVisible(fr: Fragment?, b: Boolean) {
//        val ft = mFragmentManager.beginTransaction()
//        if (b == true) ft.show(fr!!) else ft.hide(fr!!)
//        ft.commitAllowingStateLoss()
//    }

    fun toast(text: Any?) {
        Toast.makeText(mContext, getText(text), Toast.LENGTH_SHORT).show()
    }

    private fun getDrawable(drawable: Any?): Drawable? {
        if (drawable == null) return null
        return if (drawable is Drawable) drawable else ResourcesCompat.getDrawable(mContext.resources, (drawable as Int?)!!, null)
    }

    private fun getText(text: Any?): CharSequence? {
        if (text == null) return null
        if (text is CharSequence) return text
        return if (text is Int) mContext.getString((text as Int?)!!) else text.toString()
    }

    @JvmOverloads
    fun showDialogSticky(
        message: Any?,
        positiveButtonText: Any?,
        positiveListener: DialogInterface.OnClickListener? = null,
        negativeButtonText: Any? = null,
        negativeListener: DialogInterface.OnClickListener? = null,
    ): AlertDialog {
        val dlg = showDialog(
            message, positiveButtonText, positiveListener, negativeButtonText, negativeListener
        )
        dlg.setCancelable(false)
        return dlg
    }

    @JvmOverloads
    fun showDialog(
        message: Any?,
        positiveButtonText: Any?,
        positiveListener: DialogInterface.OnClickListener? = null,
        negativeButtonText: Any? = null,
        negativeListener: DialogInterface.OnClickListener? = null
    ): AlertDialog {
        val context = mContext
        val dlg = getDialog(context, message, positiveButtonText, positiveListener, negativeButtonText, negativeListener)
        dlg.setCanceledOnTouchOutside(false)
        if (lifecycle.currentState == Lifecycle.State.DESTROYED) {
            Log.w("activity is Lifecycle destroyed")
            return dlg
        }
        if (isFinishing) {
            Log.w("activity is isFinishing")
            return dlg
        }
        dlg.show()
        return dlg
    }

    private fun newAlertDialogBuilder(context: Context?): AlertDialog.Builder {
        return AlertDialog.Builder(context!!)
    }

    private fun getDialog(
        context: Context?,
        message: Any?,
        positiveButtonText: Any?,
        positiveListener: DialogInterface.OnClickListener? = null,
        negativeButtonText: Any? = null,
        negativeListener: DialogInterface.OnClickListener? = null
    ): AlertDialog {
        val builder = newAlertDialogBuilder(context)
        if (message != null) builder.setMessage(getText(message))
        if (positiveButtonText != null) builder.setPositiveButton(getText(positiveButtonText), positiveListener)
        if (negativeButtonText != null) builder.setNegativeButton(getText(negativeButtonText), negativeListener)
        return builder.create()
    }

    override fun <T : View> findViewById(@IdRes resid: Int): T? {
        if (resid == -1) {
            Log.w("id is NO_ID" + " in the " + javaClass.simpleName)
            return null
        }
        if (resid == 0) {
            Log.w("id is 0" + " in the " + javaClass.simpleName)
            return null
        }

        val v: T? = super.findViewById(resid)
        if (v == null) {
            Log.printStackTrace(Exception("!has not " + resources.getResourceName(resid) + " in the " + javaClass.simpleName))
        }
        return v
    }

    protected fun setOnClickListener(@IdRes resid: Int, onClickListener: View.OnClickListener?) {
        val view = findViewById<View>(resid) ?: return
        BH.setOnClickListener(view, onClickListener)
    }
}
