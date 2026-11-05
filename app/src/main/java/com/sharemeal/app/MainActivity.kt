package com.sharemeal.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sharemeal.app.core.config.AppConfig
import com.sharemeal.app.core.navigation.ShareMealApp
import com.sharemeal.app.ui.theme.ShareMealTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var appConfig: AppConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShareMealTheme {
                ShareMealApp(isMockMode = appConfig.isMockMode)
            }
        }
    }
}
