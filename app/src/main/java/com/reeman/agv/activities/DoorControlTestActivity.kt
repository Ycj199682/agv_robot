package com.reeman.agv.activities

import android.app.Dialog
import android.text.InputFilter
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import com.reeman.agv.R
import com.reeman.agv.base.BaseActivity
import com.reeman.agv.calling.button.CallingHelper
import com.reeman.agv.controller.DoorController
import com.reeman.agv.utils.DebounceClickListener
import com.reeman.agv.widgets.EasyDialog
import com.reeman.commons.event.CallingModelDisconnectedEvent
import com.reeman.commons.event.CallingModelReconnectSuccessEvent
import com.reeman.commons.event.CloseDoorFailedEvent
import com.reeman.commons.event.DoorClosedEvent
import com.reeman.commons.event.DoorNumSettingResultEvent
import com.reeman.commons.event.DoorOpenedEvent
import com.reeman.commons.event.OpenDoorFailedEvent
import com.reeman.commons.event.SetDoorNumFailedEvent
import com.reeman.commons.eventbus.EventBus
import com.reeman.commons.exceptions.ReconnectUsbDeviceTimeoutException
import com.reeman.commons.state.RobotInfo
import com.reeman.commons.utils.TimeUtil
import kotlinx.coroutines.Job
import timber.log.Timber

class DoorControlTestActivity : BaseActivity(), DoorController.OnAccessControlListener,DebounceClickListener,
        (View) -> Unit {

    private lateinit var etDoorNum: AppCompatEditText
    private lateinit var tvData: TextView
    private var doorController: DoorController? = null
    override fun getLayoutRes() =
        R.layout.activity_door_control_test


    override fun initCustomView() {
        etDoorNum = `$`(R.id.et_door_num)
        tvData = `$`(R.id.tv_data)
        `$`<AppCompatButton>(R.id.btn_open_door).setDebounceClickListener(this)
        `$`<AppCompatButton>(R.id.btn_close_door).setDebounceClickListener(this)
        `$`<AppCompatButton>(R.id.btn_set_door_num).setDebounceClickListener(this)
        `$`<AppCompatButton>(R.id.btn_exit).setDebounceClickListener(this)
        `$`<TextView>(R.id.tv_clean).setDebounceClickListener(this)
        tvData.movementMethod = ScrollingMovementMethod.getInstance()
        etDoorNum.filters = arrayOf(InputFilter.LengthFilter(4),
            InputFilter { source, start, end, dest, dstart, dend ->
                if (source.isEmpty()) {
                    return@InputFilter null
                }
                try {
                    val input = Integer.parseInt(dest.toString() + source.toString())
                    if (input <= 0) {
                        return@InputFilter ""
                    }
                } catch (nfe: NumberFormatException) {
                    return@InputFilter ""
                }
                null
            }
        )
    }

    override fun onResume() {
        super.onResume()
        if (RobotInfo.doorControlSetting.communicationMethod == 1) {
            `$`<AppCompatButton>(R.id.btn_set_door_num).visibility = View.VISIBLE
            EventBus.registerObserver(this, object : EventBus.EventObserver<DoorOpenedEvent> {
                override fun onEvent(event: DoorOpenedEvent) {
                    onOpenDoorSuccess(event)
                }
            })
            EventBus.registerObserver(this, object : EventBus.EventObserver<DoorClosedEvent> {
                override fun onEvent(event: DoorClosedEvent) {
                    onCloseDoorSuccess(event)
                }
            })
            EventBus.registerObserver(
                this,
                object : EventBus.EventObserver<DoorNumSettingResultEvent> {
                    override fun onEvent(event: DoorNumSettingResultEvent) {
                        onSetDoorNumSuccess(event)
                    }
                })
            EventBus.registerObserver(this, object : EventBus.EventObserver<OpenDoorFailedEvent> {
                override fun onEvent(event: OpenDoorFailedEvent) {
                    onOpenDoorFailed(event)
                }
            })
            EventBus.registerObserver(this, object : EventBus.EventObserver<CloseDoorFailedEvent> {
                override fun onEvent(event: CloseDoorFailedEvent) {
                    onCloseDoorFailed(event)
                }
            })
            EventBus.registerObserver(this, object : EventBus.EventObserver<SetDoorNumFailedEvent> {
                override fun onEvent(event: SetDoorNumFailedEvent) {
                    onSetDoorNumFailed(event)
                }
            })
            return
        }
        try {
            doorController = DoorController.createInstance()
            doorController!!.init(this)
        } catch (e: Exception) {
            Timber.w(e, "打开门控串口失败")
            EasyDialog.getInstance(this)
                .warn(getString(R.string.text_open_serial_device_failed)) { dialog: Dialog, id: Int ->
                    dialog.dismiss()
                    finish()
                }
        }
    }

    override fun onPause() {
        super.onPause()
        doorController?.unInit()
        if (RobotInfo.doorControlSetting.communicationMethod == 1) {
            CallingHelper.doorState = CallingHelper.INIT
        }
    }

    private fun updateLogData(newData: String) {
        val data: String = tvData.text.toString()
        tvData.text = String.format("%s\n%s %s", data, TimeUtil.formatMilliseconds(), newData)
        val offset: Int = tvData.lineCount * tvData.lineHeight
        if (offset > tvData.height) {
            tvData.scrollTo(0, offset - tvData.height + 20)
        }
    }

    override fun onOpenDoorSuccess() {
        if (doorController!!.currentState == DoorController.State.OPENED) return
        doorController!!.currentState = DoorController.State.OPENED
        runOnUiThread { updateLogData(getString(R.string.text_open_door_test_success)) }
    }

    override fun onCloseDoorSuccess(currentClosingDoor: String) {
        if (doorController!!.currentState == DoorController.State.CLOSED) return
        doorController!!.currentState = DoorController.State.CLOSED
        runOnUiThread { updateLogData(getString(R.string.text_close_door_test_success)) }
    }

    override fun onThrowable(isOpenDoor: Boolean, throwable: Throwable) {
        val content = when {
            throwable is ReconnectUsbDeviceTimeoutException -> getString(R.string.text_door_control_serial_port_disconnect)
            isOpenDoor -> getString(R.string.exception_open_door_failed)
            else -> getString(R.string.exception_close_door_failed)
        }
        runOnUiThread { updateLogData(content) }
    }

    override fun onCallingModelDisconnectEvent(event: CallingModelDisconnectedEvent) {
        val content = getString(
            if (event.event == 0) {
                R.string.text_door_control_model_reconnecting
            } else {
                R.string.text_door_control_model_reconnect_failed
            }
        )
        updateLogData(content)
    }

    override fun onCallingModelReconnectedEvent(event: CallingModelReconnectSuccessEvent) {
        updateLogData(getString(R.string.text_door_control_model_reconnect_success))
    }

    fun onOpenDoorSuccess(event: DoorOpenedEvent) {
        if (CallingHelper.doorState == CallingHelper.OPENED) return
        CallingHelper.doorState = CallingHelper.OPENED
        updateLogData(getString(R.string.text_open_door_test_success))
    }

    fun onOpenDoorFailed(event: OpenDoorFailedEvent) {
        updateLogData(getString(R.string.text_send_open_door_order_failed))
    }

    fun onCloseDoorSuccess(event: DoorClosedEvent) {
        if (CallingHelper.doorState == CallingHelper.CLOSED) return
        CallingHelper.doorState = CallingHelper.CLOSED
        updateLogData(getString(R.string.text_close_door_test_success))
    }

    fun onCloseDoorFailed(event: CloseDoorFailedEvent) {
        updateLogData(getString(R.string.text_send_close_door_order_failed))
    }

    fun onSetDoorNumSuccess(event: DoorNumSettingResultEvent) {
        updateLogData(getString(R.string.text_set_door_num_success))
    }

    fun onSetDoorNumFailed(event: SetDoorNumFailedEvent) {
        updateLogData(getString(R.string.text_send_set_door_num_order_failed))
    }

    override fun invoke(v: View) {
        fun getDoorNum(): Int? {
            val doorNumStr = etDoorNum.text.toString()
            if (doorNumStr.isBlank() || doorNumStr.length != 4) {
                etDoorNum.error = getString(R.string.text_please_input_door_num)
                return null
            }
            val doorNum = doorNumStr.toIntOrNull()
            if (doorNum == null) {
                etDoorNum.setText("")
                etDoorNum.error = getString(R.string.text_please_input_door_num)
                return null
            }
            return doorNum
        }
        when (v.id) {
            R.id.btn_open_door -> {
                getDoorNum()?.let {
                    if (if (RobotInfo.doorControlSetting.communicationMethod == 0) {
                            doorController!!.openDoor(it.toString(), false)
                        } else {
                            CallingHelper.openDoor(it.toString())
                        }
                    ) {
                        updateLogData(getString(R.string.text_open_door_test_cmd_send_success, it))
                    }
                }
            }

            R.id.btn_close_door -> {
                getDoorNum()?.let {
                    if (if (RobotInfo.doorControlSetting.communicationMethod == 0) {
                            doorController!!.closeDoor(it.toString(), false)
                        } else {
                            CallingHelper.closeDoor(it.toString())
                        }
                    ) {
                        updateLogData(getString(R.string.text_close_door_test_cmd_send_success, it))
                    }
                }
            }

            R.id.btn_set_door_num -> {
                getDoorNum()?.let {
                    EasyDialog.getInstance(this)
                        .confirm(getString(R.string.text_please_confirm_control_model_is_in_setting_mode)) { dialog, mId ->
                            dialog.dismiss()
                            if (mId == R.id.btn_confirm) {
                                CallingHelper.setDoorNum(it.toString())
                                updateLogData(
                                    getString(
                                        R.string.text_set_door_num_cmd_send_success,
                                        it
                                    )
                                )
                            }
                        }
                }
            }

            R.id.btn_exit -> {
                finish()
            }

            R.id.tv_clean -> {
                tvData.text = ""
                tvData.scrollTo(0, 0)

            }
        }
    }
}