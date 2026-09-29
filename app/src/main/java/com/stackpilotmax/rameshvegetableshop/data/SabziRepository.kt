package com.stackpilotmax.rameshvegetableshop.data

import android.content.Context
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

data class BillDraftItem(
    val vegetableName: String,
    val unit: String,
    val quantity: Double,
    val rate: Double,
    val directAmount: Double? = null
) {
    val isDirectAmount: Boolean get() = directAmount != null
    val amount: Double get() = LedgerRules.itemAmount(quantity, rate, directAmount)
}

data class CustomerBalance(
    val customer: CustomerEntity,
    val balance: Double
)

data class BillStatement(
    val saved: BillWithItems,
    val previousDebt: Double,
    val currentBill: Double,
    val amountPaid: Double,
    val totalOutstandingAtBill: Double,
    val isVoided: Boolean
)

class SabziRepository(context: Context) {
    private val db = AppDatabase.get(context)
    private val customerDao = db.customerDao()
    private val billDao = db.billDao()
    private val paymentDao = db.paymentDao()
    private val revisionDao = db.billRevisionDao()
    private val prefs = context.getSharedPreferences("sabzibill_settings", Context.MODE_PRIVATE)

    val customers: Flow<List<CustomerEntity>> = customerDao.observeAll()
    val bills: Flow<List<BillWithItems>> = billDao.observeAllWithItems()
    val payments: Flow<List<PaymentEntity>> = paymentDao.observeAll()

    suspend fun createCustomer(name: String, phone: String, openingDebt: Double): Long = db.withTransaction {
        val cleanName = name.trim().replace(Regex("\\s+"), " ")
        val cleanPhone = phone.trim()
        val normalizedName = LedgerRules.normalizeName(cleanName)
        val normalizedPhone = LedgerRules.normalizePhone(cleanPhone)
        require(normalizedName.isNotBlank()) { "Customer name zaroori hai" }
        require(openingDebt.isFinite() && openingDebt >= 0) { "Opening debt sahi daaliye" }

        if (normalizedPhone.isNotBlank()) {
            val phoneMatches = customerDao.getByNormalizedPhone(normalizedPhone)
            require(phoneMatches.isEmpty()) {
                "Ye phone ${phoneMatches.first().name} ke customer account mein pehle se hai"
            }
        } else {
            val namelessPhoneMatches = customerDao.getByNormalizedName(normalizedName)
                .filter { it.normalizedPhone.isBlank() }
            require(namelessPhoneMatches.isEmpty()) {
                "Is naam ka customer pehle se hai; list se select kijiye"
            }
        }

        customerDao.insert(
            CustomerEntity(
                name = cleanName,
                phone = cleanPhone,
                openingDebt = LedgerRules.money(openingDebt),
                normalizedName = normalizedName,
                normalizedPhone = normalizedPhone
            )
        )
    }

    suspend fun updateCustomer(
        customerId: Long,
        name: String,
        phone: String,
        openingDebt: Double
    ) = db.withTransaction {
        val existing = customerDao.getById(customerId) ?: error("Customer nahi mila")
        val cleanName = name.trim().replace(Regex("\\s+"), " ")
        val cleanPhone = phone.trim()
        val normalizedName = LedgerRules.normalizeName(cleanName)
        val normalizedPhone = LedgerRules.normalizePhone(cleanPhone)
        require(normalizedName.isNotBlank()) { "Customer name zaroori hai" }
        require(openingDebt.isFinite() && openingDebt >= 0) { "Opening debt sahi daaliye" }

        if (normalizedPhone.isNotBlank()) {
            val collision = customerDao.getByNormalizedPhone(normalizedPhone)
                .firstOrNull { it.id != customerId }
            require(collision == null) {
                "Ye phone ${collision?.name} ke account mein pehle se hai"
            }
        }

        customerDao.update(
            existing.copy(
                name = cleanName,
                phone = cleanPhone,
                openingDebt = LedgerRules.money(openingDebt),
                normalizedName = normalizedName,
                normalizedPhone = normalizedPhone
            )
        )
    }

    suspend fun deleteCustomer(customerId: Long) = db.withTransaction {
        customerDao.getById(customerId) ?: error("Customer nahi mila")
        check(customerDao.deleteById(customerId) == 1) {
            "Customer delete nahi hua"
        }
    }

    /**
     * Resolves one customer account without ever borrowing another customer's ledger.
     * A selected ID is trusted only when the typed name still belongs to that account.
     */
    private suspend fun resolveCustomerForBill(
        selectedCustomerId: Long?,
        customerName: String,
        customerPhone: String
    ): Long {
        val cleanName = customerName.trim().replace(Regex("\\s+"), " ")
        val cleanPhone = customerPhone.trim()
        val normalizedName = LedgerRules.normalizeName(cleanName)
        val normalizedPhone = LedgerRules.normalizePhone(cleanPhone)
        require(normalizedName.isNotBlank()) { "Customer name zaroori hai" }

        val selected = selectedCustomerId?.let { customerDao.getById(it) }
        if (selected != null && selected.normalizedName == normalizedName) {
            val phoneStillMatches = normalizedPhone.isBlank() ||
                selected.normalizedPhone.isBlank() ||
                selected.normalizedPhone == normalizedPhone
            if (phoneStillMatches) {
                if (cleanPhone.isNotBlank() && selected.normalizedPhone.isBlank()) {
                    val collision = customerDao.getByNormalizedPhone(normalizedPhone)
                        .firstOrNull { it.id != selected.id }
                    require(collision == null) {
                        "Ye phone ${collision?.name} ke account mein pehle se hai"
                    }
                    customerDao.update(
                        selected.copy(
                            phone = cleanPhone,
                            normalizedPhone = normalizedPhone
                        )
                    )
                }
                return selected.id
            }
        }

        if (normalizedPhone.isNotBlank()) {
            val byPhone = customerDao.getByNormalizedPhone(normalizedPhone)
            if (byPhone.size == 1) {
                val customer = byPhone.first()
                require(customer.normalizedName == normalizedName) {
                    "Ye phone ${customer.name} ka hai; customer list se sahi account select kijiye"
                }
                return customer.id
            }
            require(byPhone.size <= 1) {
                "Same phone ke multiple accounts mile; Customer Khata mein details sahi kijiye"
            }
        }

        val byName = customerDao.getByNormalizedName(normalizedName)
        if (byName.size == 1) {
            val customer = byName.first()
            if (
                normalizedPhone.isBlank() ||
                customer.normalizedPhone.isBlank() ||
                customer.normalizedPhone == normalizedPhone
            ) {
                if (normalizedPhone.isNotBlank() && customer.normalizedPhone.isBlank()) {
                    customerDao.update(
                        customer.copy(phone = cleanPhone, normalizedPhone = normalizedPhone)
                    )
                }
                return customer.id
            }
            // Same display name with a genuinely different phone is a separate customer.
        } else if (byName.size > 1 && normalizedPhone.isBlank()) {
            error("$cleanName naam ke multiple customers hain; neeche list se sahi customer select kijiye")
        }

        return customerDao.insert(
            CustomerEntity(
                name = cleanName,
                phone = cleanPhone,
                openingDebt = 0.0,
                normalizedName = normalizedName,
                normalizedPhone = normalizedPhone
            )
        )
    }

    suspend fun saveBill(
        editingBillId: Long?,
        selectedCustomerId: Long?,
        customerName: String,
        customerPhone: String,
        amountPaid: Double,
        items: List<BillDraftItem>
    ): Long = db.withTransaction {
        require(items.isNotEmpty()) { "Kam se kam ek sabzi add kijiye" }
        items.forEach { it.amount }

        val cleanName = customerName.trim().replace(Regex("\\s+"), " ")
        val cleanPhone = customerPhone.trim()
        val customerId = resolveCustomerForBill(selectedCustomerId, cleanName, cleanPhone)
        val total = LedgerRules.money(items.sumOf { it.amount })
        val paid = LedgerRules.money(amountPaid)
        require(paid >= 0) { "Paid amount negative nahi ho sakta" }
        val now = System.currentTimeMillis()

        val billId: Long
        val previousDebt: Double

        if (editingBillId == null) {
            previousDebt = customerBalance(customerId)
            LedgerRules.validateBillPayment(previousDebt, total, paid)
            billId = billDao.insertBill(
                BillEntity(
                    customerId = customerId,
                    customerName = cleanName,
                    customerPhone = cleanPhone,
                    total = total,
                    amountPaid = paid,
                    createdAt = now,
                    updatedAt = now,
                    previousDebtSnapshot = previousDebt
                )
            )
        } else {
            val old = billDao.getWithItems(editingBillId)
                ?: error("Bill nahi mila")
            require(old.bill.status == BillStatus.ACTIVE) { "Void bill edit nahi ho sakta" }

            saveRevision(old, reason = "EDITED")
            val sameCustomer = old.bill.customerId == customerId
            previousDebt = if (sameCustomer) {
                old.bill.previousDebtSnapshot
            } else {
                customerBalance(customerId)
            }
            LedgerRules.validateBillPayment(previousDebt, total, paid)

            paymentDao.getActiveForBill(editingBillId).forEach { payment ->
                check(paymentDao.reverse(payment.id, now) == 1) {
                    "Purani bill payment reverse nahi hui"
                }
            }

            billDao.updateBill(
                old.bill.copy(
                    customerId = customerId,
                    customerName = cleanName,
                    customerPhone = cleanPhone,
                    total = total,
                    amountPaid = paid,
                    updatedAt = now,
                    previousDebtSnapshot = previousDebt
                )
            )
            billDao.deleteItemsForBill(editingBillId)
            billId = editingBillId
        }

        billDao.insertItems(
            items.map {
                BillItemEntity(
                    billId = billId,
                    vegetableName = it.vegetableName,
                    unit = it.unit,
                    quantity = it.quantity,
                    rate = LedgerRules.money(it.rate),
                    amount = it.amount,
                    isDirectAmount = it.isDirectAmount
                )
            }
        )

        val extraPayment = LedgerRules.extraDebtPayment(total, paid)
        if (extraPayment > 0.0) {
            paymentDao.insert(
                PaymentEntity(
                    customerId = customerId,
                    amount = extraPayment,
                    note = "Payment received with bill #$billId",
                    createdAt = now,
                    sourceBillId = billId
                )
            )
        }
        billId
    }

    private suspend fun saveRevision(saved: BillWithItems, reason: String) {
        val number = revisionDao.countForBill(saved.bill.id) + 1
        val snapshot = saved.items.joinToString(separator = "\n") {
            listOf(
                it.vegetableName,
                it.unit,
                it.quantity.toString(),
                it.rate.toString(),
                it.amount.toString(),
                it.isDirectAmount.toString()
            ).joinToString("|")
        }
        revisionDao.insert(
            BillRevisionEntity(
                billId = saved.bill.id,
                revisionNumber = number,
                customerId = saved.bill.customerId,
                customerName = saved.bill.customerName,
                customerPhone = saved.bill.customerPhone,
                total = saved.bill.total,
                amountPaid = saved.bill.amountPaid,
                previousDebtSnapshot = saved.bill.previousDebtSnapshot,
                itemsSnapshot = snapshot,
                reason = reason
            )
        )
    }

    suspend fun loadBill(billId: Long): BillWithItems? = billDao.getWithItems(billId)

    suspend fun billStatement(billId: Long): BillStatement? {
        val saved = billDao.getWithItems(billId) ?: return null
        val outstandingAtBill = if (saved.bill.status == BillStatus.ACTIVE) {
            LedgerRules.money(
                saved.bill.previousDebtSnapshot +
                    LedgerRules.billOutstanding(saved.bill.total, saved.bill.amountPaid) -
                    LedgerRules.extraDebtPayment(saved.bill.total, saved.bill.amountPaid)
            )
        } else {
            saved.bill.previousDebtSnapshot
        }
        return BillStatement(
            saved = saved,
            previousDebt = saved.bill.previousDebtSnapshot,
            currentBill = saved.bill.total,
            amountPaid = saved.bill.amountPaid,
            totalOutstandingAtBill = outstandingAtBill,
            isVoided = saved.bill.status == BillStatus.VOIDED
        )
    }

    suspend fun billRevisions(billId: Long): List<BillRevisionEntity> =
        revisionDao.getForBill(billId)

    suspend fun voidBill(billId: Long) = db.withTransaction {
        val saved = billDao.getWithItems(billId) ?: error("Bill nahi mila")
        require(saved.bill.status == BillStatus.ACTIVE) { "Bill pehle se void hai" }
        saveRevision(saved, reason = "VOIDED")
        paymentDao.getActiveForBill(billId).forEach { payment ->
            check(paymentDao.reverse(payment.id, System.currentTimeMillis()) == 1) {
                "Bill ki extra payment reverse nahi hui"
            }
        }
        check(billDao.voidBill(billId, System.currentTimeMillis()) == 1) {
            "Bill void nahi hua"
        }
    }

    suspend fun deleteBill(billId: Long) = db.withTransaction {
        billDao.getWithItems(billId) ?: error("Bill nahi mila")
        paymentDao.getActiveForBill(billId).forEach { payment ->
            check(paymentDao.reverse(payment.id, System.currentTimeMillis()) == 1) {
                "Bill ki extra payment reverse nahi hui"
            }
        }
        check(billDao.deleteById(billId) == 1) {
            "Bill delete nahi hua"
        }
    }

    /**
     * Permanently removes bill history older than two calendar months.
     * Unpaid portions are rolled into customer opening debt first, so deleting
     * the historical row never silently erases money still owed.
     */
    suspend fun purgeExpiredBills(now: Long = System.currentTimeMillis()) = db.withTransaction {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = now
        calendar.add(Calendar.MONTH, -2)
        val cutoff = calendar.timeInMillis

        billDao.getBefore(cutoff).forEach { bill ->
            if (bill.status == BillStatus.ACTIVE && bill.customerId != null) {
                val outstanding = LedgerRules.billOutstanding(bill.total, bill.amountPaid)
                if (outstanding > 0.0) {
                    customerDao.addToOpeningDebt(bill.customerId, outstanding)
                }
            }
            billDao.deleteById(bill.id)
        }
    }

    suspend fun customerBalance(customerId: Long): Double {
        val customer = customerDao.getById(customerId) ?: return 0.0
        val billOutstanding = billDao.outstandingBills(customerId)
        val payments = paymentDao.totalPaid(customerId)
        return LedgerRules.money(customer.openingDebt + billOutstanding - payments)
    }

    suspend fun receivePayment(customerId: Long, amount: Double, note: String = "Payment received"): Long =
        db.withTransaction {
            val customer = customerDao.getById(customerId) ?: error("Customer nahi mila")
            val balance = customerBalance(customerId)
            val cleanAmount = LedgerRules.money(amount)
            LedgerRules.requirePaymentAllowed(balance, cleanAmount)
            paymentDao.insert(
                PaymentEntity(
                    customerId = customer.id,
                    amount = cleanAmount,
                    note = note.trim().ifBlank { "Payment received" }
                )
            )
        }

    suspend fun reversePayment(paymentId: Long) = db.withTransaction {
        val payment = paymentDao.getById(paymentId) ?: error("Payment nahi mili")
        require(!payment.isReversed) { "Payment pehle se reverse hai" }
        check(paymentDao.reverse(paymentId, System.currentTimeMillis()) == 1) {
            "Payment reverse nahi hui"
        }
    }

    suspend fun customerPayments(customerId: Long): List<PaymentEntity> =
        paymentDao.getForCustomer(customerId)

    fun vendorName(): String =
        prefs.getString("vendor_name", "Ramesh Vegetable Shop") ?: "Ramesh Vegetable Shop"

    fun upiId(): String = prefs.getString("upi_id", "") ?: ""

    fun motionEnabled(): Boolean = prefs.getBoolean("motion_enabled", true)

    fun activeBillCountVisible(): Boolean = prefs.getBoolean("active_bill_count_visible", true)

    fun saveActiveBillCountVisible(visible: Boolean) {
        prefs.edit().putBoolean("active_bill_count_visible", visible).apply()
    }

    fun saveSettings(vendorName: String, upiId: String, motionEnabled: Boolean) {
        prefs.edit()
            .putString("vendor_name", vendorName.trim().ifBlank { "Ramesh Vegetable Shop" })
            .putString("upi_id", upiId.trim())
            .putBoolean("motion_enabled", motionEnabled)
            .apply()
    }
}
