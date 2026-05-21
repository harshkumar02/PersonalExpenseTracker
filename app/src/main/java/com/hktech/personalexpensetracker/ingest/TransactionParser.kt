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
    fun extract(text: String, norm: String = text.lowercase()): T?
}

/**
 * Handles amount extraction from various SMS formats
 */
class AmountExtractor : TextExtractor<Double> {
    private val currencyFirstPattern = Regex(
        """(?:Rs\.?|INR|₹)\s*([0-9,]+(?:\.[0-9]+)?)""",
        RegexOption.IGNORE_CASE
    )

    private val plainNumberPattern = Regex("""([0-9,]+(?:\.[0-9]+)?)""")
    private val commaNumberPattern = Regex("""\b([0-9]{1,3},[0-9]{3}(?:\.[0-9]+)?)\b""")

    private val transactionKeywords = listOf(
        "debited", "spent", "paid", "received", "credited",
        "withdrawn", "purchase", "transfer", "deposit"
    )

    override fun extract(text: String, norm: String): Double? {
        // For "debited by" pattern - amount is AFTER this phrase
        if (norm.contains("debited by")) {
            val keywordIndex = norm.indexOf("debited by")
            findAmountInArea(text, keywordIndex, keywordIndex + 150)?.let { return it }
        }

        // For "debited" alone - amount is BEFORE
        if (norm.contains("debited") && !norm.contains("debited by")) {
            val keywordIndex = norm.indexOf("debited")
            findAmountInArea(text, maxOf(0, keywordIndex - 100), keywordIndex)?.let { return it }
        }

        // For "credited" patterns - look before and after
        if (norm.contains("credited")) {
            val keywordIndex = norm.indexOf("credited")
            findAmountInArea(text, maxOf(0, keywordIndex - 150), keywordIndex)?.let { return it }
            findAmountNearKeyword(text, "credited")?.let { return it }
        }

        // Check patterns where amount comes BEFORE the keyword
        for (pattern in listOf("spent using", "paid to", "paid for")) {
            if (norm.contains(pattern)) {
                val keywordIndex = norm.indexOf(pattern)
                findAmountInArea(text, maxOf(0, keywordIndex - 100), keywordIndex)?.let { return it }
            }
        }

        // Try to find amount near other transaction keywords
        transactionKeywords.filter { it != "spent" && it != "credited" && it != "debited" }.forEach { keyword ->
            if (norm.contains(keyword)) {
                findAmountNearKeyword(text, keyword)?.let { return it }
            }
        }

        // Fallback: find first currency amount
        currencyFirstPattern.find(text)?.let {
            return it.groupValues[1].replace(",", "").toDoubleOrNull()
        }

        // Last resort: find any large number with commas
        return commaNumberPattern.findAll(text)
            .mapNotNull { it.groupValues[1].replace(",", "").toDoubleOrNull() }
            .maxByOrNull { it }
    }

    private fun findAmountInArea(text: String, start: Int, end: Int): Double? {
        if (start >= end) return null
        val searchArea = text.substring(start, end)

        currencyFirstPattern.find(searchArea)?.let {
            return it.groupValues[1].replace(",", "").toDoubleOrNull()
        }
        return plainNumberPattern.find(searchArea)?.let {
            val cleaned = it.groupValues[1].replace(",", "")
            cleaned.toDoubleOrNull()
        }
    }

    private fun findAmountNearKeyword(text: String, keyword: String): Double? {
        val keywordIndex = text.lowercase().indexOf(keyword)
        if (keywordIndex == -1) return null

        val searchArea = text.substring(keywordIndex, minOf(keywordIndex + 100, text.length))

        currencyFirstPattern.find(searchArea)?.let {
            return it.groupValues[1].replace(",", "").toDoubleOrNull()
        }
        Regex("""Rs\.?\s*([0-9,]+(?:\.[0-9]+)?)""", RegexOption.IGNORE_CASE)
            .find(searchArea)?.let {
                return it.groupValues[1].replace(",", "").toDoubleOrNull()
            }
        return null
    }

    fun extractAll(text: String): List<Double> =
        currencyFirstPattern.findAll(text)
            .mapNotNull { it.groupValues[1].replace(",", "").toDoubleOrNull() }
            .toList()
}

/**
 * Handles balance extraction from SMS
 */
class BalanceExtractor : TextExtractor<Double> {
    private val balancePatterns = listOf(
        Regex("""(?:available\s*(?:bal|balance)|balance|bal|new\s*bal)\s*[:\-]?\s*(?:Rs\.?|INR|₹)?\s*([0-9,]+(?:\.[0-9]+)?)""", RegexOption.IGNORE_CASE),
        Regex("""(?:avl\s*(?:bal|limit)|limit)\s*[:\-]?\s*(?:Rs\.?|INR|₹)?\s*([0-9,]+(?:\.[0-9]+)?)""", RegexOption.IGNORE_CASE),
        Regex("""(?:new\s*bal|available\s*bal|available)\s*[:\-]?\s*(?:Rs\.?|INR|₹)?\s*([0-9,]+(?:\.[0-9]+)?)""", RegexOption.IGNORE_CASE)
    )
    private val commaNumberPattern = Regex("""\b([0-9]{1,3},[0-9]{3}(?:\.[0-9]+)?)\b""")

    override fun extract(text: String, norm: String): Double? {
        for (pattern in balancePatterns) {
            pattern.find(text)?.let { match ->
                val amount = match.groupValues[1].replace(",", "").toDoubleOrNull()
                if (amount != null && amount > 100) return amount
            }
        }
        return commaNumberPattern.findAll(text)
            .mapNotNull { it.groupValues[1].replace(",", "").toDoubleOrNull() }
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
                if (suffix.length == 4 && suffix.all { it.isDigit() }) return suffix
            }
        }
        return null
    }

    fun matchToAccount(suffix: String, accounts: Map<String, AccountEntity>): Long? =
        accounts[suffix]?.id
}

/**
 * Handles debit/credit direction detection
 */
class DirectionDetector {
    // Pre-compiled patterns
    private val DEBIT_BY = Regex("""debited\s*by""", RegexOption.IGNORE_CASE)
    private val CREDITED_TO = Regex("""credited\s+to""", RegexOption.IGNORE_CASE)
    private val ACCOUNT_CREDITED = Regex("""(?:a/c|account|xxxx)\s+\w*\s*credited""", RegexOption.IGNORE_CASE)
    private val CREDITED_WORD = Regex("""\bcredited\b""", RegexOption.IGNORE_CASE)
    private val DEBIT_KEYWORDS = Regex("""\b(debited|spent|paid|withdrawn|purchase|deducted|sent)\b""", RegexOption.IGNORE_CASE)

    fun detect(text: String): String {
        val norm = text.lowercase()

        return when {
            DEBIT_BY.containsMatchIn(norm) -> "DEBIT"
            CREDITED_TO.containsMatchIn(norm) -> "CREDIT"
            ACCOUNT_CREDITED.containsMatchIn(norm) -> "CREDIT"
            CREDITED_WORD.containsMatchIn(norm) -> "CREDIT"
            DEBIT_KEYWORDS.containsMatchIn(norm) -> "DEBIT"
            else -> "DEBIT"
        }
    }
}

/**
 * Handles payment channel detection
 */
class ChannelDetector {
    private var channels: List<PaymentChannelEntity> = emptyList()

    // Pre-compiled patterns
    private val UPI_PATTERN = Regex("""(@ok|@[a-z0-9]|vpa|upi|gpay|phonepe|paytm|google\s*pay)""", RegexOption.IGNORE_CASE)
    private val CARD_PATTERN = Regex("""(debit\s*card|credit\s*card|bank\s*card|xx[0-9]{4})""", RegexOption.IGNORE_CASE)
    private val NETBANK_PATTERN = Regex("""(netbanking|imps|neft|rtgs)""", RegexOption.IGNORE_CASE)

    fun update(channels: List<PaymentChannelEntity>) {
        if (channels.isNotEmpty()) this.channels = channels
    }

    fun detect(text: String): String {
        val norm = text.lowercase()

        when {
            UPI_PATTERN.containsMatchIn(text) -> return "UPI"
            CARD_PATTERN.containsMatchIn(norm) -> return "CARD"
            norm.contains("atm") || norm.contains("cash withdrawal") -> return "ATM"
            NETBANK_PATTERN.containsMatchIn(norm) -> return "NETBANKING"
        }

        for (channel in channels) {
            if (channel.code == "OTHER" || channel.code == "CARD" || channel.code == "UPI") continue
            if (channel.keywords.isBlank()) continue
            val keywords = channel.keywords.split(",").map { it.trim().lowercase() }
            if (keywords.any { it in norm }) return channel.code
        }

        return "OTHER"
    }
}

/**
 * Handles merchant matching
 */
class MerchantMatcher {
    private var merchants: List<MerchantEntity> = emptyList()
    private var merchantLowerNames: List<String> = emptyList()
    private var merchantAliases: List<Pair<String, String>> = emptyList() // alias, merchantName

    fun update(merchants: List<MerchantEntity>) {
        this.merchants = merchants
        this.merchantLowerNames = merchants.map { it.name.lowercase() }
        this.merchantAliases = merchants.flatMap { m ->
            m.aliases.split(",").map { alias -> alias.trim().lowercase() to m.name.lowercase() }
        }
    }

    fun find(text: String): MerchantEntity? {
        if (merchants.isEmpty()) return null

        val normalizedText = text.lowercase().replace(Regex("""\s+"""), " ")

        // Check exact match on merchant names
        for ((index, lowerName) in merchantLowerNames.withIndex()) {
            if (lowerName in normalizedText) return merchants[index]
        }

        // Check aliases
        for ((alias, merchantName) in merchantAliases) {
            if (alias in normalizedText) {
                return merchants.find { it.name.lowercase() == merchantName }
            }
        }

        return null
    }
}

/**
 * Handles skip pattern detection (OTP, non-transaction messages)
 */
class SkipDetector {
    // Pre-compiled skip patterns
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
        "deposit", "deducted", "sent", "failed"
    )

    fun shouldSkip(text: String): Boolean {
        if (skipPatterns.any { it.containsMatchIn(text) }) return true
        return !transactionKeywords.any { text.lowercase().contains(it) }
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
        "Shopping" to listOf("amazon", "flipkart", "myntra", "shopping"),
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
        if (isTransfer || transferKeywords.any { text.lowercase().contains(it) }) return "Transfers"
        merchant?.let { return it.category }

        val norm = text.lowercase()
        for ((category, hints) in categoryHints) {
            if (hints.any { it in norm }) return category
        }

        return when {
            channel == "UPI" && direction == "DEBIT" -> "Transfers"
            direction == "CREDIT" -> "Income"
            else -> "Uncategorized"
        }
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

        if (skipDetector.shouldSkip(text)) {
            Log.d("TransactionParser", "Skipped: OTP/non-transaction")
            return null
        }

        val norm = text.lowercase()
        val amount = amountExtractor.extract(text, norm) ?: run {
            Log.d("TransactionParser", "No amount found")
            return null
        }

        val balance = balanceExtractor.extract(text, norm)
        val accountSuffix = accountDetector.detect(text)
        val direction = directionDetector.detect(text)
        val channel = channelDetector.detect(text)
        val merchant = merchantMatcher.find(text)
        val accountId = accountSuffix?.let { accountDetector.matchToAccount(it, accounts) }

        val isTransfer = transferKeywords.any { norm.contains(it) }
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

    private val transferKeywords = listOf(
        "self transfer", "own account", "fund transfer", "imps to self", "neft to self"
    )
}