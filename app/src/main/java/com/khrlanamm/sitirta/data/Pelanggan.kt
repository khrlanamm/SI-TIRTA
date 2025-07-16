package com.khrlanamm.sitirta.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Pelanggan(
    var id_pelanggan: String? = null,
    var nama: String? = null,
    var rt: String? = null, // Pastikan ini sudah String
    var status_penagihan: String? = null,
    var status_pembayaran: String? = null,
    // PERUBAHAN DI SINI: Tambahkan '?' setelah Long
    var meteran: MutableMap<String, Long?>? = null
) : Parcelable
