package org.peyilo.readview.ui

import android.util.Log
import org.peyilo.readview.data.BookData
import org.peyilo.readview.loader.BookLoader
import androidx.annotation.IntRange
import org.peyilo.readview.annotation.ThreadSafe
import org.peyilo.readview.parser.ContentParser
import org.peyilo.readview.parser.ReadChapter
import org.peyilo.readview.provider.PageData
import org.peyilo.readview.provider.PageProvider

private const val TAG = "ReadBook"
object ReadBook {

    @IntRange(from = 1) var curChapIndex = 1
    @IntRange(from = 1) var curPageIndex = 1

    private lateinit var bookData: BookData
    val config = ReadConfig()
    lateinit var loader: BookLoader
    lateinit var parser: ContentParser
    lateinit var provider: PageProvider
    private val chapStatusTable = mutableMapOf<Int, ChapStatus>()
    private val readChapterTable = mutableMapOf<Int, ReadChapter>()

    private enum class ChapStatus {
        Unload,                 // 未加载
        Nonpaged,               // 未分页
        Finished                // 全部完成
    }

    fun initToc(): Boolean {
        bookData = loader.initToc()
        for (i in 1..bookData.chapCount) {              // 初始化章节状态表
            chapStatusTable[i] = ChapStatus.Unload
        }
        return true
    }

    /**
     * 多个线程调用该函数加载相同章节时，会触发竞态条件，因而需要对该章节的状态进行同步
     */
    @ThreadSafe
    fun loadChap(@IntRange(from = 1) chapIndex: Int): Boolean {
        synchronized(chapStatusTable[chapIndex]!!) {
            if (chapStatusTable[chapIndex] == ChapStatus.Unload) {      // 未加载
                try {
                    val chapData = bookData.getChap(chapIndex)
                    loader.loadChap(chapData)
                    readChapterTable[chapIndex] = parser.parse(chapData)    // 解析ChapData
                    chapStatusTable[chapIndex] = ChapStatus.Nonpaged        // 更新状态
                } catch (e: Exception) {        // 加载失败
                    Log.d(TAG, "loadChap: ${e.stackTrace}")
                    return false
                }
            }
        }
        return true
    }


    @ThreadSafe
    fun splitChap(@IntRange(from = 1) chapIndex: Int) {
        synchronized(chapStatusTable[chapIndex]!!) {
            when (chapStatusTable[chapIndex]!!) {
                ChapStatus.Unload -> {
                    throw IllegalStateException("章节[$chapIndex]未完成加载，不能进行分页!")
                }
                ChapStatus.Nonpaged -> {
                    val readChapter = readChapterTable[chapIndex]!!
                    provider.split(readChapter)
                    chapStatusTable[chapIndex] = ChapStatus.Finished
                }
                ChapStatus.Finished -> Unit
            }
        }
    }

    fun loadCurChap(): Boolean = loadChap(curChapIndex)

    fun splitCurChap() = splitChap(curChapIndex)

    fun getPage(chapIndex: Int, pageIndex: Int): PageData {
        return readChapterTable[chapIndex]!!.pages[pageIndex - 1]
    }
    fun hasNextChap(): Boolean {
        if (curChapIndex >= bookData.chapCount) return false
        return true
    }

    fun hasPrevChap(): Boolean {
        if (curChapIndex <= 1) return false
        return true
    }

    fun hasNextPage() {

    }
    fun hasPrevPage() = false
}