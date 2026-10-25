package com.khrlanamm.sitirta.ui.cetak

import java.text.NumberFormat
import java.util.Locale

const val HARGA_PER_METER = 2500
const val IURAN_BULANAN = 5000

data class Tagihan(
    val meterTerpakai: Long,
    val tagihanAir: Long,
    val totalTagihan: Long
)

fun hitungTagihan(meterLalu: Long, meterIni: Long): Tagihan {
    val meterTerpakai = meterIni - meterLalu
    val tagihanAir = meterTerpakai * HARGA_PER_METER
    return Tagihan(meterTerpakai, tagihanAir, tagihanAir + IURAN_BULANAN)
}

// Meter air tidak pernah mundur, jadi angka bulan ini minimal sama dengan bulan lalu.
fun meterValid(meterLalu: Long, meterIni: Long): Boolean = meterIni >= meterLalu

fun formatRupiah(number: Double): String {
    val localeID = Locale("in", "ID")
    val formatRupiah = NumberFormat.getCurrencyInstance(localeID)
    formatRupiah.maximumFractionDigits = 0
    return formatRupiah.format(number)
}
