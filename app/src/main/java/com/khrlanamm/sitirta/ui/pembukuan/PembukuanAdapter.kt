package com.khrlanamm.sitirta.ui.pembukuan

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.khrlanamm.sitirta.R
import com.khrlanamm.sitirta.data.Transaksi
import com.khrlanamm.sitirta.databinding.ItemTransaksiBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class PembukuanAdapter : ListAdapter<Transaksi, PembukuanAdapter.TransaksiViewHolder>(TransaksiDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransaksiViewHolder {
        val binding = ItemTransaksiBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TransaksiViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransaksiViewHolder, position: Int) {
        val transaksi = getItem(position)
        holder.bind(transaksi)
    }

    inner class TransaksiViewHolder(private val binding: ItemTransaksiBinding) : RecyclerView.ViewHolder(binding.root) {
        private val localeID = Locale("in", "ID")
        private val currencyFormat = NumberFormat.getCurrencyInstance(localeID).apply {
            maximumFractionDigits = 0
            minimumFractionDigits = 0
        }
        private val dateFormat = SimpleDateFormat("dd MMMM yyyy", localeID)

        fun bind(transaksi: Transaksi) {
            binding.tvKeterangan.text = transaksi.keterangan
            binding.tvTanggal.text = transaksi.tanggal?.toDate()?.let { dateFormat.format(it) } ?: "Tanggal tidak valid"

            val nominalFormatted = currencyFormat.format(transaksi.nominal ?: 0)
            binding.tvNominal.text = nominalFormatted

            val colorRes = if (transaksi.jenis == "Debit") R.color.green_money else R.color.red_money
            binding.tvNominal.setTextColor(ContextCompat.getColor(binding.root.context, colorRes))
        }
    }

    class TransaksiDiffCallback : DiffUtil.ItemCallback<Transaksi>() {
        override fun areItemsTheSame(oldItem: Transaksi, newItem: Transaksi): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Transaksi, newItem: Transaksi): Boolean {
            return oldItem == newItem
        }
    }
}
