package com.khrlanamm.sitirta.ui.penagihan

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PenagihanViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Halaman Penagihan"
    }
    val text: LiveData<String> = _text
}