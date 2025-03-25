package com.reeman.agv.widgets

import android.content.Context
import android.content.DialogInterface
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.widget.GridView
import com.reeman.agv.R
import com.reeman.agv.adapter.PointsAdapter

class CallingTaskChoosePointDialog(
    context: Context,
    allPoints: List<String>,
    selectedPoint: Pair<String?, String>?,
    listener: OnPointChooseResultListener
) : BaseDialog(context) {
    private var pointsAdapter: PointsAdapter?

    init {
        val root = LayoutInflater.from(context).inflate(R.layout.layout_dialog_choose_point, null)
        root.findViewById<View>(R.id.layout_point_type).visibility = View.GONE
        val gvPalletPoints = root.findViewById<GridView>(R.id.gv_points)
        pointsAdapter = PointsAdapter(allPoints, selectedPoint)
        gvPalletPoints.numColumns = 5
        gvPalletPoints.horizontalSpacing = 10
        gvPalletPoints.verticalSpacing = 5
        gvPalletPoints.adapter = pointsAdapter
        root.findViewById<View>(R.id.btn_confirm).setOnClickListener {
            dismiss()
            listener.onPointChooseResult(pointsAdapter?.selectedPoints)
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

    override fun setOnDismissListener(listener: DialogInterface.OnDismissListener?) {
        super.setOnDismissListener(listener)
        if (pointsAdapter != null) {
            pointsAdapter = null
        }
    }

    interface OnPointChooseResultListener {
        fun onPointChooseResult(points: Pair<String?, String?>?)
    }
}