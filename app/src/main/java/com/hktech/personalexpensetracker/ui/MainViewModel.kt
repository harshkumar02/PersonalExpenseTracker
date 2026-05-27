package com.hktech.personalexpensetracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hktech.personalexpensetracker.data.*
import com.hktech.personalexpensetracker.ingest.TransactionParser
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {
    // Cache database instance to avoid repeated get() calls
    private val db = AppDatabase.get(app)
    private val dao = db.txnDao()
    private val categoryDao = db.categoryDao()
    private val merchantDao = db.merchantDao()
    private val accountDao = db.accountDao()
    private val channelDao = db.paymentChannelDao()

    // DAO already sorts by ts DESC, no in-memory sort needed
    val txns = dao.all()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val categories = categoryDao.getAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val merchants = merchantDao.getAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val accounts = accountDao.getAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val paymentChannels = channelDao.getAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        // Load initial data into parser (filter out empty emissions from Flow)
        viewModelScope.launch {
            merchants.collect { list ->
                if (list.isNotEmpty()) TransactionParser.updateMerchants(list)
            }
        }
        viewModelScope.launch {
            accounts.collect { list ->
                if (list.isNotEmpty()) TransactionParser.updateAccounts(list)
            }
        }
        viewModelScope.launch {
            paymentChannels.collect { list ->
                if (list.isNotEmpty()) TransactionParser.updatePaymentChannels(list)
            }
        }
    }

    fun updateCategory(id: Long, cat: String) = viewModelScope.launch {
        dao.updateCategory(id, cat)
    }

    fun deleteTransaction(id: Long) = viewModelScope.launch {
        dao.deleteById(id)
    }

    fun addTransaction(txn: TransactionEntity) = viewModelScope.launch {
        dao.addTransaction(txn)
    }

    fun addCategory(category: CategoryEntity) = viewModelScope.launch {
        categoryDao.insert(category)
    }

    fun updateCategory(category: CategoryEntity) = viewModelScope.launch {
        categoryDao.insert(category) // Room's REPLACE strategy handles updates
    }

    fun deleteCategory(name: String) = viewModelScope.launch {
        categoryDao.deleteByName(name)
    }

    fun addMerchant(merchant: MerchantEntity) = viewModelScope.launch {
        merchantDao.insert(merchant)
    }

    fun deleteMerchant(name: String) = viewModelScope.launch {
        merchantDao.deleteByName(name)
    }

    fun addAccount(account: AccountEntity) = viewModelScope.launch {
        accountDao.insert(account)
    }

    fun updateAccount(account: AccountEntity) = viewModelScope.launch {
        accountDao.update(account)
    }

    fun deleteAccount(id: Long) = viewModelScope.launch {
        accountDao.delete(id)
    }

    fun updateTransactionAccount(txnId: Long, accountId: Long?) = viewModelScope.launch {
        dao.updateAccountId(txnId, accountId)
    }

    fun addPaymentChannel(channel: PaymentChannelEntity) = viewModelScope.launch {
        channelDao.insert(channel)
    }

    fun deletePaymentChannel(code: String) = viewModelScope.launch {
        channelDao.deleteByCode(code)
    }

    fun updateTransaction(id: Long, amount: Double, direction: String, merchant: String?, channel: String?) = viewModelScope.launch {
        dao.updateTransaction(id, amount, direction, merchant, channel)
    }
}