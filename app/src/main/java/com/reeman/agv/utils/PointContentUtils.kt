package com.reeman.agv.utils

import android.content.Context
import com.reeman.agv.R
import com.reeman.commons.state.RobotInfo
import com.reeman.points.model.custom.GenericPoint
import com.reeman.points.utils.PointCacheInfo

object PointContentUtils {
    fun getTypeShowContent(context: Context, type: String) =
        when (type) {
            GenericPoint.AGV_TAG -> context.getString(R.string.text_qr_code_type)
            GenericPoint.PRODUCT -> context.getString(R.string.text_product_point)
            GenericPoint.DELIVERY -> context.getString(R.string.text_delivery_type)
            else -> context.getString(R.string.text_unknown_type)
        }

    fun getProductionPoint(): String {
        val productionPoints = PointCacheInfo.productionPoints
        return RobotInfo.returningSetting.run {
            if (this.productionPointSetting == 0) {
                productionPoints.first().second.name
            } else {
                if (this.defaultProductionPoint.isNotBlank()) {
                    productionPoints.find { it.second.name == this.defaultProductionPoint }
                        ?.run {
                            this.second.name
                        }
                        ?: PointCacheInfo.getNearestProductionPoint(RobotInfo.currentPosition!!).second.name
                } else {
                    PointCacheInfo.getNearestProductionPoint(RobotInfo.currentPosition!!).second.name
                }
            }
        }
    }

    fun getProductionPointWithMap() = RobotInfo.returningSetting.run {
        val productionPointMapAlias = RobotInfo.elevatorSetting.productionPointMap.first
        val productionPoints = PointCacheInfo.productionPoints
        if (this.productionPointSetting == 0) {
            productionPoints.first().run { Pair(productionPointMapAlias, this.second.name) }
        } else {
            val position = if (productionPointMapAlias == RobotInfo.currentMapEvent.alias){
                RobotInfo.currentPosition!!
            }else{
                PointCacheInfo.getPointByAliasAndPointType(productionPointMapAlias,GenericPoint.LEAVE_ELEVATOR).position
            }
            if (this.defaultProductionPoint.isNotBlank()) {
                productionPoints.find { it.second.name == this.defaultProductionPoint }
                    ?.run {
                        Pair(productionPointMapAlias, this.second.name)
                    }
                    ?: PointCacheInfo.getNearestProductionPoint(position)
                        .run {
                            Pair(productionPointMapAlias, this.second.name)
                        }
            } else {
                PointCacheInfo.getNearestProductionPoint(position)
                    .run {
                        Pair(productionPointMapAlias, this.second.name)
                    }
            }
        }
    }
}