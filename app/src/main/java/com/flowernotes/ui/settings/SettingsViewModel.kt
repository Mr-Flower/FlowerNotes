package com.flowernotes.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flowernotes.data.LlmProviderType
import com.flowernotes.data.Settings
import com.flowernotes.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)

    val settings: StateFlow<Settings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Settings())

    fun setProvider(provider: LlmProviderType) {
        viewModelScope.launch { settingsRepository.setProvider(provider) }
    }

    fun setApiKey(provider: LlmProviderType, key: String) {
        viewModelScope.launch { settingsRepository.setApiKey(provider, key.trim()) }
    }
}
