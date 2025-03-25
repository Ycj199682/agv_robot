package com.reeman.agv.task.impl

import android.content.Intent
import com.google.gson.Gson
import com.reeman.agv.calling.CallingInfo
import com.reeman.agv.calling.event.CallingTaskEvent
import com.reeman.agv.calling.model.TaskInfo
import com.reeman.agv.task.Task
import com.reeman.commons.constants.Constants
import com.reeman.commons.state.RobotInfo
import com.reeman.commons.state.TaskMode
import com.reeman.ros.ROSController
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import timber.log.Timber

class TaskModeCalling(private val gson: Gson) : Task {
    private lateinit var callingTaskEvent: CallingTaskEvent
    override fun initTask(intent: Intent) {
        val callingTaskInfo = intent.getStringExtra(Constants.TASK_TARGET)
        callingTaskEvent =Json.decodeFromString(callingTaskInfo!!)
        Timber.w("呼叫模式配送配置 : ${CallingInfo.callingModeSetting} \n 点位信息 : $callingTaskInfo")
        val taskDetails = CallingInfo.getTaskDetailsByToken(callingTaskEvent.token)
        if (taskDetails != null) {
            val point = callingTaskEvent.point
            var target = point.second
            if (point.first.isNotBlank()) {
                target = point.first + " - " + point.second
            }
            CallingInfo.heartBeatInfo.currentTask = TaskInfo(
                taskDetails.firstCallingTime,
                System.currentTimeMillis(),
                TaskMode.MODE_CALLING.ordinal,
                callingTaskEvent.token,
                target
            )
        }
        ROSController.setTolerance(if (RobotInfo.returningSetting.stopNearBy) 1 else 0)
    }

    override fun getNextPointWithElevator(): Pair<String, String> {
        return callingTaskEvent.point
    }

    override fun getNextPointWithoutElevator(): String {
        return callingTaskEvent.point.second
    }

    override fun hasNext(): Boolean {
        return false
    }

    override fun getDeliveryPointSize(): Int {
        return 1
    }

    override fun getSpeed(): String {
        return CallingInfo.callingModeSetting.speed.toString()
    }

    override fun getCountDownTime(): Long {
        return CallingInfo.callingModeSetting.waitingTime.toLong()
    }

    override fun showReturnBtn(): Boolean {
        return false
    }

    override fun resetAllParameter(robotInfo: RobotInfo, callingInfo: CallingInfo) {
        super.resetAllParameter(robotInfo, callingInfo)
        ROSController.setTolerance(1)
    }

    override fun shouldRemoveFirstCallingTask(): Boolean {
        return true
    }
}