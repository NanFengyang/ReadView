package org.peyilo.readview.parser

import org.peyilo.readview.provider.PageData

class ReadChapter (val chapIndex: Int) {
    val content = mutableListOf<Content>()
    val pages = mutableListOf<PageData>()
}