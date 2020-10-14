@file:Suppress("unused")

package android.util

import android.graphics.drawable.Drawable
import android.graphics.drawable.StateListDrawable

object BU {
    fun getStateListDrawable(normal: Drawable, pressed: Drawable): StateListDrawable {
        val drawable = StateListDrawable()
        drawable.addState(intArrayOf(android.R.attr.state_pressed), pressed)
        drawable.addState(intArrayOf(android.R.attr.state_selected), pressed)
        drawable.addState(intArrayOf(), normal)
        return drawable
    }
}
