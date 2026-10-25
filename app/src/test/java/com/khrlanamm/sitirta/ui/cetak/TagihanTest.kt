package com.khrlanamm.sitirta.ui.cetak

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TagihanTest {

    @Test
    fun `bill is usage times the price per meter plus the monthly fee`() {
        val tagihan = hitungTagihan(meterLalu = 120, meterIni = 132)

        assertEquals(12L, tagihan.meterTerpakai)
        assertEquals(12L * HARGA_PER_METER, tagihan.tagihanAir)
        assertEquals(12L * HARGA_PER_METER + IURAN_BULANAN, tagihan.totalTagihan)
    }

    @Test
    fun `current tariff gives 35000 for 12 cubic meters`() {
        assertEquals(35_000L, hitungTagihan(120, 132).totalTagihan)
    }

    @Test
    fun `no usage still pays the monthly fee`() {
        val tagihan = hitungTagihan(meterLalu = 87, meterIni = 87)

        assertEquals(0L, tagihan.tagihanAir)
        assertEquals(IURAN_BULANAN.toLong(), tagihan.totalTagihan)
    }

    @Test
    fun `meter reading may stay the same or go up`() {
        assertTrue(meterValid(meterLalu = 50, meterIni = 50))
        assertTrue(meterValid(meterLalu = 50, meterIni = 51))
    }

    @Test
    fun `meter reading may not go down`() {
        assertFalse(meterValid(meterLalu = 50, meterIni = 49))
    }

    @Test
    fun `rupiah has no decimals and uses dots for thousands`() {
        val text = formatRupiah(35_000.0)

        assertTrue(text, text.startsWith("Rp"))
        assertTrue(text, text.endsWith("35.000"))
    }
}
