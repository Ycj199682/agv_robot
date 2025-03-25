package com.reeman.agv.fragments.setting

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import com.reeman.agv.R
import com.reeman.agv.base.BaseFragment
import com.reeman.agv.fragments.setting.strategy.*
import com.reeman.commons.event.AGVTagPoseEvent
import com.reeman.agv.widgets.ExpandableLayout
import com.reeman.commons.state.RobotInfo
import java.util.*

class ModeSettingFragment : BaseFragment(), ExpandableLayout.OnExpandListener {

    private lateinit var elNormalMode: ExpandableLayout
    private lateinit var elRouteMode: ExpandableLayout
    private lateinit var elQRCodeMode: ExpandableLayout
    private lateinit var elCallingMode: ExpandableLayout
    private lateinit var list: List<ExpandableLayout>
    private val layoutInitializerMap: MutableMap<ExpandableLayout, LayoutInitializer> = HashMap()

    override fun getLayoutRes(): Int {
        return R.layout.fragment_mode_setting
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        elNormalMode = findView(R.id.el_normal_mode)
        elRouteMode = findView(R.id.el_route_mode)
        elQRCodeMode = findView(R.id.el_qrcode_mode)
        elCallingMode = findView(R.id.el_calling_mode)

        elNormalMode.setOnExpandListener(this)
        elRouteMode.setOnExpandListener(this)
        elQRCodeMode.setOnExpandListener(this)
        elCallingMode.setOnExpandListener(this)

        layoutInitializerMap[elNormalMode] = NormalModelInitializer()
        layoutInitializerMap[elQRCodeMode] = QRCodeModelInitializer()
        layoutInitializerMap[elRouteMode] = RouteModelInitializer()
        layoutInitializerMap[elCallingMode] = CallingModelInitializer()

        list = listOf(elNormalMode, elRouteMode, elQRCodeMode, elCallingMode)

        if (!RobotInfo.isSpaceShip() || RobotInfo.robotType == 6) {
            elQRCodeMode.visibility = View.GONE
        }
    }

    override fun onExpand(expandableLayout: ExpandableLayout, isExpand: Boolean) {
        val ibExpandIndicator = expandableLayout.headerLayout.findViewById<ImageButton>(R.id.ib_expand_indicator)
        ibExpandIndicator.animate().rotation(if (isExpand) 90f else 0f).setDuration(200).start()

        for (layout in list) {
            if (layout != expandableLayout && layout.isOpened) {
                layout.hide()
            }
        }
        layoutInitializerMap[expandableLayout]?.initLayout(requireActivity(), expandableLayout)
    }

    override fun onAGVTagPoseEvent(event: AGVTagPoseEvent) {
        layoutInitializerMap[elQRCodeMode]?.onAGVTagPoseEvent(event)
    }
}
