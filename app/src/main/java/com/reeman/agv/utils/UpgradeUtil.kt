package com.reeman.agv.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.reeman.agv.R
import com.reeman.agv.request.ServiceFactory
import com.reeman.agv.request.url.API
import com.reeman.commons.constants.Constants
import com.reeman.commons.exceptions.CustomHttpException
import com.reeman.commons.model.request.ApkInfo
import com.reeman.commons.utils.SpManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

/**
 * 升级工具类
 */
object UpgradeUtil {

    var lastProcess = 0
    private var downloadJob: Job? = null
    private var apkInfo: ApkInfo? = null
    private var onProgressUpdate: ((progress: Int) -> Unit)? = null
    private var onSuccess: ((filePath: String) -> Unit)? = null
    private var onFailure: ((error: Throwable) -> Unit)? = null
    var isBackgroundDownload = false

    fun isDownloading() = downloadJob?.isActive == true

    /**
     * 获取后台下载完成的升级包
     */
    fun getUpgradeInfo(context: Context, dir: File): ApkInfo? {
        fun clearDir() {
            if (dir.isDirectory) {
                dir.listFiles()?.forEach {
                    it.delete()
                }
            }
        }

        val upgradeInfoJson = SpManager.getInstance().getString(Constants.KEY_UPGRADE_INFO, null)
        if (upgradeInfoJson.isNullOrBlank()) {
            clearDir()
            return null
        }
        return upgradeInfoJson.run {
            val cacheApkInfo = Gson().fromJson(this, ApkInfo::class.java)
            if (!cacheApkInfo.localPath.isNullOrBlank()) {
                val file = File(cacheApkInfo.localPath!!)
                if (file.exists()) {
                    val currentPackageName = context.packageName
                    val packageInfo = context.packageManager.getPackageInfo(
                        currentPackageName,
                        0
                    )
                    val versionCode =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            packageInfo.longVersionCode
                        } else {
                            packageInfo.versionCode.toLong()
                        }
                    if (versionCode <= cacheApkInfo.version.toInt()) {
                        return cacheApkInfo
                    }
                    file.delete()
                }
            }
            clearDir()
            SpManager.getInstance().edit().remove(Constants.KEY_UPGRADE_INFO).apply()
            return null
        }

    }


    /**
     * 根据升级包编号获取相关信息
     */
    fun getApkInfo(
        appId: String,
        apiToken: String,
        onGetApkInfoSuccess: (apkInfo: ApkInfo) -> Unit,
        onGetApkInfoFailure: (throwable: Throwable) -> Unit
    ) {
        GlobalScope.launch {
            try {
                val response =
                    ServiceFactory.getRobotService().getApkInfo(API.getAPKInfoAPI(appId), apiToken)
                if (response.isSuccessful) {
                    response.body()?.let {
                        Timber.w("body: $it")
                        it.let { apkInfo ->
                            this@UpgradeUtil.apkInfo = apkInfo
                            withContext(Dispatchers.Main) {
                                onGetApkInfoSuccess(apkInfo)
                            }
                        }
                    } ?: throw CustomHttpException(-1, "Response body is null")
                } else {
                    throw CustomHttpException(-2, response.message())
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onGetApkInfoFailure(e)
                }
                Timber.w(e, "获取升级包信息失败")
            }
        }
    }

    /**
     * 开始下载
     */
    fun startFileDownload(
        url: String,
        outputFile: File,
        onProgressUpdate: (progress: Int) -> Unit,
        onSuccess: (filePath: String) -> Unit,
        onFailure: (error: Throwable) -> Unit
    ) {
        this.onProgressUpdate = onProgressUpdate
        this.onSuccess = onSuccess
        this.onFailure = onFailure
        Timber.w("开始下载 : $url ")
        downloadJob = GlobalScope.launch {
            downloadFile(url, outputFile)
        }
    }

    private suspend fun downloadFile(
        url: String,
        outputFileDir: File,
    ) {
        withContext(Dispatchers.IO) {
            var outputFile: File? = null
            try {
                var response =
                    ServiceFactory.getRobotService().downloadApplication(url)
                var redirectCount = 0
                val maxRedirects = 5
                while (response.code() == 302 && redirectCount < maxRedirects) {
                    val redirectUrl = response.headers()["Location"]
                    if (redirectUrl != null) {
                        response = ServiceFactory.getRobotService().downloadApplication(redirectUrl)
                        redirectCount++
                    } else {
                        throw CustomHttpException(-3, "Redirect without Location header")
                    }
                }

                if (response.isSuccessful) {
                    val contentDisposition = response.headers()["Content-Disposition"]
                    val fileName = contentDisposition?.let {
                        if (it.contains("filename*")) {
                            val fileName = it.substringAfter("filename*=").substringAfter("''")
                            java.net.URLDecoder.decode(fileName, "UTF-8")
                        } else {
                            it.substringAfter("filename=")
                        }
                    } ?: throw CustomHttpException(-4, "Unknown file : $contentDisposition")
                    outputFile = File(outputFileDir, fileName)
                    response.body()?.let { body ->
                        val inputStream = body.byteStream()
                        val outputStream = FileOutputStream(outputFile)
                        val totalBytes = body.contentLength()
                        var bytesCopied: Long = 0
                        val buffer = ByteArray(1024)
                        var bytesRead: Int

                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            if (!isActive) {
                                throw CancellationException("Cancel download")
                            }
                            outputStream.write(buffer, 0, bytesRead)
                            bytesCopied += bytesRead
                            val progress = (bytesCopied * 100 / totalBytes).toInt()
                            if (lastProcess != progress) {
                                lastProcess = progress
                                withContext(Dispatchers.Main) {
                                    onProgressUpdate?.let {
                                        it(progress)
                                    }
                                }
                            }
                        }

                        outputStream.flush()
                        outputStream.close()
                        inputStream.close()
                        withContext(Dispatchers.Main) {
                            Timber.w("isBackgroundDownload : $isBackgroundDownload , apkInfo: $apkInfo")
                            if (isBackgroundDownload) {
                                apkInfo?.apply {
                                    this.localPath = outputFile.path
                                    SpManager.getInstance().edit().putString(
                                        Constants.KEY_UPGRADE_INFO,
                                        Gson().toJson(this)
                                    ).apply()

                                    ToastUtils.showShortToast("Download success")
                                }
                            }

                            onSuccess?.invoke(outputFile.path)
                        }
                        Timber.w("download success: ${outputFile.path}")
                    } ?: throw CustomHttpException(-1, "Response body is null")
                } else {
                    throw CustomHttpException(-2, response.message())
                }
            } catch (e: Exception) {
                lastProcess = 0
                if (e is CancellationException) {
                    Timber.w("下载取消")
                } else {
                    withContext(Dispatchers.Main) {
                        onFailure?.let { it(e) }
                    }
                    Timber.w(e, "下载失败")
                }
                outputFile?.let {
                    if (it.exists()) {
                        it.delete()
                        Timber.w("删除未下载完成的文件")
                    }
                }
            }
        }
    }

    fun installApk(context: Context, apkFile: File) {
        if (apkFile.exists()) {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            val apkUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val authority = "${context.packageName}.fileprovider"
                FileProvider.getUriForFile(context, authority, apkFile)
            } else {
                Uri.fromFile(apkFile)
            }
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

            context.startActivity(intent)
        } else {
            ToastUtils.showShortToast(context.getString(R.string.text_apk_file_not_exist))
            Timber.e("APK file does not exist: ${apkFile.path}")
        }
    }


    /**
     * 取消下载
     */
    fun cancelDownload() {
        lastProcess = 0
        downloadJob?.cancel()
    }

    /**
     * 后台下载后退出升级界面时释放回调
     */
    fun releaseDownloadCallback() {
        onProgressUpdate = null
        onSuccess = null
        onFailure = null
    }
}