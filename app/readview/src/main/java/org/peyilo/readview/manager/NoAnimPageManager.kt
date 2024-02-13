package org.peyilo.readview.manager

import org.peyilo.readview.ui.PageContainer
import org.peyilo.readview.ui.PageDirection
import org.peyilo.readview.utils.invisible
import org.peyilo.readview.utils.visible

// 无动画翻页
class NoAnimPageManager(readView: PageContainer): HorizontalPageManager(readView) {

    // 由于没有动画，屏幕内始终只显示了一个page，无需设置scroll，隐藏另外两个page即可
    override fun initPageScroll() {
        prevPage.invisible()
        nextPage.invisible()
    }

    // 由于NoAnimPageManager设置了不可见视图，当PageManager发生改变时，在被移除之前需要进行一些恢复操作
    // 例如恢复可见性
    override fun onManagerChanged() {
        prevPage.visible()
        nextPage.visible()
    }

    override fun startNextAnim() {      // 通过控制page的可见性即可实现翻页效果
        nextPage.visible()
        curPage.invisible()
    }

    override fun startPrevAnim() {
        prevPage.visible()
        curPage.invisible()
    }

}