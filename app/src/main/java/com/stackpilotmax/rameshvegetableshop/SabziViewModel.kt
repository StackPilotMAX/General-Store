package com.stackpilotmax.rameshvegetableshop

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stackpilotmax.rameshvegetableshop.data.BillDraftItem
import com.stackpilotmax.rameshvegetableshop.data.BillRevisionEntity
import com.stackpilotmax.rameshvegetableshop.data.BillStatement
import com.stackpilotmax.rameshvegetableshop.data.BillStatus
import com.stackpilotmax.rameshvegetableshop.data.BillWithItems
import com.stackpilotmax.rameshvegetableshop.data.CustomerEntity
import com.stackpilotmax.rameshvegetableshop.data.LedgerRules
import com.stackpilotmax.rameshvegetableshop.data.PaymentEntity
import com.stackpilotmax.rameshvegetableshop.data.SabziRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class VegetableOption(
    val name: String,
    val unit: String,
    val emoji: String
)

data class EditLedgerContext(
    val billId: Long? = null,
    val originalCustomerId: Long? = null,
    val originalOutstanding: Double = 0.0
)

enum class AppScreen {
    SPLASH,
    HOME,
    NEW_BILL,
    CUSTOMERS,
    HISTORY,
    SETTINGS
}

class SabziViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SabziRepository(application)

    // General-store billing has no permanent item catalogue. Every item is entered per bill.

    val customers: StateFlow<List<CustomerEntity>> = repository.customers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val bills: StateFlow<List<BillWithItems>> = repository.bills
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val payments: StateFlow<List<PaymentEntity>> = repository.payments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _screen = MutableStateFlow(AppScreen.SPLASH)
    val screen = _screen.asStateFlow()
    private var backTarget = AppScreen.HOME

    private val _cart = MutableStateFlow<List<BillDraftItem>>(emptyList())
    val cart = _cart.asStateFlow()

    private val _editingBillId = MutableStateFlow<Long?>(null)
    val editingBillId = _editingBillId.asStateFlow()

    private val _editLedgerContext = MutableStateFlow(EditLedgerContext())

    private val _customerName = MutableStateFlow("")
    val customerName = _customerName.asStateFlow()

    private val _customerPhone = MutableStateFlow("")
    val customerPhone = _customerPhone.asStateFlow()

    private val _selectedCustomerId = MutableStateFlow<Long?>(null)
    val selectedCustomerId = _selectedCustomerId.asStateFlow()

    private val _amountPaid = MutableStateFlow("")
    val amountPaid = _amountPaid.asStateFlow()

    private val _balances = MutableStateFlow<Map<Long, Double>>(emptyMap())
    val balances = _balances.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    private val _vendorName = MutableStateFlow(repository.vendorName())
    val vendorName = _vendorName.asStateFlow()

    private val _upiId = MutableStateFlow(repository.upiId())
    val upiId = _upiId.asStateFlow()

    private val _motionEnabled = MutableStateFlow(repository.motionEnabled())
    val motionEnabled = _motionEnabled.asStateFlow()

    private val _activeBillCountVisible = MutableStateFlow(repository.activeBillCountVisible())
    val activeBillCountVisible = _activeBillCountVisible.asStateFlow()

    private val _shareStatement = MutableStateFlow<BillStatement?>(null)
    val shareStatement = _shareStatement.asStateFlow()

    private val _customerPayments = MutableStateFlow<List<PaymentEntity>>(emptyList())
    val customerPayments = _customerPayments.asStateFlow()

    private val _billRevisions = MutableStateFlow<List<BillRevisionEntity>>(emptyList())
    val billRevisions = _billRevisions.asStateFlow()

    val billTotal: StateFlow<Double> = _cart
        .combine(_amountPaid) { items, _ -> LedgerRules.money(items.sumOf { it.amount }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val selectedCustomer: StateFlow<CustomerEntity?> = combine(
        _selectedCustomerId,
        customers
    ) { selectedId, customerList ->
        customerList.firstOrNull { it.id == selectedId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val matchingCustomers: StateFlow<List<CustomerEntity>> = combine(
        _customerName,
        customers
    ) { name, customerList ->
        val normalized = LedgerRules.normalizeName(name)
        if (normalized.isBlank()) emptyList()
        else customerList.filter { it.normalizedName == normalized }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** This value can only come from the selected customer's permanent ID. */
    val currentCustomerBalance: StateFlow<Double> = combine(
        _selectedCustomerId,
        _balances
    ) { selectedId, balanceMap ->
        selectedId?.let { balanceMap[it] } ?: 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val projectedCustomerBalance: StateFlow<Double> = combine(
        currentCustomerBalance,
        billTotal,
        _amountPaid,
        _editLedgerContext,
        _selectedCustomerId
    ) { currentBalance, total, paidText, editContext, selectedId ->
        val paid = paidText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
        LedgerRules.projectedBalance(
            currentBalance = currentBalance,
            draftTotal = total,
            amountPaid = paid,
            replacedOutstanding = editContext.originalOutstanding,
            editingSameCustomer = editContext.billId != null &&
                editContext.originalCustomerId == selectedId
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    init {
        viewModelScope.launch {
            runCatching { repository.purgeExpiredBills() }
        }
        viewModelScope.launch {
            kotlinx.coroutines.delay(1_250)
            backTarget = AppScreen.HOME
            _screen.value = AppScreen.HOME
        }
        viewModelScope.launch {
            combine(customers, bills, payments) { customerList, _, _ -> customerList }
                .collect { refreshBalances(it) }
        }
    }

    private fun navigateTo(destination: AppScreen, returnTo: AppScreen = _screen.value) {
        if (_screen.value == destination) return
        backTarget = if (returnTo == AppScreen.SPLASH) AppScreen.HOME else returnTo
        _screen.value = destination
    }

    fun open(destination: AppScreen) {
        navigateTo(destination)
    }

    fun navigateBack() {
        val current = _screen.value
        if (current == AppScreen.SPLASH || current == AppScreen.HOME) return
        if (current == AppScreen.NEW_BILL) clearDraft()
        val destination = backTarget
        backTarget = AppScreen.HOME
        _screen.value = destination
    }

    fun backHome() {
        if (_screen.value == AppScreen.NEW_BILL) clearDraft()
        backTarget = AppScreen.HOME
        _screen.value = AppScreen.HOME
    }

    fun startNewBill() {
        clearDraft()
        navigateTo(AppScreen.NEW_BILL)
    }

    fun cancelBill() {
        clearDraft()
        val destination = backTarget
        backTarget = AppScreen.HOME
        _screen.value = destination
    }

    fun setCustomerName(value: String) {
        val previousSelectedId = _selectedCustomerId.value
        _customerName.value = value
        val normalized = LedgerRules.normalizeName(value)

        if (normalized.isBlank()) {
            _selectedCustomerId.value = null
            _customerPhone.value = ""
            return
        }

        val matches = customers.value.filter { it.normalizedName == normalized }
        if (matches.size == 1) {
            val candidate = matches.first()
            val typedPhone = LedgerRules.normalizePhone(_customerPhone.value)
            val phoneCompatible = typedPhone.isBlank() ||
                candidate.normalizedPhone.isBlank() ||
                candidate.normalizedPhone == typedPhone
            if (phoneCompatible) {
                _selectedCustomerId.value = candidate.id
                if (previousSelectedId != candidate.id) {
                    _customerPhone.value = candidate.phone
                }
                return
            }
        }

        _selectedCustomerId.value = null
        if (previousSelectedId != null) {
            _customerPhone.value = ""
        }
    }

    fun setCustomerPhone(value: String) {
        _customerPhone.value = value
        val normalizedPhone = LedgerRules.normalizePhone(value)
        if (normalizedPhone.isBlank()) return

        val candidate = customers.value.singleOrNull { it.normalizedPhone == normalizedPhone }
        if (candidate != null) {
            val typedName = LedgerRules.normalizeName(_customerName.value)
            if (typedName.isBlank() || typedName == candidate.normalizedName) {
                _selectedCustomerId.value = candidate.id
                _customerName.value = candidate.name
            }
        }
    }

    fun setAmountPaid(value: String) {
        _amountPaid.value = value.filter { it.isDigit() || it == '.' }
    }

    fun selectCustomer(customer: CustomerEntity) {
        _selectedCustomerId.value = customer.id
        _customerName.value = customer.name
        _customerPhone.value = customer.phone
        _message.value = "${customer.name} ka alag khata select hua"
    }

    fun addOrUpdateItem(
        itemName: String,
        unit: String,
        quantity: Double,
        rate: Double,
        directAmount: Double? = null
    ) {
        runCatching {
            val replacement = BillDraftItem(
                vegetableName = itemName.trim(),
                unit = unit.trim(),
                quantity = quantity,
                rate = rate,
                directAmount = directAmount
            )
            replacement.amount
        }.onSuccess {
            val cleanName = itemName.trim()
            val replacement = BillDraftItem(
                vegetableName = cleanName,
                unit = unit.trim(),
                quantity = quantity,
                rate = rate,
                directAmount = directAmount
            )
            _cart.value = _cart.value
                .filterNot { it.vegetableName.equals(cleanName, ignoreCase = true) }
                .plus(replacement)
            _message.value = cleanName + " bill mein add ho gaya"
        }.onFailure {
            _message.value = it.message ?: "Item quantity aur price sahi daaliye"
        }
    }

    fun removeItem(name: String) {
        _cart.value = _cart.value.filterNot { it.vegetableName == name }
    }

    fun saveBill() {
        val displayName = _customerName.value.trim()
        val wasEditing = _editingBillId.value != null
        viewModelScope.launch {
            runCatching {
                repository.saveBill(
                    editingBillId = _editingBillId.value,
                    selectedCustomerId = _selectedCustomerId.value,
                    customerName = _customerName.value,
                    customerPhone = _customerPhone.value,
                    amountPaid = _amountPaid.value.toDoubleOrNull() ?: 0.0,
                    items = _cart.value
                )
            }.onSuccess {
                _message.value = if (wasEditing) {
                    "Bill update hua; purana charge replace hua, duplicate nahi"
                } else {
                    "$displayName ka bill sirf uske apne khate mein save hua"
                }
                clearDraft()
                backTarget = AppScreen.HOME
                _screen.value = AppScreen.HISTORY
            }.onFailure {
                _message.value = it.message ?: "Bill save nahi hua"
            }
        }
    }

    fun editBill(billId: Long) {
        viewModelScope.launch {
            val saved = repository.loadBill(billId) ?: run {
                _message.value = "Bill nahi mila"
                return@launch
            }
            if (saved.bill.status != BillStatus.ACTIVE) {
                _message.value = "Void bill edit nahi ho sakta"
                return@launch
            }
            _editingBillId.value = saved.bill.id
            _editLedgerContext.value = EditLedgerContext(
                billId = saved.bill.id,
                originalCustomerId = saved.bill.customerId,
                originalOutstanding = LedgerRules.billOutstanding(
                    saved.bill.total,
                    saved.bill.amountPaid
                )
            )
            _selectedCustomerId.value = saved.bill.customerId
            _customerName.value = saved.bill.customerName
            _customerPhone.value = saved.bill.customerPhone
            _amountPaid.value = LedgerRules.moneyText(saved.bill.amountPaid)
            _cart.value = saved.items.map {
                BillDraftItem(
                    vegetableName = it.vegetableName,
                    unit = it.unit,
                    quantity = it.quantity,
                    rate = it.rate,
                    directAmount = if (it.isDirectAmount) it.amount else null
                )
            }
            backTarget = AppScreen.HISTORY
            _screen.value = AppScreen.NEW_BILL
        }
    }

    fun voidBill(billId: Long) {
        viewModelScope.launch {
            runCatching { repository.voidBill(billId) }
                .onSuccess { _message.value = "Bill void hua aur uska debt charge reverse ho gaya" }
                .onFailure { _message.value = it.message ?: "Bill void nahi hua" }
        }
    }

    fun deleteBill(billId: Long) {
        viewModelScope.launch {
            runCatching { repository.deleteBill(billId) }
                .onSuccess {
                    if (_editingBillId.value == billId) clearDraft()
                    _billRevisions.value = emptyList()
                    _message.value = "Bill permanently delete hua; customer balance recalculate ho gaya"
                }
                .onFailure { _message.value = it.message ?: "Bill delete nahi hua" }
        }
    }

    fun createCustomer(name: String, phone: String, openingDebt: Double) {
        viewModelScope.launch {
            runCatching { repository.createCustomer(name, phone, openingDebt) }
                .onSuccess { _message.value = "$name ka customer account ban gaya" }
                .onFailure { _message.value = it.message ?: "Customer save nahi hua" }
        }
    }

    fun updateCustomer(customerId: Long, name: String, phone: String, openingDebt: Double) {
        viewModelScope.launch {
            runCatching { repository.updateCustomer(customerId, name, phone, openingDebt) }
                .onSuccess { _message.value = "Customer details update ho gayi" }
                .onFailure { _message.value = it.message ?: "Customer update nahi hua" }
        }
    }

    fun deleteCustomer(customerId: Long) {
        viewModelScope.launch {
            runCatching { repository.deleteCustomer(customerId) }
                .onSuccess {
                    if (_selectedCustomerId.value == customerId) {
                        _selectedCustomerId.value = null
                        _customerName.value = ""
                        _customerPhone.value = ""
                    }
                    _customerPayments.value = emptyList()
                    _message.value = "Customer aur payment history delete hui; purane bills Archive mein safe hain"
                }
                .onFailure { _message.value = it.message ?: "Customer delete nahi hua" }
        }
    }

    fun receivePayment(customerId: Long, amount: Double, note: String = "Payment received") {
        viewModelScope.launch {
            runCatching { repository.receivePayment(customerId, amount, note) }
                .onSuccess {
                    _message.value = "Payment save ho gaya"
                    loadCustomerPayments(customerId)
                }
                .onFailure { _message.value = it.message ?: "Payment save nahi hua" }
        }
    }

    fun loadCustomerPayments(customerId: Long) {
        viewModelScope.launch {
            _customerPayments.value = repository.customerPayments(customerId)
        }
    }

    fun reversePayment(paymentId: Long, customerId: Long) {
        viewModelScope.launch {
            runCatching { repository.reversePayment(paymentId) }
                .onSuccess {
                    _message.value = "Payment reverse hui; customer baki wapas update hua"
                    loadCustomerPayments(customerId)
                }
                .onFailure { _message.value = it.message ?: "Payment reverse nahi hui" }
        }
    }

    fun loadBillRevisions(billId: Long) {
        viewModelScope.launch {
            _billRevisions.value = repository.billRevisions(billId)
        }
    }

    fun prepareShare(billId: Long) {
        viewModelScope.launch {
            _shareStatement.value = repository.billStatement(billId)
            if (_shareStatement.value == null) _message.value = "Bill share ke liye nahi mila"
        }
    }

    fun consumeShareStatement() {
        _shareStatement.value = null
    }

    fun saveSettings(vendor: String, upi: String, motion: Boolean) {
        repository.saveSettings(vendor, upi, motion)
        _vendorName.value = vendor.trim().ifBlank { "Ramesh Vegetable Shop" }
        _upiId.value = upi.trim()
        _motionEnabled.value = motion
        _message.value = "Settings save ho gayi"
    }

    fun setActiveBillCountVisible(visible: Boolean) {
        repository.saveActiveBillCountVisible(visible)
        _activeBillCountVisible.value = visible
        _message.value = if (visible) "Active bill count dikh raha hai" else "Active bill count hide kar diya"
    }

    fun clearMessage() {
        _message.value = null
    }

    private fun clearDraft() {
        _editingBillId.value = null
        _editLedgerContext.value = EditLedgerContext()
        _cart.value = emptyList()
        _customerName.value = ""
        _customerPhone.value = ""
        _selectedCustomerId.value = null
        _amountPaid.value = ""
    }

    private suspend fun refreshBalances(customerList: List<CustomerEntity>) {
        _balances.value = customerList.associate { customer ->
            customer.id to repository.customerBalance(customer.id)
        }
    }
}
