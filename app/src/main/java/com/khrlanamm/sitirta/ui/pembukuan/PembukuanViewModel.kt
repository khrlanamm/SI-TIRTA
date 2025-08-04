package com.khrlanamm.sitirta.ui.pembukuan

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.khrlanamm.sitirta.data.Transaksi
import java.util.Calendar
import java.util.Date

class PembukuanViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val transaksiCollection = db.collection("transaksi")
    private val bendaharaCollection = db.collection("bendahara")

    // --- LIVEDATA BARU UNTUK OTORISASI ---
    private val _isBendahara = MutableLiveData<Boolean>()
    val isBendahara: LiveData<Boolean> get() = _isBendahara

    // Sinyal bahwa pengecekan otorisasi telah selesai
    private val _authCheckComplete = MutableLiveData<Boolean>()
    val authCheckComplete: LiveData<Boolean> get() = _authCheckComplete


    private val _transaksiList = MutableLiveData<List<Transaksi>>()
    val transaksiList: LiveData<List<Transaksi>> get() = _transaksiList

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _totalPemasukan = MutableLiveData<Long>()
    val totalPemasukan: LiveData<Long> get() = _totalPemasukan

    private val _totalPengeluaran = MutableLiveData<Long>()
    val totalPengeluaran: LiveData<Long> get() = _totalPengeluaran

    private val _saldoAkhir = MutableLiveData<Long>()
    val saldoAkhir: LiveData<Long> get() = _saldoAkhir

    /**
     * --- FUNGSI BARU ---
     * Mengecek apakah email pengguna saat ini terdaftar sebagai bendahara.
     */
    fun checkUserAuthorization() {
        val currentUser = auth.currentUser
        if (currentUser?.email == null) {
            _isBendahara.value = false
            _authCheckComplete.value = true
            return
        }

        bendaharaCollection.whereEqualTo("email", currentUser.email).limit(1).get()
            .addOnSuccessListener { documents ->
                // Jika ditemukan dokumen (tidak kosong), maka dia adalah bendahara
                _isBendahara.value = !documents.isEmpty
                _authCheckComplete.value = true
            }
            .addOnFailureListener {
                // Jika gagal query, anggap bukan bendahara demi keamanan
                _isBendahara.value = false
                _authCheckComplete.value = true
            }
    }

    fun fetchTransaksi(year: Int, month: Int) {
        _isLoading.value = true
        val cal = Calendar.getInstance()
        cal.set(year, month, 1, 0, 0, 0)
        val startDate = cal.time
        cal.add(Calendar.MONTH, 1)
        val endDate = cal.time

        transaksiCollection
            .whereGreaterThanOrEqualTo("tanggal", Timestamp(startDate))
            .whereLessThan("tanggal", Timestamp(endDate))
            .orderBy("tanggal", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                _isLoading.value = false
                if (error != null) {
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

    fun tambahTransaksi(keterangan: String, nominal: Long, jenis: String, tanggal: Date, onResult: (Boolean) -> Unit) {
        val transaksiBaru = Transaksi(keterangan = keterangan, nominal = nominal, jenis = jenis, tanggal = Timestamp(tanggal))
        transaksiCollection.add(transaksiBaru)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    fun updateTransaksi(transaksiId: String, keterangan: String, nominal: Long, jenis: String, tanggal: Date, onResult: (Boolean) -> Unit) {
        val updatedData = mapOf("keterangan" to keterangan, "nominal" to nominal, "jenis" to jenis, "tanggal" to Timestamp(tanggal))
        transaksiCollection.document(transaksiId).update(updatedData)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    fun deleteTransaksi(transaksiId: String, onResult: (Boolean) -> Unit) {
        transaksiCollection.document(transaksiId).delete()
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    private fun calculateTotals(list: List<Transaksi>) {
        val pemasukan = list.filter { it.jenis == "Debit" }.sumOf { it.nominal ?: 0L }
        val pengeluaran = list.filter { it.jenis == "Kredit" }.sumOf { it.nominal ?: 0L }
        _totalPemasukan.value = pemasukan
        _totalPengeluaran.value = pengeluaran
        _saldoAkhir.value = pemasukan - pengeluaran
    }
}
