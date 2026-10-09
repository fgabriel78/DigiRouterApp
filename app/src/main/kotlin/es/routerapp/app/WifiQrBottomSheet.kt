package es.routerapp.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WifiQrBottomSheet(
    network: ConsolidatedWifiNetwork,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden),
) {
    WifiQrBottomSheet(
        ssid = network.ssid,
        psk = network.psk,
        securityMode = network.securityMode,
        hidden = network.hidden,
        bands = network.bands,
        category = network.category,
        additionalIndex = network.additionalIndex,
        onDismiss = onDismiss,
        sheetState = sheetState,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WifiQrBottomSheet(
    ssid: String,
    psk: String,
    securityMode: String,
    hidden: Boolean = false,
    bands: List<String> = emptyList(),
    category: WifiNetworkCategory? = null,
    additionalIndex: Int? = null,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden),
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme

    val authType = remember(securityMode) { WifiQr.mapSecurityModeToQrAuth(securityMode) }
    val isOpen = authType == "nopass"

    val qrContent = remember(ssid, psk, securityMode, hidden) {
        WifiQr.formatWifiQrString(
            ssid = ssid,
            psk = psk,
            securityMode = securityMode,
            hidden = hidden,
        )
    }

    val qrBitmap = remember(qrContent) {
        WifiQr.generateQrImageBitmap(qrContent, sizePx = 600, margin = 1)
    }

    var passwordRevealed by remember { mutableStateOf(false) }

    fun copyPassword() {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        cm?.setPrimaryClip(ClipData.newPlainText("Wi-Fi Password", psk))
        Toast.makeText(context, context.getString(R.string.wifi_qr_password_copied), Toast.LENGTH_SHORT).show()
    }

    fun launchShareSheet() {
        val shareText = if (isOpen) {
            context.getString(R.string.wifi_qr_share_text_open, ssid)
        } else {
            context.getString(R.string.wifi_qr_share_text, ssid, psk)
        }
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.wifi_qr_share_chooser) + ": $ssid")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(sendIntent, context.getString(R.string.wifi_qr_share_chooser)))
    }

    val categoryLabel = when (category) {
        WifiNetworkCategory.PRIMARY -> stringResource(R.string.wifi_type_primary)
        WifiNetworkCategory.GUEST -> stringResource(R.string.wifi_type_guest)
        WifiNetworkCategory.ADDITIONAL -> stringResource(R.string.wifi_type_additional, additionalIndex ?: 1)
        WifiNetworkCategory.MLO -> stringResource(R.string.wifi_type_mlo)
        null -> null
    }

    val categoryIcon = when (category) {
        WifiNetworkCategory.PRIMARY -> Icons.Filled.Wifi
        WifiNetworkCategory.GUEST -> Icons.Filled.Group
        WifiNetworkCategory.ADDITIONAL -> Icons.Filled.Layers
        WifiNetworkCategory.MLO -> Icons.Filled.Wifi
        null -> Icons.Filled.Wifi
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.wifi_qr_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.primary,
                    )
                    Text(
                        text = ssid.ifEmpty { stringResource(R.string.wifi_type_primary) },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Category & Band Chips
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                categoryLabel?.let { label ->
                    SuggestionChip(
                        onClick = {},
                        label = { Text(label) },
                        icon = { Icon(categoryIcon, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    )
                }
                bands.forEach { band ->
                    SuggestionChip(
                        onClick = {},
                        label = { Text(bandName(band)) },
                    )
                }
                SuggestionChip(
                    onClick = {},
                    label = { Text(securityMode) },
                    icon = {
                        Icon(
                            if (isOpen) Icons.Filled.LockOpen else Icons.Filled.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                )
            }

            // QR Code Container (High-contrast pure white surface for optical scanner reliability)
            Surface(
                modifier = Modifier
                    .size(260.dp)
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 3.dp,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        bitmap = qrBitmap,
                        contentDescription = stringResource(R.string.wifi_qr_content_description),
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            // Passphrase Information Card
            if (isOpen) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(Icons.Filled.LockOpen, contentDescription = null, tint = colors.primary)
                        Text(
                            text = stringResource(R.string.wifi_qr_no_password),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = colors.primary)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.wifi_qr_password_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant,
                            )
                            Text(
                                text = if (passwordRevealed) psk else "•".repeat(psk.length.coerceAtLeast(8)),
                                style = MaterialTheme.typography.bodyLarge,
                                fontFamily = if (passwordRevealed) FontFamily.Monospace else FontFamily.Default,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        IconButton(onClick = { passwordRevealed = !passwordRevealed }) {
                            Icon(
                                if (passwordRevealed) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = stringResource(
                                    if (passwordRevealed) R.string.wifi_qr_hide_password else R.string.wifi_qr_show_password
                                ),
                            )
                        }
                        IconButton(onClick = ::copyPassword) {
                            Icon(
                                Icons.Filled.ContentCopy,
                                contentDescription = stringResource(R.string.wifi_qr_copy_password),
                            )
                        }
                    }
                }
            }

            // Bottom Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!isOpen) {
                    OutlinedButton(
                        onClick = ::copyPassword,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.wifi_qr_copy_password))
                    }
                }
                Button(
                    onClick = ::launchShareSheet,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.wifi_qr_share))
                }
            }
        }
    }
}
