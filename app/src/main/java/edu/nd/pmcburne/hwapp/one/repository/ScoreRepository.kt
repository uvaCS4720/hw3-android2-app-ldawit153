package edu.nd.pmcburne.hwapp.one.repository

import android.util.Log
import com.google.gson.JsonParser
import edu.nd.pmcburne.hwapp.one.data.GameDao
import edu.nd.pmcburne.hwapp.one.data.GameEntity
import edu.nd.pmcburne.hwapp.one.network.RetrofitInstance

class ScoreRepository(
    private val gameDao: GameDao
) {

    suspend fun getScores(date: String, gender: String): List<GameEntity> {
        val parts = date.split("-")
        val year = parts[0]
        val month = parts[1]
        val day = parts[2]

        try {
            val rawJson = RetrofitInstance.api.getScoresRaw(gender, year, month, day)
            Log.d("API_DEBUG", "RAW JSON: $rawJson")

            val root = JsonParser.parseString(rawJson).asJsonObject

            if (!root.has("games")) {
                Log.e("API_DEBUG", "No 'games' field in response")
                throw Exception("No games field in API response")
            }

            val gamesArray = root.getAsJsonArray("games")
            Log.d("API_DEBUG", "games count = ${gamesArray.size()}")

            val parsedGames = gamesArray.mapNotNull { gameElement ->
                try {
                    val obj = gameElement.asJsonObject
                    val gameObj = obj.getAsJsonObject("game") ?: return@mapNotNull null

                    val gameId = gameObj.get("gameID")?.asString ?: return@mapNotNull null

                    val homeObj = gameObj.getAsJsonObject("home")
                    val awayObj = gameObj.getAsJsonObject("away")

                    val homeTeam = homeObj
                        ?.getAsJsonObject("names")
                        ?.get("short")
                        ?.asString ?: "Home Team"

                    val awayTeam = awayObj
                        ?.getAsJsonObject("names")
                        ?.get("short")
                        ?.asString ?: "Away Team"

                    val homeScore = homeObj?.get("score")?.asString ?: ""
                    val awayScore = awayObj?.get("score")?.asString ?: ""

                    val status = gameObj.get("gameState")?.asString ?: "unknown"
                    val period = gameObj.get("currentPeriod")?.asString ?: ""
                    val clock = gameObj.get("contestClock")?.asString ?: ""
                    val startTime = gameObj.get("startTime")?.asString ?: ""

                    val winner = when {
                        homeObj?.get("winner")?.asBoolean == true -> homeTeam
                        awayObj?.get("winner")?.asBoolean == true -> awayTeam
                        else -> null
                    }

                    GameEntity(
                        gameId = gameId,
                        date = date,
                        gender = gender,
                        homeTeam = homeTeam,
                        awayTeam = awayTeam,
                        homeScore = homeScore,
                        awayScore = awayScore,
                        status = status,
                        period = period,
                        clock = clock,
                        startTime = startTime,
                        winner = winner
                    )
                } catch (e: Exception) {
                    Log.e("API_DEBUG", "Failed to parse one game", e)
                    null
                }
            }

            Log.d("API_DEBUG", "parsed games count = ${parsedGames.size}")
            gameDao.insertAll(parsedGames)
            return parsedGames

        } catch (e: Exception) {
            Log.e("API_DEBUG", "NETWORK OR PARSE FAILURE", e)

            val cachedGames = gameDao.getGamesByDateAndGender(date, gender)
            Log.d("API_DEBUG", "cached games count = ${cachedGames.size}")

            if (cachedGames.isNotEmpty()) {
                return cachedGames
            } else {
                throw e
            }
        }
    }
}