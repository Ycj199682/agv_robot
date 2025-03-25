package com.reeman.agv.fragments.setting.strategy

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.text.InputFilter
import android.text.Spanned
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import com.google.gson.Gson
import com.kyleduo.switchbutton.SwitchButton
import com.reeman.agv.R
import com.reeman.agv.activities.CallingConfigActivity
import com.reeman.agv.calling.CallingInfo
import com.reeman.agv.calling.button.CallingHelper
import com.reeman.agv.calling.model.MulticastSendInfo
import com.reeman.agv.calling.setting.ModeCallingSetting
import com.reeman.agv.utils.DebounceClickListener
import com.reeman.agv.utils.MulticastMessageSender
import com.reeman.agv.utils.PackageUtils
import com.reeman.agv.utils.ToastUtils
import com.reeman.agv.utils.ViewUtils
import com.reeman.agv.widgets.EasyDialog
import com.reeman.agv.widgets.ExpandableLayout
import com.reeman.agv.widgets.QRCodePairingDialog
import com.reeman.commons.constants.Constants
import com.reeman.commons.state.RobotInfo
import com.reeman.commons.utils.AESUtil
import com.reeman.commons.utils.AndroidInfoUtil
import com.reeman.commons.utils.QrCodeUtil
import com.reeman.commons.utils.SpManager
import com.reeman.commons.utils.WIFIUtils
import com.warkiz.widget.IndicatorSeekBar
import com.warkiz.widget.OnSeekChangeListener
import com.warkiz.widget.SeekParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.security.GeneralSecurityException
import java.util.regex.Pattern

class CallingModelInitializer : LayoutInitializer, OnSeekChangeListener,
    DebounceClickListener,
        (View) -> Unit {
    private lateinit var context: Context
    private val gson = Gson()

    private lateinit var isbAdjustCallingModeSpeed: IndicatorSeekBar
    private lateinit var isbAdjustCallingModeWaitingTime: IndicatorSeekBar
    private lateinit var isbStartCallingTaskCountDownTime: IndicatorSeekBar
    private lateinit var isbAdjustCallingModeCacheTime: IndicatorSeekBar
    private lateinit var swEnableCallingQueue: SwitchButton
    private lateinit var etCallingKey: EditText
    private lateinit var layoutStartCallingTaskCountDownTime: LinearLayout
    private lateinit var rgStartCallingTaskCountDown: RadioGroup


    override fun initLayout(context: Context, root: ExpandableLayout) {
        this.context = context
        fun <T : View> findView(id: Int): T = root.findViewById(id)

        fun initView() {
            isbAdjustCallingModeSpeed = findView(R.id.isb_adjust_calling_mode_speed)
            isbAdjustCallingModeWaitingTime = findView(R.id.isb_calling_waiting_time)
            isbAdjustCallingModeCacheTime = findView(R.id.isb_calling_cache_time)
            isbStartCallingTaskCountDownTime = findView(R.id.isb_start_calling_task_count_down_time)
            swEnableCallingQueue = findView(R.id.sw_enable_calling_queue)
            etCallingKey = findView(R.id.et_calling_key)
            etCallingKey.filters = arrayOf(AlphaNumericInputFilter(8))
            rgStartCallingTaskCountDown = findView(R.id.rg_start_calling_task_count_down)
            layoutStartCallingTaskCountDownTime =
                findView(R.id.layout_start_calling_task_count_down_time)
            if (!CallingInfo.callingModeSetting.startTaskCountDownSwitch) {
                layoutStartCallingTaskCountDownTime.visibility = View.GONE
            }
        }

        fun initListener() {
            findView<ImageButton>(R.id.ib_increase_calling_mode_speed).setDebounceClickListener(this)
            findView<ImageButton>(R.id.ib_decrease_calling_mode_speed).setDebounceClickListener(this)
            findView<ImageButton>(R.id.ib_increase_calling_waiting_time).setDebounceClickListener(this)
            findView<ImageButton>(R.id.ib_decrease_calling_waiting_time).setDebounceClickListener(this)
            findView<ImageButton>(R.id.ib_increase_calling_cache_time).setDebounceClickListener(this)
            findView<ImageButton>(R.id.ib_decrease_calling_cache_time).setDebounceClickListener(this)
            findView<ImageButton>(R.id.ib_decrease_start_calling_task_count_down_time).setDebounceClickListener(this)
            findView<ImageButton>(R.id.ib_increase_start_calling_task_count_down_time).setDebounceClickListener(this)
            findView<Button>(R.id.btn_calling_config).setDebounceClickListener(this)
            findView<Button>(R.id.btn_save_calling_key).setDebounceClickListener(this)
            findView<Button>(R.id.btn_calling_pairing_config_by_multicast).setDebounceClickListener(this)
            findView<Button>(R.id.btn_calling_pairing_config_by_qrcode).setDebounceClickListener(this)
            findView<RadioButton>(R.id.rb_open_start_calling_task_count_down).setDebounceClickListener(this)
            findView<RadioButton>(R.id.rb_close_start_calling_task_count_down).setDebounceClickListener(this)
            isbAdjustCallingModeSpeed.onSeekChangeListener = this
            isbAdjustCallingModeWaitingTime.onSeekChangeListener = this
            isbStartCallingTaskCountDownTime.onSeekChangeListener = this
            isbAdjustCallingModeCacheTime.onSeekChangeListener = this
            swEnableCallingQueue.setOnCheckedChangeListener { _, isChecked ->
                updateCallingModeConfig { this.openCallingQueue = isChecked }
            }
        }

        fun initData() {
            CallingInfo.callingModeSetting.let {
                etCallingKey.setText(it.key.first)
                isbAdjustCallingModeSpeed.setProgress(it.speed)
                isbAdjustCallingModeWaitingTime.setProgress(it.waitingTime.toFloat())
                isbAdjustCallingModeCacheTime.setProgress(it.cacheTime.toFloat())
                isbStartCallingTaskCountDownTime.setProgress(it.startTaskCountDownTime.toFloat())
                swEnableCallingQueue.isChecked = it.openCallingQueue
                rgStartCallingTaskCountDown.check(if (it.startTaskCountDownSwitch) R.id.rb_open_start_calling_task_count_down else R.id.rb_close_start_calling_task_count_down)
            }
        }

        if (root.tag == null) {
            initView()
            initListener()
            root.tag = "initialized"
        }
        initData()
    }

    private fun updateCallingModeConfig(update: ModeCallingSetting.() -> Unit) {
        CallingInfo.callingModeSetting.apply {
            update()
            Timber.w("修改呼叫模式设置: $this")
            SpManager.getInstance().edit()
                .putString(Constants.KEY_CALLING_MODE_CONFIG, gson.toJson(this)).apply()
        }
    }

    override fun onSeeking(seekParams: SeekParams?) {

    }

    override fun onStartTrackingTouch(seekBar: IndicatorSeekBar?) {

    }

    override fun onStopTrackingTouch(seekBar: IndicatorSeekBar) {
        when (seekBar.id) {
            R.id.isb_adjust_calling_mode_speed -> updateCallingModeConfig { this.speed = ViewUtils.onFloatValueChange(seekBar, false) }
            R.id.isb_calling_waiting_time -> updateCallingModeConfig { this.waitingTime = ViewUtils.onIntValueChange(seekBar, false) }
            R.id.isb_calling_cache_time -> updateCallingModeConfig { this.cacheTime = ViewUtils.onIntValueChange(seekBar, false) }
            R.id.isb_start_calling_task_count_down_time -> updateCallingModeConfig { this.startTaskCountDownTime = ViewUtils.onIntValueChange(seekBar, false) }
        }
    }

    override fun invoke(v: View) {
        when (v.id) {
            R.id.ib_increase_calling_mode_speed -> updateCallingModeConfig {
                this.speed = ViewUtils.onFloatValueChange(v, true)
            }

            R.id.ib_decrease_calling_mode_speed -> updateCallingModeConfig {
                this.speed = ViewUtils.onFloatValueChange(v, false)
            }

            R.id.ib_increase_calling_waiting_time -> updateCallingModeConfig {
                this.waitingTime = ViewUtils.onIntValueChange(v, true)
            }

            R.id.ib_decrease_calling_waiting_time -> updateCallingModeConfig {
                this.waitingTime = ViewUtils.onIntValueChange(v, false)
            }

            R.id.ib_increase_calling_cache_time -> updateCallingModeConfig {
                this.cacheTime = ViewUtils.onIntValueChange(v, true)
            }

            R.id.ib_decrease_calling_cache_time -> updateCallingModeConfig {
                this.cacheTime = ViewUtils.onIntValueChange(v, false)
            }

            R.id.ib_increase_start_calling_task_count_down_time -> updateCallingModeConfig {
                this.startTaskCountDownTime = ViewUtils.onIntValueChange(v, true)
            }

            R.id.ib_decrease_start_calling_task_count_down_time -> updateCallingModeConfig {
                this.startTaskCountDownTime = ViewUtils.onIntValueChange(v, false)
            }

            R.id.btn_calling_config -> {
                if (CallingHelper.isStart()) {
                    CallingHelper.stop()
                }
                CoroutineScope(Dispatchers.Main).launch {
                    EasyDialog.getLoadingInstance(context)
                        .loading(context.getString(R.string.text_enter_calling_config))
                    delay(2000)
                    if (EasyDialog.isShow()) EasyDialog.getInstance().dismiss()
                    context.startActivity(Intent(context, CallingConfigActivity::class.java))
                }
            }

            R.id.btn_save_calling_key -> {
                val keyInput = etCallingKey.text?.toString()
                if (keyInput.isNullOrBlank() || keyInput.length != 8) {
                    etCallingKey.error =
                        context.getString(R.string.text_please_input_num_and_letters_and_length_8)
                    return
                }
                updateCallingModeConfig {
                    try {
                        if (this.key.first == keyInput) {
                            ToastUtils.showShortToast(context.getString(R.string.text_calling_key_same))
                            return@updateCallingModeConfig
                        }
                        val encrypt = AESUtil.encrypt("a123456", PackageUtils.getVersion(context) + AndroidInfoUtil.getSerialNumber() + System.currentTimeMillis())
                        val token = encrypt.substring(8)
                        val tokens = listOf(token)
                        Timber.w("key : $keyInput, token: $token")
                        this.key = Pair(keyInput, tokens)
                        ToastUtils.showShortToast(context.getString(R.string.text_save_key_success))
                    } catch (e: GeneralSecurityException) {
                        Timber.w(e, "生成key失败")
                        ToastUtils.showShortToast(context.getString(R.string.text_create_token_and_save_key_failed))
                    }
                }
            }

            R.id.btn_calling_pairing_config_by_multicast ->
                EasyDialog.getInstance(context)
                    .confirm(
                        context.getString(
                            R.string.text_please_confirm_wifi_before_pairing,
                            WIFIUtils.getConnectWifiSSID(context)
                        )
                    ) { dialog, id ->
                        dialog.dismiss()
                        if (id == R.id.btn_confirm) {
                            enterPairingMode()
                        }
                    }

            R.id.btn_calling_pairing_config_by_qrcode ->
                createPairingInfo()?.let {
                    QrCodeUtil.createQRCodeBitmap(
                        it, 300, 300, "UTF-8", "H"
                    )?.let { bitmap ->
                        QRCodePairingDialog(context, bitmap).show()
                    } ?: {
                        ToastUtils.showShortToast(context.getString(R.string.text_create_qrcode_failed))
                    }
                }

            R.id.rb_open_start_calling_task_count_down ->
                updateCallingModeConfig {
                    this.startTaskCountDownSwitch = true
                    layoutStartCallingTaskCountDownTime.visibility = View.VISIBLE
                }

            R.id.rb_close_start_calling_task_count_down ->
                updateCallingModeConfig {
                    this.startTaskCountDownSwitch = false
                    layoutStartCallingTaskCountDownTime.visibility = View.GONE
                }
        }
    }

    private fun createPairingInfo() =
        try {
            val encrypt = AESUtil.encrypt("a123456", PackageUtils.getVersion(context) + AndroidInfoUtil.getSerialNumber() + System.currentTimeMillis())
            val currentToken = encrypt.take(8)
            updateCallingModeConfig {
                this.key.second.add(currentToken)
            }
            val pairInfo = gson.toJson(
                MulticastSendInfo(
                    RobotInfo.ROSHostname,
                    RobotInfo.robotAlias ?: "",
                    CallingInfo.callingModeSetting.key.first,
                    currentToken,
                    RobotInfo.robotType
                )
            )
            Timber.w("生成配对信息: $pairInfo")
            pairInfo
        } catch (e: GeneralSecurityException) {
            Timber.w(e, "创建配对信息失败")
            ToastUtils.showShortToast(context.getString(R.string.text_create_pairing_info_failed))
            null
        }

    private fun enterPairingMode() {
        try {
            val sender = MulticastMessageSender()
            createPairingInfo()?.let {
                sender.startSendingMulticast(it)
                EasyDialog.getCancelableLoadingInstance(context)
                    .loadingCancelable(
                        context.getString(
                            R.string.text_robot_already_enter_pairing_mode_please_check_your_phone,
                            RobotInfo.ROSHostname,
                            RobotInfo.robotAlias
                        ),
                        context.getString(R.string.text_exit_calling_pairing),
                        { dialog: Dialog, _: Int -> dialog.dismiss() }
                    ) {
                        ToastUtils.showShortToast(context.getString(R.string.text_already_exit_calling_pairing))
                        sender.stopSendingMulticast()
                    }
            }
        } catch (e: Exception) {
            Timber.w(e, "加入组播组失败")
            EasyDialog.getInstance(context)
                .warnError(context.getString(R.string.text_enter_multicast_group_failed))
            return
        }
    }

    private class AlphaNumericInputFilter(private val length: Int) : InputFilter {

        private val acceptedChars: Pattern = Pattern.compile("[a-zA-Z0-9]+")

        override fun filter(
            source: CharSequence?,
            start: Int,
            end: Int,
            dest: Spanned?,
            dstart: Int,
            dend: Int
        ): CharSequence? {
            if (source.isNullOrBlank()) {
                return null
            }

            val input = source.toString()

            return if (acceptedChars.matcher(input).matches() && (dest?.length?.plus(input.length)
                    ?: 0) <= length
            ) {
                null
            } else {
                ""
            }
        }
    }

}