package com.sharemeal.app.core.session

import androidx.lifecycle.ViewModel
import com.sharemeal.app.domain.model.AuthSession
import com.sharemeal.app.domain.repository.AuthenticationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class SessionViewModel @Inject constructor(
    authenticationRepository: AuthenticationRepository
) : ViewModel() {
    val session: StateFlow<AuthSession?> = authenticationRepository.session
    val isSessionRestored: StateFlow<Boolean> = authenticationRepository.isSessionRestored
}
