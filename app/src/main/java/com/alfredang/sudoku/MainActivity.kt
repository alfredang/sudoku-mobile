package com.alfredang.sudoku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.alfredang.sudoku.ui.SudokuApp
import com.alfredang.sudoku.ui.theme.SudokuTheme

class MainActivity : ComponentActivity() {

    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SudokuTheme {
                SudokuApp(viewModel)
            }
        }
    }
}
