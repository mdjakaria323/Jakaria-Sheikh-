package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.data.local.PaperDatabase
import com.example.data.repository.PaperRepository
import com.example.ui.screens.DetailScreen
import com.example.ui.screens.EditScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ScanScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.PaperViewModel
import com.example.ui.viewmodel.PaperViewModelFactory
import com.example.ui.viewmodel.Screen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize Room Local Database, DAO, and Repository
        val database = PaperDatabase.getDatabase(applicationContext)
        val repository = PaperRepository(database.paperDao())
        
        // Initialize ViewModel via factory
        val viewModelFactory = PaperViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, viewModelFactory)[PaperViewModel::class.java]

        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    val currentScreen by viewModel.currentScreen.collectAsState()
                    
                    Crossfade(
                        targetState = currentScreen,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        label = "screen_transition"
                    ) { screen ->
                        when (screen) {
                            is Screen.Home -> HomeScreen(viewModel = viewModel)
                            is Screen.Scan -> ScanScreen(viewModel = viewModel)
                            is Screen.Detail -> DetailScreen(viewModel = viewModel)
                            is Screen.Edit -> EditScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
