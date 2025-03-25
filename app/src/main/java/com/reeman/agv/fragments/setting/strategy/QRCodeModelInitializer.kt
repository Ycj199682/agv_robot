package com.reeman.agv.fragments.setting.strategy

import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import com.google.gson.Gson
import com.reeman.agv.R
import com.reeman.agv.calling.CallingInfo
import com.reeman.agv.utils.DebounceClickListener
import com.reeman.agv.utils.ToastUtils
import com.reeman.agv.utils.ViewUtils
import com.reeman.agv.widgets.ExpandableLayout
import com.reeman.commons.constants.Constants
import com.reeman.commons.event.AGVTagPoseEvent
import com.reeman.commons.settings.ModeQRCodeSetting
import com.reeman.commons.state.RobotInfo
import com.reeman.commons.utils.SpManager
import com.warkiz.widget.IndicatorSeekBar
import com.warkiz.widget.OnSeekChangeListener
import com.warkiz.widget.SeekParams
import kotlinx.coroutines.Job
import timber.log.Timber

class QRCodeModelInitializer : LayoutInitializer, OnSeekChangeListener,
    DebounceClickListener,
        (View) -> Unit {

    private lateinit var context: Context
    private val gson = Gson()

    private lateinit var isbAdjustQRCodeModeSpeed: IndicatorSeekBar
    private lateinit var rgQRCodeModeLiftControl: RadioGroup
    private lateinit var etRobotLength: EditText
    private lateinit var etRobotWidth: EditText
    private lateinit var etRobotSizeFrontQRCodeMode: EditText
    private lateinit var etRobotSizeLeftQRCodeMode: EditText
    private lateinit var etRobotSizeBehindQRCodeMode: EditText
    private lateinit var etRobotSizeRightQRCodeMode: EditText
    private lateinit var etOrientationAndDistanceCalibration: EditText
    private lateinit var etLidarWidth: EditText
    private lateinit var rgDirection: RadioGroup
    private lateinit var rgRotate: RadioGroup
    private lateinit var rgStopNearby: RadioGroup
    private lateinit var rgCallingBind: RadioGroup
    private lateinit var rgQRCodeTaskFinishAction: RadioGroup
    private lateinit var layoutStartQRCodeTaskCountDownTime: LinearLayout
    private lateinit var rgStartQRCodeTaskCountDown: RadioGroup
    private lateinit var isbStartQRCodeTaskCountDownTime: IndicatorSeekBar
    private lateinit var tvQRCodeData: TextView


    
    

    override fun initLayout(context: Context, root: ExpandableLayout) {
        this.context = context

        fun <T : View> findView(id: Int): T = root.findViewById(id)

        fun initView() {
            isbAdjustQRCodeModeSpeed = findView(R.id.isb_adjust_qrcode_mode_speed)
            etRobotLength = findView(R.id.et_length)
            etRobotWidth = findView(R.id.et_width)
            etRobotSizeFrontQRCodeMode = findView(R.id.et_front_qrcode_mode)
            etRobotSizeLeftQRCodeMode = findView(R.id.et_left_qrcode_mode)
            etRobotSizeBehindQRCodeMode = findView(R.id.et_behind_qrcode_mode)
            etRobotSizeRightQRCodeMode = findView(R.id.et_right_qrcode_mode)
            etOrientationAndDistanceCalibration = findView(R.id.et_orientation_and_distance_calibration)
            etLidarWidth = findView(R.id.et_lidar_width)
            rgDirection = findView(R.id.rg_direction)
            rgRotate = findView(R.id.rg_rotate_control)
            rgStopNearby = findView(R.id.rg_stop_nearby)
            rgCallingBind = findView(R.id.rg_calling_bind)
            rgQRCodeModeLiftControl = findView(R.id.rg_qrcode_lift_control)
            rgStartQRCodeTaskCountDown = findView(R.id.rg_start_qrcode_task_count_down)
            layoutStartQRCodeTaskCountDownTime = findView(R.id.layout_start_qrcode_task_count_down_time)
            isbStartQRCodeTaskCountDownTime = findView(R.id.isb_start_qrcode_task_count_down_time)
            tvQRCodeData = findView(R.id.tv_qrcode_data)
            rgQRCodeTaskFinishAction = findView(R.id.rg_task_finish_action)
            if (!RobotInfo.isLiftModelInstalled) {
                findView<LinearLayout>(R.id.ll_qrcode_lift_control).visibility = View.GONE
            } else {
                findView<LinearLayout>(R.id.layout_robot_size_qrcode_mode).visibility =
                    if (RobotInfo.isSupportNewFootprint) View.VISIBLE else View.GONE
                findView<LinearLayout>(R.id.layout_robot_length_and_width_qrcode_mode).visibility =
                    if (RobotInfo.isSupportNewFootprint) View.GONE else View.VISIBLE
            }
            if (!RobotInfo.modeQRCodeSetting.startTaskCountDownSwitch) {
                layoutStartQRCodeTaskCountDownTime.visibility = View.GONE
            }
            if (SpManager.getInstance().getInt(Constants.KEY_LANGUAGE_TYPE, Constants.DEFAULT_LANGUAGE_TYPE)!= 1 ){
                rgQRCodeTaskFinishAction.orientation = RadioGroup.VERTICAL
            }
        }

        fun initListener() {
            findView<ImageButton>(R.id.ib_increase_qrcode_mode_speed).setDebounceClickListener(this)
            findView<ImageButton>(R.id.ib_decrease_qrcode_mode_speed).setDebounceClickListener(this)
            findView<Button>(R.id.btn_save_length_and_width).setDebounceClickListener(this)
            findView<Button>(R.id.btn_save_orientation_and_distance_calibration).setDebounceClickListener(this)
            findView<Button>(R.id.btn_save_lidar_width).setDebounceClickListener(this)
            findView<Button>(R.id.btn_save_robot_size_qrcode_mode).setDebounceClickListener(this)
            findView<ImageView>(R.id.ib_decrease_start_qrcode_task_count_down_time).setDebounceClickListener(this)
            findView<ImageView>(R.id.ib_increase_start_qrcode_task_count_down_time).setDebounceClickListener(this)
            findView<RadioButton>(R.id.rb_forward).setDebounceClickListener(this)
            findView<RadioButton>(R.id.rb_behind).setDebounceClickListener(this)
            findView<RadioButton>(R.id.rb_open_rotation_recovery).setDebounceClickListener(this)
            findView<RadioButton>(R.id.rb_close_rotation_recovery).setDebounceClickListener(this)
            findView<RadioButton>(R.id.rb_open_stop_nearby).setDebounceClickListener(this)
            findView<RadioButton>(R.id.rb_close_stop_nearby).setDebounceClickListener(this)
            findView<RadioButton>(R.id.rb_open_calling_bind).setDebounceClickListener(this)
            findView<RadioButton>(R.id.rb_close_calling_bind).setDebounceClickListener(this)
            findView<RadioButton>(R.id.rb_open_lift_control).setDebounceClickListener(this)
            findView<RadioButton>(R.id.rb_close_lift_control).setDebounceClickListener(this)
            findView<RadioButton>(R.id.rb_open_start_qrcode_task_count_down).setDebounceClickListener(this)
            findView<RadioButton>(R.id.rb_close_start_qrcode_task_count_down).setDebounceClickListener(this)
            findView<RadioButton>(R.id.rb_return_product_point).setDebounceClickListener(this)
            findView<RadioButton>(R.id.rb_return_charge_point).setDebounceClickListener(this)
            findView<RadioButton>(R.id.rb_stay).setDebounceClickListener(this)
            isbAdjustQRCodeModeSpeed.onSeekChangeListener = this
            isbStartQRCodeTaskCountDownTime.onSeekChangeListener = this
        }

        fun initData() {
            RobotInfo.modeQRCodeSetting.let {
                isbAdjustQRCodeModeSpeed.setProgress(it.speed)
                etRobotLength.setText(String.format("%s", it.length))
                etRobotWidth.setText(String.format("%s", it.width))
                etRobotSizeFrontQRCodeMode.setText(String.format("%s", it.robotSize[0]))
                etRobotSizeLeftQRCodeMode.setText(String.format("%s", it.robotSize[1]))
                etRobotSizeBehindQRCodeMode.setText(String.format("%s", it.robotSize[2]))
                etRobotSizeRightQRCodeMode.setText(String.format("%s", it.robotSize[3]))
                etOrientationAndDistanceCalibration.setText(String.format("%s", it.orientationAndDistanceCalibration))
                etLidarWidth.setText(String.format("%s", it.lidarWidth))
                rgDirection.check(if (it.direction) R.id.rb_forward else R.id.rb_behind)
                rgRotate.check(if (it.rotate) R.id.rb_open_rotation_recovery else R.id.rb_close_rotation_recovery)
                rgStopNearby.check(if (it.stopNearby) R.id.rb_open_stop_nearby else R.id.rb_close_stop_nearby)
                rgCallingBind.check(if (it.callingBind) R.id.rb_open_calling_bind else R.id.rb_close_calling_bind)
                rgQRCodeModeLiftControl.check(if (it.lift) R.id.rb_open_lift_control else R.id.rb_close_lift_control)
                rgStartQRCodeTaskCountDown.check(if (it.startTaskCountDownSwitch) R.id.rb_open_start_qrcode_task_count_down else R.id.rb_close_start_qrcode_task_count_down)
                isbStartQRCodeTaskCountDownTime.setProgress(it.startTaskCountDownTime.toFloat())
                rgQRCodeTaskFinishAction.check(
                    when(it.finishAction){
                        0->R.id.rb_return_product_point
                        1->R.id.rb_return_charge_point
                        else->R.id.rb_stay
                    }
                )
            }

        }

        if (root.tag == null) {
            initView()
            initListener()
            root.tag = "initialized"
        }
        initData()
    }

    private fun updateQRCodeModeConfig(update: ModeQRCodeSetting.() -> Unit) {
        RobotInfo.modeQRCodeSetting.apply {
            update()
            Timber.w("修改二维码模式模式设置: $this")
            SpManager.getInstance().edit()
                .putString(Constants.KEY_QRCODE_MODE_CONFIG, gson.toJson(this)).apply()
        }
    }

    override fun onSeeking(seekParams: SeekParams?) {

    }

    override fun onStartTrackingTouch(seekBar: IndicatorSeekBar?) {
    }

    override fun onStopTrackingTouch(seekBar: IndicatorSeekBar) {
        when (seekBar.id) {
            R.id.isb_adjust_qrcode_mode_speed ->
                updateQRCodeModeConfig {
                    this.speed = ViewUtils.onFloatValueChange(seekBar, false)
                }

            R.id.isb_start_qrcode_task_count_down_time ->
                updateQRCodeModeConfig {
                    this.startTaskCountDownTime = ViewUtils.onIntValueChange(seekBar, false)
                }
        }
    }


    override fun invoke(v: View) {
        when (v.id) {
            R.id.ib_increase_qrcode_mode_speed ->
                updateQRCodeModeConfig {
                    this.speed = ViewUtils.onFloatValueChange(v, true)
                }

            R.id.ib_decrease_qrcode_mode_speed ->
                updateQRCodeModeConfig {
                    this.speed = ViewUtils.onFloatValueChange(v, false)
                }

            R.id.btn_save_length_and_width -> {
                val inputLength = ViewUtils.getInputContentToFloat(context, etRobotLength, 0F, 1.5F)
                val inputWidth = ViewUtils.getInputContentToFloat(context, etRobotWidth, 0F, 1.5F)
                if (inputWidth == Float.MAX_VALUE || inputLength == Float.MAX_VALUE) return
                updateQRCodeModeConfig {
                    this.length = inputLength
                    this.width = inputWidth
                    ToastUtils.showShortToast(context.getString(R.string.text_save_success))
                }
            }

            R.id.btn_save_orientation_and_distance_calibration -> {
                val inputOrientationAndDistanceCalibration = ViewUtils.getInputContentToFloat(
                    context,
                    etOrientationAndDistanceCalibration,
                    -2F,
                    2F
                )
                if (inputOrientationAndDistanceCalibration == Float.MAX_VALUE) return
                updateQRCodeModeConfig {
                    this.orientationAndDistanceCalibration = inputOrientationAndDistanceCalibration
                    ToastUtils.showShortToast(context.getString(R.string.text_save_success))
                }
            }

            R.id.btn_save_lidar_width -> {
                val inputLidarWidth =
                    ViewUtils.getInputContentToFloat(context, etLidarWidth, 0.05F, 1F)
                if (inputLidarWidth == Float.MAX_VALUE) return
                updateQRCodeModeConfig {
                    this.lidarWidth = inputLidarWidth
                    ToastUtils.showShortToast(context.getString(R.string.text_save_success))
                }
            }

            R.id.btn_save_robot_size_qrcode_mode -> {
                val inputRobotSizeFront =
                    ViewUtils.getInputContentToFloat(context, etRobotSizeFrontQRCodeMode, 0F, 1.5F)
                val inputRobotSizeLeft =
                    ViewUtils.getInputContentToFloat(context, etRobotSizeLeftQRCodeMode, 0F, 1.5F)
                val inputRobotSizeBehind =
                    ViewUtils.getInputContentToFloat(context, etRobotSizeBehindQRCodeMode, 0F, 1.5F)
                val inputRobotSizeRight =
                    ViewUtils.getInputContentToFloat(context, etRobotSizeRightQRCodeMode, 0F, 1.5F)
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
                updateQRCodeModeConfig {
                    this.robotSize = floatArray
                    ToastUtils.showShortToast(context.getString(R.string.text_save_success))
                }
            }

            R.id.ib_decrease_start_qrcode_task_count_down_time ->
                updateQRCodeModeConfig {
                    this.startTaskCountDownTime = ViewUtils.onIntValueChange(v, false)
                }

            R.id.ib_increase_start_qrcode_task_count_down_time ->
                updateQRCodeModeConfig {
                    this.startTaskCountDownTime = ViewUtils.onIntValueChange(v, true)
                }

            R.id.rb_forward ->
                updateQRCodeModeConfig {
                    this.direction = true
                }

            R.id.rb_behind ->
                updateQRCodeModeConfig {
                    this.direction = false
                }

            R.id.rb_open_rotation_recovery ->
                updateQRCodeModeConfig {
                    this.rotate = true
                }

            R.id.rb_close_rotation_recovery ->
                updateQRCodeModeConfig {
                    this.rotate = false
                }

            R.id.rb_open_stop_nearby ->
                updateQRCodeModeConfig {
                    this.stopNearby = true
                }

            R.id.rb_close_stop_nearby ->
                updateQRCodeModeConfig {
                    this.stopNearby = false
                }

            R.id.rb_open_calling_bind ->
                updateQRCodeModeConfig {
                    this.callingBind = true
                    CallingInfo.isQRCodeTaskUseCallingButton = true
                }

            R.id.rb_close_calling_bind ->
                updateQRCodeModeConfig {
                    this.callingBind = false
                    CallingInfo.isQRCodeTaskUseCallingButton = false
                }

            R.id.rb_open_lift_control ->
                updateQRCodeModeConfig {
                    this.lift = true
                }

            R.id.rb_close_lift_control ->
                updateQRCodeModeConfig {
                    this.lift = false
                }

            R.id.rb_open_start_qrcode_task_count_down ->
                updateQRCodeModeConfig {
                    this.startTaskCountDownSwitch = true
                    layoutStartQRCodeTaskCountDownTime.visibility = View.VISIBLE
                }

            R.id.rb_close_start_qrcode_task_count_down ->
                updateQRCodeModeConfig {
                    this.startTaskCountDownSwitch = false
                    layoutStartQRCodeTaskCountDownTime.visibility = View.GONE
                }

            R.id.rb_return_product_point -> updateQRCodeModeConfig { this.finishAction = 0 }
            R.id.rb_return_charge_point -> updateQRCodeModeConfig { this.finishAction = 1 }
            R.id.rb_stay -> updateQRCodeModeConfig { this.finishAction = 2 }
        }
    }

    override fun onAGVTagPoseEvent(event: AGVTagPoseEvent) {
        if (::tvQRCodeData.isInitialized)
            tvQRCodeData.text = event.data
    }
}