package com.youravapp.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class ThreatOverlayService : Service() {
    private var wm: WindowManager? = null
    private var rootView: LinearLayout? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.canDrawOverlays(this)) return START_NOT_STICKY
        val filePath = intent?.getStringExtra("filePath").orEmpty()
        val threat = intent?.getStringExtra("threat").orEmpty()

        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        rootView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            addView(TextView(context).apply { text = "Threat detected" })
            addView(TextView(context).apply { text = filePath })
            addView(TextView(context).apply { text = threat })
            addView(Button(context).apply { text = "Open app"; setOnClickListener { stopSelf() } })
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP }
        wm?.addView(rootView, params)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        rootView?.let { wm?.removeView(it) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
