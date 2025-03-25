package com.reeman.agv.calling.event

import com.reeman.agv.calling.model.TaskDetails
import com.reeman.agv.calling.model.QRCodeModeTaskModel
import com.reeman.commons.state.TaskMode
import com.reeman.dao.repository.entities.RouteWithPoints
import kotlinx.serialization.Serializable

class CallingEvent

class CallingButtonEvent(val key: String, val map: String, val point: String)
class QRCodeButtonEvent(
    val key: String,
    val qrCodeModeTaskModelPairList: List<Pair<QRCodeModeTaskModel, QRCodeModeTaskModel>>
)

class UnboundButtonEvent(val key: String)
class MqttConnectionEvent(val isConnected: Boolean)
class CallingTaskQueueUpdateEvent(val taskDetailsList: List<TaskDetails>)
class StartTaskCountDownEvent(val token: String, val mode: TaskMode, val code: Int)

sealed class TaskEvent

@Serializable
class CallingTaskEvent(val isButtonTask:Boolean,val token: String,val point:Pair<String,String>):TaskEvent(){
    override fun toString(): String {
        return "CallingTaskEvent(isButtonTask=$isButtonTask, token='$token', point=$point)"
    }
}

class NormalTaskEvent(val token: String, val pointList: List<Pair<String, String>>):TaskEvent(){
    override fun toString(): String {
        return "NormalTaskEvent(token='$token', pointList=$pointList)"
    }
}

class RouteTaskEvent(val token: String, val route: RouteWithPoints):TaskEvent(){
    override fun toString(): String {
        return "RouteTaskEvent(token='$token', route=$route)"
    }
}
class QRCodeTaskEvent(
    val isButtonTask:Boolean,
    val token: String,
    val qrCodePointPairList: List<Pair<Pair<String, String>, Pair<String, String>>>
):TaskEvent(){
    override fun toString(): String {
        return "QRCodeTaskEvent(isButtonTask=$isButtonTask, token='$token', qrCodePointPairList=$qrCodePointPairList)"
    }
}

class ChargeTaskEvent(val token:String,val point:String):TaskEvent(){
    override fun toString(): String {
        return "ChargeTaskEvent(token='$token', point='$point')"
    }
}

class ReturnTaskEvent(val token:String,val point:String):TaskEvent(){
    override fun toString(): String {
        return "ReturnTaskEvent(token='$token', point='$point')"
    }
}