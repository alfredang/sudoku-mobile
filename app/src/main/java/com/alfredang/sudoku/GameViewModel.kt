package com.alfredang.sudoku

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/** Visual style of a single board cell. */
enum class CellStyle { EMPTY, GIVEN, PLAYER, REVEAL_GIVEN, REVEAL_CORRECT, REVEAL_AUTO }

enum class MessageType { ERROR, SUCCESS, INFO }

/** Haptic feedback events emitted to the UI layer. */
enum class Haptic { LIGHT, MEDIUM, ERROR, SUCCESS }

data class GameMessage(val text: String, val type: MessageType)

data class CellUi(
    val value: Int,
    val style: CellStyle,
    val error: Boolean,
)

/** Complete, immutable description of what the screen should show. */
data class UiState(
    val status: GameStatus = GameStatus.MENU,
    val difficulty: Difficulty? = null,
    val cells: List<CellUi> = emptyList(),
    val selectedIndex: Int = -1,
    val timeRemaining: Int = 0,
    val message: GameMessage? = null,
    val showGiveUp: Boolean = false,
    val numberPadEnabled: Boolean = false,
    val celebrate: Boolean = false,
)

class GameViewModel : ViewModel() {

    var ui by mutableStateOf(UiState())
        private set

    private val _haptics = MutableSharedFlow<Haptic>(extraBufferCapacity = 16)
    val haptics = _haptics.asSharedFlow()

    private var solution: Array<IntArray> = emptyBoard()
    private var board: Array<IntArray> = emptyBoard()
    private var given: Set<Int> = emptySet()
    private var timerJob: Job? = null

    private fun emptyBoard() = Array(9) { IntArray(9) }

    // ---- Game lifecycle -------------------------------------------------

    fun startGame(difficulty: Difficulty) {
        emit(Haptic.MEDIUM)
        val p = SudokuEngine.generate(difficulty)
        solution = p.solution
        board = Array(9) { r -> p.puzzle[r].copyOf() }
        given = p.given

        ui = UiState(
            status = GameStatus.PLAYING,
            difficulty = difficulty,
            cells = buildCells(selectedIndex = -1),
            selectedIndex = -1,
            timeRemaining = difficulty.durationSeconds,
            message = null,
            showGiveUp = true,
            numberPadEnabled = true,
            celebrate = false,
        )
        startTimer()
    }

    fun showMenu() {
        timerJob?.cancel()
        ui = UiState(status = GameStatus.MENU)
    }

    // ---- Player interaction ---------------------------------------------

    fun selectCell(index: Int) {
        if (ui.status != GameStatus.PLAYING) return
        emit(Haptic.LIGHT)
        val newSelected = if (ui.selectedIndex == index) -1 else index
        ui = ui.copy(selectedIndex = newSelected, cells = buildCells(newSelected))
    }

    /** Enter [num] (1-9) or erase with 0 into the selected cell. */
    fun enterNumber(num: Int) {
        if (ui.status != GameStatus.PLAYING) return

        val index = ui.selectedIndex
        if (index < 0) {
            ui = ui.copy(message = GameMessage("Pick a cell first! 👆", MessageType.INFO))
            return
        }

        val row = index / 9
        val col = index % 9
        if (given.contains(index)) {
            ui = ui.copy(message = GameMessage("That number is locked! 🔒", MessageType.INFO))
            return
        }

        board[row][col] = num

        val message: GameMessage?
        if (num != 0 && SudokuEngine.hasViolation(board, row, col, num)) {
            emit(Haptic.ERROR)
            message = GameMessage("Oops! That number doesn't fit here 😊", MessageType.ERROR)
        } else {
            emit(Haptic.MEDIUM)
            message = null
        }

        ui = ui.copy(cells = buildCells(index), message = message)

        if (num != 0 && SudokuEngine.isWin(board, solution)) {
            handleWin()
        }
    }

    fun giveUp() {
        if (ui.status != GameStatus.PLAYING) return
        timerJob?.cancel()
        revealSolution(
            GameStatus.GAVE_UP,
            GameMessage("No problem! Here's the solution. Try again! 💪", MessageType.INFO),
        )
    }

    // ---- Internal helpers -----------------------------------------------

    private fun handleWin() {
        timerJob?.cancel()
        emit(Haptic.SUCCESS)
        ui = ui.copy(
            status = GameStatus.WON,
            message = GameMessage("🎉 Great job! You solved the puzzle! 🌟", MessageType.SUCCESS),
            selectedIndex = -1,
            cells = buildCells(-1),
            showGiveUp = false,
            numberPadEnabled = false,
            celebrate = true,
        )
    }

    private fun handleTimeUp() {
        timerJob?.cancel()
        revealSolution(
            GameStatus.LOST,
            GameMessage("⏰ Time's up! Let's see the answers.", MessageType.INFO),
        )
    }

    private fun revealSolution(status: GameStatus, message: GameMessage) {
        val cells = ArrayList<CellUi>(81)
        for (r in 0 until 9) {
            for (c in 0 until 9) {
                val index = SudokuEngine.keyOf(r, c)
                val solValue = solution[r][c]
                val style = when {
                    given.contains(index) -> CellStyle.REVEAL_GIVEN
                    board[r][c] == solValue && board[r][c] != 0 -> CellStyle.REVEAL_CORRECT
                    else -> CellStyle.REVEAL_AUTO
                }
                cells.add(CellUi(solValue, style, error = false))
            }
        }
        ui = ui.copy(
            status = status,
            message = message,
            cells = cells,
            selectedIndex = -1,
            showGiveUp = false,
            numberPadEnabled = false,
            celebrate = false,
        )
    }

    /** Rebuild the immutable cell list from the mutable [board]. */
    private fun buildCells(selectedIndex: Int): List<CellUi> {
        val cells = ArrayList<CellUi>(81)
        for (r in 0 until 9) {
            for (c in 0 until 9) {
                val index = SudokuEngine.keyOf(r, c)
                val value = board[r][c]
                when {
                    given.contains(index) -> cells.add(CellUi(value, CellStyle.GIVEN, false))
                    value != 0 -> {
                        val error = SudokuEngine.hasViolation(board, r, c, value)
                        cells.add(CellUi(value, CellStyle.PLAYER, error))
                    }
                    else -> cells.add(CellUi(0, CellStyle.EMPTY, false))
                }
            }
        }
        return cells
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (ui.timeRemaining > 0) {
                delay(1000)
                val remaining = ui.timeRemaining - 1
                ui = ui.copy(timeRemaining = remaining)
                if (remaining <= 0) {
                    handleTimeUp()
                    break
                }
            }
        }
    }

    private fun emit(h: Haptic) {
        _haptics.tryEmit(h)
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }
}
