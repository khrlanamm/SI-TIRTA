package com.khrlanamm.sitirta.ui.penagihan

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.khrlanamm.sitirta.databinding.FragmentPenagihanBinding
import com.khrlanamm.sitirta.ui.cetak.CetakActivity

class PenagihanFragment : Fragment() {

    private var _binding: FragmentPenagihanBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val penagihanViewModel =
            ViewModelProvider(this).get(PenagihanViewModel::class.java)

        _binding = FragmentPenagihanBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textPenagihan
        penagihanViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set listener untuk tombol cetak
        binding.btnCetak.setOnClickListener {
            // Buat Intent untuk memulai CetakActivity
            val intent = Intent(requireActivity(), CetakActivity::class.java)
            // Jalankan activity
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}