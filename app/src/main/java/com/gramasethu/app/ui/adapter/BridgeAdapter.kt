package com.gramasethu.app.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.gramasethu.app.R
import com.gramasethu.app.databinding.ItemBridgeBinding
import com.gramasethu.app.model.Bridge
import com.gramasethu.app.model.BridgeStatus

class BridgeAdapter(private val onClick: (Bridge) -> Unit)
    : ListAdapter<Bridge, BridgeAdapter.VH>(object : DiffUtil.ItemCallback<Bridge>() {
        override fun areItemsTheSame(a: Bridge, b: Bridge) = a.id == b.id
        override fun areContentsTheSame(a: Bridge, b: Bridge) =
            a.status == b.status && a.lastUpdated == b.lastUpdated
    }) {

    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        VH(ItemBridgeBinding.inflate(LayoutInflater.from(p.context), p, false))

    override fun onBindViewHolder(h: VH, pos: Int) = h.bind(getItem(pos), onClick)

    class VH(private val b: ItemBridgeBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(bridge: Bridge, onClick: (Bridge) -> Unit) {
            b.textName.text     = bridge.name
            b.textLocation.text = "${bridge.village} · ${bridge.district}"
            b.textFreshness.text = bridge.freshnessLabel
            val ctx = b.root.context
            when (bridge.effectiveStatus) {
                BridgeStatus.OPEN -> {
                    b.statusBar.setBackgroundResource(R.drawable.bg_open)
                    b.textStatus.text = "OPEN"
                    b.textStatus.setTextColor(ctx.getColor(R.color.status_open))
                }
                BridgeStatus.DAMAGED -> {
                    b.statusBar.setBackgroundResource(R.drawable.bg_damaged)
                    b.textStatus.text = "DAMAGED"
                    b.textStatus.setTextColor(ctx.getColor(R.color.status_damaged))
                }
                BridgeStatus.SUBMERGED -> {
                    b.statusBar.setBackgroundResource(R.drawable.bg_submerged)
                    b.textStatus.text = "SUBMERGED"
                    b.textStatus.setTextColor(ctx.getColor(R.color.status_submerged))
                }
                BridgeStatus.UNKNOWN -> {
                    b.statusBar.setBackgroundResource(R.drawable.bg_unknown)
                    b.textStatus.text = "UNKNOWN"
                    b.textStatus.setTextColor(ctx.getColor(R.color.status_unknown))
                }
            }
            b.root.setOnClickListener { onClick(bridge) }
        }
    }
}
