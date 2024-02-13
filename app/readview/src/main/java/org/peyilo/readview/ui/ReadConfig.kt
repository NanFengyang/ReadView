package org.peyilo.readview.ui

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface

class ReadConfig {

    var contentDimenInitialized = false         // contentView的尺寸是否完成过了一次初始化
    var contentHeight = 0F
        private set
    var contentWidth = 0F
        private set

    fun setContentDimen(w: Int, h: Int) {
        if (!contentDimenInitialized) contentDimenInitialized = true
        contentWidth = w.toFloat()
        contentHeight = h.toFloat()
    }

    var paddingLeft = 60F
    var paddingRight = 60F
    var paddingTop = 20F
    var paddingBottom = 20F


    val contentPaint: Paint by lazy { Paint().apply {
        textSize = 56F
        color = Color.parseColor("#2B2B2B")
        flags = Paint.ANTI_ALIAS_FLAG
    } }

    val titlePaint: Paint by lazy { Paint().apply {
        typeface = Typeface.DEFAULT_BOLD
        textSize = 72F
        color = Color.BLACK
        flags = Paint.ANTI_ALIAS_FLAG
    } }

    val contentTextColor get() = contentPaint.color
    val contentTextSize get() = contentPaint.textSize
    val titleTextColor get() = titlePaint.color
    val titleTextSize get() = titlePaint.textSize

    val paraOffset get() = contentPaint.measureText("测试") // 段落首行的偏移
    var titleMargin = 160F                                      // 章节标题与章节正文的间距
    var textMargin = 0F                                         // 字符间隔
    var lineMargin = 30F                                        // 行间隔
    var paraMargin = 50F                                        // 段落间隔
}