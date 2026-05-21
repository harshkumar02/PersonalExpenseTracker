package com.hktech.personalexpensetracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TransactionEntity::class, CategoryEntity::class, MerchantEntity::class, AccountEntity::class, PaymentChannelEntity::class],
    version = 7,
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

        // Migration from version 4/5/6 to 7 (add rawText column if missing)
        private val MIGRATION_4_TO_7 = object : Migration(4, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create payment_channels table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS payment_channels (
                        code TEXT PRIMARY KEY,
                        displayName TEXT NOT NULL,
                        keywords TEXT NOT NULL DEFAULT ''
                    )
                """)

                // Seed default payment channels
                db.execSQL("INSERT INTO payment_channels (code, displayName, keywords) VALUES ('UPI', 'UPI', 'upi,@ok,vpa')")
                db.execSQL("INSERT INTO payment_channels (code, displayName, keywords) VALUES ('CARD', 'Debit/Credit Card', 'debit card,credit card,bank card,card xx,card ending')")
                db.execSQL("INSERT INTO payment_channels (code, displayName, keywords) VALUES ('ATM', 'ATM', 'atm')")
                db.execSQL("INSERT INTO payment_channels (code, displayName, keywords) VALUES ('NETBANKING', 'Net Banking', 'netbanking,imps,neft,rtgs')")
                db.execSQL("INSERT INTO payment_channels (code, displayName, keywords) VALUES ('WALLET', 'Wallet', 'wallet,paytm wallet')")
                db.execSQL("INSERT INTO payment_channels (code, displayName, keywords) VALUES ('CASH', 'Cash', 'cash')")
                db.execSQL("INSERT INTO payment_channels (code, displayName, keywords) VALUES ('OTHER', 'Other', '')")

                // Add default account
                db.execSQL("INSERT INTO accounts (bankName, cardSuffix, nickname, accountType, isActive) VALUES ('Unknown', '0000', 'Unknown Account', 'CARD', 1)")
            }
        }

        private val MIGRATION_5_TO_7 = object : Migration(5, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // payment_channels table already exists in v5, but ensure default account exists
                val cursor = db.query("SELECT COUNT(*) FROM accounts WHERE cardSuffix = '0000'")
                cursor.moveToFirst()
                if (cursor.getInt(0) == 0) {
                    db.execSQL("INSERT INTO accounts (bankName, cardSuffix, nickname, accountType, isActive) VALUES ('Unknown', '0000', 'Unknown Account', 'CARD', 1)")
                }
                cursor.close()
            }
        }

        private val MIGRATION_6_TO_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // No changes in v7 yet, just a version bump
                // Future migrations will add actual schema changes here
            }
        }

        fun get(ctx: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    ctx.applicationContext,
                    AppDatabase::class.java,
                    "personal_expense_sms.db"
                )
                .addMigrations(MIGRATION_4_TO_7, MIGRATION_5_TO_7, MIGRATION_6_TO_7)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        seedDatabase(db)
                    }
                })
                .build().also { INSTANCE = it }
            }

        private fun seedDatabase(db: SupportSQLiteDatabase) {
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

            // Add default account
            db.execSQL("INSERT INTO accounts (bankName, cardSuffix, nickname, accountType, isActive) VALUES ('Unknown', '0000', 'Unknown Account', 'CARD', 1)")

            // Seed payment channels
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
    }
}