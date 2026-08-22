package com.lycoris.lycosheet.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.lycoris.lycosheet.android.ui.theme.LycoSheetTheme
import com.lycoris.lycosheet.android.ui.navigation.LycoSheetNavGraph

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LycoSheetTheme {
                LycoSheetNavGraph()
            }
        }
    }
}
