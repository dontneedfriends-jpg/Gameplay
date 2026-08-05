package app.gamenative.ui.screen.library.components

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AirplaneTicket
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.gamenative.PluviaApp
import app.gamenative.PrefManager
import app.gamenative.R
import app.gamenative.data.SteamFriend
import app.gamenative.events.SteamEvent
import app.gamenative.service.SteamService
import app.gamenative.ui.component.focusRing
import app.gamenative.ui.screen.PluviaScreen
import app.gamenative.ui.theme.PluviaTheme
import app.gamenative.ui.util.SteamIconImage
import app.gamenative.ui.util.shouldShowGamepadUI
import app.gamenative.utils.getAvatarURL
import `in`.dragonbra.javasteam.enums.EPersonaState
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

/**
 * A single menu item in the System Menu
 */
@Composable
private fun SystemMenuItem(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    isDestructive: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val backgroundColor = when {
        isFocused -> MaterialTheme.colorScheme.primaryContainer
        else -> Color.Transparent
    }

    val contentColor = when {
        isDestructive && isFocused -> MaterialTheme.colorScheme.error
        isDestructive -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
        isFocused -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .focusRing(interactionSource, RoundedCornerShape(10.dp), width = 2.dp)
            .focusRequester(focusRequester)
            .selectable(
                selected = isFocused,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 13.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
                fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}

/**
 * Status option item for the dropdown
 */
@Composable
private fun StatusOption(
    text: String,
    statusColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val backgroundColor = when {
        isFocused -> MaterialTheme.colorScheme.primaryContainer
        isSelected -> MaterialTheme.colorScheme.surfaceContainerHighest
        else -> Color.Transparent
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .focusRing(interactionSource, RoundedCornerShape(10.dp), width = 2.dp)
            .selectable(
                selected = isFocused,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(statusColor, CircleShape),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isFocused) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/**
 * Full-screen System Menu
 * Opens with START button, shows profile and system settings
 */
@Composable
fun SystemMenu(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onNavigateRoute: (String) -> Unit,
    onDownloadsClick: () -> Unit = {},
    onLogout: () -> Unit,
    onGoOnline: () -> Unit,
    isOffline: Boolean = false,
    gogLoggedIn: Boolean,
    epicLoggedIn: Boolean,
    amazonLoggedIn: Boolean,
    onGogLoginClick: () -> Unit,
    onGogLogoutClick: () -> Unit,
    onEpicLoginClick: () -> Unit,
    onEpicLogoutClick: () -> Unit,
    onAmazonLoginClick: () -> Unit,
    onAmazonLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

    var persona by remember { mutableStateOf<SteamFriend?>(null) }
    var selectedStatus by remember(persona) { mutableStateOf(persona?.state ?: EPersonaState.Online) }
    var showStatusPicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        persona = SteamService.instance?.localPersona?.value
        SteamService.userSteamId?.let {
            SteamService.requestUserPersona()
        }
    }

    DisposableEffect(true) {
        val onPersonaStateReceived: (SteamEvent.PersonaStateReceived) -> Unit = { event ->
            Timber.d("SystemMenu onPersonaStateReceived: ${event.persona.state}")
            persona = event.persona
            selectedStatus = event.persona.state
        }

        val onLoggedOut: (SteamEvent.LoggedOut) -> Unit = {
            persona = SteamFriend()
            showStatusPicker = false
        }

        PluviaApp.events.on<SteamEvent.PersonaStateReceived, Unit>(onPersonaStateReceived)
        PluviaApp.events.on<SteamEvent.LoggedOut, Unit>(onLoggedOut)

        onDispose {
            PluviaApp.events.off<SteamEvent.PersonaStateReceived, Unit>(onPersonaStateReceived)
            PluviaApp.events.off<SteamEvent.LoggedOut, Unit>(onLoggedOut)
        }
    }

    BackHandler(enabled = isOpen && showStatusPicker) {
        showStatusPicker = false
    }

    val colorOnline = PluviaTheme.colors.statusInstalled
    val colorAway = PluviaTheme.colors.statusAway
    val colorOffline = PluviaTheme.colors.statusOffline

    val getStatusColor: (EPersonaState) -> Color = { state ->
        when (state) {
            EPersonaState.Online -> colorOnline
            EPersonaState.Away -> colorAway
            EPersonaState.Invisible, EPersonaState.Offline -> colorOffline
            else -> colorOnline
        }
    }

    ConsoleSidePanel(
        isOpen = isOpen,
        onDismiss = onDismiss,
        modifier = modifier,
    ) { firstItemFocusRequester ->
        ConsolePanelHeader(
            title = stringResource(R.string.system_menu_title),
            onBack = onDismiss,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Profile section
        val profileInteractionSource = remember { MutableInteractionSource() }
        val isProfileFocused by profileInteractionSource.collectIsFocusedAsState()
        Box {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isProfileFocused) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHigh
                                    },
                                )
                                .focusRing(profileInteractionSource, RoundedCornerShape(12.dp), width = 2.dp)
                                .selectable(
                                    selected = isProfileFocused,
                                    interactionSource = profileInteractionSource,
                                    indication = null,
                                    onClick = { if (!isOffline && SteamService.isLoggedIn) showStatusPicker = !showStatusPicker },
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            // Avatar
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (persona?.avatarHash?.isNotEmpty() == true) {
                                    SteamIconImage(
                                        size = 48.dp,
                                        image = { persona?.avatarHash?.getAvatarURL() },
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            // Name and status
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = persona?.name ?: stringResource(R.string.default_user_name),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(getStatusColor(selectedStatus), CircleShape),
                                    )
                                    Text(
                                        text = when (selectedStatus) {
                                            EPersonaState.Online -> stringResource(R.string.status_online)
                                            EPersonaState.Away -> stringResource(R.string.status_away)
                                            EPersonaState.Invisible -> stringResource(R.string.status_invisible)
                                            else -> selectedStatus.name
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            // Dropdown indicator (only when online)
                            if (!isOffline && SteamService.isLoggedIn) {
                                Icon(
                                    imageVector = if (showStatusPicker) {
                                        Icons.Default.KeyboardArrowUp
                                    } else {
                                        Icons.Default.KeyboardArrowDown
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        // Status picker dropdown
                        androidx.compose.material3.DropdownMenu(
                            expanded = showStatusPicker,
                            onDismissRequest = { showStatusPicker = false },
                            modifier = Modifier
                                .width(280.dp)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .focusGroup(),
                            ) {
                                Text(
                                    text = stringResource(R.string.status),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                )
                                listOf(
                                    Triple(EPersonaState.Online, stringResource(R.string.status_online), PluviaTheme.colors.statusInstalled),
                                    Triple(EPersonaState.Away, stringResource(R.string.status_away), PluviaTheme.colors.statusAway),
                                    Triple(EPersonaState.Invisible, stringResource(R.string.status_invisible), PluviaTheme.colors.statusOffline),
                                ).forEach { (state, label, color) ->
                                    StatusOption(
                                        text = label,
                                        statusColor = color,
                                        isSelected = selectedStatus == state,
                                        onClick = {
                                            selectedStatus = state
                                            showStatusPicker = false
                                            scope.launch {
                                                SteamService.setPersonaState(state)
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Menu items
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .focusGroup(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        SystemMenuItem(
                            text = stringResource(R.string.settings_text),
                            icon = Icons.Default.Settings,
                            onClick = {
                                onNavigateRoute(PluviaScreen.Settings.route)
                                onDismiss()
                            },
                            focusRequester = firstItemFocusRequester,
                        )

                        SystemMenuItem(
                            text = stringResource(R.string.app_downloads),
                            icon = Icons.Default.Download,
                            onClick = {
                                onDownloadsClick()
                                onDismiss()
                            },
                        )

                        SystemMenuItem(
                            text = stringResource(R.string.help_and_support),
                            icon = Icons.AutoMirrored.Filled.Help,
                            onClick = {
                                uriHandler.openUri("https://discord.gg/2hKv4VfZfE")
                            },
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (isOffline || !SteamService.isLoggedIn) {
                            val goOnlineLabelRes = if (!SteamService.isLoggedIn) {
                                R.string.steam_sign_in
                            } else {
                                R.string.steam_go_online
                            }
                            SystemMenuItem(
                                text = stringResource(goOnlineLabelRes),
                                icon = Icons.AutoMirrored.Filled.Login,
                                onClick = {
                                    onGoOnline()
                                    onDismiss()
                                },
                            )
                        } else {
                            SystemMenuItem(
                                text = stringResource(R.string.steam_go_offline),
                                icon = Icons.AutoMirrored.Filled.AirplaneTicket,
                                onClick = {
                                    onNavigateRoute(PluviaScreen.Home.route + "?offline=true") // TODO: test this
                                    onDismiss()
                                },
                            )

                            SystemMenuItem(
                                text = stringResource(R.string.steam_sign_out),
                                icon = Icons.AutoMirrored.Filled.Logout,
                                onClick = {
                                    onLogout()
                                    onDismiss()
                                },
                                isDestructive = true,
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // GOG
                        SystemMenuItem(
                            text = stringResource(
                                if (gogLoggedIn) R.string.gog_settings_logout_title
                                else R.string.gog_settings_login_title,
                            ),
                            icon = if (gogLoggedIn) {
                                Icons.AutoMirrored.Filled.Logout
                            } else {
                                Icons.AutoMirrored.Filled.Login
                            },
                            onClick = {
                                if (gogLoggedIn) onGogLogoutClick() else onGogLoginClick()
                                onDismiss()
                            },
                            isDestructive = gogLoggedIn,
                        )

                        // Epic
                        SystemMenuItem(
                            text = stringResource(
                                if (epicLoggedIn) R.string.epic_settings_logout_title
                                else R.string.epic_settings_login_title,
                            ),
                            icon = if (epicLoggedIn) {
                                Icons.AutoMirrored.Filled.Logout
                            } else {
                                Icons.AutoMirrored.Filled.Login
                            },
                            onClick = {
                                if (epicLoggedIn) onEpicLogoutClick() else onEpicLoginClick()
                                onDismiss()
                            },
                            isDestructive = epicLoggedIn,
                        )

                        // Amazon
                        SystemMenuItem(
                            text = stringResource(
                                if (amazonLoggedIn) R.string.amazon_settings_logout_title
                                else R.string.amazon_settings_login_title,
                            ),
                            icon = if (amazonLoggedIn) {
                                Icons.AutoMirrored.Filled.Logout
                            } else {
                                Icons.AutoMirrored.Filled.Login
                            },
                            onClick = {
                                if (amazonLoggedIn) onAmazonLogoutClick() else onAmazonLoginClick()
                                onDismiss()
                            },
                            isDestructive = amazonLoggedIn,
                        )
                    }

                    // Gamepad hint at bottom (only on expanded screens)
                    if (shouldShowGamepadUI()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.press_b_to_close),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        }
                    }
    }
}

/**
 * Touch-first, full-screen system center used by the lower display. It keeps
 * account connections and global destinations visible without borrowing the
 * gamepad focus or the narrow single-screen side-panel layout.
 */
@Composable
fun DualSystemMenu(
    onDismiss: () -> Unit,
    onNavigateRoute: (String) -> Unit,
    onDownloadsClick: () -> Unit,
    onLogout: () -> Unit,
    onGoOnline: () -> Unit,
    isOffline: Boolean,
    gogLoggedIn: Boolean,
    epicLoggedIn: Boolean,
    amazonLoggedIn: Boolean,
    onGogLoginClick: () -> Unit,
    onGogLogoutClick: () -> Unit,
    onEpicLoginClick: () -> Unit,
    onEpicLogoutClick: () -> Unit,
    onAmazonLoginClick: () -> Unit,
    onAmazonLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    var persona by remember { mutableStateOf<SteamFriend?>(null) }
    val initialFocusRequester = remember { FocusRequester() }
    val windowInfo = LocalWindowInfo.current

    LaunchedEffect(Unit) {
        persona = SteamService.instance?.localPersona?.value
        SteamService.userSteamId?.let { SteamService.requestUserPersona() }
        withTimeoutOrNull(2_000) {
            snapshotFlow { windowInfo.isWindowFocused }
                .filter { it }
                .first()
        }
        runCatching { initialFocusRequester.requestFocus() }
    }

    DisposableEffect(Unit) {
        val onPersonaStateReceived: (SteamEvent.PersonaStateReceived) -> Unit = { event ->
            persona = event.persona
        }
        val onLoggedOut: (SteamEvent.LoggedOut) -> Unit = {
            persona = SteamFriend()
        }
        PluviaApp.events.on<SteamEvent.PersonaStateReceived, Unit>(onPersonaStateReceived)
        PluviaApp.events.on<SteamEvent.LoggedOut, Unit>(onLoggedOut)
        onDispose {
            PluviaApp.events.off<SteamEvent.PersonaStateReceived, Unit>(onPersonaStateReceived)
            PluviaApp.events.off<SteamEvent.LoggedOut, Unit>(onLoggedOut)
        }
    }

    BackHandler(onBack = onDismiss)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        ConsolePanelHeader(
            title = stringResource(R.string.system_hub_title),
            onBack = onDismiss,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .focusGroup(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(R.string.system_hub_profile_section),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (persona?.avatarHash?.isNotEmpty() == true) {
                            SteamIconImage(
                                size = 58.dp,
                                image = { persona?.avatarHash?.getAvatarURL() },
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = persona?.name ?: PrefManager.username.ifBlank {
                                stringResource(R.string.default_user_name)
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = when {
                                !SteamService.isLoggedIn -> stringResource(R.string.system_hub_not_connected)
                                isOffline -> stringResource(R.string.system_hub_offline)
                                else -> stringResource(R.string.system_hub_connected)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = when {
                                !SteamService.isLoggedIn -> MaterialTheme.colorScheme.onSurfaceVariant
                                isOffline -> PluviaTheme.colors.statusAway
                                else -> PluviaTheme.colors.statusInstalled
                            },
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Column {
                    if (isOffline || !SteamService.isLoggedIn) {
                        SystemHubRow(
                            title = stringResource(
                                if (!SteamService.isLoggedIn) R.string.steam_sign_in else R.string.steam_go_online,
                            ),
                            subtitle = stringResource(R.string.system_hub_steam_action_description),
                            icon = Icons.AutoMirrored.Filled.Login,
                            focusRequester = initialFocusRequester,
                            onClick = onGoOnline,
                        )
                    } else {
                        SystemHubRow(
                            title = stringResource(R.string.steam_go_offline),
                            subtitle = stringResource(R.string.system_hub_steam_offline_description),
                            icon = Icons.AutoMirrored.Filled.AirplaneTicket,
                            focusRequester = initialFocusRequester,
                            onClick = {
                                onNavigateRoute(PluviaScreen.Home.route + "?offline=true")
                                onDismiss()
                            },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f))
                        SystemHubRow(
                            title = stringResource(R.string.steam_sign_out),
                            subtitle = stringResource(R.string.system_hub_steam_sign_out_description),
                            icon = Icons.AutoMirrored.Filled.Logout,
                            onClick = {
                                onLogout()
                                onDismiss()
                            },
                            destructive = true,
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.system_hub_destinations_section),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Column {
                    SystemHubRow(
                        title = stringResource(R.string.app_downloads),
                        subtitle = stringResource(R.string.system_hub_downloads_description),
                        icon = Icons.Default.Download,
                        onClick = {
                            onDownloadsClick()
                            onDismiss()
                        },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f))
                    SystemHubRow(
                        title = stringResource(R.string.settings_text),
                        subtitle = stringResource(R.string.system_hub_settings_description),
                        icon = Icons.Default.Settings,
                        onClick = {
                            onNavigateRoute(PluviaScreen.Settings.route)
                            onDismiss()
                        },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f))
                    SystemHubRow(
                        title = stringResource(R.string.help_and_support),
                        subtitle = stringResource(R.string.system_hub_support_description),
                        icon = Icons.AutoMirrored.Filled.Help,
                        onClick = { uriHandler.openUri("https://discord.gg/2hKv4VfZfE") },
                    )
                }
            }

            Text(
                text = stringResource(R.string.system_hub_connections_section),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Column {
                    SystemHubServiceRow(
                        serviceName = "GOG",
                        connected = gogLoggedIn,
                        onClick = if (gogLoggedIn) onGogLogoutClick else onGogLoginClick,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f))
                    SystemHubServiceRow(
                        serviceName = "Epic Games",
                        connected = epicLoggedIn,
                        onClick = if (epicLoggedIn) onEpicLogoutClick else onEpicLoginClick,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f))
                    SystemHubServiceRow(
                        serviceName = "Amazon Games",
                        connected = amazonLoggedIn,
                        onClick = if (amazonLoggedIn) onAmazonLogoutClick else onAmazonLoginClick,
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun SystemHubServiceRow(
    serviceName: String,
    connected: Boolean,
    onClick: () -> Unit,
) {
    SystemHubRow(
        title = serviceName,
        subtitle = stringResource(
            if (connected) R.string.system_hub_connected else R.string.system_hub_not_connected,
        ),
        icon = if (connected) Icons.AutoMirrored.Filled.Logout else Icons.AutoMirrored.Filled.Login,
        onClick = onClick,
        destructive = connected,
    )
}

@Composable
private fun SystemHubRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    destructive: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val contentColor = when {
        destructive -> MaterialTheme.colorScheme.error
        focused -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier,
            )
            .background(
                if (focused) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f)
                else Color.Transparent,
            )
            .selectable(
                selected = focused,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(24.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (destructive) {
                    MaterialTheme.colorScheme.error.copy(alpha = 0.76f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
    device = "spec:width=1920px,height=1080px,dpi=440,orientation=landscape",
)
@Composable
private fun Preview_SystemMenu() {
    val context = LocalContext.current
    PrefManager.init(context)
    PluviaTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Fake background content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "Game Library",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
                }

                SystemMenu(
                    isOpen = true,
                    onDismiss = { },
                    onNavigateRoute = { },
                    onLogout = { },
                    onGoOnline = { },
                    isOffline = false,
                    gogLoggedIn = false,
                    epicLoggedIn = false,
                    amazonLoggedIn = false,
                    onGogLoginClick = { },
                    onGogLogoutClick = { },
                    onEpicLoginClick = { },
                    onEpicLogoutClick = { },
                    onAmazonLoginClick = { },
                    onAmazonLogoutClick = { },
                )
            }
        }
    }
}
