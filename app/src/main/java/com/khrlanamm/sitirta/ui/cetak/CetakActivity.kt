package com.khrlanamm.sitirta.ui.cetak

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.dantsu.escposprinter.textparser.PrinterTextParserImg
import com.khrlanamm.sitirta.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CetakActivity : ComponentActivity() {

    private var selectedPrinter: BluetoothConnection? = null

    // Launcher modern untuk meminta izin Bluetooth secara aman.
    private val requestBluetoothPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            // Cek apakah semua izin yang diminta telah diberikan.
            if (permissions.values.all { it }) {
                // Jika ya, lanjutkan proses memilih printer.
                selectPrinter()
            } else {
                // Jika tidak, tampilkan pesan kepada pengguna.
                Toast.makeText(this, "Izin Bluetooth diperlukan untuk memilih printer", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cetak)

        val selectPrinterButton: Button = findViewById(R.id.button_select_printer)
        val printButton: Button = findViewById(R.id.button_print)

        // Listener untuk tombol pilih printer.
        selectPrinterButton.setOnClickListener {
            checkAndRequestPermissions()
        }

        // Listener untuk tombol cetak.
        printButton.setOnClickListener {
            printReceipt()
        }
    }

    /**
     * Memeriksa apakah izin yang diperlukan sudah ada.
     * Jika belum, fungsi ini akan memintanya.
     */
    private fun checkAndRequestPermissions() {
        // Tentukan izin yang diperlukan berdasarkan versi Android.
        val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12 (API 31) dan yang lebih baru.
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            // Android 11 (API 30) dan yang lebih lama.
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

        // Cek apakah semua izin sudah diberikan.
        val allPermissionsGranted = permissionsToRequest.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allPermissionsGranted) {
            // Jika semua izin sudah ada, langsung buka dialog pilih printer.
            selectPrinter()
        } else {
            // Jika ada izin yang belum diberikan, minta izin tersebut.
            requestBluetoothPermissions.launch(permissionsToRequest)
        }
    }

    /**
     * Memindai printer Bluetooth yang sudah dipasangkan (paired)
     * dan menampilkan dialog untuk pengguna memilihnya.
     */
    @SuppressLint("MissingPermission") // Izin sudah dicek di checkAndRequestPermissions()
    private fun selectPrinter() {
        // FIX: Gunakan metode statis `selectFirstPaired` yang mengembalikan Array<BluetoothConnection>
        val bluetoothPrinters: Array<BluetoothConnection>? = BluetoothPrintersConnections().list

        if (!bluetoothPrinters.isNullOrEmpty()) {
            val printerNames = bluetoothPrinters.map { it.device.name }.toTypedArray()

            // Tampilkan dialog pilihan.
            AlertDialog.Builder(this)
                .setTitle("Pilih Printer")
                .setItems(printerNames) { _, which ->
                    // Simpan printer yang dipilih oleh pengguna.
                    selectedPrinter = bluetoothPrinters[which]
                    val selectedPrinterTextView: TextView = findViewById(R.id.text_selected_printer)
                    selectedPrinterTextView.text = "Terpilih: ${selectedPrinter?.device?.name}"
                    Toast.makeText(this, "Printer ${selectedPrinter?.device?.name} dipilih", Toast.LENGTH_SHORT).show()
                }
                .show()
        } else {
            Toast.makeText(this, "Tidak ada printer Bluetooth yang ter-pair!", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Memformat teks dan gambar, lalu mengirimkannya ke printer yang dipilih.
     */
    private fun printReceipt() {
        if (selectedPrinter == null) {
            Toast.makeText(this, "Silakan pilih printer terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        // Jalankan proses cetak di background thread agar tidak mengganggu UI.
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Inisialisasi printer dengan konfigurasi (lebar kertas, dll).
                // Sesuaikan parameter ini dengan printer Anda jika perlu.
                val printer = EscPosPrinter(selectedPrinter, 203, 48f, 32)
                val currentDate = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date())

                // Buat teks terformat menggunakan markup khusus dari pustaka.
                val textToPrint =
                    "[C]<img>" + PrinterTextParserImg.bitmapToHexadecimalString(printer, applicationContext.resources.getDrawable(R.drawable.bw_logo, applicationContext.theme)) + "</img>\n" +
                            "[C]================================\n" +
                            "[L]\n" +
                            "[L]<b>Nama:</b> Muhammad Lorem Ipsum\n" +
                            "[L]<b>RT:</b> 12\n" +
                            "[L]<b>Kampung:</b> Tanah Nago\n" +
                            "[C]--------------------------------\n" +
                            "[L]Meter Air Bulan Lalu:[R]12\n" +
                            "[L]Meter Air Bulan Ini:[R]16\n" +
                            "[L]<b>Meter Air Terpakai:</b>[R]<b>4</b>\n" +
                            "[L]Harga Per Meter:[R]2.500\n" +
                            "[L]Iuran Bulanan:[R]5.000\n" +
                            "[C]================================\n" +
                            "[L]<b>TOTAL PEMBAYARAN</b>[R]<b>Rp15.000</b>\n\n" +
                            "[C]Terima kasih!\n" +
                            "[C]<font size='small'>Dicetak pada: $currentDate</font>"

                // Cetak dan potong kertas (jika printer mendukung).
                printer.printFormattedTextAndCut(textToPrint)

                // Tampilkan pesan sukses di UI thread.
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CetakActivity, "Cetak berhasil!", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                // Tampilkan pesan error di UI thread.
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CetakActivity, "Gagal mencetak: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
