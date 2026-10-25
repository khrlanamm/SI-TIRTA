package com.khrlanamm.sitirta.ui.pembukuan

import com.khrlanamm.sitirta.data.Transaksi

data class TotalKas(
    val pemasukan: Long,
    val pengeluaran: Long,
    val saldoAkhir: Long
)

// "Debit" adalah pemasukan dan "Kredit" adalah pengeluaran. Jenis lain tidak dihitung.
fun hitungTotalKas(list: List<Transaksi>): TotalKas {
    val pemasukan = list.filter { it.jenis == "Debit" }.sumOf { it.nominal ?: 0L }
    val pengeluaran = list.filter { it.jenis == "Kredit" }.sumOf { it.nominal ?: 0L }
    return TotalKas(pemasukan, pengeluaran, pemasukan - pengeluaran)
}
