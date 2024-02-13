package org.peyilo.readview.provider

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import org.peyilo.readview.parser.ParagraphContent
import org.peyilo.readview.parser.ReadChapter
import org.peyilo.readview.parser.TitleContent
import org.peyilo.readview.ui.ReadBook

class DefaultPageProvider: PageProvider {

    private val config get() = ReadBook.config
    private val remainedWidth get() = config.contentWidth - config.paddingLeft - config.paddingRight
    private val remainedHeight get() = config.contentHeight - config.paddingTop - config.paddingBottom
    private val startLeft get() = config.paddingLeft
    private val startTop get() = config.paddingTop

    private val paint = Paint()

    private fun measureText(char: Char, size: Float): Float {
        return measureText(char.toString(), size)
    }

    private fun measureText(string: String, size: Float): Float {
        synchronized(paint) {
            if (paint.textSize != size) {
                paint.textSize = size
            }
            return paint.measureText(string)
        }
    }
    /**
     * 对一个段落进行断行
     * @param text 待断行的字符串
     * @param offset 段落首行的偏移量
     * @param width 一行文字的最大宽度
     * @param size 文字大小
     */
    private fun breakLines(
        text: String,
        width: Float, size: Float,
        textMargin: Float, offset: Float
    ): List<TextLine> {
        val lineList = ArrayList<TextLine>()
        var line = TextLine().apply { textSize = size }
        var w = width - offset
        var dimen: Float
        text.forEach {
            dimen = measureText(it, size)
            if (w < dimen) {    // 剩余宽度已经不足以留给该字符
                lineList.add(line)
                line = TextLine().apply { textSize = size }
                w = width
            }
            w -= dimen + textMargin
            line.add(CharData(it).apply { this@apply.width = dimen })
        }
        val lastLine = line
        if (lastLine.text.size != 0) {
            lineList.add(lastLine)
        }
        return lineList
    }

    override fun split(chap: ReadChapter) {
        // 如果留给绘制内容的空间不足以绘制标题或者正文的一行，直接返回false
        if (ReadBook.config.contentTextSize > remainedHeight
            || config.titleTextSize > remainedHeight) {
            throw IllegalStateException()
        }
        chap.pages.clear()           // 清空pageData
        val width = remainedWidth
        var height = remainedHeight
        var base = startTop
        val left = startLeft
        var curPageIndex = 1
        var page = PageData(curPageIndex)

        val firstContent = chap.content[0]
        val hasTitle = firstContent is TitleContent
        if (hasTitle) {
            val titleLines = breakLines((firstContent as TitleContent).text, width, config.titleTextSize, 0F, 0F)
            titleLines.forEach {
                base += config.titleTextSize
                it.apply {                  // 设置TextLine的base、left
                    this@apply.base = base
                    this@apply.left = left
                }
                page.lines.add(it)
                base += config.lineMargin
            }
            var offset = 0F     // 正文内容的偏移
            if (titleLines.isNotEmpty()) {
                base += config.titleMargin - config.lineMargin
                offset += config.titleMargin
                offset += config.titleTextSize * titleLines.size
                offset += config.lineMargin * (titleLines.size - 1)
            }
            height -= offset.toInt()
        }
        // 如果剩余空间已经不足以再添加一行，就换成下一页
        if (height < config.contentTextSize) {
            height = remainedHeight
            base = startTop
            chap.pages.add(page)
            curPageIndex++
            page = PageData(curPageIndex)
        }
        // 开始正文内容的处理
        val parasStartIndex = if (hasTitle) 1 else 0
        for (i in parasStartIndex until chap.content.size) {
            val para = chap.content[i] as ParagraphContent
            val paraLines = breakLines(para.text, width, config.contentTextSize,
                config.textMargin, config.paraOffset)
            for (j in paraLines.indices) {
                val line = paraLines[j]
                if (height < config.contentTextSize) {
                    height = remainedHeight
                    base = startTop
                    chap.pages.add(page)
                    curPageIndex++
                    page = PageData(curPageIndex)
                }
                base += config.contentTextSize
                if (j == 0) {       // 处理段落首行缩进
                    line.apply {
                        this@apply.base = base
                        this@apply.left = left + measureText("缩进", config.contentTextSize)
                    }
                } else {
                    line.apply {
                        this@apply.base = base
                        this@apply.left = left
                    }
                }
                page.lines.add(line)
                base += config.lineMargin
                height -= (config.contentTextSize + config.lineMargin).toInt()
            }
            base += config.paraMargin - config.lineMargin
            height -= config.paraMargin.toInt()      // 处理段落的额外间距
        }
        chap.pages.add(page)
    }

    override fun drawPage(page: PageData, canvas: Canvas, paint: Paint) {
        paint.isAntiAlias = true
        page.lines.forEach {line ->
            if (line is TextLine) {
                if (line.isTitleLine) {
                    var left = line.left
                    paint.textSize = line.textSize
                    paint.typeface = Typeface.DEFAULT_BOLD
                    line.text.forEach { charData ->
                        paint.color = charData.color
                        canvas.drawText(charData.char.toString(), left, line.base, paint)
                        left += charData.width + config.textMargin
                    }
                } else {
                    var left = line.left
                    paint.textSize = line.textSize
                    paint.typeface = Typeface.DEFAULT
                    line.text.forEach { charData ->
                        paint.color = charData.color
                        canvas.drawText(charData.char.toString(), left, line.base, paint)
                        left += charData.width + config.textMargin
                    }
                }
            }
        }
    }
}