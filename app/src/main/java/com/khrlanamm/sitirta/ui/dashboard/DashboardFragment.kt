package com.khrlanamm.sitirta.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.khrlanamm.sitirta.R
import com.khrlanamm.sitirta.data.PenanggungJawab
import com.khrlanamm.sitirta.databinding.FragmentDashboardBinding
import com.khrlanamm.sitirta.ui.auth.AccountActivity
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

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

        // NEW: Click listener for editing "Penanggung Jawab"
        binding.cardEditPenanggungJawab.setOnClickListener {
            fetchAndShowPenanggungJawabList()
        }

        binding.cardTambahPelanggan.setOnClickListener {
            // TODO: Implement add customer functionality later
            Toast.makeText(context, "Fitur ini akan segera tersedia", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToAccountActivity() {
        val intent = Intent(activity, AccountActivity::class.java)
        startActivity(intent)
    }

    // NEW: Function to fetch data from Firestore and show it in a list dialog
    private fun fetchAndShowPenanggungJawabList() {
        lifecycleScope.launch {
            try {
                val result = db.collection("penanggung_jawab").get().await()
                if (result.isEmpty) {
                    Toast.makeText(context, "Data penanggung jawab tidak ditemukan", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // FIXED: Use toList() on the result to get a List<QueryDocumentSnapshot>
                val documents = result.toList()
                val items = documents.map { doc ->
                    val pj = doc.toObject(PenanggungJawab::class.java)
                    "${pj?.prefix ?: "N/A"}: ${pj?.name ?: "Tanpa Nama"}"
                }.toTypedArray()

                AlertDialog.Builder(requireContext())
                    .setTitle("Pilih Penanggung Jawab untuk Diedit")
                    .setItems(items) { _, which ->
                        // Show edit dialog for the selected item
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

    // NEW: Function to show a dialog for editing the name
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

    // NEW: Function to update the name in Firestore
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
