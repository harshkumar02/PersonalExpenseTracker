package com.hktech.personalexpensetracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TransactionEntity::class, CategoryEntity::class, MerchantEntity::class, AccountEntity::class, PaymentChannelEntity::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun txnDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun merchantDao(): MerchantDao
    abstract fun accountDao(): AccountDao
    abstract fun paymentChannelDao(): PaymentChannelDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(ctx: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    ctx.applicationContext,
                    AppDatabase::class.java,
                    "personal_expense_sms.db"
                )
                .fallbackToDestructiveMigration()
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed default categories
                        val defaultCategories = listOf(
                            "Food", "Groceries", "Transport", "Shopping", "Utilities",
                            "Fuel", "Rent", "Education", "Transfers", "Income", "Wallet", "UPI", "Uncategorized"
                        )
                        defaultCategories.forEach { cat ->
                            db.execSQL("INSERT INTO categories (name, color) VALUES ('$cat', '#FF5722')")
                        }

                        // Seed default merchants
                        val defaultMerchants = listOf(
                            MerchantEntity("swiggy", "Food", "swigato,swiggyy,swiggi"),
                            MerchantEntity("zomato", "Food", "zomatto,zomatoo"),
                            MerchantEntity("amazon", "Shopping", "amazn,amazone,amazon pay"),
                            MerchantEntity("flipkart", "Shopping", "flipcart,flipkartt"),
                            MerchantEntity("myntra", "Shopping"),
                            MerchantEntity("blinkit", "Groceries", "blinket"),
                            MerchantEntity("bigbasket", "Groceries", "bigbaskett"),
                            MerchantEntity("uber", "Transport"),
                            MerchantEntity("ola", "Transport"),
                            MerchantEntity("irctc", "Travel"),
                            MerchantEntity("makemytrip", "Travel"),
                            MerchantEntity("airtel", "Utilities"),
                            MerchantEntity("jio", "Utilities"),
                            MerchantEntity("tata power", "Utilities"),
                            MerchantEntity("paytm", "Wallet"),
                            MerchantEntity("phonepe", "UPI"),
                            MerchantEntity("google pay", "UPI"),
                            MerchantEntity("eatsure", "Food", "www eatsure,eatsure.com")
                        )
                        defaultMerchants.forEach { m ->
                            db.execSQL("INSERT INTO merchants (name, category, aliases) VALUES ('${m.name}', '${m.category}', '${m.aliases}')")
                        }

                        // Add default account type for uncategorized
                        db.execSQL("INSERT INTO accounts (bankName, cardSuffix, nickname, accountType, isActive) VALUES ('Unknown', '0000', 'Unknown Account', 'CARD', 1)")

                        // Seed default payment channels
                        val defaultChannels = listOf(
                            PaymentChannelEntity("UPI", "UPI", "upi,@ok,vpa"),
                            PaymentChannelEntity("CARD", "Debit/Credit Card", "debit card,credit card,bank card,card xx,card ending"),
                            PaymentChannelEntity("ATM", "ATM", "atm"),
                            PaymentChannelEntity("NETBANKING", "Net Banking", "netbanking,imps,neft,rtgs"),
                            PaymentChannelEntity("WALLET", "Wallet", "wallet,paytm wallet"),
                            PaymentChannelEntity("CASH", "Cash", "cash"),
                            PaymentChannelEntity("OTHER", "Other", "")
                        )
                        defaultChannels.forEach { ch ->
                            db.execSQL("INSERT INTO payment_channels (code, displayName, keywords) VALUES ('${ch.code}', '${ch.displayName}', '${ch.keywords}')")
                        }
                    }
                })
                .build().also { INSTANCE = it }
            }
    }
}