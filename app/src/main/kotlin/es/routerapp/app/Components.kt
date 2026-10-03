package es.routerapp.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Router
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import es.routerapp.protocol.pages.Group

/** Icon for each catalog page (UI concern, so it lives here and not in the protocol module). */
fun pageIcon(id: String): ImageVector = when (id) {
    "dashboard" -> Icons.Filled.Dashboard
    "devices" -> Icons.Filled.Devices
    "wifi" -> Icons.Filled.Wifi
    "wifi-radio" -> Icons.Filled.CellTower
    "wifi-adv" -> Icons.Filled.Tune
    "wps" -> Icons.Filled.Link
    "guest" -> Icons.Filled.People
    "mssid" -> Icons.Filled.Layers
    "lan" -> Icons.Filled.Lan
    "dhcp-static" -> Icons.Filled.PushPin
    "ddns" -> Icons.Filled.Cloud
    "virtual-servers" -> Icons.Filled.Dns
    "port-trigger" -> Icons.Filled.SwapHoriz
    "dmz" -> Icons.Filled.Public
    "upnp" -> Icons.Filled.Settings
    "alg" -> Icons.Filled.Tune
    "ddos" -> Icons.Filled.Shield
    "arp" -> Icons.Filled.Lock
    "storage" -> Icons.Filled.Storage
    "time" -> Icons.Filled.Schedule
    "led" -> Icons.Filled.Lightbulb
    "reboot" -> Icons.Filled.RestartAlt
    "stats" -> Icons.Filled.BarChart
    else -> Icons.Filled.Router
}

class Palette(val container: Color, val content: Color)

@Composable
fun groupPalette(group: Group): Palette {
    val c = MaterialTheme.colorScheme
    return when (group) {
        Group.STATUS -> Palette(c.primaryContainer, c.onPrimaryContainer)
        Group.WIFI -> Palette(c.tertiaryContainer, c.onTertiaryContainer)
        Group.NETWORK -> Palette(c.secondaryContainer, c.onSecondaryContainer)
        Group.NAT -> Palette(c.primaryContainer, c.onPrimaryContainer)
        Group.SECURITY -> Palette(c.tertiaryContainer, c.onTertiaryContainer)
        Group.STORAGE -> Palette(c.secondaryContainer, c.onSecondaryContainer)
        Group.SYSTEM -> Palette(c.surfaceContainerHighest, c.onSurface)
    }
}

/** Expressive "badge": an icon on a scalloped/cookie-shaped tonal background. */
@Composable
fun ShapeBadge(
    icon: ImageVector,
    container: Color,
    content: Color,
    size: Dp = 48.dp,
    shapeIndex: Int = 0,
    modifier: Modifier = Modifier,
) {
    val polygons = listOf(
        MaterialShapes.Cookie9Sided, MaterialShapes.Clover4Leaf, MaterialShapes.Sunny,
        MaterialShapes.Cookie6Sided, MaterialShapes.Flower, MaterialShapes.SoftBurst,
    )
    val shape = polygons[shapeIndex.mod(polygons.size)].toShape()
    Box(modifier.size(size).clip(shape).background(container), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(size * 0.5f))
    }
}

/** Large number/value tile with an icon: replaces label/value text rows. Becomes a selectable filter chip when [onClick] is set. */
@Composable
fun StatTile(
    icon: ImageVector,
    value: String,
    label: String,
    palette: Palette,
    modifier: Modifier = Modifier,
    shapeIndex: Int = 0,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(28.dp)
    val colors = CardDefaults.cardColors(containerColor = palette.container, contentColor = palette.content)
    val border = if (selected) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null
    val body: @Composable ColumnScope.() -> Unit = {
        Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ShapeBadge(icon, palette.content.copy(alpha = 0.14f), palette.content, 44.dp, shapeIndex)
            Text(value, style = MaterialTheme.typography.headlineSmall, maxLines = 1)
            Text(label, style = MaterialTheme.typography.labelMedium, color = palette.content.copy(alpha = 0.75f), maxLines = 1)
        }
    }
    if (onClick != null) Card(onClick = onClick, modifier = modifier, shape = shape, colors = colors, border = border, content = body)
    else Card(modifier = modifier, shape = shape, colors = colors, border = border, content = body)
}

@Composable
fun StatTile(icon: ImageVector, value: String, label: String, palette: Palette, modifier: Modifier = Modifier, shapeIndex: Int = 0) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = palette.container, contentColor = palette.content),
    ) {
        Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ShapeBadge(icon, palette.content.copy(alpha = 0.14f), palette.content, 44.dp, shapeIndex)
            Text(value, style = MaterialTheme.typography.headlineSmall, maxLines = 1)
            Text(label, style = MaterialTheme.typography.labelMedium, color = palette.content.copy(alpha = 0.75f), maxLines = 1)
        }
    }
}

@Composable
fun LoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { ContainedLoadingIndicator(Modifier.size(64.dp)) }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ShapeBadge(Icons.Filled.CloudOff, MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer, 96.dp, 3)
        Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error)
        Button(onClick = onRetry, shapes = ButtonDefaults.shapes()) {
            Icon(Icons.Filled.Refresh, null, Modifier.size(ButtonDefaults.IconSize))
            Row(Modifier.padding(start = ButtonDefaults.IconSpacing)) { Text(stringResource(R.string.retry)) }
        }
    }
}

@Composable
fun EmptyHint(text: String = stringResource(R.string.empty_items)) {
    Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(Icons.Filled.Inbox, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
