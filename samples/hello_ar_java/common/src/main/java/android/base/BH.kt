package android.base

import android.content.Context
import android.view.View
import android.widget.TextView
import android.widget.TextView.BufferType

object BH {
    /********************************************************************
     * Resource**********************************************************
     */
    fun getText(context: Context, text: Any?): CharSequence? {
        if (text == null) return null
        if (text is CharSequence) return text
        return if (text is Int) context.getString((text as Int?)!!) else text.toString()
    }

    /********************************************************************
     * TextView**********************************************************
     */
    fun setText(tv: TextView, text: Any?) {
        setText(tv, text, BufferType.NORMAL)
    }

    fun setText(tv: TextView, text: Any?, b: BufferType?) {
        tv.setText(getText(tv.context, text), b)
    }

    @JvmStatic
    fun setOnClickListener(v: View, onClickListener: View.OnClickListener?) {
        v.setOnClickListener(onClickListener)
    }

    fun setVisibility(v: View, visibility: Int) {
        v.visibility = visibility
    }

    @JvmStatic
    fun setVisibility(v: View, visibility: Boolean) {
        v.visibility = if (visibility) View.VISIBLE else View.GONE
    }
}