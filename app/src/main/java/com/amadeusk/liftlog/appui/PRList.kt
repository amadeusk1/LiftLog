package com.amadeusk.liftlog.appui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amadeusk.liftlog.data.PR
import com.amadeusk.liftlog.data.WeightUnit

@Composable
fun PRList(
    prs: List<PR>,
    unit: WeightUnit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(prs) { pr ->
            PRRow(pr, unit)
        }
    }
}

@Composable
fun PRRow(pr: PR, unit: WeightUnit) {
    Surface(
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = pr.exercise,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))

            val displayWeight =
                if (unit == WeightUnit.LBS) pr.weight
                else pr.weight * 0.453592f

            val unitText = if (unit == WeightUnit.LBS) "lbs" else "kg"

            Text(text = "${"%.1f".format(displayWeight)} $unitText × ${pr.reps} reps")
            Text(text = "Date: ${pr.date}", fontSize = 12.sp)
        }
    }
}
