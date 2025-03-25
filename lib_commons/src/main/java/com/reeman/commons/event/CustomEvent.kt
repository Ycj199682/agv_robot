package com.reeman.commons.event

import android.content.Intent

class CustomEvent
class TimeStampEvent(val time: Long)

class AndroidNetWorkEvent(val intent: Intent)

class GreenButtonEvent
class DoorOpenedEvent(val number: Int)
class DoorClosedEvent(val number: Int)
class DoorNumSettingResultEvent(val number: Int)
class OpenDoorFailedEvent
class CloseDoorFailedEvent
class SetDoorNumFailedEvent

/**
 * @param event 0:断开连接,1:重连失败
 */
class CallingModelDisconnectedEvent(val event: Int)

class CallingModelReconnectSuccessEvent