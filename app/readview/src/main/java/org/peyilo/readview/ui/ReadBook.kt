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
    @IntRange(from = 1) var preprocessBefore = 2       // 预加载当前章节的前两章
    @IntRange(from = 1) var preprocessBehind = 2        // 预加载当前章节的后两章

    private lateinit var bookData: BookData
    private var bookDataInitialized = false
    val config = ReadConfig()
    lateinit var loader: BookLoader
    lateinit var parser: ContentParser
    lateinit var provider: PageProvider
    // 初始化目录信息时，chapStatusTable就完成了初始化
    private val chapStatusTable = mutableMapOf<Int, ChapStatus>()
    // 只有相应的章节完成加载以后才会添加进readChapterTable
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
        bookDataInitialized = true
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
                    val readChapter = parser.parse(chapData)    // 解析ChapData
                    readChapterTable[chapIndex] = readChapter
                    chapStatusTable[chapIndex] = ChapStatus.Nonpaged        // 更新状态
                    Log.d(TAG, "loadChap: chapIndex = $chapIndex")
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
            val status = chapStatusTable[chapIndex]!!
            if (status == ChapStatus.Unload) {
                throw IllegalStateException("章节[$chapIndex]未完成加载，不能进行分页!")
            } else if (status == ChapStatus.Nonpaged) {
                val readChapter = readChapterTable[chapIndex]!!
                provider.split(readChapter)
                chapStatusTable[chapIndex] = ChapStatus.Finished
                Log.d(TAG, "splitChap: chapIndex=$chapIndex")
            }
        }
    }

    private fun preprocess(chapIndex: Int, process: (index: Int) -> Unit) {
        process(chapIndex)
        var i = chapIndex - 1
        while (i > 0 && i >= chapIndex - preprocessBefore) {
            process(i)
            i--
        }
        i = chapIndex + 1
        while (i <= bookData.chapCount && i <= chapIndex + preprocessBehind) {
            process(i)
            i++
        }
    }

    // 加载当前章节以及预处理章节
    fun loadCurAndPreprocess(): Boolean {
        var failedCount = 0
        preprocess(curChapIndex) {
            val res = loadChap(it)
            if (!res) failedCount++
        }
        return failedCount == 0
    }

    // 对当前章节以及预处理章节进行分页
    fun splitCurAndPreprocess() = preprocess(curChapIndex) {
        splitChap(it)
    }

    private fun getNextPageIndexPair(): Pair<Int, Int> {
        val curChapStatus = chapStatusTable[curChapIndex]!!
        return if (curChapStatus != ChapStatus.Finished) {     // 当前章节未完成分页
            Pair(curChapIndex + 1, 1)
        } else {
            val curChap = readChapterTable[curChapIndex]!!
            if (curPageIndex < curChap.pages.size) {        // 不是当前章节的最后一页
                Pair(curChapIndex, curPageIndex + 1)
            } else {        // 当前页为当前章节的最后一页
                Pair(curChapIndex + 1, 1)
            }
        }
    }

    private fun getPrevPageIndexPair(): Pair<Int, Int> {
        val curChapStatus = chapStatusTable[curChapIndex]!!
        return if (curChapStatus != ChapStatus.Finished) {     // 当前章节未完成分页
            val prevChapStatus = chapStatusTable[curChapIndex - 1]!!
            if (prevChapStatus == ChapStatus.Finished) {
                Pair(curChapIndex - 1, readChapterTable[curChapIndex - 1]!!.pages.size)
            } else {
                Pair(curChapIndex - 1, 1)
            }
        } else {
            if (curPageIndex > 1) {        // 不是当前章节的第一页
                Pair(curChapIndex, curPageIndex - 1)
            } else {        // 当前页为当前章节的第一页
                val prevChapStatus = chapStatusTable[curChapIndex - 1]!!
                if (prevChapStatus == ChapStatus.Finished) {
                    Pair(curChapIndex - 1, readChapterTable[curChapIndex - 1]!!.pages.size)
                } else {
                    Pair(curChapIndex - 1, 1)
                }
            }
        }
    }

    // 注意：该函数不会检查是否有下一页
    @Synchronized fun moveToNextPage(): Boolean {
        val nextPageIndexPair = getNextPageIndexPair()
        val chapIndexChanged = curChapIndex != nextPageIndexPair.first
        curChapIndex = nextPageIndexPair.first
        curPageIndex = nextPageIndexPair.second
        return chapIndexChanged
    }

    @Synchronized fun moveToPrevPage(): Boolean {
        val prevPageIndexPair = getPrevPageIndexPair()
        val chapIndexChanged = curChapIndex != prevPageIndexPair.first
        curChapIndex = prevPageIndexPair.first
        curPageIndex = prevPageIndexPair.second
        return chapIndexChanged
    }

    // 调用该函数之前需要保证chapIndex、pageIndex的有效性
    // 同时需要保证章节已经完成了加载和分页
    fun getPage(chapIndex: Int, pageIndex: Int): PageData {
        Log.d(TAG, "getPage: chapIndex=$chapIndex, pageIndex=$pageIndex")
        val curChapStatus = chapStatusTable[chapIndex]!!
        return if (curChapStatus == ChapStatus.Finished) {
            readChapterTable[chapIndex]!!.pages[pageIndex - 1]
        } else {
            TODO("返回一个章节加载中的页面")
        }
    }

    fun getCurPage(): PageData {
        return getPage(curChapIndex, curPageIndex)
    }

    fun getNextPage(): PageData {
        val nextPageIndexPair = getNextPageIndexPair()
        return getPage(nextPageIndexPair.first, nextPageIndexPair.second)
    }

    fun getPrevPage(): PageData {
        val prevPageIndexPair = getPrevPageIndexPair()
        return getPage(prevPageIndexPair.first, prevPageIndexPair.second)
    }

    fun hasNextChap(): Boolean {
        return if (!bookDataInitialized) {
            false
        } else {
            curChapIndex < bookData.chapCount
        }
    }

    fun hasPrevChap(): Boolean {
        return if (!bookDataInitialized) {
            false
        } else {
            curChapIndex > 1
        }
    }

    fun hasNextPage(): Boolean {
        return if (!bookDataInitialized) {                  // BookData还未完成目录初始化
            false
        } else if (curChapIndex < bookData.chapCount) {     // 不是最后一章节
            true
        } else {        // 最后一章节
            val curChapStatus = chapStatusTable[curChapIndex]!!
            if (curChapStatus != ChapStatus.Finished) {     // 最后一章节未完成分页
                false
            } else {        // 完成了分页
                curPageIndex < readChapterTable[curChapIndex]!!.pages.size
            }
        }
    }

    fun hasPrevPage(): Boolean {
        return if (!bookDataInitialized) {
            false
        } else if (curChapIndex > 1) {      // 不是第一章节
            true
        } else {        // 第一章节
            val curChapStatus = chapStatusTable[curChapIndex]!!
            if (curChapStatus != ChapStatus.Finished) {
                false
            } else {
                curPageIndex != 1
            }
        }
    }


}