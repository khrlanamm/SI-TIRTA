package com.khrlanamm.sitirta.ui.pembukuan

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.RadioButton
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
import com.khrlanamm.sitirta.data.Transaksi
import com.khrlanamm.sitirta.databinding.FragmentPembukuanBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
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
        setupClickListeners()

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

        pembukuanAdapter.setOnItemClickListener { transaksi ->
            showItemOptionsDialog(transaksi)
        }
    }

    private fun showItemOptionsDialog(transaksi: Transaksi) {
        val options = arrayOf("Edit Transaksi", "Hapus Transaksi")
        AlertDialog.Builder(requireContext())
            .setTitle("Pilih Aksi")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> showEditTransaksiDialog(transaksi)
                    1 -> showDeleteConfirmationDialog(transaksi)
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showDeleteConfirmationDialog(transaksi: Transaksi) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Transaksi")
            .setMessage("Apakah Anda yakin ingin menghapus transaksi ini?\n\n\"${transaksi.keterangan}\"")
            .setPositiveButton("Hapus") { _, _ ->
                transaksi.id?.let { id ->
                    pembukuanViewModel.deleteTransaksi(id) { isSuccess ->
                        handleFormSubmission(isSuccess, "Transaksi berhasil dihapus", "Gagal menghapus transaksi")
                    }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun setupSpinners() {
        val bulanAdapter = ArrayAdapter.createFromResource(
            requireContext(), R.array.bulan_array, android.R.layout.simple_spinner_item
        )
        bulanAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerBulan.adapter = bulanAdapter
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
        val selectedMonth = binding.spinnerBulan.selectedItemPosition
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
        binding.btnTambahTransaksi.setOnClickListener {
            showTambahTransaksiDialog()
        }
        binding.btnUnduhLaporan.setOnClickListener {
            Toast.makeText(context, "Fitur Unduh Laporan akan segera tersedia", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showTambahTransaksiDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_tambah_transaksi, null)
        val dialogCalendar = Calendar.getInstance()
        setupFormDialog(dialogView, "Tambah Transaksi Baru", null, dialogCalendar) { keterangan, nominal, jenis, tanggal ->
            pembukuanViewModel.tambahTransaksi(keterangan, nominal, jenis, tanggal) { isSuccess ->
                handleFormSubmission(isSuccess, "Transaksi berhasil disimpan", "Gagal menyimpan transaksi")
            }
        }
    }

    private fun showEditTransaksiDialog(transaksi: Transaksi) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_tambah_transaksi, null)
        val dialogCalendar = Calendar.getInstance().apply {
            time = transaksi.tanggal?.toDate() ?: Date()
        }
        setupFormDialog(dialogView, "Edit Transaksi", transaksi, dialogCalendar) { keterangan, nominal, jenis, tanggal ->
            transaksi.id?.let { id ->
                pembukuanViewModel.updateTransaksi(id, keterangan, nominal, jenis, tanggal) { isSuccess ->
                    handleFormSubmission(isSuccess, "Transaksi berhasil diperbarui", "Gagal memperbarui transaksi")
                }
            }
        }
    }

    private fun setupFormDialog(
        dialogView: View,
        title: String,
        transaksi: Transaksi?,
        dialogCalendar: Calendar,
        onSave: (keterangan: String, nominal: Long, jenis: String, tanggal: Date) -> Unit
    ) {
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", localeID)
        val tvTanggalDipilih = dialogView.findViewById<TextView>(R.id.tv_tanggal_dipilih)
        val etKeterangan = dialogView.findViewById<EditText>(R.id.et_keterangan)
        val etNominal = dialogView.findViewById<EditText>(R.id.et_nominal)
        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.rg_jenis_transaksi)
        val rbPemasukan = dialogView.findViewById<RadioButton>(R.id.rb_pemasukan)
        val rbPengeluaran = dialogView.findViewById<RadioButton>(R.id.rb_pengeluaran)

        dialogView.findViewById<TextView>(R.id.form_title).text = title
        if (transaksi != null) {
            etKeterangan.setText(transaksi.keterangan)
            etNominal.setText(transaksi.nominal?.toString() ?: "0")
            // PERBAIKAN: Menggunakan "Debit"
            if (transaksi.jenis == "Debit") {
                rbPemasukan.isChecked = true
            } else {
                rbPengeluaran.isChecked = true
            }
        }

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

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Simpan", null)
            .setNegativeButton("Batal", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val keterangan = etKeterangan.text.toString().trim()
                val nominalStr = etNominal.text.toString().trim()
                // PERBAIKAN: Menggunakan "Debit"
                val jenis = if (radioGroup.checkedRadioButtonId == R.id.rb_pemasukan) "Debit" else "Kredit"

                if (keterangan.isEmpty() || nominalStr.isEmpty()) {
                    Toast.makeText(context, "Keterangan dan Nominal tidak boleh kosong", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val nominal = nominalStr.toLong()
                positiveButton.isEnabled = false

                onSave(keterangan, nominal, jenis, dialogCalendar.time)
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun handleFormSubmission(isSuccess: Boolean, successMsg: String, errorMsg: String) {
        if (isSuccess) {
            Toast.makeText(context, successMsg, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
