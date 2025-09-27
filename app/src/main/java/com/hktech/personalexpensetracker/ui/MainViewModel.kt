package com.hktech.personalexpensetracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hktech.personalexpensetracker.data.AppDatabase
import com.hktech.personalexpensetracker.data.TransactionEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).txnDao()

    val txns = dao.all()
        .map { it.sortedByDescending { t -> t.ts } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun updateCategory(id: Long, cat: String) = viewModelScope.launch {
        dao.updateCategory(id, cat)
    }
}
