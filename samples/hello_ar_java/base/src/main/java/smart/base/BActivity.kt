package smart.base

import android.app.Dialog
import android.base.CActivity
import android.content.Context
import android.graphics.drawable.AnimationDrawable
import android.log.Log
import android.os.Bundle
import android.util.activityAttachBaseContext
import android.view.WindowManager
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import dev.eastar.base.R
import dev.eastar.operaxinterceptor.event.OperaXEventObservable
import dev.eastar.operaxinterceptor.event.OperaXEventObserver
import dev.eastar.operaxinterceptor.event.OperaXEvents
import smart.util.localeLifecycleObserver
import java.util.*


abstract class BActivity : CActivity(), OperaXEventObserver {
    override fun update(observable: Observable, data: Any?) {
        Log.e(javaClass, observable.javaClass, data)
        if (OperaXEventObservable == observable) {
            when (data) {
                OperaXEvents.Exited -> finish()
                else -> Unit
            }
        }
    }

    private var mIsLoading = false

    interface EXTRA {
        companion object {
            val TAB = "__tab"
            val TITLE = "__title"
            val URL = "__url"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        parseExtra()
        localeLifecycleObserver()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        loadOnce()
        reload()
        updateUI()
    }

    override fun exit() {
        super.exit()
        OperaXEventObservable.notify(OperaXEvents.Exited)
    }

    ///////////////////////////////////////////
    protected fun parseExtra() {
        try {
            onParseExtra()
        } catch (e: Exception) {
            Log.printStackTrace(e)
        }
    }

    protected fun loadOnce() {
        onLoadOnce()
    }

    protected fun reload() {
        clear()
        load()
    }

    protected fun clear() {
        try {
            onClear()
        } catch (e: Exception) {
            Log.printStackTrace(e)
        }

    }

    protected fun load() {
        if (mIsLoading) {
            Log.w("mIsLoading=", mIsLoading)
            return
        }
        if (lifecycle.currentState == Lifecycle.State.DESTROYED) {
            Log.w("activity is Lifecycle destroyed")
            return
        }
        if (isFinishing) {
            Log.w("activity is isFinishing")
            return
        }
        try {
            onLoad()
        } catch (e: Exception) {
            Log.printStackTrace(e)
        }

    }

    protected fun updateUI() {
        try {
            onUpdateUI()
        } catch (e: Exception) {
            Log.printStackTrace(e)
        }

    }

    protected open fun onParseExtra() = Log.d(javaClass, intent)
    protected open fun onLoadOnce() {}
    protected open fun onReload() {}
    protected open fun onClear() {}
    protected open fun onLoad() {}
    protected open fun onUpdateUI() {}

    override fun createProgress(): DialogFragment = ProgressDialogFragment()
    class ProgressDialogFragment : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            return AlertDialog.Builder(requireActivity())
                .setView(ImageView(context).apply {
                    scaleType = ImageView.ScaleType.CENTER_INSIDE
                    setImageResource(R.drawable.loading_anim)
                    (drawable as AnimationDrawable).start()
                })
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

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(activityAttachBaseContext(newBase, Locale(PP.appLanguage)))
    }
}
