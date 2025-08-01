package com.khrlanamm.sitirta.ui.pembukuan

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.khrlanamm.sitirta.databinding.FragmentPembukuanBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
// Import Date dan Locale tidak lagi diperlukan, bisa dihapus
// import java.util.Date
// import java.util.Locale

class PembukuanFragment : Fragment() {

    private var _binding: FragmentPembukuanBinding? = null
    private val binding get() = _binding!!

    // 1. Properti untuk menyimpan printer yang dipilih & launcher untuk meminta izin
    private var selectedPrinter: BluetoothConnection? = null
    private val requestBluetoothPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.values.all { it }
            if (allGranted) {
                // Jika izin diberikan, langsung buka dialog pilih printer
                selectPrinterAndPrint()
            } else {
                Toast.makeText(requireContext(), "Izin Bluetooth diperlukan untuk mencetak", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val pembukuanViewModel = ViewModelProvider(this).get(PembukuanViewModel::class.java)
        _binding = FragmentPembukuanBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textPembukuan
        pembukuanViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 2. Menambahkan listener klik pada TextView
        binding.textPembukuan.setOnClickListener {
            // Memulai proses dengan memeriksa izin
            checkAndRequestPermissions()
        }
    }

    // 3. Fungsi untuk memeriksa & meminta izin Bluetooth sesuai versi Android
    private fun checkAndRequestPermissions() {
        val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            // Untuk Android 11 ke bawah
            arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val allPermissionsGranted = permissionsToRequest.all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }

        if (allPermissionsGranted) {
            selectPrinterAndPrint()
        } else {
            requestBluetoothPermissions.launch(permissionsToRequest)
        }
    }

    // 4. Fungsi untuk mencari printer dan menampilkan dialog pilihan
    @SuppressLint("MissingPermission")
    private fun selectPrinterAndPrint() {
        val bluetoothPrinters = BluetoothPrintersConnections().list
        if (bluetoothPrinters.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Tidak ada printer Bluetooth yang ter-pair!", Toast.LENGTH_LONG).show()
            return
        }

        val printerNames = bluetoothPrinters.map { it.device.name }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle("Pilih Printer")
            .setItems(printerNames) { _, which ->
                selectedPrinter = bluetoothPrinters[which]
                // Setelah printer dipilih, langsung jalankan fungsi cetak
                printStruk()
            }
            .show()
    }

    // 5. Fungsi utama untuk mencetak struk
    private fun printStruk() {
        if (selectedPrinter == null) {
            Toast.makeText(requireContext(), "Proses dibatalkan, printer tidak dipilih.", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Inisialisasi printer dengan koneksi yang dipilih
                val printer = EscPosPrinter(selectedPrinter, 203, 48f, 32)
                val textToPrint = formatStrukTokoWindy()
                printer.printFormattedTextAndCut(textToPrint)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Struk berhasil dicetak!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Gagal mencetak: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // 6. Fungsi untuk memformat teks struk "TOKO WINDY"
    private fun formatStrukTokoWindy(): String {
        // PERUBAHAN 1: Buat list item agar lebih mudah diformat
        val items = listOf(
            Triple("Indomilk Kids", 10, "4.000"),
            Triple("Lolipop", 3, "5.000"),
            Triple("Serba 10rb", 1, "10.000"),
            Triple("Lakban kertas 1inch", 1, "10.000"),
            Triple("Buku Gambar A3", 1, "10.000"),
            Triple("Kertas B2", 3, "2.000")
        )

        val itemsText = items.joinToString("") { (nama, qty, harga) ->
            // PERUBAHAN 2: Format nama barang dan qty agar rapi
            // Nama barang diberi ruang 22 karakter, lalu diikuti qty
            val leftColumn = "${nama.padEnd(22)}${qty}"
            "[L]${leftColumn}[R]${harga}\n"
        }

        // PERUBAHAN 3: Alamat diubah dan Tanggal dibuat statis
        return "[C]<b>TOKO WINDY & SELI</b>\n" +
                "[C]Jl. Poros Tanah Nago\n" +
                "[C]Silaut, Pesisir Selatan\n" + // Alamat diperbaiki
                "[C]================================\n" +
                itemsText + // Menggunakan item yang sudah diformat
                "[C]--------------------------------\n" +
                "[L]<b>Total:</b>[R]<b>91.000</b>\n\n" +
                "[C]Dicetak Pada:\n" +
                "[C]22/07/2025 19:54\n" + // Tanggal diubah menjadi statis
                "[C]Terima Kasih sudah berbelanja"
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}