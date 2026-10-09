package es.routerapp.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DevicesOther
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tablet
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.routerapp.protocol.pages.ConnectedDevice

fun DeviceCategory.icon(): ImageVector = when (this) {
    DeviceCategory.PHONE -> Icons.Filled.Smartphone
    DeviceCategory.TABLET -> Icons.Filled.Tablet
    DeviceCategory.COMPUTER -> Icons.Filled.Computer
    DeviceCategory.TV -> Icons.Filled.Tv
    DeviceCategory.CONSOLE -> Icons.Filled.SportsEsports
    DeviceCategory.IOT -> Icons.Filled.Lightbulb
    DeviceCategory.PRINTER -> Icons.Filled.Print
    DeviceCategory.ROUTER -> Icons.Filled.Router
    DeviceCategory.OTHER -> Icons.Filled.DevicesOther
}

fun DeviceCategory.labelRes(): Int = when (this) {
    DeviceCategory.PHONE -> R.string.category_phone
    DeviceCategory.TABLET -> R.string.category_tablet
    DeviceCategory.COMPUTER -> R.string.category_computer
    DeviceCategory.TV -> R.string.category_tv
    DeviceCategory.CONSOLE -> R.string.category_console
    DeviceCategory.IOT -> R.string.category_iot
    DeviceCategory.PRINTER -> R.string.category_printer
    DeviceCategory.ROUTER -> R.string.category_router
    DeviceCategory.OTHER -> R.string.category_other
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DeviceDetailSheet(
    device: ConnectedDevice,
    initialMetadata: DeviceCustomMetadata?,
    onDismiss: () -> Unit,
    onSave: (DeviceCustomMetadata) -> Unit,
    onReset: () -> Unit,
    sheetState: SheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden),
) {
    val resolved = remember(device.mac, device.name, device.ip, initialMetadata) {
        DeviceResolver.resolve(
            mac = device.mac,
            hostname = device.name,
            ip = device.ip,
            customMeta = initialMetadata,
        )
    }

    var alias by remember(device.mac, initialMetadata) {
        mutableStateOf(initialMetadata?.alias ?: "")
    }
    var selectedCategory by remember(device.mac, initialMetadata, resolved.category) {
        mutableStateOf(initialMetadata?.category ?: resolved.category)
    }

    val colors = MaterialTheme.colorScheme

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
        ) {
            // Header title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.device_details_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            // Alias field
            OutlinedTextField(
                value = alias,
                onValueChange = { alias = it },
                label = { Text(stringResource(R.string.device_alias_label)) },
                placeholder = {
                    Text(
                        device.name.ifEmpty { stringResource(R.string.device_alias_placeholder) },
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                trailingIcon = {
                    if (alias.isNotEmpty()) {
                        IconButton(onClick = { alias = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.cancel))
                        }
                    }
                },
            )

            // Category picker
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.device_category_label),
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.onSurfaceVariant,
                    )
                    if (initialMetadata?.category == null && resolved.isCategoryInferred) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = colors.secondaryContainer,
                        ) {
                            Text(
                                text = stringResource(R.string.device_category_suggested),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (cat in DeviceCategory.entries) {
                        val isSelected = selectedCategory == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedCategory = if (isSelected) null else cat
                            },
                            label = { Text(stringResource(cat.labelRes())) },
                            leadingIcon = {
                                Icon(
                                    cat.icon(),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                        )
                    }
                }
            }

            // Hardware & Connection metrics
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.device_specs_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.onSurfaceVariant,
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Original Hostname
                        MetricRow(
                            label = stringResource(R.string.device_hostname),
                            value = device.name.ifEmpty { "-" },
                        )

                        // Vendor or Inferred Vendor
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.device_vendor),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.onSurfaceVariant,
                            )
                            if (resolved.vendor != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        text = resolved.vendor,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    if (resolved.vendorSource == VendorSource.HOSTNAME_HEURISTIC) {
                                        Text(
                                            text = stringResource(R.string.device_vendor_inferred),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.primary,
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = stringResource(R.string.device_vendor_unknown),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }

                        // IP Address
                        MetricRow(
                            label = stringResource(R.string.device_ip),
                            value = device.ip.ifEmpty { "-" },
                        )

                        // MAC Address
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.device_mac),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.onSurfaceVariant,
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = device.mac,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                )
                                if (resolved.isPrivateMac) {
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text(stringResource(R.string.device_private_mac)) },
                                        icon = {
                                            Icon(
                                                Icons.Filled.Shield,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                            )
                                        },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = colors.tertiaryContainer,
                                            labelColor = colors.onTertiaryContainer,
                                            iconContentColor = colors.onTertiaryContainer,
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                    )
                                }
                            }
                        }

                        // Connection Type
                        MetricRow(
                            label = stringResource(R.string.device_connection),
                            value = if (device.wifi) stringResource(R.string.devices_wifi) else stringResource(R.string.devices_cable),
                        )

                        // Signal Level (if Wi-Fi)
                        if (device.wifi && device.level != null) {
                            MetricRow(
                                label = stringResource(R.string.device_signal),
                                value = "${device.level}/5",
                            )
                        }

                        // Link Speed (if available)
                        if (device.rate != null) {
                            MetricRow(
                                label = stringResource(R.string.device_link_rate),
                                value = "${device.rate} Mbit/s",
                            )
                        }

                        // EasyMesh Node (if present)
                        val nodeName = device.nodeName
                        if (!nodeName.isNullOrEmpty()) {
                            MetricRow(
                                label = stringResource(R.string.device_mesh_node),
                                value = nodeName,
                            )
                        }
                    }
                }

                // Educational banner for Private MAC
                if (resolved.isPrivateMac) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colors.tertiaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                Icons.Filled.Shield,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = colors.onTertiaryContainer,
                            )
                            Text(
                                text = stringResource(R.string.device_private_mac_explanation),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onTertiaryContainer,
                            )
                        }
                    }
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val hasCustomData = initialMetadata != null &&
                    (!initialMetadata.alias.isNullOrBlank() || initialMetadata.category != null || !initialMetadata.notes.isNullOrBlank())

                OutlinedButton(
                    onClick = {
                        onReset()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    enabled = hasCustomData || alias.isNotEmpty() || selectedCategory != null,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(stringResource(R.string.device_reset_custom))
                }

                Button(
                    onClick = {
                        onSave(
                            DeviceCustomMetadata(
                                alias = alias.trim().ifEmpty { null },
                                category = selectedCategory,
                            )
                        )
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(stringResource(R.string.save))
                }
            }
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}
