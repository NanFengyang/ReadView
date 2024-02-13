package org.peyilo.readview.manager

import org.peyilo.readview.ui.PageContainer
import org.peyilo.readview.ui.PageDirection

open class VerticalPageManager(readView: PageContainer): PageManager(readView) {

    override fun decideInitDire(dx: Float, dy: Float): PageDirection {
        return if (dy < 0) {
            PageDirection.NEXT
        }
        else {
            PageDirection.PREV
        }
    }

}