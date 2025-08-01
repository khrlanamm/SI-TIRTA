package com.khrlanamm.sitirta.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.khrlanamm.sitirta.R
import com.khrlanamm.sitirta.data.PenanggungJawab
import com.khrlanamm.sitirta.databinding.FragmentDashboardBinding
import com.khrlanamm.sitirta.ui.auth.AccountActivity
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()

    private var backPressedTime: Long = 0
    private var backToast: Toast? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activityIntent = requireActivity().intent
        val userName = activityIntent.getStringExtra("USER_NAME")
        val userPhotoUrl = activityIntent.getStringExtra("USER_PHOTO_URL")

        setupWelcomeText(userName)
        setupProfilePicture(userPhotoUrl)
        setupClickListeners()
        setupOnBackPressed()
    }

    private fun setupWelcomeText(userName: String?) {
        if (userName != null) {
            val displayName = userName.split(" ").take(2).joinToString(" ")
            binding.accountName.text = "Halo, $displayName!"
        }
    }

    private fun setupProfilePicture(photoUrl: String?) {
        if (photoUrl != null) {
            Glide.with(this)
                .load(photoUrl)
                .circleCrop()
                .into(binding.accountButton)
        }
    }

    private fun setupClickListeners() {
        binding.accountButton.setOnClickListener { navigateToAccountActivity() }
        binding.accountName.setOnClickListener { navigateToAccountActivity() }

        binding.cardEditPenanggungJawab.setOnClickListener {
            fetchAndShowPenanggungJawabList()
        }

        // UPDATED: Call the new function to add a customer
        binding.cardTambahPelanggan.setOnClickListener {
            showAddCustomerDialog()
        }
    }

    private fun navigateToAccountActivity() {
        val intent = Intent(activity, AccountActivity::class.java)
        startActivity(intent)
    }

    private fun fetchAndShowPenanggungJawabList() {
        lifecycleScope.launch {
            try {
                val result = db.collection("penanggung_jawab").get().await()
                if (result.isEmpty) {
                    Toast.makeText(context, "Data penanggung jawab tidak ditemukan", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val documents = result.toList()
                val items = documents.map { doc ->
                    val pj = doc.toObject(PenanggungJawab::class.java)
                    "${pj?.prefix ?: "N/A"}: ${pj?.name ?: "Tanpa Nama"}"
                }.toTypedArray()

                AlertDialog.Builder(requireContext())
                    .setTitle("Pilih Penanggung Jawab untuk Diedit")
                    .setItems(items) { _, which ->
                        val selectedDoc = documents[which]
                        showEditPenanggungJawabDialog(selectedDoc)
                    }
                    .setNegativeButton("Batal", null)
                    .show()

            } catch (e: Exception) {
                Toast.makeText(context, "Gagal memuat data: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showEditPenanggungJawabDialog(doc: QueryDocumentSnapshot) {
        val pj = doc.toObject(PenanggungJawab::class.java)
        val currentName = pj?.name ?: ""
        val prefix = pj?.prefix ?: "N/A"

        val editText = EditText(requireContext()).apply {
            setText(currentName)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(50, 20, 50, 20)
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Nama (Prefix: $prefix)")
            .setView(editText)
            .setPositiveButton("Simpan") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty() && newName != currentName) {
                    updatePenanggungJawabName(doc.id, newName)
                } else if (newName.isEmpty()) {
                    Toast.makeText(context, "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun updatePenanggungJawabName(docId: String, newName: String) {
        lifecycleScope.launch {
            try {
                db.collection("penanggung_jawab").document(docId)
                    .update("name", newName)
                    .await()
                Toast.makeText(context, "Nama berhasil diperbarui", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal memperbarui nama: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // NEW: Function to show dialog for adding a new customer
    private fun showAddCustomerDialog() {
        lifecycleScope.launch {
            try {
                // Fetch PJ list for the spinner
                val pjResult = db.collection("penanggung_jawab").get().await()
                if (pjResult.isEmpty) {
                    Toast.makeText(context, "Data Penanggung Jawab tidak ditemukan. Tambah PJ terlebih dahulu.", Toast.LENGTH_LONG).show()
                    return@launch
                }
                val pjList = pjResult.toObjects(PenanggungJawab::class.java)
                val pjDisplayList = pjList.map { "${it.prefix} - ${it.name}" }

                // Create layout for the dialog
                val layout = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(50)
                }

                val etNama = EditText(requireContext()).apply { hint = "Nama Pelanggan" }
                val etRt = EditText(requireContext()).apply { hint = "RT (Contoh: 1)" }
                val etMeterLalu = EditText(requireContext()).apply {
                    hint = "Meter Bulan Lalu"
                    inputType = InputType.TYPE_CLASS_NUMBER
                }
                val spinnerPj = Spinner(requireContext())
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, pjDisplayList)
                spinnerPj.adapter = adapter

                layout.addView(etNama)
                layout.addView(etRt)
                layout.addView(etMeterLalu)
                layout.addView(TextView(requireContext()).apply { text = "\nPenanggung Jawab:" })
                layout.addView(spinnerPj)

                AlertDialog.Builder(requireContext())
                    .setTitle("Tambah Pelanggan Baru")
                    .setView(layout)
                    .setPositiveButton("Simpan") { _, _ ->
                        val nama = etNama.text.toString().trim()
                        val rt = etRt.text.toString().trim()
                        val meterLaluStr = etMeterLalu.text.toString().trim()
                        val selectedPjPosition = spinnerPj.selectedItemPosition

                        if (nama.isEmpty() || rt.isEmpty() || meterLaluStr.isEmpty()) {
                            Toast.makeText(context, "Semua kolom harus diisi", Toast.LENGTH_SHORT).show()
                            return@setPositiveButton
                        }

                        val selectedPj = pjList[selectedPjPosition]
                        val meterLalu = meterLaluStr.toLong()

                        generateNewCustomerIdAndSave(selectedPj, nama, rt, meterLalu)
                    }
                    .setNegativeButton("Batal", null)
                    .show()

            } catch (e: Exception) {
                Toast.makeText(context, "Gagal memuat dialog: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // NEW: Function to generate new ID and save customer data
    private fun generateNewCustomerIdAndSave(pj: PenanggungJawab, nama: String, rt: String, meterLalu: Long) {
        lifecycleScope.launch {
            try {
                val prefix = pj.prefix ?: throw IllegalStateException("Prefix PJ tidak boleh null")

                // Find the last customer with the same prefix
                val query = db.collection("pelanggan")
                    .whereGreaterThanOrEqualTo("id_pelanggan", prefix)
                    .whereLessThan("id_pelanggan", prefix + '\uf8ff')
                    .orderBy("id_pelanggan", Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .await()

                var lastNumber = 0
                if (!query.isEmpty) {
                    val lastId = query.documents[0].id
                    lastNumber = lastId.removePrefix(prefix).toIntOrNull() ?: 0
                }

                val newNumber = lastNumber + 1
                val newId = "$prefix${String.format("%03d", newNumber)}"

                // Prepare data to be saved
                val newCustomerData = hashMapOf(
                    "id_pelanggan" to newId,
                    "nama" to nama,
                    "rt" to rt,
                    "meteran" to hashMapOf(
                        "bulan_lalu" to meterLalu,
                        "bulan_ini" to null
                    ),
                    "status_penagihan" to "Belum Ditagih",
                    "status_pembayaran" to "Belum Bayar"
                )

                // Save new customer with the generated ID as the document ID
                db.collection("pelanggan").document(newId).set(newCustomerData).await()
                Toast.makeText(context, "Pelanggan baru berhasil ditambahkan: $newId", Toast.LENGTH_LONG).show()

            } catch (e: Exception) {
                Toast.makeText(context, "Gagal menyimpan pelanggan: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }


    private fun setupOnBackPressed() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (backPressedTime + 2000 > System.currentTimeMillis()) {
                    backToast?.cancel()
                    requireActivity().finish()
                    return
                } else {
                    backToast = Toast.makeText(context, "Tekan sekali lagi untuk keluar", Toast.LENGTH_SHORT)
                    backToast?.show()
                }
                backPressedTime = System.currentTimeMillis()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
