package org.peyilo.readview.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.Scroller
import org.peyilo.readview.annotation.ThreadSafe
import org.peyilo.readview.manager.*
import kotlin.math.hypot

private const val TAG = "PageContainer"
abstract class PageContainer(context: Context, attrs: AttributeSet?): FrameLayout(context, attrs) {

    internal var curPage: ReadPage
    internal var prevPage: ReadPage
    internal var nextPage: ReadPage

    private var pageManager: PageManager? = null
    private val defaultPageManager: PageManager by lazy {  NoAnimPageManager(this) }

    private val supportShadowDraw get() = manager.supportShadowDraw         // 是否支持阴影绘制

    var scaledTouchSlop = ViewConfiguration.get(context).scaledTouchSlop    // 区分点击和滑动的界限，默认为24
    var flipTouchSlop = 40      // 触发翻页的最小滑动距离

    // 当PageContainer已经layout过了，调用set方法更改翻页模式时，会触发UI变化
    // 但是在PageContainer还未进行measure、layout、draw时，并不会触发UI变化
    // 记住，需要在主线程更改翻页模式，并且更改时，请保证此时页面不处于被手指拖住未复原状态
    // 否则可能会因为manager实例改变，PageManager内部scrolledView为null触发NullPointerException
    var flipMode: FlipMode = FlipMode.NoAnim                     // 翻页模式
        set(value) {
            val lastManager = manager
            val needRefresh = !lastManager.needPageScrollInit    // 如果此时的manager的scroll已经初始化过了，代表已经执行过onLayout了
            pageManager = createPageController(value)
            lastManager.onManagerChanged()
            if (needRefresh) {
                lastManager.abortAnim()
                requestLayout()
            }
        }

    private val manager get(): PageManager {
        return if (pageManager == null) {
            defaultPageManager
        } else {
            pageManager!!
        }
    }

    private fun createPageController(mode: FlipMode): PageManager = when (mode) {
        FlipMode.NoAnim -> NoAnimPageManager(this)
        FlipMode.Cover -> CoverPageManager(this)
        FlipMode.Slide -> SlidePageManager(this)
        FlipMode.Simulation -> SimulationPageManager(this)
    }

    init {
        curPage = ReadPage(context, position = ReadPage.Position.CUR)
        prevPage = ReadPage(context, position = ReadPage.Position.PREV)
        nextPage = ReadPage(context, position = ReadPage.Position.NEXT)
    }

    @ThreadSafe
    @Synchronized
    fun initPage(initializer: (page: ReadPage) -> Unit) {
        listOf(nextPage, curPage, prevPage).forEach {
            initializer(it)             // 初始化三个子View
            addView(it)                 // 添加三个子View
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (manager.needPageScrollInit) {
            manager.initPageScroll()
        }
    }

    internal val scroller by lazy { Scroller(context, LinearInterpolator()) }
    internal val downPoint = PointF()
    internal val upPoint = PointF()
    internal val curPoint = PointF()
    private var isMove = false                              // 此次手势是否为滑动手势
    private var needStartAnim = false                          // 此次手势是否需要触发滑动动画
    private var initDire = PageDirection.NONE               // 初始滑动方向，其确定了要滑动的page
    private var realTimeDire = PageDirection.NONE

    override fun onTouchEvent(event: MotionEvent): Boolean {
        curPoint.set(event.x, event.y)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downPoint.set(event.x, event.y)
                if (manager.isRunning) {            // 如果翻页动画还在执行，立刻结束动画
                    manager.abortAnim()
                }
                manager.onDown()
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = curPoint.x - downPoint.x       // 水平偏移量
                val dy = curPoint.y - downPoint.y       // 垂直偏移量
                val distance = hypot(dx, dy)            // 距离
                // 为防止抖动的点击事件被看作滑动事件引入了scaledTouchSlop
                // 这引入了一个新的问题
                // 如果先在屏幕滑动一个大于scaledTouchSlop的距离，触发滑动效果，然后再往回滑动
                // 当距离小于scaledTouchSlop，就无法触发滑动效果
                // 因此引入了isMove来解决这个问题
                if (isMove || distance > scaledTouchSlop) {
                    if (!isMove) {
                        // 确定初始滑动方向，并且由初始滑动方向确定scrolledView。在一次滑动事件中，scrolledView不会发生改变
                        initDire = manager.decideInitDire(dx, dy)
                        if (initDire == PageDirection.NONE) {
                            isMove = false
                        } else {
                            isMove = true
                            // 决定是否要滑动
                            needStartAnim = (initDire == PageDirection.NEXT && hasNextPage())
                                    || initDire == PageDirection.PREV && hasPrevPage()
                            if (needStartAnim) manager.prepareAnim(initDire)
                        }
                    }
                    if (needStartAnim)
                        // 跟随手指滑动
                        manager.onMove(initDire, dx.toInt(), dy.toInt())
                }
                realTimeDire = manager.decideRealTimeDire(event.x, event.y)
            }
            MotionEvent.ACTION_UP -> {
                upPoint.set(event.x, event.y)
                if (isMove) {               // 本系列事件为一个滑动事件，处理最终滑动方向
                    if (needStartAnim) {
                        when (manager.decideEndDire(initDire, realTimeDire)) {
                            PageDirection.NEXT -> {
                                manager.startNextAnim()
                                nextCarousel()
                            }
                            PageDirection.PREV -> {
                                manager.startPrevAnim()
                                prevCarousel()
                            }
                            PageDirection.NONE -> {
                                manager.resetPageScroll(initDire)
                            }
                        }
                        needStartAnim = false
                    }
                    isMove = false            // 清除isMove的状态
                    initDire = PageDirection.NONE
                } else {                      // 本事件为一个点击事件
                    // 触发点击事件
                    val xPercent = downPoint.x / width * 100
                    val yPercent = downPoint.y / height * 100
                    performClick()
                    onClickRegionListener?.onClickRegion(xPercent.toInt(), yPercent.toInt())
                }
                manager.onUp()
                realTimeDire = PageDirection.NONE
            }
        }
        return true
    }


    // 按照下一页的顺序进行循环轮播
    private fun nextCarousel() {
        val temp = prevPage         // 移除最顶层的prevPage，插入到最底层，即nextPage下面
        prevPage = curPage          // 再更新prevPage、curPage、nextPage指向的page
        curPage = nextPage
        nextPage = temp
        removeView(nextPage)
        addView(nextPage, 0)
        updatePagePosition()
        updatePage(nextPage, PageDirection.NEXT)
        manager.onNextCarousel()
    }

    // 按照上一页的顺序进行循环轮播
    private fun prevCarousel() {
        val temp = nextPage         // 移除最底层的nextPage，插入到最顶层，即prevPage上面
        nextPage = curPage          // 再更新prevPage、curPage、nextPage指向的page
        curPage = prevPage
        prevPage = temp
        removeView(prevPage)
        addView(prevPage, 2)
        updatePagePosition()
        updatePage(nextPage, PageDirection.PREV)
        manager.onPrevCarousel()
    }

    private fun updatePagePosition() {
        prevPage.position = ReadPage.Position.PREV
        curPage.position = ReadPage.Position.CUR
        nextPage.position = ReadPage.Position.NEXT
    }

    override fun computeScroll() {
        super.computeScroll()
        manager.computeScroll()
    }

    /**
     * 翻向下一页
     * @return 是否翻页成功
     */
    fun flipToNextPage(): Boolean {
        if (hasNextPage()) {
            manager.flipToNextPage()
            nextCarousel()
            return true
        }
        return false
    }

    /**
     * 翻向上一页
     * @return 是否翻页成功
     */
    fun flipToPrevPage(): Boolean {
        if (hasPrevPage()) {
            manager.flipToPrevPage()
            prevCarousel()
            return true
        }
        return false
    }

    abstract fun hasNextPage(): Boolean
    abstract fun hasPrevPage(): Boolean

    protected abstract fun updatePage(page: ReadPage, dire: PageDirection)

    fun setShadowWidth(width: Int) {
        manager.setShadowWidth(width)
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        manager.dispatchDraw(canvas)
    }

    interface OnClickRegionListener {
        /**
         * 点击事件回调
         * @param xPercent 点击的位置在x轴方向上的百分比，例如xPercent=50，表示点击的位置为屏幕x轴方向上的中间
         * @param yPercent 点击的位置在y轴方向上的百分比
         */
        fun onClickRegion(xPercent: Int, yPercent: Int)
    }
    private var onClickRegionListener: OnClickRegionListener? = null
    fun setOnClickRegionListener(listener: OnClickRegionListener) {
        this.onClickRegionListener = listener
    }
    fun setOnClickRegionListener(listener: (xPercent: Int, yPercent: Int) -> Unit) {
        this.onClickRegionListener = object : OnClickRegionListener {
            override fun onClickRegion(xPercent: Int, yPercent: Int) {
                listener(xPercent, yPercent)
            }
        }
    }

}