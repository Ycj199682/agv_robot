package com.reeman.agv.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.reeman.agv.R
import com.reeman.points.model.custom.GenericPoint

class TypeGroupAdapter(
    private val mapList: List<Pair<String, List<GenericPoint>>>,
    private val listener: OnTypeGroupItemClickListener
) : RecyclerView.Adapter<TypeGroupAdapter.TableGroupViewHolder>() {
    private var index = 0
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TableGroupViewHolder {
        val root = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_page_item, parent, false)
        return TableGroupViewHolder(root)
    }

    override fun onBindViewHolder(holder: TableGroupViewHolder, position: Int) {}
    override fun onBindViewHolder(
        holder: TableGroupViewHolder,
        position: Int,
        payloads: List<Any>
    ) {
        val context = holder.itemView.context
        if (index == position) {
            holder.tvTableGroup.background = ResourcesCompat.getDrawable(
                context.resources,
                R.drawable.bg_config_button_active,
                context.theme
            )
        } else {
            holder.tvTableGroup.background = ResourcesCompat.getDrawable(
                context.resources,
                R.drawable.bg_config_button_inactive,
                context.theme
            )
        }
        val (first, second) = mapList[position]
        holder.tvTableGroup.text = first
        holder.tvTableGroup.setOnClickListener { v: View? ->
            val oldIndex = index
            index = position
            notifyItemChanged(oldIndex)
            notifyItemChanged(index)
            listener.onTypeGroupItemClick(second)
        }
    }

    override fun getItemCount(): Int {
        return mapList.size
    }

    class TableGroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tvTableGroup: TextView

        init {
            tvTableGroup = itemView.findViewById(R.id.tv_page)
        }
    }

    interface OnTypeGroupItemClickListener {
        fun onTypeGroupItemClick(points: List<GenericPoint>)
    }
}