package com.khrlanamm.sitirta.ui.pembukuan

import com.khrlanamm.sitirta.data.Transaksi
import org.junit.Assert.assertEquals
import org.junit.Test

class TotalKasTest {

    @Test
    fun `debit is income, credit is spending, balance is the difference`() {
        val total = hitungTotalKas(
            listOf(
                Transaksi(keterangan = "Iuran Juni", nominal = 450_000, jenis = "Debit"),
                Transaksi(keterangan = "Pipa", nominal = 120_000, jenis = "Kredit"),
                Transaksi(keterangan = "Iuran susulan", nominal = 35_000, jenis = "Debit"),
            )
        )

        assertEquals(TotalKas(pemasukan = 485_000, pengeluaran = 120_000, saldoAkhir = 365_000), total)
    }

    @Test
    fun `missing amounts count as zero`() {
        val total = hitungTotalKas(listOf(Transaksi(nominal = null, jenis = "Debit")))

        assertEquals(TotalKas(0, 0, 0), total)
    }

    @Test
    fun `unknown types are ignored`() {
        val total = hitungTotalKas(listOf(Transaksi(nominal = 10_000, jenis = "Debet")))

        assertEquals(TotalKas(0, 0, 0), total)
    }

    @Test
    fun `spending can push the balance below zero`() {
        val total = hitungTotalKas(listOf(Transaksi(nominal = 50_000, jenis = "Kredit")))

        assertEquals(-50_000L, total.saldoAkhir)
    }
}
