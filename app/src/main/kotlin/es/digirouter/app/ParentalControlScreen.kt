package es.digirouter.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.routerapp.protocol.pages.ConnectedDevice
import es.routerapp.protocol.parental.BlacklistEntry
import es.routerapp.protocol.parental.FilterCategories
import es.routerapp.protocol.parental.FilterLevel
import es.routerapp.protocol.parental.ParentalProfile
import es.routerapp.protocol.parental.ProfileDevice
import es.routerapp.protocol.parental.ProfileStatus
import es.routerapp.protocol.parental.TimeLimitConfig
import es.routerapp.protocol.parental.WebInsights

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentalControlScreen(
    viewModel: ParentalControlViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    var editingProfile by remember { mutableStateOf<ParentalProfile?>(null) }
    var isNewProfile by remember { mutableStateOf(false) }
    var profileToDelete by remember { mutableStateOf<ParentalProfile?>(null) }
    var inspectingProfile by remember { mutableStateOf<ParentalProfile?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.parental_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::loadData, enabled = !state.isLoading) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = {
                        viewModel.clearError()
                        editingProfile = ParentalProfile(name = "")
                        isNewProfile = true
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Profile")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading && state.profiles.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ContainedLoadingIndicator()
                    }
                }
                state.errorMessage != null && state.profiles.isEmpty() && editingProfile == null -> {
                    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(16.dp))
                            Text(state.errorMessage ?: "", color = MaterialTheme.colorScheme.error, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = viewModel::loadData) {
                                Text("Retry")
                            }
                        }
                    }
                }
                state.profiles.isEmpty() -> {
                    EmptyProfilesView(onAddClick = {
                        viewModel.clearError()
                        editingProfile = ParentalProfile(name = "")
                        isNewProfile = true
                    })
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            ParentalSummaryBanner(
                                profilesCount = state.profiles.size,
                                activeProfilesCount = state.profiles.count { it.status == ProfileStatus.ACTIVE },
                                totalDevicesCount = state.profiles.sumOf { it.devices.size }
                            )
                        }

                        items(state.profiles, key = { it.stack }) { profile ->
                            ProfileCard(
                                profile = profile,
                                onTogglePause = { viewModel.togglePause(profile) },
                                onEdit = {
                                    editingProfile = profile
                                    isNewProfile = false
                                },
                                onDelete = { profileToDelete = profile },
                                onOpenInsights = {
                                    inspectingProfile = profile
                                    viewModel.loadInsights(profile.stack, dateOffset = 1)
                                }
                            )
                        }
                    }
                }
            }

            // Edit / Create Sheet
            editingProfile?.let { profile ->
                ProfileEditorBottomSheet(
                    profile = profile,
                    isNew = isNewProfile,
                    activeDevices = state.activeDevices,
                    allProfiles = state.profiles,
                    isSaving = state.isSaving,
                    errorMessage = state.errorMessage,
                    onClearError = viewModel::clearError,
                    onDismiss = {
                        editingProfile = null
                        viewModel.clearError()
                    },
                    onSave = { updated ->
                        viewModel.saveProfile(updated, isNewProfile) {
                            editingProfile = null
                        }
                    }
                )
            }

            // Web Insights Sheet
            inspectingProfile?.let { profile ->
                WebInsightsSheet(
                    profile = profile,
                    insights = state.selectedProfileInsights,
                    isLoading = state.isLoadingInsights,
                    onDismiss = { inspectingProfile = null },
                    onDateSelected = { offset -> viewModel.loadInsights(profile.stack, offset) },
                    onUnblockUrl = { url -> viewModel.unblockUrl(profile.stack, url) }
                )
            }

            // Delete Confirmation
            profileToDelete?.let { profile ->
                AlertDialog(
                    onDismissRequest = { profileToDelete = null },
                    title = { Text(stringResource(R.string.parental_delete_profile)) },
                    text = { Text(stringResource(R.string.parental_delete_confirm, profile.name)) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.deleteProfile(profile.stack) {
                                    profileToDelete = null
                                }
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(stringResource(R.string.backup_action_delete))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { profileToDelete = null }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ParentalSummaryBanner(
    profilesCount: Int,
    activeProfilesCount: Int,
    totalDevicesCount: Int,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(MaterialTheme.colorScheme.tertiary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiary, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = stringResource(R.string.parental_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = "$activeProfilesCount / $profilesCount profiles online • $totalDevicesCount device(s)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun ProfileCard(
    profile: ParentalProfile,
    onTogglePause: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenInsights: () -> Unit,
) {
    val isPaused = profile.status == ProfileStatus.PAUSED
    val c = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPaused) c.surfaceContainerLow else c.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                if (isPaused) c.errorContainer else c.primaryContainer,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = profile.name.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isPaused) c.onErrorContainer else c.onPrimaryContainer
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = profile.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.parental_devices_count, profile.devices.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = c.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isPaused) c.errorContainer else c.primaryContainer,
                ) {
                    Text(
                        text = stringResource(if (isPaused) R.string.parental_status_paused else R.string.parental_status_active),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isPaused) c.onErrorContainer else c.onPrimaryContainer
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Quota / Bedtime summary chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (profile.weekdayLimit.timeLimitEnable) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = c.surfaceContainerHighest,
                    ) {
                        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(14.dp), tint = c.primary)
                            Spacer(Modifier.width(4.dp))
                            Text("${profile.weekdayLimit.timeLimitMinutes} min limit", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                if (profile.weekdayLimit.bedTimeEnable) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = c.surfaceContainerHighest,
                    ) {
                        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Bedtime, contentDescription = null, modifier = Modifier.size(14.dp), tint = c.tertiary)
                            Spacer(Modifier.width(4.dp))
                            Text("${profile.weekdayLimit.bedTimeBeginFormatted} - ${profile.weekdayLimit.bedTimeEndFormatted}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = c.surfaceContainerHighest,
                ) {
                    Text(
                        text = profile.filterLevel.name,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = c.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = c.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(12.dp))

            // Card Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onTogglePause,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPaused) c.primary else c.error
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(if (isPaused) R.string.parental_action_resume else R.string.parental_action_pause),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row {
                    IconButton(onClick = onOpenInsights) {
                        Icon(Icons.Filled.Insights, contentDescription = "Insights", tint = c.primary)
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = c.onSurfaceVariant)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = c.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyProfilesView(onAddClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.parental_empty_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.parental_empty_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onAddClick,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.parental_add_profile))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditorBottomSheet(
    profile: ParentalProfile,
    isNew: Boolean,
    activeDevices: List<ConnectedDevice>,
    allProfiles: List<ParentalProfile>,
    isSaving: Boolean,
    errorMessage: String? = null,
    onClearError: () -> Unit = {},
    onDismiss: () -> Unit,
    onSave: (ParentalProfile) -> Unit,
) {
    var name by remember { mutableStateOf(profile.name) }
    var filterLevel by remember { mutableStateOf(profile.filterLevel) }
    val selectedDevices = remember { mutableStateListOf<ProfileDevice>().apply { addAll(profile.devices) } }
    var weekdayTimeLimitEnable by remember { mutableStateOf(profile.weekdayLimit.timeLimitEnable) }
    var weekdayTimeLimitMins by remember { mutableIntStateOf(profile.weekdayLimit.timeLimitMinutes) }
    var weekdayBedTimeEnable by remember { mutableStateOf(profile.weekdayLimit.bedTimeEnable) }
    var weekdayBedTimeBegin by remember { mutableIntStateOf(profile.weekdayLimit.bedTimeBeginMinutes) }
    var weekdayBedTimeEnd by remember { mutableIntStateOf(profile.weekdayLimit.bedTimeEndMinutes) }

    var weekendTimeLimitEnable by remember { mutableStateOf(profile.weekendLimit.timeLimitEnable) }
    var weekendTimeLimitMins by remember { mutableIntStateOf(profile.weekendLimit.timeLimitMinutes) }
    var weekendBedTimeEnable by remember { mutableStateOf(profile.weekendLimit.bedTimeEnable) }
    var weekendBedTimeBegin by remember { mutableIntStateOf(profile.weekendLimit.bedTimeBeginMinutes) }
    var weekendBedTimeEnd by remember { mutableIntStateOf(profile.weekendLimit.bedTimeEndMinutes) }

    var filterCategories by remember { mutableStateOf(profile.filterContent) }
    val blacklist = remember { mutableStateListOf<BlacklistEntry>().apply { addAll(profile.blackList) } }
    var newBlacklistUrl by remember { mutableStateOf("") }

    val illegalCharsRegex = remember { Regex("""[\\/":*?<>|&]""") }
    val trimmedName = name.trim()
    val nameError = when {
        name.isEmpty() -> null
        trimmedName.isEmpty() -> stringResource(R.string.parental_name_error_blank)
        name.startsWith(" ") || name.endsWith(" ") -> stringResource(R.string.parental_name_error_whitespace)
        name.contains(illegalCharsRegex) -> stringResource(R.string.parental_name_error_chars)
        allProfiles.any { it.stack != profile.stack && it.name.equals(trimmedName, ignoreCase = true) } -> stringResource(R.string.parental_name_error_duplicate)
        trimmedName.length > 32 -> "Name must be 32 characters or less"
        else -> null
    }
    val isNameValid = trimmedName.isNotEmpty() && nameError == null

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.parental_profile_name),
        stringResource(R.string.parental_step_devices),
        stringResource(R.string.parental_step_schedule),
        stringResource(R.string.parental_step_content)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberBottomSheetState(SheetValue.Expanded)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = if (isNew) stringResource(R.string.parental_add_profile) else stringResource(R.string.parental_edit_profile),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            PrimaryTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                when (selectedTab) {
                    0 -> {
                        // Profile Details & Preset
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            OutlinedTextField(
                                value = name,
                                onValueChange = {
                                    name = it
                                    if (errorMessage != null) onClearError()
                                },
                                label = { Text(stringResource(R.string.parental_profile_name)) },
                                placeholder = { Text(stringResource(R.string.parental_profile_name_hint)) },
                                isError = nameError != null,
                                supportingText = nameError?.let { err -> { Text(err, color = MaterialTheme.colorScheme.error) } },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Text(
                                text = stringResource(R.string.parental_filter_level),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterLevel.entries.forEach { level ->
                                    FilterChip(
                                        selected = filterLevel == level,
                                        onClick = {
                                            filterLevel = level
                                            if (level != FilterLevel.CUSTOM) {
                                                filterCategories = FilterCategories.forLevel(level)
                                            }
                                        },
                                        label = {
                                            Text(
                                                when (level) {
                                                    FilterLevel.CHILD -> stringResource(R.string.parental_level_child)
                                                    FilterLevel.PRE_TEEN -> stringResource(R.string.parental_level_preteen)
                                                    FilterLevel.TEEN -> stringResource(R.string.parental_level_teen)
                                                    FilterLevel.ADULT -> stringResource(R.string.parental_level_adult)
                                                    FilterLevel.CUSTOM -> stringResource(R.string.parental_level_custom)
                                                }
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                    1 -> {
                        // Live Device Picker
                        LiveDevicePicker(
                            activeDevices = activeDevices,
                            selectedDevices = selectedDevices,
                            allProfiles = allProfiles,
                            currentProfileStack = profile.stack,
                            onDeviceToggled = { dev ->
                                val existing = selectedDevices.find { it.deviceMac.equals(dev.mac, ignoreCase = true) }
                                if (existing != null) {
                                    selectedDevices.remove(existing)
                                } else {
                                    selectedDevices.add(
                                        ProfileDevice(
                                            deviceMac = dev.mac,
                                            entryName = dev.name,
                                            clientType = if (dev.wifi) "Phone" else "PC",
                                            pstack = profile.stack
                                        )
                                    )
                                }
                            }
                        )
                    }
                    2 -> {
                        // Weekday / Weekend Schedules & Bedtime
                        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            Text(stringResource(R.string.parental_schedule_weekdays), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.parental_limit_enable))
                                Switch(checked = weekdayTimeLimitEnable, onCheckedChange = { weekdayTimeLimitEnable = it })
                            }
                            if (weekdayTimeLimitEnable) {
                                Text("${weekdayTimeLimitMins} minutes (${weekdayTimeLimitMins / 60}h ${weekdayTimeLimitMins % 60}m)", style = MaterialTheme.typography.bodySmall)
                                @Suppress("DEPRECATION")
                                Slider(
                                    value = weekdayTimeLimitMins.toFloat(),
                                    onValueChange = { weekdayTimeLimitMins = it.toInt() },
                                    valueRange = 15f..480f,
                                    steps = 30
                                )
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.parental_bedtime_enable))
                                Switch(checked = weekdayBedTimeEnable, onCheckedChange = { weekdayBedTimeEnable = it })
                            }
                            if (weekdayBedTimeEnable) {
                                Text("Bedtime: ${TimeLimitConfig.formatTime(weekdayBedTimeBegin)} to ${TimeLimitConfig.formatTime(weekdayBedTimeEnd)}", style = MaterialTheme.typography.bodySmall)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Column(Modifier.weight(1f)) {
                                        Text("Starts: ${TimeLimitConfig.formatTime(weekdayBedTimeBegin)}", style = MaterialTheme.typography.labelSmall)
                                        @Suppress("DEPRECATION")
                                        Slider(
                                            value = weekdayBedTimeBegin.toFloat(),
                                            onValueChange = { weekdayBedTimeBegin = (it.toInt() / 15) * 15 },
                                            valueRange = 1080f..1439f
                                        )
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text("Ends: ${TimeLimitConfig.formatTime(weekdayBedTimeEnd)}", style = MaterialTheme.typography.labelSmall)
                                        @Suppress("DEPRECATION")
                                        Slider(
                                            value = weekdayBedTimeEnd.toFloat(),
                                            onValueChange = { weekdayBedTimeEnd = (it.toInt() / 15) * 15 },
                                            valueRange = 240f..600f
                                        )
                                    }
                                }
                            }

                            HorizontalDivider()

                            Text(stringResource(R.string.parental_schedule_weekends), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.parental_limit_enable))
                                Switch(checked = weekendTimeLimitEnable, onCheckedChange = { weekendTimeLimitEnable = it })
                            }
                            if (weekendTimeLimitEnable) {
                                Text("${weekendTimeLimitMins} minutes (${weekendTimeLimitMins / 60}h ${weekendTimeLimitMins % 60}m)", style = MaterialTheme.typography.bodySmall)
                                @Suppress("DEPRECATION")
                                Slider(
                                    value = weekendTimeLimitMins.toFloat(),
                                    onValueChange = { weekendTimeLimitMins = it.toInt() },
                                    valueRange = 15f..600f,
                                    steps = 38
                                )
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.parental_bedtime_enable))
                                Switch(checked = weekendBedTimeEnable, onCheckedChange = { weekendBedTimeEnable = it })
                            }
                            if (weekendBedTimeEnable) {
                                Text("Bedtime: ${TimeLimitConfig.formatTime(weekendBedTimeBegin)} to ${TimeLimitConfig.formatTime(weekendBedTimeEnd)}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    3 -> {
                        // Content Categories & Blacklist
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(stringResource(R.string.parental_step_content), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                            CategoryFilterRow(stringResource(R.string.parental_category_adult), filterCategories.adultContentEnable) {
                                filterCategories = filterCategories.copy(adultContentEnable = it)
                            }
                            CategoryFilterRow(stringResource(R.string.parental_category_gambling), filterCategories.gamblingEnable) {
                                filterCategories = filterCategories.copy(gamblingEnable = it)
                            }
                            CategoryFilterRow(stringResource(R.string.parental_category_social), filterCategories.socialNetworkingEnable) {
                                filterCategories = filterCategories.copy(socialNetworkingEnable = it)
                            }
                            CategoryFilterRow(stringResource(R.string.parental_category_games), filterCategories.gamesEnable) {
                                filterCategories = filterCategories.copy(gamesEnable = it)
                            }
                            CategoryFilterRow(stringResource(R.string.parental_category_media), filterCategories.mediaEnable) {
                                filterCategories = filterCategories.copy(mediaEnable = it)
                            }
                            CategoryFilterRow(stringResource(R.string.parental_category_downloads), filterCategories.downLoadEnable) {
                                filterCategories = filterCategories.copy(downLoadEnable = it)
                            }

                            HorizontalDivider()

                            Text(stringResource(R.string.parental_blacklist_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = newBlacklistUrl,
                                    onValueChange = { newBlacklistUrl = it },
                                    placeholder = { Text(stringResource(R.string.parental_blacklist_hint)) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                Button(
                                    onClick = {
                                        val trimmed = newBlacklistUrl.trim().lowercase()
                                        if (trimmed.isNotEmpty() && blacklist.none { it.url.equals(trimmed, ignoreCase = true) }) {
                                            blacklist.add(BlacklistEntry(url = trimmed, pstack = profile.stack))
                                            newBlacklistUrl = ""
                                        }
                                    },
                                    enabled = newBlacklistUrl.isNotBlank()
                                ) {
                                    Text(stringResource(R.string.parental_blacklist_add))
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                blacklist.forEach { entry ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Filled.Block, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(8.dp))
                                                Text(entry.url, style = MaterialTheme.typography.bodyMedium)
                                            }
                                            IconButton(onClick = { blacklist.remove(entry) }, modifier = Modifier.size(24.dp)) {
                                                Icon(Icons.Filled.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            errorMessage?.let { error ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onClearError, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(onClick = onDismiss, enabled = !isSaving) {
                    Text("Cancel")
                }
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = {
                        val updated = profile.copy(
                            name = trimmedName,
                            filterLevel = filterLevel,
                            devices = selectedDevices.toList(),
                            weekdayLimit = profile.weekdayLimit.copy(
                                timeLimitEnable = weekdayTimeLimitEnable,
                                timeLimitMinutes = weekdayTimeLimitMins,
                                bedTimeEnable = weekdayBedTimeEnable,
                                bedTimeBeginMinutes = weekdayBedTimeBegin,
                                bedTimeEndMinutes = weekdayBedTimeEnd
                            ),
                            weekendLimit = profile.weekendLimit.copy(
                                timeLimitEnable = weekendTimeLimitEnable,
                                timeLimitMinutes = weekendTimeLimitMins,
                                bedTimeEnable = weekendBedTimeEnable,
                                bedTimeBeginMinutes = weekendBedTimeBegin,
                                bedTimeEndMinutes = weekendBedTimeEnd
                            ),
                            filterContent = filterCategories,
                            blackList = blacklist.toList()
                        )
                        onSave(updated)
                    },
                    enabled = !isSaving && isNameValid
                ) {
                    if (isSaving) {
                        ContainedLoadingIndicator()
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Save Profile")
                }
            }
        }
    }
}

@Composable
private fun CategoryFilterRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun LiveDevicePicker(
    activeDevices: List<ConnectedDevice>,
    selectedDevices: List<ProfileDevice>,
    allProfiles: List<ParentalProfile>,
    currentProfileStack: String,
    onDeviceToggled: (ConnectedDevice) -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = activeDevices.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.mac.contains(searchQuery, ignoreCase = true)
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.parental_live_devices_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.parental_live_devices_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by name or MAC...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.parental_live_devices_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                filtered.forEach { device ->
                    val isSelected = selectedDevices.any { it.deviceMac.equals(device.mac, ignoreCase = true) }
                    val assignedProfile = allProfiles.find { p ->
                        p.stack != currentProfileStack && p.devices.any { it.deviceMac.equals(device.mac, ignoreCase = true) }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDeviceToggled(device) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { onDeviceToggled(device) }
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                imageVector = if (device.wifi) Icons.Filled.Wifi else Icons.Filled.Lan,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = device.name.ifBlank { "Device" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${device.mac} • ${device.ip}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (assignedProfile != null) {
                                    Text(
                                        text = stringResource(R.string.parental_device_already_assigned, assignedProfile.name),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Medium
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebInsightsSheet(
    profile: ParentalProfile,
    insights: WebInsights?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onDateSelected: (Int) -> Unit,
    onUnblockUrl: (String) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberBottomSheetState(SheetValue.Expanded)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.parental_insights_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }

            Spacer(Modifier.height(16.dp))

            // Date selector chips: 1 = Today, 2 = Yesterday, 3 = 2 Days Ago
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    1 to stringResource(R.string.parental_insights_date_today),
                    2 to stringResource(R.string.parental_insights_date_yesterday),
                    3 to stringResource(R.string.parental_insights_date_2d)
                ).forEach { (offset, label) ->
                    FilterChip(
                        selected = insights?.dateOffset == offset,
                        onClick = { onDateSelected(offset) },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (isLoading) {
                Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                    ContainedLoadingIndicator()
                }
            } else if (insights != null) {
                // Online Time Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text(
                            text = stringResource(R.string.parental_insights_time_spent),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.height(8.dp))
                        val hours = insights.dailyTimeUsedMinutes / 60
                        val mins = insights.dailyTimeUsedMinutes % 60
                        Text(
                            text = "${hours}h ${mins}m",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Top Visited Domains
                Text(
                    text = stringResource(R.string.parental_insights_top_domains),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                if (insights.topVisited.isEmpty()) {
                    Text(stringResource(R.string.parental_insights_top_empty), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        insights.topVisited.forEach { domain ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(10.dp))
                                        Text(domain.url, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    }
                                    Text("${domain.timeSpentMinutes} min", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Blocked History Attempts
                Text(
                    text = stringResource(R.string.parental_insights_blocked),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                if (insights.blockedHistory.isEmpty()) {
                    Text(stringResource(R.string.parental_insights_blocked_empty), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        insights.blockedHistory.forEach { entry ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Block, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(10.dp))
                                        Column {
                                            Text(entry.url, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                            if (entry.timestamp.isNotBlank()) {
                                                Text(entry.timestamp, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                    if (entry.isUnblocked) {
                                        Text(
                                            text = stringResource(R.string.parental_status_unblocked),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        OutlinedButton(
                                            onClick = { onUnblockUrl(entry.url) },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(stringResource(R.string.parental_action_unblock), fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
