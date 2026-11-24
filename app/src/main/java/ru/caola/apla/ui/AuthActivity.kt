package ru.caola.apla.ui

import android.content.Intent
import android.os.Bundle

/** Backward-compatible entry point for older navigation references. */
class AuthActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
