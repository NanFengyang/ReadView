package org.peyilo.readview.data

/**
 * 保存章节数据
 */
class ChapData(
    val chapIndex: Int
): AdditionalData(), BookChild {
    var title: String? = null
    lateinit var content: String
    lateinit var parent: DataContainer
}