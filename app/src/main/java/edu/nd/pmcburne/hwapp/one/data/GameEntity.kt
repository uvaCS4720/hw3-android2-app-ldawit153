package edu.nd.pmcburne.hwapp.one.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey
    val gameId: String,
    val date: String,
    val gender: String,
    val homeTeam: String,
    val awayTeam: String,
    val homeScore: String,
    val awayScore: String,
    val status: String,
    val period: String,
    val clock: String,
    val startTime: String,
    val winner: String?
)