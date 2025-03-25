package com.reeman.agv.fragments.main

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import androidx.appcompat.widget.AppCompatButton
import com.kyleduo.switchbutton.SwitchButton
import com.reeman.agv.R
import com.reeman.agv.base.BaseFragment
import com.reeman.agv.calling.CallingInfo
import com.reeman.agv.calling.exception.NoFindPointException
import com.reeman.agv.calling.utils.PointCheckUtil.filterNonExistentPathPoints
import com.reeman.agv.contract.ModeRouteContract
import com.reeman.agv.fragments.main.ModeRouteEditFragment.ModeRouteEditClickListener
import com.reeman.agv.fragments.main.listener.OnGreenButtonClickListener
import com.reeman.agv.presenter.impl.ModeRoutePresenter
import com.reeman.agv.utils.DebounceClickListener
import com.reeman.agv.utils.VoiceHelper
import com.reeman.agv.widgets.EasyDialog
import com.reeman.commons.state.RobotInfo
import com.reeman.dao.repository.entities.RouteWithPoints
import com.reeman.points.utils.PointCacheInfo
import com.reeman.ros.ROSController
import kotlinx.coroutines.Job
import timber.log.Timber

class ModeRouteFragment(
    private var isEditMode: Boolean,
    private val modeRouteClickListener: ModeRouteClickListener
) : BaseFragment(), ModeRouteContract.View, OnGreenButtonClickListener,DebounceClickListener {
    private lateinit var presenter: ModeRoutePresenter
    private lateinit var btnStart: AppCompatButton
    private lateinit var routeWithPointsList: List<RouteWithPoints>

    
    
    override fun getLayoutRes(): Int {
        return R.layout.fragment_mode_route
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presenter = ModeRoutePresenter(this)
        presenter.getAllRoute(RobotInfo.navigationMode)
        btnStart = findView(R.id.btn_start)
        val sbMode = findView<SwitchButton>(R.id.sb_mode)
        sbMode.isChecked = !isEditMode
        sbMode.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            initViewByMode(!isChecked)
            isEditMode = !isChecked
        }
        btnStart.setDebounceClickListener { onStartClick()  }
    }

    private fun initViewByMode(isEditMode: Boolean) {
        if (isEditMode) {
            initRouteEditMode()
            btnStart.isClickable = false
            btnStart.setBackgroundResource(R.drawable.bg_common_button_inactive)
        } else {
            initRouteTaskMode()
            btnStart.isClickable = true
            btnStart.setBackgroundResource(R.drawable.bg_common_button_active)
        }
    }

    private fun initRouteEditMode() {
        childFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.custom_anim_enter, R.anim.custom_anim_exit)
            .replace(
                R.id.route_fragment_view,
                ModeRouteEditFragment(routeWithPointsList, modeRouteEditClickListener)
            ).commit()
    }

    private fun initRouteTaskMode() {
        childFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.custom_anim_enter, R.anim.custom_anim_exit)
            .replace(R.id.route_fragment_view, ModeRouteTaskFragment(routeWithPointsList)).commit()
    }

    private val modeRouteEditClickListener: ModeRouteEditClickListener =
        object : ModeRouteEditClickListener {
            override fun onAddRouteTask(routeWithPoints: RouteWithPoints) {
                modeRouteClickListener.onAddClick(routeWithPoints)
            }

            override fun onEditRouteTask(routeWithPoints: RouteWithPoints) {
                modeRouteClickListener.onEditClick(routeWithPoints)
            }
        }

    private fun onStartClick() {
        if (EasyDialog.isShow()) EasyDialog.getInstance().dismiss()
        val currentFragment = childFragmentManager.findFragmentById(R.id.route_fragment_view)
        if (currentFragment is ModeRouteTaskFragment) {
            if (currentFragment.selectedRouteIndex < 0 || currentFragment.selectedRouteIndex >= routeWithPointsList.size) {
                EasyDialog.getInstance(requireActivity())
                    .warnError(getString(R.string.voice_please_select_route))
                VoiceHelper.play("voice_please_select_route")
                return
            }
            val routeWithPoints = routeWithPointsList[currentFragment.selectedRouteIndex]
            val pointsVOList = routeWithPoints.pointsVOList
            if (pointsVOList.isNullOrEmpty()) {
                EasyDialog.getInstance(requireActivity())
                    .warnError(getString(R.string.voice_please_choose_route_point_first))
                return
            }
            try {
                filterNonExistentPathPoints(pointsVOList, PointCacheInfo.routeModelPoints)
                if (RobotInfo.isLiftModelInstalled && RobotInfo.isSpaceShip()) {
                    val firstOpenLiftActionPoint = pointsVOList.firstOrNull { it.liftAction != 0 }
                    firstOpenLiftActionPoint?.let {
                        if (RobotInfo.liftModelState == 0 && (it.liftAction == 2 || it.liftAction == 4)) {
                            EasyDialog.getInstance(requireContext())
                                .confirm(
                                    getString(R.string.text_lift_up),
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
                modeRouteClickListener.onStart(routeWithPoints)
            } catch (e: NoFindPointException) {
                Timber.w(e, "路线模式找不到点")
                EasyDialog.getInstance(requireContext()).warnError(
                    getString(
                        R.string.text_filter_not_exist_points,
                        e.points.toString()
                    )
                )
            }
        }
    }

    override fun onGetAllRouteFailed(throwable: Throwable) {
        EasyDialog.getInstance(requireContext())
            .warn(getString(R.string.text_get_route_failed, throwable.message)) { dialog, id ->
                dialog.dismiss()
                modeRouteClickListener.onGetRouteFailed()
            }
    }

    override fun onGetAllRouteSuccess(routeWithPointsList: List<RouteWithPoints>) {
        this.routeWithPointsList = routeWithPointsList
        initViewByMode(isEditMode)
        if (!isEditMode && RobotInfo.isLifting) {
            EasyDialog.getInstance(requireContext())
                .warn(getString(R.string.text_check_altitude_moving)) { dialog: Dialog, id: Int ->
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
                                .loading(getString(R.string.text_pickup_model_resetting))
                        }, 200)
                    }
                    dialog.dismiss()
                }
            return
        }
    }

    override fun onKeyUpEvent() {
        if (isEditMode) return
        onStartClick()
    }

    interface ModeRouteClickListener {
        fun onAddClick(routeWithPoints: RouteWithPoints)
        fun onEditClick(routeWithPoints: RouteWithPoints)
        fun onStart(routeWithPoints: RouteWithPoints)
        fun onGetRouteFailed()
    }
}