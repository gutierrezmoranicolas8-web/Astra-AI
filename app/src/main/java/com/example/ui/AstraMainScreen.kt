package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.chat.AstraChatScreen
import com.example.ui.screens.models.ModelsCatalogScreen
import com.example.ui.screens.search.SemanticSearchScreen
import com.example.ui.screens.settings.AstraSettingsScreen
import com.example.ui.screens.splitscreen.SplitScreenArenaScreen
import com.example.ui.viewmodel.AstraNavDestination
import com.example.ui.viewmodel.AstraViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AstraMainScreen(
    viewModel: AstraViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle Back Button correctly
    BackHandler(enabled = uiState.activeConversationDetail != null || uiState.currentDestination != AstraNavDestination.CHAT) {
        if (uiState.activeConversationDetail != null) {
            viewModel.closeConversationDetail()
        } else {
            viewModel.setDestination(AstraNavDestination.CHAT)
        }
    }

    LaunchedEffect(uiState.infoMessage) {
        uiState.infoMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearInfoMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(
                                    brush = Brush.linearGradient(
                                        listOf(Color(0xFF6366F1), Color(0xFFA855F7), Color(0xFFEC4899))
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Astra AI",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Box(modifier = Modifier.padding(start = 10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Astra AI",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.5.sp
                                    )
                                )
                                Box(
                                    modifier = Modifier
                                        .padding(start = 6.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "6 MODELOS",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                NavigationBarItem(
                    selected = uiState.currentDestination == AstraNavDestination.CHAT,
                    onClick = { viewModel.setDestination(AstraNavDestination.CHAT) },
                    icon = { Icon(Icons.Default.ChatBubble, contentDescription = "Chat") },
                    label = { Text("Chat Hub", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = uiState.currentDestination == AstraNavDestination.SPLIT_SCREEN,
                    onClick = { viewModel.setDestination(AstraNavDestination.SPLIT_SCREEN) },
                    icon = { Icon(Icons.Default.CompareArrows, contentDescription = "Comparador") },
                    label = { Text("Dividida", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = uiState.currentDestination == AstraNavDestination.SEARCH,
                    onClick = { viewModel.setDestination(AstraNavDestination.SEARCH) },
                    icon = { Icon(Icons.Default.Psychology, contentDescription = "Búsqueda Semántica") },
                    label = { Text("Búsqueda", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = uiState.currentDestination == AstraNavDestination.MODELS,
                    onClick = { viewModel.setDestination(AstraNavDestination.MODELS) },
                    icon = { Icon(Icons.Default.WorkspacePremium, contentDescription = "Modelos") },
                    label = { Text("Planes", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = uiState.currentDestination == AstraNavDestination.SETTINGS,
                    onClick = { viewModel.setDestination(AstraNavDestination.SETTINGS) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Ajustes") },
                    label = { Text("Ajustes", fontSize = 11.sp) }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.currentDestination) {
                AstraNavDestination.CHAT -> AstraChatScreen(viewModel = viewModel, uiState = uiState)
                AstraNavDestination.SPLIT_SCREEN -> SplitScreenArenaScreen(viewModel = viewModel, uiState = uiState)
                AstraNavDestination.SEARCH -> SemanticSearchScreen(viewModel = viewModel, uiState = uiState)
                AstraNavDestination.MODELS -> ModelsCatalogScreen(viewModel = viewModel, uiState = uiState)
                AstraNavDestination.SETTINGS -> AstraSettingsScreen(viewModel = viewModel, uiState = uiState)
            }
        }
    }
}
