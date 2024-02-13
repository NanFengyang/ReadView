package org.peyilo.readview.parser

import org.peyilo.readview.data.ChapData

interface ContentParser {

    /**
     * 解析ChapData的内容，生成一个由多个Content以及基本章节信息构成的ReadChapter
     */
    fun parse(chapData: ChapData): ReadChapter

}