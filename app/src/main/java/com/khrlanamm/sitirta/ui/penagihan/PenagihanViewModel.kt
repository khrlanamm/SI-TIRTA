package com.khrlanamm.sitirta.ui.penagihan

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.khrlanamm.sitirta.data.Pelanggan
import com.khrlanamm.sitirta.data.PenanggungJawab

class PenagihanViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val pelangganCollection = db.collection("pelanggan")
    private val pjCollection = db.collection("penanggung_jawab")

    private var isDataFetched = false

    // Stores the original list from Firestore
    private var _originalPelangganList = listOf<Pelanggan>()
    private var _originalPenanggungJawabList = listOf<PenanggungJawab>() // Original PJ list

    // LiveData for the list of customers to be displayed in the UI
    private val _pelangganList = MutableLiveData<List<Pelanggan>>()
    val pelangganList: LiveData<List<Pelanggan>> get() = _pelangganList

    // LiveData for the list of Persons in Charge
    private val _penanggungJawabList = MutableLiveData<List<PenanggungJawab>>()
    val penanggungJawabList: LiveData<List<PenanggungJawab>> get() = _penanggungJawabList

    // LiveData for statistics
    private val _statistik = MutableLiveData<Map<String, Pair<Int, Int>>>()
    val statistik: LiveData<Map<String, Pair<Int, Int>>> get() = _statistik

    // Initial PJ filter is null, which means "Semua" (All)
    private var currentPjPrefix: String? = null
    private var currentSearchQuery: String = ""
    private var currentPaymentStatusFilter: List<String> = listOf("Sudah Bayar", "Belum Bayar")

    fun fetchData() {
        if (isDataFetched) return
        isDataFetched = true

        fetchDataFromFirestore()
        fetchPenanggungJawab()
    }

    private fun fetchPenanggungJawab() {
        pjCollection.addSnapshotListener { snapshots, error ->
            if (error != null) {
                // Handle error
                return@addSnapshotListener
            }

            if (snapshots != null) {
                _originalPenanggungJawabList = snapshots.toObjects(PenanggungJawab::class.java)
                _penanggungJawabList.value = _originalPenanggungJawabList
            }
        }
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

        // 1. Filter based on Person in Charge (PJ)
        // If currentPjPrefix is null, this filter is skipped (showing all)
        currentPjPrefix?.let { prefix ->
            if (prefix.isNotEmpty()) { // Ensure prefix is not empty
                filteredList = filteredList.filter {
                    it.id_pelanggan?.startsWith(prefix, ignoreCase = true) == true
                }
            }
        }

        // 2. Filter based on Name Search
        if (currentSearchQuery.isNotEmpty()) {
            filteredList = filteredList.filter {
                it.nama?.contains(currentSearchQuery, ignoreCase = true) == true
            }
        }

        // 3. Filter based on Payment Status
        filteredList = filteredList.filter { pelanggan ->
            currentPaymentStatusFilter.any { status ->
                pelanggan.status_pembayaran.equals(status, ignoreCase = true)
            }
        }

        val sortedList = filteredList.sortedWith(
            compareBy(
                { it.status_penagihan != "Belum Ditagih" }, // false (Belum Ditagih) comes first
                { it.nama } // Then sort by name
            )
        )

        _pelangganList.value = sortedList
        calculateStatistics(sortedList)
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
        val sudahDibayar = list.count { it.status_pembayaran.equals("Sudah Bayar", ignoreCase = true) }

        val belumDitagih = total - sudahDitagih
        val belumDibayar = total - sudahDibayar

        val stats = mapOf(
            "penagihan" to Pair(sudahDitagih, belumDitagih),
            "pembayaran" to Pair(sudahDibayar, belumDibayar)
        )
        _statistik.value = stats
    }

    fun setPjFilter(pjName: String?) {
        // If pjName is null (from "Semua"), set prefix to null.
        // Otherwise, find the prefix from the original list.
        currentPjPrefix = if (pjName == null) {
            null
        } else {
            _originalPenanggungJawabList.find { it.name == pjName }?.prefix
        }
        applyFilters()
    }

    fun setSearchQuery(query: String) {
        currentSearchQuery = query
        applyFilters()
    }

    fun setPaymentStatusFilter(statuses: List<String>) {
        currentPaymentStatusFilter = statuses
        applyFilters()
    }
}
