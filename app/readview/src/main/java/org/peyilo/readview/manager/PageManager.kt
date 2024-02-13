package org.peyilo.readview.manager

import android.graphics.Canvas
import org.peyilo.readview.ui.PageContainer
import org.peyilo.readview.ui.PageDirection
import org.peyilo.readview.ui.ReadPage

abstract class PageManager(protected val container: PageContainer) {

    val curPage get() = container.curPage
    val prevPage get() = container.prevPage
    val nextPage get() = container.nextPage

    var animDuration = 1000
    val scroller get() = container.scroller
    val downPoint get() = container.downPoint
    val upPoint get() = container.upPoint
    val curPoint get() = container.curPoint

    // 当一个DOWN事件到来时，PageContainer会根据isRunning的值决定是否调用abortAnim()
    // 如果为true，就表示当前翻页动画正在执行，将会调用abortAnim()结束动画
    var isRunning = false

    protected var isDragging = false

    var scrolledView: ReadPage? = null          // 滑动的视图
    var secondView: ReadPage? = null
    var thirdView: ReadPage? = null

    var needPageScrollInit = true

    open val supportShadowDraw = false          // 是否支持阴影绘制

    /**
     * 子类需要重写该方法，在该方法内完成prevPage、nextPage（curPage可以不需要初始化）的scroll初始化
     * 注意：一定要将prevPage、nextPage在这里调用ReadPage#scrollTo滑动到初始位置
     */
    open fun initPageScroll() {
        needPageScrollInit = false
    }

    abstract fun decideInitDire(dx: Float, dy: Float): PageDirection

    open fun decideEndDire(initDire: PageDirection, realTimeDire: PageDirection): PageDirection {
        return if (initDire == PageDirection.NEXT && realTimeDire == PageDirection.NEXT) {
            PageDirection.NEXT
        } else if (initDire == PageDirection.PREV && realTimeDire == PageDirection.PREV) {
            PageDirection.PREV
        } else {
            PageDirection.NONE
        }
    }

    open fun decideRealTimeDire(curX: Float, curY: Float): PageDirection = PageDirection.NONE

    // 当initDire完成初始化且有开启动画的需要，PageContainer就会调用该函数
    open fun prepareAnim(initDire: PageDirection) = Unit

    open fun onNextCarousel() = Unit        // 顺序轮播以后将page滑动到正确位置
    open fun onPrevCarousel() = Unit        // 逆序轮播以后将page滑动到正确位置

    open fun computeScroll() = Unit

    open fun abortAnim() = Unit             // 停止翻页滑动动画

    open fun startNextAnim() = Unit         // 开启向下一页翻页的动画

    open fun startPrevAnim() = Unit         // 开启向上一页翻页的动画

    open fun resetPageScroll(initDire: PageDirection) = Unit    // 开启翻页复位动画

    open fun onDown() = Unit                // DOWN事件到来时，PageContainer会调用该方法

    open fun onUp() {
        if (isDragging) isDragging = false
    }

    open fun onMove(initDire: PageDirection, dx: Int, dy: Int) {    // 跟随手指滑动
        if (!isDragging) isDragging = true
    }

    open fun flipToNextPage() {
        prepareAnim(PageDirection.NEXT)
        startNextAnim()
    }

    open fun flipToPrevPage() {
        prepareAnim(PageDirection.PREV)
        startPrevAnim()
    }

    open fun onManagerChanged() = Unit

    open fun setShadowWidth(width: Int) {
        throw IllegalStateException("该PageManager不支持阴影绘制！")
    }

    open fun dispatchDraw(canvas: Canvas) = Unit
}