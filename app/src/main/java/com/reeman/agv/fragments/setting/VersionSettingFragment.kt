package com.reeman.agv.fragments.setting

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.reeman.agv.BuildConfig
import com.reeman.agv.R
import com.reeman.agv.base.BaseApplication.mApp
import com.reeman.agv.base.BaseFragment
import com.reeman.agv.utils.DebounceClickListener
import com.reeman.agv.utils.PackageUtils
import com.reeman.agv.utils.ToastUtils
import com.reeman.agv.utils.UpgradeUtil
import com.reeman.agv.widgets.EasyDialog
import com.reeman.agv.widgets.ProcessDialog
import com.reeman.commons.constants.Constants
import com.reeman.commons.event.VersionInfoEvent
import com.reeman.commons.exceptions.CustomHttpException
import com.reeman.commons.model.request.ApkInfo
import com.reeman.commons.utils.AESUtil
import com.reeman.commons.utils.MMKVManager
import com.reeman.commons.utils.PrecisionUtils
import com.reeman.commons.utils.TimeUtil
import com.reeman.ros.ROSController
import kotlinx.coroutines.Job
import timber.log.Timber
import java.io.File

class VersionSettingFragment : BaseFragment(), DebounceClickListener, (View) -> Unit {
    private lateinit var tvNavigationVersion: TextView
    private lateinit var tvPowerBoardVersion: TextView
    private lateinit var btnDownloadProcess: Button
    private lateinit var btnUpgrade: Button

    
    

    private var processDialog: ProcessDialog? = null
    private lateinit var dir: File
    override fun getLayoutRes() = R.layout.fragment_version_setting

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val tvAppVersion = findView<TextView>(R.id.tv_app_version)
        tvNavigationVersion = findView(R.id.tv_navigation_version)
        tvPowerBoardVersion = findView(R.id.tv_power_board_version)

        tvAppVersion.text = PackageUtils.getVersion(requireContext())

        tvNavigationVersion.setDebounceClickListener(this)
        tvPowerBoardVersion.setDebounceClickListener(this)
        btnUpgrade.setDebounceClickListener(this)
        btnDownloadProcess.setDebounceClickListener(this)
    }

    override fun onResume() {
        super.onResume()
        ROSController.heartBeat()
    }

    override fun onPause() {
        super.onPause()
        if (UpgradeUtil.isDownloading()) {
            UpgradeUtil.releaseDownloadCallback()
        }
    }

    override fun onVersionEvent(event: VersionInfoEvent) {
        tvNavigationVersion.text = event.softVer
        tvPowerBoardVersion.text = event.hardwareVer
    }

    override fun invoke(v: View) {
        when (v.id) {
            R.id.tv_navigation_version -> {
                tvNavigationVersion.text = ""
                ROSController.heartBeat()
            }

            R.id.tv_power_board_version -> {
                tvPowerBoardVersion.text = ""
                ROSController.heartBeat()
            }
        }
    }
}