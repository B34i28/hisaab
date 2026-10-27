package com.example.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

enum class AppLanguage(val code: String, val title: String, val nativeTitle: String) {
    ENGLISH("EN", "English", "English"),
    URDU("UR", "Urdu", "اردو");

    val displayName: String get() = title

    companion object {
        fun fromCode(code: String?): AppLanguage {
            return when (code?.uppercase()) {
                "UR" -> URDU
                else -> ENGLISH
            }
        }
    }
}

enum class ThemeMode(val code: String, val displayName: String, val urduDisplayName: String) {
    SYSTEM("SYSTEM", "System Default", "سسٹم کا ڈیفالٹ"),
    LIGHT("LIGHT", "Light", "لائٹ موڈ"),
    DARK("DARK", "Dark", "ڈارک موڈ");

    companion object {
        fun fromCode(code: String?): ThemeMode {
            return when (code?.uppercase()) {
                "LIGHT" -> LIGHT
                "DARK" -> DARK
                else -> SYSTEM
            }
        }
    }
}

data class AppStrings(
    // Bottom navigation
    val navDashboard: String = "Dashboard",
    val navCustomers: String = "Customers",
    val navSales: String = "Sales",
    val navExpenses: String = "Expenses",
    val navSuppliers: String = "Suppliers",
    val navReports: String = "Reports",

    // Customer & Udhaar strings
    val customersTitle: String = "Customer Credit (Udhaar)",
    val customerLedgerTitle: String = "Customer Ledger",
    val totalReceivable: String = "Total Receivable",
    val customersOweYou: String = "Customers owe you",
    val youWillGet: String = "You will get",
    val youWillGive: String = "You will give",
    val cashReceived: String = "Cash Received",
    val netCash: String = "Net Cash",
    val badDebt: String = "Bad Debt",
    val badDebtWriteOff: String = "Bad Debt (Write-offs)",
    val addCustomer: String = "Add Customer",
    val editCustomer: String = "Edit Customer",
    val customerName: String = "Customer Name *",
    val customerPhone: String = "Phone Number",
    val customerNote: String = "Note / Address",
    val creditLimit: String = "Credit Limit (PKR, Optional)",
    val creditLimitLabel: String = "Credit Limit",
    val owingOnly: String = "Owing only",
    val allCustomers: String = "All",
    val totalCreditGiven: String = "Total Credit Given",
    val totalReceivedBack: String = "Total Received",
    val totalWriteOffs: String = "Write-Offs",
    val currentBalance: String = "Current Balance",
    val receivePayment: String = "Receive Payment",
    val writeOff: String = "Write Off",
    val sendReminder: String = "Send Reminder",
    val selectCustomer: String = "Customer (Optional)",
    val noCustomerCashOnly: String = "No customer (Cash only)",
    val newCustomerQuickAdd: String = "+ New Customer",
    val amountReceivedNow: String = "Amount Received Now (PKR) *",
    val remainingUdhaar: String = "Remaining Udhaar (Credit)",
    val creditLimitWarningTitle: String = "Credit Limit Exceeded",
    val creditLimitWarningMessage: String = "This sale will exceed the customer's credit limit. Do you want to proceed anyway?",
    val writeOffConfirmTitle: String = "Confirm Write-Off",
    val writeOffConfirmMessage: String = "Are you sure you want to write off this balance as bad debt? This will reduce your net profit.",
    val writeOffNoteRequired: String = "Note / Reason is required for write-off.",
    val amountExceedsBalance: String = "Amount cannot exceed current outstanding balance.",
    val receivedCannotExceedTotal: String = "Received amount cannot exceed total sale amount.",
    val noCreditWithoutCustomer: String = "Credit sales require selecting a customer.",
    val noCustomersYet: String = "No customers added yet. Tap + to add a customer.",
    val customerStatement: String = "Customer Statement",
    val shareStatement: String = "Share Statement",
    val creditSaleItem: String = "Credit Sale",
    val customerPaymentItem: String = "Customer Payment",
    val writeOffItem: String = "Bad Debt Write-off",
    val customerOutstanding: String = "Customer Outstanding (Udhaar)",
    val customersOwing: String = "customers owing",
    val noOwingCustomers: String = "No outstanding balances found.",
    val customer: String = "Customer",
    val balance: String = "Balance",

    // Header & Shop selector
    val selectShop: String = "Select Shop",
    val yourShops: String = "YOUR SHOPS",
    val manageShops: String = "Manage Shops",
    val addNewShop: String = "Add New Shop",
    val activeShop: String = "ACTIVE SHOP",
    val currentlyActive: String = "Currently Active",
    val noShopSelected: String = "No Shop Selected",
    val renameShop: String = "Rename Shop",
    val renameCurrentShop: String = "Rename Current Shop",
    val deleteShop: String = "Delete Shop",
    val deleteCurrentShop: String = "Delete Current Shop",
    val allShopsHeader: String = "ALL SHOPS",

    // Time periods
    val today: String = "Today",
    val thisWeek: String = "This Week",
    val thisMonth: String = "This Month",
    val lastMonth: String = "Last Month",
    val allTime: String = "All Time",
    val custom: String = "Custom",

    // Metrics
    val netProfit: String = "Net Profit",
    val netLoss: String = "Net Loss",
    val profitMargin: String = "Profit Margin",
    val cashPaidOut: String = "Cash Paid Out",
    val totalPayable: String = "Payable to Suppliers",
    val totalSales: String = "Total Sales",
    val totalExpenses: String = "Total Expenses",
    val totalPurchases: String = "Total Purchases",
    val netCashFlow: String = "Net Cash Flow",
    val profitTag: String = "PROFIT",
    val lossTag: String = "LOSS",

    // Actions & Buttons
    val quickActions: String = "Quick Actions",
    val addSale: String = "Add Sale",
    val addExpense: String = "Add Expense",
    val addSupplier: String = "Add Supplier",
    val addPurchase: String = "Add Purchase",
    val recordSale: String = "Record Sale",
    val recordExpense: String = "Record Expense",
    val recordPurchase: String = "Record Purchase",
    val recordPayment: String = "Record Payment",
    val payNow: String = "Pay Now",
    val save: String = "Save",
    val saveChanges: String = "Save Changes",
    val cancel: String = "Cancel",
    val delete: String = "Delete",
    val edit: String = "Edit",
    val confirm: String = "Confirm",
    val search: String = "Search...",
    val filter: String = "Filter",
    val all: String = "All",
    val exportCsv: String = "Export CSV",
    val sharePdf: String = "Share as PDF",
    val shareReport: String = "Share Report",
    val back: String = "Back",
    val viewAll: String = "View All",
    val recentTransactions: String = "Recent Transactions",

    // Sales & Expenses lists
    val totalSalesRecorded: String = "TOTAL SALES RECORDED",
    val totalExpensesRecorded: String = "TOTAL EXPENSES RECORDED",
    val transactionsCount: String = "transactions",
    val filterByCategory: String = "Filter by Category",
    val allCategories: String = "All Categories",
    val saleAmountPrompt: String = "Sale Amount (PKR) *",
    val expenseAmountPrompt: String = "Expense Amount (PKR) *",
    val noteOptionalPrompt: String = "Note / Description (Optional)",
    val categoryPrompt: String = "Category *",

    // Supplier details & Ledger
    val supplierLedger: String = "Supplier Ledger",
    val totalPurchased: String = "Total Purchased",
    val totalPaid: String = "Total Paid",
    val balanceRemaining: String = "Balance Remaining",
    val phone: String = "Phone",
    val note: String = "Note",
    val billAmount: String = "Total Bill Amount",
    val paidNow: String = "Amount Paid Now",
    val remainingBill: String = "Remaining Balance",
    val paymentAmount: String = "Payment Amount",
    val paymentDate: String = "Date",
    val paymentNote: String = "Note / Receipt #",
    val callSupplier: String = "Call Supplier",
    val purchaseItem: String = "Purchase (Stock / Bill)",
    val paymentItem: String = "Cash Payment",

    // Settings
    val settingsAndBackup: String = "Settings & Backup",
    val appearanceAndTheme: String = "APPEARANCE & THEME",
    val darkModeTitle: String = "Theme Mode",
    val languageTitle: String = "App Language",
    val appLock: String = "APP LOCK & SECURITY",
    val enableAppLock: String = "Enable Biometric / Device Lock",
    val setCustomPin: String = "Set Custom 4-Digit PIN",
    val backupAndRestore: String = "DATA BACKUP & RESTORE",
    val exportBackup: String = "Export Backup (JSON)",
    val restoreBackup: String = "Restore Backup from File",

    // App Lock & Security
    val fingerprintNote: String = "Fingerprint uses the fingerprints saved on this phone.",
    val useHisaabPin: String = "Use Hisaab PIN",
    val hisaabLocked: String = "Hisaab Locked",
    val enterAppPinPrompt: String = "Enter your 4-6 digit Hisaab PIN",
    val forgotPin: String = "Forgot PIN?",
    val forgotPinTitle: String = "Reset App PIN",
    val forgotPinWarning: String = "Resetting your PIN requires clearing app data, which will erase all records unless a backup exists.",
    val restoreFromBackup: String = "Restore from Hisaab Backup",
    val clearAllData: String = "Clear All Data",

    // Empty states
    val noTransactionsYet: String = "No transactions recorded for this period.",
    val noSalesYet: String = "No sales recorded yet. Tap + to record a sale.",
    val noExpensesYet: String = "No expenses recorded yet. Tap + to log an expense.",
    val noSuppliersYet: String = "No suppliers found. Tap + to add a supplier.",
    val zeroBalanceCongrats: String = "All clear! No balance owed to suppliers.",
    val noShopSelectedState: String = "Please create or select a shop to manage ledger accounts.",

    // First Shop & Onboarding
    val welcomeTitle: String = "Welcome to Hisaab",
    val welcomeSubtitle: String = "Offline-first shop bookkeeping app.\nTrack sales, expenses, purchases, and customer/supplier credit with zero hassle.",
    val addFirstShopTitle: String = "Add your first shop",
    val shopNamePrompt: String = "Shop or Business Name",
    val shopNameEmptyError: String = "Shop name cannot be empty",
    val quickSuggestions: String = "Quick suggestions:",
    val startUsingApp: String = "Start Using Hisaab",
    val offlineSecurityNotice: String = "🔒 100% Offline & Private: All your records stay safe on this phone.",

    // Dialog messages
    val deleteConfirmTitle: String = "Are you sure?",
    val deleteWarning: String = "This action cannot be undone.",
    val enterValidAmount: String = "Please enter a valid positive amount.",
    val amountTooLargeError: String = "Amount exceeds maximum limit (Rs 999 Billion).",
    val invalidPin: String = "PIN must be 4 digits.",
    val currencyUnit: String = "PKR"
)

val EnglishStrings = AppStrings()

val UrduStrings = AppStrings(
    // Bottom navigation
    navDashboard = "ڈیش بورڈ",
    navCustomers = "گاہک (ادھار)",
    navSales = "سیلز (فروخت)",
    navExpenses = "اخراجات",
    navSuppliers = "سپلائرز کھاتہ",
    navReports = "رپورٹس",

    // Customer & Udhaar strings
    customersTitle = "گاہک کھاتہ (ادھار)",
    customerLedgerTitle = "گاہک کھاتہ",
    totalReceivable = "کل وصول طلب (ادھار)",
    customersOweYou = "گاہکوں سے لینی ہے",
    youWillGet = "آپ کو ملیں گے",
    youWillGive = "آپ نے دینے ہیں",
    cashReceived = "وصول شدہ کیش",
    netCash = "خالص کیش",
    badDebt = "ڈوبی رقم (بیڈ ڈیٹ)",
    badDebtWriteOff = "رائٹ آف (ڈوبی رقم)",
    addCustomer = "نیا گاہک",
    editCustomer = "گاہک میں ترمیم",
    customerName = "گاہک کا نام *",
    customerPhone = "فون نمبر",
    customerNote = "نوٹ / پتہ",
    creditLimit = "ادھار کی حد (اختیاری)",
    creditLimitLabel = "ادھار حد",
    owingOnly = "صرف بقایا والے",
    allCustomers = "تمام گاہک",
    totalCreditGiven = "کل ادھار دیا گیا",
    totalReceivedBack = "کل وصول شدہ",
    totalWriteOffs = "رائٹ آف رقم",
    currentBalance = "موجودہ بقایا",
    receivePayment = "رقم وصول کریں",
    writeOff = "رائٹ آف کریں",
    sendReminder = "یاد دہانی بھیجیں",
    selectCustomer = "گاہک منتخب کریں (اختیاری)",
    noCustomerCashOnly = "کوئی گاہک نہیں (صرف نقد)",
    newCustomerQuickAdd = "+ نیا گاہک",
    amountReceivedNow = "ابھی وصول کردہ رقم *",
    remainingUdhaar = "بقایا ادھار",
    creditLimitWarningTitle = "ادھار حد سے تجاوز",
    creditLimitWarningMessage = "یہ سیل گاہک کی مقررہ ادھار حد سے زیادہ ہے۔ کیا آپ پھر بھی جاری رکھنا چاہتے ہیں؟",
    writeOffConfirmTitle = "رائٹ آف کی تصدیق",
    writeOffConfirmMessage = "کیا آپ واقعی یہ رقم ڈوبے ہوئے قرض کے طور پر رائٹ آف کرنا چاہتے ہیں؟ اس سے خالص منافع کم ہو جائے گا۔",
    writeOffNoteRequired = "رائٹ آف کے لیے وجہ یا نوٹ لازمی ہے۔",
    amountExceedsBalance = "رقم موجودہ بقایا سے زیادہ نہیں ہو سکتی۔",
    receivedCannotExceedTotal = "وصول شدہ رقم کل رقم سے زیادہ نہیں ہو سکتی۔",
    noCreditWithoutCustomer = "ادھار پر سیل کے لیے گاہک منتخب کرنا لازمی ہے۔",
    noCustomersYet = "ابھی تک کوئی گاہک درج نہیں ہے۔ نیا گاہک شامل کرنے کے لیے + دبائیں۔",
    customerStatement = "گاہک کھاتہ اسٹیٹمنٹ",
    shareStatement = "اسٹیٹمنٹ شیئر کریں",
    creditSaleItem = "ادھار سیل",
    customerPaymentItem = "گاہک کی ادائیگی",
    writeOffItem = "رائٹ آف (ڈوب گئی رقم)",
    customerOutstanding = "گاہکوں کا بقایا (ادھار)",
    customersOwing = "گاہکوں کے ذمہ واجب الادا",
    noOwingCustomers = "کوئی بقایا رقم نہیں ہے۔",
    customer = "گاہک",
    balance = "بقایا",

    // Header & Shop selector
    selectShop = "دکان منتخب کریں",
    yourShops = "آپ کی دکانیں",
    manageShops = "دکانوں کا انتظام",
    addNewShop = "نئی دکان شامل کریں",
    activeShop = "موجودہ دکان",
    currentlyActive = "فی الوقت فعال",
    noShopSelected = "کوئی دکان منتخب نہیں",
    renameShop = "دکان کا نام تبدیل کریں",
    renameCurrentShop = "موجودہ دکان کا نام بدلیں",
    deleteShop = "دکان حذف کریں",
    deleteCurrentShop = "موجودہ دکان حذف کریں",
    allShopsHeader = "تمام دکانیں",

    // Time periods
    today = "آج",
    thisWeek = "اس ہفتے",
    thisMonth = "اس مہینے",
    lastMonth = "گزشتہ ماہ",
    allTime = "تمام وقت",
    custom = "اپنی مرضی",

    // Metrics
    netProfit = "خالص منافع",
    netLoss = "خالص نقصان",
    profitMargin = "منافع کا تناسب",
    cashPaidOut = "کل نقد ادائیگی",
    totalPayable = "سپلائرز کے واجبات",
    totalSales = "کل سیلز",
    totalExpenses = "کل اخراجات",
    totalPurchases = "کل خریداری (مال)",
    netCashFlow = "خالص کیش فلو",
    profitTag = "منافع",
    lossTag = "نقصان",

    // Actions & Buttons
    quickActions = "فوری کارروائیاں",
    addSale = "سیل شامل کریں",
    addExpense = "خرچہ درج کریں",
    addSupplier = "نیا سپلائر",
    addPurchase = "خریداری مال",
    recordSale = "سیل محفوظ کریں",
    recordExpense = "خرچ محفوظ کریں",
    recordPurchase = "خریداری محفوظ کریں",
    recordPayment = "ادائیگی درج کریں",
    payNow = "ادائیگی کریں",
    save = "محفوظ کریں",
    saveChanges = "تبدیلیاں محفوظ کریں",
    cancel = "منسوخ",
    delete = "حذف کریں",
    edit = "ترمیم کریں",
    confirm = "تصدیق کریں",
    search = "تلاش کریں...",
    filter = "فلٹر",
    all = "سب",
    exportCsv = "ایکسپورٹ CSV",
    sharePdf = "پی ڈی ایف شیئر کریں",
    shareReport = "رپورٹ شیئر کریں",
    back = "واپس",
    viewAll = "سب دیکھیں",
    recentTransactions = "حالیہ لین دین",

    // Sales & Expenses lists
    totalSalesRecorded = "کل درج شدہ سیلز",
    totalExpensesRecorded = "کل درج شدہ اخراجات",
    transactionsCount = "ٹرانزیکشنز",
    filterByCategory = "کیٹیگری فلٹر",
    allCategories = "تمام کیٹیگریز",
    saleAmountPrompt = "سیل کی رقم (روپے) *",
    expenseAmountPrompt = "خرچ کی رقم (روپے) *",
    noteOptionalPrompt = "نوٹ / تفصیل (اختیاری)",
    categoryPrompt = "کیٹیگری *",

    // Supplier details & Ledger
    supplierLedger = "سپلائر کھاتہ",
    totalPurchased = "کل خریدا گیا",
    totalPaid = "کل ادا شدہ",
    balanceRemaining = "باقی رقم (واجب الادا)",
    phone = "فون نمبر",
    note = "تفصیل / نوٹ",
    billAmount = "کل بل رقم",
    paidNow = "ابھی ادا کردہ رقم",
    remainingBill = "بقایا رقم",
    paymentAmount = "ادائیگی کی رقم",
    paymentDate = "تاریخ",
    paymentNote = "نوٹ / رسید نمبر",
    callSupplier = "کال کریں",
    purchaseItem = "خریداری مال",
    paymentItem = "نقد ادائیگی",

    // Settings
    settingsAndBackup = "ترتیبات اور بیک اپ",
    appearanceAndTheme = "ڈیزائن اور تھیم",
    darkModeTitle = "تھیم موڈ",
    languageTitle = "ایپ کی زبان",
    appLock = "ایپ لاک اور سیکیورٹی",
    enableAppLock = "فنگر پرنٹ یا پن لاک لگائیں",
    setCustomPin = "4 ہندسوں کا پن کوڈ سیٹ کریں",
    backupAndRestore = "ڈیٹا بیک اپ اور بحالی",
    exportBackup = "بیک اپ محفوظ کریں (JSON)",
    restoreBackup = "بیک اپ فائل سے بحال کریں",

    // App Lock & Security
    fingerprintNote = "فنگر پرنٹ اس فون میں محفوظ فنگر پرنٹس کا استعمال کرتا ہے۔",
    useHisaabPin = "حساب پن استعمال کریں",
    hisaabLocked = "حساب ایپ لاک ہے",
    enterAppPinPrompt = "اپنا 4 سے 6 ہندسوں کا حساب پن درج کریں",
    forgotPin = "پن بھول گئے؟",
    forgotPinTitle = "ایپ پن ری سیٹ کریں",
    forgotPinWarning = "پن ری سیٹ کرنے کے لیے ایپ ڈیٹا صاف کرنا پڑے گا، جس سے تمام ریکارڈز ختم ہو جائیں گے الا یہ کہ آپ کے پاس بیک اپ موجود ہو۔",
    restoreFromBackup = "حساب بیک اپ سے بحال کریں",
    clearAllData = "تمام ڈیٹا صاف کریں",

    // Empty states
    noTransactionsYet = "اس مدت میں کوئی لین دین درج نہیں ہے۔",
    noSalesYet = "ابھی تک کوئی سیل نہیں ہوئی۔ نیا ریکارڈ درج کرنے کے لیے + دبائیں۔",
    noExpensesYet = "کوئی خرچہ درج نہیں ہے۔ نیا خرچ درج کرنے کے لیے + دبائیں۔",
    noSuppliersYet = "کوئی سپلائر درج نہیں ہے۔ نیا سپلائر شامل کرنے کے لیے + دبائیں۔",
    zeroBalanceCongrats = "ماشاءاللہ! کوئی رقم واجب الادا نہیں ہے۔",
    noShopSelectedState = "کھاتہ چلانے کے لیے براہ کرم ایک دکان بنائیں یا منتخب کریں۔",

    // First Shop & Onboarding
    welcomeTitle = "حساب ایپ میں خوش آمدید",
    welcomeSubtitle = "آپ کے کاروبار کے کھاتے کے لیے آف لائن اور محفوظ ایپ۔ سیلز، اخراجات اور ادھار کھاتہ آسانی سے سنبھالیں۔",
    addFirstShopTitle = "اپنی پہلی دکان کا اندراج کریں",
    shopNamePrompt = "دکان یا کاروبار کا نام",
    shopNameEmptyError = "دکان کا نام خالی نہیں ہو سکتا",
    quickSuggestions = "فوری تجاویز:",
    startUsingApp = "حساب کا استعمال شروع کریں",
    offlineSecurityNotice = "🔒 100% آف لائن اور پرائیویٹ: آپ کا تمام ڈیٹا صرف آپ کے فون میں محفوظ ہے۔",

    // Dialog messages
    deleteConfirmTitle = "کیا آپ واقعی حذف کرنا چاہتے ہیں؟",
    deleteWarning = "اس کارروائی کو واپس نہیں لایا جا سکتا۔",
    enterValidAmount = "براہ کرم درست رقم درج کریں۔",
    amountTooLargeError = "رقم کی حد 999 ارب روپے سے زیادہ نہیں ہو سکتی۔",
    invalidPin = "پن کوڈ 4 ہندسوں پر مشتمل ہونا چاہیے۔",
    currencyUnit = "روپے"
)

val LocalAppStrings = staticCompositionLocalOf { EnglishStrings }
val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.ENGLISH }

object LocalStrings {
    val current: AppStrings
        @Composable
        @ReadOnlyComposable
        get() = LocalAppStrings.current
}
