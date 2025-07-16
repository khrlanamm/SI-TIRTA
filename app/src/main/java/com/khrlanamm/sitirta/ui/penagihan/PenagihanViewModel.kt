package com.khrlanamm.sitirta.ui.penagihan

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.khrlanamm.sitirta.data.Pelanggan

class PenagihanViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val pelangganCollection = db.collection("pelanggan")

    // Menyimpan daftar asli dari Firestore
    private var _originalPelangganList = listOf<Pelanggan>()

    // LiveData untuk daftar pelanggan yang akan ditampilkan di UI
    private val _pelangganList = MutableLiveData<List<Pelanggan>>()
    val pelangganList: LiveData<List<Pelanggan>> get() = _pelangganList

    // LiveData untuk statistik
    private val _statistik = MutableLiveData<Map<String, Pair<Int, Int>>>()
    val statistik: LiveData<Map<String, Pair<Int, Int>>> get() = _statistik

    // Filter states
    private var currentPjPrefix: String? = null // null berarti tampilkan semua
    private var currentSearchQuery: String = ""
    private var currentPaymentStatusFilter: String? = null // null berarti tampilkan semua

    init {
        fetchDataFromFirestore()
    }

    private fun fetchDataFromFirestore() {
        pelangganCollection.addSnapshotListener { snapshots, error ->
            if (error != null) {
                // Handle error
                return@addSnapshotListener
            }

            if (snapshots != null) {
                _originalPelangganList = snapshots.toObjects(Pelanggan::class.java)
                applyFilters()
            }
        }
    }

    private fun applyFilters() {
        var filteredList = _originalPelangganList

        // 1. Filter berdasarkan Penanggung Jawab (PJ)
        currentPjPrefix?.let { prefix ->
            filteredList = filteredList.filter {
                it.id_pelanggan?.startsWith(prefix, ignoreCase = true) == true
            }
        }

        // 2. Filter berdasarkan Pencarian Nama
        if (currentSearchQuery.isNotEmpty()) {
            filteredList = filteredList.filter {
                it.nama?.contains(currentSearchQuery, ignoreCase = true) == true
            }
        }

        // 3. Filter berdasarkan Status Pembayaran
        currentPaymentStatusFilter?.let { status ->
            filteredList = filteredList.filter {
                it.status_pembayaran.equals(status, ignoreCase = true)
            }
        }

        _pelangganList.value = filteredList
        calculateStatistics(filteredList)
    }

    private fun calculateStatistics(list: List<Pelanggan>) {
        if (list.isEmpty()) {
            _statistik.value = mapOf(
                "penagihan" to Pair(0, 0),
                "pembayaran" to Pair(0, 0)
            )
            return
        }

        val total = list.size
        val sudahDitagih = list.count { it.status_penagihan.equals("Sudah Ditagih", ignoreCase = true) }
        val sudahDibayar = list.count { it.status_pembayaran.equals("Sudah Dibayar", ignoreCase = true) }

        val belumDitagih = total - sudahDitagih
        val belumDibayar = total - sudahDibayar

        val stats = mapOf(
            "penagihan" to Pair(sudahDitagih, belumDitagih),
            "pembayaran" to Pair(sudahDibayar, belumDibayar)
        )
        _statistik.value = stats
    }

    fun setPjFilter(pjName: String?) {
        currentPjPrefix = when (pjName) {
            "Ari Widodo" -> "AW"
            "Sukarsin" -> "SK"
            "Suparman" -> "SM"
            "Suparnen" -> "SN"
            else -> null // Jika null atau nama lain, tampilkan semua
        }
        applyFilters()
    }

    fun setSearchQuery(query: String) {
        currentSearchQuery = query
        applyFilters()
    }

    fun setPaymentStatusFilter(status: String?) {
        currentPaymentStatusFilter = status
        applyFilters()
    }
}
