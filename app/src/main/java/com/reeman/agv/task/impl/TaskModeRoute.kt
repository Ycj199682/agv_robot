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
import com.reeman.commons.utils.TimeUtil
import com.reeman.dao.repository.entities.PointsVO
import com.reeman.dao.repository.entities.RouteWithPoints
import com.reeman.points.model.custom.GenericPoint
import com.reeman.points.utils.PointCacheInfo
import com.reeman.ros.ROSController
import timber.log.Timber

class TaskModeRoute(private val gson: Gson) : Task {
    private lateinit var route: RouteWithPoints
    private var arrivedPointSize = 0
    private var targetPoints: List<PointsVO>? = null
    private var targetPointListIndex = 0
    private var routeStr: String? = null
    private var token: String? = null
    private var isTest = false
    private var isAbortTaskToProductionPoint = false

    override fun initTask(intent: Intent) {
        route = intent.getSerializableExtra(Constants.TASK_TARGET) as RouteWithPoints
        isTest = intent.getBooleanExtra(Constants.TASK_TEST, false)
        if (route.isExecuteAgainSwitch && !isTest) {
            routeStr = Gson().toJson(route)
        }
        if (intent.hasExtra(Constants.TASK_TOKEN)) {
            token = intent.getStringExtra(Constants.TASK_TOKEN)
        }
        targetPoints = route.pointsVOList
        //暂时使用二维码模式的设置
        if (targetPoints?.find { it.pointType == GenericPoint.AGV_TAG } != null) {
            if (RobotInfo.isLiftModelInstalled) {
                ROSController.ioControl(4)
                ROSController.setTolerance(if (RobotInfo.modeQRCodeSetting.stopNearby) 1 else 0)
            } else {
                ROSController.setTolerance(if (RobotInfo.returningSetting.stopNearBy) 1 else 0)
            }
            ROSController.avoidObstacle(RobotInfo.modeQRCodeSetting.rotate)
            ROSController.agvMode(if (RobotInfo.modeQRCodeSetting.direction) 1 else 0)
            Timber.w("路线模式中有二维码类型的点位,使用二维码模式的设置: ${RobotInfo.modeQRCodeSetting}")
        }
        Timber.w("路线模式配送配置 : ${RobotInfo.modeRouteSetting} \n点位信息 : $route \n返航配置 : ${RobotInfo.returningSetting}")
        val currentTimeMillis = System.currentTimeMillis()
        CallingInfo.heartBeatInfo.currentTask = TaskInfo(
            currentTimeMillis,
            currentTimeMillis,
            TaskMode.MODE_ROUTE.ordinal,
            token,
            nextPointWithoutElevator
        )
        ROSController.setTolerance(if (RobotInfo.returningSetting.stopNearBy) 1 else 0)
    }

    override fun isExecuteAgainSwitch(): Boolean {
        return route.isExecuteAgainSwitch
    }

    override fun getNextPointWithoutElevator(): String {
        if (isAbortTaskToProductionPoint) return PointContentUtils.getProductionPoint()
        if (targetPoints == null || targetPoints!!.isEmpty() || targetPointListIndex >= targetPoints!!.size) {
            if (route.taskFinishAction == 0 || isTest) {
                return PointContentUtils.getProductionPoint()
            }
            if (route.taskFinishAction == 1) {
                return PointCacheInfo.chargePoint.second.name
            }
            if (RobotInfo.isLowPower()) {
                Timber.w("路线模式设置任务结束重新开始路线巡航时触发低电")
                return PointCacheInfo.chargePoint.second.name
            }
            val commutingTimeSetting = RobotInfo.commutingTimeSetting
            if (commutingTimeSetting.open && !TimeUtil.isCurrentInTimeScope(
                    commutingTimeSetting.workingTime,
                    commutingTimeSetting.afterWorkTime
                )
            ) {
                Timber.w("路线模式设置任务结束重新开始路线巡航时触发下班")
                return PointCacheInfo.chargePoint.second.name
            }
            targetPoints = route.pointsVOList
            targetPointListIndex = 0
        }
        return targetPoints!![targetPointListIndex].point
    }

    override fun arrivedPoint(pointName: String) {
        arrivedPointSize++
        targetPointListIndex++
        CallingInfo.heartBeatInfo.currentTask!!.targetPoint = nextPointWithoutElevator
    }

    override fun hasNext(): Boolean {
        val commutingTimeSetting = RobotInfo.commutingTimeSetting
        val afterWork = commutingTimeSetting.open && !TimeUtil.isCurrentInTimeScope(
            commutingTimeSetting.workingTime,
            commutingTimeSetting.afterWorkTime
        )
        return !targetPoints.isNullOrEmpty() && (targetPointListIndex < targetPoints!!.size || (route.taskFinishAction == 2 && !isTest)) && !RobotInfo.isLowPower() && !afterWork
    }

    override fun getDeliveryPointSize(): Int {
        return arrivedPointSize
    }

    override fun getSpeed(): String {
        if (!hasNext()) {
            if (route.taskFinishAction == 0 || isTest) {
                Timber.w("任务结束,使用返回出品点的速度")
                return RobotInfo.returningSetting.gotoProductionPointSpeed.toString()
            } else if (route.taskFinishAction == 1) {
                Timber.w("任务结束,使用返回充电桩的速度")
                return RobotInfo.returningSetting.gotoChargingPileSpeed.toString()
            } else {
                if (RobotInfo.isLowPower()) {
                    Timber.w("触发低电,使用返回充电桩的速度")
                    return RobotInfo.returningSetting.gotoChargingPileSpeed.toString()
                }
                val commutingTimeSetting = RobotInfo.commutingTimeSetting
                if (commutingTimeSetting.open && !TimeUtil.isCurrentInTimeScope(
                        commutingTimeSetting.workingTime,
                        commutingTimeSetting.afterWorkTime
                    )
                ) {
                    Timber.w("下班,使用返回充电桩的速度")
                    return RobotInfo.returningSetting.gotoChargingPileSpeed.toString()
                }
            }
        }
        return RobotInfo.modeRouteSetting.speed.toString()
    }

    override fun getRouteName(): String {
        return route.routeName
    }

    override fun showReturnBtn(): Boolean {
        return !hasNext() && taskFinishAction != 2
    }


    override fun getTaskFinishAction(): Int {
        if (isTest) {
            return 2
        }
        return if (route.taskFinishAction == 2) {
            return 3
        } else {
            route.taskFinishAction
        }
    }

    override fun showSkipCurrentTargetBtn(): Boolean {
        if (hasNext()) {
            val index = if (route.taskFinishAction == 2) {
                if (targetPointListIndex < targetPoints!!.size) {
                    targetPointListIndex
                } else {
                    if (isTest) {
                        -1
                    } else {
                        0
                    }
                }
            } else {
                targetPointListIndex
            }
            if (index != -1 && index < targetPoints!!.size - 1 && targetPoints!![index].liftAction == 0) {
                return true
            }
        }
        return false
    }

    override fun skipCurrentTarget() {
        targetPointListIndex++
    }

    override fun getCountDownTime(): Long {
        val pointsVO = if (targetPointListIndex >= targetPoints!!.size) {
            targetPoints!![targetPoints!!.size - 1]
        } else {
            targetPoints!![targetPointListIndex]
        }
        return if (pointsVO.isOpenWaitingTime) {
            pointsVO.waitingTime.toLong()
        } else 0
    }

    override fun getPlayArrivedTipTime(): Long {
        val index = if (targetPointListIndex == 0) {
            targetPoints!!.size - 1
        } else {
            targetPointListIndex - 1
        }
        val pointsVO = targetPoints!![index]
        return if (pointsVO.isOpenWaitingTime) {
            pointsVO.waitingTime.toLong()
        } else 0
    }

    override fun clearAllPoints() {
        targetPoints = null
    }

    override fun resetAllParameter(robotInfo: RobotInfo, callingInfo: CallingInfo) {
        super.resetAllParameter(robotInfo, callingInfo)
        ROSController.setTolerance(1)
        if (targetPoints?.find { it.pointType == GenericPoint.AGV_TAG } != null) {
            ROSController.agvMode(Constants.DEFAULT_QRCODE_MODE_DIRECTION)
            ROSController.agvMove(Constants.DEFAULT_ORIENTATION_AND_DISTANCE_CALIBRATION)
            ROSController.avoidObstacle(true)
        }
    }

    override fun getRouteTask(): RouteWithPoints? {
        return if (routeStr != null) Gson().fromJson(
            routeStr,
            RouteWithPoints::class.java
        ) else null
    }

    override fun shouldRemoveFirstCallingTask(): Boolean {
        val firstCallingDetails = CallingInfo.getFirstCallingDetails()
        Timber.w("队列中第一个任务 : $firstCallingDetails")
        return firstCallingDetails != null && firstCallingDetails.key == token && firstCallingDetails.mode == TaskMode.MODE_ROUTE
    }

    override fun getAction(): String {
        if (isAbortTaskToProductionPoint) return TaskAction.production_point
        if (targetPoints == null || targetPoints!!.isEmpty() || targetPointListIndex >= targetPoints!!.size) {
            if (route.taskFinishAction == 0 || isTest) {
                return TaskAction.production_point
            }
            if (route.taskFinishAction == 1) {
                return TaskAction.charge_point
            }
            if (RobotInfo.isLowPower()) {
                return TaskAction.charge_point
            }
            val commutingTimeSetting = RobotInfo.commutingTimeSetting
            if (commutingTimeSetting.open && !TimeUtil.isCurrentInTimeScope(
                    commutingTimeSetting.workingTime,
                    commutingTimeSetting.afterWorkTime
                )
            ) {
                return TaskAction.charge_point
            }
            targetPoints = route.pointsVOList
            targetPointListIndex = 0
        }
        return if (targetPoints!![targetPointListIndex].pointType == GenericPoint.AGV_TAG) {
            TaskAction.agv_point
        } else {
            TaskAction.route_point
        }
    }

    override fun withLiftFunction(): Boolean {
        val index = if (targetPointListIndex == 0) {
            targetPoints!!.size - 1
        } else {
            targetPointListIndex - 1
        }
        val pointsVO = targetPoints!![index]
        return pointsVO.liftAction != 0
    }

    override fun isOpenedManualLiftUpControl(isArrived: Boolean): Boolean {
        if (isArrived) {
            if (targetPoints.isNullOrEmpty()) return false
            val index = if (targetPointListIndex == 0) {
                targetPoints!!.size - 1
            } else {
                targetPointListIndex - 1
            }
            return targetPoints!![index].liftAction == 3
        }
        if (!hasNext()) return false
        return targetPoints?.run {
            val pointsVO = this[targetPointListIndex]
            return pointsVO.liftAction == 3
        } ?: false
    }

    override fun isOpenedManualLiftDownControl(isArrived: Boolean): Boolean {
        if (isArrived) {
            if (targetPoints.isNullOrEmpty()) return false
            val index = if (targetPointListIndex == 0) {
                targetPoints!!.size - 1
            } else {
                targetPointListIndex - 1
            }
            return targetPoints!![index].liftAction == 4
        }
        if (!hasNext()) return false
        return targetPoints?.run {
            val pointsVO = this[targetPointListIndex]
            pointsVO.liftAction == 4
        } ?: false
    }

    //暂时使用二维码模式的设置
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

    //暂时使用二维码模式的设置
    override fun setOrientationAndDistanceCalibration() {
        ROSController.agvMove(if (RobotInfo.modeQRCodeSetting.direction) -RobotInfo.modeQRCodeSetting.orientationAndDistanceCalibration else RobotInfo.modeQRCodeSetting.orientationAndDistanceCalibration)
    }

    override fun setAbortTaskToProductionPoint(isAbortTaskToProductionPoint: Boolean) {
        this.isAbortTaskToProductionPoint = isAbortTaskToProductionPoint
        if (isAbortTaskToProductionPoint) {
            routeStr = null
        }
    }

    override fun isAbortTaskToProductionPoint() = isAbortTaskToProductionPoint


}