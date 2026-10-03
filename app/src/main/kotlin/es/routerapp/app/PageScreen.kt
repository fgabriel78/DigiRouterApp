package es.routerapp.app

import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import es.routerapp.protocol.RouterException
import es.routerapp.protocol.i18n.tr
import es.routerapp.protocol.pages.Field
import es.routerapp.protocol.pages.FieldType
import es.routerapp.protocol.pages.Instance
import es.routerapp.protocol.pages.Option
import es.routerapp.protocol.pages.Page
import es.routerapp.protocol.pages.PageAction
import es.routerapp.protocol.pages.PageEngine
import es.routerapp.protocol.pages.Section
import es.routerapp.protocol.pages.validate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Maps router/network failures to a message for the user. */
fun describeError(ctx: Context, e: Throwable): String = when (e) {
    is RouterException -> ctx.getString(R.string.err_router, e.code)
    is java.io.IOException -> ctx.getString(R.string.err_network, e.message ?: ctx.getString(R.string.err_no_detail))
    else -> e.message ?: e.toString()
}

@Composable
fun errorText(e: Throwable): String = describeError(LocalContext.current, e)

private suspend fun <T> io(block: () -> T): Result<T> = withContext(Dispatchers.IO) { runCatching(block) }

@Composable
fun GenericPageScreen(page: Page, engine: PageEngine, onBack: () -> Unit) {
    var reloadKey by remember { mutableIntStateOf(0) }
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val loaded by produceState<Result<Map<String, List<Instance>>>?>(null, reloadKey) {
        value = null
        value = io { page.sections.associate { it.id to engine.load(it) } }
    }
    var pendingAction by remember { mutableStateOf<PageAction?>(null) }

    ScreenScaffold(tr(page.title), onBack, snack, groupPalette(page.group), pageIcon(page.id)) {
        val res = loaded
        when {
            res == null -> LoadingState()
            res.isFailure -> ErrorState(errorText(res.exceptionOrNull()!!)) { reloadKey++ }
            else -> {
                val data = res.getOrThrow()
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    items(page.sections, key = { it.id }) { section ->
                        SectionView(section, data[section.id].orEmpty(), engine, scope, snack) { reloadKey++ }
                    }
                    items(page.actions, key = { it.title }) { a ->
                        val c = MaterialTheme.colorScheme
                        Button(
                            onClick = { pendingAction = a },
                            shapes = ButtonDefaults.shapes(),
                            colors = if (a.danger) ButtonDefaults.buttonColors(containerColor = c.errorContainer, contentColor = c.onErrorContainer)
                            else ButtonDefaults.buttonColors(),
                            contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight),
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        ) {
                            Icon(pageIcon(page.id), null, Modifier.size(ButtonDefaults.IconSize))
                            Text(tr(a.title), Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        }
    }

    pendingAction?.let { a ->
        ConfirmDialog(tr(a.title), tr(a.confirm), danger = a.danger, onDismiss = { pendingAction = null }) {
            pendingAction = null
            scope.launch {
                val r = io { engine.run(a) }
                snack.showSnackbar(if (r.isSuccess) ctx.getString(R.string.order_sent) else describeError(ctx, r.exceptionOrNull()!!))
            }
        }
    }
}

/** Expressive screen frame: collapsing large top app bar, tinted by the page's group colour. */
@Composable
fun ScreenScaffold(
    title: String,
    onBack: () -> Unit,
    snack: SnackbarHostState,
    palette: Palette? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    actions: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    FilledIconButton(
                        onClick = onBack,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(),
                    ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) }
                },
                actions = {
                    actions()
                    if (icon != null && palette != null) {
                        ShapeBadge(icon, palette.container, palette.content, 44.dp, 0, Modifier.padding(horizontal = 12.dp))
                    }
                },
                scrollBehavior = scroll,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        snackbarHost = { SnackbarHost(snack) },
    ) { padding -> Column(Modifier.padding(padding).fillMaxSize()) { content() } }
}

@Composable
fun ConfirmDialog(title: String, text: String, danger: Boolean = false, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Filled.Warning, null, tint = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
        },
        title = { Text(title) },
        text = { Text(text) },
        shape = RoundedCornerShape(32.dp),
        confirmButton = {
            Button(
                onClick = onConfirm,
                shapes = ButtonDefaults.shapes(),
                colors = if (danger) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.buttonColors(),
            ) { Text(stringResource(R.string.continue_action)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

// ---------------------------------------------------------------- sections

@Composable
private fun SectionView(
    section: Section,
    instances: List<Instance>,
    engine: PageEngine,
    scope: CoroutineScope,
    snack: SnackbarHostState,
    reload: () -> Unit,
) {
    var showAdd by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(tr(section.title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp))
        section.note?.let { Text(tr(it), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 8.dp)) }
        val shown = if (section.applyToAll) instances.take(1) else instances
        if (shown.isEmpty()) EmptyHint()
        shown.forEach { inst -> InstanceCard(section, inst, instances, engine, scope, snack, reload) }
        if (section.add != null) {
            FilledTonalButton(onClick = { showAdd = true }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, null, Modifier.size(ButtonDefaults.IconSize))
                Text(stringResource(R.string.add), Modifier.padding(start = 8.dp))
            }
        }
    }
    if (showAdd) {
        AddDialog(section, onDismiss = { showAdd = false }) { values ->
            showAdd = false
            scope.launch {
                val r = io { engine.add(section, values) }
                snack.showSnackbar(if (r.isSuccess) ctx.getString(R.string.added) else describeError(ctx, r.exceptionOrNull()!!))
                reload()
            }
        }
    }
}

@Composable
private fun InstanceCard(
    section: Section,
    inst: Instance,
    all: List<Instance>,
    engine: PageEngine,
    scope: CoroutineScope,
    snack: SnackbarHostState,
    reload: () -> Unit,
) {
    val edits = remember(inst) { mutableStateMapOf<String, String>().apply { putAll(inst.values) } }
    var busy by remember(inst) { mutableStateOf(false) }
    var confirmSave by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val ctx = LocalContext.current

    val editable = if (section.readOnly) emptyList() else section.fields.filter { it.type != FieldType.Info }
    val visible = section.fields.filter { it.visibleIf?.invoke(edits) ?: true }
    val errors = visible.filter { it in editable && edits[it.key] != inst.values[it.key] }
        .mapNotNull { f -> f.validate(edits[f.key].orEmpty(), edits)?.let { f.key to it } }.toMap()
    val dirty = editable.any { edits[it.key] != inst.values[it.key] }

    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (section.multi && !section.applyToAll) {
                Text(section.itemTitle(inst.values), style = MaterialTheme.typography.titleSmall)
            }
            if (section.fields.isEmpty() && section.showAll) {
                inst.values.filterKeys { it != "stack" }.toSortedMap().forEach { (k, v) -> InfoRow(k, v) }
            }
            visible.forEach { f ->
                FieldEditor(f, edits[f.key].orEmpty(), edits, !busy && !section.readOnly, errors[f.key]) { edits[f.key] = it }
            }
            if (!section.readOnly && (editable.isNotEmpty() || section.deletable)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (editable.isNotEmpty()) {
                        Button(
                            enabled = dirty && errors.isEmpty() && !busy,
                            shapes = ButtonDefaults.shapes(),
                            onClick = {
                                if (section.saveWarning != null) confirmSave = true
                                else doSave(ctx, section, inst, all, edits, engine, scope, snack, reload) { busy = it }
                            },
                        ) {
                            Icon(Icons.Filled.Check, null, Modifier.size(ButtonDefaults.IconSize))
                            Text(stringResource(R.string.save), Modifier.padding(start = 8.dp))
                        }
                    }
                    if (section.deletable) {
                        FilledIconButton(
                            enabled = !busy,
                            onClick = { confirmDelete = true },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            ),
                        ) { Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete)) }
                    }
                    if (busy) LoadingIndicator(Modifier.size(40.dp))
                }
            }
        }
    }

    if (confirmSave) {
        ConfirmDialog(stringResource(R.string.confirm_changes), tr(section.saveWarning.orEmpty()), onDismiss = { confirmSave = false }) {
            confirmSave = false
            doSave(ctx, section, inst, all, edits, engine, scope, snack, reload) { busy = it }
        }
    }
    if (confirmDelete) {
        ConfirmDialog(stringResource(R.string.delete), stringResource(R.string.confirm_delete, section.itemTitle(inst.values)), danger = true, onDismiss = { confirmDelete = false }) {
            confirmDelete = false
            scope.launch {
                busy = true
                val r = io { engine.delete(section, inst) }
                snack.showSnackbar(if (r.isSuccess) ctx.getString(R.string.deleted) else describeError(ctx, r.exceptionOrNull()!!))
                busy = false
                reload()
            }
        }
    }
}

private fun doSave(
    ctx: Context,
    section: Section,
    inst: Instance,
    all: List<Instance>,
    edits: Map<String, String>,
    engine: PageEngine,
    scope: CoroutineScope,
    snack: SnackbarHostState,
    reload: () -> Unit,
    setBusy: (Boolean) -> Unit,
) {
    val edited = edits.toMap()
    scope.launch {
        setBusy(true)
        val r = io { engine.save(section, inst, edited, all) }
        snack.showSnackbar(
            r.fold(
                onSuccess = { if (it.isEmpty()) ctx.getString(R.string.no_changes) else ctx.getString(R.string.saved) },
                onFailure = { describeError(ctx, it) },
            )
        )
        setBusy(false)
        reload()
    }
}

@Composable
private fun AddDialog(section: Section, onDismiss: () -> Unit, onConfirm: (Map<String, String>) -> Unit) {
    val spec = section.add ?: return
    val values = remember { mutableStateMapOf<String, String>().apply { spec.defaults.forEach { (k, v) -> if (!v.startsWith("@")) put(k, v) } } }
    val errors = spec.fields.mapNotNull { f -> f.validate(values[f.key].orEmpty(), values)?.let { f.key to it } }.toMap()
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Add, null) },
        title = { Text(tr(spec.title)) },
        shape = RoundedCornerShape(32.dp),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                spec.fields.forEach { f ->
                    FieldEditor(f, values[f.key].orEmpty(), values, true, if (values[f.key].isNullOrEmpty()) null else errors[f.key]) { values[f.key] = it }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = errors.isEmpty(),
                shapes = ButtonDefaults.shapes(),
                onClick = { onConfirm(spec.fields.associate { it.key to values[it.key].orEmpty().trim() }) },
            ) { Text(stringResource(R.string.add)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

// ---------------------------------------------------------------- field widgets

@Composable
fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value.ifEmpty { "—" }, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun FieldEditor(field: Field, value: String, all: Map<String, String>, enabled: Boolean, error: String?, onChange: (String) -> Unit) {
    when (val t = field.type) {
        FieldType.Info -> InfoRow(tr(field.label), value)
        FieldType.Switch -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(tr(field.label), style = MaterialTheme.typography.bodyLarge)
                field.help?.let { Text(tr(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Switch(
                checked = value == "1",
                enabled = enabled,
                onCheckedChange = { onChange(if (it) "1" else "0") },
                thumbContent = if (value == "1") {
                    { Icon(Icons.Filled.Check, null, Modifier.size(SwitchIconSize)) }
                } else null,
            )
        }
        is FieldType.Choice -> ChoiceField(field, t.options(all), value, enabled, error, onChange)
        FieldType.TimeOfDay -> {
            val ctx = LocalContext.current
            val minutes = value.toIntOrNull() ?: 0
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(tr(field.label), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                FilledTonalButton(enabled = enabled, shapes = ButtonDefaults.shapes(), onClick = {
                    TimePickerDialog(ctx, { _, h, m -> onChange((h * 60 + m).toString()) }, minutes / 60, minutes % 60, true).show()
                }) {
                    Icon(Icons.Filled.Schedule, null, Modifier.size(ButtonDefaults.IconSize))
                    Text("%02d:%02d".format(minutes / 60, minutes % 60), Modifier.padding(start = 8.dp))
                }
            }
        }
        else -> {
            var reveal by remember { mutableStateOf(false) }
            val secret = t is FieldType.Secret
            OutlinedTextField(
                value = value,
                onValueChange = onChange,
                enabled = enabled,
                label = { Text(tr(field.label)) },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                isError = error != null,
                supportingText = (error ?: field.help?.let { tr(it) })?.let { { Text(it) } },
                visualTransformation = if (secret && !reveal) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(
                    keyboardType = when (t) {
                        is FieldType.Number -> KeyboardType.Number
                        FieldType.Ipv4 -> KeyboardType.Decimal
                        is FieldType.Secret -> KeyboardType.Password
                        else -> KeyboardType.Text
                    }
                ),
                trailingIcon = if (secret) {
                    {
                        IconButton(onClick = { reveal = !reveal }) {
                            Icon(if (reveal) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, contentDescription = null)
                        }
                    }
                } else if (t is FieldType.Number && t.unit != null) {
                    { Text(t.unit!!, Modifier.padding(end = 12.dp)) }
                } else null,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private val SwitchIconSize = 16.dp

@Composable
private fun ChoiceField(field: Field, options: List<Option>, value: String, enabled: Boolean, error: String?, onChange: (String) -> Unit) {
    val all = if (options.any { it.value == value }) options else listOf(Option(value, value.ifEmpty { "—" })) + options
    // Few short options read better as a segmented selector than as a hidden dropdown.
    if (all.size in 2..3 && all.all { tr(it.label).length <= 12 }) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(tr(field.label), style = MaterialTheme.typography.bodyLarge)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                all.forEachIndexed { i, o ->
                    SegmentedButton(
                        selected = o.value == value,
                        onClick = { onChange(o.value) },
                        enabled = enabled,
                        shape = SegmentedButtonDefaults.itemShape(i, all.size),
                    ) { Text(tr(o.label), maxLines = 1) }
                }
            }
        }
        return
    }
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { if (enabled) expanded = it }) {
        OutlinedTextField(
            value = all.firstOrNull { it.value == value }?.label?.let { tr(it) }.orEmpty(),
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(tr(field.label)) },
            shape = RoundedCornerShape(20.dp),
            isError = error != null,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, shape = RoundedCornerShape(20.dp)) {
            all.forEach { o -> DropdownMenuItem(text = { Text(tr(o.label)) }, onClick = { onChange(o.value); expanded = false }) }
        }
    }
}

