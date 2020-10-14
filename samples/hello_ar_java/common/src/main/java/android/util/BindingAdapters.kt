package android.util

import android.view.View
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter

@BindingAdapter("gone")
fun View.isGone(gone: Boolean) {
    isGone = gone
}

@BindingAdapter("visible")
fun View.isVisible(visible: Boolean) {
    isVisible = visible
}

@BindingAdapter("isInvisible")
fun View.isInvisible(invisible: Boolean) {
    isInvisible = invisible
}



