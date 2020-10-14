package android.base

import android.base.BH.setOnClickListener
import android.base.BH.setVisibility
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.log.Log.printStackTrace
import android.log.Log.w
import android.log.LogFragment
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import io.reactivex.disposables.Disposable

/** @author r */
abstract class CFragment : LogFragment() {
    val mContext: Context by lazy { requireContext() }
    val mActivity: CActivity by lazy { requireActivity() as CActivity }
    val intent: Intent by lazy { requireActivity().intent }

    var disposables = mutableSetOf<Disposable>()
    fun Disposable.autoDispose() = disposables.add(this)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun autoDisposable() {
                disposables.forEach { if (!it.isDisposed) it.dispose() }
            }
        })
    }


    fun exit() {
        mActivity.exit()
    }

    fun main() {
        mActivity.main()
    }

    fun onBackPressedEx(): Boolean {
        return false
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    fun getText(text: Any?): CharSequence? {
        if (text == null) return null
        if (text is CharSequence) return text
        return if (text is Int) mContext!!.getString((text as Int?)!!) else text.toString()
    }

    fun toast(text: Any?) {
        Toast.makeText(mContext, getText(text), Toast.LENGTH_SHORT).show()
    }

    fun showProgress() {
        mActivity.showProgress()
    }

    fun dismissProgress() {
        mActivity.dismissProgress()
    }

    @JvmOverloads
    fun showDialog(
        message: Any?,
        positiveButtonText: Any?,
        positiveListener: DialogInterface.OnClickListener? = null,
        negativeButtonText: Any? = null,
        negativeListener: DialogInterface.OnClickListener? = null,
    ): AlertDialog {
        return mActivity.showDialog(message, positiveButtonText, positiveListener, negativeButtonText, negativeListener)
    }

    fun <T : View> findViewById(@IdRes resid: Int): T? {
        if (resid == -1) {
            w("id is NO_ID" + " in the " + javaClass.simpleName)
            return null
        }
        if (resid == 0) {
            w("id is 0" + " in the " + javaClass.simpleName)
            return null
        }

        val v: T? = requireView().findViewById(resid)
        if (v == null) {
            printStackTrace(Exception("!has not " + resources.getResourceName(resid) + " in the " + javaClass.simpleName))
        }
        return v
    }

    protected fun setOnClickListener(@IdRes resid: Int, onClickListener: View.OnClickListener?) {
        val view = findViewById<View>(resid) ?: return
        setOnClickListener(view, onClickListener)
    }

    protected fun setVisibility(@IdRes resid: Int, visibility: Boolean) {
        val view = findViewById<View>(resid) ?: return
        setVisibility(view, visibility)
    }

    protected fun isEmpty(cs: CharSequence?): Boolean {
        return cs == null || cs.isEmpty()
    }
}