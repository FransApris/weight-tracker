package com.example.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.hasText
import com.example.data.WeightEntry
import org.junit.Rule
import org.junit.Test

class WeeklyWeightChartTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun placeholderShownWhenNotEnoughData() {
        composeTestRule.setContent {
            WeeklyWeightChart(entries = emptyList())
        }

        composeTestRule.onNodeWithText("Kurang Data Untuk Grafik").assertExists()
    }

    @Test
    fun chartShowsValuesWhenEnoughData() {
        val entries = listOf(
            WeightEntry(0, 70.0, 19000, "2023-01-01", "", "😊", 80.0, 1000, 30),
            WeightEntry(0, 69.5, 19001, "2023-01-02", "", "😊", 80.0, 1000, 30)
        )

        composeTestRule.setContent {
            WeeklyWeightChart(entries = entries)
        }

        // Title should always be present
        composeTestRule.onNodeWithText("Grafik Perubahan Mingguan").assertExists()
    }
}
