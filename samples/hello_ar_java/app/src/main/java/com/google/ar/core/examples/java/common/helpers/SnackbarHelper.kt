/*
 * Copyright 2017 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.ar.core.examples.java.common.helpers

import android.app.Activity
import android.view.View
import android.widget.TextView
import com.google.android.material.snackbar.BaseTransientBottomBar.BaseCallback
import com.google.android.material.snackbar.Snackbar

/**
 * 샘플 스낵바를 관리하는 도우미입니다. Android 상용구 코드를 숨기고 더 간단한 메서드를 노출합니다.
 */
class SnackbarHelper {
    private var messageSnackbar: Snackbar? = null

    private enum class DismissBehavior {
        HIDE, SHOW, FINISH
    }

    private var maxLines = 2
    private var lastMessage = ""
    private var snackbarView: View? = null
    val isShowing: Boolean
        get() = messageSnackbar != null

    /** Shows a snackbar with a given message.  */
    fun showMessage(activity: Activity, message: String) {
        if (!message.isEmpty() && (!isShowing || lastMessage != message)) {
            lastMessage = message
            show(activity, message, DismissBehavior.HIDE)
        }
    }

    /** Shows a snackbar with a given message, and a dismiss button.  */
    fun showMessageWithDismiss(activity: Activity, message: String) {
        show(activity, message, DismissBehavior.SHOW)
    }

    /**
     * Shows a snackbar with a given error message. When dismissed, will finish the activity. Useful
     * for notifying errors, where no further interaction with the activity is possible.
     */
    fun showError(activity: Activity, errorMessage: String) {
        show(activity, errorMessage, DismissBehavior.FINISH)
    }

    /**
     * Hides the currently showing snackbar, if there is one. Safe to call from any thread. Safe to
     * call even if snackbar is not shown.
     */
    fun hide(activity: Activity) {
        if (!isShowing) {
            return
        }
        lastMessage = ""
        val messageSnackbarToHide = messageSnackbar
        messageSnackbar = null
        activity.runOnUiThread { messageSnackbarToHide?.dismiss() }
    }

    fun setMaxLines(lines: Int) {
        maxLines = lines
    }

    /**
     * Sets the view that will be used to find a suitable parent view to hold the Snackbar view.
     *
     *
     * To use the root layout ([android.R.id.content]), pass in `null`.
     *
     * @param snackbarView the view to pass to [     ][com.google.android.material.snackbar.Snackbar.make] which will be used to find a
     * suitable parent, which is a [androidx.coordinatorlayout.widget.CoordinatorLayout], or
     * the window decor's content view, whichever comes first.
     */
    fun setParentView(snackbarView: View?) {
        this.snackbarView = snackbarView
    }

    private fun show(activity: Activity, message: String, dismissBehavior: DismissBehavior) {
        //activity.runOnUiThread {
        //
        //    val view = if (snackbarView == null) activity.findViewById(android.R.id.content) else snackbarView
        //    messageSnackbar = Snackbar.make(view!!, message, Snackbar.LENGTH_INDEFINITE).apply {
        //        view.setBackgroundColor(BACKGROUND_COLOR)
        //        if (dismissBehavior != DismissBehavior.HIDE) {
        //            setAction("Dismiss") {
        //                dismiss()
        //            }
        //
        //            if (dismissBehavior == DismissBehavior.FINISH) {
        //                addCallback(object : BaseCallback<Snackbar?>() {
        //                    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
        //                        super.onDismissed(transientBottomBar, event)
        //                        activity.finish()
        //                    }
        //
        //                })
        //            }
        //        }
        //        (view.findViewById<View>(com.google.android.material.R.id.snackbar_text) as TextView).apply {
        //            maxLines = maxLines
        //        }
        //        show()
        //    }
        //}
    }

    companion object {
        private const val BACKGROUND_COLOR = -0x40cdcdce
    }
}