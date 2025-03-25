package com.reeman.commons.utils

import android.os.FileObserver
import android.util.Log
import timber.log.Timber
import java.io.File

class UsbFileObserver(
    val path: String,
    private val callback: UsbEventCallback
) : FileObserver(path, ALL_EVENTS) {

    override fun onEvent(event: Int, path: String?) {
        Log.w(this::class.simpleName, "event: $event, path: ${this.path}")
        when (event) {
            ATTRIB -> {
                Timber.w("attrib : ${this.path}")
                if (!File(this.path).exists()) {
                    callback.onUsbDeviceDetached(path)
                }
            }
        }
    }
}

interface UsbEventCallback {
    fun onUsbDeviceDetached(path: String?)
}

