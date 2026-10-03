package es.routerapp.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.routerapp.protocol.i18n.tr
import es.routerapp.protocol.pages.Catalog
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
                    if (loggedIn) AppNavigation(vm, state.username)
                    else LoginScreen(state, vm::setAddress, vm::setUsername, vm::login)
                }
            }
        }
    }
}

@Composable
private fun LoginScreen(state: UiState, onAddress: (String) -> Unit, onUsername: (String) -> Unit, onLogin: (String) -> Unit) {
    var password by remember { mutableStateOf("") }
    var reveal by remember { mutableStateOf(false) }
    val c = MaterialTheme.colorScheme
    Scaffold { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ShapeBadge(Icons.Filled.Router, c.primaryContainer, c.onPrimaryContainer, 128.dp, 0)
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
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                visualTransformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
            state.error?.let {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Info, null, tint = c.error)
                    val errorRes = when (it) {
                        LoginError.WrongPassword -> R.string.login_wrong_password
                        LoginError.CleartextRestricted -> R.string.login_cleartext_restricted
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
private fun AppNavigation(vm: RouterViewModel, username: String) {
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
            page == null -> HomeScreen(username, onOpen = { pageId = it.id }, onLogout = vm::logout)
            page.id == "dashboard" -> DashboardScreen(engine) { pageId = null }
            page.id == "devices" -> DevicesScreen(engine) { pageId = null }
            else -> GenericPageScreen(page, engine) { pageId = null }
        }
    }
}

@Composable
private fun HomeScreen(username: String, onOpen: (Page) -> Unit, onLogout: () -> Unit) {
    val groups = Catalog.visiblePages(username).groupBy { it.group }
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
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            groups.forEach { (group, pages) ->
                item(key = "h_${group.name}", span = { GridItemSpan(2) }) {
                    Text(
                        tr(group.title),
                        style = MaterialTheme.typography.titleMedium,
                        color = c.primary,
                        modifier = Modifier.padding(start = 8.dp, top = 12.dp),
                    )
                }
                items(pages, key = { it.id }) { p -> PageTile(p, groupPalette(group)) { onOpen(p) } }
            }
        }
    }
}

@Composable
private fun PageTile(page: Page, palette: Palette, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 124.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = palette.container, contentColor = palette.content),
    ) {
        Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ShapeBadge(pageIcon(page.id), palette.content.copy(alpha = 0.14f), palette.content, 52.dp, page.id.hashCode())
            Text(tr(page.title), style = MaterialTheme.typography.titleSmall, maxLines = 2)
        }
    }
}