package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pilot_accounts")
data class PilotAccountEntity(
  @PrimaryKey val phoneNumber: String,
  val pilotName: String,
  val passwordHash: String,
  val salt: String,
  val createdAtMillis: Long = System.currentTimeMillis(),
)
