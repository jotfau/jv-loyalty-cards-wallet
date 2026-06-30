package com.jv.loyaltycardswallet

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class CardViewModel(private val repository: CardRepository) : ViewModel() {
    val allCards: LiveData<List<Card>> = repository.allCards

    fun insert(card: Card) = viewModelScope.launch {
        repository.insert(card)
    }

    fun update(card: Card) = viewModelScope.launch {
        repository.update(card)
    }

    fun delete(card: Card) = viewModelScope.launch {
        repository.delete(card)
    }
}
