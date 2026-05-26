package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.LogType
import com.example.data.WeightEntry
import com.example.ui.components.WeeklyWeightChart
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.WeightViewModel
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this, 
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS), 
                    101
                )
            }
        }

        enableEdgeToEdge()
        setContent {
            val viewModel: WeightViewModel = viewModel()
            val themePreset by viewModel.themePreset.collectAsState()
            val themeMode by viewModel.themeMode.collectAsState()

            val isDark = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            MyApplicationTheme(themePreset = themePreset, darkTheme = isDark) {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    WeightTrackerApp(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding),
                        isDark = isDark
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightTrackerApp(
    viewModel: WeightViewModel,
    modifier: Modifier = Modifier,
    isDark: Boolean
) {
    val context = LocalContext.current
    val entries by viewModel.allEntries.collectAsState()
    
    // Bottom tab routing navigation status
    var selectedTab by remember { mutableStateOf(0) }
    
    // Add weight detailed log form triggers
    var showAddDialog by remember { mutableStateOf(false) }

    // Height config for BMI calculation
    val sharedPrefs = remember { context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE) }
    var heightCm by remember { mutableStateOf(sharedPrefs.getFloat("user_height", 170.0f)) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "⚖️ BeratKu",
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "Pro",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("app_bar_add_log_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Tambah Entri Berat Badan",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                tonalElevation = 8.dp,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("main_navigation_bar")
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { iconWithBadge(selected = selectedTab == 0, active = Icons.Filled.Home, inactive = Icons.Outlined.Home, label = "Dasbor") },
                    label = { Text("Dasbor") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { iconWithBadge(selected = selectedTab == 1, active = Icons.AutoMirrored.Filled.List, inactive = Icons.AutoMirrored.Outlined.List, label = "Riwayat") },
                    label = { Text("Riwayat") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { iconWithBadge(selected = selectedTab == 2, active = Icons.Filled.Refresh, inactive = Icons.Outlined.Refresh, label = "Cloud Sync") },
                    label = { Text("Cloud") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { iconWithBadge(selected = selectedTab == 3, active = Icons.Filled.Settings, inactive = Icons.Outlined.Settings, label = "Pengaturan") },
                    label = { Text("Setelan") }
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == 0 || selectedTab == 1) {
                ExtendedFloatingActionButton(
                    text = { Text("Catat Berat", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Create, "Catat Berat Badan") },
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("floating_action_add_weight_button")
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (selectedTab) {
                0 -> DashboardScreen(viewModel = viewModel, entries = entries, heightCm = heightCm)
                1 -> HistoryScreen(viewModel = viewModel, entries = entries, heightCm = heightCm)
                2 -> CloudSyncScreen(viewModel = viewModel)
                3 -> SettingsScreen(
                    viewModel = viewModel, 
                    heightCm = heightCm, 
                    onHeightChanged = { newH ->
                        heightCm = newH
                        sharedPrefs.edit().putFloat("user_height", newH).apply()
                    }
                )
            }
        }
    }

    if (showAddDialog) {
        val sortedEntries = remember(entries) { entries.sortedByDescending { it.dateEpochDay } }
        val latestEntry = sortedEntries.firstOrNull()
        
        AddWeightDialog(
            latestWeight = latestEntry?.weight,
            onDismiss = { showAddDialog = false },
            onSave = { w, d, note, emoji, waist, water, activity ->
                viewModel.addWeightEntry(w, d, note, emoji, waist, water, activity)
                showAddDialog = false
                Toast.makeText(context, "Berhasil mencatat berat badan harian!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun iconWithBadge(selected: Boolean, active: androidx.compose.ui.graphics.vector.ImageVector, inactive: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Icon(
        imageVector = if (selected) active else inactive,
        contentDescription = label
    )
}

// ========================
// 1. DASHBOARD SCREEN
// ========================
@Composable
fun DashboardScreen(
    viewModel: WeightViewModel,
    entries: List<WeightEntry>,
    heightCm: Float
) {
    val sortedEntries = remember(entries) { entries.sortedByDescending { it.dateEpochDay } }
    val latestEntry = sortedEntries.firstOrNull()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
    ) {
        // Welcome and Brief
        item {
            Text(
                text = "Bagaimana progresmu hari ini?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Summary Cards Section
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card: Current Weight
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(130.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Berat Terbaru",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = if (latestEntry != null) "${latestEntry.weight}" else "--",
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = " kg",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                        Text(
                            text = if (latestEntry != null) latestEntry.dateString else "Belum ada entri",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }

                // Card: BMI Calculator
                val bmiValue = remember(latestEntry, heightCm) {
                    if (latestEntry != null && heightCm > 0) {
                        val hM = heightCm / 100.0f
                        latestEntry.weight / (hM * hM)
                    } else {
                        0.0
                    }
                }

                val (bmiCategory, bmiColor) = remember(bmiValue) {
                    when {
                        bmiValue <= 0.0 -> Pair("Masukkan tinggi", Color.Gray)
                        bmiValue < 18.5 -> Pair("Kurang Berat", Color(0xFF1E88E5))
                        bmiValue < 24.9 -> Pair("Ideal", Color(0xFF43A047))
                        bmiValue < 29.9 -> Pair("Kelebihan", Color(0xFFFB8C00))
                        else -> Pair("Obesitas", Color(0xFFE53935))
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(130.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Kategori Indeks BMI",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = if (bmiValue > 0) String.format(Locale.US, "%.1f", bmiValue) else "--",
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = " BMI",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = bmiColor.copy(alpha = 0.15f),
                            modifier = Modifier.align(Alignment.Start)
                        ) {
                            Text(
                                text = bmiCategory,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = bmiColor
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Graphical Weekly Analytics View
        item {
            WeeklyWeightChart(entries = entries)
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Today's Detailed Progress Highlight
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Ringkasan Log",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Metrik Log Terakhir Kamu",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        
                        if (latestEntry != null) {
                            Text(
                                text = latestEntry.feelingEmoji,
                                fontSize = 24.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (latestEntry != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            MetricIndicator(
                                symbol = "📏",
                                value = if (latestEntry.waistCircumferenceCm > 0.0) "${latestEntry.waistCircumferenceCm}cm" else "-",
                                label = "L. Pinggang"
                            )
                            MetricIndicator(
                                symbol = "🏃",
                                value = if (latestEntry.exerciseMinutes > 0) "${latestEntry.exerciseMinutes}m" else "-",
                                label = "Olahraga"
                            )
                            MetricIndicator(
                                symbol = "💧",
                                value = if (latestEntry.waterIntakeMl > 0) "${latestEntry.waterIntakeMl}ml" else "-",
                                label = "Konsumsi Air"
                            )
                        }

                        if (latestEntry.notes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Catatan Harian:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "\"${latestEntry.notes}\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Belum ada entri catatan harian hari ini. Yuk, mulai rekam data pertama untuk memantau progres sehatmu!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricIndicator(
    symbol: String,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(4.dp)
    ) {
        Text(
            text = symbol,
            fontSize = 20.sp,
            modifier = Modifier.size(24.dp),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
    }
}

// ========================
// 2. HISTORY SCREEN
// ========================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryScreen(
    viewModel: WeightViewModel,
    entries: List<WeightEntry>,
    heightCm: Float
) {
    val context = LocalContext.current
    var entryToDelete by remember { mutableStateOf<WeightEntry?>(null) }

    // Interactive Search and Emoji/Condition filter states
    var searchQuery by remember { mutableStateOf("") }
    var selectedFeelingFilter by remember { mutableStateOf<String?>(null) }

    // Chronological order is required to calculate differences correctly
    val itemsWithDiff = remember(entries) {
        val chronList = entries.sortedBy { it.dateEpochDay }
        val diffMap = mutableMapOf<Int, Double>()
        
        for (i in chronList.indices) {
            val curr = chronList[i]
            if (i == 0) {
                diffMap[curr.id] = 0.0
            } else {
                val prev = chronList[i - 1]
                diffMap[curr.id] = curr.weight - prev.weight
            }
        }
        
        // Return reverse list (newest first) with computed index weights diff
        chronList.reversed().map { Pair(it, diffMap[it.id] ?: 0.0) }
    }

    // Filter list dynamically based on inputs
    val filteredItems = remember(itemsWithDiff, searchQuery, selectedFeelingFilter) {
        itemsWithDiff.filter { (entry, _) ->
            val matchQuery = searchQuery.isEmpty() ||
                entry.notes.contains(searchQuery, ignoreCase = true) ||
                entry.dateString.contains(searchQuery, ignoreCase = true)
            
            val matchFeeling = selectedFeelingFilter == null || entry.feelingEmoji == selectedFeelingFilter
            matchQuery && matchFeeling
        }
    }

    if (itemsWithDiff.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("📚", fontSize = 48.sp, modifier = Modifier.padding(bottom = 12.dp))
                Text(
                    text = "Riwayat Catatan Kosong",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Seluruh log berat badan mingguan dan harian yang Anda tambahkan akan terdokumentasi di sini.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp, start = 16.dp, end = 16.dp)
        ) {
            // Header Content: Title and CSV export action
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Histori Timbangan (${itemsWithDiff.size} Log)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(onClick = { viewModel.exportCsv(context) }) {
                        Icon(Icons.Default.Share, "Ekspor CSV", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ekspor CSV")
                    }
                }
            }

            // Interactive Search layout
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari catatan (cth: #cheatday, puasa)...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cari") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Batal Pencarian")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("history_search_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }

            // Interactive Filter Chips Row
            item {
                Column(modifier = Modifier.padding(bottom = 16.dp)) {
                    Text(
                        text = "Saring Berdasarkan Kondisi",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    
                    val moods = listOf(
                        Triple(null, "🌟", "Semua"),
                        Triple("😊", "😊", "Segar"),
                        Triple("💪", "💪", "Aktif"),
                        Triple("😴", "😴", "Lelah"),
                        Triple("🥗", "🥗", "Bersih"),
                        Triple("🥵", "🥵", "Lemas"),
                        Triple("😔", "😔", "Stres"),
                        Triple("🍕", "🍕", "Khilaf"),
                        Triple("🧁", "🧁", "Manis")
                    )
                    
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(moods) { (emoji, dispEmoji, label) ->
                            val isSelected = selectedFeelingFilter == emoji
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedFeelingFilter = emoji },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(dispEmoji)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(label)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
            }

            // Results lists matching filter outputs
            if (filteredItems.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("🔍", fontSize = 36.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Hasil Tidak Ditemukan",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Tidak ada log timbangan yang cocok dengan pencarian atau filter Anda.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    searchQuery = ""
                                    selectedFeelingFilter = null
                                }
                            ) {
                                Text("Atur Ulang Pencarian")
                            }
                        }
                    }
                }
            } else {
                items(filteredItems, key = { it.first.id }) { (entry, diff) ->
                    HistoryWeightCard(
                        entry = entry,
                        diff = diff,
                        heightCm = heightCm,
                        onDelete = { entryToDelete = entry }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }

    if (entryToDelete != null) {
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            title = { Text("Hapus Catatan?") },
            text = { Text("Apakah Anda yakin ingin menghapus data timbangan pada tanggal ${entryToDelete?.dateString}?") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        entryToDelete?.let { viewModel.deleteWeightEntry(it) }
                        entryToDelete = null
                        Toast.makeText(context, "Berhasil menghapus entri!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Hapus", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun HistoryWeightCard(
    entry: WeightEntry,
    diff: Double,
    heightCm: Float,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .animateContentSize()
            .testTag("history_weight_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side: Emotional status indicator + Weight details
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(entry.feelingEmoji, fontSize = 24.sp)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${entry.weight}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = " kg",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }
                        
                        // Localization formatting
                        val friendlyDate = try {
                            val inputSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            val outputSdf = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
                            val dObj = inputSdf.parse(entry.dateString)
                            outputSdf.format(dObj ?: Date())
                        } catch (e: Exception) {
                            entry.dateString
                        }
                        
                        Text(
                            text = friendlyDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Right side: Difference trends & interaction triggers
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (diff != 0.0) {
                        val isLoss = diff < 0.0
                        val diffText = String.format(Locale.US, "%.1f", diff)
                        val badgeColor = if (isLoss) Color(0xFF2D6A4F) else Color(0xFFC94A29)
                        val badgeBg = badgeColor.copy(alpha = 0.12f)
                        
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = badgeBg,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                text = if (isLoss) "$diffText kg" else "+$diffText kg",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor
                            )
                        }
                    }

                    // Delete triggering action
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus entri ini",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        )
                    }

                    // Expand indicator action
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Sembunyikan detail" else "Tampilkan detail",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            // Quick display of mini badges in collapsed state
            if (!expanded && (entry.waistCircumferenceCm > 0.0 || entry.waterIntakeMl > 0 || entry.exerciseMinutes > 0 || entry.notes.isNotEmpty())) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (entry.waistCircumferenceCm > 0.0) {
                        Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                            Text("📏 ${entry.waistCircumferenceCm}cm", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (entry.waterIntakeMl > 0) {
                        Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                            Text("💧 ${entry.waterIntakeMl}ml", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (entry.exerciseMinutes > 0) {
                        Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                            Text("🏃 ${entry.exerciseMinutes}m", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (entry.notes.isNotEmpty()) {
                        Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                            Text("📝 Jurnal", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Dynamic animations for expanding comprehensive details
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 1. BMI status analysis
                        if (heightCm > 0.0f) {
                            val heightM = heightCm / 100.0f
                            val bmi = entry.weight / (heightM * heightM)
                            val bmiText = String.format(Locale.US, "%.1f", bmi)
                            
                            val (bmiCategory, bmiColor) = when {
                                bmi < 18.5 -> Pair("Kurang Berat Badan", Color(0xFF2196F3))
                                bmi < 25.0 -> Pair("Berat Badan Ideal (Sangat Sehat) ✔️", Color(0xFF4CAF50))
                                bmi < 30.0 -> Pair("Kelebihan Berat Badan ⚠️", Color(0xFFFFA726))
                                else -> Pair("Kategori Obesitas (Batas Waspada) 🚨", Color(0xFFEF5350))
                            }
                            
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = bmiColor.copy(alpha = 0.07f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(bmiColor.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("🧬", fontSize = 16.sp)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Indeks Massa Tubuh (BMI / IMT)",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "$bmiText ",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 15.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "• $bmiCategory",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = bmiColor
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Hydration progress representation
                        if (entry.waterIntakeMl > 0) {
                            val waterPct = (entry.waterIntakeMl / 2000f).coerceIn(0f, 1f)
                            val isCompleted = entry.waterIntakeMl >= 2000
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF2196F3).copy(alpha = 0.05f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("💧", fontSize = 16.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Asupan Cairan Harian",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = "${entry.waterIntakeMl} ml / 2000 ml",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isCompleted) Color(0xFF4CAF50) else Color(0xFF2196F3)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { waterPct },
                                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                        color = Color(0xFF2196F3),
                                        trackColor = Color(0xFF2196F3).copy(alpha = 0.15f)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (isCompleted) "Target hidrasi hari ini terpenuhi! ✔️" else "Minum ${((2000 - entry.waterIntakeMl).coerceAtLeast(0))} ml lagi untuk melengkapi.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // 3. Activity tracking duration & Waist progress side-by-side
                        if (entry.exerciseMinutes > 0 || entry.waistCircumferenceCm > 0.0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (entry.exerciseMinutes > 0) {
                                    val (intensity, color) = when {
                                        entry.exerciseMinutes < 15 -> Pair("Pemanasan", Color(0xFF2196F3))
                                        entry.exerciseMinutes <= 45 -> Pair("Sangat Baik", Color(0xFF4CAF50))
                                        else -> Pair("Luar Biasa", Color(0xFFE91E63))
                                    }
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.06f)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("🏃", fontSize = 14.sp)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Aktivitas", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("${entry.exerciseMinutes} Menit", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = color.copy(alpha = 0.15f)
                                            ) {
                                                Text(intensity, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
                                            }
                                        }
                                    }
                                }

                                if (entry.waistCircumferenceCm > 0.0) {
                                    val waistLimit = 90.0
                                    val isWaistSafe = entry.waistCircumferenceCm <= waistLimit
                                    val color = if (isWaistSafe) Color(0xFF4CAF50) else Color(0xFFFFA726)
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.06f)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("📏", fontSize = 14.sp)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Pinggang", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("${entry.waistCircumferenceCm} cm", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = color.copy(alpha = 0.15f)
                                            ) {
                                                Text(if (isWaistSafe) "Sehat/Aman" else "Batas Aman: 90cm", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 4. Detailed Journal text & #Hashtag recognition area
                        if (entry.notes.isNotEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("📝", fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Jurnal Kesehatan Harian",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = entry.notes,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    
                                    val hashtags = entry.notes.split("\\s+".toRegex()).filter { it.startsWith("#") && it.length > 1 }
                                    if (hashtags.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            hashtags.forEach { tag ->
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                                ) {
                                                    Text(
                                                        text = tag,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 5. Easy Log text copying
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                val summaryText = buildString {
                                    append("Log Berat Badan (${entry.dateString}):\n")
                                    append("• Berat: ${entry.weight} kg")
                                    if (diff != 0.0) append(" (${if (diff < 0) "" else "+"}${String.format(Locale.US, "%.1f", diff)} kg)")
                                    append("\n")
                                    if (entry.waistCircumferenceCm > 0.0) append("• Lingkar Pinggang: ${entry.waistCircumferenceCm} cm\n")
                                    if (entry.waterIntakeMl > 0) append("• Air: ${entry.waterIntakeMl} ml\n")
                                    if (entry.exerciseMinutes > 0) append("• Olahraga: ${entry.exerciseMinutes} menit\n")
                                    if (entry.notes.isNotEmpty()) append("• Catatan: ${entry.notes}\n")
                                    append("Dipantau dengan aplikasi BeratKu.")
                                }
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("Log Berat Badan", summaryText)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Log berhasil disalin ke papan klip!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.outlinedButtonColors()
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Salin Ringkasan", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Salin Ringkasan Log")
                        }
                    }
                }
            }
        }
    }
}

// ========================
// 3. CLOUD SYNC SCREEN
// ========================
@Composable
fun CloudSyncScreen(viewModel: WeightViewModel) {
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val syncLogs by viewModel.syncLogs.collectAsState()
    var serverUrlInput by remember { mutableStateOf("https://api.weighttracker-cloud.io/v1") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
    ) {
        item {
            Text(
                text = "Cloud Sync & Backup",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Pastikan histori berat badan Anda tetap aman dengan melakukan enkripsi dan pencadangan data ke cloud.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Connection Summary Widget
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(if (isSyncing) Color(0xFFFB8C00) else Color(0xFF43A047))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSyncing) "Sedang melakukan sinkronisasi..." else syncState,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.syncWithCloud() },
                            enabled = !isSyncing
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(Icons.Default.Refresh, "Backup")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sinkronisasi")
                            }
                        }

                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.clearSyncLogs() },
                            enabled = syncLogs.isNotEmpty()
                        ) {
                            Text("Bersihkan Log")
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Advanced REST Endpoint Node Customizer
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Kustom Server Cloud Endpoint",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = serverUrlInput,
                        onValueChange = {
                            serverUrlInput = it
                            viewModel.updateCloudServerUrl(it)
                        },
                        label = { Text("Cloud API Endpoint") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors()
                    )
                    Text(
                        text = "Ganti URL di atas jika Anda ingin mengintegrasikannya dengan server REST API / Firebase Database kustom milik Anda pribadi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Logs Output Terminal
        item {
            Text(
                text = "Console Logs Antrean Jaringan",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        if (syncLogs.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "Terminal logs kosong. Tekan 'Sinkronisasi' untuk mendaftarkan payload entri dan melihat negosiasi sync jaringan lokal-cloud.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }
        } else {
            items(syncLogs) { log ->
                val logColor = when (log.type) {
                    LogType.SUCCESS -> Color(0xFF2D6A4F)
                    LogType.ERROR -> Color(0xFFCC3333)
                    LogType.WARNING -> Color(0xFFFF9900)
                    LogType.INFO -> MaterialTheme.colorScheme.primary
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(10.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "[${log.timestamp}]",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = log.message,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = logColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

// ========================
// 4. SETTINGS SCREEN
// ========================
@Composable
fun SettingsScreen(
    viewModel: WeightViewModel,
    heightCm: Float,
    onHeightChanged: (Float) -> Unit
) {
    val context = LocalContext.current
    
    // Theme options
    val themePreset by viewModel.themePreset.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    // Alarm configurations
    val reminderEnabled by viewModel.reminderEnabled.collectAsState()
    val reminderHour by viewModel.reminderHour.collectAsState()
    val reminderMinute by viewModel.reminderMinute.collectAsState()

    var heightInput by remember { mutableStateOf(heightCm.toInt().toString()) }
    var editHeightMode by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
    ) {
        item {
            Text(
                text = "Setelan & Kustomisasi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Kelola preferensi notifikasi harian, kustomisasi palet warna, integrasikan kalender, dan ekspor data.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Section: Personal Metrics
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Profil Fisik (Kalkulator BMI)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (editHeightMode) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = heightInput,
                                onValueChange = { heightInput = it },
                                label = { Text("Tinggi Badan (cm)") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                            Button(onClick = {
                                val parsed = heightInput.toFloatOrNull()
                                if (parsed != null && parsed > 50f && parsed < 280f) {
                                    onHeightChanged(parsed)
                                    editHeightMode = false
                                } else {
                                    Toast.makeText(context, "Masukkan tinggi yang valid!", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Text("Simpan")
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Tinggi Badan", style = MaterialTheme.typography.bodyMedium)
                                Text("${heightCm.toInt()} cm", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            }
                            OutlinedButton(onClick = { editHeightMode = true }) {
                                Text("Ubah")
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Section: Daily Reminder Alarm Configuration
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Pengingat Harian (Push Notif)",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Mengirimkan notifikasi agar Anda rutin menimbang tepat waktu.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = reminderEnabled,
                            onCheckedChange = { viewModel.updateReminderSettings(it, reminderHour, reminderMinute) },
                            modifier = Modifier.testTag("reminder_switch_toggle")
                        )
                    }

                    if (reminderEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Waktu Pengingat", style = MaterialTheme.typography.bodyMedium)
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Simple interactive numerical clock setter
                                SpinnerTimeChanger(
                                    currentValue = reminderHour,
                                    maxValue = 23,
                                    onChanged = { h -> viewModel.updateReminderSettings(true, h, reminderMinute) }
                                )
                                Text(" : ", fontWeight = FontWeight.Black, fontSize = 20.sp)
                                SpinnerTimeChanger(
                                    currentValue = reminderMinute,
                                    maxValue = 59,
                                    onChanged = { m -> viewModel.updateReminderSettings(true, reminderHour, m) }
                                )
                            }
                        }

                        // Calendar Sync Trigger button
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                viewModel.scheduleCalendarSlot()
                                Toast.makeText(context, "Sistem Kalender Google disinkronisasikan!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.DateRange, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sinkronisasi Jadwal ke Kalender")
                        }
                        Text(
                            text = "Tekan tombol di atas untuk mendaftarkan event jadwal harian timbang secara terintegrasi di Aplikasi Google Kalender default ponsel Anda.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Section: Dark mode & Customize Themes
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Kustomisasi Tampilan & Tema",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Light/Dark mode choice buttons
                    Text("Pilihan Mode Gelap:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("system" to "Sistem", "light" to "Terang", "dark" to "Gelap").forEach { (id, label) ->
                            val selected = themeMode == id
                            SuggestionChip(
                                onClick = { viewModel.setThemeMode(id) },
                                label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    labelColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Preset coloring palettes selection
                    Text("Palet Warna Aplikasi:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))

                    listOf(
                        "classic" to "Violet Classic 💜",
                        "teal" to "Ocean Teal 🌊",
                        "sunset" to "Sunset Amber 🌅",
                        "forest" to "Forest Green 🌿"
                    ).forEach { (id, title) ->
                        val selected = themePreset == id
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { viewModel.setThemePreset(id) },
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent
                            ),
                            border = if (selected) CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))) else CardDefaults.outlinedCardBorder()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = title, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                                if (selected) {
                                    Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Section: Database Operations
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Cadangan Data & Reset",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { viewModel.exportCsv(context) }
                    ) {
                        Icon(Icons.Default.Share, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Simpan & Ekspor CSV")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    var confirmDeleteAll by remember { mutableStateOf(false) }
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        onClick = { confirmDeleteAll = true }
                    ) {
                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Bersihkan Seluruh Database")
                    }

                    if (confirmDeleteAll) {
                        AlertDialog(
                            onDismissRequest = { confirmDeleteAll = false },
                            title = { Text("Reset Total Data?") },
                            text = { Text("Tindakan ini permanen dan akan menghapus semua riwayat catatan berat badan Anda yang tersimpan di perangkat lokal.") },
                            confirmButton = {
                                Button(
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    onClick = {
                                        viewModel.clearAllData()
                                        confirmDeleteAll = false
                                        Toast.makeText(context, "Database dibersihkan!", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Text("Reset Semua", color = Color.White)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { confirmDeleteAll = false }) {
                                    Text("Batal")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SpinnerTimeChanger(
    currentValue: Int,
    maxValue: Int,
    onChanged: (Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(
            modifier = Modifier.size(28.dp),
            onClick = {
                val next = if (currentValue == 0) maxValue else currentValue - 1
                onChanged(next)
            }
        ) {
            Text("-", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
        }
        
        Text(
            text = String.format(Locale.getDefault(), "%02d", currentValue),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(28.dp),
            textAlign = TextAlign.Center
        )

        IconButton(
            modifier = Modifier.size(28.dp),
            onClick = {
                val next = if (currentValue == maxValue) 0 else currentValue + 1
                onChanged(next)
            }
        ) {
            Text("+", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
        }
    }
}

// ========================
// 5. INPUT DIALOG FORM
// ========================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWeightDialog(
    latestWeight: Double? = null,
    onDismiss: () -> Unit,
    onSave: (
        weight: Double,
        date: Date,
        notes: String,
        feelingEmoji: String,
        waist: Double,
        water: Int,
        activity: Int
    ) -> Unit
) {
    val context = LocalContext.current
    var weightInput by remember { mutableStateOf("") }
    var noteInput by remember { mutableStateOf("") }
    var feelingInput by remember { mutableStateOf("😊") }
    var waistInput by remember { mutableStateOf("") }
    var waterInput by remember { mutableStateOf(1000) } // ml
    var exerciseInput by remember { mutableStateOf("") }

    // Date configuration using Calendar and Java Date
    var selectedDate by remember { mutableStateOf(Date()) }

    // Form validation status
    val parsedWeight = weightInput.toDoubleOrNull()
    val isWeightValid = parsedWeight != null && parsedWeight in 10.0..500.0

    // Small helpers to easily step values
    fun adjustWeight(delta: Double) {
        val baseVal = weightInput.toDoubleOrNull() ?: latestWeight ?: 70.0
        val target = (baseVal + delta).coerceIn(10.0, 500.0)
        weightInput = String.format(Locale.US, "%.1f", target)
    }

    fun adjustWaist(delta: Double) {
        val baseVal = waistInput.toDoubleOrNull() ?: 0.0
        val target = (baseVal + delta).coerceIn(0.0, 300.0)
        waistInput = if (target > 0.0) String.format(Locale.US, "%.1f", target) else ""
    }

    fun adjustExercise(delta: Int) {
        val baseVal = exerciseInput.toIntOrNull() ?: 0
        val target = (baseVal + delta).coerceIn(0, 480)
        exerciseInput = if (target > 0) target.toString() else ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("⚖️", fontSize = 18.sp)
                    }
                }
                Column {
                    Text(
                        text = "Catat Log Timbangan",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Lengkapi detail untuk progres akurat",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                // Section 1: Weight Input & Copier
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Berat Utama (kg)*",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                
                                if (latestWeight != null) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                        modifier = Modifier.clickable {
                                            weightInput = String.format(Locale.US, "%.1f", latestWeight)
                                        }
                                    ) {
                                        Text(
                                            text = "Gunakan Terakhir: $latestWeight kg ↩",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = weightInput,
                                onValueChange = { weightInput = it },
                                placeholder = { Text("0.0") },
                                label = { Text("Berat Sekarang (kg)") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("add_weight_input"),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )

                            // Quick adjustment buttons
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    -1.0 to "-1 kg",
                                    -0.1 to "-0.1",
                                    0.1 to "+0.1",
                                    1.0 to "+1 kg"
                                ).forEach { (valDelta, textLabel) ->
                                    Button(
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                        onClick = { adjustWeight(valDelta) },
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                    ) {
                                        Text(text = textLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            
                            if (weightInput.isNotEmpty() && !isWeightValid) {
                                Text(
                                    text = "⚠️ Masukkan berat valid (10 - 500 kg)",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.align(Alignment.Start)
                                )
                            }
                        }
                    }
                }

                // Section 2: Date Selector
                item {
                    val sdfIndo = remember { SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID")) }
                    val cal = remember(selectedDate) { Calendar.getInstance().apply { time = selectedDate } }
                    
                    fun adjustDate(field: Int, amount: Int) {
                        val newCal = Calendar.getInstance().apply { time = selectedDate }
                        newCal.add(field, amount)
                        if (newCal.timeInMillis <= System.currentTimeMillis()) {
                            selectedDate = newCal.time
                        }
                    }

                    Column {
                        Text(
                            text = "Tanggal Pengukuran",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = "Tanggal Pengukuran",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = sdfIndo.format(selectedDate),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            "Hari",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            IconButton(
                                                modifier = Modifier.size(28.dp),
                                                onClick = { adjustDate(Calendar.DAY_OF_MONTH, -1) }
                                            ) {
                                                Text("-", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, fontSize = 20.sp)
                                            }
                                            Text(
                                                text = String.format(Locale.getDefault(), "%02d", cal.get(Calendar.DAY_OF_MONTH)),
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            IconButton(
                                                modifier = Modifier.size(28.dp),
                                                onClick = { adjustDate(Calendar.DAY_OF_MONTH, 1) }
                                            ) {
                                                Text("+", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, fontSize = 20.sp)
                                            }
                                        }
                                    }

                                    val monthsIndo = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Ags", "Sep", "Okt", "Nov", "Des")
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1.2f)
                                    ) {
                                        Text(
                                            "Bulan",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            IconButton(
                                                modifier = Modifier.size(28.dp),
                                                onClick = { adjustDate(Calendar.MONTH, -1) }
                                            ) {
                                                Text("-", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, fontSize = 20.sp)
                                            }
                                            Text(
                                                text = monthsIndo.getOrElse(cal.get(Calendar.MONTH)) { "" },
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            IconButton(
                                                modifier = Modifier.size(28.dp),
                                                onClick = { adjustDate(Calendar.MONTH, 1) }
                                            ) {
                                                Text("+", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, fontSize = 20.sp)
                                            }
                                        }
                                    }

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1.1f)
                                    ) {
                                        Text(
                                            "Tahun",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            IconButton(
                                                modifier = Modifier.size(28.dp),
                                                onClick = { adjustDate(Calendar.YEAR, -1) }
                                            ) {
                                                Text("-", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, fontSize = 20.sp)
                                            }
                                            Text(
                                                text = cal.get(Calendar.YEAR).toString(),
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            IconButton(
                                                modifier = Modifier.size(28.dp),
                                                onClick = { adjustDate(Calendar.YEAR, 1) }
                                            ) {
                                                Text("+", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, fontSize = 20.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 3: Condition / Feeling
                item {
                    val feelings = listOf(
                        Triple("😊", "Segar", "Kondisi sehat & bugar"),
                        Triple("💪", "Aktif", "Habis olahraga / stamina prima"),
                        Triple("😴", "Lelah", "Kurang tidur / butuh istirahat"),
                        Triple("🥗", "Bersih", "Konsumsi makanan sehat & bersih"),
                        Triple("🥵", "Lemas", "Dehidrasi / lemas / kurang cairan"),
                        Triple("😔", "Stres", "Kondisi kelelahan pikiran"),
                        Triple("🍕", "Khilaf", "Makan makanan berminyak / junk food"),
                        Triple("🧁", "Manis", "Konsumsi gula berlebih hari ini")
                    )

                    Column {
                        Text(
                            text = "Kondisi Fisik & Mood",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            feelings.chunked(2).forEach { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    pair.forEach { (emoji, label, desc) ->
                                        val selected = feelingInput == emoji
                                        OutlinedCard(
                                            onClick = { feelingInput = emoji },
                                            modifier = Modifier.weight(1f),
                                            colors = CardDefaults.outlinedCardColors(
                                                containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else Color.Transparent
                                            ),
                                            border = if (selected) {
                                                CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)))
                                            } else {
                                                CardDefaults.outlinedCardBorder()
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(emoji, fontSize = 16.sp)
                                                    }
                                                }
                                                Column {
                                                    Text(
                                                        text = label,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp,
                                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = desc,
                                                        fontSize = 8.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 4: Physical Body tape (Waist Circumference)
                item {
                    Column {
                        Text(
                            text = "Lingkar Pinggang (Opsional)",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                colors = ButtonDefaults.filledTonalButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                                onClick = { adjustWaist(-1.0) },
                                modifier = Modifier.size(44.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("-1", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Button(
                                colors = ButtonDefaults.filledTonalButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                                onClick = { adjustWaist(-0.5) },
                                modifier = Modifier.size(44.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("-0.5", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            OutlinedTextField(
                                value = waistInput,
                                onValueChange = { waistInput = it },
                                placeholder = { Text("0.0") },
                                label = { Text("Melingkar (cm)") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("add_waist_input"),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )

                            Button(
                                colors = ButtonDefaults.filledTonalButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                                onClick = { adjustWaist(0.5) },
                                modifier = Modifier.size(44.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("+0.5", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            Button(
                                colors = ButtonDefaults.filledTonalButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                                onClick = { adjustWaist(1.0) },
                                modifier = Modifier.size(44.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("+1", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                        Text(
                            text = "💡 Lingkar pinggang ideal: Pria < 90cm, Wanita < 80cm.",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                        )
                    }
                }

                // Section 5: Water Hydration
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Konsumsi Air Cairan: $waterInput ml",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (waterInput > 0) {
                                TextButton(
                                    onClick = { waterInput = 0 },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Reset", fontSize = 11.sp)
                                }
                            }
                        }
                        
                        // Glass illustrations mapping
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val activeGlasses = (waterInput / 250).coerceIn(0, 10)
                            for (i in 1..10) {
                                val opacity = if (i <= activeGlasses) 1.0f else 0.25f
                                Text(
                                    text = "🥛",
                                    fontSize = 18.sp,
                                    modifier = Modifier.weight(1f).alpha(opacity),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Slider(
                            value = waterInput.toFloat(),
                            onValueChange = { waterInput = (it / 250).toInt() * 250 },
                            valueRange = 0f..4000f,
                            steps = 15
                        )

                        // Quick buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                250 to "+250 ml 🥛",
                                500 to "+500 ml 🍼",
                                1000 to "+1L 🥤"
                            ).forEach { (amt, lbl) ->
                                OutlinedButton(
                                    onClick = { waterInput = (waterInput + amt).coerceAtMost(4000) },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                ) {
                                    Text(lbl, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Section 6: Exercise Activites
                item {
                    Column {
                        Text(
                            text = "Durasi Olahraga/Latihan",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                colors = ButtonDefaults.filledTonalButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                                onClick = { adjustExercise(-15) },
                                modifier = Modifier.size(44.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("-15", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            Button(
                                colors = ButtonDefaults.filledTonalButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                                onClick = { adjustExercise(-5) },
                                modifier = Modifier.size(44.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("-5", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            OutlinedTextField(
                                value = exerciseInput,
                                onValueChange = { exerciseInput = it },
                                placeholder = { Text("0") },
                                label = { Text("Olahraga (menit)") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("add_exercise_input"),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )

                            Button(
                                colors = ButtonDefaults.filledTonalButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                                onClick = { adjustExercise(5) },
                                modifier = Modifier.size(44.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("+5", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            Button(
                                colors = ButtonDefaults.filledTonalButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                                onClick = { adjustExercise(15) },
                                modifier = Modifier.size(44.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("+15", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Section 7: Health Notes & Hashtag injects
                item {
                    Column {
                        Text(
                            text = "Jurnal Kesehatan Harian",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = noteInput,
                            onValueChange = { noteInput = it },
                            placeholder = { Text("Tulis keadaan porsi makanan, jam tidur, atau pemicu timbangan...") },
                            label = { Text("Catatan Diet & Jurnal") },
                            modifier = Modifier
                                        .fillMaxWidth()
                                .testTag("add_notes_input"),
                            singleLine = false,
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors()
                        )
                        
                        // Suggestion tags row
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(
                                "#puasa",
                                "#cheatday",
                                "#begadang",
                                "#dietketat",
                                "#workout",
                                "#highcarb"
                            ).forEach { tag ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .clickable {
                                            if (!noteInput.contains(tag)) {
                                                noteInput = if (noteInput.isEmpty()) tag else "$noteInput $tag"
                                            }
                                        }
                                        .padding(vertical = 2.dp)
                                ) {
                                    Text(
                                        text = tag,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                modifier = Modifier.testTag("add_weight_save_button"),
                enabled = isWeightValid,
                onClick = {
                    val w = parsedWeight
                    if (w != null) {
                        val waistVal = waistInput.toDoubleOrNull() ?: 0.0
                        val actMin = exerciseInput.toIntOrNull() ?: 0

                        onSave(w, selectedDate, noteInput, feelingInput, waistVal, waterInput, actMin)
                    }
                }
            ) {
                Text("Simpan Detil Log")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )

}

