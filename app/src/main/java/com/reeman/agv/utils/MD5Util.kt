package com.reeman.agv.utils

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object MD5Util {

    fun getFileMD5(file: File): String? {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val inputStream = FileInputStream(file)
            val buffer = ByteArray(1024)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                md.update(buffer, 0, bytesRead)
            }
            val digest = md.digest()
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}