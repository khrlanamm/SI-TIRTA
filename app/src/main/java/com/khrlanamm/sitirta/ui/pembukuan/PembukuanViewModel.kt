package com.khrlanamm.sitirta.ui.pembukuan

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PembukuanViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Halaman Pembukuan"
    }
    val text: LiveData<String> = _text
}