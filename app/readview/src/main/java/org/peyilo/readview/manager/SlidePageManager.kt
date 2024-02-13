package org.peyilo.readview.manager

import org.peyilo.readview.ui.PageContainer
import org.peyilo.readview.ui.PageDirection

private const val TAG = "SlidePageManager"
/**
 * 左右滑动翻页
 * NEXT翻页时，CUR和NEXT页面一起滑动；PREV翻页时，CUR和PREV页面一起滑动
 */
class SlidePageManager(container: PageContainer): HorizontalPageManager(container) {

    private var curAnimDire: PageDirection = PageDirection.NONE         // 当前正在执行的滑动动画方向

    override fun initPageScroll() {
        super.initPageScroll()
        prevPage.scrollTo(container.width, 0)
        nextPage.scrollTo(-container.width, 0)
    }

    override fun prepareAnim(initDire: PageDirection) {
        when(initDire) {
            PageDirection.NEXT -> {
                scrolledView = curPage
                secondView = nextPage
            }
            PageDirection.PREV -> {
                scrolledView = curPage
                secondView = prevPage
            }
            else -> throw IllegalStateException()
        }
    }

    override fun onMove(initDire: PageDirection, dx: Int, dy: Int) {
        super.onMove(initDire, dx, dy)
        if (dx < 0) {         // Next
            if (initDire == PageDirection.NEXT) {
                scrolledView!!.scrollTo(-dx, 0)
                secondView!!.scrollTo(-dx - container.width, 0)
            } else {
                scrolledView!!.scrollTo(0, 0)
                secondView!!.scrollTo(-container.width, 0)
            }
        } else {                    // Prev
            if (initDire == PageDirection.PREV) {
                scrolledView!!.scrollTo(-dx, 0)
                secondView!!.scrollTo(container.width - dx, 0)
            } else {
                scrolledView!!.scrollTo(0, 0)
                secondView!!.scrollTo(container.width, 0)
            }
        }
    }

    override fun onNextCarousel() {
        super.onNextCarousel()
        nextPage.scrollTo(-container.width, 0)
    }

    override fun onPrevCarousel() {
        super.onPrevCarousel()
        prevPage.scrollTo(container.width, 0)
    }

    override fun startNextAnim() {
        super.startNextAnim()
        val scrollX = scrolledView!!.scrollX
        val dx = container.width - scrollX
        isRunning = true
        curAnimDire = PageDirection.NEXT
        scroller.startScroll(scrollX, 0, dx, 0, animDuration)     // dx大于0往左滑，小于0往右滑
        container.invalidate()
    }

    override fun startPrevAnim() {
        super.startPrevAnim()
        val scrollX = scrolledView!!.scrollX
        val dx = - container.width - scrollX
        isRunning = true
        curAnimDire = PageDirection.PREV
        scroller.startScroll(scrollX, 0, dx, 0, animDuration)
        container.invalidate()
    }

    override fun resetPageScroll(initDire: PageDirection) {
        super.resetPageScroll(initDire)
        val scrollX = scrolledView!!.scrollX
        val dx = -scrollX
        isRunning = true
        curAnimDire = initDire
        scroller.startScroll(scrollX, 0, dx, 0, animDuration)
        container.invalidate()
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            val x = scroller.currX
            scrolledView!!.scrollTo(x, scroller.currY)
            val sx = when (curAnimDire) {       // NEXT
                PageDirection.NEXT -> x - container.width
                PageDirection.PREV ->  x + container.width
                else -> throw IllegalStateException()
            }
            secondView!!.scrollTo(sx, scroller.currY)
            // 滑动动画结束
            if (scroller.currX == scroller.finalX && scroller.currY == scroller.finalY) {
                scroller.forceFinished(true)
                isRunning = false
                curAnimDire = PageDirection.NONE
                scrolledView = null
                secondView = null
            }
        }
    }

    override fun abortAnim() {
        super.abortAnim()
        if (!scroller.isFinished) {
            scroller.forceFinished(true)
            scrolledView!!.scrollTo(scroller.finalX, scroller.finalY)
            when (curAnimDire) {       // NEXT
                PageDirection.NEXT ->  secondView!!.scrollTo(scroller.finalX - container.width, scroller.finalY)
                PageDirection.PREV ->  secondView!!.scrollTo(scroller.finalX + container.width, scroller.finalY)
                else -> throw IllegalStateException()
            }
            isRunning = false
            scrolledView = null
            secondView = null
        }
        curAnimDire = PageDirection.NONE
    }

    override fun onManagerChanged() {
        super.onManagerChanged()
        prevPage.scrollTo(0, 0)
        nextPage.scrollTo(0, 0)
    }
}