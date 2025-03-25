package com.reeman.agv.widgets

import android.content.Context
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import com.reeman.agv.R

class ProcessDialog(
    context: Context,
    process: Int,
    cancelListener: () -> Unit,
    backgroundDownloadListener: () -> Unit
) : BaseDialog(context) {

    private val pbDownload: ProgressBar
    private val tvProcess: TextView


    init {
        val root = LayoutInflater.from(context).inflate(R.layout.layout_dialog_process, null)
        pbDownload = root.findViewById(R.id.pb_download)
        tvProcess = root.findViewById(R.id.tv_progress)
        tvProcess.text = context.getString(R.string.text_download_process, String.format("%s%%", process.toString()))
        pbDownload.progress = process
        root.findViewById<Button>(R.id.btn_cancel).setOnClickListener {
            cancelListener.invoke()
            dismiss()
        }
        root.findViewById<Button>(R.id.btn_background_download).setOnClickListener {
            backgroundDownloadListener.invoke()
            dismiss()
        }
        setContentView(root)
        val window = window
        val params = window!!.attributes
        val displayMetrics = DisplayMetrics()
        window.windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels
        params.width = (screenWidth * 0.5).toInt()
        window.attributes = params
    }

    fun updateProgress(process: Int) {
        pbDownload.progress = process
        tvProcess.text = context.getString(R.string.text_download_process, String.format("%s%%", process.toString()))
    }
}
