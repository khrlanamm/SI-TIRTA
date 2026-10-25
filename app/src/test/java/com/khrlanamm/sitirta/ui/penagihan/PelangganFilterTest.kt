package com.khrlanamm.sitirta.ui.penagihan

import com.khrlanamm.sitirta.data.Pelanggan
import org.junit.Assert.assertEquals
import org.junit.Test

class PelangganFilterTest {

    private val semuaStatus = listOf("Sudah Bayar", "Belum Bayar")

    private fun pelanggan(id: String, nama: String, tagih: String, bayar: String) =
        Pelanggan(id_pelanggan = id, nama = nama, status_penagihan = tagih, status_pembayaran = bayar)

    private val daftar = listOf(
        pelanggan("A01", "Budi", "Sudah Ditagih", "Sudah Bayar"),
        pelanggan("A02", "Ani", "Belum Ditagih", "Belum Bayar"),
        pelanggan("B01", "Citra", "Sudah Ditagih", "Belum Bayar"),
        pelanggan("B02", "Dedi", "Belum Ditagih", "Belum Bayar"),
    )

    @Test
    fun `customers not yet billed come first, then by name`() {
        val hasil = filterPelanggan(daftar, null, "", semuaStatus)

        assertEquals(listOf("Ani", "Dedi", "Budi", "Citra"), hasil.map { it.nama })
    }

    @Test
    fun `person in charge prefix filters on the customer id`() {
        val hasil = filterPelanggan(daftar, "b", "", semuaStatus)

        assertEquals(listOf("B02", "B01"), hasil.map { it.id_pelanggan })
    }

    @Test
    fun `empty prefix means every person in charge`() {
        assertEquals(4, filterPelanggan(daftar, "", "", semuaStatus).size)
    }

    @Test
    fun `name search ignores case`() {
        val hasil = filterPelanggan(daftar, null, "CIT", semuaStatus)

        assertEquals(listOf("Citra"), hasil.map { it.nama })
    }

    @Test
    fun `payment status filter keeps only matching customers`() {
        val hasil = filterPelanggan(daftar, null, "", listOf("Sudah Bayar"))

        assertEquals(listOf("Budi"), hasil.map { it.nama })
    }

    @Test
    fun `statistics count billed and paid customers`() {
        val statistik = hitungStatistik(daftar)

        assertEquals(Pair(2, 2), statistik["penagihan"])
        assertEquals(Pair(1, 3), statistik["pembayaran"])
    }

    @Test
    fun `statistics of an empty list are all zero`() {
        val statistik = hitungStatistik(emptyList())

        assertEquals(Pair(0, 0), statistik["penagihan"])
        assertEquals(Pair(0, 0), statistik["pembayaran"])
    }
}
