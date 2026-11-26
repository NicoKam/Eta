package io.github.mangi.eta.data.repository

import io.github.mangi.eta.EtaApp
import io.github.mangi.eta.data.datastore.SettingsDataStore
import io.github.mangi.eta.data.model.ChatInputSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = EtaApp::class, sdk = [36])
class ChatInputSettingsRepositoryTest {
    @Test
    fun missingSettingKeepsOfficialNewlineBehavior() {
        assertFalse(ChatInputSettings().sendOnEnter)
        assertFalse(Json.decodeFromString<ChatInputSettings>("{}").sendOnEnter)
    }

    @Test
    fun existingForkSettingRemainsCompatible() {
        assertTrue(Json.decodeFromString<ChatInputSettings>("{\"sendOnEnter\":true}").sendOnEnter)
    }

    @Test
    fun toggleRoundTripDoesNotChangeOfficialSettings() = runBlocking {
        val beforeInput = SettingsDataStore.chatInputSettings()
        val beforeOfficial = SettingsDataStore.settings()
        try {
            ChatInputSettingsRepository.setSendOnEnter(true)
            assertTrue(ChatInputSettingsRepository.settingsFlow().first().sendOnEnter)
            assertTrue(SettingsDataStore.chatInputSettings().sendOnEnter)
            assertEquals(beforeOfficial, SettingsDataStore.settings())

            ChatInputSettingsRepository.setSendOnEnter(false)
            assertFalse(ChatInputSettingsRepository.settingsFlow().first().sendOnEnter)
            assertEquals(beforeOfficial, SettingsDataStore.settings())
        } finally {
            SettingsDataStore.setChatInputSettings(beforeInput)
        }
    }
}
