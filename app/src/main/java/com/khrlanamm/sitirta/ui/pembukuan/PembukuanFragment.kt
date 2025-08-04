package com.khrlanamm.sitirta.ui.pembukuan

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.khrlanamm.sitirta.R
import com.khrlanamm.sitirta.databinding.FragmentPembukuanBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PembukuanFragment : Fragment() {

    private var _binding: FragmentPembukuanBinding? = null
    private val binding get() = _binding!!
    private lateinit var pembukuanViewModel: PembukuanViewModel
    private lateinit var pembukuanAdapter: PembukuanAdapter

    private val calendar = Calendar.getInstance()
    private val localeID = Locale("in", "ID")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        pembukuanViewModel = ViewModelProvider(this)[PembukuanViewModel::class.java]
        _binding = FragmentPembukuanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
        setupSpinners()
        setupObservers()
        setupClickListeners() // Pastikan fungsi ini dipanggil

        // Load data for the current month and year initially
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)
        binding.spinnerTahun.setSelection(getYearAdapter().getPosition(currentYear.toString()))
        binding.spinnerBulan.setSelection(currentMonth)
    }

    private fun setupToolbar() {
        (activity as? AppCompatActivity)?.setSupportActionBar(binding.toolbar)
        (activity as? AppCompatActivity)?.supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
        binding.toolbar.setNavigationOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }
    }

    private fun setupRecyclerView() {
        pembukuanAdapter = PembukuanAdapter()
        binding.rvPembukuan.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = pembukuanAdapter
        }
    }

    private fun setupSpinners() {
        // Setup Month Spinner
        val bulanAdapter = ArrayAdapter.createFromResource(
            requireContext(), R.array.bulan_array, android.R.layout.simple_spinner_item
        )
        bulanAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerBulan.adapter = bulanAdapter

        // Setup Year Spinner
        binding.spinnerTahun.adapter = getYearAdapter()

        val selectionListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                fetchDataBasedOnSelection()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        binding.spinnerBulan.onItemSelectedListener = selectionListener
        binding.spinnerTahun.onItemSelectedListener = selectionListener
    }

    private fun getYearAdapter(): ArrayAdapter<String> {
        val years = (2020..calendar.get(Calendar.YEAR) + 1).map { it.toString() }
        return ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, years).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    private fun fetchDataBasedOnSelection() {
        val selectedYear = binding.spinnerTahun.selectedItem.toString().toInt()
        val selectedMonth = binding.spinnerBulan.selectedItemPosition // 0 for January, 1 for February, etc.
        pembukuanViewModel.fetchTransaksi(selectedYear, selectedMonth)
    }

    private fun setupObservers() {
        val currencyFormat = NumberFormat.getCurrencyInstance(localeID).apply {
            maximumFractionDigits = 0
            minimumFractionDigits = 0
        }

        pembukuanViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
            binding.rvPembukuan.isVisible = !isLoading
        }

        pembukuanViewModel.transaksiList.observe(viewLifecycleOwner) { list ->
            pembukuanAdapter.submitList(list)
            binding.tvEmptyData.isVisible = list.isEmpty() && !binding.progressBar.isVisible
        }

        pembukuanViewModel.totalPemasukan.observe(viewLifecycleOwner) {
            binding.tvTotalPemasukan.text = currencyFormat.format(it)
        }
        pembukuanViewModel.totalPengeluaran.observe(viewLifecycleOwner) {
            binding.tvTotalPengeluaran.text = currencyFormat.format(it)
        }
        pembukuanViewModel.saldoAkhir.observe(viewLifecycleOwner) {
            binding.tvSaldoAkhir.text = currencyFormat.format(it)
        }
    }

    private fun setupClickListeners() {
        // Listener dipindahkan ke tombol baru
        binding.btnTambahTransaksi.setOnClickListener {
            showTambahTransaksiDialog()
        }

        // Listener untuk tombol unduh (logika menyusul)
        binding.btnUnduhLaporan.setOnClickListener {
            Toast.makeText(context, "Fitur Unduh Laporan akan segera tersedia", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showTambahTransaksiDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_tambah_transaksi, null)
        val dialogCalendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", localeID)

        val tvTanggalDipilih = dialogView.findViewById<TextView>(R.id.tv_tanggal_dipilih)
        tvTanggalDipilih.text = dateFormat.format(dialogCalendar.time)

        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            dialogCalendar.set(Calendar.YEAR, year)
            dialogCalendar.set(Calendar.MONTH, month)
            dialogCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            tvTanggalDipilih.text = dateFormat.format(dialogCalendar.time)
        }

        dialogView.findViewById<MaterialButton>(R.id.btn_pilih_tanggal).setOnClickListener {
            DatePickerDialog(requireContext(), dateSetListener,
                dialogCalendar.get(Calendar.YEAR),
                dialogCalendar.get(Calendar.MONTH),
                dialogCalendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Simpan", null) // Set to null to override and prevent auto-closing
            .setNegativeButton("Batal", null)
            .create()
            .apply {
                setOnShowListener {
                    val positiveButton = getButton(AlertDialog.BUTTON_POSITIVE)
                    positiveButton.setOnClickListener {
                        val keterangan = dialogView.findViewById<EditText>(R.id.et_keterangan).text.toString().trim()
                        val nominalStr = dialogView.findViewById<EditText>(R.id.et_nominal).text.toString().trim()
                        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.rg_jenis_transaksi)
                        val jenis = if (radioGroup.checkedRadioButtonId == R.id.rb_pemasukan) "Debit" else "Kredit"

                        if (keterangan.isEmpty() || nominalStr.isEmpty()) {
                            Toast.makeText(context, "Keterangan dan Nominal tidak boleh kosong", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }

                        val nominal = nominalStr.toLong()
                        positiveButton.isEnabled = false // Disable button to prevent double click

                        pembukuanViewModel.tambahTransaksi(keterangan, nominal, jenis, dialogCalendar.time) { isSuccess ->
                            if (isSuccess) {
                                Toast.makeText(context, "Transaksi berhasil disimpan", Toast.LENGTH_SHORT).show()
                                dismiss()
                            } else {
                                Toast.makeText(context, "Gagal menyimpan transaksi", Toast.LENGTH_SHORT).show()
                                positiveButton.isEnabled = true // Re-enable on failure
                            }
                        }
                    }
                }
                show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
