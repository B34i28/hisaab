package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.Shop
import com.example.util.AppLanguage
import com.example.util.LocalAppLanguage
import com.example.util.LocalAppStrings
import com.example.util.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopTopAppBar(
    currentShop: Shop?,
    allShops: List<Shop>,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    isDarkTheme: Boolean = false,
    onSelectShop: (Long) -> Unit,
    onAddNewShopClick: () -> Unit,
    onRenameShopClick: (Shop) -> Unit,
    onDeleteShopClick: (Shop) -> Unit,
    onReportsClick: () -> Unit = {},
    onToggleTheme: () -> Unit = {},
    onToggleLanguage: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val strings = LocalAppStrings.current
    val currentLang = LocalAppLanguage.current
    var expanded by remember { mutableStateOf(false) }

    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        ),
        title = {
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    onClick = { expanded = true },
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.testTag("shop_selector_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Storefront,
                                    contentDescription = "Shop Icon",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = currentShop?.name ?: strings.selectShop,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Expand shops dropdown",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.width(280.dp)
                ) {
                    Text(
                        text = strings.yourShops,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    allShops.forEach { shop ->
                        val isSelected = shop.id == currentShop?.id
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = shop.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            },
                            onClick = {
                                expanded = false
                                onSelectShop(shop.id)
                            },
                            modifier = Modifier.testTag("shop_item_${shop.id}")
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = strings.addNewShop,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        text = {
                            Text(
                                text = strings.addNewShop,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        onClick = {
                            expanded = false
                            onAddNewShopClick()
                        },
                        modifier = Modifier.testTag("add_shop_menu_item")
                    )

                    if (currentShop != null) {
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = strings.renameCurrentShop
                                )
                            },
                            text = { Text(strings.renameCurrentShop) },
                            onClick = {
                                expanded = false
                                onRenameShopClick(currentShop)
                            }
                        )

                        if (allShops.size > 1) {
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = strings.deleteCurrentShop,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                text = {
                                    Text(
                                        strings.deleteCurrentShop,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    expanded = false
                                    onDeleteShopClick(currentShop)
                                }
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = strings.settingsAndBackup
                            )
                        },
                        text = { Text(strings.settingsAndBackup) },
                        onClick = {
                            expanded = false
                            onSettingsClick()
                        },
                        modifier = Modifier.testTag("menu_settings_item")
                    )
                }
            }
        },
        actions = {
            // Quick Language Toggle Chip (Urdu <-> English)
            Surface(
                onClick = onToggleLanguage,
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .testTag("top_bar_language_toggle")
            ) {
                Text(
                    text = if (currentLang == AppLanguage.URDU) "EN" else "اردو",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }

            // Quick Dark Mode Toggle
            IconButton(
                onClick = onToggleTheme,
                modifier = Modifier
                    .padding(end = 2.dp)
                    .testTag("top_bar_theme_toggle")
            ) {
                Icon(
                    imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = if (isDarkTheme) "Switch to Light Mode" else "Switch to Dark Mode",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Reports button
            IconButton(
                onClick = onReportsClick,
                modifier = Modifier
                    .padding(end = 2.dp)
                    .testTag("top_bar_reports_button")
            ) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = strings.navReports,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Settings button
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .testTag("top_bar_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = strings.settingsAndBackup,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}
