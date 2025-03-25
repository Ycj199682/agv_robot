package com.reeman.agv.widgets

import android.content.Context
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.widget.GridView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.reeman.agv.R
import com.reeman.agv.adapter.GenericPointsAdapter
import com.reeman.agv.adapter.TypeGroupAdapter
import com.reeman.points.model.custom.GenericPoint

class PointChooseDialog(
    context: Context,
    allPoints: List<Pair<String, List<GenericPoint>>>,
    selectedPoint: GenericPoint?,
    listener: OnPointChooseResultListener
) : BaseDialog(context) {
    init {
        val root = LayoutInflater.from(context).inflate(R.layout.layout_dialog_choose_point, null)
        val rvPointTypes = root.findViewById<RecyclerView>(R.id.rv_point_type_list)
        val gvPalletPoints = root.findViewById<GridView>(R.id.gv_points)
        val pointsAdapter =
            GenericPointsAdapter(
                allPoints.first().second,
                if (selectedPoint != null) Pair("", selectedPoint) else null
            )

        gvPalletPoints.numColumns = 5
        gvPalletPoints.horizontalSpacing = 10
        gvPalletPoints.verticalSpacing = 5
        gvPalletPoints.adapter = pointsAdapter
        val typeGroupAdapter =
            TypeGroupAdapter(allPoints, object : TypeGroupAdapter.OnTypeGroupItemClickListener {
                override fun onTypeGroupItemClick(points: List<GenericPoint>) {
                    pointsAdapter.updateData(points,pointsAdapter.getSelectPoints(),"")
                }
            })
        rvPointTypes.adapter = typeGroupAdapter
        rvPointTypes.layoutManager = LinearLayoutManager(context)
        root.findViewById<View>(R.id.btn_confirm).setOnClickListener {
            dismiss()
            pointsAdapter.getSelectPoints()?.let {
                listener.onPointChooseResult(it)
            }
        }
        setContentView(root)
        val window = window
        val params = window!!.attributes
        val displayMetrics = DisplayMetrics()
        window.windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        params.width = (screenWidth * 0.8).toInt()
        params.height = (screenHeight * 0.8).toInt()
        window.attributes = params
    }

    interface OnPointChooseResultListener {
        fun onPointChooseResult(points: Pair<String, GenericPoint>)
    }
}