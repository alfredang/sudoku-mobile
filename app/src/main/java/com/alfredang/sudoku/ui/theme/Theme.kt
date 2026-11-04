package com.alfredang.sudoku.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/* ---- Brand palette (ported from the original web game) ------------------ */

val CreamBackground = Color(0xFFFFF8E7)
val TealPrimary = Color(0xFF4ECDC4)
val EasyGreen = Color(0xFF7BC74D)
val MediumOrange = Color(0xFFF9A825)
val HardRed = Color(0xFFF94144)

/* ---- Board-specific colors --------------------------------------------- */

val CellBackground = Color(0xFFFFFFFF)
val CellSelected = Color(0xFFFFFACD)
val CellGivenBackground = Color(0xFFF0F0F0)
val CellError = Color(0xFFFF6B6B)
val TextDark = Color(0xFF2D3436)
val GivenText = Color(0xFF000000)
val PlayerText = Color(0xFF1565C0)
val RevealText = Color(0xFF2E7D32)
val PlayerCorrectBackground = Color(0xFFE3F2FD)
val AutoRevealedBackground = Color(0xFFE8F5E9)

/* ---- Message colors ----------------------------------------------------- */

val ErrorMsgBg = Color(0xFFFFE0E0)
val ErrorMsgText = Color(0xFFC62828)
val SuccessMsgBg = Color(0xFFE8F5E9)
val SuccessMsgText = Color(0xFF2E7D32)
val InfoMsgBg = Color(0xFFE3F2FD)
val InfoMsgText = Color(0xFF1565C0)

private val SudokuColorScheme = lightColorScheme(
    primary = TealPrimary,
    onPrimary = Color.White,
    background = CreamBackground,
    onBackground = TextDark,
    surface = CreamBackground,
    onSurface = TextDark,
)

@Composable
fun SudokuTheme(content: @Composable () -> Unit) {
    // The game keeps a single, kid-friendly light look regardless of system theme.
    @Suppress("UNUSED_VARIABLE")
    val isDark = isSystemInDarkTheme()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = CreamBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = SudokuColorScheme,
        typography = Typography(),
        content = content,
    )
}
