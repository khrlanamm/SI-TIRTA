package com.khrlanamm.sitirta.ui.pembukuan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.khrlanamm.sitirta.databinding.FragmentPembukuanBinding

class PembukuanFragment : Fragment() {

    private var _binding: FragmentPembukuanBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val pembukuanViewModel =
            ViewModelProvider(this).get(PembukuanViewModel::class.java)

        _binding = FragmentPembukuanBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textPembukuan
        pembukuanViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}