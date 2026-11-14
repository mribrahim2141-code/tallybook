/*
 * Copyright (c) 2018.
 *
 * This file is part of MoneyWallet.
 *
 * MoneyWallet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MoneyWallet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MoneyWallet.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.oriondev.moneywallet.background;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;

import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.RadarData;
import com.github.mikephil.charting.data.RadarDataSet;
import com.github.mikephil.charting.data.RadarEntry;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.interfaces.datasets.IRadarDataSet;
import com.oriondev.moneywallet.model.CurrencyUnit;
import com.oriondev.moneywallet.model.Money;
import com.oriondev.moneywallet.model.MoneyScale;
import com.oriondev.moneywallet.model.OverviewData;
import com.oriondev.moneywallet.model.OverviewExpenseCategory;
import com.oriondev.moneywallet.model.OverviewSetting;
import com.oriondev.moneywallet.model.PeriodMoney;
import com.oriondev.moneywallet.storage.database.Contract;
import com.oriondev.moneywallet.storage.database.DataContentProvider;
import com.oriondev.moneywallet.storage.preference.PreferenceManager;
import com.oriondev.moneywallet.utils.CurrencyManager;
import com.oriondev.moneywallet.utils.DateUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by andrea on 17/08/18.
 */
public class OverviewDataLoader extends AbstractGenericLoader<OverviewData> {

    private final static int[] COLOR_PALETTE = new int[] {
            Color.parseColor("#D32F2F"),
            Color.parseColor("#FF5722"),
            Color.parseColor("#FDD835"),
            Color.parseColor("#4CAF50"),
            Color.parseColor("#039BE5"),
            Color.parseColor("#673AB7")
    };

    private final OverviewSetting mOverviewSetting;

    public OverviewDataLoader(Context context, OverviewSetting overviewSetting) {
        super(context);
        mOverviewSetting = overviewSetting;
    }

    @Override
    protected Uri getObservedUri() {
        return DataContentProvider.CONTENT_ALL;
    }

    @Override
    public OverviewData loadInBackground() {
        Money totalIncomes = new Money();
        Money totalExpenses = new Money();
        Money totalNetIncomes = new Money();
        Money chartNetIncomes = new Money();
        Money totalTransfers = new Money();
        List<PeriodMoney> periodMoneyList = new ArrayList<>();
        Uri uri = DataContentProvider.CONTENT_CATEGORY_LINES;
        String[] projection = new String[] {
                Contract.Transaction.DATE,
                Contract.Transaction.DIRECTION,
                Contract.Transaction.WALLET_CURRENCY,
                Contract.Transaction.MONEY,
                Contract.Transaction.CATEGORY_TAG
        };
        String selection;
        String[] selectionArgs;
        long currentWallet = PreferenceManager.getCurrentWallet();
        if (currentWallet == PreferenceManager.TOTAL_WALLET_ID) {
            selection = Contract.Transaction.WALLET_COUNT_IN_TOTAL + " = 1";
            selectionArgs = null;
        } else {
            selection = Contract.Transaction.WALLET_ID + " = ?";
            selectionArgs = new String[] {String.valueOf(currentWallet)};
        }
        selection += " AND " + Contract.Transaction.CONFIRMED + " = '1' AND " + Contract.Transaction.COUNT_IN_TOTAL + " = '1'";
        selection += " AND " + Contract.Transaction.REPORT_FILTER;
        selection += " AND DATETIME(" + Contract.Transaction.DATE + ") <= DATETIME('now', 'localtime')";
        selection += " AND DATETIME(" + Contract.Transaction.DATE + ") >= DATETIME('" + DateUtils.getSQLDateTimeString(mOverviewSetting.getStartDate()) + "')";
        selection += " AND DATETIME(" + Contract.Transaction.DATE + ") <= DATETIME('" + DateUtils.getSQLDateTimeString(mOverviewSetting.getEndDate()) + "')";
        switch (mOverviewSetting.getType()) {
            case CASH_FLOW:
                switch (mOverviewSetting.getCashFlow()) {
                    // Both of these chart one direction on its own, so transfers stay out of
                    // them. NET_INCOMES keeps them, because it charts a net.
                    case INCOMES:
                        selection += " AND " + Contract.Transaction.DIRECTION + " = ? AND " + Contract.Transaction.NOT_TRANSFER;
                        selectionArgs = appendSelectionArgs(selectionArgs, String.valueOf(Contract.Direction.INCOME));
                        break;
                    case EXPENSES:
                        selection += " AND " + Contract.Transaction.DIRECTION + " = ? AND " + Contract.Transaction.NOT_TRANSFER;
                        selectionArgs = appendSelectionArgs(selectionArgs, String.valueOf(Contract.Direction.EXPENSE));
                        break;
                }
                break;
            case CATEGORY:
                selection += " AND (" + Contract.Transaction.CATEGORY_ID + " = ? OR " + Contract.Transaction.CATEGORY_PARENT_ID + " = ?)";
                selectionArgs = appendSelectionArgs(selectionArgs, String.valueOf(mOverviewSetting.getCategoryId()), String.valueOf(mOverviewSetting.getCategoryId()));
                break;
        }
        String sortOrder = Contract.Transaction.DATE + " ASC";
        Cursor cursor = getContext().getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder);
        if (cursor != null) {
            cursor.moveToFirst();
            PeriodMoney currentPeriod = null;
            while (isAnotherPeriodNeeded(currentPeriod)) {
                currentPeriod = getNextPeriod(currentPeriod);
                if (!cursor.isAfterLast()) {
                    do {
                        Date date = DateUtils.getDateFromSQLDateTimeString(cursor.getString(cursor.getColumnIndex(Contract.Transaction.DATE)));
                        if (belongToPeriod(currentPeriod, date)) {
                            int direction = cursor.getInt(cursor.getColumnIndex(Contract.Transaction.DIRECTION));
                            String currency = cursor.getString(cursor.getColumnIndex(Contract.Transaction.WALLET_CURRENCY));
                            long money = cursor.getLong(cursor.getColumnIndex(Contract.Transaction.MONEY));
                            String categoryTag = cursor.getString(cursor.getColumnIndexOrThrow(Contract.Transaction.CATEGORY_TAG));
                            boolean incoming = direction == Contract.Direction.INCOME;
                            if (Contract.CategoryTag.TRANSFER.equals(categoryTag)) {
                                currentPeriod.addTransfer(currency, money, incoming);
                                if (incoming) {
                                    chartNetIncomes.addMoney(currency, money);
                                } else {
                                    chartNetIncomes.removeMoney(currency, money);
                                }
                            } else if (incoming) {
                                currentPeriod.addIncome(currency, money);
                                chartNetIncomes.addMoney(currency, money);
                            } else if (direction == Contract.Direction.EXPENSE) {
                                currentPeriod.addExpense(currency, money);
                                chartNetIncomes.removeMoney(currency, money);
                            }
                        } else {
                            break;
                        }
                    } while (cursor.moveToNext());
                }
                periodMoneyList.add(currentPeriod);
            }
            cursor.close();
        }
        // Summary figures are independent of the chart's selected cash-flow/category filter.
        loadSummaryTotals(totalIncomes, totalExpenses, totalNetIncomes, totalTransfers);

        // Generate chart data. The line chart compares income, expenses and net flow
        // side-by-side when the complete cash-flow view is selected. Each currency remains
        // an independent series; amounts from different currencies are never combined.
        List<IBarDataSet> barDataSets = new ArrayList<>();
        List<ILineDataSet> lineDataSets = new ArrayList<>();
        List<IRadarDataSet> radarDataSets = new ArrayList<>();
        boolean showIncomeExpenseTrend = mOverviewSetting.getType() == OverviewSetting.Type.CASH_FLOW
                && mOverviewSetting.getCashFlow() == OverviewSetting.CashFlow.NET_INCOMES;
        for (String currency : chartNetIncomes.getCurrencies()) {
            List<BarEntry> barEntryList = new ArrayList<>();
            List<Entry> netLineEntries = new ArrayList<>();
            List<Entry> incomeLineEntries = new ArrayList<>();
            List<Entry> expenseLineEntries = new ArrayList<>();
            List<RadarEntry> radarEntryList = new ArrayList<>();
            CurrencyUnit currencyUnit = CurrencyManager.getCurrency(currency);
            for (int i = 0; i < periodMoneyList.size(); i++) {
                PeriodMoney periodMoney = periodMoneyList.get(i);
                long netMoney = periodMoney.getNetIncomes().getMoney(currency);
                int decimals = CurrencyManager.getDecimals(currencyUnit);
                float netValue = MoneyScale.toHumanAmount(netMoney, decimals).floatValue();
                barEntryList.add(new BarEntry(i, netValue));
                netLineEntries.add(new Entry(i, netValue));
                radarEntryList.add(new RadarEntry(netValue));
                if (showIncomeExpenseTrend) {
                    float incomeValue = MoneyScale.toHumanAmount(
                            periodMoney.getIncomes().getMoney(currency), decimals).floatValue();
                    float expenseValue = MoneyScale.toHumanAmount(
                            periodMoney.getExpenses().getMoney(currency), decimals).floatValue();
                    incomeLineEntries.add(new Entry(i, incomeValue));
                    expenseLineEntries.add(new Entry(i, expenseValue));
                }
            }
            int color = COLOR_PALETTE[barDataSets.size() % COLOR_PALETTE.length];
            BarDataSet barDataSet = new BarDataSet(barEntryList, currency);
            barDataSet.setColor(color);
            barDataSets.add(barDataSet);

            if (showIncomeExpenseTrend) {
                addTrendLineSet(lineDataSets, incomeLineEntries,
                        currency + " · " + getContext().getString(R.string.hint_incomes));
                addTrendLineSet(lineDataSets, expenseLineEntries,
                        currency + " · " + getContext().getString(R.string.hint_expenses));
            }
            addTrendLineSet(lineDataSets, netLineEntries,
                    currency + " · " + getContext().getString(R.string.hint_net_incomes));

            RadarDataSet radarDataSet = new RadarDataSet(radarEntryList, currency);
            radarDataSet.setColor(color);
            radarDataSet.setFillColor(color);
            radarDataSet.setDrawFilled(true);
            radarDataSets.add(radarDataSet);
        }
        BarData barData = null;
        if (!barDataSets.isEmpty()) {
            barData = new BarData(barDataSets);
            int groupSize = barData.getDataSetCount();
            if (groupSize > 1) {
                barData.setBarWidth(0.8f / groupSize);
                barData.groupBars(0, 0.08f, 0.12f / groupSize);
            }
        }
        LineData lineData = null;
        if (!lineDataSets.isEmpty()) {
            lineData = new LineData(lineDataSets);
        }
        // the chart library has a bug when the radar data set is empty
        RadarData radarData = null;
        if (!radarDataSets.isEmpty()) {
            radarData = new RadarData(radarDataSets);
        }
        // return generated data
        Money totalBalance = loadWalletBalance();
        List<OverviewExpenseCategory> topExpenseCategories = loadTopExpenseCategories();
        return new OverviewData(barData, lineData, radarData, periodMoneyList,
                totalIncomes, totalExpenses, totalNetIncomes, totalTransfers, totalBalance,
                topExpenseCategories);
    }


    /**
     * Loads period-wide income, expense, transfer, and net totals independently from the chart
     * filter. Selecting an income/expense chart or drilling into a category must not change
     * the summary card's meaning.
     */
    private void addTrendLineSet(List<ILineDataSet> lineDataSets, List<Entry> entries, String label) {
        int color = COLOR_PALETTE[lineDataSets.size() % COLOR_PALETTE.length];
        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setColor(color);
        dataSet.setCircleColor(color);
        dataSet.setCircleRadius(2f);
        dataSet.setLineWidth(1.8f);
        lineDataSets.add(dataSet);
    }

    private void loadSummaryTotals(Money totalIncomes, Money totalExpenses,
                                   Money totalNetIncomes, Money totalTransfers) {
        String[] projection = new String[] {
                Contract.Transaction.DIRECTION,
                Contract.Transaction.WALLET_CURRENCY,
                Contract.Transaction.MONEY,
                Contract.Transaction.CATEGORY_TAG
        };
        long currentWallet = PreferenceManager.getCurrentWallet();
        String selection;
        String[] selectionArgs;
        if (currentWallet == PreferenceManager.TOTAL_WALLET_ID) {
            selection = Contract.Transaction.WALLET_COUNT_IN_TOTAL + " = 1";
            selectionArgs = null;
        } else {
            selection = Contract.Transaction.WALLET_ID + " = ?";
            selectionArgs = new String[] {String.valueOf(currentWallet)};
        }
        selection += " AND " + Contract.Transaction.CONFIRMED + " = '1' AND "
                + Contract.Transaction.COUNT_IN_TOTAL + " = '1'";
        selection += " AND " + Contract.Transaction.REPORT_FILTER;
        selection += " AND DATETIME(" + Contract.Transaction.DATE + ") <= DATETIME('now', 'localtime')";
        selection += " AND DATETIME(" + Contract.Transaction.DATE + ") >= DATETIME('"
                + DateUtils.getSQLDateTimeString(mOverviewSetting.getStartDate()) + "')";
        selection += " AND DATETIME(" + Contract.Transaction.DATE + ") <= DATETIME('"
                + DateUtils.getSQLDateTimeString(mOverviewSetting.getEndDate()) + "')";

        Cursor cursor = getContext().getContentResolver().query(
                DataContentProvider.CONTENT_CATEGORY_LINES, projection, selection, selectionArgs,
                Contract.Transaction.DATE + " ASC");
        if (cursor == null) {
            return;
        }
        try {
            while (cursor.moveToNext()) {
                int direction = cursor.getInt(cursor.getColumnIndexOrThrow(
                        Contract.Transaction.DIRECTION));
                String currency = cursor.getString(cursor.getColumnIndexOrThrow(
                        Contract.Transaction.WALLET_CURRENCY));
                long amount = cursor.getLong(cursor.getColumnIndexOrThrow(
                        Contract.Transaction.MONEY));
                String categoryTag = cursor.getString(cursor.getColumnIndexOrThrow(
                        Contract.Transaction.CATEGORY_TAG));
                if (Contract.CategoryTag.TRANSFER.equals(categoryTag)) {
                    if (direction == Contract.Direction.INCOME) {
                        totalTransfers.addMoney(currency, amount);
                        totalNetIncomes.addMoney(currency, amount);
                    } else if (direction == Contract.Direction.EXPENSE) {
                        totalTransfers.removeMoney(currency, amount);
                        totalNetIncomes.removeMoney(currency, amount);
                    }
                } else if (direction == Contract.Direction.INCOME) {
                    totalIncomes.addMoney(currency, amount);
                    totalNetIncomes.addMoney(currency, amount);
                } else if (direction == Contract.Direction.EXPENSE) {
                    totalExpenses.addMoney(currency, amount);
                    totalNetIncomes.removeMoney(currency, amount);
                }
            }
        } finally {
            cursor.close();
        }
    }

    /**
     * Ranks expense categories independently for each currency. Amounts in different currencies
     * are never compared or combined, and the same report filters as the period detail are used.
     */
    private List<OverviewExpenseCategory> loadTopExpenseCategories() {
        Map<String, Map<Long, OverviewExpenseCategory>> byCurrency = new HashMap<>();
        String[] projection = new String[] {
                Contract.Transaction.CATEGORY_ID,
                Contract.Transaction.CATEGORY_NAME,
                Contract.Transaction.MONEY,
                Contract.Transaction.WALLET_CURRENCY
        };
        long currentWallet = PreferenceManager.getCurrentWallet();
        String selection;
        String[] selectionArgs;
        if (currentWallet == PreferenceManager.TOTAL_WALLET_ID) {
            selection = Contract.Transaction.WALLET_COUNT_IN_TOTAL + " = 1";
            selectionArgs = null;
        } else {
            selection = Contract.Transaction.WALLET_ID + " = ?";
            selectionArgs = new String[] {String.valueOf(currentWallet)};
        }
        selection += " AND " + Contract.Transaction.CONFIRMED + " = '1' AND "
                + Contract.Transaction.COUNT_IN_TOTAL + " = '1'";
        selection += " AND " + Contract.Transaction.REPORT_FILTER;
        selection += " AND DATETIME(" + Contract.Transaction.DATE + ") <= DATETIME('now', 'localtime')";
        selection += " AND DATETIME(" + Contract.Transaction.DATE + ") >= DATETIME('"
                + DateUtils.getSQLDateTimeString(mOverviewSetting.getStartDate()) + "')";
        selection += " AND DATETIME(" + Contract.Transaction.DATE + ") <= DATETIME('"
                + DateUtils.getSQLDateTimeString(mOverviewSetting.getEndDate()) + "')";
        selection += " AND " + Contract.Transaction.DIRECTION + " = "
                + Contract.Direction.EXPENSE + " AND " + Contract.Transaction.NOT_TRANSFER;

        Cursor cursor = getContext().getContentResolver().query(
                DataContentProvider.CONTENT_CATEGORY_LINES, projection, selection, selectionArgs,
                Contract.Transaction.CATEGORY_ID);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    long categoryId = cursor.getLong(cursor.getColumnIndexOrThrow(
                            Contract.Transaction.CATEGORY_ID));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(
                            Contract.Transaction.CATEGORY_NAME));
                    String currency = cursor.getString(cursor.getColumnIndexOrThrow(
                            Contract.Transaction.WALLET_CURRENCY));
                    long amount = cursor.getLong(cursor.getColumnIndexOrThrow(
                            Contract.Transaction.MONEY));
                    Map<Long, OverviewExpenseCategory> categories = byCurrency.get(currency);
                    if (categories == null) {
                        categories = new HashMap<>();
                        byCurrency.put(currency, categories);
                    }
                    OverviewExpenseCategory category = categories.get(categoryId);
                    if (category == null) {
                        categories.put(categoryId, new OverviewExpenseCategory(
                                categoryId, name, currency, amount));
                    } else {
                        category.addAmount(amount);
                    }
                } while (cursor.moveToNext());
            }
            cursor.close();
        }

        List<String> currencies = new ArrayList<>(byCurrency.keySet());
        Collections.sort(currencies);
        List<OverviewExpenseCategory> result = new ArrayList<>();
        for (String currency : currencies) {
            List<OverviewExpenseCategory> categories = new ArrayList<>(
                    byCurrency.get(currency).values());
            Collections.sort(categories, new Comparator<OverviewExpenseCategory>() {
                @Override
                public int compare(OverviewExpenseCategory left, OverviewExpenseCategory right) {
                    int amountOrder = Long.compare(right.getAmount(), left.getAmount());
                    if (amountOrder != 0) {
                        return amountOrder;
                    }
                    String leftName = left.getName() == null ? "" : left.getName();
                    String rightName = right.getName() == null ? "" : right.getName();
                    return String.CASE_INSENSITIVE_ORDER.compare(leftName, rightName);
                }
            });
            for (int i = 0; i < Math.min(3, categories.size()); i++) {
                result.add(categories.get(i));
            }
        }
        return result;
    }

    /**
     * Reads the live balance from the existing wallet projection. The projection's transaction
     * total does not include the wallet's opening amount, so both are added here. A total-wallet
     * view keeps currencies separate and includes only wallets configured to count in the total.
     */
    private Money loadWalletBalance() {
        Money balance = new Money();
        String[] projection = new String[] {
                Contract.Wallet.CURRENCY,
                Contract.Wallet.START_MONEY,
                Contract.Wallet.TOTAL_MONEY
        };
        long currentWallet = PreferenceManager.getCurrentWallet();
        String selection;
        String[] selectionArgs;
        if (currentWallet == PreferenceManager.TOTAL_WALLET_ID) {
            selection = Contract.Wallet.COUNT_IN_TOTAL + " = 1";
            selectionArgs = null;
        } else {
            selection = Contract.Wallet.ID + " = ?";
            selectionArgs = new String[] {String.valueOf(currentWallet)};
        }
        Cursor cursor = getContext().getContentResolver().query(
                DataContentProvider.CONTENT_WALLETS, projection, selection, selectionArgs, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    String currency = cursor.getString(cursor.getColumnIndexOrThrow(Contract.Wallet.CURRENCY));
                    long opening = cursor.getLong(cursor.getColumnIndexOrThrow(Contract.Wallet.START_MONEY));
                    long transactions = cursor.getLong(cursor.getColumnIndexOrThrow(Contract.Wallet.TOTAL_MONEY));
                    balance.addMoney(currency, opening + transactions);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return balance;
    }

    private String[] appendSelectionArgs(String[] oldArgs, String... newArgs) {
        String[] finalArgs = new String[(oldArgs != null ? oldArgs.length : 0) + newArgs.length];
        if (oldArgs != null) {
            System.arraycopy(oldArgs, 0, finalArgs, 0, oldArgs.length);
        }
        System.arraycopy(newArgs, 0, finalArgs, finalArgs.length - newArgs.length, newArgs.length);
        return finalArgs;
    }

    /**
     * This method is used to determine if another period must be added to the period list.
     * @param periodMoney is the last period analyzed.
     * @return true if the end date has not already been reached.
     */
    private boolean isAnotherPeriodNeeded(PeriodMoney periodMoney) {
        return periodMoney == null || DateUtils.isBefore(periodMoney.getEndDate(), mOverviewSetting.getEndDate());
    }

    private PeriodMoney getNextPeriod(PeriodMoney lastPeriod) {
        Calendar startCalendar = Calendar.getInstance();
        if (lastPeriod != null) {
            startCalendar.setTime(lastPeriod.getEndDate());
            startCalendar.add(Calendar.MILLISECOND, 1);
        } else {
            startCalendar.setTime(mOverviewSetting.getStartDate());
            startCalendar.set(Calendar.HOUR_OF_DAY, 0);
            startCalendar.set(Calendar.MINUTE, 0);
            startCalendar.set(Calendar.SECOND, 0);
            startCalendar.set(Calendar.MILLISECOND, 0);
        }
        Date startDate = startCalendar.getTime();
        Calendar endCalendar = Calendar.getInstance();
        endCalendar.setTime(startDate);
        switch (mOverviewSetting.getGroupType()) {
            case YEARLY:
                // the end date is the last day of the same year of the start date
                endCalendar.set(Calendar.MONTH, Calendar.DECEMBER);
                endCalendar.set(Calendar.DAY_OF_MONTH, 31);
                break;
            case MONTHLY:
                // the end date is generally the last day of the same month of the start month
                // but the user could have specified a different 'first day of month' so the
                // calculation is a bit more tricky:
                // step 1: we check if the start day is after the first day of month
                int firstDayOfMonth = PreferenceManager.getFirstDayOfMonth();
                int currentDayOfMonth = endCalendar.get(Calendar.DAY_OF_MONTH);
                if (currentDayOfMonth >= firstDayOfMonth) {
                    // step 2: we set as 1 the current day of the same month
                    endCalendar.set(Calendar.DAY_OF_MONTH, 1);
                    // step 3: we move to the next month
                    endCalendar.add(Calendar.MONTH, 1);
                }
                // step 4: we move to the first day of month and then we move one day first
                endCalendar.set(Calendar.DAY_OF_MONTH, firstDayOfMonth);
                endCalendar.add(Calendar.DAY_OF_MONTH, -1);
                break;
            case WEEKLY:
                // we can check which day of week is the start date and count how many days are
                // needed to reach the last day of the week that can also be personalized by the
                // user so the calculation is a bit more tricky:
                // step 1: obtain the day of week of the start day and the user preferred one
                int firstDayOfWeek = PreferenceManager.getFirstDayOfWeek();
                int currentDayOfWeek = endCalendar.get(Calendar.DAY_OF_WEEK);
                // step 2: calculate how many days are needed to reach the last day of week
                int offset = firstDayOfWeek - currentDayOfWeek;
                if (offset <= 0) {
                    offset += 7;
                }
                int requiredDays = offset - 1;
                // step 3: add those days to the current start date
                endCalendar.add(Calendar.DAY_OF_MONTH, requiredDays);
                break;
            case DAILY:
                // no special calculation is needed
                break;
        }
        endCalendar.set(Calendar.HOUR_OF_DAY, 23);
        endCalendar.set(Calendar.MINUTE, 59);
        endCalendar.set(Calendar.SECOND, 59);
        endCalendar.set(Calendar.MILLISECOND, 999);
        if (endCalendar.getTimeInMillis() > mOverviewSetting.getEndDate().getTime()) {
            endCalendar.setTime(mOverviewSetting.getEndDate());
        }
        return new PeriodMoney(startDate, endCalendar.getTime());
    }

    private boolean belongToPeriod(PeriodMoney periodMoney, Date date) {
        return DateUtils.isAfterEqual(date, periodMoney.getStartDate()) && DateUtils.isBeforeEqual(date, periodMoney.getEndDate());
    }
}