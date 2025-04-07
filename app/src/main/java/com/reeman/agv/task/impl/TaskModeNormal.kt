package com.reeman.agv.task.impl

import android.content.Intent
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.reeman.agv.calling.CallingInfo
import com.reeman.agv.calling.model.TaskInfo
import com.reeman.agv.task.Task
import com.reeman.agv.utils.PointContentUtils
import com.reeman.commons.constants.Constants
import com.reeman.commons.state.RobotInfo
import com.reeman.commons.state.TaskAction
import com.reeman.commons.state.TaskMode
import com.reeman.points.utils.PointCacheInfo
import com.reeman.ros.ROSController
import kotlinx.serialization.json.Json
import timber.log.Timber

class TaskModeNormal(private val gson: Gson) : Task {
    private var pointList: List<Pair<String, String>>? = null
    private var currentPointListIndex = 0
    private var arrivedPointSize = 0
    private var token: String? = null
    private var isAbortTaskToProductionPoint = false

    override fun initTask(intent: Intent) {
        val taskTarget = intent.getStringExtra(Constants.TASK_TARGET)
        if (intent.hasExtra(Constants.TASK_TOKEN)) {
            token = intent.getStringExtra(Constants.TASK_TOKEN)
        }
        pointList = Json.decodeFromString(taskTarget!!)
        Timber.tag("mylog-task-init").w("普通模式配送配置 : ${RobotInfo.modeNormalSetting} \n 点位信息 : $taskTarget\n返航配置 : ${RobotInfo.returningSetting}")
        val currentTimeMillis = System.currentTimeMillis()
        CallingInfo.heartBeatInfo.currentTask = TaskInfo(
            currentTimeMillis,
            currentTimeMillis,
            TaskMode.MODE_NORMAL.ordinal,
            token,
            nextPoint
        )
        if (RobotInfo.isNormalModeWithManualLiftControl) {
            ROSController.avoidObstacle(RobotInfo.modeNormalSetting.rotate)
            ROSController.setTolerance(if (RobotInfo.modeNormalSetting.stopNearBy) 1 else 0)
        } else {
            ROSController.setTolerance(if (RobotInfo.returningSetting.stopNearBy) 1 else 0)
        }
    }

    private val nextPoint: String
        get() {
            if (RobotInfo.isElevatorMode) {
                val (first, second) = nextPointWithElevator
                return "$first - $second"
            }
            return nextPointWithoutElevator
        }

    /**
     * 获取下一个配送点(带梯控)
     *
     * @return
     */
    override fun getNextPointWithElevator(): Pair<String, String> {
        if (isAbortTaskToProductionPoint) {
            return PointContentUtils.getProductionPointWithMap()
        }
        return if (pointList.isNullOrEmpty() || currentPointListIndex < 0 || currentPointListIndex > pointList!!.size - 1) {
            when (RobotInfo.modeNormalSetting.finishAction) {
                0 -> PointContentUtils.getProductionPointWithMap()

                1 -> PointCacheInfo.chargePoint.run { Pair(this.first, this.second.name) }
                else -> Pair("", "")
            }
        } else {
            pointList!![currentPointListIndex]
        }
    }

    /**
     * 获取下一个配送点
     *
     * @return
     */
    override fun getNextPointWithoutElevator(): String {
        if (isAbortTaskToProductionPoint) return PointContentUtils.getProductionPoint()
        return if (pointList.isNullOrEmpty() || currentPointListIndex < 0 || currentPointListIndex > pointList!!.size - 1) {
            when (RobotInfo.modeNormalSetting.finishAction) {
                0 -> PointContentUtils.getProductionPoint()

                1 -> PointCacheInfo.chargePoint.second.name
                else -> ""
            }
        } else pointList!![currentPointListIndex].second
    }

    /**
     * 到达点位后从待配送列表中移除点位
     *
     * @param pointName
     */
    override fun arrivedPoint(pointName: String) {
        arrivedPointSize++
        currentPointListIndex++
        CallingInfo.heartBeatInfo.currentTask!!.targetPoint = nextPoint
    }

    override fun showSkipCurrentTargetBtn(): Boolean {
        return pointList != null && currentPointListIndex != -1 && currentPointListIndex < pointList!!.size - 1
    }

    override fun skipCurrentTarget() {
        currentPointListIndex++
    }

    override fun hasNext(): Boolean {
        return !pointList.isNullOrEmpty() && currentPointListIndex != -1 && currentPointListIndex < pointList!!.size
    }

    override fun showReturnBtn(): Boolean {
        return hasNext() || (!hasNext() && RobotInfo.modeNormalSetting.finishAction != 2)
    }

    override fun getDeliveryPointSize(): Int {
        return arrivedPointSize
    }

    override fun setRobotWidthAndLidar(reset: Boolean) {
        if (RobotInfo.isNormalModeWithManualLiftControl) {
            if (reset) {
                ROSController.lidarMin(Constants.DEFAULT_QRCODE_MODE_LIDAR_WIDTH_WITHOUT_THING)
            } else {
                ROSController.lidarMin(RobotInfo.modeNormalSetting.lidarWidth)
            }
            val robotSize = if (RobotInfo.isSupportNewFootprint) {
                if (reset) {
                    doubleArrayOf(0.0, 0.0, 0.0, 0.0)
                } else {
                    RobotInfo.modeNormalSetting.robotSize.map { it.toDouble() }.toDoubleArray()
                }
            } else {
                if (reset) {
                    doubleArrayOf(0.0, 0.0)
                } else {
                    doubleArrayOf(
                        RobotInfo.modeNormalSetting.length.toDouble(),
                        RobotInfo.modeNormalSetting.width.toDouble()
                    )
                }
            }
            ROSController.footprint(robotSize)
        }
    }

    override fun getSpeed(): String {
        return (if (hasNext()) RobotInfo.modeNormalSetting.speed else RobotInfo.returningSetting.gotoProductionPointSpeed).toString()
    }

    override fun isOpenedManualLiftUpControl(isArrived: Boolean): Boolean {
        return RobotInfo.isNormalModeWithManualLiftControl
    }

    override fun isOpenedManualLiftDownControl(isArrived: Boolean): Boolean {
        return RobotInfo.isNormalModeWithManualLiftControl
    }

    override fun getCountDownTime(): Long {
        return if (RobotInfo.modeNormalSetting.waitingCountDownTimerOpen) RobotInfo.modeNormalSetting.waitingTime.toLong() else 0
    }

    override fun getPlayArrivedTipTime(): Long {
        return if (RobotInfo.modeNormalSetting.waitingCountDownTimerOpen) RobotInfo.modeNormalSetting.playArrivedTipTime.toLong() else -1
    }

    override fun clearAllPoints() {
        currentPointListIndex = -1
    }

    override fun isArrivedLastPointAndEndInPlace(): Boolean {
        return !hasNext() && RobotInfo.modeNormalSetting.finishAction == 2
    }

    override fun getTaskFinishAction(): Int {
        return RobotInfo.modeNormalSetting.finishAction
    }

    override fun resetAllParameter(robotInfo: RobotInfo, callingInfo: CallingInfo) {
        super.resetAllParameter(robotInfo, callingInfo)
        if (RobotInfo.isNormalModeWithManualLiftControl) {
            ROSController.avoidObstacle(true)
        }
        ROSController.setTolerance(1)
        Timber.w("将普通模式相关配置恢复默认值")
    }

    override fun resetStopNearByParameter() {
        if (RobotInfo.isNormalModeWithManualLiftControl) {
            ROSController.setTolerance(if (RobotInfo.modeNormalSetting.stopNearBy) 1 else 0)
        } else {
            ROSController.setTolerance(if (RobotInfo.returningSetting.stopNearBy) 1 else 0)
        }
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
        val firstCallingDetails = CallingInfo.getFirstCallingDetails()
        Timber.w("队列中第一个任务 : $firstCallingDetails")
        return firstCallingDetails != null && firstCallingDetails.key == token && firstCallingDetails.mode == TaskMode.MODE_NORMAL
    }

    override fun getAction(): String {
        if (isAbortTaskToProductionPoint)return TaskAction.production_point
        return if (hasNext()) {
            TaskAction.delivery_point
        } else {
            RobotInfo.modeNormalSetting.finishAction.run {
                when (this) {
                    0 -> TaskAction.production_point
                    1 -> TaskAction.charge_point
                    else -> "stay"
                }
            }
        }
    }

    override fun setAbortTaskToProductionPoint(isAbortTaskToProductionPoint: Boolean) {
        this.isAbortTaskToProductionPoint = isAbortTaskToProductionPoint
    }

    override fun isAbortTaskToProductionPoint() = isAbortTaskToProductionPoint

}