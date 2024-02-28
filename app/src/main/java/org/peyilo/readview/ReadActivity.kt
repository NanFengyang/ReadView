package org.peyilo.readview

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import org.peyilo.readview.ui.FlipMode
import org.peyilo.readview.ui.ReadView

class ReadActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_read)
        supportActionBar?.hide()
        val readView = findViewById<ReadView>(R.id.read_view)
        readView.flipMode = FlipMode.Cover
        readView.initPage { page ->
            page.bindLayout(layoutId = R.layout.item_view_page,
                contentId = R.id.page_content,
                headerId = R.id.page_header,
                footerId = R.id.page_footer
            )
            (page.content as View).setBackgroundColor(Color.WHITE)
            page.header!!.setBackgroundColor(Color.WHITE)
            page.footer!!.setBackgroundColor(Color.WHITE)
        }

        readView.setOnClickRegionListener { xPercent, _ ->
            when (xPercent) {
                in 0..30 -> readView.flipToPrevPage()
                in 70..100 -> readView.flipToNextPage()
            }
        }
        when (intent.getIntExtra("MODE", 1)) {
            1 -> {
                readView.openBook(SfacgLoader(217202))
            }
        }

        findViewById<Button>(R.id.mode_no_anim).setOnClickListener {
            readView.flipMode = FlipMode.NoAnim
        }
        findViewById<Button>(R.id.mode_cover).setOnClickListener {
            readView.flipMode = FlipMode.Cover
        }
        findViewById<Button>(R.id.mode_slide).setOnClickListener {
            readView.flipMode = FlipMode.Slide
        }
        findViewById<Button>(R.id.mode_simulation).setOnClickListener {
            readView.flipMode = FlipMode.Simulation
        }
    }
}