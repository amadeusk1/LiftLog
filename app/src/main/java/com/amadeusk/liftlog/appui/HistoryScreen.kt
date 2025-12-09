package com.amadeusk.liftlog.appui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.amadeusk.liftlog.data.PR
import com.amadeusk.liftlog.data.WeightUnit

@Composable
fun HistoryScreen(
    prs: List<PR>,
    unit: WeightUnit
) {
    PRList(
        prs = prs,
        unit = unit,
        modifier = Modifier.fillMaxSize()
    )
}
