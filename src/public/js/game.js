/* ===========================
   GAME STATE
   =========================== */

// Safe haptic call (defined by capacitor-init.js, no-op on web)
function haptic(style) {
  if (window.triggerHaptic) window.triggerHaptic(style);
}

// Current game state: 'menu', 'playing', 'won', 'lost', 'gaveup'
let gameState = 'menu';

// The complete solved board (9x9 2D array)
let solution = [];

// The puzzle with cells removed (0 = empty)
let puzzle = [];

// The player's current board (mutable copy of puzzle)
let playerBoard = [];

// Set of "row,col" strings for pre-filled cells that can't be changed
let givenCells = new Set();

// Currently selected cell { row, col } or null
let selectedCell = null;

// Timer
let timerInterval = null;
let timeRemaining = 0;

// Current difficulty
let difficulty = '';

// Timer durations in seconds for each difficulty
const TIMER_DURATIONS = { easy: 900, medium: 600, hard: 300 };

// Number of cells to remove for each difficulty
const CELLS_TO_REMOVE = { easy: 30, medium: 40, hard: 50 };

/* ===========================
   SUDOKU GENERATION
   =========================== */

/**
 * Shuffle an array in-place using the Fisher-Yates algorithm.
 * This ensures random puzzle generation each time.
 */
function shuffleArray(arr) {
  for (let i = arr.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [arr[i], arr[j]] = [arr[j], arr[i]];
  }
  return arr;
}

/**
 * Check if placing `num` at position (row, col) is valid.
 * Checks the three Sudoku constraints: row, column, and 3x3 box.
 */
function isValidPlacement(board, row, col, num) {
  // Check row — no duplicate in the same row
  for (let c = 0; c < 9; c++) {
    if (c !== col && board[row][c] === num) return false;
  }

  // Check column — no duplicate in the same column
  for (let r = 0; r < 9; r++) {
    if (r !== row && board[r][col] === num) return false;
  }

  // Check 3x3 box — no duplicate in the same box
  const boxRow = Math.floor(row / 3) * 3;
  const boxCol = Math.floor(col / 3) * 3;
  for (let r = boxRow; r < boxRow + 3; r++) {
    for (let c = boxCol; c < boxCol + 3; c++) {
      if (r !== row && c !== col && board[r][c] === num) return false;
    }
  }

  return true;
}

/**
 * Recursively fill the board using backtracking.
 * Tries shuffled numbers 1-9 in each empty cell for randomness.
 * Returns true if the board is successfully filled.
 */
function fillBoard(board) {
  // Find the next empty cell
  for (let row = 0; row < 9; row++) {
    for (let col = 0; col < 9; col++) {
      if (board[row][col] === 0) {
        // Try numbers 1-9 in random order
        const numbers = shuffleArray([1, 2, 3, 4, 5, 6, 7, 8, 9]);
        for (const num of numbers) {
          if (isValidPlacement(board, row, col, num)) {
            board[row][col] = num;
            if (fillBoard(board)) return true;
            board[row][col] = 0; // Backtrack
          }
        }
        return false; // No valid number found, trigger backtracking
      }
    }
  }
  return true; // All cells filled successfully
}

/**
 * Verify that a completed board is a valid Sudoku solution.
 * Every row, column, and 3x3 box must contain digits 1-9 exactly once (sum = 45).
 */
function isValidSolution(board) {
  for (let i = 0; i < 9; i++) {
    const rowSet = new Set();
    const colSet = new Set();
    for (let j = 0; j < 9; j++) {
      rowSet.add(board[i][j]);
      colSet.add(board[j][i]);
    }
    // Each row and column must have exactly {1,2,3,4,5,6,7,8,9}
    if (rowSet.size !== 9 || colSet.size !== 9) return false;
    for (let n = 1; n <= 9; n++) {
      if (!rowSet.has(n) || !colSet.has(n)) return false;
    }
  }
  // Check all nine 3x3 boxes
  for (let boxRow = 0; boxRow < 9; boxRow += 3) {
    for (let boxCol = 0; boxCol < 9; boxCol += 3) {
      const boxSet = new Set();
      for (let r = boxRow; r < boxRow + 3; r++) {
        for (let c = boxCol; c < boxCol + 3; c++) {
          boxSet.add(board[r][c]);
        }
      }
      if (boxSet.size !== 9) return false;
      for (let n = 1; n <= 9; n++) {
        if (!boxSet.has(n)) return false;
      }
    }
  }
  return true;
}

/**
 * Generate a complete, valid 9x9 Sudoku solution.
 * Creates an empty grid, fills it using backtracking, and verifies correctness.
 */
function generateSolvedBoard() {
  const board = Array.from({ length: 9 }, () => Array(9).fill(0));
  fillBoard(board);

  // Safety check: regenerate if the solution is invalid (should never happen)
  if (!isValidSolution(board)) {
    console.warn('Generated board failed validation, regenerating...');
    return generateSolvedBoard();
  }

  return board;
}

/**
 * Deep-copy a 9x9 2D array.
 */
function deepCopy(board) {
  return board.map(row => [...row]);
}

/**
 * Create a puzzle by removing cells from the solved board.
 * The number of removed cells depends on the difficulty.
 */
function createPuzzle(solvedBoard, diff) {
  const puzzleBoard = deepCopy(solvedBoard);
  const removeCount = CELLS_TO_REMOVE[diff];

  // Build a list of all 81 cell positions and shuffle it
  const positions = [];
  for (let r = 0; r < 9; r++) {
    for (let c = 0; c < 9; c++) {
      positions.push([r, c]);
    }
  }
  shuffleArray(positions);

  // Remove cells
  for (let i = 0; i < removeCount; i++) {
    const [r, c] = positions[i];
    puzzleBoard[r][c] = 0;
  }

  return puzzleBoard;
}

/* ===========================
   GAME INITIALIZATION
   =========================== */

/**
 * Start a new game with the given difficulty.
 * Generates puzzle, sets up state, renders board, starts timer.
 */
function startGame(diff) {
  haptic('medium');
  difficulty = diff;
  gameState = 'playing';

  // Generate a fresh puzzle
  solution = generateSolvedBoard();
  puzzle = createPuzzle(solution, difficulty);
  playerBoard = deepCopy(puzzle);

  // Track which cells are pre-filled (given)
  givenCells.clear();
  for (let r = 0; r < 9; r++) {
    for (let c = 0; c < 9; c++) {
      if (puzzle[r][c] !== 0) {
        givenCells.add(r + ',' + c);
      }
    }
  }

  selectedCell = null;

  // Set up timer
  timeRemaining = TIMER_DURATIONS[difficulty];

  // Update UI
  const badge = document.getElementById('difficulty-badge');
  badge.className = difficulty;
  const labels = { easy: '😊 Easy', medium: '🤔 Medium', hard: '🔥 Hard' };
  badge.textContent = labels[difficulty];

  // Switch screens
  document.getElementById('menu-screen').classList.add('hidden');
  document.getElementById('game-screen').classList.remove('hidden');

  // Enable controls
  document.getElementById('btn-giveup').classList.remove('hidden');
  setNumberPadEnabled(true);

  clearMessage();
  renderBoard();
  updateTimerDisplay();
  startTimer();
}

/**
 * Return to the difficulty selection menu.
 */
function showMenu() {
  if (timerInterval) clearInterval(timerInterval);
  gameState = 'menu';
  selectedCell = null;

  document.getElementById('game-screen').classList.add('hidden');
  document.getElementById('menu-screen').classList.remove('hidden');
  clearMessage();
}

/* ===========================
   BOARD RENDERING
   =========================== */

/**
 * Render the full 9x9 board by creating 81 cell elements.
 * Each cell gets appropriate classes based on its state.
 */
function renderBoard() {
  const board = document.getElementById('board');
  board.innerHTML = '';
  board.classList.remove('celebrate');

  for (let row = 0; row < 9; row++) {
    for (let col = 0; col < 9; col++) {
      const cell = document.createElement('div');
      cell.className = 'cell';
      cell.dataset.row = row;
      cell.dataset.col = col;

      const value = playerBoard[row][col];
      const key = row + ',' + col;

      // Determine cell type and styling
      if (givenCells.has(key)) {
        cell.classList.add('given');
        cell.textContent = value;
      } else if (value !== 0) {
        cell.classList.add('player');
        cell.textContent = value;
        // Check for rule violations
        if (hasViolation(row, col, value)) {
          cell.classList.add('error');
        }
      }

      // Highlight selected cell
      if (selectedCell && selectedCell.row === row && selectedCell.col === col) {
        cell.classList.add('selected');
      }

      // Add thicker borders at 3x3 box boundaries
      if (col === 2 || col === 5) cell.classList.add('box-border-right');
      if (row === 2 || row === 5) cell.classList.add('box-border-bottom');

      // Click handler to select this cell
      cell.addEventListener('click', () => selectCell(row, col));

      board.appendChild(cell);
    }
  }
}

/**
 * Handle clicking on a cell to select it.
 */
function selectCell(row, col) {
  if (gameState !== 'playing') return;
  haptic('light');

  // Toggle selection: clicking the same cell deselects it
  if (selectedCell && selectedCell.row === row && selectedCell.col === col) {
    selectedCell = null;
  } else {
    selectedCell = { row, col };
  }
  renderBoard();
}

/* ===========================
   NUMBER ENTRY & VALIDATION
   =========================== */

/**
 * Enter a number (1-9) or erase (0) in the selected cell.
 */
function enterNumber(num) {
  if (gameState !== 'playing') return;

  // Must have a cell selected
  if (!selectedCell) {
    showMessage('Pick a cell first! 👆', 'info');
    return;
  }

  const { row, col } = selectedCell;
  const key = row + ',' + col;

  // Can't change given (pre-filled) cells
  if (givenCells.has(key)) {
    showMessage('That number is locked! 🔒', 'info');
    return;
  }

  // Place the number (or erase with 0)
  playerBoard[row][col] = num;

  // Check for violations (only when placing a number, not erasing)
  if (num !== 0 && hasViolation(row, col, num)) {
    haptic('error');
    showMessage("Oops! That number doesn't fit here 😊", 'error');
  } else {
    haptic('medium');
    clearMessage();
  }

  renderBoard();

  // Check if the puzzle is complete and correct
  if (num !== 0 && checkWin()) {
    handleWin();
  }
}

/**
 * Check if placing `num` at (row, col) violates Sudoku rules.
 * Checks against the current playerBoard for duplicates.
 */
function hasViolation(row, col, num) {
  // Check row
  for (let c = 0; c < 9; c++) {
    if (c !== col && playerBoard[row][c] === num) return true;
  }
  // Check column
  for (let r = 0; r < 9; r++) {
    if (r !== row && playerBoard[r][col] === num) return true;
  }
  // Check 3x3 box
  const boxRow = Math.floor(row / 3) * 3;
  const boxCol = Math.floor(col / 3) * 3;
  for (let r = boxRow; r < boxRow + 3; r++) {
    for (let c = boxCol; c < boxCol + 3; c++) {
      if ((r !== row || c !== col) && playerBoard[r][c] === num) return true;
    }
  }
  return false;
}

/**
 * Check if the player has correctly completed the entire puzzle.
 * Every cell must be filled and match the solution.
 */
function checkWin() {
  for (let r = 0; r < 9; r++) {
    for (let c = 0; c < 9; c++) {
      if (playerBoard[r][c] === 0) return false;
      if (playerBoard[r][c] !== solution[r][c]) return false;
    }
  }
  return true;
}

/**
 * Handle a win — stop timer, show celebration, animate board.
 */
function handleWin() {
  clearInterval(timerInterval);
  gameState = 'won';
  haptic('success');
  showMessage('🎉 Great job! You solved the puzzle! 🌟', 'success');
  setNumberPadEnabled(false);
  document.getElementById('btn-giveup').classList.add('hidden');
  document.getElementById('board').classList.add('celebrate');
}

/* ===========================
   TIMER
   =========================== */

/**
 * Start the countdown timer. Ticks every second.
 */
function startTimer() {
  if (timerInterval) clearInterval(timerInterval);

  timerInterval = setInterval(() => {
    timeRemaining--;
    updateTimerDisplay();

    // Add urgency styling when 30 seconds or less remain
    if (timeRemaining <= 30) {
      document.getElementById('timer-display').classList.add('urgent');
    }

    // Time's up!
    if (timeRemaining <= 0) {
      handleTimeUp();
    }
  }, 1000);
}

/**
 * Update the timer display with the current remaining time.
 */
function updateTimerDisplay() {
  const minutes = Math.floor(timeRemaining / 60);
  const seconds = timeRemaining % 60;
  const formatted = String(minutes).padStart(2, '0') + ':' + String(seconds).padStart(2, '0');
  const display = document.getElementById('timer-display');
  display.textContent = '⏱️ ' + formatted;
  display.classList.remove('urgent');
  if (timeRemaining <= 30) {
    display.classList.add('urgent');
  }
}

/**
 * Handle when the timer reaches zero.
 */
function handleTimeUp() {
  clearInterval(timerInterval);
  gameState = 'lost';
  showMessage("⏰ Time's up! Let's see the answers.", 'info');
  revealSolution();
}

/* ===========================
   GIVE UP & SOLUTION REVEAL
   =========================== */

/**
 * Player gives up — stop timer and show the solution.
 */
function giveUp() {
  if (gameState !== 'playing') return;
  clearInterval(timerInterval);
  gameState = 'gaveup';
  showMessage('No problem! Here\'s the solution. Try again! 💪', 'info');
  revealSolution();
}

/**
 * Reveal the full solution with color-coded cells:
 *  - Black: original given numbers
 *  - Blue: player's correct entries
 *  - Green: auto-revealed answers
 */
function revealSolution() {
  const board = document.getElementById('board');
  board.innerHTML = '';

  for (let row = 0; row < 9; row++) {
    for (let col = 0; col < 9; col++) {
      const cell = document.createElement('div');
      cell.className = 'cell';
      cell.textContent = solution[row][col];

      const key = row + ',' + col;

      if (givenCells.has(key)) {
        // Original given number — black text, gray background
        cell.classList.add('given');
      } else if (playerBoard[row][col] === solution[row][col] && playerBoard[row][col] !== 0) {
        // Player entered the correct number — blue text
        cell.classList.add('player-correct');
      } else {
        // Auto-revealed — green text
        cell.classList.add('auto-revealed');
      }

      // 3x3 box borders
      if (col === 2 || col === 5) cell.classList.add('box-border-right');
      if (row === 2 || row === 5) cell.classList.add('box-border-bottom');

      board.appendChild(cell);
    }
  }

  // Disable controls
  setNumberPadEnabled(false);
  document.getElementById('btn-giveup').classList.add('hidden');
}

/* ===========================
   UI HELPERS
   =========================== */

/**
 * Show a friendly message in the message area.
 * Type can be 'error', 'success', or 'info'.
 */
function showMessage(text, type) {
  const area = document.getElementById('message-area');
  area.textContent = text;
  area.className = '';
  if (type === 'error') area.classList.add('error-msg');
  else if (type === 'success') area.classList.add('success-msg');
  else if (type === 'info') area.classList.add('info-msg');
}

/**
 * Clear the message area.
 */
function clearMessage() {
  const area = document.getElementById('message-area');
  area.textContent = '';
  area.className = '';
}

/**
 * Enable or disable all number pad buttons.
 */
function setNumberPadEnabled(enabled) {
  const buttons = document.querySelectorAll('.btn-number');
  buttons.forEach(btn => { btn.disabled = !enabled; });
}
