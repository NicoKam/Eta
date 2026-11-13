package io.github.mangi.eta.data.repository

import io.github.mangi.eta.data.datastore.SettingsDataStore
import io.github.mangi.eta.data.model.ChatInputSettings
import kotlinx.coroutines.flow.Flow

/**
 * 对话输入框行为配置的读写入口；输入框自身直接订阅 [settingsFlow]，
 * 避免把开关状态逐层透传进聊天舞台。
 */
internal object ChatInputSettingsRepository {
    fun settingsFlow(): Flow<ChatInputSettings> = SettingsDataStore.chatInputSettingsFlow()

    suspend fun setSendOnEnter(enabled: Boolean) {
        SettingsDataStore.setChatInputSettings(
            SettingsDataStore.chatInputSettings().copy(sendOnEnter = enabled),
        )
    }
}
