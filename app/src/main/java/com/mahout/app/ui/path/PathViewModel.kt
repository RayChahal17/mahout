package com.mahout.app.ui.path

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahout.app.domain.path.model.Action
import com.mahout.app.domain.path.usecase.ObserveActiveActionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class PathViewModel @Inject constructor(
    observeActiveActionsUseCase: ObserveActiveActionsUseCase
) : ViewModel() {

    val actions: StateFlow<List<Action>> =
        observeActiveActionsUseCase()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
