package com.reeman.agv.task.impl

import android.content.Intent
import com.reeman.agv.calling.CallingInfo
import com.reeman.agv.calling.model.TaskInfo
import com.reeman.agv.task.Task
import com.reeman.commons.constants.Constants
import com.reeman.commons.state.RobotInfo
import com.reeman.commons.state.TaskAction
import com.reeman.commons.state.TaskMode
import com.reeman.points.utils.PointCacheInfo
import com.reeman.ros.ROSController
import timber.log.Timber

class TaskModeCharge : Task {
    private var token: String? = null


    override fun initTask(intent: Intent) {
        if (intent.hasExtra(Constants.TASK_TOKEN)) {
            token = intent.getStringExtra(Constants.TASK_TOKEN)
        }
        val currentTimeMillis = System.currentTimeMillis()
        CallingInfo.heartBeatInfo.currentTask = TaskInfo(
            currentTimeMillis,
            currentTimeMillis,
            TaskMode.MODE_CHARGE.ordinal,
            token,
            PointCacheInfo.chargePoint.second.name
        )
        ROSController.setTolerance(if (RobotInfo.returningSetting.stopNearBy) 1 else 0)
        Timber.w("返航配置 : ${RobotInfo.returningSetting}")
    }

    override fun getNextPointWithElevator(): Pair<String, String> {
        val (first, second) = PointCacheInfo.chargePoint
        return Pair(first, second.name)
    }

    override fun getNextPointWithoutElevator(): String {
        return PointCacheInfo.chargePoint.second.name
    }

    override fun hasNext(): Boolean {
        return false
    }

    override fun getDeliveryPointSize(): Int {
        return 0
    }

    override fun getSpeed(): String {
        return RobotInfo.returningSetting.gotoChargingPileSpeed.toString()
    }

    override fun showReturnBtn(): Boolean {
        return false
    }

    override fun resetStopNearByParameter() {
        ROSController.setTolerance(if (RobotInfo.returningSetting.stopNearBy) 1 else 0)
    }

    override fun resetAllParameter(robotInfo: RobotInfo, callingInfo: CallingInfo) {
        super.resetAllParameter(robotInfo, callingInfo)
        ROSController.setTolerance(1)
    }

    override fun updateRobotSize(expansion: Boolean) {
        val robotType = RobotInfo.robotType
        if (robotType == 2 || robotType == 3) { //24/22.5
            ROSController.robotRadius(if (expansion) 0.28f else 0f)
        } else {
            val supportNewFootprint = RobotInfo.isSupportNewFootprint
            val robotSize = if (expansion) {
                if (supportNewFootprint) {
                    when (robotType) {
                        1 -> doubleArrayOf(0.3, 0.2, 0.3, 0.2)
                        4, 6, 7 -> doubleArrayOf(0.43, 0.28, 0.43, 0.28)
                        else -> doubleArrayOf(0.37, 0.26, 0.37, 0.26)
                    }
                } else {
                    when (robotType) {
                        1 -> doubleArrayOf(0.6, 0.4)
                        4, 6, 7 -> doubleArrayOf(0.85, 0.6)
                        else -> doubleArrayOf(0.74, 0.51)
                    }
                }
            } else {
                if (supportNewFootprint) {
                    doubleArrayOf(0.0, 0.0, 0.0, 0.0)
                } else {
                    doubleArrayOf(0.0, 0.0)
                }
            }
            ROSController.footprint(robotSize)
        }
    }

    override fun shouldRemoveFirstCallingTask(): Boolean {
        return token != null
    }

    override fun getAction() = TaskAction.charge_point

}