package com.reeman.agv.fragments.setting.strategy

import android.content.Context
import com.reeman.agv.widgets.ExpandableLayout
import com.reeman.commons.event.AGVTagPoseEvent

interface LayoutInitializer {
    fun initLayout(context: Context, root: ExpandableLayout)

    fun onAGVTagPoseEvent(event: AGVTagPoseEvent){

    }
}