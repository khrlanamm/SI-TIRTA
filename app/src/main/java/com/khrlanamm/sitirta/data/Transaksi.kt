package com.khrlanamm.sitirta.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Transaksi(
    @DocumentId
    val id: String? = null,
    val tanggal: Timestamp? = null,
    val keterangan: String? = null,
    val nominal: Long? = 0, // Gunakan Long untuk nominal uang
    val jenis: String? = null // "Debit" atau "Kredit"
) {
    constructor() : this(null, null, null, 0, null)
}
