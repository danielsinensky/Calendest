package com.example.calendest.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.calendest.data.local.entity.SavedGoogleAccountEntity

@Dao
interface SavedGoogleAccountDao {

    @Query("SELECT * FROM saved_google_accounts WHERE isRevoked = 0 ORDER BY lastUsedAt DESC")
    suspend fun getSavedAccounts(): List<SavedGoogleAccountEntity>

    @Query("SELECT * FROM saved_google_accounts WHERE isLoggedOut = 0 AND isRevoked = 0 ORDER BY lastUsedAt DESC")
    suspend fun getSwitchableAccounts(): List<SavedGoogleAccountEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAccount(account: SavedGoogleAccountEntity)

    @Query("UPDATE saved_google_accounts SET isLoggedOut = 1 WHERE email = :email")
    suspend fun markLoggedOut(email: String)

    @Query("UPDATE saved_google_accounts SET isLoggedOut = 0, isRevoked = 0, lastUsedAt = :time WHERE email = :email")
    suspend fun markLoggedIn(email: String, time: Long = System.currentTimeMillis())

    @Query("UPDATE saved_google_accounts SET isRevoked = 1, isLoggedOut = 1 WHERE email = :email")
    suspend fun markRevoked(email: String)
}