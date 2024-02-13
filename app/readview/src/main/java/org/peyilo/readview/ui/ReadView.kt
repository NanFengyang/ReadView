package org.peyilo.readview.ui

import android.content.Context
import android.util.AttributeSet
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

    override fun hasNextPage(): Boolean = ReadBook.hasNextPage()

    override fun hasPrevPage(): Boolean = ReadBook.hasPrevPage()

    override fun updatePage(page: ReadPage, dire: PageDirection) = Unit

    private fun prepareInitToc() {
        showTocInitializingView()
    }

    private fun showTocInitializingView() {

    }

    private fun showTocInitFailedView() {

    }

    private fun showChapLoadingView() {

    }

    private fun refreshAllPage() {
        (curPage.content as ContentView).setContent(ReadBook.getPage(ReadBook.curChapIndex, ReadBook.curPageIndex + 1))
        (nextPage.content as ContentView).setContent(ReadBook.getPage(ReadBook.curChapIndex, ReadBook.curPageIndex + 2))
        (prevPage.content as ContentView).setContent(ReadBook.getPage(ReadBook.curChapIndex, ReadBook.curPageIndex))
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
        prepareInitToc()
        startTask {
            val initTocRes = ReadBook.initToc()
            if (initTocRes) {
                showChapLoadingView()
                val loadChapRes = ReadBook.loadCurChap()
                if (loadChapRes) {
                    // 等待视图宽高数据，用来分页
                    while (!attached) {
                        Thread.sleep(100)
                    }
                    post {
                        ReadBook.splitCurChap()
                        refreshAllPage()
                    }
                }
            } else {
                showTocInitFailedView()
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