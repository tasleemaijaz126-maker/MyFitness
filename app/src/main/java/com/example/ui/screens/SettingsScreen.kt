package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.GymSetting
import com.example.util.BiometricAuthManager
import com.example.util.BiometricResult
import com.example.util.BiometricStatus
import com.example.data.model.AppLanguage
import com.example.data.model.AppThemeMode
import com.example.data.firebase.AuthState
import com.example.data.firebase.FirebaseConfig
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.viewmodel.GymViewModel
import com.example.util.OwnerSignatureDisplay
import com.example.util.QrCodeView
import com.example.util.SignatureHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: GymViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.gymSettings.collectAsStateWithLifecycle()
    val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()

    var gymName by remember(settings) { mutableStateOf(settings.gymName) }
    var gymTagline by remember(settings) { mutableStateOf(settings.gymTagline) }
    var gymAddress by remember(settings) { mutableStateOf(settings.gymAddress) }
    var gymCity by remember(settings) { mutableStateOf(settings.gymCity) }
    var gymPhone by remember(settings) { mutableStateOf(settings.gymPhone) }
    var gymEmail by remember(settings) { mutableStateOf(settings.gymEmail) }
    var gymGstin by remember(settings) { mutableStateOf(settings.gymGstin) }
    var invoiceTerms by remember(settings) { mutableStateOf(settings.invoiceTerms) }
    var ownerSignatureName by remember(settings) { mutableStateOf(settings.ownerSignatureName) }
    var ownerSignatureStyleId by remember(settings) { mutableStateOf(settings.ownerSignatureStyleId) }
    var gymLocationUrl by remember(settings) { mutableStateOf(settings.gymLocationUrl) }
    var signatureCategory by remember { mutableStateOf("All") }
    var signatureSearchQuery by remember { mutableStateOf("") }
    var previewingSignatureStyle by remember { mutableStateOf<com.example.util.SignatureStyleOption?>(null) }

    // Biometric Security States
    val biometricStatus = remember(context) { BiometricAuthManager.checkBiometricStatus(context) }
    val activity = remember(context) { context as? FragmentActivity }
    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }

    // Reset Data Dialog State
    var showResetDialog by remember { mutableStateOf(false) }
    var resetConfirmInput by remember { mutableStateOf("") }
    var showFirebaseConfigDialog by remember { mutableStateOf(false) }
    var customProjectUrl by remember { mutableStateOf(FirebaseConfig.getProjectUrl(context)) }
    var customAnonKey by remember { mutableStateOf(FirebaseConfig.getAnonKey(context)) }

    val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(bottom = 100.dp, start = 16.dp, end = 16.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            Column {
                Text(
                    text = AppStrings.get("settings", appLanguage),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "System configuration, cloud backend, profile & preferences",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Section: Authenticated Gym Owner & Cloud Account
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(CrimsonPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = CrimsonPrimary, modifier = Modifier.size(28.dp))
                            }
                            Column {
                                val session = (authState as? AuthState.Authenticated)?.session
                                Text(
                                    text = session?.ownerName ?: "Gym Owner",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = session?.email ?: "owner@gym.com",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.signOut() },
                            modifier = Modifier.testTag("btn_sign_out")
                        ) {
                            Icon(Icons.Default.ExitToApp, contentDescription = "Sign Out", tint = CrimsonPrimary)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Cloud Sync Info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.CloudDone, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(18.dp))
                            Text(
                                text = syncStatus,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = EmeraldSuccess
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = { viewModel.syncWithCloud() },
                                enabled = !isSyncing
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Sync Now", fontSize = 12.sp)
                                }
                            }

                            IconButton(
                                onClick = { showFirebaseConfigDialog = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = "Config", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        // Section: Gym Business Profile
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = AppStrings.get("gym_profile", appLanguage),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(Icons.Default.Business, contentDescription = null, tint = CrimsonPrimary)
                    }

                    OutlinedTextField(
                        value = gymName,
                        onValueChange = { gymName = it },
                        label = { Text("Gym Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = gymTagline,
                        onValueChange = { gymTagline = it },
                        label = { Text("Tagline / Motto") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = gymPhone,
                        onValueChange = { gymPhone = it },
                        label = { Text("Helpline Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = gymEmail,
                        onValueChange = { gymEmail = it },
                        label = { Text("Business Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = gymAddress,
                        onValueChange = { gymAddress = it },
                        label = { Text("Street Address") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = gymCity,
                        onValueChange = { gymCity = it },
                        label = { Text("City & State") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = gymGstin,
                        onValueChange = { gymGstin = it },
                        label = { Text("GSTIN / Tax ID") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = invoiceTerms,
                        onValueChange = { invoiceTerms = it },
                        label = { Text("Invoice Terms & Conditions") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5
                    )

                    Button(
                        onClick = {
                            viewModel.updateGymSettings(
                                settings.copy(
                                    gymName = gymName.trim(),
                                    gymTagline = gymTagline.trim(),
                                    gymPhone = gymPhone.trim(),
                                    gymEmail = gymEmail.trim(),
                                    gymAddress = gymAddress.trim(),
                                    gymCity = gymCity.trim(),
                                    gymGstin = gymGstin.trim(),
                                    invoiceTerms = invoiceTerms.trim()
                                )
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(AppStrings.get("save_changes", appLanguage), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section: Owner Signature & Digital Signatory (112+ Signature Styles Gallery)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.testTag("card_owner_signature")
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Owner Signature Gallery",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "112+ distinct styles across 14 categories with auto-invoice rendering",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Default.Draw, contentDescription = null, tint = CrimsonPrimary)
                    }

                    // Owner Name Input
                    OutlinedTextField(
                        value = ownerSignatureName,
                        onValueChange = { ownerSignatureName = it },
                        label = { Text("Authorized Signatory Name") },
                        placeholder = { Text("e.g. Zameer Khan") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_signature_name"),
                        singleLine = true
                    )

                    // Active Selected Signature Preview Banner
                    val activeStyle = remember(ownerSignatureStyleId) {
                        SignatureHelper.styles.firstOrNull { it.id == ownerSignatureStyleId } ?: SignatureHelper.styles.first()
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Active Signature: ${activeStyle.name}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Category: ${activeStyle.category} • Style #${activeStyle.number}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = EmeraldSuccess
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("ACTIVE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 9.sp), color = Color.White)
                                }
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(6.dp)) {
                                OwnerSignatureDisplay(
                                    name = ownerSignatureName.ifBlank { "Owner Signature" },
                                    styleId = activeStyle.id,
                                    color = Color(0xFF0F172A),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                )
                            }
                        }
                    }

                    // Search & Category Filter
                    OutlinedTextField(
                        value = signatureSearchQuery,
                        onValueChange = { signatureSearchQuery = it },
                        placeholder = { Text("Search signature fonts by name or category...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (signatureSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { signatureSearchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_search_signatures"),
                        singleLine = true
                    )

                    // 14 Category Filter Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(SignatureHelper.categories) { cat ->
                            val isSelected = signatureCategory.equals(cat, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { signatureCategory = cat },
                                label = { Text(cat, fontSize = 12.sp) },
                                leadingIcon = {
                                    if (isSelected) Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CrimsonPrimary,
                                    selectedLabelColor = Color.White,
                                    selectedLeadingIconColor = Color.White
                                ),
                                modifier = Modifier.testTag("filter_sig_cat_${cat.lowercase()}")
                            )
                        }
                    }

                    // Filtered Styles List
                    val filteredStyles = remember(signatureCategory, signatureSearchQuery) {
                        SignatureHelper.styles.filter { item ->
                            val matchesCategory = signatureCategory == "All" || item.category.equals(signatureCategory, ignoreCase = true)
                            val matchesSearch = signatureSearchQuery.isBlank() ||
                                item.name.contains(signatureSearchQuery, ignoreCase = true) ||
                                item.category.contains(signatureSearchQuery, ignoreCase = true) ||
                                item.description.contains(signatureSearchQuery, ignoreCase = true)
                            matchesCategory && matchesSearch
                        }
                    }

                    Text(
                        text = "Available Styles (${filteredStyles.size})",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Signature Style Cards Carousel / Horizontal Row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(filteredStyles, key = { it.id }) { styleOption ->
                            val isSelected = ownerSignatureStyleId == styleOption.id
                            Surface(
                                modifier = Modifier
                                    .width(220.dp)
                                    .testTag("signature_card_${styleOption.id}"),
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) CrimsonPrimary.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    if (isSelected) 2.dp else 1.dp,
                                    if (isSelected) CrimsonPrimary else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = styleOption.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = if (isSelected) CrimsonPrimary else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = styleOption.category,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = "Selected",
                                                tint = CrimsonPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    // Signature Drawing Canvas Preview
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                            .background(Color.White, RoundedCornerShape(8.dp))
                                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                            .padding(4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        OwnerSignatureDisplay(
                                            name = ownerSignatureName.ifBlank { "Sample" },
                                            styleId = styleOption.id,
                                            color = Color(0xFF1E293B),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(44.dp)
                                        )
                                    }

                                    // Action Buttons: Select & Preview Signature
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { previewingSignatureStyle = styleOption },
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.ZoomIn, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Preview", fontSize = 11.sp)
                                        }

                                        Button(
                                            onClick = { ownerSignatureStyleId = styleOption.id },
                                            modifier = Modifier.weight(1.2f),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isSelected) EmeraldSuccess else CrimsonPrimary
                                            )
                                        ) {
                                            Text(
                                                text = if (isSelected) "Selected ✓" else "Select",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Save Signature Button
                    Button(
                        onClick = {
                            viewModel.updateOwnerSignature(ownerSignatureName, ownerSignatureStyleId)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_save_signature"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Signature & Sync to Cloud", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section: Gym Location & Google Maps QR Code
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Gym Location QR Code",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Dynamic Google Maps QR encoded onto customer invoices",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = CrimsonPrimary)
                    }

                    OutlinedTextField(
                        value = gymLocationUrl,
                        onValueChange = { gymLocationUrl = it },
                        label = { Text("Google Maps URL / Shortlink") },
                        placeholder = { Text("https://maps.app.goo.gl/... or https://maps.google.com/?q=...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3
                    )

                    // QR Code live preview row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val activeUrl = if (gymLocationUrl.isNotBlank()) {
                                gymLocationUrl
                            } else {
                                "https://maps.google.com/?q=${gymName.ifBlank { "My Fitness Gym" }}"
                            }
                            QrCodeView(
                                content = activeUrl,
                                modifier = Modifier.size(78.dp),
                                darkColor = android.graphics.Color.BLACK,
                                lightColor = android.graphics.Color.WHITE
                            )
                        }

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Scan to Open Location",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (gymLocationUrl.isNotBlank()) "Encoded: ${gymLocationUrl.take(35)}..." else "Default: Search query from Gym Name",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (gymLocationUrl.isNotBlank()) {
                                TextButton(
                                    onClick = {
                                        try {
                                            val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(gymLocationUrl.trim()))
                                            mapIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            context.startActivity(mapIntent)
                                        } catch (e: Exception) {
                                            viewModel.showToast("Could not open URL: ${e.localizedMessage}")
                                        }
                                    },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Test Open in Maps", fontSize = 12.sp, color = CrimsonPrimary)
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.updateGymLocation(gymLocationUrl)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Gym Location & Sync", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section: App Appearance & UI Style (4 Complete Themes)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "App Appearance / UI Style",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Choose from 4 full-system UI themes (Classic, Modern, Premium, Minimal)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }

                    // 4 Theme Cards Grid / Column
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        AppThemeMode.entries.forEach { mode ->
                            val isSelected = appTheme == mode
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setTheme(mode) }
                                    .testTag("theme_option_${mode.name.lowercase()}"),
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) Color(mode.primaryHex).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(
                                    if (isSelected) 2.dp else 1.dp,
                                    if (isSelected) Color(mode.primaryHex) else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(Color(mode.primaryHex))
                                                .border(2.dp, Color(mode.surfaceHex), CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = mode.displayName,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = if (isSelected) Color(mode.primaryHex) else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = mode.description,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    if (isSelected) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(mode.primaryHex)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("ACTIVE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 9.sp), color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Section: Default Invoice Template Selector (Silver, Gold, Platinum, Diamond)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Default Invoice Template",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "4 Professional Formats",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(com.example.data.model.InvoiceTemplates.availableTemplates) { template ->
                                val isSelected = settings.activeInvoiceTemplateId == template.id
                                Surface(
                                    modifier = Modifier
                                        .width(160.dp)
                                        .clickable { viewModel.selectActiveInvoiceTemplate(template.id) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) Color(template.headerColor).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                                    border = androidx.compose.foundation.BorderStroke(
                                        if (isSelected) 2.dp else 1.dp,
                                        if (isSelected) Color(template.accentColor) else MaterialTheme.colorScheme.outlineVariant
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(template.headerColor))
                                            )
                                            if (isSelected) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = EmeraldSuccess
                                                ) {
                                                    Text(
                                                        text = "ACTIVE",
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = template.name,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = template.subtitle,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Language selector
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "App Interface Language",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppLanguage.entries.forEach { lang ->
                                FilterChip(
                                    selected = appLanguage == lang,
                                    onClick = { viewModel.setLanguage(lang) },
                                    label = { Text("${lang.displayName} (${lang.nativeName})", fontSize = 12.sp) },
                                    leadingIcon = {
                                        if (appLanguage == lang) Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Biometric & App Lock Security (Fingerprint / Face ID)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.testTag("card_biometric_security")
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Fingerprint,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Biometric & App Security",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Fingerprint / Face ID lock for gym owner privacy",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Biometric Device Hardware Status Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = when (biometricStatus) {
                            BiometricStatus.AVAILABLE -> EmeraldSuccess.copy(alpha = 0.12f)
                            BiometricStatus.NOT_ENROLLED -> AmberAccent.copy(alpha = 0.15f)
                            else -> MaterialTheme.colorScheme.surface
                        },
                        border = BorderStroke(
                            1.dp,
                            when (biometricStatus) {
                                BiometricStatus.AVAILABLE -> EmeraldSuccess.copy(alpha = 0.4f)
                                BiometricStatus.NOT_ENROLLED -> AmberAccent.copy(alpha = 0.4f)
                                else -> MaterialTheme.colorScheme.outlineVariant
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = when (biometricStatus) {
                                    BiometricStatus.AVAILABLE -> Icons.Default.Shield
                                    BiometricStatus.NOT_ENROLLED -> Icons.Default.Warning
                                    else -> Icons.Default.Security
                                },
                                contentDescription = null,
                                tint = when (biometricStatus) {
                                    BiometricStatus.AVAILABLE -> EmeraldSuccess
                                    BiometricStatus.NOT_ENROLLED -> AmberAccent
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = when (biometricStatus) {
                                        BiometricStatus.AVAILABLE -> "Biometric Sensor Active & Ready"
                                        BiometricStatus.NOT_ENROLLED -> "Sensor Detected (No Biometrics Enrolled)"
                                        BiometricStatus.NO_HARDWARE -> "No Hardware Sensor (Passcode Available)"
                                        else -> "Biometrics Ready with KeyStore Guard"
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = biometricStatus.displayName,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Enable / Disable Customer OTP Verification
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Customer Mobile OTP Verification",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (settings.requireOtpForMemberCreation) "Require 6-digit SMS OTP before registering new members" else "Optional: Create members directly without SMS OTP",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = if (settings.requireOtpForMemberCreation) AmberAccent else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.requireOtpForMemberCreation,
                                onCheckedChange = { enabled ->
                                    viewModel.toggleOtpVerification(enabled)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.testTag("switch_customer_otp")
                            )
                        }
                    }

                    // Toggle: Enable Biometric Unlock
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Enable Biometric App Lock",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Require fingerprint or face scan when returning to app",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.isBiometricEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled && activity != null && biometricStatus.isAvailable) {
                                        // Verify biometric before turning on
                                        BiometricAuthManager.promptBiometric(
                                            activity = activity,
                                            title = "Enable Biometric Security",
                                            subtitle = "Scan your fingerprint or face to verify ownership",
                                            onResult = { result ->
                                                if (result is BiometricResult.Success) {
                                                    viewModel.toggleBiometricSecurity(true)
                                                } else {
                                                    viewModel.showToast("Biometric verification required to enable lock")
                                                }
                                            }
                                        )
                                    } else {
                                        viewModel.toggleBiometricSecurity(enabled)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.testTag("switch_biometric_lock")
                            )
                        }
                    }

                    // 4-Digit Backup Security PIN
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    Icons.Default.Key,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "Security Passcode (PIN)",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (settings.securityPin.isNotBlank()) "4-digit PIN is active & secured" else "No passcode set",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        color = if (settings.securityPin.isNotBlank()) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            TextButton(
                                onClick = {
                                    pinInput = settings.securityPin
                                    showPinDialog = true
                                },
                                modifier = Modifier.testTag("btn_configure_pin")
                            ) {
                                Text(
                                    text = if (settings.securityPin.isNotBlank()) "Change" else "Set PIN",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    // If biometric or PIN is configured, show auto-lock and quick test options
                    if (settings.isBiometricEnabled || settings.securityPin.isNotBlank()) {
                        // Test Biometric Verification Button
                        if (settings.isBiometricEnabled && biometricStatus.isAvailable && activity != null) {
                            OutlinedButton(
                                onClick = {
                                    BiometricAuthManager.promptBiometric(
                                        activity = activity,
                                        title = "Biometric Sensor Test",
                                        subtitle = "Testing fingerprint & face recognition hardware",
                                        onResult = { res ->
                                            when (res) {
                                                is BiometricResult.Success -> viewModel.showToast("Biometric verification verified successfully!")
                                                is BiometricResult.Failed -> viewModel.showToast("Verification failed. Fingerprint/face not recognized.")
                                                is BiometricResult.Cancelled -> viewModel.showToast("Test cancelled by user.")
                                                is BiometricResult.Error -> viewModel.showToast("Error: ${res.message}")
                                            }
                                        }
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_test_biometric"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Test Biometric Sensor", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // Auto-Lock Timeout Preference
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "Auto-Lock Timeout",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            val timeoutOptions = listOf(
                                Pair(0, "Immediately"),
                                Pair(1, "1 min"),
                                Pair(5, "5 mins"),
                                Pair(15, "15 mins"),
                                Pair(30, "30 mins"),
                                Pair(-1, "Never")
                            )

                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(timeoutOptions) { (mins, label) ->
                                    val isSelected = settings.biometricAutoLockMinutes == mins
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.setBiometricAutoLockMinutes(mins) },
                                        label = { Text(label, fontSize = 11.sp) },
                                        leadingIcon = {
                                            if (isSelected) Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        modifier = Modifier.testTag("filter_timeout_$mins")
                                    )
                                }
                            }
                        }

                        // Instant Lock Button
                        Button(
                            onClick = {
                                viewModel.lockApp()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("btn_lock_app_now"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Lock App Session Now", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Section: System Alerts & Activity Logs
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = AmberAccent)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "System Notifications & Audit Log",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (notifications.isNotEmpty()) {
                            TextButton(onClick = { viewModel.markAllNotificationsAsRead() }) {
                                Text("Mark Read", fontSize = 12.sp, color = CrimsonPrimary)
                            }
                        }
                    }

                    if (notifications.isEmpty()) {
                        Text(
                            text = "No alerts logged yet. New registrations and payments will appear here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            notifications.take(5).forEach { notif ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(if (notif.isRead) Color.Gray else CrimsonPrimary)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(notif.title, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                            Text(notif.message, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(dateFormat.format(Date(notif.timestamp)), style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Destructive Reset All Data
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Text(
                                text = "Reset All Gym Data",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Text(
                        text = "Resetting your data will permanently delete your gym customers, memberships, payments, invoices, plans, reports and other application data from Cloud Firestore. This action cannot be undone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            resetConfirmInput = ""
                            showResetDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_reset_all_data"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reset All Gym Data", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Reset Confirmation Dialog with "RESET" string requirement
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text("Permanent Factory Reset")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "This will delete all customer records, payments, active memberships, invoices, and analytics for your gym account.\n\nTo confirm, please type RESET in the box below:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = resetConfirmInput,
                        onValueChange = { resetConfirmInput = it },
                        placeholder = { Text("Type RESET to confirm") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_reset_confirmation")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllData(resetConfirmInput) { success ->
                            if (success) {
                                showResetDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    enabled = resetConfirmInput.trim() == "RESET",
                    modifier = Modifier.testTag("btn_confirm_reset_execute")
                ) {
                    Text("Permanently Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Cloud Configuration Dialog
    if (showFirebaseConfigDialog) {
        AlertDialog(
            onDismissRequest = { showFirebaseConfigDialog = false },
            title = { Text("Firebase Cloud Credentials") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Configured Firebase Cloud Firestore instance for multi-gym data isolation and permanent cloud storage.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = customProjectUrl,
                        onValueChange = { customProjectUrl = it },
                        label = { Text("Firebase Project ID") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = customAnonKey,
                        onValueChange = { customAnonKey = it },
                        label = { Text("Web API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveFirebaseConfig(customProjectUrl, customAnonKey)
                        showFirebaseConfigDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                ) {
                    Text("Save & Reconnect", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFirebaseConfigDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Signature Style Full High-Res Zoom Preview Dialog
    previewingSignatureStyle?.let { style ->
        AlertDialog(
            onDismissRequest = { previewingSignatureStyle = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(style.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text("Category: ${style.category} • Style #${style.number}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { previewingSignatureStyle = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = style.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Zoom Preview Canvas
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(12.dp)) {
                            OwnerSignatureDisplay(
                                name = ownerSignatureName.ifBlank { "Authorized Signatory" },
                                styleId = style.id,
                                color = Color(0xFF0F172A),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(90.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Font: ${style.fontFileName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Flourish: ${style.flourishType.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        ownerSignatureStyleId = style.id
                        viewModel.updateOwnerSignature(ownerSignatureName, style.id)
                        previewingSignatureStyle = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                ) {
                    Text("Select & Apply Signature", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { previewingSignatureStyle = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Passcode / Security PIN Configuration Dialog
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Backup 4-Digit Passcode")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Set a 4-digit backup PIN to unlock the app if biometrics fail or are temporarily unavailable.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) pinInput = it },
                        label = { Text("4-Digit Passcode") },
                        placeholder = { Text("e.g. 1234") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_security_pin"),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInput.length == 4) {
                            viewModel.setSecurityPin(pinInput)
                            showPinDialog = false
                        } else {
                            viewModel.showToast("Please enter a complete 4-digit PIN")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("btn_save_pin")
                ) {
                    Text("Save Passcode", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row {
                    if (settings.securityPin.isNotBlank()) {
                        TextButton(
                            onClick = {
                                viewModel.setSecurityPin("")
                                showPinDialog = false
                            }
                        ) {
                            Text("Remove PIN", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    TextButton(onClick = { showPinDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}
