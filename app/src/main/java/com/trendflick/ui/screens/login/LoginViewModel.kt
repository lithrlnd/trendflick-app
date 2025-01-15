package com.trendflick.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trendflick.data.repository.AtProtocolRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val atProtocolRepository: AtProtocolRepository
) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState())
    val loginState: StateFlow<LoginState> = _loginState

    fun login(handle: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState(isLoading = true)
            
            atProtocolRepository.createSession(handle, password)
                .onSuccess { session ->
                    _loginState.value = LoginState(isLoggedIn = true, session = session)
                }
                .onFailure { error ->
                    _loginState.value = LoginState(error = error.message ?: "Login failed")
                }
        }
    }
}

data class LoginState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val session: com.trendflick.data.model.AtSession? = null,
    val error: String? = null
) 