package org.peyilo.readview

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class SimulationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simulation)
        supportActionBar?.hide()
    }
}