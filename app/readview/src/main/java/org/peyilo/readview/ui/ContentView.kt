package org.peyilo.readview.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.View
import org.peyilo.readview.provider.PageData

private const val TAG = "ContentView"
/**
 * 正文显示视图
 */
class ContentView(context: Context, attrs: AttributeSet? = null):
    View(context, attrs), ReadContent {

    private val paint by lazy { Paint() }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        ReadBook.config.setContentDimen(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        content?.let {
            ReadBook.provider.drawPage(content!!, canvas, paint)
        }
    }

    private var content: PageData? = null
    override fun setContent(page: PageData) {
        content = page
        invalidate()
    }
}