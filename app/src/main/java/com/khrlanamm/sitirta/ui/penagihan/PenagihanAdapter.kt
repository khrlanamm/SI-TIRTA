package com.khrlanamm.sitirta.ui.penagihan

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.khrlanamm.sitirta.R
import com.khrlanamm.sitirta.data.Pelanggan
import com.khrlanamm.sitirta.databinding.ItemPersonBinding
import com.khrlanamm.sitirta.ui.cetak.CetakActivity

class PenagihanAdapter(private var pelangganList: List<Pelanggan>) :
    RecyclerView.Adapter<PenagihanAdapter.PelangganViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PelangganViewHolder {
        val binding = ItemPersonBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PelangganViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PelangganViewHolder, position: Int) {
        val pelanggan = pelangganList[position]
        holder.bind(pelanggan)
    }

    override fun getItemCount(): Int = pelangganList.size

    fun updateData(newPelangganList: List<Pelanggan>) {
        pelangganList = newPelangganList
        notifyDataSetChanged()
    }

    inner class PelangganViewHolder(private val binding: ItemPersonBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val context: Context = binding.root.context

        fun bind(pelanggan: Pelanggan) {
            binding.tvPersonName.text = pelanggan.nama
            binding.tvPersonId.text = pelanggan.id_pelanggan
            binding.tvPersonRt.text = "RT ${pelanggan.rt}"

            // Atur status penagihan
            setupButtonStatus(
                binding.btnStatusTagihan,
                pelanggan.status_penagihan,
                "Sudah Ditagih",
                "Belum Ditagih",
                R.color.status_green,
                R.color.status_blue
            )

            // Atur status pembayaran
            setupButtonStatus(
                binding.btnStatusPembayaran,
                pelanggan.status_pembayaran,
                "Sudah Bayar",
                "Belum Bayar",
                R.color.status_green,
                R.color.status_blue
            )

            // Set OnClickListener
            itemView.setOnClickListener {
                val intent = Intent(context, CetakActivity::class.java)
                intent.putExtra(CetakActivity.EXTRA_PELANGGAN, pelanggan)
                context.startActivity(intent)
            }
        }

        private fun setupButtonStatus(
            button: com.google.android.material.button.MaterialButton,
            status: String?,
            textSudah: String,
            textBelum: String,
            colorSudah: Int,
            colorBelum: Int
        ) {
            if (status.equals(textSudah, ignoreCase = true)) {
                button.text = textSudah
                button.backgroundTintList = ContextCompat.getColorStateList(context, colorSudah)
            } else {
                button.text = textBelum
                button.backgroundTintList = ContextCompat.getColorStateList(context, colorBelum)
            }
        }
    }
}
