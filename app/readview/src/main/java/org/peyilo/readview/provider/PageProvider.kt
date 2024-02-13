package org.peyilo.readview.provider

import android.graphics.Canvas
import android.graphics.Paint
import org.peyilo.readview.parser.ReadChapter

interface PageProvider {

    fun split(chap: ReadChapter)

    fun drawPage(page: PageData, canvas: Canvas, paint: Paint)

}