package com.alfredang.sudoku.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import android.view.HapticFeedbackConstants
import com.alfredang.sudoku.CellStyle
import com.alfredang.sudoku.Difficulty
import com.alfredang.sudoku.GameMessage
import com.alfredang.sudoku.GameStatus
import com.alfredang.sudoku.GameViewModel
import com.alfredang.sudoku.Haptic
import com.alfredang.sudoku.MessageType
import com.alfredang.sudoku.UiState
import com.alfredang.sudoku.ui.theme.AutoRevealedBackground
import com.alfredang.sudoku.ui.theme.CellBackground
import com.alfredang.sudoku.ui.theme.CellError
import com.alfredang.sudoku.ui.theme.CellGivenBackground
import com.alfredang.sudoku.ui.theme.CellSelected
import com.alfredang.sudoku.ui.theme.CreamBackground
import com.alfredang.sudoku.ui.theme.EasyGreen
import com.alfredang.sudoku.ui.theme.ErrorMsgBg
import com.alfredang.sudoku.ui.theme.ErrorMsgText
import com.alfredang.sudoku.ui.theme.GivenText
import com.alfredang.sudoku.ui.theme.HardRed
import com.alfredang.sudoku.ui.theme.InfoMsgBg
import com.alfredang.sudoku.ui.theme.InfoMsgText
import com.alfredang.sudoku.ui.theme.MediumOrange
import com.alfredang.sudoku.ui.theme.PlayerCorrectBackground
import com.alfredang.sudoku.ui.theme.PlayerText
import com.alfredang.sudoku.ui.theme.RevealText
import com.alfredang.sudoku.ui.theme.SuccessMsgBg
import com.alfredang.sudoku.ui.theme.SuccessMsgText
import com.alfredang.sudoku.ui.theme.TealPrimary

@Composable
fun SudokuApp(vm: GameViewModel) {
    val view = LocalView.current

    // Bridge ViewModel haptic events to the platform haptic feedback API.
    LaunchedEffect(Unit) {
        vm.haptics.collect { h ->
            val constant = when (h) {
                Haptic.LIGHT -> HapticFeedbackConstants.KEYBOARD_TAP
                Haptic.MEDIUM -> HapticFeedbackConstants.VIRTUAL_KEY
                Haptic.ERROR -> HapticFeedbackConstants.LONG_PRESS
                Haptic.SUCCESS -> HapticFeedbackConstants.CONFIRM
            }
            view.performHapticFeedback(constant)
        }
    }

    val ui = vm.ui
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBackground)
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (ui.status == GameStatus.MENU) {
            MenuScreen(onSelect = vm::startGame)
        } else {
            GameScreen(
                ui = ui,
                onSelectCell = vm::selectCell,
                onNumber = vm::enterNumber,
                onGiveUp = vm::giveUp,
                onNewGame = vm::showMenu,
            )
        }
    }
}

/* ---- Menu --------------------------------------------------------------- */

@Composable
private fun MenuScreen(onSelect: (Difficulty) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 500.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Sudoku Fun! 🧩",
            color = TealPrimary,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Pick your challenge!",
            color = Color(0xFF666666),
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Difficulty.entries.forEach { diff ->
            DifficultyButton(diff) { onSelect(diff) }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DifficultyButton(difficulty: Difficulty, onClick: () -> Unit) {
    val bg = when (difficulty) {
        Difficulty.EASY -> EasyGreen
        Difficulty.MEDIUM -> MediumOrange
        Difficulty.HARD -> HardRed
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 320.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(difficulty.emoji, fontSize = 30.sp)
        Spacer(Modifier.width(16.dp))
        Text(
            text = difficulty.label,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = difficulty.description,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 14.sp,
        )
    }
}

/* ---- Game --------------------------------------------------------------- */

@Composable
private fun GameScreen(
    ui: UiState,
    onSelectCell: (Int) -> Unit,
    onNumber: (Int) -> Unit,
    onGiveUp: () -> Unit,
    onNewGame: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 500.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TopBar(ui.difficulty, ui.timeRemaining)
        Spacer(Modifier.height(12.dp))
        MessageArea(ui.message)
        Spacer(Modifier.height(10.dp))
        SudokuBoard(
            ui = ui,
            onSelectCell = onSelectCell,
        )
        Spacer(Modifier.height(16.dp))
        NumberPad(enabled = ui.numberPadEnabled, onNumber = onNumber)
        Spacer(Modifier.height(16.dp))
        ActionButtons(showGiveUp = ui.showGiveUp, onGiveUp = onGiveUp, onNewGame = onNewGame)
    }
}

@Composable
private fun TopBar(difficulty: Difficulty?, timeRemaining: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (difficulty != null) {
            val badgeColor = when (difficulty) {
                Difficulty.EASY -> EasyGreen
                Difficulty.MEDIUM -> MediumOrange
                Difficulty.HARD -> HardRed
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(badgeColor)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Text(
                    text = "${difficulty.emoji} ${difficulty.label}",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        val urgent = timeRemaining <= 30
        val minutes = timeRemaining / 60
        val seconds = timeRemaining % 60
        val formatted = "%02d:%02d".format(minutes, seconds)

        // Pulse the timer when time is running out.
        val alpha = if (urgent) {
            val transition = rememberInfiniteTransition(label = "urgent")
            transition.animateFloat(
                initialValue = 1f,
                targetValue = 0.4f,
                animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
                label = "urgentAlpha",
            ).value
        } else 1f

        Text(
            text = "⏱️ $formatted",
            color = (if (urgent) HardRed else TealPrimary).copy(alpha = alpha),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun MessageArea(message: GameMessage?) {
    val (bg, fg) = when (message?.type) {
        MessageType.ERROR -> ErrorMsgBg to ErrorMsgText
        MessageType.SUCCESS -> SuccessMsgBg to SuccessMsgText
        MessageType.INFO -> InfoMsgBg to InfoMsgText
        null -> Color.Transparent to Color.Transparent
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        if (message != null) {
            Text(
                text = message.text,
                color = fg,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
    }
}

@Composable
private fun SudokuBoard(ui: UiState, onSelectCell: (Int) -> Unit) {
    // Celebration pulse on win.
    val scale = if (ui.celebrate) {
        val transition = rememberInfiniteTransition(label = "celebrate")
        transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.03f,
            animationSpec = infiniteRepeatable(tween(300), RepeatMode.Reverse),
            label = "celebrateScale",
        ).value
    } else 1f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 450.dp)
            .aspectRatio(1f)
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .border(3.dp, TealPrimary, RoundedCornerShape(12.dp))
            .background(CellBackground),
    ) {
        Column(Modifier.fillMaxSize()) {
            for (row in 0 until 9) {
                Row(Modifier.fillMaxWidth().weight(1f)) {
                    for (col in 0 until 9) {
                        val index = row * 9 + col
                        val cell = ui.cells.getOrNull(index)
                        BoardCell(
                            value = cell?.value ?: 0,
                            style = cell?.style ?: CellStyle.EMPTY,
                            error = cell?.error ?: false,
                            selected = ui.selectedIndex == index,
                            clickable = ui.status == GameStatus.PLAYING,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            onClick = { onSelectCell(index) },
                        )
                    }
                }
            }
        }
        // 3x3 box grid lines drawn on top of the cells.
        Canvas(Modifier.fillMaxSize()) {
            val cellW = size.width / 9f
            val cellH = size.height / 9f
            for (i in 1 until 9) {
                val thick = i % 3 == 0
                val stroke = if (thick) 2.5.dp.toPx() else 1f
                val color = if (thick) TealPrimary else TealPrimary.copy(alpha = 0.35f)
                drawLine(color, Offset(i * cellW, 0f), Offset(i * cellW, size.height), stroke)
                drawLine(color, Offset(0f, i * cellH), Offset(size.width, i * cellH), stroke)
            }
        }
    }
}

@Composable
private fun BoardCell(
    value: Int,
    style: CellStyle,
    error: Boolean,
    selected: Boolean,
    clickable: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val background = when {
        error -> CellError
        selected -> CellSelected
        style == CellStyle.GIVEN || style == CellStyle.REVEAL_GIVEN -> CellGivenBackground
        style == CellStyle.REVEAL_CORRECT -> PlayerCorrectBackground
        style == CellStyle.REVEAL_AUTO -> AutoRevealedBackground
        else -> CellBackground
    }
    val textColor = when {
        error -> Color.White
        style == CellStyle.GIVEN || style == CellStyle.REVEAL_GIVEN -> GivenText
        style == CellStyle.PLAYER || style == CellStyle.REVEAL_CORRECT -> PlayerText
        style == CellStyle.REVEAL_AUTO -> RevealText
        else -> PlayerText
    }

    val cellModifier = if (clickable) modifier.clickable(onClick = onClick) else modifier
    Box(
        modifier = cellModifier.background(background),
        contentAlignment = Alignment.Center,
    ) {
        if (value != 0) {
            Text(
                text = value.toString(),
                color = textColor,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NumberPad(enabled: Boolean, onNumber: (Int) -> Unit) {
    val view = LocalView.current
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (n in 1..9) {
            PadButton(
                label = n.toString(),
                background = TealPrimary,
                textColor = Color.White,
                enabled = enabled,
                contentDescription = "Enter $n",
            ) {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                onNumber(n)
            }
        }
        PadButton(
            label = "⌫",
            background = Color(0xFFE0E0E0),
            textColor = Color(0xFF555555),
            enabled = enabled,
            contentDescription = "Erase",
        ) {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            onNumber(0)
        }
    }
}

@Composable
private fun PadButton(
    label: String,
    background: Color,
    textColor: Color,
    enabled: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val alpha = if (enabled) 1f else 0.4f
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(background.copy(alpha = alpha))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = textColor.copy(alpha = alpha),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ActionButtons(showGiveUp: Boolean, onGiveUp: () -> Unit, onNewGame: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
    ) {
        if (showGiveUp) {
            ActionButton(
                label = "🏳️ Give Up",
                background = Color(0xFFFFCDD2),
                textColor = Color(0xFFC62828),
                onClick = onGiveUp,
            )
        }
        ActionButton(
            label = "🔄 New Game",
            background = TealPrimary,
            textColor = Color.White,
            onClick = onNewGame,
        )
    }
}

@Composable
private fun ActionButton(
    label: String,
    background: Color,
    textColor: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
