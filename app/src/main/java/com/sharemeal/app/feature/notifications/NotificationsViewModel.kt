package com.sharemeal.app.feature.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sharemeal.app.core.common.AppResult
import com.sharemeal.app.domain.model.AppNotification
import com.sharemeal.app.domain.repository.NotificationRepository
import com.sharemeal.app.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val isLoading: Boolean = true,
    val notifications: List<AppNotification> = emptyList(),
    val errorText: String? = null
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val user = userRepository.currentUser.value
                ?: (userRepository.getCurrentUser() as? AppResult.Success)?.data
            notificationRepository.notifications.collect { all ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        notifications = if (user == null) emptyList()
                        else all.filter { notification -> notification.recipientId == user.id }
                            .sortedByDescending { notification -> notification.createdAt }
                    )
                }
            }
        }
    }

    fun markRead(notificationId: String) {
        viewModelScope.launch {
            when (notificationRepository.markRead(notificationId)) {
                is AppResult.Success -> Unit
                is AppResult.Failure -> _uiState.update {
                    it.copy(errorText = "Could not update notification.")
                }
            }
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            val user = userRepository.currentUser.value ?: return@launch
            notificationRepository.markAllRead(user.id)
        }
    }
}
