package com.jv.loyaltycardswallet

import androidx.lifecycle.LiveData

class CardRepository(private val cardDao: CardDao) {
    val allCards: LiveData<List<Card>> = cardDao.getAllCards()

    suspend fun insert(card: Card) {
        cardDao.insert(card)
    }

    suspend fun update(card: Card) {
        cardDao.update(card)
    }

    suspend fun delete(card: Card) {
        cardDao.delete(card)
    }

    suspend fun getCard(id: Int): Card? {
        return cardDao.getCard(id)
    }
}
