package com.alfredang.sudoku

/**
 * Difficulty levels. Each carries its countdown duration (seconds) and the
 * number of cells removed from the solved board to form the puzzle.
 *
 * Mirrors the original web game:
 *  - easy   : 15 minutes, 30 cells removed
 *  - medium : 10 minutes, 40 cells removed
 *  - hard   :  5 minutes, 50 cells removed
 */
enum class Difficulty(
    val label: String,
    val emoji: String,
    val description: String,
    val durationSeconds: Int,
    val cellsToRemove: Int,
) {
    EASY("Easy", "😊", "15 minutes", 900, 30),
    MEDIUM("Medium", "🤔", "10 minutes", 600, 40),
    HARD("Hard", "🔥", "5 minutes", 300, 50),
}

/** How a finished/abandoned puzzle resolved. */
enum class GameStatus { MENU, PLAYING, WON, LOST, GAVE_UP }

/**
 * Classification of a cell when the solution is revealed (give up / time up).
 */
enum class RevealKind { GIVEN, PLAYER_CORRECT, AUTO_REVEALED }

/** Immutable snapshot of one generated puzzle. */
data class Puzzle(
    val solution: Array<IntArray>,
    val puzzle: Array<IntArray>,
    val given: Set<Int>, // packed row*9+col indices that are pre-filled
)

/**
 * Pure Sudoku logic — generation, validation, win checking. No Android or UI
 * dependencies, so it is trivially unit-testable. Faithful port of the original
 * JavaScript implementation (backtracking generator + cell removal).
 */
object SudokuEngine {

    fun keyOf(row: Int, col: Int): Int = row * 9 + col

    /** Check whether placing [num] at ([row], [col]) is legal on [board]. */
    fun isValidPlacement(board: Array<IntArray>, row: Int, col: Int, num: Int): Boolean {
        for (c in 0 until 9) {
            if (c != col && board[row][c] == num) return false
        }
        for (r in 0 until 9) {
            if (r != row && board[r][col] == num) return false
        }
        val boxRow = (row / 3) * 3
        val boxCol = (col / 3) * 3
        for (r in boxRow until boxRow + 3) {
            for (c in boxCol until boxCol + 3) {
                if (r != row && c != col && board[r][c] == num) return false
            }
        }
        return true
    }

    /** Fill [board] in place using randomized backtracking. */
    private fun fillBoard(board: Array<IntArray>): Boolean {
        for (row in 0 until 9) {
            for (col in 0 until 9) {
                if (board[row][col] == 0) {
                    val numbers = (1..9).shuffled()
                    for (num in numbers) {
                        if (isValidPlacement(board, row, col, num)) {
                            board[row][col] = num
                            if (fillBoard(board)) return true
                            board[row][col] = 0
                        }
                    }
                    return false
                }
            }
        }
        return true
    }

    private fun generateSolvedBoard(): Array<IntArray> {
        val board = Array(9) { IntArray(9) }
        fillBoard(board)
        return board
    }

    private fun deepCopy(board: Array<IntArray>): Array<IntArray> =
        Array(9) { r -> board[r].copyOf() }

    /** Generate a fresh, solvable puzzle for the given [difficulty]. */
    fun generate(difficulty: Difficulty): Puzzle {
        val solution = generateSolvedBoard()
        val puzzle = deepCopy(solution)

        val positions = ArrayList<Int>(81)
        for (i in 0 until 81) positions.add(i)
        positions.shuffle()

        for (i in 0 until difficulty.cellsToRemove) {
            val p = positions[i]
            puzzle[p / 9][p % 9] = 0
        }

        val given = HashSet<Int>()
        for (r in 0 until 9) {
            for (c in 0 until 9) {
                if (puzzle[r][c] != 0) given.add(keyOf(r, c))
            }
        }
        return Puzzle(solution, puzzle, given)
    }

    /**
     * Does placing [num] at ([row], [col]) on [playerBoard] break a Sudoku rule
     * against the numbers currently on the board?
     */
    fun hasViolation(playerBoard: Array<IntArray>, row: Int, col: Int, num: Int): Boolean {
        for (c in 0 until 9) {
            if (c != col && playerBoard[row][c] == num) return true
        }
        for (r in 0 until 9) {
            if (r != row && playerBoard[r][col] == num) return true
        }
        val boxRow = (row / 3) * 3
        val boxCol = (col / 3) * 3
        for (r in boxRow until boxRow + 3) {
            for (c in boxCol until boxCol + 3) {
                if ((r != row || c != col) && playerBoard[r][c] == num) return true
            }
        }
        return false
    }

    /** True when every cell is filled and matches the solution. */
    fun isWin(playerBoard: Array<IntArray>, solution: Array<IntArray>): Boolean {
        for (r in 0 until 9) {
            for (c in 0 until 9) {
                if (playerBoard[r][c] == 0) return false
                if (playerBoard[r][c] != solution[r][c]) return false
            }
        }
        return true
    }
}
