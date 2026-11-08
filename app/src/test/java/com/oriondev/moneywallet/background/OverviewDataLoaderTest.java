package com.oriondev.moneywallet.background;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.oriondev.moneywallet.model.Group;
import com.oriondev.moneywallet.model.OverviewData;
import com.oriondev.moneywallet.model.OverviewSetting;
import com.oriondev.moneywallet.model.PeriodMoney;
import com.oriondev.moneywallet.storage.database.Contract;
import com.oriondev.moneywallet.storage.database.DataContentProvider;
import com.oriondev.moneywallet.storage.database.TestDatabases;
import com.oriondev.moneywallet.storage.database.TransferContentValuesBuilder;
import com.oriondev.moneywallet.storage.preference.PreferenceManager;
import com.oriondev.moneywallet.ui.activity.NewEditTransactionActivity;
import com.oriondev.moneywallet.utils.DateUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.assertEquals;

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

    private OverviewData loadOverview(OverviewSetting.CashFlow cashFlow) {
        OverviewSetting setting = new OverviewSetting(
                at(1, 0, 0, 0, 0),
                at(31, 23, 59, 59, 999),
                Group.YEARLY,
                cashFlow);
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
