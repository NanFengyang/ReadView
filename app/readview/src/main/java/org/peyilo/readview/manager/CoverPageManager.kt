package org.peyilo.readview.manager

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.Log
import org.peyilo.readview.ui.PageContainer
import org.peyilo.readview.ui.PageDirection

private const val TAG = "CoverPageManager"
// 覆盖翻页
class CoverPageManager(readView: PageContainer): HorizontalPageManager(readView) {

    override val supportShadowDraw = true
    var shadowWidth: Int = 15               // 阴影宽度
        private set

    private val shadowPaint: Paint = Paint()
    private val gradientColors = intArrayOf(-0x71000000, 0x00000000)
    private val gradientPositions = floatArrayOf(0.0f, 1.0f)

    override fun setShadowWidth(width: Int) {
        shadowWidth = width
    }
    override fun initPageScroll() {
        super.initPageScroll()
        prevPage.scrollTo(container.width, 0)
        nextPage.scrollTo(0, 0)
    }

    override fun prepareAnim(initDire: PageDirection) {
        scrolledView = when (initDire) {
            PageDirection.NEXT -> curPage
            PageDirection.PREV -> prevPage
            else -> throw IllegalStateException()
        }
    }

    override fun onNextCarousel() {
        super.onNextCarousel()
        nextPage.scrollTo(0, 0)
    }

    override fun onPrevCarousel() {
        super.onPrevCarousel()
        prevPage.scrollTo(container.width, 0)
        nextPage.scrollTo(0, 0)
    }

    override fun onMove(initDire: PageDirection, dx: Int, dy: Int) {
        super.onMove(initDire, dx, dy)
        if (dx < 0) {         // Next
            // 一开始往左滑（NEXT），之后往右滑（PREV），但是scrolledView由initDire确定
            // 如果一开始往左滑，scrolledView就是curPage。需要保证curPage中途往右滑动时
            // curPage的右侧边界不能超过ReadView的右侧边界
            if (initDire == PageDirection.NEXT) {
                scrolledView!!.scrollTo(-dx, 0)
            } else {
                // 设置完以上检查initDire的语句以后，page有概率卡在边缘处一点点，无法再接着滑动
                // 以贴合ReadView边界 ，需要直接调用代码保证边界对齐
                scrolledView!!.scrollTo(container.width, 0)
            }
        } else {                    // Prev
            if (initDire == PageDirection.PREV) {
                scrolledView!!.scrollTo(container.width + shadowWidth - dx, 0)
            } else {
                scrolledView!!.scrollTo(0, 0)
            }
        }
    }

    override fun resetPageScroll(initDire: PageDirection) {
        super.resetPageScroll(initDire)

        // 处理最终翻页结果
        val scrollX = scrolledView!!.scrollX
        val dx = if (initDire === PageDirection.NEXT) {
            -scrollX
        } else {
            container.width + shadowWidth - scrollX
        }
        // TODO：如果需要滑动的距离比较小，可能会出现卡顿，将持续时间设置得小一点会解决这个问题
        isRunning = true
        scroller.startScroll(scrollX, 0, dx, 0, animDuration)
        container.invalidate()
    }

    override fun abortAnim() {
        super.abortAnim()
        if (!scroller.isFinished) {
            scroller.forceFinished(true)
            scrolledView!!.scrollTo(scroller.finalX, scroller.finalY)
            isRunning = false
            scrolledView = null
        }
    }

    override fun startNextAnim() {
        super.startNextAnim()
        val scrollX = scrolledView!!.scrollX
        val dx = container.width + shadowWidth - scrollX
        isRunning = true
        scroller.startScroll(scrollX, 0, dx, 0, animDuration)
        // 由于PageContainer调用该函数之后会发生布局变化，会自己触发重绘
    }

    override fun startPrevAnim() {
        super.startPrevAnim()
        val scrollX = scrolledView!!.scrollX
        val dx = - scrollX
        isRunning = true
        scroller.startScroll(scrollX, 0, dx, 0, animDuration)
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            // 由于Scroller默认插值器是先加速后减小，减小到最后scrollX的增值小于1，就导致scroller.currX
            // 不会发生改变，scrollTo也就不会触发重绘。
            // 因此，当碰见这种情形时，意味着动画就差一点点距离就完成了，但是会因为增值小于1卡在那
            // 需要直接让其结束动画，滑动到最终位置
            scrolledView!!.scrollTo(scroller.currX, scroller.currY)

            // Log.d(TAG, "computeScroll: ${scroller.currX}")
            // 滑动动画结束
            if (scroller.currX == scroller.finalX && scroller.currY == scroller.finalY) {
                scroller.forceFinished(true)
                isRunning = false
                scrolledView = null
            }

        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        if (scrolledView != null) {
            container.apply {
                // 绘制阴影
                val x: Int = width - scrolledView!!.scrollX
                val min: Int = -shadowWidth
                if (x in min until width) {
                    val gradient = LinearGradient(
                        x.toFloat(), 0f, (x + shadowWidth).toFloat(), 0f,
                        gradientColors, gradientPositions, Shader.TileMode.CLAMP
                    )
                    shadowPaint.shader = gradient
                    canvas.drawRect(
                        x.toFloat(),
                        0f,
                        (x + shadowWidth).toFloat(),
                        height.toFloat(),
                        shadowPaint
                    )
                }
            }
        }
    }

    override fun onManagerChanged() {
        super.onManagerChanged()
        prevPage.scrollTo(0, 0)
    }
}