package com.reeman.agv.adapter

import android.graphics.Color
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.reeman.agv.R
import com.reeman.agv.widgets.TableNumberView
import com.reeman.commons.utils.ClickRestrict
import com.reeman.points.model.custom.GenericPoint

class GenericPointsAdapter : BaseAdapter {
    private var allPoints: List<GenericPoint>
    private var selectedPoints: Pair<String, GenericPoint>?
    private var currentMap = ""

    fun getSelectPoints() = selectedPoints
    constructor(allPoints: List<GenericPoint>, selectedPoint: Pair<String, GenericPoint>?) {
        this.allPoints = allPoints
        selectedPoints = selectedPoint
    }

    constructor(
        allPoints: List<GenericPoint>,
        selectedPoint: Pair<String, GenericPoint>?,
        currentMap: String
    ) {
        this.allPoints = allPoints
        selectedPoints = selectedPoint
        this.currentMap = currentMap
    }

    fun updateData(
        allPoints: List<GenericPoint>,
        selectedPoint: Pair<String, GenericPoint>?,
        currentMap: String
    ) {
        this.allPoints = allPoints
        selectedPoints = selectedPoint
        this.currentMap = currentMap
        notifyDataSetChanged()
    }

    override fun getCount(): Int {
        return allPoints.size
    }

    override fun getItem(position: Int): Any {
        return allPoints[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val point = allPoints[position]
        val tableNumberView: TableNumberView
        if (convertView == null) {
            tableNumberView = TableNumberView(parent.context)
            tableNumberView.setOnClickListener { v: View ->
                if (ClickRestrict.restrictFrequency(300)) return@setOnClickListener
                val currentPoint = v.tag as GenericPoint
                if (isPointSelected(currentPoint)) {
                    setPointUnselected()
                    notifyDataSetChanged()
                } else {
                    selectedPoints = Pair(currentMap, currentPoint)
                    notifyDataSetChanged()
                }
            }
        } else {
            tableNumberView = convertView as TableNumberView
        }
        val isSelected = isPointSelected(point)
        tableNumberView.select(isSelected)
        if (isSelected) {
            tableNumberView.setTextColor(Color.WHITE)
            tableNumberView.setBackgroundResource(R.drawable.bg_table_number_selected)
        } else {
            tableNumberView.setTextColor(Color.parseColor("#FF666666"))
            tableNumberView.setBackgroundResource(R.drawable.bg_table_number_normal)
        }
        tableNumberView.text = point.name
        tableNumberView.tag = point
        if (point.name.length > 10) {
            tableNumberView.setTextSize(12f)
        } else {
            tableNumberView.setTextSize(16f)
        }
        return tableNumberView
    }

    private fun isPointSelected(point: GenericPoint): Boolean {
        if (selectedPoints == null) return false
        return if (TextUtils.isEmpty(currentMap)) {
            selectedPoints!!.second.name == point.name
        } else {
            selectedPoints!!.second.name == point.name && currentMap == selectedPoints!!.first
        }
    }

    private fun setPointUnselected() {
        selectedPoints = null
    }
}