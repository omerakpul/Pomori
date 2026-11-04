package com.pomori.ui.digest

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pomori.domain.usecase.GetDigest
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@HiltViewModel
class DigestViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getDigest: GetDigest,
) : ViewModel() {

    /** Navigasyon argümanı olarak gelir; özet ekranı seanssız açılamaz. */
    private val sessionId: Long = checkNotNull(savedStateHandle.get<Long>(ARG_SESSION_ID)) {
        "DigestViewModel için '$ARG_SESSION_ID' argümanı gerekli"
    }

    private val expanded = MutableStateFlow<Set<String>>(emptySet())

    val uiState: StateFlow<DigestUiState> = combine(getDigest(sessionId), expanded) { digest, expanded ->
        DigestUiState(
            isLoading = false,
            total = digest.total,
            apps = digest.apps,
            expanded = expanded,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), DigestUiState())

    /** Bir uygulamanın bildirim listesini açar ya da kapatır. */
    fun toggleExpanded(packageName: String) {
        expanded.update { if (packageName in it) it - packageName else it + packageName }
    }

    companion object {
        const val ARG_SESSION_ID = "sessionId"
        private const val STOP_TIMEOUT_MS = 5_000L
    }
}
