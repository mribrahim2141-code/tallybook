package com.oriondev.moneywallet.background;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.oriondev.moneywallet.model.Group;
import com.oriondev.moneywallet.model.OverviewData;
import com.oriondev.moneywallet.model.OverviewExpenseCategory;
import com.oriondev.moneywallet.model.OverviewSetting;
import com.oriondev.moneywallet.model.PeriodMoney;
import com.oriondev.moneywallet.storage.database.Contract;
import com.oriondev.moneywallet.storage.database.DataContentProvider;
import com.oriondev.moneywallet.storage.database.SplitPartsCodec;
import com.oriondev.moneywallet.storage.database.TestDatabases;
import com.oriondev.moneywallet.storage.database.TransferContentValuesBuilder;
import com.oriondev.moneywallet.storage.preference.PreferenceManager;
import com.oriondev.moneywallet.ui.activity.NewEditTransactionActivity;
import com.oriondev.moneywallet.utils.DateUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Verifies that overview totals agree with the detailed period summary: transfer principal is
 * kept out of earned/spent figures, transfer fees remain expenses, and wallet balances include
 * opening money without combining different currencies.
 */
@RunWith(RobolectricTestRunner.class)
public class OverviewDataLoaderTest {

    private static final String ICON = "{\"type\":\"color\",\"color\":\"#000000\",\"name\":\"T\"}";

    private Context mContext;
    private ContentResolver mResolver;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        TestDatabases.useFreshDatabase(mContext);
        mResolver = mContext.getContentResolver();
    }

    @Test
    public void summarySeparatesTransferPrincipalAndIncludesOpeningBalance() {
        long wallet = insertWallet("Checking", "EUR", 1000L, true);
        long other = insertWallet("Savings", "EUR", 0L, true);
        PreferenceManager.setCurrentWallet(mContext, wallet);
        insertIncome(wallet, 50L);
        insertTransfer(other, wallet, 300L, 0L);
        insertTransfer(wallet, other, 100L, 10L);

        OverviewData data = loadOverview(OverviewSetting.CashFlow.NET_INCOMES);
        assertEquals(1, data.getPeriodCount());
        PeriodMoney period = data.getPeriodMoney(0);

        assertEquals("ordinary income only", 50L, period.getIncomes().getMoney("EUR"));
        assertEquals("transfer fee only", 10L, period.getExpenses().getMoney("EUR"));
        assertEquals("incoming less outgoing transfer principal", 200L,
                period.getTransfers().getMoney("EUR"));
        assertEquals(240L, period.getNetIncomes().getMoney("EUR"));
        assertEquals(50L, data.getTotalIncomes().getMoney("EUR"));
        assertEquals(10L, data.getTotalExpenses().getMoney("EUR"));
        assertEquals(200L, data.getTotalTransfers().getMoney("EUR"));
        assertEquals(240L, data.getTotalNetIncomes().getMoney("EUR"));
        assertEquals("opening balance + income + transfer in - transfer out - fee",
                1240L, data.getTotalBalance().getMoney("EUR"));
    }

    @Test
    public void totalWalletBalanceKeepsCurrenciesSeparateAndHonorsWalletInclusion() {
        insertWallet("Cash EUR", "EUR", 100L, true);
        insertWallet("Cash USD", "USD", 200L, true);
        insertWallet("Hidden wallet", "EUR", 900L, false);
        PreferenceManager.setCurrentWallet(mContext, PreferenceManager.TOTAL_WALLET_ID);

        OverviewData data = loadOverview(OverviewSetting.CashFlow.NET_INCOMES);

        assertEquals(100L, data.getTotalBalance().getMoney("EUR"));
        assertEquals(200L, data.getTotalBalance().getMoney("USD"));
    }

    @Test
    public void topExpenseCategoriesAggregateRankAndKeepCurrenciesSeparate() {
        long eurWallet = insertWallet("EUR cash", "EUR", 0L, true);
        long otherEurWallet = insertWallet("EUR savings", "EUR", 0L, true);
        long usdWallet = insertWallet("USD cash", "USD", 0L, true);
        PreferenceManager.setCurrentWallet(mContext, PreferenceManager.TOTAL_WALLET_ID);

        long food = insertExpenseCategory("Food", true);
        long rent = insertExpenseCategory("Rent", true);
        long transport = insertExpenseCategory("Transport", true);
        long health = insertExpenseCategory("Health", true);
        long hidden = insertExpenseCategory("Hidden", false);
        long fuel = insertExpenseCategory("Fuel", true);

        insertExpense(eurWallet, food, 100L);
        insertExpense(eurWallet, food, 50L);
        insertExpense(eurWallet, rent, 200L);
        insertExpense(eurWallet, transport, 75L);
        insertExpense(eurWallet, health, 50L);
        insertExpense(eurWallet, hidden, 900L);
        insertExpense(usdWallet, fuel, 500L);
        // Transfer principal is not spending and must not appear among expense categories.
        insertTransfer(eurWallet, otherEurWallet, 1000L, 0L);

        OverviewData data = loadOverview(OverviewSetting.CashFlow.NET_INCOMES);
        java.util.List<OverviewExpenseCategory> top = data.getTopExpenseCategories();

        assertEquals("top three for EUR followed by the independent USD ranking", 4, top.size());
        assertEquals(rent, top.get(0).getCategoryId());
        assertEquals(200L, top.get(0).getAmount());
        assertEquals("EUR", top.get(0).getCurrency());
        assertEquals(food, top.get(1).getCategoryId());
        assertEquals(150L, top.get(1).getAmount());
        assertEquals(transport, top.get(2).getCategoryId());
        assertEquals(75L, top.get(2).getAmount());
        assertEquals(fuel, top.get(3).getCategoryId());
        assertEquals(500L, top.get(3).getAmount());
        assertEquals("USD", top.get(3).getCurrency());
        for (OverviewExpenseCategory category : top) {
            assertTrue("hidden categories and transfer principal must not be ranked",
                    category.getCategoryId() != hidden);
        }
    }

    @Test
    public void topExpenseCategoriesCountSplitPartsAndMainCategoryRemainder() {
        long wallet = insertWallet("Split expenses", "EUR", 0L, true);
        PreferenceManager.setCurrentWallet(mContext, PreferenceManager.TOTAL_WALLET_ID);

        long main = insertExpenseCategory("Other", true);
        long food = insertExpenseCategory("Food", true);
        long health = insertExpenseCategory("Health", true);

        // The transaction total is 1,000: 600 belongs to Food, 300 to Health,
        // and the remaining 100 stays under its main category, Other.
        insertSplitExpense(wallet, main, 1000L, food, 600L, health, 300L);
        insertExpense(wallet, food, 100L);

        java.util.List<OverviewExpenseCategory> top =
                loadOverview(OverviewSetting.CashFlow.NET_INCOMES).getTopExpenseCategories();

        assertEquals(3, top.size());
        assertEquals(food, top.get(0).getCategoryId());
        assertEquals(700L, top.get(0).getAmount());
        assertEquals(health, top.get(1).getCategoryId());
        assertEquals(300L, top.get(1).getAmount());
        assertEquals(main, top.get(2).getCategoryId());
        assertEquals(100L, top.get(2).getAmount());
    }

    @Test
    public void topExpenseCategoriesHonorSelectedWalletAndDateRange() {
        long selectedWallet = insertWallet("Selected", "EUR", 0L, true);
        long otherWallet = insertWallet("Other", "EUR", 0L, true);
        PreferenceManager.setCurrentWallet(mContext, selectedWallet);
        long food = insertExpenseCategory("Food", true);

        insertExpenseAt(selectedWallet, food, 120L, at(15, 12, 0, 0, 0));
        insertExpenseAt(selectedWallet, food, 400L, at(5, 12, 0, 0, 0));
        insertExpenseAt(selectedWallet, food, 500L, at(25, 12, 0, 0, 0));
        insertExpenseAt(otherWallet, food, 900L, at(15, 12, 0, 0, 0));

        java.util.List<OverviewExpenseCategory> top = loadOverview(
                at(10, 0, 0, 0, 0), at(20, 23, 59, 59, 999))
                .getTopExpenseCategories();

        assertEquals(1, top.size());
        assertEquals(food, top.get(0).getCategoryId());
        assertEquals("only the selected wallet's in-range expense is counted",
                120L, top.get(0).getAmount());
    }

    private long insertExpenseCategory(String name, boolean showReport) {
        ContentValues values = new ContentValues();
        values.put(Contract.Category.NAME, name);
        values.put(Contract.Category.ICON, ICON);
        values.put(Contract.Category.TYPE, Contract.CategoryType.EXPENSE.getValue());
        values.put(Contract.Category.SHOW_REPORT, showReport);
        return ContentUris.parseId(mResolver.insert(DataContentProvider.CONTENT_CATEGORIES, values));
    }

    private void insertSplitExpense(long walletId, long mainCategoryId, long total,
                                    long firstCategoryId, long firstAmount,
                                    long secondCategoryId, long secondAmount) {
        ContentValues values = new ContentValues();
        values.put(Contract.Transaction.MONEY, total);
        values.put(Contract.Transaction.DATE, midMonth());
        values.put(Contract.Transaction.DESCRIPTION, "Split expense");
        values.put(Contract.Transaction.CATEGORY_ID, mainCategoryId);
        values.put(Contract.Transaction.DIRECTION, Contract.Direction.EXPENSE);
        values.put(Contract.Transaction.TYPE, NewEditTransactionActivity.TYPE_STANDARD);
        values.put(Contract.Transaction.WALLET_ID, walletId);
        values.put(Contract.Transaction.CONFIRMED, true);
        values.put(Contract.Transaction.COUNT_IN_TOTAL, true);
        values.put(Contract.Transaction.SPLIT_PARTS, SplitPartsCodec.encode(Arrays.asList(
                new SplitPartsCodec.Part(firstCategoryId, firstAmount, null),
                new SplitPartsCodec.Part(secondCategoryId, secondAmount, null))));
        mResolver.insert(DataContentProvider.CONTENT_TRANSACTIONS, values);
    }

    private void insertExpenseAt(long walletId, long categoryId, long money, Date date) {
        ContentValues values = new ContentValues();
        values.put(Contract.Transaction.MONEY, money);
        values.put(Contract.Transaction.DATE, DateUtils.getSQLDateTimeString(date));
        values.put(Contract.Transaction.DESCRIPTION, "Dated expense");
        values.put(Contract.Transaction.CATEGORY_ID, categoryId);
        values.put(Contract.Transaction.DIRECTION, Contract.Direction.EXPENSE);
        values.put(Contract.Transaction.TYPE, NewEditTransactionActivity.TYPE_STANDARD);
        values.put(Contract.Transaction.WALLET_ID, walletId);
        values.put(Contract.Transaction.CONFIRMED, true);
        values.put(Contract.Transaction.COUNT_IN_TOTAL, true);
        mResolver.insert(DataContentProvider.CONTENT_TRANSACTIONS, values);
    }

    private void insertExpense(long walletId, long categoryId, long money) {
        ContentValues values = new ContentValues();
        values.put(Contract.Transaction.MONEY, money);
        values.put(Contract.Transaction.DATE, midMonth());
        values.put(Contract.Transaction.DESCRIPTION, "Expense");
        values.put(Contract.Transaction.CATEGORY_ID, categoryId);
        values.put(Contract.Transaction.DIRECTION, Contract.Direction.EXPENSE);
        values.put(Contract.Transaction.TYPE, NewEditTransactionActivity.TYPE_STANDARD);
        values.put(Contract.Transaction.WALLET_ID, walletId);
        values.put(Contract.Transaction.CONFIRMED, true);
        values.put(Contract.Transaction.COUNT_IN_TOTAL, true);
        mResolver.insert(DataContentProvider.CONTENT_TRANSACTIONS, values);
    }

    private OverviewData loadOverview(OverviewSetting.CashFlow cashFlow) {
        return loadOverview(at(1, 0, 0, 0, 0), at(31, 23, 59, 59, 999), cashFlow);
    }

    private OverviewData loadOverview(Date start, Date end) {
        return loadOverview(start, end, OverviewSetting.CashFlow.NET_INCOMES);
    }

    private OverviewData loadOverview(Date start, Date end,
                                       OverviewSetting.CashFlow cashFlow) {
        OverviewSetting setting = new OverviewSetting(start, end, Group.YEARLY, cashFlow);
        return new OverviewDataLoader(mContext, setting).loadInBackground();
    }

    private static Date at(int day, int hour, int minute, int second, int millisecond) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(2026, Calendar.JANUARY, day, hour, minute, second);
        calendar.set(Calendar.MILLISECOND, millisecond);
        return calendar.getTime();
    }

    private static String midMonth() {
        return DateUtils.getSQLDateTimeString(at(15, 12, 0, 0, 0));
    }

    private long insertWallet(String name, String currency, long opening, boolean countInTotal) {
        ContentValues values = new ContentValues();
        values.put(Contract.Wallet.NAME, name);
        values.put(Contract.Wallet.ICON, ICON);
        values.put(Contract.Wallet.CURRENCY, currency);
        values.put(Contract.Wallet.START_MONEY, opening);
        values.put(Contract.Wallet.COUNT_IN_TOTAL, countInTotal);
        values.put(Contract.Wallet.ARCHIVED, false);
        return ContentUris.parseId(mResolver.insert(DataContentProvider.CONTENT_WALLETS, values));
    }

    private void insertIncome(long walletId, long money) {
        ContentValues category = new ContentValues();
        category.put(Contract.Category.NAME, "Salary");
        category.put(Contract.Category.ICON, ICON);
        category.put(Contract.Category.TYPE, Contract.CategoryType.INCOME.getValue());
        category.put(Contract.Category.SHOW_REPORT, true);
        long categoryId = ContentUris.parseId(
                mResolver.insert(DataContentProvider.CONTENT_CATEGORIES, category));

        ContentValues values = new ContentValues();
        values.put(Contract.Transaction.MONEY, money);
        values.put(Contract.Transaction.DATE, midMonth());
        values.put(Contract.Transaction.DESCRIPTION, "Pay");
        values.put(Contract.Transaction.CATEGORY_ID, categoryId);
        values.put(Contract.Transaction.DIRECTION, Contract.Direction.INCOME);
        values.put(Contract.Transaction.TYPE, NewEditTransactionActivity.TYPE_STANDARD);
        values.put(Contract.Transaction.WALLET_ID, walletId);
        values.put(Contract.Transaction.CONFIRMED, true);
        values.put(Contract.Transaction.COUNT_IN_TOTAL, true);
        mResolver.insert(DataContentProvider.CONTENT_TRANSACTIONS, values);
    }

    private void insertTransfer(long fromWalletId, long toWalletId, long money, long fee) {
        TransferContentValuesBuilder builder = new TransferContentValuesBuilder()
                .description("Move")
                .date(midMonth())
                .fromWalletId(fromWalletId)
                .toWalletId(toWalletId)
                .taxWalletId(fromWalletId)
                .fromMoney(money)
                .toMoney(money)
                .taxMoney(fee)
                .note("")
                .confirmed(true)
                .countInTotal(true);
        mResolver.insert(DataContentProvider.CONTENT_TRANSFERS, builder.build());
    }
}
