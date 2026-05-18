package com.gramasethu.app.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.gramasethu.app.databinding.FragmentBridgeListBinding
import com.gramasethu.app.model.BridgeStatus
import com.gramasethu.app.ui.BridgeDetailActivity
import com.gramasethu.app.ui.adapter.BridgeAdapter
import com.gramasethu.app.viewmodel.BridgeViewModel

class BridgeListFragment : Fragment() {

    private var _b: FragmentBridgeListBinding? = null
    private val b get() = _b!!
    private val vm: BridgeViewModel by activityViewModels()

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentBridgeListBinding.inflate(i, c, false); return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = BridgeAdapter { bridge ->
            startActivity(Intent(requireContext(), BridgeDetailActivity::class.java)
                .putExtra(BridgeDetailActivity.EXTRA_ID, bridge.id))
        }
        b.recyclerBridges.layoutManager = LinearLayoutManager(requireContext())
        b.recyclerBridges.adapter = adapter

        b.editSearch.addTextChangedListener { vm.setQuery(it.toString()) }

        b.chipAll.setOnClickListener       { vm.setFilter(null) }
        b.chipOpen.setOnClickListener      { vm.setFilter(BridgeStatus.OPEN) }
        b.chipDamaged.setOnClickListener   { vm.setFilter(BridgeStatus.DAMAGED) }
        b.chipSubmerged.setOnClickListener { vm.setFilter(BridgeStatus.SUBMERGED) }
        b.chipUnknown.setOnClickListener   { vm.setFilter(BridgeStatus.UNKNOWN) }

        vm.filteredBridges.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            b.textEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
