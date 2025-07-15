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

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Akun Saya"

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}