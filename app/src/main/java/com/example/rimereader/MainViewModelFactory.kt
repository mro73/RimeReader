package com.example.rimereader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

// Nasza fabryka przyjmuje narzędzia (baza danych i repozytorium)
class MainViewModelFactory(
    private val bookmarkDao: BookmarkDao,
    private val audioRepository: AudioRepository
) : ViewModelProvider.Factory {

    // I tworzy z nich gotowego do pracy ViewModela
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(bookmarkDao, audioRepository) as T
        }
        throw IllegalArgumentException("Nieznana klasa ViewModel")
    }
}