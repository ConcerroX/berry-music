package com.berryos.music.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.ListItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import art.rippledesign.compose.rui.RAlertDialog
import coil3.compose.AsyncImage
import com.berryos.music.app.navigation.AppActions
import com.berryos.music.app.navigation.SearchRoute
import com.berryos.music.app.navigation.SettingsRoute
import com.berryos.music.core.auth.AuthController
import com.berryos.music.core.auth.AuthState
import com.berryos.music.resources.Res
import com.berryos.music.resources.account_service_name
import com.berryos.music.resources.add_account
import com.berryos.music.resources.expand
import com.berryos.music.resources.ic_add
import com.berryos.music.resources.ic_arrow_drop_down
import com.berryos.music.resources.ic_local_music
import com.berryos.music.resources.ic_person
import com.berryos.music.resources.ic_search
import com.berryos.music.resources.ic_settings
import com.berryos.music.resources.local_music
import com.berryos.music.resources.profile
import com.berryos.music.resources.search
import com.berryos.music.resources.select_account
import com.berryos.music.resources.settings
import com.berryos.music.resources.sign_in_failed
import com.berryos.music.resources.signing_in
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val DrawerHeaderBackground = Color(0xFFEEEEEE)
private val DrawerTitleColor = Color(0xFF212121)
private val DrawerSubtitleColor = Color(0xFF757575)

@Composable
internal fun AppDrawer(
    tabs: List<TabItem>,
    selectedTabIndex: Int,
    actions: AppActions,
    authState: AuthState,
    auth: AuthController,
    onSelectTab: (Int) -> Unit,
    onClose: () -> Unit,
) {
    var showAccountDialog by rememberSaveable { mutableStateOf(false) }
    val localMode = authState.showingLocal
    Column(modifier = Modifier.fillMaxSize()) {
        DrawerHeader(
            title = if (localMode) {
                stringResource(Res.string.local_music)
            } else {
                stringResource(Res.string.account_service_name)
            },
            subtitle = if (localMode) null else accountSubtitle(authState),
            avatarUrl = if (localMode) null else authState.activeAccount?.avatarUrl,
            onClick = { showAccountDialog = true },
        )
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            tabs.forEachIndexed { index, tab ->
                DrawerItem(stringResource(tab.label), tab.filledIcon, index == selectedTabIndex) {
                    onSelectTab(index)
                    onClose()
                }
            }
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            DrawerItem(stringResource(Res.string.search), Res.drawable.ic_search) {
                actions.open(SearchRoute)
                onClose()
            }
            DrawerItem(stringResource(Res.string.settings), Res.drawable.ic_settings) {
                actions.open(SettingsRoute)
                onClose()
            }
        }
    }
    if (showAccountDialog) {
        AccountDialog(
            authState = authState,
            onSignIn = {
                auth.signIn()
                onClose()
            },
            onSelectAccount = auth::selectAccount,
            onSelectLocal = auth::selectLocal,
            onDismissRequest = { showAccountDialog = false },
        )
    }
}

@Composable
private fun accountSubtitle(authState: AuthState): String? {
    val active = authState.activeAccount
    return when {
        authState.signingIn -> stringResource(Res.string.signing_in)
        authState.error != null -> stringResource(Res.string.sign_in_failed)
        active != null -> active.displayName
        else -> null
    }
}

@Composable
private fun DrawerHeader(title: String, subtitle: String?, avatarUrl: String?, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DrawerHeaderBackground)
            .statusBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            DrawerAvatar(avatarUrl)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clickable(onClick = onClick)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.subtitle2,
                        color = DrawerTitleColor,
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.body2,
                            color = DrawerSubtitleColor,
                        )
                    }
                }
                Icon(
                    painter = painterResource(Res.drawable.ic_arrow_drop_down),
                    contentDescription = stringResource(Res.string.expand),
                    tint = DrawerSubtitleColor,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DrawerAvatar(avatarUrl: String?) {
    Box(
        modifier = Modifier
            .padding(start = 16.dp, top = 16.dp)
            .size(64.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colors.primary),
        contentAlignment = Alignment.Center,
    ) {
        if (avatarUrl != null) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = stringResource(Res.string.profile),
                contentScale = ContentScale.Crop,
                error = painterResource(Res.drawable.ic_local_music),
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                painter = painterResource(Res.drawable.ic_local_music),
                contentDescription = stringResource(Res.string.profile),
                tint = Color.White,
                modifier = Modifier.size(40.dp),
            )
        }
    }
}

@Composable
private fun AccountDialog(
    authState: AuthState,
    onSignIn: () -> Unit,
    onSelectAccount: (String) -> Unit,
    onSelectLocal: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    RAlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(Res.string.select_account)) },
        text = {
            Column {
                AccountItem(
                    label = stringResource(Res.string.local_music),
                    icon = Res.drawable.ic_local_music,
                    onClick = {
                        onSelectLocal()
                        onDismissRequest()
                    },
                )
                authState.accounts.forEach { account ->
                    AccountItem(
                        label = stringResource(Res.string.account_service_name),
                        icon = Res.drawable.ic_person,
                        avatarUrl = account.avatarUrl,
                        secondaryLabel = account.displayName,
                        onClick = {
                            onSelectAccount(account.id)
                            onDismissRequest()
                        },
                    )
                }
                AccountItem(
                    label = if (authState.signingIn) {
                        stringResource(Res.string.signing_in)
                    } else {
                        stringResource(Res.string.add_account)
                    },
                    icon = Res.drawable.ic_add,
                    enabled = !authState.signingIn,
                    onClick = {
                        onDismissRequest()
                        onSignIn()
                    },
                )
            }
        },
        contentPadding = PaddingValues(bottom = 8.dp),
    )
}

@Composable
private fun AccountItem(
    label: String,
    icon: DrawableResource,
    avatarUrl: String? = null,
    secondaryLabel: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AccountAvatar(icon, avatarUrl)
        Spacer(modifier = Modifier.width(20.dp))
        if (secondaryLabel != null) {
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.54f),
                )
                Text(
                    text = secondaryLabel,
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.54f),
                )
            }
        } else {
            Text(
                text = label,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.54f),
            )
        }
    }
}

@Composable
private fun AccountAvatar(icon: DrawableResource, avatarUrl: String?) {
    if (avatarUrl != null) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            error = painterResource(icon),
            modifier = Modifier.size(40.dp).clip(CircleShape),
        )
    } else {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colors.onSurface.copy(alpha = 0.54f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colors.surface,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun DrawerItem(label: String, icon: DrawableResource, selected: Boolean = false, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        icon = { Icon(painter = painterResource(icon), contentDescription = null) },
        text = {
            Text(
                text = label,
                color = if (selected) MaterialTheme.colors.primary else Color.Unspecified,
            )
        },
    )
}
