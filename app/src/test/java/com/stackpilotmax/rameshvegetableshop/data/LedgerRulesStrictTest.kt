package com.stackpilotmax.rameshvegetableshop.data

import org.junit.Assert.assertEquals
import org.junit.Test

class LedgerRulesStrictTest {
    @Test
    fun oldDebtPlusNewBillEqualsExpectedOutstanding() {
        assertEquals(5420.0, LedgerRules.projectedBalance(5000.0, 420.0, 0.0, 0.0, false), 0.001)
    }

    @Test
    fun paymentIsSubtractedOnceAndNeverAddedToDebt() {
        assertEquals(5220.0, LedgerRules.projectedBalance(5000.0, 420.0, 200.0, 0.0, false), 0.001)
    }

    @Test
    fun editingReplacesOldBillContribution() {
        assertEquals(5420.0, LedgerRules.projectedBalance(5420.0, 420.0, 0.0, 420.0, true), 0.001)
    }

    @Test
    fun itemAmountUsesOneSourceOfTruth() {
        assertEquals(420.0, LedgerRules.itemAmount(2.0, 210.0, null), 0.001)
        assertEquals(420.0, LedgerRules.itemAmount(1.0, 999.0, 420.0), 0.001)
    }
}
