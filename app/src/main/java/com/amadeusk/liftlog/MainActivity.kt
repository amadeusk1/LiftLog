package com.amadeusk.liftlog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.amadeusk.liftlog.appui.LiftLogApp
import com.amadeusk.liftlog.ui.theme.LiftLogTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LiftLogTheme {
                LiftLogApp()
            }
        }
    }
}
