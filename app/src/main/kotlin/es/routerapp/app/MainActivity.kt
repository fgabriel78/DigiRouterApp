package es.routerapp.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.routerapp.protocol.i18n.tr
import es.routerapp.protocol.pages.Catalog
import es.routerapp.protocol.pages.Group
import es.routerapp.protocol.pages.Page

class MainActivity : ComponentActivity() {
    private val vm: RouterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RouterTheme {
                val state by vm.state.collectAsStateWithLifecycle()
                val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
                val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
                AnimatedContent(
                    targetState = state.loggedIn,
                    transitionSpec = { (fadeIn(effects) + scaleIn(spatial, 0.92f)) togetherWith fadeOut(effects) },
                    label = "session",
                ) { loggedIn ->
                    if (loggedIn) AppNavigation(vm, state.username, state.address)
                    else LoginScreen(state, vm::setAddress, vm::setUsername, vm::setRememberPassword, vm::login)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.evictIdleConnections()
    }
}

@Composable
private fun LoginScreen(
    state: UiState,
    onAddress: (String) -> Unit,
    onUsername: (String) -> Unit,
    onRememberPassword: (Boolean) -> Unit,
    onLogin: (String) -> Unit,
) {
    var password by remember(state.savedPassword) { mutableStateOf(state.savedPassword) }
    var reveal by remember { mutableStateOf(false) }
    val c = MaterialTheme.colorScheme
    val imeVisible = WindowInsets.isImeVisible
    val badgeSize by animateDpAsState(
        targetValue = if (imeVisible) 48.dp else 128.dp,
        label = "badgeSize",
    )
    Scaffold { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ShapeBadge(Icons.Filled.Router, c.primaryContainer, c.onPrimaryContainer, badgeSize, 0)
            Text(stringResource(R.string.login_title), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Spacer(8)
            OutlinedTextField(
                value = state.address,
                onValueChange = onAddress,
                leadingIcon = { Icon(Icons.Filled.Router, null) },
                label = { Text(stringResource(R.string.login_address)) },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.username,
                onValueChange = onUsername,
                leadingIcon = { Icon(Icons.Filled.Person, null) },
                label = { Text(stringResource(R.string.login_username)) },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                leadingIcon = { Icon(Icons.Filled.Lock, null) },
                trailingIcon = {
                    IconButton(onClick = { reveal = !reveal }) {
                        Icon(if (reveal) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null)
                    }
                },
                label = { Text(stringResource(R.string.login_password)) },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = if (reveal) FontFamily.Default else FontFamily.Monospace,
                    letterSpacing = if (reveal) TextUnit.Unspecified else 2.sp,
                ),
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                visualTransformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onRememberPassword(!state.rememberPassword) },
            ) {
                Checkbox(
                    checked = state.rememberPassword,
                    onCheckedChange = { onRememberPassword(it) },
                )
                Text(
                    text = stringResource(R.string.login_remember_password),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
            state.error?.let {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Info, null, tint = c.error)
                    val errorRes = when (it) {
                        LoginError.WrongPassword -> R.string.login_wrong_password
                        LoginError.CleartextRestricted -> R.string.login_cleartext_restricted
                        LoginError.SessionExpired -> R.string.login_session_expired
                        LoginError.Unreachable, LoginError.Other -> R.string.login_unreachable
                    }
                    Text(
                        stringResource(errorRes),
                        color = c.error, style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Button(
                onClick = { onLogin(password) },
                enabled = !state.loggingIn && password.isNotEmpty(),
                shapes = ButtonDefaults.shapes(),
                contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.loggingIn) LoadingIndicator(Modifier.size(28.dp))
                else Icon(Icons.AutoMirrored.Filled.Login, null, Modifier.size(ButtonDefaults.IconSize))
                Text(stringResource(R.string.login_button), Modifier.padding(start = 8.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Info, null, Modifier.size(18.dp), tint = c.onSurfaceVariant)
                Text(stringResource(R.string.login_busy_warning), style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Spacer(height: Int) = Box(Modifier.size(height.dp))

@Composable
private fun AppNavigation(vm: RouterViewModel, username: String, routerAddress: String = "192.168.1.1") {
    var pageId by rememberSaveable { mutableStateOf<String?>(null) }
    val engine = vm.engine
    BackHandler(enabled = pageId != null) { pageId = null }
    if (engine == null) return
    val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    AnimatedContent(
        targetState = pageId,
        transitionSpec = { (fadeIn(effects) + scaleIn(spatial, 0.94f)) togetherWith (fadeOut(effects) + scaleOut(spatial, 0.94f)) },
        label = "nav",
    ) { id ->
        val page = id?.let { Catalog.byId(it) }
        when {
            id == "security-health" || page?.id == "security-health" -> SecurityHealthScreen(
                viewModel = SecurityHealthViewModel(clientProvider = { vm.client }, recoveryRunner = vm),
                onBack = { pageId = null },
                onNavigateToPage = { pageId = it },
            )
            page == null -> HomeScreen(username, routerAddress, onOpen = { pageId = it.id }, onLogout = vm::logout)
            page.id == "dashboard" -> DashboardScreen(
                engine = engine,
                onBack = { pageId = null },
                recovery = vm,
                onNavigateToSecurity = { pageId = "security-health" },
            )
            page.id == "devices" -> DevicesScreen(engine, onBack = { pageId = null }, recovery = vm)
            page.id == "speedtest" -> SpeedTestScreen(onBack = { pageId = null })
            else -> GenericPageScreen(page, engine, onBack = { pageId = null }, recovery = vm)
        }
    }
}

@Composable
private fun HomeScreen(
    username: String,
    routerAddress: String = "192.168.1.1",
    onOpen: (Page) -> Unit,
    onLogout: () -> Unit,
) {
    val allPages = Catalog.visiblePages(username)
    val quickAccessIds = setOf("dashboard", "devices", "speedtest", "wifi")
    val quickAccessPages = quickAccessIds.mapNotNull { id -> allPages.find { it.id == id } }
    val categoryGroups = allPages
        .filter { it.id !in quickAccessIds }
        .groupBy { it.group }
        .filter { it.value.isNotEmpty() }

    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val c = MaterialTheme.colorScheme
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                subtitle = { Text(username) },
                actions = {
                    IconButton(onClick = onLogout) { Icon(Icons.AutoMirrored.Filled.Logout, stringResource(R.string.logout)) }
                },
                scrollBehavior = scroll,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = c.surface),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "hero_card") {
                HeroStatusCard(
                    address = routerAddress,
                    onClick = {
                        allPages.find { it.id == "dashboard" }?.let(onOpen)
                    },
                )
            }

            if (quickAccessPages.isNotEmpty()) {
                item(key = "quick_access") {
                    QuickAccessBentoGrid(
                        pages = quickAccessPages,
                        onOpen = onOpen,
                    )
                }
            }

            categoryGroups.forEach { (group, pages) ->
                item(key = "group_${group.name}") {
                    CategoryGroupContainer(
                        groupTitle = tr(group.title),
                        pages = pages,
                        onOpen = onOpen,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroStatusCard(
    modelName: String = stringResource(R.string.home_router_model),
    address: String = "192.168.1.1",
    isOnline: Boolean = true,
    onClick: () -> Unit,
) {
    val c = MaterialTheme.colorScheme
    val dark = isSystemInDarkTheme()
    val statusColor = if (isOnline) {
        if (dark) Color(0xFF81C784) else Color(0xFF2E7D32)
    } else c.error
    val statusContainer = if (isOnline) {
        if (dark) Color(0xFF1B5E20).copy(alpha = 0.35f) else Color(0xFFE8F5E9)
    } else c.errorContainer

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = c.surfaceContainer, contentColor = c.onSurface),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ShapeBadge(
                    icon = Icons.Filled.Router,
                    container = c.primaryContainer,
                    content = c.onPrimaryContainer,
                    size = 56.dp,
                    shapeIndex = 0,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = modelName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = statusContainer,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(statusColor, shape = CircleShape),
                                )
                                Text(
                                    text = stringResource(if (isOnline) R.string.home_hero_online else R.string.home_hero_offline),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = statusColor,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                        Text(
                            text = address,
                            style = MaterialTheme.typography.bodySmall,
                            color = c.onSurfaceVariant,
                        )
                    }
                }
            }

            HorizontalDivider(color = c.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.home_hero_diagnostics),
                    style = MaterialTheme.typography.labelMedium,
                    color = c.primary,
                    fontWeight = FontWeight.Medium,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = c.primary,
                )
            }
        }
    }
}

@Composable
private fun QuickAccessTile(
    page: Page,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val c = MaterialTheme.colorScheme
    val (badgeContainer, badgeContent) = when (page.id) {
        "dashboard" -> c.primaryContainer to c.onPrimaryContainer
        "devices" -> c.secondaryContainer to c.onSecondaryContainer
        "speedtest" -> c.tertiaryContainer to c.onTertiaryContainer
        "wifi" -> c.primaryContainer to c.onPrimaryContainer
        else -> c.surfaceContainerHighest to c.onSurface
    }

    Card(
        onClick = onClick,
        modifier = modifier.height(112.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = c.surfaceContainerLow,
            contentColor = c.onSurface,
        ),
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ShapeBadge(
                    icon = pageIcon(page.id),
                    container = badgeContainer,
                    content = badgeContent,
                    size = 42.dp,
                    shapeIndex = page.id.hashCode(),
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = c.onSurfaceVariant.copy(alpha = 0.5f),
                )
            }
            Text(
                text = tr(page.title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun QuickAccessBentoGrid(
    pages: List<Page>,
    onOpen: (Page) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.home_quick_access),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
        )
        pages.chunked(2).forEach { rowPages ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowPages.forEach { page ->
                    QuickAccessTile(
                        page = page,
                        modifier = Modifier.weight(1f),
                        onClick = { onOpen(page) },
                    )
                }
                if (rowPages.size == 1) {
                    Box(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CategoryItemRow(
    page: Page,
    onOpen: () -> Unit,
) {
    val c = MaterialTheme.colorScheme
    val (badgeContainer, badgeContent) = when (page.group) {
        Group.WIFI -> c.primaryContainer to c.onPrimaryContainer
        Group.NETWORK -> c.secondaryContainer to c.onSecondaryContainer
        Group.NAT -> c.primaryContainer to c.onPrimaryContainer
        Group.SECURITY -> c.errorContainer to c.onErrorContainer
        Group.STORAGE -> c.tertiaryContainer to c.onTertiaryContainer
        Group.SYSTEM -> c.surfaceContainerHighest to c.onSurface
        Group.STATUS -> c.primaryContainer to c.onPrimaryContainer
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ShapeBadge(
            icon = pageIcon(page.id),
            container = badgeContainer,
            content = badgeContent,
            size = 44.dp,
            shapeIndex = page.id.hashCode(),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = tr(page.title),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = c.onSurface,
            )
            val desc = page.description
            if (!desc.isNullOrEmpty()) {
                Text(
                    text = tr(desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = c.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = c.onSurfaceVariant.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun CategoryGroupContainer(
    groupTitle: String,
    pages: List<Page>,
    onOpen: (Page) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = MaterialTheme.colorScheme
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = groupTitle,
            style = MaterialTheme.typography.titleMedium,
            color = c.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp),
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = c.surfaceContainerLow,
                contentColor = c.onSurface,
            ),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                pages.forEachIndexed { index, page ->
                    CategoryItemRow(page = page, onOpen = { onOpen(page) })
                    if (index < pages.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = c.outlineVariant.copy(alpha = 0.35f),
                        )
                    }
                }
            }
        }
    }
}