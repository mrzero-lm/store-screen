package com.storescreen.player

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.ByteArrayInputStream

/**
 * Full-screen WebView that serves the files in assets/ from a real https address,
 * which is what lets YouTube embeds play (they refuse file:// pages).
 *
 * Remote: right = next video, up = ad break now, down = demo speed on/off.
 */
class MainActivity : Activity() {

    private lateinit var web: WebView
    private val host = "com.storescreen.player"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        web = WebView(this)
        web.setBackgroundColor(0xFF000000.toInt())
        setContentView(web)

        with(web.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        }
        web.webChromeClient = WebChromeClient()
        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val uri = request?.url ?: return null
                if (uri.host != host) return null
                var path = (uri.path ?: "").trimStart('/')
                if (path.isEmpty()) path = "index.html"
                return try {
                    WebResourceResponse(mimeFor(path), "utf-8", assets.open(path))
                } catch (e: Exception) {
                    WebResourceResponse(
                        "text/plain", "utf-8", 404, "Not Found",
                        emptyMap(), ByteArrayInputStream(ByteArray(0))
                    )
                }
            }
        }
        web.loadUrl("https://$host/index.html")
    }

    private fun mimeFor(path: String): String =
        when (path.substringAfterLast('.', "").lowercase()) {
            "html" -> "text/html"
            "json" -> "application/json"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "js" -> "application/javascript"
            "css" -> "text/css"
            else -> "application/octet-stream"
        }

    @Suppress("DEPRECATION")
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val js = when (keyCode) {
            KeyEvent.KEYCODE_DPAD_RIGHT -> "window.nextItem && nextItem()"
            KeyEvent.KEYCODE_DPAD_UP -> "window.adBreakNow && adBreakNow()"
            KeyEvent.KEYCODE_DPAD_DOWN -> "window.toggleDemo && toggleDemo()"
            else -> null
        }
        if (js != null) {
            web.evaluateJavascript(js, null)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onResume() {
        super.onResume()
        web.onResume()
    }

    override fun onPause() {
        web.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        web.destroy()
        super.onDestroy()
    }
}
