package com.stackpilotmax.rameshvegetableshop.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class LedgerRulesTest {
    @Test
    fun `Raju debt and bill produce only Raju total`() {
        val rajuAfterBill = LedgerRules.projectedBalance(
            currentBalance = 500.0,
            draftTotal = 250.0,
            amountPaid = 0.0,
            replacedOutstanding = 0.0,
            editingSameCustomer = false
        )
        assertEquals(750.0, rajuAfterBill, 0.001)
    }

    @Test
    fun `new Rajesh starts from zero instead of Raju debt`() {
        val rajeshAfterBill = LedgerRules.projectedBalance(
            currentBalance = 0.0,
            draftTotal = 120.0,
            amountPaid = 0.0,
            replacedOutstanding = 0.0,
            editingSameCustomer = false
        )
        assertEquals(120.0, rajeshAfterBill, 0.001)
    }

    @Test
    fun `editing same bill replaces old outstanding`() {
        val corrected = LedgerRules.projectedBalance(
            currentBalance = 750.0,
            draftTotal = 300.0,
            amountPaid = 0.0,
            replacedOutstanding = 250.0,
            editingSameCustomer = true
        )
        assertEquals(800.0, corrected, 0.001)
    }

    @Test
    fun `editing and moving bill to another customer does not subtract old charge there`() {
        val newCustomerBalance = LedgerRules.projectedBalance(
            currentBalance = 100.0,
            draftTotal = 300.0,
            amountPaid = 0.0,
            replacedOutstanding = 250.0,
            editingSameCustomer = false
        )
        assertEquals(400.0, newCustomerBalance, 0.001)
    }

    @Test
    fun `payment cannot exceed customer balance`() {
        assertThrows(IllegalArgumentException::class.java) {
            LedgerRules.requirePaymentAllowed(balance = 500.0, amount = 501.0)
        }
    }

    @Test
    fun `direct item total overrides multiplication`() {
        assertEquals(
            95.0,
            LedgerRules.itemAmount(quantity = 2.0, rate = 40.0, directAmount = 95.0),
            0.001
        )
    }

    @Test
    fun `customer names are normalized without merging different names`() {
        assertEquals("raju", LedgerRules.normalizeName("  RAJU  "))
        assertEquals("rajesh", LedgerRules.normalizeName("Rajesh"))
    }

    @Test
    fun `customer can pay less than existing debt when buying vegetables`() {
        LedgerRules.validateBillPayment(previousDebt = 2000.0, billTotal = 400.0, amountPaid = 1400.0)
        assertEquals(1000.0, LedgerRules.extraDebtPayment(400.0, 1400.0), 0.001)
        assertEquals(0.0, LedgerRules.billOutstanding(400.0, 1400.0), 0.001)
    }

    @Test
    fun `customer can clear all debt and bill together`() {
        LedgerRules.validateBillPayment(previousDebt = 2000.0, billTotal = 400.0, amountPaid = 2400.0)
        assertEquals(2000.0, LedgerRules.extraDebtPayment(400.0, 2400.0), 0.001)
    }

    @Test
    fun `payment above customer total debt is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            LedgerRules.validateBillPayment(previousDebt = 2000.0, billTotal = 400.0, amountPaid = 2400.01)
        }
    }

    @Test
    fun `projected balance uses extra payment against older debt`() {
        val projected = LedgerRules.projectedBalance(
            currentBalance = 2000.0,
            draftTotal = 400.0,
            amountPaid = 1400.0,
            replacedOutstanding = 0.0,
            editingSameCustomer = false
        )
        assertEquals(1000.0, projected, 0.001)
    }
}
