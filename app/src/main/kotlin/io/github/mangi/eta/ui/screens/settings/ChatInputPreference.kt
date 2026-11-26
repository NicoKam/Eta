package io.github.mangi.eta.ui.screens.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mangi.eta.R
import io.github.mangi.eta.data.repository.ChatInputSettingsRepository
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Fork 输入配置独立收口，官方设置页只需插入一行调用。 */
@Composable
internal fun ChatInputSendOnEnterPreference() {
    val scope = rememberCoroutineScope()
    val settings by remember { ChatInputSettingsRepository.settingsFlow() }
        .collectAsStateWithLifecycle(initialValue = null)
    SwitchPreference(
        title = stringResource(R.string.settings_send_on_enter),
        summary = stringResource(R.string.settings_send_on_enter_summary),
        checked = settings?.sendOnEnter == true,
        enabled = settings != null,
        onCheckedChange = { enabled ->
            scope.launch { ChatInputSettingsRepository.setSendOnEnter(enabled) }
        },
        startAction = {
            Icon(
                imageVector = Icons.Rounded.Keyboard,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp).size(24.dp),
                tint = MiuixTheme.colorScheme.onBackground,
            )
        },
    )
}
