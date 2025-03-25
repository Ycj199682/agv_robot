package com.reeman.agv.utils

import android.media.MediaExtractor
import timber.log.Timber

object PlayUtils {

    /**
     * 检查音频文件是否损坏
     */
    fun isMp3FileCorrupted(filePath: String): Boolean {
        val mediaExtractor = MediaExtractor()
        return try {
            mediaExtractor.setDataSource(filePath)
            val trackCount = mediaExtractor.trackCount
            Timber.w("音轨: $trackCount")
            trackCount <= 0
        } catch (e: Exception) {
            Timber.w(e,"音频文件已损坏")
            true
        } finally {
            mediaExtractor.release()
        }
    }

}