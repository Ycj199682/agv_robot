package com.reeman.agv.fragments.main

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentResultListener
import com.bigkoo.pickerview.builder.TimePickerBuilder
import com.bigkoo.pickerview.view.TimePickerView
import com.reeman.agv.R
import com.reeman.agv.base.BaseFragment
import com.reeman.agv.calling.CallingInfo
import com.reeman.agv.calling.exception.NoFindPointException
import com.reeman.agv.calling.utils.PointCheckUtil
import com.reeman.agv.contract.ModeRouteContract
import com.reeman.agv.presenter.impl.ModeRoutePresenter
import com.reeman.agv.utils.DebounceClickListener
import com.reeman.agv.widgets.AutoWrapTextViewGroup
import com.reeman.commons.constants.Constants
import com.reeman.dao.repository.entities.PointsVO
import com.reeman.commons.utils.SpManager
import com.reeman.commons.utils.TimeUtil
import com.reeman.dao.repository.entities.RouteWithPoints
import com.reeman.agv.utils.ToastUtils
import com.reeman.agv.utils.VoiceHelper
import com.reeman.agv.widgets.EasyDialog
import com.reeman.commons.state.RobotInfo
import com.reeman.points.utils.PointCacheInfo
import com.reeman.ros.ROSController
import kotlinx.coroutines.Job
import timber.log.Timber
import java.util.Calendar

class ModeRouteAttributesEditFragment(
    private val routeWithPoints: RouteWithPoints,
    private val clickListener: ModeRouteAttributesEditClickListener
) : BaseFragment(), ModeRouteContract.View,DebounceClickListener, (View) -> Unit {

    private lateinit var presenter: ModeRoutePresenter
    private lateinit var etRouteName: EditText
    private lateinit var tvTaskAgainInterval: TextView
    private lateinit var tvNoPoint: TextView
    private lateinit var awtvgPoints: AutoWrapTextViewGroup


    override fun getLayoutRes(): Int {
        return R.layout.fragment_mode_route_attributes_edit
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presenter = ModeRoutePresenter(this)
        findView<TextView>(R.id.tv_only_return).setDebounceClickListener(this)
        findView<TextView>(R.id.tv_return_and_save).setDebounceClickListener(this)
        findView<ImageButton>(R.id.iv_task_aging_interval).setDebounceClickListener(this)
        findView<ImageButton>(R.id.iv_edit_point).setDebounceClickListener(this)
        findView<AppCompatButton>(R.id.btn_start_test).setDebounceClickListener(this)

        tvTaskAgainInterval = findView(R.id.tv_task_again_interval)
        tvNoPoint = findView(R.id.tv_no_point)
        awtvgPoints = findView(R.id.awtvg_points)
        etRouteName = findView(R.id.et_route_name)
        val rgFinishAction = findView<RadioGroup>(R.id.rg_finish_action)
        val rgTaskCycleSwitch = findView<RadioGroup>(R.id.rg_task_cycle_switch)
        val layoutTaskAgainInterval = findView<LinearLayout>(R.id.layout_task_again_interval)
        val layoutTaskCycleSwitch = findView<LinearLayout>(R.id.layout_task_cycle_switch)

        etRouteName.setText(routeWithPoints.routeName)
        etRouteName.setSelection(routeWithPoints.routeName.length)

        when (routeWithPoints.taskFinishAction) {
            0 -> rgFinishAction.check(R.id.rb_return_product_point)
            2 -> rgFinishAction.check(R.id.rb_start_route_cruising_again)
            1 -> rgFinishAction.check(R.id.rb_return_charge_point)
        }

        rgTaskCycleSwitch.check(if (routeWithPoints.isExecuteAgainSwitch) R.id.rb_open_task_cycle else R.id.rb_close_task_cycle)

        layoutTaskAgainInterval.visibility =
            if (routeWithPoints.isExecuteAgainSwitch) View.VISIBLE else View.GONE
        layoutTaskCycleSwitch.visibility =
            if (routeWithPoints.taskFinishAction == 2) View.GONE else View.VISIBLE

        if (routeWithPoints.taskFinishAction == 2) {
            layoutTaskAgainInterval.visibility = View.GONE
        }

        tvTaskAgainInterval.text =
            TimeUtil.formatTimeHourMinSec(routeWithPoints.executeAgainTime * 1000L)

        val pointList = routeWithPoints.pointsVOList.map { it.point }

        if (pointList.isEmpty()) {
            tvNoPoint.visibility = View.VISIBLE
            awtvgPoints.visibility = View.GONE
        } else {
            tvNoPoint.visibility = View.GONE
            awtvgPoints.visibility = View.VISIBLE
        }
        awtvgPoints.setData(pointList)

        rgFinishAction.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rb_return_product_point -> {
                    routeWithPoints.taskFinishAction = 0
                    layoutTaskCycleSwitch.visibility = View.VISIBLE
                    layoutTaskAgainInterval.visibility =
                        if (routeWithPoints.isExecuteAgainSwitch) View.VISIBLE else View.GONE
                }

                R.id.rb_start_route_cruising_again -> {
                    routeWithPoints.taskFinishAction = 2
                    layoutTaskCycleSwitch.visibility = View.GONE
                    layoutTaskAgainInterval.visibility = View.GONE
                }

                R.id.rb_return_charge_point -> {
                    routeWithPoints.taskFinishAction = 1
                    layoutTaskCycleSwitch.visibility = View.VISIBLE
                    layoutTaskAgainInterval.visibility =
                        if (routeWithPoints.isExecuteAgainSwitch) View.VISIBLE else View.GONE
                }
            }
        }

        rgTaskCycleSwitch.setOnCheckedChangeListener { _, checkedId ->
            routeWithPoints.isExecuteAgainSwitch = checkedId == R.id.rb_open_task_cycle
            layoutTaskAgainInterval.visibility =
                if (routeWithPoints.isExecuteAgainSwitch) View.VISIBLE else View.GONE
        }

        val languageType = SpManager.getInstance()
            .getInt(Constants.KEY_LANGUAGE_TYPE, Constants.DEFAULT_LANGUAGE_TYPE)
        rgFinishAction.orientation =
            if (languageType == 1) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
    }

    private fun showTimePicker(seconds: Long) {
        val date = Calendar.getInstance()
        val hours = (seconds / 3600).toInt()
        date.set(Calendar.HOUR_OF_DAY, hours)
        val minutes = ((seconds - hours * 3600) / 60).toInt()
        date.set(Calendar.MINUTE, minutes)
        date.set(Calendar.SECOND, (seconds - hours * 3600 - minutes * 60).toInt())
        val timePicker = TimePickerBuilder(requireContext()) { date1, _ ->
            val cycleTime = TimeUtil.formatHourAndMinuteAndSecond(date1)
            tvTaskAgainInterval.text = cycleTime
            routeWithPoints.executeAgainTime =
                date1.seconds + date1.minutes * 60 + date1.hours * 60 * 60
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
            .setItemVisibleCount(9)
            .setLineSpacingMultiplier(2.0f)
            .isAlphaGradient(true)
            .setOutSideCancelable(false)
            .build()
        timePicker.show()
    }

    override fun onUpdateRouteSuccess() {
        if (EasyDialog.isShow()) EasyDialog.getInstance().dismiss()
        ToastUtils.showShortToast(getString(R.string.text_save_success))
        clickListener.onReturnClick()
    }

    override fun onUpdateRouteFailed(throwable: Throwable) {
        if (EasyDialog.isShow()) EasyDialog.getInstance().dismiss()
        EasyDialog.getInstance(requireContext())
            .warnError(getString(R.string.text_save_route_failed, throwable.message))
    }

    override fun onAddRouteSuccess() {
        if (EasyDialog.isShow()) EasyDialog.getInstance().dismiss()
        ToastUtils.showShortToast(getString(R.string.text_save_success))
        clickListener.onReturnClick()
    }

    override fun onAddRouteFailed(throwable: Throwable) {
        if (EasyDialog.isShow()) EasyDialog.getInstance().dismiss()
        EasyDialog.getInstance(requireContext())
            .warnError(getString(R.string.text_save_route_failed, throwable.message))
    }

    override fun onGetAllRouteFailed(throwable: Throwable) {
        if (EasyDialog.isShow()) EasyDialog.getInstance().dismiss()
        EasyDialog.getInstance(requireContext())
            .warnError(getString(R.string.text_save_route_failed, throwable.message))
    }

    override fun onGetAllRouteSuccess(routeWithPointsList: List<RouteWithPoints>) {
        if (EasyDialog.isShow()) EasyDialog.getInstance().dismiss()
        when {
            routeWithPointsList.isEmpty() -> {
                if (routeWithPoints.id > 1) routeWithPoints.id = 0
                presenter.addRoute(routeWithPoints)
            }

            routeWithPointsList.any { it.routeName == routeWithPoints.routeName && it.id != routeWithPoints.id } -> {
                ToastUtils.showShortToast(getString(R.string.text_route_name_cannot_same))
            }

            routeWithPoints.id == 0L -> presenter.addRoute(routeWithPoints)
            else -> presenter.updateRoute(routeWithPoints)
        }
    }

    private fun onStartClick() {
        if (EasyDialog.isShow()) EasyDialog.getInstance().dismiss()
        val pointsVOList = routeWithPoints.pointsVOList
        if (pointsVOList.isNullOrEmpty()) {
            EasyDialog.getInstance(requireActivity())
                .warnError(getString(R.string.voice_please_choose_route_point_first))
            return
        }
        if (RobotInfo.isLiftModelInstalled && RobotInfo.isSpaceShip()) {
            val firstOpenLiftActionPoint = pointsVOList.firstOrNull { it.liftAction != 0 }
            firstOpenLiftActionPoint?.let {
                if (RobotInfo.liftModelState == 0 && (it.liftAction == 2 || it.liftAction == 4)) {
                    EasyDialog.getInstance(requireContext())
                        .confirm(
                            getString(R.string.text_lift_down),
                            getString(R.string.text_cancel),
                            getString(
                                R.string.text_check_point_lift_down_but_lift_model_already_down,
                                it.point
                            )
                        ) { dialog, id ->
                            dialog.dismiss()
                            if (id == R.id.btn_confirm) {
                                ROSController.ioControl(0x04)
                                mHandler.postDelayed({
                                    if (RobotInfo.isEmergencyButtonDown) {
                                        VoiceHelper.play("voice_scram_stop_turn_on")
                                        EasyDialog.getInstance(requireContext())
                                            .warnError(getString(R.string.voice_scram_stop_turn_on))
                                        return@postDelayed
                                    }
                                    ROSController.liftUp()
                                    CallingInfo.isLifting = true
                                    EasyDialog.getLoadingInstance(requireContext())
                                        .loading(getString(R.string.text_pickup_model_lifting_up))
                                }, 200)
                            }
                        }
                    return
                } else if (RobotInfo.liftModelState == 1 && (it.liftAction == 1 || it.liftAction == 3)) {
                    EasyDialog.getInstance(requireContext())
                        .confirm(
                            getString(R.string.text_lift_down),
                            getString(R.string.text_cancel),
                            getString(
                                R.string.text_check_point_lift_up_but_lift_model_already_up,
                                it.point
                            )
                        ) { dialog, id ->
                            dialog.dismiss()
                            if (id == R.id.btn_confirm) {
                                ROSController.ioControl(0x04)
                                mHandler.postDelayed({
                                    if (RobotInfo.isEmergencyButtonDown) {
                                        VoiceHelper.play("voice_scram_stop_turn_on")
                                        EasyDialog.getInstance(requireContext())
                                            .warnError(getString(R.string.voice_scram_stop_turn_on))
                                        return@postDelayed
                                    }
                                    ROSController.liftDown()
                                    CallingInfo.isLifting = true
                                    EasyDialog.getLoadingInstance(requireContext())
                                        .loading(getString(R.string.text_pickup_model_lifting_down))
                                }, 200)
                            }
                        }
                    return
                }
            }
        }
        clickListener.onStartTestClick(routeWithPoints)
    }

    interface ModeRouteAttributesEditClickListener {
        fun onEditPoints(routeWithPoints: RouteWithPoints)
        fun onReturnClick()
        fun onStartTestClick(routeWithPoints: RouteWithPoints)
    }

    override fun invoke(v: View) {
        when (v.id) {
            R.id.btn_start_test -> {
                onStartClick()
            }

            R.id.tv_only_return -> {
                EasyDialog.getInstance(requireContext())
                    .confirm(getString(R.string.text_confirm_only_exit_route_attributes_edit)) { dialog, id1 ->
                        dialog.dismiss()
                        if (id1 == R.id.btn_confirm) {
                            clickListener.onReturnClick()
                        }
                    }
            }

            R.id.tv_return_and_save -> {
                val routeName = etRouteName.text.toString()
                if (routeName.isEmpty()) {
                    etRouteName.error = getString(R.string.text_route_name_cannot_be_null)
                    return
                }
                if (routeWithPoints.taskFinishAction != 2 && routeWithPoints.isExecuteAgainSwitch && routeWithPoints.executeAgainTime < 1) {
                    ToastUtils.showShortToast(getString(R.string.text_task_execute_again_time_must_bigger_than_one_seconds))
                    return
                }
                val pointsVOList = routeWithPoints.pointsVOList
                if (pointsVOList.isNullOrEmpty()) {
                    ToastUtils.showShortToast(getString(R.string.text_please_select_task_point_first))
                    return
                }
                if (routeWithPoints.taskFinishAction == 2 || routeWithPoints.isExecuteAgainSwitch) {
                    routeWithPoints.pointsVOList.firstOrNull { it.liftAction != 0 }
                        ?.let { firstLiftPoint ->
                            routeWithPoints.pointsVOList.lastOrNull { it.liftAction != 0 && it != firstLiftPoint }
                                ?.let { lastLiftPoint ->
                                    if ((firstLiftPoint.liftAction in listOf(1,3) && lastLiftPoint.liftAction in listOf(1,3))||
                                        (firstLiftPoint.liftAction in listOf(2,4) && lastLiftPoint.liftAction in listOf(2,4))){
                                        EasyDialog.getInstance(requireContext()).warnError(getString(R.string.text_check_first_and_last_lift_point_has_same_action))
                                        return
                                    }
                                }
                        }
                    if (routeWithPoints.pointsVOList.count { it.liftAction != 0 } == 1){
                        EasyDialog.getInstance(requireContext()).warnError(getString(R.string.text_check_only_one_lift_point))
                        return
                    }
                }
                routeWithPoints.routeName = routeName
                Timber.w("保存路线任务: $routeWithPoints")
                EasyDialog.getLoadingInstance(requireContext())
                    .loading(getString(R.string.text_is_saving))
                presenter.getAllRoute(robotInfo.navigationMode)
            }

            R.id.iv_task_aging_interval -> {
                val time = tvTaskAgainInterval.text.toString().split(":")
                val hour = time[0].toInt()
                val minute = time[1].toInt()
                val second = time[2].toInt()
                showTimePicker(hour * 3600L + minute * 60L + second)
            }

            R.id.iv_edit_point -> clickListener.onEditPoints(routeWithPoints)
        }
    }

}
