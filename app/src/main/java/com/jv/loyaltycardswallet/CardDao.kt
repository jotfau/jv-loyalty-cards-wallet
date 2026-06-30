package com.jv.loyaltycardswallet

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface CardDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(card: Card)

    @Update
    suspend fun update(card: Card)

    @Delete
    suspend fun delete(card: Card)

    @Query("SELECT * FROM card ORDER BY name ASC")
    fun getAllCards(): LiveData<List<Card>>

    @Query("SELECT * FROM card WHERE id = :id")
    suspend fun getCard(id: Int): Card?
}
