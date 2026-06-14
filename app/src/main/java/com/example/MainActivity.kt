package com.example

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Room Database & Repository
        val database = AppDatabase.getDatabase(this)
        val repository = BotRepository(database.botDao())

        setContent {
            MyApplicationTheme {
                val viewModel: BotViewModel = viewModel(
                    factory = BotViewModelFactory(repository)
                )
                MainScreen(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: BotViewModel) {
    val config by viewModel.configuration.collectAsStateWithLifecycle()
    val tickers by viewModel.tickers.collectAsStateWithLifecycle()
    val openTrades by viewModel.openTrades.collectAsStateWithLifecycle()
    val tradesHistory by viewModel.allTrades.collectAsStateWithLifecycle()
    val logs by viewModel.recentLogs.collectAsStateWithLifecycle()

    var activeTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("main_scaffold"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(50))
                                .background(TealPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SmartToy,
                                contentDescription = "Logo",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "MEXC Pro Algotrade",
                                fontWeight = FontWeight.SemiBold,
                                color = PlatinumWhite,
                                fontSize = 15.sp,
                                lineHeight = 15.sp
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(if (config.isActive) TealPrimary else SilverText)
                                )
                                Text(
                                    text = if (config.isActive) "نشط الآن • Scalping v2.4" else "متوقف • Scalping v2.4",
                                    color = if (config.isActive) TealPrimary else SilverText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Start/Stop indicator widget
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (config.isActive) EmeraldProfit.copy(alpha = 0.12f)
                                else DividerSlate
                            )
                            .border(
                                1.dp,
                                if (config.isActive) EmeraldProfit.copy(alpha = 0.3f)
                                else SilverText.copy(alpha = 0.2f),
                                RoundedCornerShape(20.dp)
                            )
                            .clickable { viewModel.toggleBotActive() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                        .size(6.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(if (config.isActive) TealPrimary else SilverText)
                        )
                        Text(
                            text = if (config.isActive) "تعطيل البوت" else "تشغيل البوت",
                            color = if (config.isActive) TealPrimary else SilverText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepCarbonBackground,
                    titleContentColor = PlatinumWhite
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = BottomNavBg,
                modifier = Modifier
                    .navigationBarsPadding()
                    .border(1.dp, DividerSlate, androidx.compose.ui.graphics.RectangleShape),
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Filled.Dashboard, contentDescription = "الرئيسية") },
                    label = { Text("الرئيسية", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TealPrimary,
                        selectedTextColor = TealPrimary,
                        unselectedIconColor = SilverText,
                        unselectedTextColor = SilverText,
                        indicatorColor = DividerSlate
                    ),
                    modifier = Modifier.testTag("tab_dashboard")
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = {
                        BadgedBox(badge = {
                            if (openTrades.isNotEmpty()) {
                                Badge(containerColor = EmeraldProfit) {
                                    Text(openTrades.size.toString(), color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }) {
                            Icon(Icons.Filled.ShowChart, contentDescription = "المراكز")
                        }
                    },
                    label = { Text("التحليلات", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TealPrimary,
                        selectedTextColor = TealPrimary,
                        unselectedIconColor = SilverText,
                        unselectedTextColor = SilverText,
                        indicatorColor = DividerSlate
                    ),
                    modifier = Modifier.testTag("tab_positions")
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(Icons.Filled.Code, contentDescription = "بايثون") },
                    label = { Text("المحفظة", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TealPrimary,
                        selectedTextColor = TealPrimary,
                        unselectedIconColor = SilverText,
                        unselectedTextColor = SilverText,
                        indicatorColor = DividerSlate
                    ),
                    modifier = Modifier.testTag("tab_python")
                )
                NavigationBarItem(
                    selected = activeTab == 3,
                    onClick = { activeTab = 3 },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = "إعدادات") },
                    label = { Text("الإعدادات", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TealPrimary,
                        selectedTextColor = TealPrimary,
                        unselectedIconColor = SilverText,
                        unselectedTextColor = SilverText,
                        indicatorColor = DividerSlate
                    ),
                    modifier = Modifier.testTag("tab_settings")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                0 -> DashboardTab(viewModel, config, tickers, openTrades)
                1 -> PositionsTab(viewModel, openTrades, tradesHistory, logs)
                2 -> PythonCodeTab()
                3 -> SettingsTab(viewModel, config)
            }
        }
    }
}

@Composable
fun DashboardTab(
    viewModel: BotViewModel,
    config: BotConfiguration,
    tickers: List<TickerState>,
    openTrades: List<SimulatedTrade>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_column")
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Balance Overview Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BalanceCardBg),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BalanceCardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.5f))
                                .border(1.dp, Color.Black.copy(alpha = 0.05f), RoundedCornerShape(50))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "USDT Asset",
                                color = SilverText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "إجمالي رصيد الفيوتشرز",
                            color = SilverText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.End
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "$%.2f".format(config.accountBalance),
                        color = PlatinumWhite,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val totalUnrealizedPnl = openTrades.sumOf { trade ->
                        val ticker = tickers.find { t -> t.symbol == trade.symbol }
                        if (ticker != null) {
                            val sideFactor = if (trade.side == "LONG") 1.0 else -1.0
                            val pct = (ticker.currentPrice - trade.entryPrice) / trade.entryPrice
                            sideFactor * pct * trade.entryPrice * trade.quantity
                        } else 0.0
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isProfit = totalUnrealizedPnl >= 0.0
                        val badgeText = (if (isProfit) "+" else "") + "$%.2f".format(totalUnrealizedPnl) + 
                                " (%s)".format(if (isProfit) "إيجابي" else "سلبي")
                        val badgeBg = if (isProfit) Color(0xFFD1E8D5) else Color(0xFFF9DEDC)
                        val badgeTextCol = if (isProfit) Color(0xFF006D31) else Color(0xFFBA1A1A)
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "اليوم",
                                color = SilverText,
                                fontSize = 10.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(badgeBg)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = badgeText,
                                    color = badgeTextCol,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        
                        Text(
                            text = "الرافعة: %dx | هامش: %s".format(config.leverage, config.marginMode),
                            color = SilverText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Quick Control Actions panel
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ObsidianGrey80),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DividerSlate),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "التحكم السريع في محرك سكالبينج",
                        color = PlatinumWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "عند التشغيل، سيقوم البوت بمراقبة المؤشرات وفتح وإغلاق الصفقات تلقائياً بناءً على إعداداتك.",
                        color = SilverText,
                        fontSize = 11.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { viewModel.toggleBotActive() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (config.isActive) CrimsonLoss else TealPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("toggle_bot_button")
                    ) {
                        Icon(
                            imageVector = if (config.isActive) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (config.isActive) "تعطيل البوت التلقائي 🔴" else "تشغيل البوت التلقائي ⚡",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Section Title: Active Markets
        item {
            Text(
                text = "مؤشرات السوق والفرص (رصد فوري):",
                color = TealPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Right
            )
        }

        // Coins monitoring list
        items(tickers) { ticker ->
            CoinMonitorItem(ticker, viewModel, openTrades, config)
        }
    }
}

@Composable
fun CoinMonitorItem(
    ticker: TickerState,
    viewModel: BotViewModel,
    openTrades: List<SimulatedTrade>,
    config: BotConfiguration
) {
    val hasOpenTrade = openTrades.any { it.symbol == ticker.symbol }
    val activeTrade = openTrades.find { it.symbol == ticker.symbol }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ObsidianGrey80),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DividerSlate),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Coin header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side: Price & Percent
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "%.3f USDT".format(ticker.currentPrice),
                        color = PlatinumWhite,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 15.sp
                    )
                    Text(
                        text = (if (ticker.priceChange24h >= 0.0) "+" else "") + "%.2f%%".format(ticker.priceChange24h),
                        color = if (ticker.priceChange24h >= 0.0) EmeraldProfit else CrimsonLoss,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Right side: Symbol and status
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (hasOpenTrade) {
                        val sideLabel = activeTrade?.side ?: "LONG"
                        val pillColor = if (sideLabel == "LONG") EmeraldProfit else CrimsonLoss
                        val pillBg = if (sideLabel == "LONG") LongPillBgColor else Color(0xFFF9DEDC)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(pillBg)
                                .border(1.dp, pillColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "لديك صفقة $sideLabel",
                                color = pillColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = ticker.symbol.split("/")[0],
                        color = PlatinumWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = DividerSlate, thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Technical details (Indicators panel)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // RSI block
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                    Text("مؤشر RSI (${config.rsiPeriod})", color = SilverText, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    val rsiVal = ticker.rsi ?: 50.0
                    val rsiColor = when {
                        rsiVal < config.rsiOversold -> EmeraldProfit
                        rsiVal > config.rsiOverbought -> CrimsonLoss
                        else -> SilverText
                    }
                    Text(
                        text = "%.2f".format(rsiVal),
                        color = rsiColor,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                }

                // EMA block
                Column(modifier = Modifier.weight(1.5f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "EMA (${config.emaShortPeriod}/${config.emaLongPeriod})",
                        color = SilverText,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "S: %.2f | L: %.2f".format(ticker.emaShort ?: 0.0, ticker.emaLong ?: 0.0),
                        color = PlatinumWhite,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Target Decision Banner
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text("الإشارة الفنية", color = SilverText, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    val signal = ticker.signal
                    val sigColor = when (signal) {
                        "BUY LONG" -> EmeraldProfit
                        "SELL SHORT" -> CrimsonLoss
                        else -> SilverText
                    }
                    val sigText = when (signal) {
                        "BUY LONG" -> "شراء LONG ⚡"
                        "SELL SHORT" -> "بيع SHORT ⚡"
                        else -> "مراقبة 🔍"
                    }
                    Text(
                        text = sigText,
                        color = sigColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = DividerSlate, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Lower interactive segment: Spiking & Manual trigger
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Manual overrides
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { viewModel.manualOpenPosition(ticker.symbol, "LONG") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LongPillBgColor,
                            contentColor = EmeraldProfit
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("عقد LONG +", color = EmeraldProfit, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { viewModel.manualOpenPosition(ticker.symbol, "SHORT") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF9DEDC),
                            contentColor = CrimsonLoss
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("عقد SHORT +", color = CrimsonLoss, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Interactive volatile spikes buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("تحفيز هزة سعرية:", color = SilverText, fontSize = 10.sp)
                    IconButton(
                        onClick = { viewModel.triggerMarketSpike(ticker.symbol, isUpward = true) },
                        modifier = Modifier.size(28.dp).background(DividerSlate, RoundedCornerShape(6.dp))
                    ) {
                        Icon(Icons.Filled.ArrowUpward, contentDescription = "Spike Up", tint = EmeraldProfit, modifier = Modifier.size(14.dp))
                    }
                    IconButton(
                        onClick = { viewModel.triggerMarketSpike(ticker.symbol, isUpward = false) },
                        modifier = Modifier.size(28.dp).background(DividerSlate, RoundedCornerShape(6.dp))
                    ) {
                        Icon(Icons.Filled.ArrowDownward, contentDescription = "Spike Down", tint = CrimsonLoss, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun PositionsTab(
    viewModel: BotViewModel,
    openTrades: List<SimulatedTrade>,
    tradesHistory: List<SimulatedTrade>,
    logs: List<TradingLog>
) {
    var selectedSubTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Sub-Tab selector: Positions vs Historic Trades vs Live Logs
        TabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = BottomNavBg,
            contentColor = TealPrimary,
            modifier = Modifier.clip(RoundedCornerShape(12.dp)).border(1.dp, DividerSlate, RoundedCornerShape(12.dp))
        ) {
            Tab(selected = selectedSubTab == 0, onClick = { selectedSubTab = 0 }) {
                Text(
                    text = "المراكز (${openTrades.size})",
                    modifier = Modifier.padding(vertical = 12.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedSubTab == 0) TealPrimary else SilverText
                )
            }
            Tab(selected = selectedSubTab == 1, onClick = { selectedSubTab = 1 }) {
                Text(
                    text = "سجل الصفقات",
                    modifier = Modifier.padding(vertical = 12.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedSubTab == 1) TealPrimary else SilverText
                )
            }
            Tab(selected = selectedSubTab == 2, onClick = { selectedSubTab = 2 }) {
                Text(
                    text = "الكونسول",
                    modifier = Modifier.padding(vertical = 12.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedSubTab == 2) TealPrimary else SilverText
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tab Content
        when (selectedSubTab) {
            0 -> OpenPositionsView(viewModel, openTrades)
            1 -> TradesHistoryView(viewModel, tradesHistory)
            2 -> BotConsoleLogsView(viewModel, logs)
        }
    }
}

@Composable
fun OpenPositionsView(viewModel: BotViewModel, openTrades: List<SimulatedTrade>) {
    if (openTrades.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Filled.OfflineBolt, contentDescription = "Empty", tint = SilverText.copy(alpha = 0.3f), modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "لا توجد مراكز فيوتشرز مفتوحة حالياً.",
                color = PlatinumWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "يرجى تشغيل البوت التلقائي أو فتح صفقة يدوية فورية من التاب الرئيسي لاختبار إدارة جني الأرباح ووقف الخسارة المدمج.",
                color = SilverText,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
            items(openTrades) { trade ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ObsidianGrey80),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DividerSlate),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val sideColor = if (trade.side == "LONG") EmeraldProfit else CrimsonLoss
                            Text(
                                text = "${trade.symbol.split("/")[0]} ${trade.side} ${trade.leverage}x",
                                color = sideColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "جاري التداول..",
                                color = TealPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = DividerSlate, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(horizontalAlignment = Alignment.Start) {
                                Text("سعر الدخول", color = SilverText, fontSize = 11.sp)
                                Text("%.3f".format(trade.entryPrice), color = PlatinumWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Column(horizontalAlignment = Alignment.Start) {
                                Text("جني الأرباح (TP)", color = SilverText, fontSize = 11.sp)
                                Text("%.3f".format(trade.takeProfitPrice), color = EmeraldProfit, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Column(horizontalAlignment = Alignment.Start) {
                                Text("وقف الخسارة (SL)", color = SilverText, fontSize = 11.sp)
                                Text("%.3f".format(trade.stopLossPrice), color = CrimsonLoss, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "الكمية المفتوحة: %.5f".format(trade.quantity),
                                color = SilverText,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Button(
                                onClick = { viewModel.manualClosePosition(trade.id, trade.entryPrice) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF9DEDC),
                                    contentColor = CrimsonLoss
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("إغلاق ماركت 🚨", color = CrimsonLoss, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TradesHistoryView(viewModel: BotViewModel, tradesHistory: List<SimulatedTrade>) {
    val completedTrades = tradesHistory.filter { it.status != "OPEN" }

    if (completedTrades.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Filled.History, contentDescription = "Empty", tint = SilverText.copy(alpha = 0.3f), modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "سجل الصفقات فارغ حالياً.",
                color = PlatinumWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "الصفقات المنتهية عبر ضرب الأهداف الفنية (TP/SL) أو الإغلاق اليدوي تظهر هنا فور تصفيتها.",
                color = SilverText,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { viewModel.clearHistory() },
                    colors = ButtonDefaults.textButtonColors(contentColor = CrimsonLoss)
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("مسح السجل التاريخي", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                items(completedTrades) { trade ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = ObsidianGrey80),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DividerSlate),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val tradePnl = trade.pnl
                                val pnlColor = if (tradePnl >= 0.0) EmeraldProfit else CrimsonLoss
                                Text(
                                    text = (if (tradePnl >= 0.0) "+" else "") + "%.2f USDT".format(tradePnl),
                                    color = pnlColor,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 14.sp
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val statusMsg = when (trade.status) {
                                        "CLOSED_TP" -> "ضرب الهدف جني الأرباح ✅"
                                        "CLOSED_SL" -> "ضرب الهدف وقف الخسارة 🛑"
                                        else -> "إغلاق ماركت يدوي ⚠️"
                                    }
                                    Text(statusMsg, color = SilverText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${trade.symbol.split("/")[0]} ${trade.side}",
                                        color = PlatinumWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "تاريخ الإغلاق: " + SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(trade.exitTimestamp ?: trade.timestamp)),
                                    color = SilverText,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = "دخول: %.2f | خروج: %.2f".format(trade.entryPrice, trade.exitPrice ?: 0.0),
                                    color = SilverText,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BotConsoleLogsView(viewModel: BotViewModel, logs: List<TradingLog>) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = { viewModel.clearAllLogs() },
                colors = ButtonDefaults.textButtonColors(contentColor = SilverText)
            ) {
                Icon(Icons.Filled.ClearAll, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("مسح الشاشة", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Text("قائمة مراقبة المحيط الفني والقرارات:", color = SilverText, fontSize = 11.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxSize().weight(1f),
            colors = CardDefaults.cardColors(containerColor = DefaultBlackCarbon),
            shape = RoundedCornerShape(8.dp),
            border = borderSilverMedium
        ) {
            if (logs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "لا توجد أي عملية مسجلة الآن.\nسيتم تدوين الفحص الفني وأوامر الدخول هنا ثانية بثانية.",
                        color = SilverText,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(logs) { log ->
                        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
                        val levelColor = when (log.level) {
                            "TRIGGER" -> EmeraldProfit
                            "WARNING" -> AlertGolden
                            "ERROR" -> CrimsonLoss
                            else -> SilverText
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            // Message
                            Text(
                                text = log.message,
                                color = if (log.level == "TRIGGER") EmeraldProfit else Color(0xFFE2E9D8),
                                fontSize = 11.sp,
                                modifier = Modifier.weight(1f).padding(end = 4.dp),
                                textAlign = TextAlign.Right
                            )

                            // Status Level identifier
                            Text(
                                text = "[${log.level}]",
                                color = levelColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            // Timestamp
                            Text(
                                text = timeStr,
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PythonCodeTab() {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("python_tab")
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App intro
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ObsidianGrey80)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.End) {
                    Text(
                        text = "محاكي ومطوّر كود البوت (Python & CCXT)",
                        color = TealPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "لقد تمت صياغة الكود ليتناسب بشكل مثالي مع منصة MEXC Futures. يتضمن استخدام مكتبة CCXT Pro بالتوازي مع asyncio لمنع التجمد وتأخير البيانات، مع ضبط التحوط وتوزيع وقف الخسارة وجني الأرباح المناسب للسكالبنج.",
                        color = SilverText,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Right
                    )
                }
            }
        }

        // Action buttons: Copy & Share
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, PythonScriptProvider.ARABIC_PYTHON_SCRIPT)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "مشاركة كود البوت")
                        context.startActivity(shareIntent)
                    },
                    modifier = Modifier.weight(1f).testTag("share_code_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Share, contentDescription = "Share", tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("مشاركة الكود", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(PythonScriptProvider.ARABIC_PYTHON_SCRIPT))
                        Toast.makeText(context, "تم نسخ الكود البرمجي بالكامل للذاكرة!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f).testTag("copy_code_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("نسخ الكود بالكامل", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // Code block container
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DefaultBlackCarbon),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DividerSlate)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("main_scalper.py", color = Color.White.copy(alpha = 0.5f), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(TealPrimary.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Python 3 & CCXT", color = TealPrimary, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = DividerSlate.copy(alpha = 0.3f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Simple readable text-box showing python code
                    Text(
                        text = PythonScriptProvider.ARABIC_PYTHON_SCRIPT,
                        color = Color(0xFFE2E9D8),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        textAlign = TextAlign.Left
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsTab(viewModel: BotViewModel, config: BotConfiguration) {
    val context = LocalContext.current

    // Local form states
    var leverage by remember { mutableFloatStateOf(config.leverage.toFloat()) }
    var marginMode by remember { mutableStateOf(config.marginMode) }
    var positionSizePercent by remember { mutableStateOf(config.positionSizePercent.toString()) }
    var rsiOverbought by remember { mutableStateOf(config.rsiOverbought.toString()) }
    var rsiOversold by remember { mutableStateOf(config.rsiOversold.toString()) }
    var emaShortPeriod by remember { mutableStateOf(config.emaShortPeriod.toString()) }
    var emaLongPeriod by remember { mutableStateOf(config.emaLongPeriod.toString()) }

    var apiKey by remember { mutableStateOf(config.apiKey) }
    var apiSecret by remember { mutableStateOf(config.apiSecret) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_tab")
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // MEXC API Credentials Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ObsidianGrey80),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DividerSlate),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.End) {
                    Text(
                        text = "ربط المحفظة ومنصة التداول (MEXC API)",
                        color = TealPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "تدرج بيانات الاتصال في ملف بيئة .env للبوت الفعلي، ويتم حفظها محلياً للأمان التام.",
                        color = SilverText,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("MEXC API Key") },
                        colors = outlinedTextFieldColorsHex(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("api_key_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = apiSecret,
                        onValueChange = { apiSecret = it },
                        label = { Text("MEXC API Secret") },
                        colors = outlinedTextFieldColorsHex(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("api_secret_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            viewModel.updateKeys(apiKey, apiSecret)
                            Toast.makeText(context, "تم حفظ وتحديث مفاتيح MEXC محلياً بنجاح!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Text("حفظ المفاتيح 💾", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Futures Trading Parameters Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ObsidianGrey80),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DividerSlate),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.End) {
                    Text(
                        text = "بارامترات العقود والمخاطر الصارمة",
                        color = TealPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Leverage slider
                    Text(
                        text = "الرافعة المالية للعقود الآجلة: ${leverage.toInt()}x",
                        color = PlatinumWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Slider(
                        value = leverage,
                        onValueChange = { leverage = it },
                        valueRange = 1f..100f,
                        steps = 99,
                        colors = SliderDefaults.colors(
                            thumbColor = TealPrimary,
                            activeTrackColor = TealPrimary,
                            inactiveTrackColor = DividerSlate
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Margin Mode Toggle
                    Text(
                        text = "وضعية الهامش:",
                        color = PlatinumWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { marginMode = "CROSS" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (marginMode == "CROSS") TealPrimary else BottomNavBg,
                                contentColor = if (marginMode == "CROSS") Color.White else SilverText
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "متقاطع (CROSS)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = if (marginMode == "CROSS") Color.White else SilverText
                            )
                        }
                        Button(
                            onClick = { marginMode = "ISOLATED" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (marginMode == "ISOLATED") TealPrimary else BottomNavBg,
                                contentColor = if (marginMode == "ISOLATED") Color.White else SilverText
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "معزول (ISOLATED)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = if (marginMode == "ISOLATED") Color.White else SilverText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Position Size Percent input
                    OutlinedTextField(
                        value = positionSizePercent,
                        onValueChange = { positionSizePercent = it },
                        label = { Text("نسبة التخصيص من المحفظة لكل صفقة (%)") },
                        colors = outlinedTextFieldColorsHex(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Indicators configuration
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ObsidianGrey80),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DividerSlate),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.End) {
                    Text(
                        text = "معايرة ومؤشرات السكالبينج الفنية",
                        color = TealPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = rsiOversold,
                            onValueChange = { rsiOversold = it },
                            label = { Text("مستوى ذروة البيع (Buy)") },
                            colors = outlinedTextFieldColorsHex(),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = rsiOverbought,
                            onValueChange = { rsiOverbought = it },
                            label = { Text("مستوى ذروة الشراء (Sell)") },
                            colors = outlinedTextFieldColorsHex(),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = emaShortPeriod,
                            onValueChange = { emaShortPeriod = it },
                            label = { Text("فترة الأسي القصير (EMA Fast)") },
                            colors = outlinedTextFieldColorsHex(),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = emaLongPeriod,
                            onValueChange = { emaLongPeriod = it },
                            label = { Text("فترة الأسي الطويل (EMA Slow)") },
                            colors = outlinedTextFieldColorsHex(),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val lev = leverage.toInt()
                            val sizePct = positionSizePercent.toDoubleOrNull() ?: 5.0
                            val rsiOb = rsiOverbought.toDoubleOrNull() ?: 70.0
                            val rsiOs = rsiOversold.toDoubleOrNull() ?: 30.0
                            val emaS = emaShortPeriod.toIntOrNull() ?: 9
                            val emaL = emaLongPeriod.toIntOrNull() ?: 21

                            viewModel.updateConfig(
                                leverage = lev,
                                marginMode = marginMode,
                                positionSizePercent = sizePct,
                                rsiOverbought = rsiOb,
                                rsiOversold = rsiOs,
                                emaShortPeriod = emaS,
                                emaLongPeriod = emaL,
                                symbols = config.symbolsCsv.split(",").map { it.trim() }
                            )

                            Toast.makeText(context, "تم حفظ ومعايرة إعدادات التداول والمؤشرات بنجاح!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("حفظ ومعايرة الإعدادات الفنية 🎯", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// Visual layout helper objects and decorations
val DefaultBlackCarbon = Color(0xFF040609)
val borderSilverMedium = androidx.compose.foundation.BorderStroke(1.dp, DividerSlate)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun outlinedTextFieldColorsHex() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = TealPrimary,
    unfocusedBorderColor = DividerSlate,
    focusedLabelColor = TealPrimary,
    unfocusedLabelColor = SilverText,
    focusedTextColor = PlatinumWhite,
    unfocusedTextColor = PlatinumWhite
)
