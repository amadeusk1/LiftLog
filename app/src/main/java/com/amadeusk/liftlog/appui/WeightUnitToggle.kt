package com.amadeusk.liftlog.appui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.amadeusk.liftlog.data.WeightUnit

@Composable
fun WeightUnitToggle(
    current: WeightUnit,
    onUnitChange: (WeightUnit) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // LBS button
        Button(
            onClick = { onUnitChange(WeightUnit.LBS) },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (current == WeightUnit.LBS) Color(0xFF6A5AE0) else Color.Gray
            )
        ) { Text("lbs") }

        // KG button
        Button(
            onClick = { onUnitChange(WeightUnit.KG) },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (current == WeightUnit.KG) Color(0xFF6A5AE0) else Color.Gray
            )
        ) { Text("kg") }
    }
}
