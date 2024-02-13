package org.peyilo.readview.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import org.peyilo.readview.provider.PageData

/**
 * 提供段落评论按钮的正文显示视图
 */
class ContentReviewView(context: Context, attrs: AttributeSet? = null):
    View(context, attrs), ReadContent {
    override fun setContent(page: PageData) {
        TODO("Not yet implemented")
    }
}