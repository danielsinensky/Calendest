package com.example.calendest.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_google_accounts")
data class SavedGoogleAccountEntity(
    @PrimaryKey val email: String,
    val isLoggedOut: Boolean = false,
    val isRevoked: Boolean = false,
    val lastUsedAt: Long = System.currentTimeMillis()
)