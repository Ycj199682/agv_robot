package com.reeman.agv.utils

import android.content.res.Resources
import android.view.View
import android.widget.TextView
import kotlinx.coroutines.*
import timber.log.Timber

interface DebounceClickListener {
    val clickDelay: Long
        get() = 200L
    val clickJobs: MutableMap<View, Job?>
        get() = mutableMapOf()
    fun View.setDebounceClickListener(listener: (View) -> Unit) {
        this.setOnClickListener { view ->
            clickJobs.values.forEach { it?.cancel() }
            val newJob = CoroutineScope(Dispatchers.Main).launch {
                delay(clickDelay)
                val viewIdName = try {
                    resources.getResourceEntryName(view.id)
                } catch (e: Resources.NotFoundException) {
                    "unknown"
                }
                if (view is TextView) {
                    Timber.tag(this@DebounceClickListener::class.java.simpleName).w("点击[${view.text}] (ID: $viewIdName)")
                } else {
                    Timber.tag(this@DebounceClickListener::class.java.simpleName).w("点击View (ID: $viewIdName)")
                }
                listener(view)
                clickJobs.remove(view)
            }
            clickJobs[view] = newJob
        }
    }
}
