package com.amadeusk.liftlog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.math.log10


// Sub-tabs within the Info screen
enum class InfoSubTab {
    TDEE,
    ONE_RM,
    PROTEIN,
    BODY_FAT
}

@Composable
fun ToolsScreen() {
    var currentSubTab by remember { mutableStateOf(InfoSubTab.TDEE) }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Sub-tabs row
        TabRow(
            selectedTabIndex = currentSubTab.ordinal,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = currentSubTab == InfoSubTab.TDEE,
                onClick = { currentSubTab = InfoSubTab.TDEE },
                text = { Text("TDEE") }
            )
            Tab(
                selected = currentSubTab == InfoSubTab.ONE_RM,
                onClick = { currentSubTab = InfoSubTab.ONE_RM },
                text = { Text("1RM") }
            )
            Tab(
                selected = currentSubTab == InfoSubTab.PROTEIN,
                onClick = { currentSubTab = InfoSubTab.PROTEIN },
                text = { Text("Protein") }
            )
            Tab(
                selected = currentSubTab == InfoSubTab.BODY_FAT,
                onClick = { currentSubTab = InfoSubTab.BODY_FAT },
                text = { Text("Body Fat %") }
            )
        }

        when (currentSubTab) {
            InfoSubTab.TDEE -> TdeeCalculator()
            InfoSubTab.ONE_RM -> OneRmCalculator()
            InfoSubTab.PROTEIN -> ProteinNeedsCalculator()
            InfoSubTab.BODY_FAT -> BodyFatCalculator()
        }
    }
}

@Composable
fun TdeeCalculator() {
    val scrollState = rememberScrollState()

    var weightText by remember { mutableStateOf("") } // kg
    var heightText by remember { mutableStateOf("") } // cm
    var ageText by remember { mutableStateOf("") }

    var isMale by remember { mutableStateOf(true) }
    var activityIndex by remember { mutableStateOf(1) } // 0..4

    val activityLabels = listOf(
        "Sedentary (x1.2)",
        "Light (x1.375)",
        "Moderate (x1.55)",
        "Heavy (x1.725)",
        "Athlete (x1.9)"
    )
    val activityMultipliers = listOf(1.2, 1.375, 1.55, 1.725, 1.9)

    val weight = weightText.toDoubleOrNull()
    val height = heightText.toDoubleOrNull()
    val age = ageText.toIntOrNull()

    val bmr = if (weight != null && height != null && age != null) {
        if (isMale) {
            10 * weight + 6.25 * height - 5 * age + 5
        } else {
            10 * weight + 6.25 * height - 5 * age - 161
        }
    } else null

    val tdee = bmr?.let { it * activityMultipliers[activityIndex] }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "TDEE (Total Daily Energy Expenditure)",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "This estimates how many calories you burn per day based on your stats and activity. " +
                    "It uses the Mifflin–St Jeor equation.",
            style = MaterialTheme.typography.bodySmall
        )

        OutlinedTextField(
            value = weightText,
            onValueChange = { weightText = it },
            label = { Text("Body weight (kg)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = heightText,
            onValueChange = { heightText = it },
            label = { Text("Height (cm)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = ageText,
            onValueChange = { ageText = it },
            label = { Text("Age (years)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Text("Sex", style = MaterialTheme.typography.labelMedium)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = isMale,
                    onClick = { isMale = true }
                )
                Text("Male")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = !isMale,
                    onClick = { isMale = false }
                )
                Text("Female")
            }
        }

        Text("Activity level", style = MaterialTheme.typography.labelMedium)

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            activityLabels.forEachIndexed { index, label ->
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = activityIndex == index,
                        onClick = { activityIndex = index }
                    )
                    Text(label)
                }
            }
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        if (bmr != null && tdee != null) {
            Text("Estimated BMR: ${bmr.toInt()} kcal/day")
            Text("Estimated TDEE: ${tdee.toInt()} kcal/day")

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Rough guidelines:",
                style = MaterialTheme.typography.labelMedium
            )
            Text("- Mild fat loss: TDEE - 250 to 400 kcal")
            Text("- Aggressive cut: TDEE - 500 to 700 kcal")
            Text("- Slow bulk: TDEE + 200 to 300 kcal")
        } else {
            Text(
                text = "Fill in weight, height, and age to calculate your TDEE.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun OneRmCalculator() {
    val scrollState = rememberScrollState()

    var weightText by remember { mutableStateOf("") }
    var repsText by remember { mutableStateOf("") }

    val weight = weightText.toDoubleOrNull()
    val reps = repsText.toIntOrNull()

    // Epley formula: 1RM = w * (1 + reps / 30)
    val oneRm = if (weight != null && reps != null && reps in 1..12) {
        weight * (1.0 + reps.toDouble() / 30.0)
    } else null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "1RM (One-Rep Max) Estimator",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "Enter the weight you lifted and how many clean reps you did. " +
                    "This uses the Epley formula, which works best for sets of 1–12 reps.",
            style = MaterialTheme.typography.bodySmall
        )

        OutlinedTextField(
            value = weightText,
            onValueChange = { weightText = it },
            label = { Text("Weight used (same unit as app)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = repsText,
            onValueChange = { repsText = it },
            label = { Text("Reps (1–12)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        if (oneRm != null) {
            Text("Estimated 1RM: ${String.format("%.1f", oneRm)}")

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Suggested training percentages:",
                style = MaterialTheme.typography.labelMedium
            )
            val percents = listOf(0.6, 0.7, 0.8, 0.9)
            percents.forEach { p ->
                val w = oneRm * p
                Text(
                    text = "${(p * 100).toInt()}% ≈ ${String.format("%.1f", w)}"
                )
            }
        } else {
            Text(
                text = "Enter a valid weight and reps (1–12) to estimate your 1RM.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun ProteinNeedsCalculator() {
    val scrollState = rememberScrollState()

    var weightText by remember { mutableStateOf("") }
    var useKg by remember { mutableStateOf(true) } // Unit inside this screen

    // Goal: cut / maintain / bulk
    var goal by remember { mutableStateOf("Recomp / Maintain") }

    val weight = weightText.toDoubleOrNull()

    val weightKg = if (weight != null) {
        if (useKg) weight else weight * 0.45359237
    } else null

    val (low, high) = when (goal) {
        "Cut / Fat loss" -> 2.0 to 2.7   // g/kg
        "Recomp / Maintain" -> 1.6 to 2.2
        "Bulk / Gain" -> 1.6 to 2.0
        else -> 1.6 to 2.2
    }

    val gramsRange = weightKg?.let { kg ->
        val lowG = kg * low
        val highG = kg * high
        lowG to highG
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Protein Needs",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "General evidence-based ranges for lifters are around 1.6–2.2 g/kg of bodyweight per day, " +
                    "higher when cutting, slightly lower when bulking.",
            style = MaterialTheme.typography.bodySmall
        )

        OutlinedTextField(
            value = weightText,
            onValueChange = { weightText = it },
            label = { Text(if (useKg) "Body weight (kg)" else "Body weight (lb)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Text("Units", style = MaterialTheme.typography.labelMedium)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = useKg,
                    onClick = { useKg = true }
                )
                Text("kg")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = !useKg,
                    onClick = { useKg = false }
                )
                Text("lb")
            }
        }

        Text("Goal", style = MaterialTheme.typography.labelMedium)

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("Cut / Fat loss", "Recomp / Maintain", "Bulk / Gain").forEach { g ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = goal == g,
                        onClick = { goal = g }
                    )
                    Text(g)
                }
            }
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        if (gramsRange != null) {
            val (lowG, highG) = gramsRange
            Text(
                text = "Recommended daily protein:",
                style = MaterialTheme.typography.labelMedium
            )
            Text("${lowG.toInt()} – ${highG.toInt()} g per day")

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Example splits:",
                style = MaterialTheme.typography.labelMedium
            )
            val meals = 3
            val mealsHigh = 4
            Text(
                "- If you eat $meals meals: " +
                        "${(lowG / meals).toInt()} – ${(highG / meals).toInt()} g per meal"
            )
            Text(
                "- If you eat $mealsHigh meals: " +
                        "${(lowG / mealsHigh).toInt()} – ${(highG / mealsHigh).toInt()} g per meal"
            )
        } else {
            Text(
                text = "Enter your bodyweight to get a recommended daily protein range.",
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Notes:",
            style = MaterialTheme.typography.labelMedium
        )
        Text("- Aim for a good protein source in every meal (meat, eggs, dairy, whey, tofu, etc.).")
        Text("- More total calories > tiny differences in protein when bulking.")
        Text("- When cutting, higher protein helps keep muscle while losing fat.")
    }
}
@Composable
fun BodyFatCalculator() {
    val scrollState = rememberScrollState()

    // Unit selection
    var useMetric by remember { mutableStateOf(true) }  // true = kg/cm, false = lb/in
    val heightUnit = if (useMetric) "cm" else "in"
    val weightUnit = if (useMetric) "kg" else "lb"

    // Shared sex selection
    var isMale by remember { mutableStateOf(true) }

    // BMI method inputs
    var bmiHeightText by remember { mutableStateOf("") }
    var bmiWeightText by remember { mutableStateOf("") }
    var ageText by remember { mutableStateOf("") }

    // Navy method inputs
    var heightText by remember { mutableStateOf("") }
    var neckText by remember { mutableStateOf("") }
    var waistText by remember { mutableStateOf("") }
    var hipText by remember { mutableStateOf("") }

    // Convert text → Double?
    fun d(s: String) = s.toDoubleOrNull()

    // -------------------------
    // UNIT CONVERSION HELPERS
    // -------------------------
    fun toKg(v: Double) = if (useMetric) v else v * 0.45359237
    fun toCm(v: Double) = if (useMetric) v else v * 2.54

    // -------------------------
    // BMI-BASED BODY FAT
    // -------------------------
    val bmiHeightCm = d(bmiHeightText)?.let { toCm(it) }
    val bmiWeightKg = d(bmiWeightText)?.let { toKg(it) }
    val age = ageText.toIntOrNull()

    val bmi = if (bmiHeightCm != null && bmiWeightKg != null && bmiHeightCm > 0) {
        val m = bmiHeightCm / 100.0
        bmiWeightKg / (m * m)
    } else null

    val bmiBodyFat = if (bmi != null && age != null) {
        val sexFactor = if (isMale) 1.0 else 0.0
        1.20 * bmi + 0.23 * age - 10.8 * sexFactor - 5.4
    } else null

    // -------------------------
    // NAVY METHOD BODY FAT
    // -------------------------
    val navyHeight = d(heightText)?.let { toCm(it) }
    val neck = d(neckText)?.let { toCm(it) }
    val waist = d(waistText)?.let { toCm(it) }
    val hip = d(hipText)?.let { toCm(it) }

    val hasMaleInputs =
        isMale && navyHeight != null && neck != null && waist != null

    val hasFemaleInputs =
        !isMale && navyHeight != null && neck != null && waist != null && hip != null

    val navyBodyFat: Double? = when {
        hasMaleInputs && (waist!! - neck!!) > 0 -> {
            val density =
                1.0324 -
                        0.19077 * log10(waist - neck) +
                        0.15456 * log10(navyHeight!!)
            (495.0 / density) - 450.0
        }

        hasFemaleInputs && (waist!! + hip!! - neck!!) > 0 -> {
            val density =
                1.29579 -
                        0.35004 * log10(waist + hip - neck) +
                        0.22100 * log10(navyHeight!!)
            (495.0 / density) - 450.0
        }

        else -> null
    }

    val invalidShape = when {
        hasMaleInputs && waist != null && neck != null && (waist - neck) <= 0 ->
            "Waist must be larger than neck for the Navy method."
        hasFemaleInputs && waist != null && hip != null && neck != null &&
                (waist + hip - neck) <= 0 ->
            "Waist + hip must be larger than neck."
        else -> null
    }

    // -------------------------
    // UI
    // -------------------------
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text("Body Fat Percentage", style = MaterialTheme.typography.titleMedium)

        // UNIT TOGGLE
        Text("Units", style = MaterialTheme.typography.labelMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = useMetric, onClick = { useMetric = true })
            Text("Metric (kg / cm)")
            Spacer(modifier = Modifier.width(16.dp))
            RadioButton(selected = !useMetric, onClick = { useMetric = false })
            Text("Imperial (lb / in)")
        }

        // SEX TOGGLE
        Text("Sex", style = MaterialTheme.typography.labelMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = isMale, onClick = { isMale = true })
            Text("Male")
            Spacer(modifier = Modifier.width(16.dp))
            RadioButton(selected = !isMale, onClick = { isMale = false })
            Text("Female")
        }

        // -------------------------
        // BMI METHOD UI
        // -------------------------
        Divider()
        Text("Quick Estimate (BMI Method)", style = MaterialTheme.typography.labelLarge)
        Text(
            "Fast estimate using BMI, age, and sex. Less precise than Navy method.",
            style = MaterialTheme.typography.bodySmall
        )

        OutlinedTextField(
            value = bmiWeightText,
            onValueChange = { bmiWeightText = it },
            label = { Text("Body weight ($weightUnit)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = bmiHeightText,
            onValueChange = { bmiHeightText = it },
            label = { Text("Height ($heightUnit)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = ageText,
            onValueChange = { ageText = it },
            label = { Text("Age (years)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        bmi?.let { Text("BMI: ${String.format("%.1f", it)}") }

        bmiBodyFat?.let {
            if (!it.isNaN() && !it.isInfinite()) {
                Text("Estimated BF% (BMI): ${String.format("%.1f", it)}%")
            }
        } ?: Text(
            "Enter weight, height, and age for BMI estimate.",
            style = MaterialTheme.typography.bodySmall
        )

        // -------------------------
        // NAVY METHOD UI
        // -------------------------
        Divider()
        Text("US Navy Tape-Measure Method", style = MaterialTheme.typography.labelLarge)
        Text("More accurate if measurements are correct.", style = MaterialTheme.typography.bodySmall)

        OutlinedTextField(
            value = heightText,
            onValueChange = { heightText = it },
            label = { Text("Height ($heightUnit)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = neckText,
            onValueChange = { neckText = it },
            label = { Text("Neck ($heightUnit)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = waistText,
            onValueChange = { waistText = it },
            label = { Text("Waist ($heightUnit)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        if (!isMale) {
            OutlinedTextField(
                value = hipText,
                onValueChange = { hipText = it },
                label = { Text("Hip ($heightUnit)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (navyBodyFat != null && !navyBodyFat.isNaN() && !navyBodyFat.isInfinite()) {
            Text(
                "Estimated BF% (Navy): ${String.format("%.1f", navyBodyFat)}%",
                style = MaterialTheme.typography.headlineSmall
            )
        } else {
            Text(
                "Enter all tape measurements for Navy estimate.",
                style = MaterialTheme.typography.bodySmall
            )
        }

        invalidShape?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}
