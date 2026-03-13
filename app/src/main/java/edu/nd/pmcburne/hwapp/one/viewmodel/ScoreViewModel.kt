package edu.nd.pmcburne.hwapp.one.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import edu.nd.pmcburne.hwapp.one.GameDisplay
import edu.nd.pmcburne.hwapp.one.repository.ScoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

data class ScoreUiState(
    val selectedDate: String = "",
    val gender: String = "men",
    val games: List<GameDisplay> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val year: Int = 2026,
    val month: Int = 1,
    val day: Int = 1
)

class ScoreViewModel(
    private val repository: ScoreRepository
) : ViewModel() {

    private val todayCalendar = Calendar.getInstance()

    private val _uiState = MutableStateFlow(
        ScoreUiState(
            selectedDate = buildTodayString(),
            year = todayCalendar.get(Calendar.YEAR),
            month = todayCalendar.get(Calendar.MONTH) + 1,
            day = todayCalendar.get(Calendar.DAY_OF_MONTH)
        )
    )
    val uiState: StateFlow<ScoreUiState> = _uiState.asStateFlow()

    init {
        refreshScores()
    }

    fun setGender(gender: String) {
        _uiState.value = _uiState.value.copy(gender = gender)
        refreshScores()
    }

    fun setDate(date: String) {
        val parts = date.split("-")
        _uiState.value = _uiState.value.copy(
            selectedDate = date,
            year = parts[0].toInt(),
            month = parts[1].toInt(),
            day = parts[2].toInt()
        )
        refreshScores()
    }

    fun refreshScores() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            try {
                val games = repository.getScores(
                    _uiState.value.selectedDate,
                    _uiState.value.gender
                )

                _uiState.value = _uiState.value.copy(
                    games = games.map { game ->
                        val statusLower = game.status.lowercase()

                        val statusDisplay = when {
                            statusLower.contains("final") -> "Final"
                            statusLower.contains("live") || statusLower.contains("in") -> "In Progress"
                            statusLower.contains("pre") -> "Upcoming"
                            else -> game.status
                        }

                        val scoreLine = if (game.homeScore.isNotBlank() || game.awayScore.isNotBlank()) {
                            "${game.awayTeam}: ${game.awayScore}   ${game.homeTeam}: ${game.homeScore}"
                        } else {
                            ""
                        }

                        val detailLine = when {
                            statusDisplay == "Upcoming" -> "Start Time: ${game.startTime}"
                            statusDisplay == "In Progress" -> "Period: ${game.period}   Time Remaining: ${game.clock}"
                            statusDisplay == "Final" -> "Final"
                            else -> ""
                        }

                        GameDisplay(
                            awayTeam = game.awayTeam,
                            homeTeam = game.homeTeam,
                            statusDisplay = statusDisplay,
                            scoreLine = scoreLine,
                            detailLine = detailLine,
                            winner = game.winner ?: ""
                        )
                    },
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Could not load scores."
                )
            }
        }
    }

    private fun buildTodayString(): String {
        val year = todayCalendar.get(Calendar.YEAR)
        val month = (todayCalendar.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        val day = todayCalendar.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
        return "$year-$month-$day"
    }
}

class ScoreViewModelFactory(
    private val repository: ScoreRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScoreViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ScoreViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}