package com.hktech.personalexpensetracker.ingest

import com.hktech.personalexpensetracker.data.TransactionEntity

object TransactionParser {
    private val amtRegex = Regex("""(?:INR|Rs\.?|₹)\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]+)?|[0-9]+(?:\.[0-9]+)?)""", RegexOption.IGNORE_CASE)
    private val cardRegex = Regex("""(?:Debit|Credit)\s*Card\s*(?:xx|ending|no\.?)?\s*([0-9]{2,4})""", RegexOption.IGNORE_CASE)
    private val acctRegex = Regex("""A/c(?:ount)?\s*(?:xx|ending|no\.?)?\s*([0-9]{2,4})""", RegexOption.IGNORE_CASE)

    private val debitHints = listOf("debited","spent","used at","payment of","withdrawn","purchase","txn at","paid to","transfer to")
    private val creditHints = listOf("credited","received","refund","reversal","cashback","interest","salary")

    private val merchantMap = mapOf(
        "amazon" to "Shopping", "flipkart" to "Shopping", "myntra" to "Shopping",
        "swiggy" to "Food", "zomato" to "Food", "blinkit" to "Groceries", "bigbasket" to "Groceries",
        "uber" to "Transport", "ola" to "Transport", "irctc" to "Travel", "makemytrip" to "Travel",
        "airtel" to "Utilities", "jio" to "Utilities", "tata power" to "Utilities",
        "paytm" to "Wallet", "phonepe" to "UPI", "google pay" to "UPI"
    )

    fun parse(text: String, receivedAt: Long, source: String = "SMS"): TransactionEntity? {
        val norm = text.lowercase()
        val amount = amtRegex.find(text)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() ?: return null

        val direction = when {
            creditHints.any { norm.contains(it) } -> "CREDIT"
            debitHints.any  { norm.contains(it) } -> "DEBIT"
            else -> "DEBIT"
        }

        val channel = when {
            "upi" in norm || "@ok" in norm || "vpa" in norm -> "UPI"
            "debit card" in norm || "credit card" in norm || cardRegex.containsMatchIn(text) -> "CARD"
            "atm" in norm -> "ATM"
            listOf("netbanking","imps","neft","rtgs").any { it in norm } -> "NETBANKING"
            "wallet" in norm || "paytm wallet" in norm -> "WALLET"
            else -> "OTHER"
        }

        val acct = (cardRegex.find(text) ?: acctRegex.find(text))?.groupValues?.get(1)
        val merchantKey = merchantMap.keys.firstOrNull { it in norm }
        val merchant = merchantKey?.replaceFirstChar { it.uppercase() }
        val category = when {
            merchantKey != null -> merchantMap[merchantKey] ?: "Uncategorized"
            listOf("fuel","petrol","bharat petroleum").any { it in norm } -> "Fuel"
            listOf("electric","bill","dth","gas").any { it in norm } -> "Utilities"
            "rent" in norm -> "Rent"
            listOf("school","college","fee").any { it in norm } -> "Education"
            channel == "UPI" && direction == "DEBIT" -> "Transfers"
            direction == "CREDIT" -> "Income"
            else -> "Uncategorized"
        }

        return TransactionEntity(
            ts = receivedAt,
            source = source,
            channel = channel,
            direction = direction,
            merchant = merchant,
            amount = amount,
            accountHint = acct,
            rawText = text,
            category = category
        )
    }
}
