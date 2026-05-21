package com.hktech.personalexpensetracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hktech.personalexpensetracker.data.*
import com.hktech.personalexpensetracker.ingest.TransactionParser
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).txnDao()
    private val categoryDao = AppDatabase.get(app).categoryDao()
    private val merchantDao = AppDatabase.get(app).merchantDao()
    private val accountDao = AppDatabase.get(app).accountDao()
    private val channelDao = AppDatabase.get(app).paymentChannelDao()

    val txns = dao.all()
        .map { it.sortedByDescending { t -> t.ts } }
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
        // Load initial data into parser
        viewModelScope.launch {
            // Get initial data immediately
            val initialMerchants = merchantDao.getAllList()
            TransactionParser.updateMerchants(initialMerchants)

            val initialAccounts = accountDao.getAllList()
            TransactionParser.updateAccounts(initialAccounts)

            val initialChannels = channelDao.getAllList()
            TransactionParser.updatePaymentChannels(initialChannels)

            // Then continue collecting for updates
            merchants.collect { list ->
                TransactionParser.updateMerchants(list)
            }
        }
        viewModelScope.launch {
            accounts.collect { list ->
                TransactionParser.updateAccounts(list)
            }
        }
        viewModelScope.launch {
            paymentChannels.collect { list ->
                TransactionParser.updatePaymentChannels(list)
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