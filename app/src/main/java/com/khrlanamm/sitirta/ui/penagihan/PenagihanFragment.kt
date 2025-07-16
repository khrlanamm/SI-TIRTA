package com.khrlanamm.sitirta.ui.penagihan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.khrlanamm.sitirta.R
import com.khrlanamm.sitirta.databinding.FragmentPenagihanBinding

class PenagihanFragment : Fragment() {

    private var _binding: FragmentPenagihanBinding? = null
    private val binding get() = _binding!!

    private lateinit var penagihanViewModel: PenagihanViewModel
    private lateinit var penagihanAdapter: PenagihanAdapter

    private var lastCheckedRadioButtonId = -1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        penagihanViewModel = ViewModelProvider(this)[PenagihanViewModel::class.java]
        _binding = FragmentPenagihanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        // setupDropdown() akan dipanggil dari dalam observeViewModel
        setupRecyclerView()
        setupSearch()
        setupFilterRadioButtons()
        observeViewModel()
    }

    private fun setupToolbar() {
        (activity as? AppCompatActivity)?.setSupportActionBar(binding.toolbar)
        (activity as? AppCompatActivity)?.supportActionBar?.apply {
            title = "Penagihan"
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
        binding.toolbar.setNavigationOnClickListener {
            activity?.onBackPressed()
        }
    }

    // Method ini sekarang menerima daftar nama sebagai argumen
    private fun setupDropdown(pjNames: List<String>) {
        // Tambahkan "Semua" di awal daftar untuk opsi clear filter
        val items = mutableListOf("Semua").apply { addAll(pjNames) }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, items)
        binding.actvPenanggungJawab.setAdapter(adapter)

        binding.actvPenanggungJawab.setOnItemClickListener { _, _, position, _ ->
            // Gunakan nama yang dipilih untuk memfilter
            val selectedPjName = if (items[position] == "Semua") null else items[position]
            penagihanViewModel.setPjFilter(selectedPjName)
        }
    }

    private fun setupRecyclerView() {
        penagihanAdapter = PenagihanAdapter(emptyList())
        binding.rvPenagihan.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = penagihanAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener { text ->
            penagihanViewModel.setSearchQuery(text.toString())
        }
    }

    private fun setupFilterRadioButtons() {
        binding.rgStatus.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId == lastCheckedRadioButtonId) {
                group.clearCheck()
                penagihanViewModel.setPaymentStatusFilter(null)
                lastCheckedRadioButtonId = -1
                return@setOnCheckedChangeListener
            }

            lastCheckedRadioButtonId = checkedId
            when (checkedId) {
                R.id.rb_sudah_dibayar -> penagihanViewModel.setPaymentStatusFilter("Sudah Bayar")
                R.id.rb_belum_dibayar -> penagihanViewModel.setPaymentStatusFilter("Belum Bayar")
            }
        }
    }

    private fun observeViewModel() {
        // Observer baru untuk daftar penanggung jawab
        penagihanViewModel.penanggungJawabList.observe(viewLifecycleOwner) { pjList ->
            // Ambil daftar nama dari objek PenanggungJawab
            val pjNames = pjList.mapNotNull { it.name }
            // Panggil setupDropdown dengan daftar nama yang didapat dari Firebase
            setupDropdown(pjNames)
        }

        penagihanViewModel.pelangganList.observe(viewLifecycleOwner) { pelanggan ->
            penagihanAdapter.updateData(pelanggan)
        }

        penagihanViewModel.statistik.observe(viewLifecycleOwner) { stats ->
            val totalPenagihan = (stats["penagihan"]?.first ?: 0) + (stats["penagihan"]?.second ?: 0)
            val totalPembayaran = (stats["pembayaran"]?.first ?: 0) + (stats["pembayaran"]?.second ?: 0)

            val sudahDitagih = stats["penagihan"]?.first ?: 0
            val belumDitagih = stats["penagihan"]?.second ?: 0
            val sudahDibayar = stats["pembayaran"]?.first ?: 0
            val belumDibayar = stats["pembayaran"]?.second ?: 0

            val persenSudahDitagih = if (totalPenagihan > 0) (sudahDitagih * 100) / totalPenagihan else 0
            binding.tvSudahDitagih.text = "Sudah Ditagih : $sudahDitagih ($persenSudahDitagih%)"
            binding.progressSudahDitagih.progress = persenSudahDitagih

            val persenBelumDitagih = if (totalPenagihan > 0) (belumDitagih * 100) / totalPenagihan else 0
            binding.tvBelumDitagih.text = "Belum Ditagih : $belumDitagih ($persenBelumDitagih%)"
            binding.progressBelumDitagih.progress = persenBelumDitagih

            val persenSudahDibayar = if (totalPembayaran > 0) (sudahDibayar * 100) / totalPembayaran else 0
            binding.tvSudahDibayar.text = "Sudah Bayar : $sudahDibayar ($persenSudahDibayar%)"
            binding.progressSudahDibayar.progress = persenSudahDibayar

            val persenBelumDibayar = if (totalPembayaran > 0) (belumDibayar * 100) / totalPembayaran else 0
            binding.tvBelumDibayar.text = "Belum Bayar : $belumDibayar ($persenBelumDibayar%)"
            binding.progressBelumDibayar.progress = persenBelumDibayar
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}