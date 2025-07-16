package com.khrlanamm.sitirta.ui.cetak

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.dantsu.escposprinter.textparser.PrinterTextParserImg
import com.google.firebase.firestore.FirebaseFirestore
import com.khrlanamm.sitirta.R
import com.khrlanamm.sitirta.data.Pelanggan
import com.khrlanamm.sitirta.databinding.ActivityCetakBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CetakActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PELANGGAN = "extra_pelanggan"
        const val HARGA_PER_METER = 2500
        const val IURAN_BULANAN = 5000
    }

    private lateinit var binding: ActivityCetakBinding
    // Pastikan properti di data class Pelanggan menggunakan 'var' agar bisa diubah.
    private var pelanggan: Pelanggan? = null
    private var selectedPrinter: BluetoothConnection? = null
    private val db = FirebaseFirestore.getInstance()

    private val requestBluetoothPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.values.all { it }) {
                selectPrinter()
            } else {
                Toast.makeText(this, "Izin Bluetooth diperlukan untuk memilih printer", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCetakBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        getPelangganData()
        setupUI()
        setupListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun getPelangganData() {
        pelanggan = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_PELANGGAN, Pelanggan::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_PELANGGAN)
        }

        if (pelanggan == null) {
            Toast.makeText(this, "Gagal memuat data pelanggan", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupUI() {
        pelanggan?.let { p ->
            // Setup data pelanggan
            binding.itemPelanggan.tvPersonName.text = p.nama
            binding.itemPelanggan.tvPersonId.text = p.id_pelanggan
            binding.itemPelanggan.tvPersonRt.text = "RT ${p.rt}"
            updateStatusButtons(p.status_penagihan, p.status_pembayaran)

            // Setup detail tagihan
            val (namaBulanLalu, namaBulanIni) = getMonthNames()
            binding.tvMeterLaluLabel.text = "Meter Air $namaBulanLalu:"
            binding.tvMeterIniLabel.text = "Meter Air $namaBulanIni:"
            binding.tvTotalTagihanBulanIniLabel.text = "Total Tagihan Bulan $namaBulanIni:"

            val meterLalu = p.meteran?.get("bulan_lalu") ?: 0
            binding.tvMeterLaluValue.text = meterLalu.toString()

            val meterIni = p.meteran?.get("bulan_ini")
            if (meterIni != null) {
                binding.etMeterIniValue.setText(meterIni.toString())
                binding.etMeterIniValue.isEnabled = false
                binding.buttonHitung.isEnabled = false
                calculateAndDisplayBill(meterLalu, meterIni)
            }

            binding.switchSudahDibayar.isChecked = p.status_pembayaran.equals("Sudah Dibayar", ignoreCase = true)
        }
    }

    private fun setupListeners() {
        binding.buttonSelectPrinter.setOnClickListener { checkAndRequestPermissions() }
        binding.buttonHitung.setOnClickListener { handleHitungTagihan() }
        binding.buttonPrint.setOnClickListener { printReceipt() }
        binding.switchSudahDibayar.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && pelanggan?.status_pembayaran?.equals("Belum Dibayar", ignoreCase = true) == true) {
                handlePembayaran()
            }
        }
    }

    private fun handlePembayaran() {
        AlertDialog.Builder(this)
            .setTitle("Konfirmasi Pembayaran")
            .setMessage("Apakah Anda yakin ingin menandai tagihan ini sebagai 'Sudah Dibayar'?")
            .setPositiveButton("Ya, Yakin") { _, _ ->
                updatePaymentStatus()
            }
            .setNegativeButton("Batal") { dialog, _ ->
                binding.switchSudahDibayar.isChecked = false
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun updatePaymentStatus() {
        pelanggan?.id_pelanggan?.let { id ->
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    db.collection("pelanggan").document(id)
                        .update("status_pembayaran", "Sudah Dibayar")
                        .await()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@CetakActivity, "Status pembayaran berhasil diperbarui", Toast.LENGTH_SHORT).show()

                        // FIX: Perbarui data pelanggan lokal agar UI konsisten
                        // PENTING: Ini hanya akan berfungsi jika properti di Pelanggan.kt adalah 'var'
                        pelanggan?.status_pembayaran = "Sudah Dibayar"

                        updateStatusButtons(pelanggan?.status_penagihan, pelanggan?.status_pembayaran)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@CetakActivity, "Gagal memperbarui status: ${e.message}", Toast.LENGTH_LONG).show()
                        binding.switchSudahDibayar.isChecked = false
                    }
                }
            }
        }
    }

    private fun handleHitungTagihan() {
        val meterIniStr = binding.etMeterIniValue.text.toString()
        if (meterIniStr.isEmpty()) {
            Toast.makeText(this, "Meter bulan ini tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }
        val meterIni = meterIniStr.toLong()
        val meterLalu = pelanggan?.meteran?.get("bulan_lalu") ?: 0

        if (meterIni < meterLalu) {
            Toast.makeText(this, "Meter bulan ini tidak boleh lebih kecil dari bulan lalu", Toast.LENGTH_SHORT).show()
            return
        }

        calculateAndDisplayBill(meterLalu, meterIni)
        updateBillingDataInFirestore(meterIni)
    }

    private fun calculateAndDisplayBill(meterLalu: Long, meterIni: Long) {
        val selisih = meterIni - meterLalu
        val tagihanAir = selisih * HARGA_PER_METER
        val totalTagihan = tagihanAir + IURAN_BULANAN

        binding.tvMeterDitagihValue.text = "$selisih m³"
        binding.tvTotalTagihanAirValue.text = formatRupiah(tagihanAir.toDouble())
        binding.tvTotalTagihanBulanIniValue.text = formatRupiah(totalTagihan.toDouble())
    }

    private fun updateBillingDataInFirestore(meterBulanIni: Long) {
        pelanggan?.id_pelanggan?.let { id ->
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val updates = mapOf(
                        "meteran.bulan_ini" to meterBulanIni,
                        "status_penagihan" to "Sudah Ditagih"
                    )
                    db.collection("pelanggan").document(id).update(updates).await()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@CetakActivity, "Tagihan berhasil disimpan", Toast.LENGTH_SHORT).show()
                        binding.buttonHitung.isEnabled = false
                        binding.etMeterIniValue.isEnabled = false

                        // FIX: Perbarui data pelanggan lokal agar UI konsisten
                        // PENTING: Ini hanya akan berfungsi jika properti di Pelanggan.kt adalah 'var'
                        pelanggan?.status_penagihan = "Sudah Ditagih"

                        // FIX: Gunakan sintaks yang benar untuk update Map
                        // PENTING: Ini hanya akan berfungsi jika 'meteran' di Pelanggan.kt adalah MutableMap
                        (pelanggan?.meteran as? MutableMap)?.put("bulan_ini", meterBulanIni)

                        updateStatusButtons(pelanggan?.status_penagihan, pelanggan?.status_pembayaran)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@CetakActivity, "Gagal menyimpan tagihan: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (permissionsToRequest.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
            selectPrinter()
        } else {
            requestBluetoothPermissions.launch(permissionsToRequest)
        }
    }

    @SuppressLint("MissingPermission")
    private fun selectPrinter() {
        val bluetoothPrinters = BluetoothPrintersConnections().list
        if (bluetoothPrinters.isNullOrEmpty()) {
            Toast.makeText(this, "Tidak ada printer Bluetooth yang ter-pair!", Toast.LENGTH_LONG).show()
            return
        }
        val printerNames = bluetoothPrinters.map { it.device.name }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Pilih Printer")
            .setItems(printerNames) { _, which ->
                selectedPrinter = bluetoothPrinters[which]
                binding.textSelectedPrinter.text = "Terhubung: ${selectedPrinter?.device?.name}"
                binding.textSelectedPrinter.background = ContextCompat.getDrawable(this, R.drawable.status_connected_background)
            }
            .show()
    }

    private fun printReceipt() {
        if (selectedPrinter == null) {
            Toast.makeText(this, "Silakan pilih printer terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }
        if (binding.tvTotalTagihanBulanIniValue.text.isNullOrEmpty()) {
            Toast.makeText(this, "Silakan hitung tagihan terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val printer = EscPosPrinter(selectedPrinter, 203, 48f, 32)
                val textToPrint = formatReceiptText()
                printer.printFormattedTextAndCut(textToPrint)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CetakActivity, "Cetak berhasil!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CetakActivity, "Gagal mencetak: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun formatReceiptText(): String {
        val p = pelanggan ?: return ""
        val (namaBulanLalu, namaBulanIni) = getMonthNames()
        val currentDate = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date())

        val meterLalu = p.meteran?.get("bulan_lalu") ?: 0
        val meterIni = binding.etMeterIniValue.text.toString().toLongOrNull() ?: 0
        val selisih = meterIni - meterLalu
        val tagihanAir = selisih * HARGA_PER_METER
        val totalTagihan = tagihanAir + IURAN_BULANAN

        val logo = "[C]<img>" + PrinterTextParserImg.bitmapToHexadecimalString(
            EscPosPrinter(selectedPrinter, 203, 48f, 32),
            applicationContext.resources.getDrawable(R.drawable.bw_logo, applicationContext.theme)
        ) + "</img>\n"

        val statusPembayaranText = if (pelanggan?.status_pembayaran.equals("Sudah Dibayar", ignoreCase = true)) {
            "[C]<b>LUNAS</b>\n"
        } else {
            "[C]<b>BELUM DIBAYAR</b>\n"
        }

        return logo +
                "[C]<b>KPSPAM BANYU AGUNG</b>\n" +
                "[C]Kp. Tanah Nago Sungai Pulai\n" +
                "[C]================================\n" +
                "[L]<b>ID:</b> ${p.id_pelanggan}\n" +
                "[L]<b>Nama:</b> ${p.nama}\n" +
                "[L]<b>RT:</b> ${p.rt}\n" +
                "[C]--------------------------------\n" +
                "[L]Meter $namaBulanLalu:[R]$meterLalu\n" +
                "[L]Meter $namaBulanIni:[R]$meterIni\n" +
                "[L]<b>Meter Terpakai:</b>[R]<b>$selisih m³</b>\n" +
                "[L]Harga per meter:[R]${formatRupiah(HARGA_PER_METER.toDouble())}\n" +
                "[L]Total Tagihan Air:[R]${formatRupiah(tagihanAir.toDouble())}\n" +
                "[L]Iuran Bulanan:[R]${formatRupiah(IURAN_BULANAN.toDouble())}\n" +
                "[C]================================\n" +
                "[L]<b>TOTAL TAGIHAN</b>[R]<b>${formatRupiah(totalTagihan.toDouble())}</b>\n\n" +
                "[C]================================\n" +
                statusPembayaranText +
                "\n" +
                "[C]Terima kasih!\n" +
                "[C]<font size='small'>Dicetak pada: $currentDate</font>"
    }

    private fun getMonthNames(): Pair<String, String> {
        val calendar = Calendar.getInstance()
        val locale = Locale("id", "ID")
        val monthFormat = SimpleDateFormat("MMMM yyyy", locale)

        val namaBulanIni = monthFormat.format(calendar.time)
        calendar.add(Calendar.MONTH, -1)
        val namaBulanLalu = monthFormat.format(calendar.time)

        return Pair(namaBulanLalu, namaBulanIni)
    }

    private fun formatRupiah(number: Double): String {
        val localeID = Locale("in", "ID")
        val formatRupiah = NumberFormat.getCurrencyInstance(localeID)
        formatRupiah.maximumFractionDigits = 0
        return formatRupiah.format(number)
    }

    private fun updateStatusButtons(statusTagihan: String?, statusBayar: String?) {
        setupButton(binding.itemPelanggan.btnStatusTagihan, statusTagihan, "Sudah Ditagih", "Belum Ditagih", R.color.status_green, R.color.status_blue)
        // FIX: Menggunakan warna merah untuk status "Belum Dibayar"
        setupButton(binding.itemPelanggan.btnStatusPembayaran, statusBayar, "Sudah Dibayar", "Belum Dibayar", R.color.status_green, R.color.status_blue)
    }

    private fun setupButton(button: com.google.android.material.button.MaterialButton, status: String?, textSudah: String, textBelum: String, colorSudah: Int, colorBelum: Int) {
        if (status.equals(textSudah, ignoreCase = true)) {
            button.text = textSudah
            button.backgroundTintList = ContextCompat.getColorStateList(this, colorSudah)
        } else {
            button.text = textBelum
            button.backgroundTintList = ContextCompat.getColorStateList(this, colorBelum)
        }
    }
}
