package com.khrlanamm.sitirta.ui.pembukuan

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.khrlanamm.sitirta.data.Transaksi
import java.util.Calendar
import java.util.Date

class PembukuanViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val transaksiCollection = db.collection("transaksi")

    // LiveData untuk daftar transaksi yang akan ditampilkan
    private val _transaksiList = MutableLiveData<List<Transaksi>>()
    val transaksiList: LiveData<List<Transaksi>> get() = _transaksiList

    // LiveData untuk status loading
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    // LiveData untuk hasil kalkulasi
    private val _totalPemasukan = MutableLiveData<Long>()
    val totalPemasukan: LiveData<Long> get() = _totalPemasukan

    private val _totalPengeluaran = MutableLiveData<Long>()
    val totalPengeluaran: LiveData<Long> get() = _totalPengeluaran

    private val _saldoAkhir = MutableLiveData<Long>()
    val saldoAkhir: LiveData<Long> get() = _saldoAkhir

    /**
     * Mengambil data transaksi dari Firestore untuk bulan dan tahun tertentu.
     */
    fun fetchTransaksi(year: Int, month: Int) {
        _isLoading.value = true

        val cal = Calendar.getInstance()
        // Set ke awal bulan
        cal.set(year, month, 1, 0, 0, 0)
        val startDate = cal.time

        // Set ke awal bulan berikutnya untuk batas akhir query
        cal.add(Calendar.MONTH, 1)
        val endDate = cal.time

        transaksiCollection
            .whereGreaterThanOrEqualTo("tanggal", Timestamp(startDate))
            .whereLessThan("tanggal", Timestamp(endDate))
            .orderBy("tanggal", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                _isLoading.value = false
                if (error != null) {
                    // Handle error, mungkin dengan LiveData terpisah untuk error
                    _transaksiList.value = emptyList()
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    val list = snapshots.toObjects(Transaksi::class.java)
                    _transaksiList.value = list
                    calculateTotals(list)
                }
            }
    }

    /**
     * Menambahkan transaksi baru ke Firestore.
     */
    fun tambahTransaksi(keterangan: String, nominal: Long, jenis: String, tanggal: Date, onResult: (Boolean) -> Unit) {
        val transaksiBaru = Transaksi(
            keterangan = keterangan,
            nominal = nominal,
            jenis = jenis,
            tanggal = Timestamp(tanggal)
        )

        transaksiCollection.add(transaksiBaru)
            .addOnSuccessListener {
                onResult(true) // Sukses
            }
            .addOnFailureListener {
                onResult(false) // Gagal
            }
    }

    private fun calculateTotals(list: List<Transaksi>) {
        val pemasukan = list.filter { it.jenis == "Debit" }.sumOf { it.nominal ?: 0L }
        val pengeluaran = list.filter { it.jenis == "Kredit" }.sumOf { it.nominal ?: 0L }

        _totalPemasukan.value = pemasukan
        _totalPengeluaran.value = pengeluaran
        _saldoAkhir.value = pemasukan - pengeluaran
    }
}
