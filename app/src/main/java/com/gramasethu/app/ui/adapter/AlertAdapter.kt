package com.gramasethu.app.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.gramasethu.app.R
import com.gramasethu.app.databinding.ItemAlertBinding
import com.gramasethu.app.model.Bridge
import com.gramasethu.app.model.BridgeStatus

class AlertAdapter : ListAdapter<Bridge, AlertAdapter.VH>(object : DiffUtil.ItemCallback<Bridge>() {
    override fun areItemsTheSame(a: Bridge, b: Bridge) = a.id == b.id
    override fun areContentsTheSame(a: Bridge, b: Bridge) = a.status == b.status
}) {
    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        VH(ItemAlertBinding.inflate(LayoutInflater.from(p.context), p, false))

    override fun onBindViewHolder(h: VH, pos: Int) = h.bind(getItem(pos))

    class VH(private val b: ItemAlertBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(bridge: Bridge) {
            b.textName.text     = bridge.name
            b.textLocation.text = "${bridge.village} · ${bridge.district}"
            b.textFreshness.text = bridge.freshnessLabel
            b.textRoute.text    = "🗺 ${bridge.alternativeRoute}"
            val ctx = b.root.context
            when (bridge.effectiveStatus) {
                BridgeStatus.SUBMERGED -> {
                    b.root.setCardBackgroundColor(ctx.getColor(R.color.alert_sub_bg))
                    b.textSeverity.text = "🚨 SUBMERGED"
                    b.textSeverity.setTextColor(ctx.getColor(R.color.status_submerged))
                }
                BridgeStatus.DAMAGED -> {
                    b.root.setCardBackgroundColor(ctx.getColor(R.color.alert_dmg_bg))
                    b.textSeverity.text = "⚡ DAMAGED"
                    b.textSeverity.setTextColor(ctx.getColor(R.color.status_damaged))
                }
                else -> {}
            }
        }
    }
}
