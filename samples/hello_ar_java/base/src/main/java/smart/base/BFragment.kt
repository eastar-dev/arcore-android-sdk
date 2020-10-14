package smart.base

import android.base.CFragment
import android.log.Log
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import dev.eastar.operaxinterceptor.event.OperaXEventObserver
import java.util.*

abstract class BFragment : CFragment(), OperaXEventObserver {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        parseExtra()
        loadOnce()
        clear()
        load()
        updateUI()
    }

    ///////////////////////////////////////////
    override fun update(observable: Observable, data: Any) {
        Log.e(javaClass, observable.javaClass, data)
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

    public fun reload() {
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

    protected var mIsLoading = false
    protected fun load() {
        if (mIsLoading) {
            Log.w("mIsLoading=", mIsLoading)
            return
        }
        if (lifecycle.currentState == Lifecycle.State.DESTROYED) {
            Log.w("activity is Lifecycle destroyed")
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

    protected open fun onParseExtra() {}
    protected open fun onLoadOnce() {}
    protected open fun onClear() {}
    protected open fun onLoad() {}
    protected open fun onUpdateUI() {}
}