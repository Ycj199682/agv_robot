package com.reeman.agv.fragments.setting

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import com.google.gson.Gson
import com.reeman.agv.R
import com.reeman.agv.base.BaseFragment
import com.reeman.agv.constants.Errors
import com.reeman.agv.utils.DebounceClickListener
import com.reeman.agv.utils.ViewUtils
import com.reeman.agv.widgets.CallingTaskChoosePointDialog
import com.reeman.agv.widgets.EasyDialog
import com.reeman.commons.constants.Constants
import com.reeman.commons.settings.ReturningSetting
import com.reeman.commons.state.NavigationMode
import com.reeman.commons.state.RobotInfo
import com.reeman.commons.utils.SpManager
import com.reeman.points.model.custom.GenericPoint
import com.reeman.points.model.custom.GenericPointsWithMap
import com.reeman.points.process.PointRefreshProcessor
import com.reeman.points.process.callback.RefreshPointDataCallback
import com.reeman.points.process.impl.DeliveryPointsRefreshProcessingStrategy
import com.reeman.points.process.impl.DeliveryPointsWithMapsRefreshProcessingStrategy
import com.reeman.points.process.impl.FixedDeliveryPointsRefreshProcessingStrategy
import com.reeman.points.process.impl.FixedDeliveryPointsWithMapsRefreshProcessingStrategy
import com.reeman.points.utils.PointCacheInfo
import com.warkiz.widget.IndicatorSeekBar
import com.warkiz.widget.OnSeekChangeListener
import com.warkiz.widget.SeekParams
import kotlinx.coroutines.Job
import timber.log.Timber
import java.lang.Error

class ReturningSettingFragment : BaseFragment(), OnSeekChangeListener, DebounceClickListener,
        (View) -> Unit {
    private val gson = Gson()

    private lateinit var isbAdjustGotoProductionPointSpeed: IndicatorSeekBar
    private lateinit var isbAdjustGotoChargingPileSpeed: IndicatorSeekBar
    private lateinit var rgReturningCountDown: RadioGroup
    private lateinit var layoutReturningCountDownTime: LinearLayout
    private lateinit var isbReturningCountDownTime: IndicatorSeekBar
    private lateinit var rgStopNearby: RadioGroup
    private lateinit var rgProductionPointSetting: RadioGroup
    private lateinit var layoutDefaultProductionPointSetting: LinearLayout
    private lateinit var tvDefaultProductionPoint: TextView

    
    
    override fun getLayoutRes() = R.layout.fragment_returning_setting
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fun initView() {
            isbAdjustGotoProductionPointSpeed =
                findView(R.id.isb_adjust_goto_production_point_speed)
            isbAdjustGotoChargingPileSpeed = findView(R.id.isb_adjust_goto_charging_pile_speed)
            rgReturningCountDown = findView(R.id.rg_returning_count_down)
            layoutReturningCountDownTime = findView(R.id.layout_returning_count_down_time)
            isbReturningCountDownTime = findView(R.id.isb_returning_count_down_time)
            rgStopNearby = findView(R.id.rg_setting_stop_nearby)
            rgProductionPointSetting = findView(R.id.rg_production_point_setting)
            layoutDefaultProductionPointSetting =
                findView(R.id.layout_default_production_point_setting)
            tvDefaultProductionPoint = findView(R.id.tv_default_production_point)
        }

        fun initData() {
            RobotInfo.returningSetting.let {
                isbAdjustGotoProductionPointSpeed.setProgress(it.gotoProductionPointSpeed)
                isbAdjustGotoChargingPileSpeed.setProgress(it.gotoChargingPileSpeed)
                rgStopNearby.check(if (it.stopNearBy) R.id.rb_open_stop_nearby else R.id.rb_close_stop_nearby)
                rgReturningCountDown.check(if (it.startTaskCountDownSwitch) R.id.rb_open_returning_count_down else R.id.rb_close_returning_count_down)
                layoutReturningCountDownTime.visibility =
                    if (it.startTaskCountDownSwitch) View.VISIBLE else View.GONE
                isbReturningCountDownTime.setProgress(it.startTaskCountDownTime.toFloat())
                rgProductionPointSetting.check(if (it.productionPointSetting == 0) R.id.rb_only_one_production_point else R.id.rb_many_production_point)
                layoutDefaultProductionPointSetting.visibility =
                    if (it.productionPointSetting == 0) View.GONE else View.VISIBLE
                tvDefaultProductionPoint.text =
                    if (it.defaultProductionPoint.isNullOrBlank()) getString(R.string.text_not_select_point) else it.defaultProductionPoint
            }
        }

        fun initListener() {
            findView<ImageButton>(R.id.ib_decrease_goto_production_point_speed).setDebounceClickListener(
                this
            )
            findView<ImageButton>(R.id.ib_increase_goto_production_point_speed).setDebounceClickListener(
                this
            )
            findView<ImageButton>(R.id.ib_decrease_goto_charging_pile_speed).setDebounceClickListener(
                this
            )
            findView<ImageButton>(R.id.ib_increase_goto_charging_pile_speed).setDebounceClickListener(
                this
            )
            findView<ImageButton>(R.id.ib_decrease_returning_count_down_time).setDebounceClickListener(
                this
            )
            findView<ImageButton>(R.id.ib_increase_returning_count_down_time).setDebounceClickListener(
                this
            )
            findView<RadioButton>(R.id.rb_open_stop_nearby).setDebounceClickListener(this)
            findView<RadioButton>(R.id.rb_close_stop_nearby).setDebounceClickListener(this)
            findView<RadioButton>(R.id.rb_open_returning_count_down).setDebounceClickListener(this)
            findView<RadioButton>(R.id.rb_close_returning_count_down).setDebounceClickListener(this)
            findView<RadioButton>(R.id.rb_only_one_production_point).setDebounceClickListener(this)
            findView<RadioButton>(R.id.rb_many_production_point).setDebounceClickListener(this)
            findView<AppCompatButton>(R.id.btn_choose).setDebounceClickListener(this)
            isbAdjustGotoProductionPointSpeed.onSeekChangeListener = this
            isbAdjustGotoChargingPileSpeed.onSeekChangeListener = this
        }
        initView()
        initListener()
        initData()
    }

    private fun updateReturningSetting(update: ReturningSetting.() -> Unit) {
        RobotInfo.returningSetting.apply {
            update()
            Timber.w("修改返航设置: $this")
            SpManager.getInstance().edit()
                .putString(Constants.KEY_RETURNING_CONFIG, gson.toJson(this)).apply()
        }
    }

    override fun onSeeking(seekParams: SeekParams) {}
    override fun onStartTrackingTouch(seekBar: IndicatorSeekBar) {}
    override fun onStopTrackingTouch(seekBar: IndicatorSeekBar) {
        when (seekBar.id) {
            R.id.isb_returning_count_down_time -> updateReturningSetting {
                this.startTaskCountDownTime = ViewUtils.onIntValueChange(seekBar, false)
            }

            R.id.isb_adjust_goto_production_point_speed -> updateReturningSetting {
                this.gotoProductionPointSpeed = ViewUtils.onFloatValueChange(seekBar, false)
            }

            R.id.isb_adjust_goto_charging_pile_speed -> updateReturningSetting {
                this.gotoChargingPileSpeed = ViewUtils.onFloatValueChange(seekBar, false)
            }
        }
    }

    override fun invoke(v: View) {
        when (v.id) {
            R.id.btn_choose -> loadPoints()
            R.id.rb_open_stop_nearby -> updateReturningSetting { this.stopNearBy = true }
            R.id.rb_close_stop_nearby -> updateReturningSetting { this.stopNearBy = false }
            R.id.rb_open_returning_count_down ->
                updateReturningSetting {
                    this.startTaskCountDownSwitch = true
                    layoutReturningCountDownTime.visibility = View.VISIBLE

                }

            R.id.rb_close_returning_count_down ->
                updateReturningSetting {
                    this.startTaskCountDownSwitch = false
                    layoutReturningCountDownTime.visibility = View.GONE
                }

            R.id.rb_only_one_production_point ->
                updateReturningSetting {
                    this.productionPointSetting = 0
                    layoutDefaultProductionPointSetting.visibility = View.GONE
                }

            R.id.rb_many_production_point -> updateReturningSetting {
                this.productionPointSetting = 1
                layoutDefaultProductionPointSetting.visibility = View.VISIBLE
            }

            R.id.ib_decrease_goto_production_point_speed -> updateReturningSetting {
                this.gotoProductionPointSpeed = ViewUtils.onFloatValueChange(v, false)
            }

            R.id.ib_increase_goto_production_point_speed -> updateReturningSetting {
                this.gotoProductionPointSpeed = ViewUtils.onFloatValueChange(v, true)
            }

            R.id.ib_decrease_goto_charging_pile_speed -> updateReturningSetting {
                this.gotoChargingPileSpeed = ViewUtils.onFloatValueChange(v, false)
            }

            R.id.ib_increase_goto_charging_pile_speed -> updateReturningSetting {
                this.gotoChargingPileSpeed = ViewUtils.onFloatValueChange(v, true)
            }

            R.id.ib_decrease_returning_count_down_time -> updateReturningSetting {
                this.startTaskCountDownTime = ViewUtils.onIntValueChange(v, false)
            }

            R.id.ib_increase_returning_count_down_time -> updateReturningSetting {
                this.startTaskCountDownTime = ViewUtils.onIntValueChange(v, true)
            }
        }
    }

    private fun loadPoints() {
        EasyDialog.getLoadingInstance(context)
            .loading(getString(R.string.text_init_product_point))
        PointRefreshProcessor(if (RobotInfo.isElevatorMode) {
            if (RobotInfo.navigationMode == NavigationMode.autoPathMode) {
                DeliveryPointsWithMapsRefreshProcessingStrategy(false)
            } else {
                FixedDeliveryPointsWithMapsRefreshProcessingStrategy(false)
            }
        } else {
            if (RobotInfo.navigationMode == NavigationMode.autoPathMode) {
                DeliveryPointsRefreshProcessingStrategy()
            } else {
                FixedDeliveryPointsRefreshProcessingStrategy()
            }
        },
            object : RefreshPointDataCallback {
                override fun onPointsLoadSuccess(pointList: List<GenericPoint>) {
                    showProductionPointChooseDialog()
                }

                override fun onPointsWithMapsLoadSuccess(pointsWithMapList: List<GenericPointsWithMap>) {
                    showProductionPointChooseDialog()
                }

                override fun onThrowable(throwable: Throwable) {
                    if (EasyDialog.isShow()) EasyDialog.getInstance().dismiss()
                    EasyDialog.getInstance(requireActivity())
                        .warnError(Errors.getDataLoadFailedTip(requireActivity(), throwable))
                }
            }).process(
            ip = RobotInfo.ROSIPAddress,
            useLocalData = false,
            checkEnterElevatorPoint = RobotInfo.supportEnterElevatorPoint(),
            pointTypes = arrayListOf(GenericPoint.PRODUCT)
        )
    }

    private fun showProductionPointChooseDialog() {
        if (EasyDialog.isShow()) EasyDialog.getInstance().dismiss()
        val points = PointCacheInfo.productionPoints.map { it.second.name }
        CallingTaskChoosePointDialog(
            requireActivity(),
            points,
            Pair("", RobotInfo.returningSetting.defaultProductionPoint),
            object : CallingTaskChoosePointDialog.OnPointChooseResultListener {
                override fun onPointChooseResult(points: Pair<String?, String?>?) {
                    points?.let {
                        updateReturningSetting { this.defaultProductionPoint = it.second }
                        tvDefaultProductionPoint.text = if (it.second.isNullOrBlank()) {
                            getString(R.string.text_not_choose_point)
                        } else {
                            it.second
                        }
                        return
                    }
                    updateReturningSetting { this.defaultProductionPoint = "" }
                    tvDefaultProductionPoint.text = getString(R.string.text_not_choose_point)
                }
            }
        ).show()
    }


}