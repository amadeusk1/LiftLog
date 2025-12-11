package com.amadeusk.liftlog

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp


@Composable
fun InfoScreen() {
    val context = LocalContext.current
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {

        // -------------------------
        // TITLE
        // -------------------------
        Text(
            "Information & Resources",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        // NEW NOTE BELOW TITLE
        Text(
            text = "Have something to add? Email: contact@amadeusk.dev",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:contact@amadeusk.dev")
                }
                context.startActivity(intent)
            }
        )
        // -------------------------
        // EXTERNAL RESOURCES — FITNESS
        // -------------------------
        SectionTitle("Recommended Fitness Resources")

        ResourceCard(
            title = "Jeff Nippard (YouTube) — Evidence-based training",
            url = "https://www.youtube.com/user/icecream4PRs",
            context = context
        )

        ResourceCard(
            title = "Stronger By Science — Training & nutrition deep dives",
            url = "https://www.strongerbyscience.com",
            context = context
        )

        ResourceCard(
            title = "Examine.com — Supplement research",
            url = "https://examine.com",
            context = context
        )

        ResourceCard(
            title = "Renaissance Periodization (RP) — Hypertrophy templates",
            url = "https://renaissanceperiodization.com",
            context = context
        )
        // -------------------------
        // STEROID RESOURCES SECTION
        // -------------------------
        SectionTitle("Steriods + Other Compounds Resources")

        ResourceCard(
            title = "ThinkSteroids — Articles",
            url = "https://thinksteroids.com/contributors/",
            context = context
        )
        ResourceCard(
            title = "ThinkSteroids — Community Forum",
            url = "https://thinksteroids.com/community/",
            context = context
        )

        ResourceCard(
            title = "Tanner Tattered FAQ (One of the best FAQ's)",
            url = "https://docs.google.com/document/d/12Lw3X20BPpSpd4iEiEad2FR-NF6r4GMs_Srw7zj1W4M/edit",
            context = context
        )


        // -------------------------
        // DISCLAIMER
        // -------------------------
        SectionTitle("Disclaimer")
        Text(
            "These resources are provided for educational, training, and harm-reduction purposes only. "
                    + "Nothing here replaces medical advice.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(20.dp))

        // -------------------------
        // TRAINING FUNDAMENTALS
        // -------------------------
        SectionTitle("Training Fundamentals")

        InfoBullet("Progressive overload is the #1 driver of muscle and strength.")
        InfoBullet("Aim for 10–20 hard sets per muscle per week.")
        InfoBullet("Most hypertrophy work is best done between 5–30 reps.")
        InfoBullet("Compound lifts are king: Squat, Deadlift, Bench, OHP, Rows, Pull-ups.")
        InfoBullet("Failure training is fine, but don’t take every set to failure.")

        // -------------------------
        // NUTRITION BASICS
        // -------------------------
        SectionTitle("Nutrition Basics")

        InfoBullet("Daily protein target: 1.6–2.2 g per kg bodyweight.")
        InfoBullet("Calorie balance determines weight gain/loss.")
        InfoBullet("Carbs fuel training; fats support hormones.")
        InfoBullet("Creatine monohydrate 3–5g daily is safe & effective.")
        InfoBullet("Hydration affects performance more than people think.")


        // -------------------------
        // RECOVERY & SLEEP
        // -------------------------
        SectionTitle("Recovery & Sleep")

        InfoBullet("Aim for 7–9 hours of sleep per night.")
        InfoBullet("Sleep improves strength, mood, hunger control, and muscle growth.")
        InfoBullet("Walking helps recovery more than stretching does.")
        InfoBullet("Deload every 4–8 weeks if you're feeling beat down.")


        // -------------------------
        // INJURY PREVENTION
        // -------------------------
        SectionTitle("Injury Prevention")

        InfoBullet("Warm up with lighter sets of your main movement.")
        InfoBullet("Increase load slowly week to week.")
        InfoBullet("Train through a full range of motion where possible.")
        InfoBullet("Pain ≠ normal; learn the difference between fatigue and injury.")


        // -------------------------
        // HYPERTROPHY GUIDELINES
        // -------------------------
        SectionTitle("Hypertrophy Guidelines")

        InfoBullet("Train 2–3x per muscle group per week.")
        InfoBullet("Use movements you can feel working the target muscle.")
        InfoBullet("Most growth comes from the last 5 reps before failure.")
        InfoBullet("You don't need fancy machines — good form beats fancy equipment.")

        // -------------------------
        // STRENGTH GUIDELINES
        // -------------------------
        SectionTitle("Strength Guidelines")

        InfoBullet("Practice low-rep (1–6) compound lifts with good form.")
        InfoBullet("Track progress weekly (your app already does this!).")
        InfoBullet("Add weight or reps gradually — micro plates help.")
        InfoBullet("Rest 2–5 minutes between heavy sets.")


        // -------------------------
        // USEFUL IN-APP TOOLS
        // -------------------------
        SectionTitle("Tools in This App")

        InfoBullet("PR Calculator (Epley formula)")
        InfoBullet("Bodyweight tracking with graphs")
        InfoBullet("TDEE Calculator")
        InfoBullet("Protein Intake Calculator")
        InfoBullet("Body Fat Estimation Calculator (BMI & Tape Method)")






    }
}


// ---------------------------------------------------------
// COMPONENTS
// ---------------------------------------------------------

@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun InfoBullet(text: String) {
    Text("• $text", style = MaterialTheme.typography.bodyMedium)
}

@Composable
fun ResourceCard(title: String, url: String, context: android.content.Context) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
