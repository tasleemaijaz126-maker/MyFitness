package com.example

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.Invoice
import com.example.data.firebase.AuthState
import com.example.ui.i18n.AppStrings
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BiometricLockScreen
import com.example.ui.screens.CreateMembershipScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InvoicesScreen
import com.example.ui.screens.MembersScreen
import com.example.ui.screens.PlansScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MyFitnessTheme
import com.example.ui.viewmodel.GymViewModel

sealed class Screen(val route: String, val titleKey: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "dashboard", Icons.Default.Dashboard)
    object Members : Screen("members", "memberships", Icons.Default.People)
    object Plans : Screen("plans", "plans", Icons.Default.FitnessCenter)
    object Invoices : Screen("invoices", "invoices", Icons.Default.Receipt)
    object Reports : Screen("reports", "reports", Icons.Default.Assessment)
    object CreateMember : Screen("create_member", "create_member", Icons.Default.PersonAdd)
    object Settings : Screen("settings", "settings", Icons.Default.Settings)
}

class MainActivity : FragmentActivity() {

    private val gymViewModel: GymViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        com.example.util.SignatureHelper.initialize(applicationContext)

        setContent {
            val appTheme by gymViewModel.appTheme.collectAsStateWithLifecycle()
            val authState by gymViewModel.authState.collectAsStateWithLifecycle()
            val gymSettings by gymViewModel.gymSettings.collectAsStateWithLifecycle()
            val isAppLocked by gymViewModel.isAppLocked.collectAsStateWithLifecycle()
            val toastMessage by gymViewModel.toastMessage.collectAsStateWithLifecycle()

            var isSplashFinished by remember { mutableStateOf(false) }
            val snackbarHostState = remember { SnackbarHostState() }

            LaunchedEffect(toastMessage) {
                toastMessage?.let { msg ->
                    snackbarHostState.showSnackbar(msg)
                    gymViewModel.clearToast()
                }
            }

            // Listen to Firebase Auth state in main entry point to guarantee Gym ID scoping across operations
            LaunchedEffect(authState) {
                if (authState is AuthState.Authenticated) {
                    val ownerSession = (authState as AuthState.Authenticated).session
                    gymViewModel.onOwnerAuthenticated(ownerSession.userId)
                }
            }

            MyFitnessTheme(themeMode = appTheme) {
                AnimatedContent(
                    targetState = when {
                        !isSplashFinished || authState is AuthState.Checking -> "SPLASH"
                        authState !is AuthState.Authenticated -> "AUTH"
                        isAppLocked -> "LOCKED"
                        else -> "APP"
                    },
                    transitionSpec = {
                        fadeIn(tween(350)) togetherWith fadeOut(tween(250))
                    },
                    label = "root_navigation"
                ) { state ->
                    when (state) {
                        "SPLASH" -> {
                            SplashScreen(
                                isReadyToTransition = authState !is AuthState.Checking,
                                onSplashFinished = { isSplashFinished = true }
                            )
                        }
                        "AUTH" -> {
                            AuthScreen(viewModel = gymViewModel)
                        }
                        "LOCKED" -> {
                            BiometricLockScreen(
                                viewModel = gymViewModel,
                                gymSetting = gymSettings
                            )
                        }
                        "APP" -> {
                            MainAppContent(
                                viewModel = gymViewModel,
                                snackbarHostState = snackbarHostState
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        gymViewModel.onAppForegrounded()
    }

    override fun onPause() {
        super.onPause()
        gymViewModel.onAppBackgrounded()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(
    viewModel: GymViewModel,
    snackbarHostState: SnackbarHostState
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
    var selectedInvoiceForPreview by remember { mutableStateOf<Invoice?>(null) }
    var membersFilterArg by remember { mutableStateOf<String?>("ALL") }

    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val gymSettings by viewModel.gymSettings.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val unreadNotificationsCount = remember(notifications) { notifications.count { !it.isRead } }

    val navItems = listOf(
        Screen.Dashboard,
        Screen.Members,
        Screen.Plans,
        Screen.Invoices,
        Screen.Reports
    )

    BackHandler(enabled = currentScreen != Screen.Dashboard) {
        currentScreen = Screen.Dashboard
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            if (currentScreen != Screen.CreateMember) {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FitnessCenter,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = gymSettings.gymName.ifEmpty { AppStrings.get("app_title", appLanguage) },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.3.sp
                                ),
                                maxLines = 1
                            )
                        }
                    },
                    actions = {
                        if (gymSettings.isBiometricEnabled) {
                            IconButton(
                                onClick = { viewModel.lockApp() },
                                modifier = Modifier.testTag("quick_lock_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Lock Session",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Quick Action to Settings Screen
                        IconButton(
                            onClick = { currentScreen = Screen.Settings },
                            modifier = Modifier.testTag("settings_top_btn")
                        ) {
                            Box {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = if (currentScreen == Screen.Settings) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                if (unreadNotificationsCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                            .align(Alignment.TopEnd)
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        bottomBar = {
            if (currentScreen != Screen.CreateMember) {
                NavigationBar(
                    modifier = Modifier.testTag("main_navigation_bar"),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    navItems.forEach { screen ->
                        val isSelected = currentScreen == screen
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentScreen = screen },
                            icon = {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = AppStrings.get(screen.titleKey, appLanguage)
                                )
                            },
                            label = {
                                Text(
                                    text = AppStrings.get(screen.titleKey, appLanguage),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 10.sp
                                    )
                                )
                            },
                            modifier = Modifier.testTag("nav_item_${screen.route}"),
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CrimsonPrimary,
                                selectedTextColor = CrimsonPrimary,
                                indicatorColor = CrimsonPrimary.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "screen_transition",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) { screen ->
            when (screen) {
                Screen.Dashboard -> {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToCreateMember = { currentScreen = Screen.CreateMember },
                        onNavigateToMembers = { filter ->
                            if (filter != null) viewModel.setStatusFilter(filter)
                            currentScreen = Screen.Members
                        },
                        onNavigateToPlans = { currentScreen = Screen.Plans },
                        onNavigateToInvoices = { invoice ->
                            selectedInvoiceForPreview = invoice
                            currentScreen = Screen.Invoices
                        },
                        onNavigateToReports = { currentScreen = Screen.Reports }
                    )
                }

                Screen.Members -> {
                    MembersScreen(
                        viewModel = viewModel,
                        initialFilter = membersFilterArg,
                        onNavigateToCreateMember = { currentScreen = Screen.CreateMember },
                        onNavigateToInvoices = { invoice ->
                            selectedInvoiceForPreview = invoice
                            currentScreen = Screen.Invoices
                        }
                    )
                }

                Screen.Plans -> {
                    PlansScreen(viewModel = viewModel)
                }

                Screen.Invoices -> {
                    InvoicesScreen(
                        viewModel = viewModel,
                        initialSelectedInvoice = selectedInvoiceForPreview
                    )
                }

                Screen.Reports -> {
                    ReportsScreen(viewModel = viewModel)
                }

                Screen.CreateMember -> {
                    CreateMembershipScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = Screen.Dashboard },
                        onNavigateToInvoices = { invoiceNum ->
                            val inv = viewModel.allInvoices.value.find { it.invoiceNumber == invoiceNum }
                            selectedInvoiceForPreview = inv
                            currentScreen = Screen.Invoices
                        }
                    )
                }

                Screen.Settings -> {
                    SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }
}
