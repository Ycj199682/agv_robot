package com.reeman.agv.fragments.setting.strategy

import android.app.Dialog
import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import com.google.gson.Gson
import com.reeman.agv.R
import com.reeman.agv.utils.DebounceClickListener
import com.reeman.agv.utils.ToastUtils
import com.reeman.agv.utils.ViewUtils
import com.reeman.agv.widgets.EasyDialog
import com.reeman.agv.widgets.ExpandableLayout
import com.reeman.agv.widgets.TimePickerDialog
import com.reeman.commons.constants.Constants
import com.reeman.commons.settings.ModeNormalSetting
import com.reeman.commons.state.RobotInfo
import com.reeman.commons.utils.SpManager
import com.warkiz.widget.IndicatorSeekBar
import com.warkiz.widget.OnSeekChangeListener
import com.warkiz.widget.SeekParams
import timber.log.Timber
import java.util.concurrent.TimeUnit

class NormalModelInitializer : LayoutInitializer, OnSeekChangeListener,
    DebounceClickListener,
        (View) -> Unit {

    private val gson = Gson()
    private lateinit var context: Context

    private lateinit var isbAdjustNormalModeSpeed: IndicatorSeekBar

    private lateinit var tvNormalModePickingHours: TextView
    private lateinit var tvNormalModePickingMinutes: TextView
    private lateinit var tvNormalModePickingSeconds: TextView

    private lateinit var btnNormalModePickingTimeSetting: Button

    private lateinit var tvNormalModePlayArrivedTipHours: TextView
    private lateinit var tvNormalModePlayArrivedTipMinutes: TextView
    private lateinit var tvNormalModePlayArrivedSeconds: TextView

    private lateinit var btnNormalModePlayArrivedTipTimeSetting: Button

    private lateinit var rgNormalModeFinishActionControl: RadioGroup
    private lateinit var rgNormalModeWaitingTimeControl: RadioGroup
    private lateinit var rgNormalModeManualLiftControl: RadioGroup

    private lateinit var layoutManualLiftSetting: LinearLayout

    private lateinit var etRobotLengthNormalMode: EditText
    private lateinit var etRobotWidthNormalMode: EditText

    private lateinit var etRobotSizeFrontNormalMode: EditText
    private lateinit var etRobotSizeLeftNormalMode: EditText
    private lateinit var etRobotSizeBehindNormalMode: EditText
    private lateinit var etRobotSizeRightNormalMode: EditText

    private lateinit var etLidarWidthNormalMode: EditText

    private lateinit var rgRotateNormalMode: RadioGroup
    private lateinit var rgStopNearbyNormalMode: RadioGroup

    private lateinit var layoutStartNormalTaskCountDownTime: LinearLayout
    private lateinit var rgStartNormalTaskCountDown: RadioGroup
    private lateinit var isbStartNormalTaskCountDownTime: IndicatorSeekBar


    override fun initLayout(context: Context, root: ExpandableLayout) {
        this.context = context
        fun <T : View> findView(id: Int): T = root.findViewById(id)

        fun initView() {
            isbAdjustNormalModeSpeed = findView(R.id.isb_adjust_normal_mode_speed)
            tvNormalModePickingHours = findView(R.id.tv_picking_hours)
            tvNormalModePickingMinutes = findView(R.id.tv_picking_minutes)
            tvNormalModePickingSeconds = findView(R.id.tv_picking_seconds)
            tvNormalModePlayArrivedTipHours = findView(R.id.tv_play_arrived_tip_hours)
            tvNormalModePlayArrivedTipMinutes = findView(R.id.tv_play_arrived_tip_minutes)
            tvNormalModePlayArrivedSeconds = findView(R.id.tv_play_arrived_tip_seconds)
            btnNormalModePlayArrivedTipTimeSetting = findView(R.id.btn_play_arrived_tip_time_setting)
            rgNormalModeWaitingTimeControl = findView(R.id.rg_waiting_time_control)
            rgNormalModeFinishActionControl = findView(R.id.rg_normal_mode_finish_action)
            rgNormalModeManualLiftControl = findView(R.id.rg_manual_lift_control)
            btnNormalModePickingTimeSetting = findView(R.id.btn_picking_time_setting)
            layoutManualLiftSetting = findView(R.id.layout_manual_lift_setting)
            etRobotLengthNormalMode = findView(R.id.et_length_normal_mode)
            etRobotWidthNormalMode = findView(R.id.et_width_normal_mode)
            etLidarWidthNormalMode = findView(R.id.et_lidar_width_normal_mode)
            etRobotSizeFrontNormalMode = findView(R.id.et_front_normal_mode)
            etRobotSizeLeftNormalMode = findView(R.id.et_left_normal_mode)
            etRobotSizeBehindNormalMode = findView(R.id.et_behind_normal_mode)
            etRobotSizeRightNormalMode = findView(R.id.et_right_normal_mode)
            rgRotateNormalMode = findView(R.id.rg_rotate_control_normal_mode)
            rgStopNearbyNormalMode = findView(R.id.rg_stop_nearby_normal_mode)
            rgStartNormalTaskCountDown = findView(R.id.rg_start_normal_task_count_down)
            layoutStartNormalTaskCountDownTime =
                findView(R.id.layout_start_normal_task_count_down_time)
            isbStartNormalTaskCountDownTime =
                findView(R.id.isb_start_normal_task_count_down_time)
            if (!RobotInfo.isSpaceShip() || !RobotInfo.isLiftModelInstalled) {
                findView<View>(R.id.layout_manual_lift_control).visibility = View.GONE
            } else {
                findView<View>(R.id.layout_robot_size_normal_mode).visibility =
                    if (RobotInfo.isSupportNewFootprint) View.VISIBLE else View.GONE
                findView<View>(R.id.layout_robot_length_and_width_normal_mode).visibility =
                    if (RobotInfo.isSupportNewFootprint) View.GONE else View.VISIBLE
            }
            layoutManualLiftSetting.visibility =
                if (RobotInfo.isNormalModeWithManualLiftControl) View.VISIBLE else View.GONE
            if (!RobotInfo.modeNormalSetting.startTaskCountDownSwitch) {
                layoutStartNormalTaskCountDownTime.visibility = View.GONE
            }
            btnNormalModePickingTimeSetting.setBackgroundResource(if (RobotInfo.modeNormalSetting.waitingCountDownTimerOpen) R.drawable.bg_common_button_active else R.drawable.bg_common_button_inactive)
            btnNormalModePlayArrivedTipTimeSetting.setBackgroundResource(if (RobotInfo.modeNormalSetting.waitingCountDownTimerOpen) R.drawable.bg_common_button_active else R.drawable.bg_common_button_inactive)
            if (SpManager.getInstance().getInt(Constants.KEY_LANGUAGE_TYPE, Constants.DEFAULT_LANGUAGE_TYPE) != 1) {
                rgNormalModeFinishActionControl.orientation = RadioGroup.VERTICAL
            }

        }

        fun initListener() {
            findView<ImageButton>(R.id.ib_increase_normal_mode_speed)
                .setDebounceClickListener(this)
            findView<ImageButton>(R.id.ib_decrease_normal_mode_speed)
                .setDebounceClickListener(this)
            findView<Button>(R.id.btn_save_length_and_width_normal_mode)
                .setDebounceClickListener(this)
            findView<Button>(R.id.btn_save_lidar_width_normal_mode)
                .setDebounceClickListener(this)
            findView<ImageButton>(R.id.ib_decrease_start_normal_task_count_down_time)
                .setDebounceClickListener(this)
            findView<ImageButton>(R.id.ib_increase_start_normal_task_count_down_time)
                .setDebounceClickListener(this)
            findView<RadioButton>(R.id.rb_open_normal_mode_rotation_recovery)
                .setDebounceClickListener(this)
            findView<RadioButton>(R.id.rb_close_normal_mode_rotation_recovery)
                .setDebounceClickListener(this)
            findView<RadioButton>(R.id.rb_open_stop_nearby_normal_mode)
                .setDebounceClickListener(this)
            findView<RadioButton>(R.id.rb_close_stop_nearby_normal_mode)
                .setDebounceClickListener(this)
            findView<RadioButton>(R.id.rb_open_count_down_timer)
                .setDebounceClickListener(this)
            findView<RadioButton>(R.id.rb_close_count_down_timer)
                .setDebounceClickListener(this)
            findView<RadioButton>(R.id.rb_return_product_point)
                .setDebounceClickListener(this)
            findView<RadioButton>(R.id.rb_return_charge_point)
                .setDebounceClickListener(this)
            findView<RadioButton>(R.id.rb_stay).setDebounceClickListener(this)
            findView<RadioButton>(R.id.rb_open_manual_lift_control)
                .setDebounceClickListener(this)
            findView<RadioButton>(R.id.rb_close_manual_lift_control)
                .setDebounceClickListener(this)
            findView<RadioButton>(R.id.rb_open_start_normal_task_count_down)
                .setDebounceClickListener(this)
            findView<RadioButton>(R.id.rb_close_start_normal_task_count_down)
                .setDebounceClickListener(this)
            findView<Button>(R.id.btn_save_robot_size_normal_mode).setDebounceClickListener(this)
            btnNormalModePickingTimeSetting.setDebounceClickListener(this)
            btnNormalModePlayArrivedTipTimeSetting.setDebounceClickListener(this)
            isbAdjustNormalModeSpeed.onSeekChangeListener = this
            isbStartNormalTaskCountDownTime.onSeekChangeListener = this
        }

        fun initData() {
            RobotInfo.modeNormalSetting.let {
                isbAdjustNormalModeSpeed.setProgress(it.speed)
                isbStartNormalTaskCountDownTime.setProgress(it.startTaskCountDownTime.toFloat())
                rgNormalModeFinishActionControl.check(getRadioButtonIdByFinishAction(it.finishAction))
                rgNormalModeWaitingTimeControl.check(if (it.waitingCountDownTimerOpen) R.id.rb_open_count_down_timer else R.id.rb_close_count_down_timer)
                rgNormalModeManualLiftControl.check(if (it.manualLiftControlOpen) R.id.rb_open_manual_lift_control else R.id.rb_close_manual_lift_control)
                rgRotateNormalMode.check(if (it.rotate) R.id.rb_open_normal_mode_rotation_recovery else R.id.rb_close_normal_mode_rotation_recovery)
                rgStopNearbyNormalMode.check(if (it.stopNearBy) R.id.rb_open_stop_nearby_normal_mode else R.id.rb_close_stop_nearby_normal_mode)
                rgStartNormalTaskCountDown.check(if (it.startTaskCountDownSwitch) R.id.rb_open_start_normal_task_count_down else R.id.rb_close_start_normal_task_count_down)
                etRobotLengthNormalMode.setText(it.length.toString())
                etRobotWidthNormalMode.setText(it.width.toString())
                etLidarWidthNormalMode.setText(it.lidarWidth.toString())
                etRobotSizeFrontNormalMode.setText(it.robotSize[0].toString())
                etRobotSizeLeftNormalMode.setText(it.robotSize[1].toString())
                etRobotSizeBehindNormalMode.setText(it.robotSize[2].toString())
                etRobotSizeRightNormalMode.setText(it.robotSize[3].toString())
                updateWaitingTime(it.waitingTime)
                updatePlayArrivedTipTime(it.playArrivedTipTime)
            }
        }
        if (root.tag == null) {
            initView()
            initListener()
            root.tag = "initialized"
        }
        initData()
    }

    fun updateNormalModeConfig(update: ModeNormalSetting.() -> Unit) {
        RobotInfo.modeNormalSetting.apply {
            update()
            Timber.w("修改普通模式设置: $this")
            SpManager.getInstance().edit()
                .putString(Constants.KEY_NORMAL_MODE_CONFIG, gson.toJson(this)).apply()
        }
    }

    override fun onSeeking(seekParams: SeekParams?) {

    }

    override fun onStartTrackingTouch(seekBar: IndicatorSeekBar?) {

    }

    override fun onStopTrackingTouch(seekBar: IndicatorSeekBar) {
        when (seekBar.id) {
            R.id.isb_start_normal_task_count_down_time ->
                updateNormalModeConfig {
                    this.startTaskCountDownTime = ViewUtils.onIntValueChange(seekBar, false)
                }

            R.id.isb_adjust_normal_mode_speed ->
                updateNormalModeConfig {
                    this.speed = ViewUtils.onFloatValueChange(seekBar, false)
                }
        }
    }

    private fun getRadioButtonIdByFinishAction(finishAction: Int) =
        when (finishAction) {
            0 -> R.id.rb_return_product_point
            1 -> R.id.rb_return_charge_point
            else -> R.id.rb_stay
        }

    private fun updateWaitingTime(seconds: Int) {
        val millisSeconds = seconds * 1000
        val hour = TimeUnit.MILLISECONDS.toHours(millisSeconds.toLong()).toInt()
        val minute = TimeUnit.MILLISECONDS.toMinutes(millisSeconds.toLong()).toInt() % 60
        val second = TimeUnit.MILLISECONDS.toSeconds(millisSeconds.toLong()).toInt() % 60
        tvNormalModePickingHours.text = hour.toString()
        tvNormalModePickingMinutes.text = minute.toString()
        tvNormalModePickingSeconds.text = second.toString()
    }

    private fun updatePlayArrivedTipTime(seconds: Int) {
        val millisSeconds = seconds * 1000
        val hour = TimeUnit.MILLISECONDS.toHours(millisSeconds.toLong()).toInt()
        val minute = TimeUnit.MILLISECONDS.toMinutes(millisSeconds.toLong()).toInt() % 60
        val second = TimeUnit.MILLISECONDS.toSeconds(millisSeconds.toLong()).toInt() % 60
        tvNormalModePlayArrivedTipHours.text = hour.toString()
        tvNormalModePlayArrivedTipMinutes.text = minute.toString()
        tvNormalModePlayArrivedSeconds.text = second.toString()
    }


    override fun invoke(v: View) {
        when (v.id) {
            R.id.ib_increase_normal_mode_speed ->
                updateNormalModeConfig { this.speed = ViewUtils.onFloatValueChange(v, true) }

            R.id.ib_decrease_normal_mode_speed ->
                updateNormalModeConfig { this.speed = ViewUtils.onFloatValueChange(v, false) }

            R.id.btn_save_length_and_width_normal_mode -> {
                val length =
                    ViewUtils.getInputContentToFloat(context, etRobotLengthNormalMode, 0f, 1.5f)
                val width =
                    ViewUtils.getInputContentToFloat(context, etRobotWidthNormalMode, 0f, 1.5f)
                if (length == Float.MAX_VALUE || width == Float.MAX_VALUE) return
                updateNormalModeConfig {
                    this.length = length
                    this.width = width
                    ToastUtils.showShortToast(context.getString(R.string.text_save_success))
                }
            }

            R.id.btn_save_robot_size_normal_mode -> {
                val inputRobotSizeFront =
                    ViewUtils.getInputContentToFloat(context, etRobotSizeFrontNormalMode, 0F, 1.5F)
                val inputRobotSizeLeft =
                    ViewUtils.getInputContentToFloat(context, etRobotSizeLeftNormalMode, 0F, 1.5F)
                val inputRobotSizeBehind =
                    ViewUtils.getInputContentToFloat(context, etRobotSizeBehindNormalMode, 0F, 1.5F)
                val inputRobotSizeRight =
                    ViewUtils.getInputContentToFloat(context, etRobotSizeRightNormalMode, 0F, 1.5F)
                if (inputRobotSizeFront == Float.MAX_VALUE ||
                    inputRobotSizeLeft == Float.MAX_VALUE ||
                    inputRobotSizeBehind == Float.MAX_VALUE ||
                    inputRobotSizeRight == Float.MAX_VALUE
                ) return
                val floatArray = FloatArray(4)
                floatArray[0] = inputRobotSizeFront
                floatArray[1] = inputRobotSizeLeft
                floatArray[2] = inputRobotSizeBehind
                floatArray[3] = inputRobotSizeRight
                updateNormalModeConfig {
                    this.robotSize = floatArray
                    ToastUtils.showShortToast(context.getString(R.string.text_save_success))
                }
            }

            R.id.btn_save_lidar_width_normal_mode -> {
                val lidarWidth =
                    ViewUtils.getInputContentToFloat(context, etLidarWidthNormalMode, 0.05f, 1f)
                if (lidarWidth == Float.MAX_VALUE) return
                updateNormalModeConfig { this.lidarWidth = lidarWidth }
                ToastUtils.showShortToast(context.getString(R.string.text_save_success))
            }

            R.id.ib_increase_start_normal_task_count_down_time ->
                updateNormalModeConfig {
                    this.startTaskCountDownTime = ViewUtils.onIntValueChange(v, true)
                }

            R.id.ib_decrease_start_normal_task_count_down_time ->
                updateNormalModeConfig {
                    this.startTaskCountDownTime = ViewUtils.onIntValueChange(v, false)
                }

            R.id.rb_open_normal_mode_rotation_recovery ->
                if (!RobotInfo.modeNormalSetting.rotate) updateNormalModeConfig {
                    this.rotate = true
                }

            R.id.rb_close_normal_mode_rotation_recovery ->
                if (RobotInfo.modeNormalSetting.rotate) updateNormalModeConfig {
                    this.rotate = false
                }

            R.id.rb_open_stop_nearby_normal_mode ->
                if (!RobotInfo.modeNormalSetting.stopNearBy) updateNormalModeConfig {
                    this.stopNearBy = true
                }

            R.id.rb_close_stop_nearby_normal_mode ->
                if (RobotInfo.modeNormalSetting.stopNearBy) updateNormalModeConfig {
                    this.stopNearBy = false
                }

            R.id.rb_open_count_down_timer -> {
                if (RobotInfo.isNormalModeWithManualLiftControl) {
                    EasyDialog.getInstance(context).confirm(
                        context.getString(R.string.text_count_down_and_manual_lift_cannot_both_open_if_open_count_down)
                    ) { dialog, id ->
                        if (id == R.id.btn_confirm) {
                            updateNormalModeConfig {
                                this.waitingCountDownTimerOpen = true
                                this.manualLiftControlOpen = false
                                rgNormalModeManualLiftControl.check(R.id.rb_close_manual_lift_control)
                                btnNormalModePickingTimeSetting.setBackgroundResource(R.drawable.bg_common_button_active)
                                btnNormalModePlayArrivedTipTimeSetting.setBackgroundResource(R.drawable.bg_common_button_active)
                                layoutManualLiftSetting.visibility = View.GONE
                            }
                        } else {
                            rgNormalModeWaitingTimeControl.check(R.id.rb_close_count_down_timer)
                        }
                        dialog.dismiss()
                    }
                } else {
                    updateNormalModeConfig {
                        this.waitingCountDownTimerOpen = true
                        btnNormalModePickingTimeSetting.setBackgroundResource(R.drawable.bg_common_button_active)
                        btnNormalModePlayArrivedTipTimeSetting.setBackgroundResource(R.drawable.bg_common_button_active)
                    }
                }
            }

            R.id.rb_close_count_down_timer ->
                updateNormalModeConfig {
                    this.waitingCountDownTimerOpen = false
                    btnNormalModePickingTimeSetting.setBackgroundResource(R.drawable.bg_common_button_inactive)
                    btnNormalModePlayArrivedTipTimeSetting.setBackgroundResource(R.drawable.bg_common_button_inactive)
                }

            R.id.rb_return_product_point ->
                updateNormalModeConfig { this.finishAction = 0 }

            R.id.rb_return_charge_point ->
                updateNormalModeConfig { this.finishAction = 1 }

            R.id.rb_stay ->
                updateNormalModeConfig { this.finishAction = 2 }

            R.id.rb_open_manual_lift_control -> {
                if (RobotInfo.modeNormalSetting.waitingCountDownTimerOpen) {
                    EasyDialog.getInstance(context)
                        .confirm(context.getString(R.string.text_manual_lift_and_count_down_cannot_both_open_if_open_manual_lift)) { dialog, id ->
                            if (id == R.id.btn_confirm) {
                                updateNormalModeConfig {
                                    this.waitingCountDownTimerOpen = false
                                    this.manualLiftControlOpen = true
                                    rgNormalModeWaitingTimeControl.check(R.id.rb_close_count_down_timer)
                                    btnNormalModePickingTimeSetting.setBackgroundResource(R.drawable.bg_common_button_inactive)
                                    btnNormalModePlayArrivedTipTimeSetting.setBackgroundResource(R.drawable.bg_common_button_inactive)
                                    layoutManualLiftSetting.visibility = View.VISIBLE
                                }
                            } else {
                                rgNormalModeManualLiftControl.check(R.id.rb_close_manual_lift_control)
                            }
                            dialog.dismiss()
                        }
                } else {
                    updateNormalModeConfig {
                        this.manualLiftControlOpen = true
                        layoutManualLiftSetting.visibility = View.VISIBLE
                    }
                }
            }

            R.id.rb_close_manual_lift_control ->
                updateNormalModeConfig {
                    this.manualLiftControlOpen = false
                    layoutManualLiftSetting.visibility = View.GONE
                }

            R.id.rb_open_start_normal_task_count_down ->
                updateNormalModeConfig {
                    this.startTaskCountDownSwitch = true
                    layoutStartNormalTaskCountDownTime.visibility = View.VISIBLE
                }

            R.id.rb_close_start_normal_task_count_down ->
                updateNormalModeConfig {
                    this.startTaskCountDownSwitch = false
                    layoutStartNormalTaskCountDownTime.visibility = View.GONE
                }

            R.id.btn_play_arrived_tip_time_setting -> {
                if (!RobotInfo.modeNormalSetting.waitingCountDownTimerOpen) {
                    ToastUtils.showShortToast(context.getString(R.string.text_check_not_open_waiting_time_cannot_set_play_arrived_tip_time))
                    return
                }
                if (RobotInfo.modeNormalSetting.waitingTime <= 10) {
                    ToastUtils.showShortToast(context.getString(R.string.text_check_waiting_time_too_short_cannot_set_play_arrived_time))
                    return
                }
                TimePickerDialog(
                    context,
                    RobotInfo.modeNormalSetting.playArrivedTipTime,
                    0,
                    object : TimePickerDialog.OnTimePickerConfirmListener {
                        override fun onConfirm(dialog: Dialog, waitingTime: Int) {
                            if (waitingTime > RobotInfo.modeNormalSetting.waitingTime - 10) {
                                ToastUtils.showShortToast(context.getString(R.string.text_play_arrived_tip_time_must_smaller_than_waiting_time))
                                return
                            }
                            updateNormalModeConfig {
                                this.playArrivedTipTime = waitingTime
                            }
                            updatePlayArrivedTipTime(waitingTime)
                            dialog.dismiss()
                        }
                    }
                ).show()
            }

            R.id.btn_picking_time_setting -> {
                if (!RobotInfo.modeNormalSetting.waitingCountDownTimerOpen) {
                    ToastUtils.showShortToast(context.getString(R.string.text_check_not_open_waiting_time))
                    return
                }
                TimePickerDialog(context,
                    RobotInfo.modeNormalSetting.waitingTime,
                    object : TimePickerDialog.OnTimePickerConfirmListener {
                        override fun onConfirm(dialog: Dialog, waitingTime: Int) {
                            updateNormalModeConfig {
                                this.waitingTime = waitingTime
                                if (waitingTime <= 10) {
                                    this.playArrivedTipTime = 0
                                } else if (waitingTime < this.playArrivedTipTime + 10) {
                                    this.playArrivedTipTime = waitingTime - 10
                                }
                                updatePlayArrivedTipTime(this.playArrivedTipTime)
                                updateWaitingTime(waitingTime)
                            }
                            dialog.dismiss()
                        }
                    }).show()
            }
        }
    }

}