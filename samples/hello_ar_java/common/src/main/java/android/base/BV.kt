package android.base

import android.content.Context
import android.util.DisplayMetrics
import android.util.TypedValue

object BV {
    fun create(context: Context) {
        displayMetrics = context.resources.displayMetrics
        packageName = context.packageName;
    }

    var packageName = ""

    @JvmField
    var displayMetrics: DisplayMetrics? = null

    @JvmStatic
    fun dp2px(dp: Float): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, displayMetrics).toInt()
    }
}