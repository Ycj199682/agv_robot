package com.reeman.agv.fragments.setting.strategy

import android.content.Context
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import com.google.gson.Gson
import com.reeman.agv.R
import com.reeman.agv.utils.DebounceClickListener
import com.reeman.agv.utils.ViewUtils
import com.reeman.agv.widgets.ExpandableLayout
import com.reeman.commons.constants.Constants
import com.reeman.commons.settings.ModeRouteSetting
import com.reeman.commons.state.RobotInfo
import com.reeman.commons.utils.SpManager
import com.warkiz.widget.IndicatorSeekBar
import com.warkiz.widget.OnSeekChangeListener
import com.warkiz.widget.SeekParams
import kotlinx.coroutines.Job
import timber.log.Timber

class RouteModelInitializer : LayoutInitializer, OnSeekChangeListener,
    DebounceClickListener,
        (View) -> Unit {
    private lateinit var context: Context
    private val gson = Gson()

    private lateinit var isbAdjustRouteModeSpeed: IndicatorSeekBar
    private lateinit var layoutStartRouteTaskCountDownTime: LinearLayout
    private lateinit var rgStartRouteTaskCountDown: RadioGroup
    private lateinit var isbStartRouteTaskCountDownTime: IndicatorSeekBar


    
    
    override fun initLayout(context: Context, root: ExpandableLayout) {
        this.context = context
        fun <T : View> findView(id: Int): T = root.findViewById(id)


        fun initView() {
            isbAdjustRouteModeSpeed = findView(R.id.isb_adjust_route_mode_speed)
            layoutStartRouteTaskCountDownTime = findView(R.id.layout_start_route_task_count_down_time)
            rgStartRouteTaskCountDown = findView(R.id.rg_start_route_task_count_down)
            isbStartRouteTaskCountDownTime = findView(R.id.isb_start_route_task_count_down_time)
            if (RobotInfo.modeRouteSetting.startTaskCountDownSwitch) {
                layoutStartRouteTaskCountDownTime.visibility = View.GONE
            }
        }

        fun initListener() {
            findView<ImageButton>(R.id.ib_increase_route_mode_speed).setDebounceClickListener(this)
            findView<ImageButton>(R.id.ib_decrease_route_mode_speed).setDebounceClickListener(this)
            findView<RadioButton>(R.id.rb_open_start_route_task_count_down).setDebounceClickListener(this)
            findView<RadioButton>(R.id.rb_close_start_route_task_count_down).setDebounceClickListener(this)
            isbAdjustRouteModeSpeed.onSeekChangeListener = this
            isbStartRouteTaskCountDownTime.onSeekChangeListener = this
        }

        fun initData() {
            RobotInfo.modeRouteSetting.let {
                isbAdjustRouteModeSpeed.setProgress(it.speed)
                rgStartRouteTaskCountDown.check(if (it.startTaskCountDownSwitch) R.id.rb_open_start_route_task_count_down else R.id.rb_close_start_route_task_count_down)
                isbStartRouteTaskCountDownTime.setProgress(it.startTaskCountDownTime.toFloat())
            }
        }

        if (root.tag == null) {
            initView()
            initListener()
            root.tag = "initialized"
        }
        initData()
    }

    private fun updateRouteModeConfig(update: ModeRouteSetting.() -> Unit) {
        RobotInfo.modeRouteSetting.apply {
            update()
            Timber.w("修改路线模式设置: $this")
            SpManager.getInstance().edit()
                .putString(Constants.KEY_ROUTE_MODE_CONFIG, gson.toJson(this)).apply()
        }
    }

    override fun onSeeking(seekParams: SeekParams?) {

    }

    override fun onStartTrackingTouch(seekBar: IndicatorSeekBar?) {

    }

    override fun onStopTrackingTouch(seekBar: IndicatorSeekBar) {
        when (seekBar.id) {
            R.id.isb_adjust_route_mode_speed -> updateRouteModeConfig {
                this.speed = ViewUtils.onFloatValueChange(seekBar, false)
            }

            R.id.isb_start_route_task_count_down_time -> updateRouteModeConfig {
                this.startTaskCountDownTime = ViewUtils.onIntValueChange(seekBar, false)
            }
        }

    }


    override fun invoke(v: View) {
        when (v.id) {
            R.id.ib_increase_route_mode_speed -> updateRouteModeConfig {
                this.speed = ViewUtils.onFloatValueChange(v, true)
            }

            R.id.ib_decrease_route_mode_speed -> updateRouteModeConfig {
                this.speed = ViewUtils.onFloatValueChange(v, false)
            }

            R.id.rb_open_start_route_task_count_down ->
                updateRouteModeConfig {
                    this.startTaskCountDownSwitch = true
                    layoutStartRouteTaskCountDownTime.visibility = View.VISIBLE
                }
            R.id.rb_close_start_route_task_count_down ->
                updateRouteModeConfig {
                    this.startTaskCountDownSwitch = false
                    layoutStartRouteTaskCountDownTime.visibility = View.GONE
                }

        }
    }
}