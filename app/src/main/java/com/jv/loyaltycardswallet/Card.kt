package com.jv.loyaltycardswallet

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "card")
data class Card(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val cardNumber: String,
    val barcodeType: String = "CODE128",
    val notes: String = ""
)
