package com.hktech.personalexpensetracker.ingest

import android.util.Log
import com.hktech.personalexpensetracker.data.AccountEntity
import com.hktech.personalexpensetracker.data.MerchantEntity
import com.hktech.personalexpensetracker.data.PaymentChannelEntity
import com.hktech.personalexpensetracker.data.TransactionEntity

/**
 * Base interface for all extractors
 */
interface TextExtractor<T> {
    fun extract(text: String): T?
}

/**
 * Handles amount extraction from various SMS formats
 */
class AmountExtractor : TextExtractor<Double> {
    // Pattern for currency followed by amount: Rs. 470, INR 354, ₹500
    private val currencyFirstPattern = Regex(
        """(?:Rs\.?|INR|₹)\s*([0-9,]+(?:\.[0-9]+)?)""",
        RegexOption.IGNORE_CASE
    )

    // Patterns that indicate this is a transaction amount (not balance)
    private val transactionKeywords = listOf(
        "debited", "spent", "paid", "received", "credited",
        "withdrawn", "purchase", "transfer", "deposit"
    )

    override fun extract(text: String): Double? {
        val norm = text.lowercase()

        // For "credited" patterns - amount can be BEFORE the keyword
        // Handle: "INR 16.00... credited", "earned ... credited", "interest ... credited"
        if (norm.contains("credited")) {
            val keywordIndex = norm.indexOf("credited")
            // Look 150 chars BEFORE "credited" for the amount
            val searchStart = maxOf(0, keywordIndex - 150)
            val searchAreaBefore = text.substring(searchStart, keywordIndex)
            findAmountInArea(searchAreaBefore)?.let { return it }
            // Also look after in case amount is after
            findAmountNearKeyword(text, "credited")?.let { return it }
        }

        // For "debited" - amount is BEFORE this phrase
        if (norm.contains("debited")) {
            val keywordIndex = norm.indexOf("debited")
            val searchStart = maxOf(0, keywordIndex - 100)
            val searchArea = text.substring(searchStart, keywordIndex)
            findAmountInArea(searchArea)?.let { return it }
        }

        // For patterns where amount comes BEFORE the keyword
        val beforeKeywordPatterns = listOf(
            "spent using", "paid to", "paid for"
        )
        for (pattern in beforeKeywordPatterns) {
            if (norm.contains(pattern)) {
                val keywordIndex = norm.indexOf(pattern)
                val searchStart = maxOf(0, keywordIndex - 100)
                val searchArea = text.substring(searchStart, keywordIndex)
                findAmountInArea(searchArea)?.let { return it }
            }
        }

        // Try to find amount near other transaction keywords
        transactionKeywords.filter { it != "spent" && it != "credited" }.forEach { keyword ->
            if (norm.contains(keyword)) {
                findAmountNearKeyword(text, keyword)?.let { return it }
            }
        }

        // Fallback: find first currency amount
        val amounts = currencyFirstPattern.findAll(text).map {
            it.groupValues[1].replace(",", "").toDoubleOrNull()
        }.filterNotNull().toList()

        if (amounts.isNotEmpty()) {
            // For credit transactions, first amount is usually the interest/credit
            // For debit transactions, first amount is usually the transaction amount
            return amounts.firstOrNull()
        }

        // Last resort: find any large number with commas
        return findLargeNumber(text)
    }

    private fun findAmountInArea(searchArea: String): Double? {
        // Look for amount with currency prefix first
        currencyFirstPattern.find(searchArea)?.let {
            return it.groupValues[1].replace(",", "").toDoubleOrNull()
        }
        // Also check for plain numbers with commas (like "3,600.00" without INR/Rs)
        return Regex("""([0-9,]+(?:\.[0-9]+)?)""").find(searchArea)?.let {
            val cleaned = it.groupValues[1].replace(",", "")
            cleaned.toDoubleOrNull()
        }
    }

    private fun findAmountNearKeyword(text: String, keyword: String): Double? {
        val lowerText = text.lowercase()
        val keywordIndex = lowerText.indexOf(keyword)
        if (keywordIndex == -1) return null

        // Search for amount within 100 chars after keyword
        val searchStart = keywordIndex
        val searchEnd = minOf(keywordIndex + 100, text.length)
        val searchArea = text.substring(searchStart, searchEnd)

        currencyFirstPattern.find(searchArea)?.let {
            return it.groupValues[1].replace(",", "").toDoubleOrNull()
        }

        // Also check for "Rs.XXX" format without space
        Regex("""Rs\.?\s*([0-9,]+(?:\.[0-9]+)?)""", RegexOption.IGNORE_CASE)
            .find(searchArea)?.let {
                return it.groupValues[1].replace(",", "").toDoubleOrNull()
            }

        return null
    }

    private fun findLargeNumber(text: String): Double? {
        // Find numbers with comma formatting like 2,884 or 50,259
        val commaNumberPattern = Regex("""\b([0-9]{1,3},[0-9]{3}(?:\.[0-9]+)?)\b""")
        return commaNumberPattern.findAll(text)
            .map { it.groupValues[1].replace(",", "").toDoubleOrNull() }
            .filterNotNull()
            .maxByOrNull { it }
    }

    fun extractAll(text: String): List<Double> {
        return currencyFirstPattern.findAll(text)
            .mapNotNull { it.groupValues[1].replace(",", "").toDoubleOrNull() }
            .toList()
    }
}

/**
 * Handles balance extraction from SMS
 */
class BalanceExtractor : TextExtractor<Double> {
    // Patterns for balance-related keywords
    private val balancePatterns = listOf(
        Regex("""(?:available\s*(?:bal|balance)|balance|bal|new\s*bal)\s*[:\-]?\s*(?:Rs\.?|INR|₹)?\s*([0-9,]+(?:\.[0-9]+)?)""", RegexOption.IGNORE_CASE),
        Regex("""(?:avl\s*(?:bal|limit)|limit)\s*[:\-]?\s*(?:Rs\.?|INR|₹)?\s*([0-9,]+(?:\.[0-9]+)?)""", RegexOption.IGNORE_CASE),
        Regex("""(?:new\s*bal|available\s*bal|available)\s*[:\-]?\s*(?:Rs\.?|INR|₹)?\s*([0-9,]+(?:\.[0-9]+)?)""", RegexOption.IGNORE_CASE)
    )

    override fun extract(text: String): Double? {
        for (pattern in balancePatterns) {
            pattern.find(text)?.let { match ->
                val amount = match.groupValues[1].replace(",", "").toDoubleOrNull()
                // Ignore small amounts (likely interest amounts, not balances)
                if (amount != null && amount > 100) {
                    return amount
                }
            }
        }
        // Fallback: find largest number with comma formatting (likely balance)
        return findLargestBalanceAmount(text)
    }

    private fun findLargestBalanceAmount(text: String): Double? {
        val commaNumberPattern = Regex("""\b([0-9]{1,3},[0-9]{3}(?:\.[0-9]+)?)\b""")
        return commaNumberPattern.findAll(text)
            .map { it.groupValues[1].replace(",", "").toDoubleOrNull() }
            .filterNotNull()
            .maxByOrNull { it }
    }
}

/**
 * Handles account/card suffix detection
 */
class AccountDetector {
    private val patterns = listOf(
        Regex("""XX([0-9]{4})\b""", RegexOption.IGNORE_CASE),
        Regex("""(?:xx|ending|no\.?|card\s*)\s*([0-9]{4})""", RegexOption.IGNORE_CASE),
        Regex("""(?:a/c|account)\s*(?:xx|ending)?\s*([0-9]{4})""", RegexOption.IGNORE_CASE),
        Regex("""\*([0-9]{4})\b""")
    )

    fun detect(text: String): String? {
        for (pattern in patterns) {
            pattern.find(text)?.let { match ->
                val suffix = match.groupValues[1]
                if (suffix.length == 4 && suffix.all { it.isDigit() }) {
                    return suffix
                }
            }
        }
        return null
    }

    fun matchToAccount(suffix: String, accounts: Map<String, AccountEntity>): Long? {
        return accounts[suffix]?.id
    }
}

/**
 * Handles debit/credit direction detection
 */
class DirectionDetector {
    private val creditKeywords = listOf(
        "credited", "received", "deposited", "refund credited", "cashback",
        "salary credited", "money received", "fund received", "credit to",
        "amount credited", "is credited", "has been credited", "cr.", "+"
    )

    private val debitKeywords = listOf(
        "debited", "spent", "withdrawn", "paid", "purchase", "deducted",
        "dr.", "debit to", "paid to", "transfer to", "payment of",
        "-", "debit", "debit for", "sent", "send"
    )

    fun detect(text: String): String {
        val norm = text.lowercase()

        // "debited by" is the most authoritative indicator of DEBIT
        // Even if "credited" appears elsewhere (e.g., "JIO credited"), we trust "debited by"
        if (Regex("""debited\s*by""").containsMatchIn(norm)) {
            return "DEBIT"
        }

        // "credited" in the context of your account (not a third party)
        // Patterns like "A/c credited", "account credited", "credited to your"
        if (Regex("""(?:a/c|account|your\s+(?:account|a/c))\s+credited""").containsMatchIn(norm)) {
            return "CREDIT"
        }

        // Check remaining credit keywords (but exclude "credited" as it's ambiguous)
        val remainingCreditKeywords = creditKeywords.filter { it != "credited" }
        if (remainingCreditKeywords.any { norm.contains(it) }) {
            return "CREDIT"
        }

        // Check debit keywords (excluding "debited by" which we already handled)
        val remainingDebitKeywords = debitKeywords.filter { it != "debited" }
        if (remainingDebitKeywords.any { norm.contains(it) }) {
            return "DEBIT"
        }

        // Default assumption
        return "DEBIT"
    }
}

/**
 * Handles payment channel detection
 */
class ChannelDetector {
    private var channels: List<PaymentChannelEntity> = emptyList()

    fun update(channels: List<PaymentChannelEntity>) {
        if (channels.isNotEmpty()) this.channels = channels
    }

    fun detect(text: String): String {
        val norm = text.lowercase()

        // Check UPI patterns first
        if (Regex("""(@ok|@[a-z0-9]|vpa|upi|gpay|phonepe|paytm|google\s*pay)""", RegexOption.IGNORE_CASE).containsMatchIn(text)) {
            return "UPI"
        }

        // Check card patterns
        if (Regex("""(debit\s*card|credit\s*card|bank\s*card|xx[0-9]{4})""", RegexOption.IGNORE_CASE).containsMatchIn(norm)) {
            return "CARD"
        }

        // Check database channels
        for (channel in channels) {
            if (channel.code == "OTHER" || channel.code == "CARD" || channel.code == "UPI") continue
            if (channel.keywords.isBlank()) continue

            val keywords = channel.keywords.split(",").map { it.trim().lowercase() }
            if (keywords.any { it in norm }) {
                return channel.code
            }
        }

        // Check ATM
        if ("atm" in norm || "cash withdrawal" in norm) {
            return "ATM"
        }

        // Check netbanking
        if (Regex("""(netbanking|imps|neft|rtgs)""", RegexOption.IGNORE_CASE).containsMatchIn(norm)) {
            return "NETBANKING"
        }

        return "OTHER"
    }
}

/**
 * Handles merchant matching
 */
class MerchantMatcher {
    private var merchants: List<MerchantEntity> = emptyList()

    fun update(merchants: List<MerchantEntity>) {
        this.merchants = merchants
    }

    fun find(text: String): MerchantEntity? {
        if (merchants.isEmpty()) return null

        val normalizedText = text.lowercase().replace(Regex("""\s+"""), " ")

        for (merchant in merchants) {
            // Exact match
            if (merchant.name.lowercase() in normalizedText) {
                return merchant
            }

            // Alias match
            if (merchant.aliases.isNotBlank()) {
                val aliases = merchant.aliases.split(",").map { it.trim().lowercase() }
                if (aliases.any { alias ->
                    alias in normalizedText ||
                    alias.replace(Regex("""\s+"""), " ") in normalizedText
                }) {
                    return merchant
                }
            }
        }

        return null
    }
}

/**
 * Handles skip pattern detection (OTP, non-transaction messages)
 */
class SkipDetector {
    private val skipPatterns = listOf(
        Regex("""otp|one.?time.?password""", RegexOption.IGNORE_CASE),
        Regex("""secret|do not share|never share""", RegexOption.IGNORE_CASE),
        Regex("""fund\s*bal|securities|demat|pms|portfolio""", RegexOption.IGNORE_CASE),
        Regex("""(zerodha|groww|upstox|indstocks)""", RegexOption.IGNORE_CASE),
        Regex("""ussd|dial\s*\*|star\s*121""", RegexOption.IGNORE_CASE),
        Regex("""recharge|ulpack|last.?call|call\s*duration""", RegexOption.IGNORE_CASE),
        Regex("""verification\s*code|login\s*otp|transaction\s*otp""", RegexOption.IGNORE_CASE),
        Regex("""\b[0-9]{4,8}\b.*(?:otp|verification)""", RegexOption.IGNORE_CASE)
    )

    private val transactionKeywords = listOf(
        "debited", "credited", "spent", "received", "paid", "withdrawn",
        "purchase", "payment", "transaction", "txn", "transfer",
        "deposit", "deducted", "sent", "received", "failed"
    )

    fun shouldSkip(text: String): Boolean {
        if (skipPatterns.any { it.containsMatchIn(text) }) {
            return true
        }
        if (!transactionKeywords.any { text.lowercase().contains(it) }) {
            return true
        }
        return false
    }
}

/**
 * Determines transaction category based on various hints
 */
class CategoryResolver {
    private val transferKeywords = listOf(
        "self transfer", "own account", "fund transfer", "imps to self",
        "neft to self", "to own", "own a/c"
    )

    private val categoryHints = mapOf(
        "Fuel" to listOf("fuel", "petrol", "bharat petroleum", "shell", "hpcl", "ioc"),
        "Utilities" to listOf("electric", "electricity", "bill", "dth", "gas", "water", "bsnl"),
        "Rent" to listOf("rent", "rental"),
        "Education" to listOf("school", "college", "fee", "tuition"),
        "Groceries" to listOf("grocery", "supermarket", "bigbasket", "blinkit", "dmart"),
        "Food" to listOf("restaurant", "food", "swiggy", "zomato", "dominos", "mcdonalds"),
        "Shopping" to listOf("amazon", "flipkart", "myntra", "shopping", "myntra"),
        "Transport" to listOf("uber", "ola", "taxi"),
        "Healthcare" to listOf("hospital", "pharmacy", "medicine", "doctor"),
        "Entertainment" to listOf("movie", "netflix", "hotstar", "spotify", "youtube")
    )

    fun resolve(
        isTransfer: Boolean,
        merchant: MerchantEntity?,
        text: String,
        channel: String,
        direction: String
    ): String {
        if (isTransfer || transferKeywords.any { text.lowercase().contains(it) }) {
            return "Transfers"
        }

        merchant?.let { return it.category }

        val norm = text.lowercase()
        for ((category, hints) in categoryHints) {
            if (hints.any { it in norm }) {
                return category
            }
        }

        if (channel == "UPI" && direction == "DEBIT") {
            return "Transfers"
        }

        if (direction == "CREDIT") {
            return "Income"
        }

        return "Uncategorized"
    }
}

/**
 * Main Transaction Parser - orchestrates all extractors
 */
object TransactionParser {
    private val amountExtractor = AmountExtractor()
    private val balanceExtractor = BalanceExtractor()
    private val accountDetector = AccountDetector()
    private val directionDetector = DirectionDetector()
    private val channelDetector = ChannelDetector()
    private val merchantMatcher = MerchantMatcher()
    private val skipDetector = SkipDetector()
    private val categoryResolver = CategoryResolver()

    private var merchants: List<MerchantEntity> = emptyList()
    private var accounts: Map<String, AccountEntity> = emptyMap()

    fun updateMerchants(newMerchants: List<MerchantEntity>) {
        merchants = newMerchants ?: emptyList()
        merchantMatcher.update(merchants)
    }

    fun updateAccounts(newAccounts: List<AccountEntity>) {
        accounts = newAccounts.filter { it.isActive }.associateBy { it.cardSuffix }
        Log.d("TransactionParser", "Updated accounts: ${accounts.keys}, count=${accounts.size}")
    }

    fun updatePaymentChannels(channels: List<PaymentChannelEntity>) {
        channelDetector.update(channels)
    }

    data class ParseResult(
        val transaction: TransactionEntity,
        val detectedAccountId: Long?,
        val balance: Double?
    )

    fun parse(text: String, receivedAt: Long, source: String = "SMS"): ParseResult? {
        Log.d("TransactionParser", "Parsing: $text")

        // Skip non-transaction messages
        if (skipDetector.shouldSkip(text)) {
            Log.d("TransactionParser", "Skipped: OTP/non-transaction")
            return null
        }

        // Extract all components
        val amount = amountExtractor.extract(text) ?: run {
            Log.d("TransactionParser", "No amount found")
            return null
        }

        val balance = balanceExtractor.extract(text)
        val accountSuffix = accountDetector.detect(text)
        val direction = directionDetector.detect(text)
        val channel = channelDetector.detect(text)
        val merchant = merchantMatcher.find(text)
        val accountId = accountSuffix?.let { accountDetector.matchToAccount(it, accounts) }

        // Determine category
        val isTransfer = listOf("self transfer", "own account", "fund transfer", "imps to self", "neft to self")
            .any { text.lowercase().contains(it) }
        val category = categoryResolver.resolve(isTransfer, merchant, text, channel, direction)

        Log.d("TransactionParser", "Result: amt=$amount, dir=$direction, merchant=${merchant?.name}, acc=$accountSuffix (id=$accountId), bal=$balance")

        return ParseResult(
            transaction = TransactionEntity(
                ts = receivedAt,
                source = source,
                channel = channel,
                direction = direction,
                merchant = merchant?.name?.replaceFirstChar { it.uppercase() },
                amount = amount,
                accountHint = accountSuffix,
                rawText = text,
                category = category,
                accountId = accountId,
                isTransfer = isTransfer
            ),
            detectedAccountId = accountId,
            balance = balance
        )
    }
}