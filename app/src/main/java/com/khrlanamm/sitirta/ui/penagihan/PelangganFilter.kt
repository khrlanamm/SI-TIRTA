package com.khrlanamm.sitirta.ui.penagihan

import com.khrlanamm.sitirta.data.Pelanggan

// pjPrefix null atau kosong berarti "Semua" penanggung jawab.
fun filterPelanggan(
    list: List<Pelanggan>,
    pjPrefix: String?,
    searchQuery: String,
    paymentStatuses: List<String>
): List<Pelanggan> {
    var filteredList = list

    if (!pjPrefix.isNullOrEmpty()) {
        filteredList = filteredList.filter {
            it.id_pelanggan?.startsWith(pjPrefix, ignoreCase = true) == true
        }
    }

    if (searchQuery.isNotEmpty()) {
        filteredList = filteredList.filter {
            it.nama?.contains(searchQuery, ignoreCase = true) == true
        }
    }

    filteredList = filteredList.filter { pelanggan ->
        paymentStatuses.any { status ->
            pelanggan.status_pembayaran.equals(status, ignoreCase = true)
        }
    }

    return filteredList.sortedWith(
        compareBy(
            { it.status_penagihan != "Belum Ditagih" }, // false (Belum Ditagih) comes first
            { it.nama } // Then sort by name
        )
    )
}

// Pair berisi (sudah, belum) untuk "penagihan" dan "pembayaran".
fun hitungStatistik(list: List<Pelanggan>): Map<String, Pair<Int, Int>> {
    val total = list.size
    val sudahDitagih = list.count { it.status_penagihan.equals("Sudah Ditagih", ignoreCase = true) }
    val sudahDibayar = list.count { it.status_pembayaran.equals("Sudah Bayar", ignoreCase = true) }

    return mapOf(
        "penagihan" to Pair(sudahDitagih, total - sudahDitagih),
        "pembayaran" to Pair(sudahDibayar, total - sudahDibayar)
    )
}
