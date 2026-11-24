package ru.caola.apla.ui

import android.content.Intent
import android.os.Bundle
import ru.caola.apla.data.DebugLog

class MainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DebugLog.init(applicationContext)
        DebugLog.installCrashHandler()
        startActivity(Intent(this, ModeSelectionActivity::class.java))
        finish()
    }
}
