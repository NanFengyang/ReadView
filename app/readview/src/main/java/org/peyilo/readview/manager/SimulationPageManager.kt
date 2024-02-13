package org.peyilo.readview.manager

import android.graphics.*
import android.util.Log
import org.peyilo.readview.ui.PageContainer
import org.peyilo.readview.ui.PageDirection
import org.peyilo.readview.utils.screenshot
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.log

private const val TAG = "SimulationPageManager"
// 仿真翻页
/**
 * 参考链接：
 * 1. Android自定义View——从零开始实现书籍翻页效果（一）：https://juejin.cn/post/6844903529715335182
 * 2. Android自定义View——从零开始实现书籍翻页效果（二）：https://juejin.cn/post/6844903529874718728
 */
class SimulationPageManager(container: PageContainer): HorizontalPageManager(container)  {

    // override val supportShadowDraw: Boolean = true

    private val bezierStart1 = PointF()         // 第一条贝塞尔曲线的起始点、终点、控制点
    private val bezierEnd1 = PointF()
    private val bezierControl1 = PointF()
    private val bezierStart2 = PointF()         // 第二条贝塞尔曲线的起始点、终点、控制点
    private val bezierEnd2 = PointF()
    private val bezierControl2 = PointF()
    private val bezierVertex1 = PointF()        // C区域直角三角形（近似为直角，实际上比90°稍大）斜边与两条贝塞尔曲线相切的两个点
    private val bezierVertex2 = PointF()
    private val touchPoint = PointF()                 // 触摸点
    private val cornerVertex = PointF()               // 页脚顶点
    private val middlePoint = PointF()                // 触摸点、页脚定点连线的中点
    private val m1 = PointF()                         // bezierStart1、bezierEnd1连线的中点
    private val m2 = PointF()                         // bezierStart2、bezierEnd2连线的中点

    /**
     * 计算各个点坐标，假设调用该函数之前touchPoint、cornerVertex已经初始化
     * 注意：调用该函数前，要保证touchPoint有效性
     */
    private fun computePoints() {
        computeMiddlePoint(touchPoint, cornerVertex, middlePoint)
        bezierControl1.y = cornerVertex.y
        bezierControl2.x = cornerVertex.x
        // 设touchPoint和cornerVertex连线为直线L1，过middlePoint作L1的垂直平分线L2
        // 与矩形屏幕边界相交于bezierControl1、bezierControl2
        var k1 = (touchPoint.y - cornerVertex.y) / (touchPoint.x - cornerVertex.x)  // 直线L1斜率
        var k2 = -1/k1      // 直线L2斜率
        bezierControl1.x = middlePoint.x + (cornerVertex.y - middlePoint.y) / k2
        bezierControl2.y = middlePoint.y + (cornerVertex.x - middlePoint.x) * k2
        // 设bezierStart1、bezierStart2连线为L3，则L3为touchPoint、middlePoint线段的垂直平分线
        bezierStart1.y = cornerVertex.y
        bezierStart1.x = bezierControl1.x - (cornerVertex.x - bezierControl1.x) / 2
        // 限制页面左侧不能翻起来，模拟真实书籍的装订
        if (touchPoint.x > 0 && bezierStart1.x < 0) {
            val w0 = container.width - bezierStart1.x           // 限制bezierStart1.x不能小于0
            val w1 = abs(cornerVertex.x - touchPoint.x)      // 如果小于0，需要对touchPoint进行特殊处理
            val w2 = container.width * w1 / w0
            touchPoint.x = abs(cornerVertex.x - w2)
            val h1 = abs(cornerVertex.y - touchPoint.y)
            val h2 = w2 * h1 / w1
            touchPoint.y = abs(cornerVertex.y - h2)
            // touchPoint更新后，需要重新计算与touchPoint有关系的坐标
            computeMiddlePoint(touchPoint, cornerVertex, middlePoint)
            k1 = (touchPoint.y - cornerVertex.y) / (touchPoint.x - cornerVertex.x)  // 直线L1斜率
            k2 = -1/k1      // 直线L2斜率
            bezierControl1.x = middlePoint.x + (cornerVertex.y - middlePoint.y) / k2
            bezierControl2.y = middlePoint.y + (cornerVertex.x - middlePoint.x) * k2
            bezierStart1.x = bezierControl1.x - (cornerVertex.x - bezierControl1.x) / 2
        }
        bezierStart2.x = cornerVertex.x
        bezierStart2.y = bezierControl2.y - (cornerVertex.y- bezierControl2.y) / 2
        // 设bezierEnd1为bezierControl1和touchPoint连线与bezierStart1、bezierStart2连线的交点
        // 设bezierEnd1为bezierControl2和touchPoint连线与bezierStart1、bezierStart2连线的交点
        computeCrossPoint(touchPoint, bezierControl1, bezierStart1, bezierStart2, bezierEnd1)
        computeCrossPoint(touchPoint, bezierControl2, bezierStart1, bezierStart2, bezierEnd2)
        computeMiddlePoint(bezierStart1, bezierEnd1, m1)
        computeMiddlePoint(bezierStart2, bezierEnd2, m2)
        computeMiddlePoint(m1, bezierControl1, bezierVertex1)      // bezierVertex1为m1、bezierControl1连线的中点
        computeMiddlePoint(m2, bezierControl2, bezierVertex2)      // bezierVertex2为m2、bezierControl2连线的中点
    }

    /**
     * 求解直线P1P2和直线P3P4的交点坐标，并将交点坐标保存到result中
     */
    private fun computeCrossPoint(P1: PointF, P2: PointF, P3: PointF, P4: PointF, result: PointF) {
        // 二元函数通式： y=ax+b
        val a1 = (P2.y - P1.y) / (P2.x - P1.x)
        val b1 = (P1.x * P2.y - P2.x * P1.y) / (P1.x - P2.x)
        val a2 = (P4.y - P3.y) / (P4.x - P3.x)
        val b2 = (P3.x * P4.y - P4.x * P3.y) / (P3.x - P4.x)
        result.x = (b2 - b1) / (a1 - a2)
        result.y = a1 * result.x + b1
    }

    // 计算P1P2的中点坐标，并保存到result中
    private fun computeMiddlePoint(P1: PointF, P2: PointF, result: PointF) {
        result.x = (P1.x + P2.x) / 2
        result.y = (P1.y + P2.y) / 2
    }

    /**
     * 仿真翻页时有三个区域：A、B、C
     * A区域：当前页区域
     * B区域：下一页区域
     * C区域：当前页背面区域
     */
    private val pathA = Path()
    private val pathB = Path()
    private val pathC = Path()

    private fun computePaths() {
        // 计算A区域的边界
        pathA.reset()
        pathA.moveTo(bezierStart1.x, bezierStart1.y)
        pathA.quadTo(bezierControl1.x, bezierControl1.y, bezierEnd1.x, bezierEnd1.y)
        pathA.lineTo(touchPoint.x, touchPoint.y)
        pathA.lineTo(bezierEnd2.x, bezierEnd2.y)
        pathA.quadTo(bezierControl2.x, bezierControl2.y, bezierStart2.x, bezierStart2.y)
        // 根据cornerVertex位置不同（左上角或者右上角）绘制区域A
        pathA.lineTo(container.width.toFloat(), container.height.toFloat() - cornerVertex.y)
        pathA.lineTo(0F, container.height.toFloat() - cornerVertex.y)
        pathA.lineTo(0F, cornerVertex.y)
        pathA.close()
        // 计算C区域的边界
        pathC.reset()
        pathC.moveTo(bezierVertex1.x, bezierVertex1.y)      // 将曲线简化为直线，再减去与A区域相交的区域即可获得C区域
        pathC.lineTo(bezierEnd1.x, bezierEnd1.y)
        pathC.lineTo(touchPoint.x, touchPoint.y)
        pathC.lineTo(bezierEnd2.x, bezierEnd2.y)
        pathC.lineTo(bezierVertex2.x, bezierVertex2.y)
        pathC.close()
        pathC.op(pathA, Path.Op.DIFFERENCE)
        // 计算B区域的边界
        pathB.reset()
        pathB.moveTo(bezierStart1.x, bezierStart1.y)
        pathB.quadTo(bezierControl1.x, bezierControl1.y, bezierEnd1.x, bezierEnd1.y)
        pathB.lineTo(touchPoint.x, touchPoint.y)
        pathB.lineTo(bezierEnd2.x, bezierEnd2.y)
        pathB.quadTo(bezierControl2.x, bezierControl2.y, bezierStart2.x, bezierStart2.y)
        pathB.lineTo(cornerVertex.x, cornerVertex.y)
        pathB.close()               // 先取B+C区域，然后减去C区域即可获得B区域
        pathB.op(pathC, Path.Op.DIFFERENCE)
    }

    override fun initPageScroll() {
        super.initPageScroll()
        prevPage.scrollTo(container.width, 0)
        // nextPage.scrollTo(0, 0)
    }

    private var topBitmap: Bitmap? = null
        set(value) {
            field?.let {
                if (value != null) {
                    Log.e(TAG, "bitmap未置空")
                }
            }
            field = value
        }
    private var bottomBitmap: Bitmap? = null
        set(value) {
            field?.let {
                if (value != null) {
                    Log.e(TAG, "bitmap未置空")
                }
            }
            field = value
        }

    override fun prepareAnim(initDire: PageDirection) {
        super.prepareAnim(initDire)
        when (initDire) {
            PageDirection.NEXT -> {
                topBitmap = curPage.screenshot()
                bottomBitmap = nextPage.screenshot()
            }
            PageDirection.PREV -> {
                topBitmap = prevPage.screenshot()
                bottomBitmap = curPage.screenshot()
            }
            else -> throw IllegalStateException()
        }
    }

    private enum class AnimMode {
        TopRightCorner, BottomRightCorner, NextLandscape, PrevLandscape, None
    }
    private var animMode = AnimMode.None

    override fun decideInitDire(dx: Float, dy: Float): PageDirection {
        val initDire = super.decideInitDire(dx, dy)
        animMode = when (initDire) {
            PageDirection.NEXT -> {     // 向下一页翻页时，根据本轮手势的DOWN坐标决定翻页动画的三种不同模式：右上角翻页、右下角翻页、横向翻页
                if (downPoint.y < container.height * 1 / 3) {               // 右上角翻页
                    AnimMode.TopRightCorner
                } else if (downPoint.y > container.height * 2 / 3) {    // 右下角翻页
                    AnimMode.BottomRightCorner
                } else {   // 横向翻页
                    AnimMode.NextLandscape
                }
            }
            PageDirection.PREV -> {     // 向上一页翻页时，只有横向翻页一种模式
                AnimMode.PrevLandscape
            }
            else -> AnimMode.None
        }
        if (animMode != AnimMode.None) {
            // 横向翻页通过touchPoint实现，因此也要设置cornerVertex
            if (downPoint.y < container.height / 2) {
                cornerVertex.x = container.width.toFloat()
                cornerVertex.y = 0F
            } else {
                cornerVertex.x = container.width.toFloat()
                cornerVertex.y = container.height.toFloat()
            }
        }
        return initDire
    }

    private fun clearBitmap() {
        topBitmap?.recycle()
        bottomBitmap?.recycle()
        topBitmap = null
        bottomBitmap = null
    }

    private val regionPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 5F
    }

    /**
     * 限制TouchPoint的坐标不出界，并做一些近似处理
     */
    private fun setTouchPoint(x: Float, y: Float) {
        touchPoint.x = x
        when (animMode) {
            AnimMode.TopRightCorner, AnimMode.BottomRightCorner -> {
                if (y > 0 && y < container.height) {
                    touchPoint.y = y
                } else if (y <= 0) {            // 限制touchPoint.y不出界
                    touchPoint.y = 1F
                } else {
                    touchPoint.y = container.height - 1F
                }
            }
            AnimMode.NextLandscape, AnimMode.PrevLandscape -> {
                if (cornerVertex.y == 0F) {
                    touchPoint.y = 1F
                } else {
                    touchPoint.y = container.height - 1F
                }
            }
            else -> Unit
        }
    }

    override fun onMove(initDire: PageDirection, dx: Int, dy: Int) {
        super.onMove(initDire, dx, dy)
        setTouchPoint(curPoint.x, curPoint.y)
        computePoints()
        computePaths()
        container.invalidate()
    }

    override fun onNextCarousel() {
        super.onNextCarousel()
        prevPage.scrollTo(container.width, 0)
        nextPage.scrollTo(0, 0)
    }

    override fun onPrevCarousel() {
        super.onPrevCarousel()
        prevPage.scrollTo(container.width, 0)
        curPage.scrollTo(0, 0)
    }

    override fun flipToNextPage() {
        cornerVertex.x = container.width.toFloat()
        cornerVertex.y = container.height.toFloat()
        touchPoint.x = cornerVertex.x
        touchPoint.y = cornerVertex.y - 1F
        super.flipToNextPage()
    }

    override fun flipToPrevPage() {
        cornerVertex.x = container.width.toFloat()
        cornerVertex.y = container.height.toFloat()
        touchPoint.x = 0F
        touchPoint.y = cornerVertex.y - 1F
        super.flipToPrevPage()
    }

    override fun startPrevAnim() {
        val dx = - touchPoint.x + cornerVertex.x - 1F
        val dy =  - touchPoint.y + cornerVertex.y + 1F
        isRunning = true
        scroller.startScroll(
            touchPoint.x.toInt(), touchPoint.y.toInt(), dx.toInt(), dy.toInt(), animDuration
        )
    }

    override fun startNextAnim() {
        val dx = - touchPoint.x - cornerVertex.x
        val dy = - touchPoint.y + cornerVertex.y + 1F
        isRunning = true
        scroller.startScroll(
            touchPoint.x.toInt(), touchPoint.y.toInt(), dx.toInt(), dy.toInt(), animDuration
        )
    }

    override fun resetPageScroll(initDire: PageDirection) {
        super.resetPageScroll(initDire)
        when (initDire) {
            PageDirection.NEXT -> {
                startPrevAnim()
            }
            PageDirection.PREV -> {
                startNextAnim()
            }
            else -> Unit
        }
    }

    override fun computeScroll() {
        super.computeScroll()
        if (scroller.computeScrollOffset()) {
            setTouchPoint(scroller.currX.toFloat(), scroller.currY.toFloat())
            computePoints()
            computePaths()
            container.invalidate()
            // 滑动动画结束
            if (scroller.currX == scroller.finalX && scroller.currY == scroller.finalY) {
                scroller.forceFinished(true)
                isRunning = false
                clearBitmap()
            }
        }
    }

    override fun abortAnim() {
        super.abortAnim()
        if (!scroller.isFinished) {
            isRunning = false
            scroller.forceFinished(true)
            clearBitmap()
        }
    }

    private fun debugPath(canvas: Canvas) {
        canvas.drawPath(pathA, regionPaint)
        canvas.drawPath(pathB, regionPaint)
        canvas.drawPath(pathC, regionPaint)
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (isDragging || isRunning) {
            drawRegionA(canvas)
            drawRegionB(canvas)
            drawRegionC(canvas)
            drawShadow(canvas)
        }
    }

    private fun drawRegionA(canvas: Canvas) {
        canvas.save()
        canvas.clipPath(pathA)
        canvas.drawBitmap(topBitmap!!, 0F, 0F, regionPaint)
        canvas.restore()
    }

    private fun drawRegionB(canvas: Canvas) {
        canvas.save()
        canvas.clipPath(pathB)
        canvas.drawBitmap(bottomBitmap!!, 0F, 0F, regionPaint)
        canvas.restore()
    }

    private val matrixArray = floatArrayOf(0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 1F)
    private val matrix = Matrix()
    private fun drawRegionC(canvas: Canvas) {
        canvas.save()
        canvas.clipPath(pathC)
        // 计算两个控制点之间的距离
        val dis = hypot(cornerVertex.x - bezierControl1.x, bezierControl2.y - cornerVertex.y)
        val sin = (cornerVertex.x - bezierControl1.x) / dis
        val cos = (bezierControl2.y - cornerVertex.y) / dis
        matrixArray[0] = - (1 - 2 * sin * sin)
        matrixArray[1] = 2 * sin * cos
        matrixArray[3] = 2 * sin * cos
        matrixArray[4] = 1 - 2 * sin * sin
        matrix.reset()
        matrix.setValues(matrixArray)
        matrix.preTranslate(-bezierControl1.x, -bezierControl1.y)
        matrix.postTranslate(bezierControl1.x, bezierControl1.y)
        canvas.drawBitmap(topBitmap!!, matrix, null)
        canvas.restore()
    }

    private fun drawShadow(canvas: Canvas) {
        // TODO()
    }

    override fun onManagerChanged() {
        super.onManagerChanged()
        prevPage.scrollTo(0, 0)
    }

}