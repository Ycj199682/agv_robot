package com.reeman.agv.fragments.main

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bigkoo.pickerview.builder.TimePickerBuilder
import com.reeman.agv.R
import com.reeman.agv.adapter.RoutePointsAdapter
import com.reeman.agv.base.BaseFragment
import com.reeman.agv.utils.DebounceClickListener
import com.reeman.agv.utils.PointContentUtils
import com.reeman.agv.utils.ToastUtils
import com.reeman.agv.widgets.EasyDialog
import com.reeman.agv.widgets.PointChooseDialog
import com.reeman.commons.state.RobotInfo
import com.reeman.commons.utils.TimeUtil
import com.reeman.dao.repository.entities.PointsVO
import com.reeman.dao.repository.entities.RouteWithPoints
import com.reeman.points.model.custom.GenericPoint
import com.reeman.points.utils.PointCacheInfo
import kotlinx.coroutines.Job
import java.util.Calendar
import java.util.Date

class ModeRoutePointEditFragment(
    val routeWithPoints: RouteWithPoints,
    private val modeRoutePointEditListener: OnModeRoutePointEditListener
) : BaseFragment(), RoutePointsAdapter.OnRoutePointsAdapterClickListener,DebounceClickListener,
        (View) -> Unit {

    private lateinit var btnAddPoint: AppCompatButton
    private lateinit var tvPointName: TextView
    private lateinit var rgWaitingTimeControlSwitch: RadioGroup
    private lateinit var tvWaitingTimeSetting: TextView
    private lateinit var tvWaitingTime: TextView
    private lateinit var ivEditWaitingTime: ImageView
    private lateinit var layoutEditPoint: ConstraintLayout
    private lateinit var tvNotChoosePointShow: TextView
    private lateinit var routePointsAdapter: RoutePointsAdapter
    private lateinit var tvPointType: TextView
    private lateinit var tvLiftActionSetting: TextView
    private lateinit var rgLiftAction: RadioGroup
    private lateinit var rbCloseLiftControl: RadioButton
    private lateinit var rbAutoLiftUp: RadioButton
    private lateinit var rbAutoLiftDown: RadioButton
    private lateinit var rbManualLiftUp: RadioButton
    private lateinit var rbManualLiftDown: RadioButton

    
    
    override fun getLayoutRes() = R.layout.fragment_mode_route_edit_point

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val rvTaskPoint = findView<RecyclerView>(R.id.rv_task_point)
        routePointsAdapter = RoutePointsAdapter(
            requireContext(),
            routeWithPoints.pointsVOList.map { PointsVO(it) }.toMutableList(),
            this
        )
        rvTaskPoint.adapter = routePointsAdapter
        rvTaskPoint.layoutManager = LinearLayoutManager(requireContext())
        btnAddPoint = findView(R.id.btn_add)
        layoutEditPoint = findView(R.id.layout_edit_point)
        tvNotChoosePointShow = findView(R.id.tv_not_choose_point_show)
        tvPointName = findView(R.id.tv_point_name)
        rgWaitingTimeControlSwitch = findView(R.id.rg_waiting_time_switch)
        tvWaitingTimeSetting = findView(R.id.tv_waiting_time_setting)
        tvWaitingTime = findView(R.id.tv_waiting_time)
        ivEditWaitingTime = findView(R.id.iv_edit_waiting_time)
        tvPointType = findView(R.id.tv_point_type)
        tvLiftActionSetting = findView(R.id.tv_lift_action_setting)
        rgLiftAction = findView(R.id.rg_lift_action)
        rbCloseLiftControl = findView(R.id.rb_close_lift_control)
        rbAutoLiftUp = findView(R.id.rb_auto_lift_up)
        rbAutoLiftDown = findView(R.id.rb_auto_lift_down)
        rbManualLiftUp = findView(R.id.rb_manual_lift_up)
        rbManualLiftDown = findView(R.id.rb_manual_lift_down)
        btnAddPoint.setDebounceClickListener(this)
        ivEditWaitingTime.setDebounceClickListener(this)
        rbCloseLiftControl.setDebounceClickListener(this)
        rbAutoLiftUp.setDebounceClickListener(this)
        rbAutoLiftDown.setDebounceClickListener(this)
        rbManualLiftUp.setDebounceClickListener(this)
        rbManualLiftDown.setDebounceClickListener(this)
        findView<ImageView>(R.id.iv_clear_point).setDebounceClickListener(this)
        findView<AppCompatButton>(R.id.btn_return).setDebounceClickListener(this)
        findView<RadioButton>(R.id.rb_open_rotation_recovery).setDebounceClickListener(this)
        findView<RadioButton>(R.id.rb_close_rotation_recovery).setDebounceClickListener(this)
        updateView(-1)
    }

    private fun updateView(position: Int, pointsVO: PointsVO? = null) {
        if (position == -1) {
            layoutEditPoint.visibility = View.GONE
            tvNotChoosePointShow.visibility = View.VISIBLE
        } else if (pointsVO != null) {
            layoutEditPoint.visibility = View.VISIBLE
            tvNotChoosePointShow.visibility = View.GONE
            tvPointName.text = pointsVO.point
            tvPointType.text =
                PointContentUtils.getTypeShowContent(requireContext(), pointsVO.pointType)
            rgWaitingTimeControlSwitch.check(
                if (pointsVO.isOpenWaitingTime) {
                    R.id.rb_open_rotation_recovery
                } else {
                    R.id.rb_close_rotation_recovery
                }
            )
            updateWaitingTimeVisibility(
                if (pointsVO.isOpenWaitingTime) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            )
            tvWaitingTime.text = TimeUtil.formatTimeHourMinSec(pointsVO.waitingTime * 1000L)
            if (RobotInfo.isSpaceShip() && RobotInfo.isLiftModelInstalled) {
                tvLiftActionSetting.visibility = View.VISIBLE
                rgLiftAction.visibility = View.VISIBLE
            } else {
                tvLiftActionSetting.visibility = View.GONE
                rgLiftAction.visibility = View.GONE
            }
            if (pointsVO.pointType.equals(GenericPoint.AGV_TAG)) {
                rbAutoLiftUp.visibility = View.VISIBLE
                rbAutoLiftDown.visibility = View.VISIBLE
                rbManualLiftUp.visibility = View.GONE
                rbManualLiftDown.visibility = View.GONE
            } else {
                rbManualLiftUp.visibility = View.VISIBLE
                rbManualLiftDown.visibility = View.VISIBLE
                rbAutoLiftUp.visibility = View.GONE
                rbAutoLiftDown.visibility = View.GONE
            }
            updateLiftActionView(pointsVO.liftAction)
        }
    }

    private fun updateLiftActionView(liftAction: Int) {
        rgLiftAction.check(
            when (liftAction) {
                1 -> R.id.rb_auto_lift_up
                2 -> R.id.rb_auto_lift_down
                3 -> R.id.rb_manual_lift_up
                4 -> R.id.rb_manual_lift_down
                else -> R.id.rb_close_lift_control
            }
        )
    }

    private fun updateLiftAction(liftAction: Int, pointsVO: PointsVO) {
        if (pointsVO.isOpenWaitingTime) {
            updateWaitingTimeVisibility(View.GONE)
            rgWaitingTimeControlSwitch.check(R.id.rb_close_rotation_recovery)
            pointsVO.isOpenWaitingTime = false
        }
        pointsVO.liftAction = liftAction
        routePointsAdapter.updateItem(pointsVO)
    }

    /**
     * 检查顶升设置是否合法
     */
    private fun checkLiftAction(liftAction: Int): Boolean {

        /**
         * 找出当前点位前的最后一个开启了顶升控制的点位
         */
        fun findPreviousLiftAction(pointsVOList: List<PointsVO>): PointsVO? {
            for (i in (routePointsAdapter.getSelectItemIndex() - 1) downTo 0) {
                if (pointsVOList[i].liftAction != 0) {
                    return pointsVOList[i]
                }
            }
            return null
        }

        /**
         * 找出当前点位后的第一个开启了顶升控制的点位
         */
        fun findNextLiftAction(pointsVOList: List<PointsVO>): PointsVO? {
            for (i in routePointsAdapter.getSelectItemIndex() + 1 until pointsVOList.size) {
                if (pointsVOList[i].liftAction != 0) {
                    return pointsVOList[i]
                }
            }
            return null
        }


        if (liftAction == 0) return true
        val pointsVOList = routePointsAdapter.pointsVOList
        val isNextActionLegal = findNextLiftAction(pointsVOList)?.run {
            if (this.liftAction == 1 || this.liftAction == 3) {
                if (liftAction == 1 || liftAction == 3) {
                    EasyDialog.getInstance(requireContext()).warnError(
                        getString(
                            R.string.text_next_point_lift_action_is_lift_up,
                            this.point
                        )
                    )
                    return@run false
                }
            } else {
                if (liftAction == 2 || liftAction == 4) {
                    EasyDialog.getInstance(requireContext()).warnError(
                        getString(
                            R.string.text_next_point_lift_action_is_lift_down,
                            this.point
                        )
                    )
                    return@run false
                }

            }
            true
        } ?: true
        if (!isNextActionLegal) return false
        return findPreviousLiftAction(pointsVOList)?.run {
            if (this.liftAction == 1 || this.liftAction == 3) {
                if (liftAction == 1 || liftAction == 3) {
                    EasyDialog.getInstance(requireContext()).warnError(
                        getString(
                            R.string.text_previous_point_lift_action_is_lift_up,
                            this.point
                        )
                    )

                    return@run false
                }
            } else {
                if (liftAction == 2 || liftAction == 4) {
                    EasyDialog.getInstance(requireContext()).warnError(
                        getString(
                            R.string.text_previous_point_lift_action_is_lift_down,
                            this.point
                        )
                    )
                    return@run false
                }

            }
            true
        } ?: true
    }

    private fun updateWaitingTimeVisibility(visibility: Int) {
        tvWaitingTimeSetting.visibility = visibility
        tvWaitingTime.visibility = visibility
        ivEditWaitingTime.visibility = visibility
    }

    private fun showTimePicker(pointsVO: PointsVO) {
        val date = Calendar.getInstance()
        val hours = (pointsVO.waitingTime / 3600)
        date[Calendar.HOUR_OF_DAY] = hours
        val minutes = ((pointsVO.waitingTime - hours * 3600) / 60)
        date[Calendar.MINUTE] = minutes
        date[Calendar.SECOND] = (pointsVO.waitingTime - hours * 3600 - minutes * 60)
        val timePicker = TimePickerBuilder(
            requireContext()
        ) { date1: Date, v: View? ->
            val newTime = date1.seconds + date1.minutes * 60 + date1.hours * 60 * 60
            if (newTime == 0) {
                ToastUtils.showShortToast(getString(R.string.text_waiting_time_cannot_be_zero))
                return@TimePickerBuilder
            }
            val cycleTime =
                TimeUtil.formatHourAndMinuteAndSecond(date1)
            tvWaitingTime.text = cycleTime
            pointsVO.waitingTime = newTime
            routePointsAdapter.updateItem(pointsVO)
        }
            .setSubmitText(getString(R.string.text_confirm))
            .setCancelText(getString(R.string.text_cancel))
            .setType(booleanArrayOf(false, false, false, true, true, true))
            .setLabel("", "", "", "h", "m", "s")
            .setTitleSize(20)
            .setDate(date)
            .isCyclic(true)
            .isDialog(true)
            .setSubCalSize(24)
            .setContentTextSize(24)
            .setItemVisibleCount(9) //若设置偶数，实际值会加1（比如设置6，则最大可见条目为7）
            .setLineSpacingMultiplier(2.0f)
            .isAlphaGradient(true)
            .setOutSideCancelable(false)
            .build()
        timePicker.show()
    }

    interface OnModeRoutePointEditListener {
        fun onReturnClick(routeWithPoints: RouteWithPoints)
    }

    override fun onDeleteClick(position: Int, pointsVO: PointsVO) {
        EasyDialog.getInstance(requireContext())
            .confirm(
                getString(
                    R.string.text_confirm_to_delete_point,
                    pointsVO.point
                )
            ) { dialog, id ->
                dialog.dismiss()
                if (id == R.id.btn_confirm) {
                    routePointsAdapter.remove(position)
                }
            }
    }

    override fun onItemClick(position: Int, pointsVO: PointsVO?) {
        updateView(position, pointsVO)
    }

    override fun invoke(v: View) {
        when (v.id) {
            R.id.btn_add -> {
                PointChooseDialog(
                    requireContext(),
                    PointCacheInfo.routeModelPoints.groupBy { it.type }
                        .map {
                            PointContentUtils.getTypeShowContent(
                                requireContext(),
                                it.key
                            ) to it.value
                        },
                    null,
                    object : PointChooseDialog.OnPointChooseResultListener {
                        override fun onPointChooseResult(points: Pair<String, GenericPoint>) {
                            routePointsAdapter.addItem(
                                PointsVO().getDefault(
                                    points.second.name,
                                    points.second.type
                                )
                            )
                        }
                    }).show()
            }

            R.id.btn_return -> {
                val oldPointsVOList = routeWithPoints.pointsVOList
                val newPointsVOList = routePointsAdapter.pointsVOList
                if (oldPointsVOList.equals(newPointsVOList)) {
                    modeRoutePointEditListener.onReturnClick(routeWithPoints)
                    return
                }
                EasyDialog.getInstance(requireContext())
                    .neutral(
                        getString(R.string.text_save_and_return),
                        getString(R.string.text_only_exit),
                        getString(R.string.text_cancel),
                        getString(R.string.text_will_not_save_change_after_confirm)
                    ) { dialog, mId ->
                        dialog.dismiss()
                        if (mId == R.id.btn_confirm) {
                            routeWithPoints.pointsVOList = routePointsAdapter.pointsVOList
                            ToastUtils.showShortToast(getString(R.string.text_save_success))
                            modeRoutePointEditListener.onReturnClick(routeWithPoints)
                        } else if (mId == R.id.btn_neutral) {
                            modeRoutePointEditListener.onReturnClick(routeWithPoints)
                        }
                    }
            }

            R.id.rb_open_rotation_recovery -> {
                routePointsAdapter.getCurrentItem()?.let {
                    if (it.isOpenWaitingTime) return
                    if (it.liftAction != 0) {
                        EasyDialog.getInstance(requireContext())
                            .warnError(getString(R.string.text_please_close_lift_control_first))
                        return
                    }
                    updateWaitingTimeVisibility(View.VISIBLE)
                    it.isOpenWaitingTime = true
                    routePointsAdapter.updateItem(it)
                }
            }

            R.id.rb_close_rotation_recovery -> {
                routePointsAdapter.getCurrentItem()?.let {
                    if (!it.isOpenWaitingTime) return
                    updateWaitingTimeVisibility(View.GONE)
                    it.isOpenWaitingTime = false
                    routePointsAdapter.updateItem(it)
                }
            }

            R.id.iv_edit_waiting_time -> {
                routePointsAdapter.getCurrentItem()?.let {
                    showTimePicker(it)
                }
            }

            R.id.iv_clear_point -> {
                EasyDialog.getInstance(requireContext()).confirm(
                    getString(R.string.text_confirm_clear_all_points)
                ) { dialog, mId ->
                    dialog.dismiss()
                    if (mId == R.id.btn_confirm) {
                        routePointsAdapter.clear()
                    }
                }
            }

            R.id.rb_close_lift_control -> {
                routePointsAdapter.getCurrentItem()?.let {
                    if (it.liftAction == 0) return
                    it.liftAction = 0
                    routePointsAdapter.updateItem(it)
                }
            }

            R.id.rb_auto_lift_up -> {
                routePointsAdapter.getCurrentItem()?.let {
                    if (it.liftAction == 1) return
                    if (!checkLiftAction(1)) {
                        updateLiftActionView(it.liftAction)
                        return
                    }
                    updateLiftAction(1,it)
                }
            }

            R.id.rb_auto_lift_down -> {
                routePointsAdapter.getCurrentItem()?.let {
                    if (it.liftAction == 2) return
                    if (!checkLiftAction(2)) {
                        updateLiftActionView(it.liftAction)
                        return
                    }
                    updateLiftAction(2,it)
                }
            }

            R.id.rb_manual_lift_up -> {
                routePointsAdapter.getCurrentItem()?.let {
                    if (it.liftAction == 3) return
                    if (!checkLiftAction(3)) {
                        updateLiftActionView(it.liftAction)
                        return
                    }
                    updateLiftAction(3,it)
                }
            }

            R.id.rb_manual_lift_down -> {
                routePointsAdapter.getCurrentItem()?.let {
                    if (it.liftAction == 4) return
                    if (!checkLiftAction(4)) {
                        updateLiftActionView(it.liftAction)
                        return
                    }
                    updateLiftAction(4,it)
                }
            }
        }
    }

}