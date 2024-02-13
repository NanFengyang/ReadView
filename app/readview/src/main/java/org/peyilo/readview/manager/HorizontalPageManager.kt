package org.peyilo.readview.manager

import android.graphics.PointF
import org.peyilo.readview.ui.PageContainer
import org.peyilo.readview.ui.PageDirection
import kotlin.math.abs

open class HorizontalPageManager(readView: PageContainer): PageManager(readView) {

    override fun decideInitDire(dx: Float, dy: Float): PageDirection {
        return if (abs(dx) < container.flipTouchSlop) {
            PageDirection.NONE
        } else if (dx < 0) {
            PageDirection.NEXT
        } else {
            PageDirection.PREV
        }
    }

    override fun onDown() {
        super.onDown()
        flagPoint.x = downPoint.x
        flagPoint.y = downPoint.y
        lastRealTimeDire = PageDirection.NONE
    }

    private val flagPoint = PointF()
    private var lastRealTimeDire = PageDirection.NONE
    override fun decideRealTimeDire(curX: Float, curY: Float): PageDirection {
        val dx = curX - flagPoint.x
        return if (abs(dx) >= container.flipTouchSlop) {
            flagPoint.x = curX
            flagPoint.y = curY
            if (dx < 0) {
                lastRealTimeDire = PageDirection.NEXT
                PageDirection.NEXT
            } else {
                lastRealTimeDire = PageDirection.PREV
                PageDirection.PREV
            }
        } else {
            lastRealTimeDire
        }
    }
}