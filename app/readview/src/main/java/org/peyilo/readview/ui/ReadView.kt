package org.peyilo.readview.ui

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import androidx.annotation.IntRange
import org.peyilo.readview.data.BookData
import org.peyilo.readview.loader.BookLoader
import org.peyilo.readview.loader.DefaultNativeLoader
import org.peyilo.readview.loader.SimpleTextLoader
import org.peyilo.readview.parser.DefaultContentParser
import org.peyilo.readview.provider.DefaultPageProvider
import java.io.File
import java.util.concurrent.Executors

private const val TAG = "ReadView"
class ReadView(context: Context, attrs: AttributeSet? = null): PageContainer(context, attrs) {

    private val threadPool by lazy { Executors.newFixedThreadPool(10) }
    private var attached = false

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        attached = true
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        attached = false
        threadPool.shutdownNow()        // 关闭正在执行的所有线程
    }

    private fun startTask(task: Runnable) {
        threadPool.submit(task)
    }

    fun hasNextChap(): Boolean = ReadBook.hasNextChap()

    fun hasPrevChap(): Boolean = ReadBook.hasPrevChap()

    override fun hasNextPage(): Boolean = ReadBook.hasNextPage()

    override fun hasPrevPage(): Boolean = ReadBook.hasPrevPage()

    override fun updatePage(page: ReadPage, dire: PageDirection) {
        var changed = false         // 章节是否发生改变
        if (dire == PageDirection.NEXT) {
            changed = ReadBook.moveToNextPage()
            refreshNextPage()
        } else if (dire == PageDirection.PREV) {
            changed = ReadBook.moveToPrevPage()
            refreshPrevPage()
        }
        if (changed) startTask {    // 若章节发生改变，需要对预处理章节进行加载、分页
            ReadBook.loadCurAndPreprocess()
            ReadBook.splitCurAndPreprocess()
        }
    }

    private fun prepareInitToc() {
        showTocInitializingView()
    }

    private fun showTocInitializingView() {

    }

    private fun showTocInitFailedView() {

    }

    private fun showChapLoadingView() {

    }

    // 刷新当前页
    private fun refreshCurPage() {
        curPage.content.setContent(ReadBook.getCurPage())
    }

    // 刷新上一页
    private fun refreshPrevPage() {
        if (ReadBook.hasPrevPage())     // 只有当有上一页的时候才会刷新
            prevPage.content.setContent(ReadBook.getPrevPage())
    }

    // 刷新下一页
    private fun refreshNextPage() {
        if (ReadBook.hasNextPage())     // 只有当有下一页的时候才会刷新
            nextPage.content.setContent(ReadBook.getNextPage())
    }

    private fun refreshAllPage() {
        refreshCurPage()
        refreshNextPage()
        refreshPrevPage()
    }

    fun openBook(
        loader: BookLoader,
        @IntRange(from = 1) chapIndex: Int = 1,
        @IntRange(from = 1) pageIndex: Int = 1
    ) {
        ReadBook.loader = loader
        ReadBook.parser = DefaultContentParser()
        ReadBook.provider = DefaultPageProvider()
        ReadBook.curChapIndex = chapIndex
        ReadBook.curPageIndex = pageIndex
        prepareInitToc()            // 准备加载目录，显示加载目录中视图
        startTask {
            val initTocRes = ReadBook.initToc()         // 加载目录信息
            if (initTocRes) {
                Log.d(TAG, "openBook: init toc success")
                showChapLoadingView()                   // 显示加载章节中的视图
                val loadChapRes = ReadBook.loadCurAndPreprocess()
                if (loadChapRes) {
                    // 等待视图宽高数据，用来分页
                    while (!attached) {
                        Thread.sleep(100)  // 如果该while循环体为空，可能会造成视图一直卡在加载章节中视图
                    }
                    post {  // 章节内容完成加载，且视图宽高数据已经完成初始化，开始分页，并在完成以后立刻刷新视图
                        ReadBook.splitCurAndPreprocess()
                        refreshAllPage()
                    }
                } else {
                    Log.d(TAG, "openBook: 加载章节内容失败")
                }
            } else {
                showTocInitFailedView()             // 加载目录失败
            }
        }
    }

    fun openFile(
        file: File,
        @IntRange(from = 1) chapIndex: Int = 1,
        @IntRange(from = 1) pageIndex: Int = 1
    ) {
        openBook(DefaultNativeLoader(file), chapIndex, pageIndex)
    }

    fun showText(text: String) {
        openBook(SimpleTextLoader(text))
    }

    interface Callback {
        fun onTocInitSuccess(bookData: BookData) = Unit
        fun onTocInitFailed(e: Exception) = Unit
        fun onLoadChap() = Unit
    }
}