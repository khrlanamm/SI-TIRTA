package com.khrlanamm.sitirta.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.khrlanamm.sitirta.ui.auth.AccountActivity
import com.khrlanamm.sitirta.databinding.FragmentDashboardBinding

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

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

        // Memanggil fungsi yang sudah diperbarui
        setupClickListeners()
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
        binding.accountButton.setOnClickListener {
            navigateToAccountActivity()
        }

        binding.accountName.setOnClickListener {
            navigateToAccountActivity()
        }
    }

    private fun navigateToAccountActivity() {
        val intent = Intent(activity, AccountActivity::class.java)
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}