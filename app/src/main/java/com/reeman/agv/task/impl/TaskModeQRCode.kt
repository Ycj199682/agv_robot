package com.reeman.agv.task.impl

import android.content.Intent
import com.google.gson.Gson
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

class TaskModeQRCode(private val gson: Gson) : Task {
    private var isArrivedFirstPoint = false
    private var arrivedPointSize = 0
    private var qrCodePointsPairList: List<Pair<Pair<String, String>, Pair<String, String>>>? = null
    private var currentPairIndex = 0
    private var token: String? = null
    private var isAbortTaskToProductionPoint = false

    override fun initTask(intent: Intent) {
        val taskTarget = intent.getStringExtra(Constants.TASK_TARGET)
        token = intent.getStringExtra(Constants.TASK_TOKEN)

        qrCodePointsPairList = Json.decodeFromString(taskTarget!!)
        if (RobotInfo.modeQRCodeSetting.lift && RobotInfo.isLiftModelInstalled) {
            ROSController.ioControl(4)
            ROSController.setTolerance(if (RobotInfo.modeQRCodeSetting.stopNearby) 1 else 0)
        } else {
            ROSController.setTolerance(if (RobotInfo.returningSetting.stopNearBy) 1 else 0)
        }
        ROSController.avoidObstacle(RobotInfo.modeQRCodeSetting.rotate)
        ROSController.agvMode(if (RobotInfo.modeQRCodeSetting.direction) 1 else 0)
        Timber.w("标签码模式配送配置 : ${RobotInfo.modeQRCodeSetting} \n 点位信息 : $taskTarget\n返航配置 : ${RobotInfo.returningSetting}")
        val currentTimeMillis = System.currentTimeMillis()
        var createTime = currentTimeMillis
        if (!token.isNullOrBlank()) {
            val taskDetails = CallingInfo.getTaskDetailsByToken(token!!)
            if (taskDetails != null) {
                createTime = taskDetails.firstCallingTime
            }
        }
        CallingInfo.heartBeatInfo.currentTask = TaskInfo(
            createTime,
            currentTimeMillis,
            TaskMode.MODE_QRCODE.ordinal,
            token,
            nextPoint
        )
    }

    private val nextPoint: String
        get() {
            if (RobotInfo.isElevatorMode) {
                val (first, second) = nextPointWithElevator
                return "$first - $second"
            }
            return nextPointWithoutElevator
        }

    override fun getNextPointWithoutElevator(): String {
        if (isAbortTaskToProductionPoint)return PointContentUtils.getProductionPoint()
        return if (qrCodePointsPairList == null || currentPairIndex < 0 || currentPairIndex > qrCodePointsPairList!!.size - 1) {
            when (RobotInfo.modeQRCodeSetting.finishAction) {
                0 -> PointContentUtils.getProductionPoint()
                1 -> PointCacheInfo.chargePoint.second.name
                else -> ""
            }
        } else {
            val (first, second) = qrCodePointsPairList!![currentPairIndex]
            if (isArrivedFirstPoint) {
                second.second
            } else first.second
        }
    }

    override fun getNextPointWithElevator(): Pair<String, String> {
        if (isAbortTaskToProductionPoint)return PointContentUtils.getProductionPointWithMap()
        return if (qrCodePointsPairList.isNullOrEmpty() || currentPairIndex < 0 || currentPairIndex > qrCodePointsPairList!!.size - 1) {
            when (RobotInfo.modeQRCodeSetting.finishAction) {
                0 -> PointContentUtils.getProductionPointWithMap()
                1 -> PointCacheInfo.chargePoint.run { Pair(this.first, this.second.name) }
                else -> Pair("", "")
            }
        } else {
            val (first, second) = qrCodePointsPairList!![currentPairIndex]
            if (isArrivedFirstPoint) {
                second
            } else {
                first
            }
        }
    }

    override fun arrivedPoint(pointName: String) {
        arrivedPointSize++
        isArrivedFirstPoint = if (isArrivedFirstPoint) {
            currentPairIndex++
            false
        } else {
            true
        }
        CallingInfo.heartBeatInfo.currentTask!!.targetPoint = nextPoint
    }

    override fun hasNext(): Boolean {
        return !qrCodePointsPairList.isNullOrEmpty() && currentPairIndex != -1 && currentPairIndex < qrCodePointsPairList!!.size
    }

    override fun getDeliveryPointSize(): Int {
        return arrivedPointSize
    }

    override fun getSpeed(): String {
        return (if (hasNext()) RobotInfo.modeQRCodeSetting.speed else RobotInfo.returningSetting.gotoProductionPointSpeed).toString()
    }

    override fun withLiftFunction(): Boolean {
        return RobotInfo.modeQRCodeSetting.lift && RobotInfo.isLiftModelInstalled
    }

    override fun clearAllPoints() {
        currentPairIndex = -1
    }

    override fun setOrientationAndDistanceCalibration() {
        ROSController.agvMove(if (RobotInfo.modeQRCodeSetting.direction) -RobotInfo.modeQRCodeSetting.orientationAndDistanceCalibration else RobotInfo.modeQRCodeSetting.orientationAndDistanceCalibration)
    }

    override fun showReturnBtn(): Boolean {
        return !hasNext() && RobotInfo.modeQRCodeSetting.finishAction !=2
    }

    override fun isArrivedLastPointAndEndInPlace(): Boolean {
        return !hasNext() && RobotInfo.modeQRCodeSetting.finishAction == 2
    }

    override fun getTaskFinishAction(): Int {
        return RobotInfo.modeQRCodeSetting.finishAction
    }


    override fun setRobotWidthAndLidar(reset: Boolean) {
        val robotSize = if (RobotInfo.isSupportNewFootprint) {
            if (reset) {
                doubleArrayOf(0.0, 0.0, 0.0, 0.0)
            } else {
                RobotInfo.modeQRCodeSetting.robotSize.map { it.toDouble() }.toDoubleArray()
            }
        } else {
            if (reset) {
                doubleArrayOf(0.0, 0.0)
            } else {
                doubleArrayOf(
                    RobotInfo.modeQRCodeSetting.length.toDouble(),
                    RobotInfo.modeQRCodeSetting.width.toDouble()
                )
            }
        }
        ROSController.footprint(robotSize)
        if (reset) {
            ROSController.lidarMin(Constants.DEFAULT_QRCODE_MODE_LIDAR_WIDTH_WITHOUT_THING)
        } else {
            ROSController.lidarMin(RobotInfo.modeQRCodeSetting.lidarWidth)
        }
    }

    override fun resetAllParameter(robotInfo: RobotInfo, callingInfo: CallingInfo) {
        super.resetAllParameter(robotInfo, callingInfo)
        ROSController.agvMode(Constants.DEFAULT_QRCODE_MODE_DIRECTION)
        ROSController.agvMove(Constants.DEFAULT_ORIENTATION_AND_DISTANCE_CALIBRATION)
        ROSController.avoidObstacle(true)
        ROSController.setTolerance(1)
        Timber.w("将二维码模式相关配置恢复默认值")
    }

    override fun shouldRemoveFirstCallingTask(): Boolean {
        val firstTaskDetails = CallingInfo.getFirstCallingDetails()
        return firstTaskDetails != null && firstTaskDetails.key == token && firstTaskDetails.mode == TaskMode.MODE_QRCODE
    }

    override fun getAction(): String {
        if (isAbortTaskToProductionPoint)return TaskAction.production_point
        return if (hasNext()) {
            TaskAction.agv_point
        } else {
            RobotInfo.modeQRCodeSetting.finishAction.run {
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