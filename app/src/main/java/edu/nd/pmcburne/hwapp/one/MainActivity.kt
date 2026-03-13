package edu.nd.pmcburne.hwapp.one

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import edu.nd.pmcburne.hwapp.one.data.GameDatabase
import edu.nd.pmcburne.hwapp.one.repository.ScoreRepository
import edu.nd.pmcburne.hwapp.one.ui.theme.HWStarterRepoTheme
import edu.nd.pmcburne.hwapp.one.viewmodel.ScoreViewModel
import edu.nd.pmcburne.hwapp.one.viewmodel.ScoreViewModelFactory
import java.util.Calendar

class MainActivity : ComponentActivity() {

    private val viewModel: ScoreViewModel by viewModels {
        ScoreViewModelFactory(
            ScoreRepository(
                GameDatabase.getDatabase(applicationContext).gameDao()
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            HWStarterRepoTheme {
                BasketballScoresApp(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasketballScoresApp(viewModel: ScoreViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Basketball Scores") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterChip(
                    selected = uiState.gender == "men",
                    onClick = { viewModel.setGender("men") },
                    label = { Text("Men") }
                )

                FilterChip(
                    selected = uiState.gender == "women",
                    onClick = { viewModel.setGender("women") },
                    label = { Text("Women") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        val cal = Calendar.getInstance()
                        val dialog = DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val formattedMonth = (month + 1).toString().padStart(2, '0')
                                val formattedDay = dayOfMonth.toString().padStart(2, '0')
                                val date = "$year-$formattedMonth-$formattedDay"
                                viewModel.setDate(date)
                            },
                            uiState.year,
                            uiState.month - 1,
                            uiState.day
                        )
                        dialog.show()
                    }
                ) {
                    Text("Pick Date: ${uiState.selectedDate}")
                }

                Button(
                    onClick = { viewModel.refreshScores() }
                ) {
                    Text("Refresh")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (uiState.games.isEmpty() && !uiState.isLoading) {
                Text("No games found for this date.")
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(uiState.games) { game ->
                        GameCard(game)
                    }
                }
            }
        }
    }
}

@Composable
fun GameCard(game: GameDisplay) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${game.awayTeam} @ ${game.homeTeam}",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text("Status: ${game.statusDisplay}")

            if (game.scoreLine.isNotBlank()) {
                Text(game.scoreLine)
            }

            if (game.detailLine.isNotBlank()) {
                Text(game.detailLine)
            }

            if (game.winner.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Winner: ${game.winner}")
            }
        }
    }
}

data class GameDisplay(
    val awayTeam: String,
    val homeTeam: String,
    val statusDisplay: String,
    val scoreLine: String,
    val detailLine: String,
    val winner: String
)