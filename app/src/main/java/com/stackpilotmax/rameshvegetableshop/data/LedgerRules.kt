package com.stackpilotmax.rameshvegetableshop.data

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

object LedgerRules {
    private const val EPSILON = 0.005

    fun normalizeName(value: String): String = value
        .trim()
        .lowercase(Locale.ROOT)
        .replace(Regex("\\s+"), " ")

    fun normalizePhone(value: String): String = value.filter(Char::isDigit)

    fun money(value: Double): Double {
        require(value.isFinite()) { "Amount valid number hona chahiye" }
        return BigDecimal.valueOf(value)
            .setScale(2, RoundingMode.HALF_UP)
            .toDouble()
    }

    fun itemAmount(quantity: Double, rate: Double, directAmount: Double?): Double {
        require(quantity > 0 && quantity.isFinite()) { "Quantity zero se badi honi chahiye" }
        require(rate >= 0 && rate.isFinite()) { "Rate sahi daaliye" }
        directAmount?.let {
            require(it >= 0 && it.isFinite()) { "Direct total sahi daaliye" }
        }
        return money(directAmount ?: quantity * rate)
    }

    /** A bill can never create negative outstanding debt. */
    fun billOutstanding(total: Double, amountPaid: Double): Double =
        money((total - amountPaid).coerceAtLeast(0.0))

    /**
     * amountPaid is cash received at this counter. It first settles today's bill;
     * only the remainder can reduce the customer's older outstanding balance.
     */
    fun validateBillPayment(previousDebt: Double, billTotal: Double, amountPaid: Double) {
        require(previousDebt.isFinite() && previousDebt >= -EPSILON) {
            "Customer ka baki amount valid nahi hai"
        }
        require(billTotal.isFinite() && billTotal >= 0) {
            "Bill total valid nahi hai"
        }
        require(amountPaid.isFinite() && amountPaid >= 0) {
            "Paid amount negative nahi ho sakta"
        }
        val maximum = money(previousDebt.coerceAtLeast(0.0) + billTotal)
        require(amountPaid <= maximum + EPSILON) {
            "Paid amount ₹${money(amountPaid)} customer ke total baki ₹$maximum se zyada nahi ho sakta"
        }
    }

    fun extraDebtPayment(billTotal: Double, amountPaid: Double): Double =
        money((amountPaid - billTotal).coerceAtLeast(0.0))

    /**
     * Strict ledger equation:
     * previous balance + current bill - actual payment.
     * A payment is never added to debt and today's bill is never counted twice.
     */
    fun projectedBalance(
        currentBalance: Double,
        draftTotal: Double,
        amountPaid: Double,
        replacedOutstanding: Double,
        editingSameCustomer: Boolean
    ): Double {
        val oldContribution = if (editingSameCustomer) replacedOutstanding else 0.0
        val billPaid = amountPaid.coerceAtLeast(0.0)
        return money(
            (currentBalance - oldContribution + draftTotal - billPaid).coerceAtLeast(0.0)
        )
    }

    fun requirePaymentAllowed(balance: Double, amount: Double) {
        require(amount.isFinite() && amount > 0) { "Payment zero se badi honi chahiye" }
        require(amount <= balance + EPSILON) {
            "Payment ₹${money(amount)} baki ₹${money(balance)} se zyada nahi ho sakti"
        }
    }

    fun moneyText(value: Double): String {
        val rounded = money(value)
        return if (rounded % 1.0 == 0.0) {
            rounded.toLong().toString()
        } else {
            String.format(Locale.US, "%.2f", rounded)
        }
    }
}
