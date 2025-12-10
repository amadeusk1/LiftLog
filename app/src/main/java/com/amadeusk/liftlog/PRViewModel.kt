package com.amadeusk.liftlog

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.amadeusk.liftlog.data.PR
import com.amadeusk.liftlog.data.loadPrsFromFile
import com.amadeusk.liftlog.data.savePrsToFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class PRUiState(
    val prs: List<PR> = emptyList()
)

class PRViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(
        PRUiState(
            prs = loadPrsFromFile(application) // 🡐 load from file at startup
        )
    )
    val uiState: StateFlow<PRUiState> = _uiState

    private fun persist() {
        // Save current list of PRs to file
        savePrsToFile(getApplication(), _uiState.value.prs)
    }

    fun addPr(exercise: String, weight: Double, reps: Int, date: String) {
        val pr = PR(
            exercise = exercise.trim(),
            weight = weight,
            reps = reps,
            date = date.trim()
        )
        _uiState.update { it.copy(prs = it.prs + pr) }
        persist()
    }

    fun deletePr(pr: PR) {
        _uiState.update { it.copy(prs = it.prs - pr) }
        persist()
    }

    fun updatePr(updated: PR) {
        _uiState.update { state ->
            state.copy(
                prs = state.prs.map { pr ->
                    if (pr.id == updated.id) updated else pr
                }
            )
        }
        persist()
    }

}
