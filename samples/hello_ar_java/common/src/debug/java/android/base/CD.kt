package android.base

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import dev.eastar.ktx.dpf

object CD {
    @JvmField
    var DEVELOP = false

    @JvmField
    var PASS = false

    @SuppressLint("StaticFieldLeak")
    lateinit var CONTEXT: Context

    private val rect = Rect()
    private val paint = Paint().apply {
        color = 0x55ff0000
        style = Paint.Style.STROKE
        strokeWidth = 8.dpf
    }

    @JvmStatic
    fun webViewDraw(view: View, canvas: Canvas) {
        view.getDrawingRect(rect)
        canvas.drawRect(rect, paint)
    }
}