package com.gramasethu.app.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.gramasethu.app.databinding.FragmentAlertsBinding
import com.gramasethu.app.model.BridgeStatus
import com.gramasethu.app.ui.adapter.AlertAdapter
import com.gramasethu.app.viewmodel.BridgeViewModel

class AlertsFragment : Fragment() {

    private var _b: FragmentAlertsBinding? = null
    private val b get() = _b!!
    private val vm: BridgeViewModel by activityViewModels()

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentAlertsBinding.inflate(i, c, false); return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = AlertAdapter()
        b.recyclerAlerts.layoutManager = LinearLayoutManager(requireContext())
        b.recyclerAlerts.adapter = adapter

        vm.allBridges.observe(viewLifecycleOwner) { list ->
            val alerts = list.filter {
                it.effectiveStatus == BridgeStatus.SUBMERGED ||
                it.effectiveStatus == BridgeStatus.DAMAGED
            }
            adapter.submitList(alerts)
            b.layoutNoAlerts.visibility = if (alerts.isEmpty()) View.VISIBLE else View.GONE
            b.recyclerAlerts.visibility = if (alerts.isNotEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
