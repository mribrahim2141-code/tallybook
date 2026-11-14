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

package com.oriondev.moneywallet.ui.fragment.singlepanel;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.viewpager.widget.ViewPager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.oriondev.moneywallet.R;
import com.oriondev.moneywallet.background.OverviewDataLoader;
import com.oriondev.moneywallet.model.Money;
import com.oriondev.moneywallet.utils.CurrencyManager;
import com.oriondev.moneywallet.utils.MoneyProgressMath;
import com.oriondev.moneywallet.model.OverviewChangeMath;
import com.oriondev.moneywallet.model.OverviewData;
import com.oriondev.moneywallet.model.OverviewSetting;
import com.oriondev.moneywallet.model.PeriodMoney;
import com.oriondev.moneywallet.picker.OverviewSettingPicker;
import com.oriondev.moneywallet.storage.database.Contract;
import com.oriondev.moneywallet.storage.database.DataContentProvider;
import com.oriondev.moneywallet.storage.preference.CurrentWalletController;
import com.oriondev.moneywallet.storage.preference.PreferenceManager;
import com.oriondev.moneywallet.ui.activity.PeriodDetailActivity;
import com.oriondev.moneywallet.ui.adapter.pager.OverviewChartViewPagerAdapter;
import com.oriondev.moneywallet.ui.adapter.recycler.OverviewItemAdapter;
import com.oriondev.moneywallet.ui.fragment.base.SinglePanelFragment;
import com.oriondev.moneywallet.utils.DateFormatter;
import com.oriondev.moneywallet.utils.DateUtils;
import com.oriondev.moneywallet.utils.MoneyFormatter;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by andrea on 17/08/18.
 */
public class OverviewSinglePanelFragment extends SinglePanelFragment implements OverviewSettingPicker.Controller, LoaderManager.LoaderCallbacks<OverviewData>, OverviewItemAdapter.Controller, CurrentWalletController {

    private static final int LOADER_OVERVIEW_DATA = 34848;

    private static final String TAG_SETTING_PICKER = "OverviewSinglePanelFragment::Tag::OverviewSettingPicker";

    private OverviewChartViewPagerAdapter mViewPagerAdapter;
    private OverviewItemAdapter mRecyclerViewAdapter;

    private OverviewSettingPicker mOverviewSettingPicker;

    private TextView mIncomeSummaryTextView;
    private TextView mExpenseSummaryTextView;
    private TextView mNetSummaryTextView;
    private TextView mBalanceSummaryTextView;
    private View mSavingsRateContainer;
    private TextView mSavingsRateTextView;
    private View mComparisonContainer;
    private TextView mIncomeChangeTextView;
    private TextView mExpenseChangeTextView;
    private TextView mIncomeChangePercentTextView;
    private TextView mExpenseChangePercentTextView;
    private TextView mNetChangeTextView;
    private TextView mNetChangePercentTextView;
    private View mTopExpensesContainer;
    private TextView mTopExpensesTextView;
    private View mUpcomingEventsContainer;
    private TextView mUpcomingEventsTextView;
    private View mUpcomingDebtsContainer;
    private TextView mUpcomingDebtsTextView;

    private BroadcastReceiver mBroadcastReceiver;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mBroadcastReceiver = PreferenceManager.registerCurrentWalletObserver(context, this);
    }

    @Override
    public void onDetach() {
        PreferenceManager.unregisterCurrentWalletObserver(getActivity(), mBroadcastReceiver);
        super.onDetach();
    }

    @Override
    protected void onCreatePanelView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_panel_overview, parent, true);
        ViewPager viewPager = view.findViewById(R.id.chart_view_pager);
        RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
        mIncomeSummaryTextView = view.findViewById(R.id.summary_income_value);
        mExpenseSummaryTextView = view.findViewById(R.id.summary_expense_value);
        mNetSummaryTextView = view.findViewById(R.id.summary_net_value);
        mBalanceSummaryTextView = view.findViewById(R.id.summary_balance_value);
        mSavingsRateContainer = view.findViewById(R.id.summary_savings_rate_container);
        mSavingsRateTextView = view.findViewById(R.id.summary_savings_rate_value);
        mComparisonContainer = view.findViewById(R.id.summary_comparison_container);
        mIncomeChangeTextView = view.findViewById(R.id.summary_income_change_value);
        mExpenseChangeTextView = view.findViewById(R.id.summary_expense_change_value);
        mIncomeChangePercentTextView = view.findViewById(R.id.summary_income_change_percent);
        mExpenseChangePercentTextView = view.findViewById(R.id.summary_expense_change_percent);
        mNetChangeTextView = view.findViewById(R.id.summary_net_change_value);
        mNetChangePercentTextView = view.findViewById(R.id.summary_net_change_percent);
        mTopExpensesContainer = view.findViewById(R.id.summary_top_expenses_container);
        mTopExpensesTextView = view.findViewById(R.id.summary_top_expenses_value);
        mUpcomingEventsContainer = view.findViewById(R.id.summary_upcoming_events_container);
        mUpcomingEventsTextView = view.findViewById(R.id.summary_upcoming_events_value);
        mUpcomingDebtsContainer = view.findViewById(R.id.summary_upcoming_debts_container);
        mUpcomingDebtsTextView = view.findViewById(R.id.summary_upcoming_debts_value);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        // initialize adapters
        mViewPagerAdapter = new OverviewChartViewPagerAdapter();
        mRecyclerViewAdapter = new OverviewItemAdapter(this);
        // attach adapters
        viewPager.setAdapter(mViewPagerAdapter);
        recyclerView.setAdapter(mRecyclerViewAdapter);
        // initialize picker
        FragmentManager fragmentManager = getChildFragmentManager();
        mOverviewSettingPicker = OverviewSettingPicker.createPicker(fragmentManager, TAG_SETTING_PICKER);
    }

    @Override
    protected int getTitleRes() {
        return R.string.menu_overview;
    }

    @Override
    protected boolean showsCurrentWallet() {
        return true;
    }

    @MenuRes
    protected int onInflateMenu() {
        return R.menu.menu_overview_fragment;
    }

    @Override
    protected boolean isFloatingActionButtonEnabled() {
        return false;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.advanced_settings) {
            mOverviewSettingPicker.showPicker();
        }
        return false;
    }

    @Override
    public void onOverviewSettingChanged(String tag, OverviewSetting overviewSetting) {
        LoaderManager.getInstance(this).restartLoader(LOADER_OVERVIEW_DATA, null, this);
    }

    @NonNull
    @Override
    public Loader<OverviewData> onCreateLoader(int id, Bundle args) {
        OverviewSetting setting = mOverviewSettingPicker.getCurrentSettings();
        return new OverviewDataLoader(getActivity(), setting);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<OverviewData> loader, OverviewData data) {
        mViewPagerAdapter.setData(data);
        mRecyclerViewAdapter.setData(data);
        MoneyFormatter formatter = MoneyFormatter.getInstance();
        formatter.setDivider("\n");
        mIncomeSummaryTextView.setText(formatter.getTintedString(
                data.getTotalIncomes(), MoneyFormatter.TintMode.INCOME));
        mExpenseSummaryTextView.setText(formatter.getTintedString(
                data.getTotalExpenses(), MoneyFormatter.TintMode.EXPENSE));
        mNetSummaryTextView.setText(formatter.getTintedString(data.getTotalNetIncomes()));
        mBalanceSummaryTextView.setText(formatter.getTintedString(data.getTotalBalance()));
        updateSavingsRate(data.getTotalIncomes(), data.getTotalExpenses());
        StringBuilder topExpenses = new StringBuilder();
        for (com.oriondev.moneywallet.model.OverviewExpenseCategory category : data.getTopExpenseCategories()) {
            if (topExpenses.length() > 0) {
                topExpenses.append('\n');
            }
            BigDecimal share = OverviewChangeMath.expenseShare(
                    category.getAmount(), data.getTotalExpenses().getMoney(category.getCurrency()));
            String shareText = share == null ? getString(R.string.overview_savings_rate_unavailable)
                    : getString(R.string.overview_comparison_percent_value, "",
                            NumberFormat.getNumberInstance().format(share));
            topExpenses.append(getString(R.string.overview_top_expense_item,
                    category.getCurrency(), category.getName(),
                    formatter.getNotTintedString(new com.oriondev.moneywallet.model.Money(
                            category.getCurrency(), category.getAmount())), shareText));
        }
        mTopExpensesTextView.setText(topExpenses.toString());
        mTopExpensesContainer.setVisibility(topExpenses.length() > 0 ? View.VISIBLE : View.GONE);
        updateUpcomingEvents();
        updateUpcomingDebts();
        // PeriodMoney follows the selected chart filter. The comparison shows both income and
        // expense changes, so it is only meaningful when the chart includes the complete net flow.
        OverviewSetting currentSetting = mOverviewSettingPicker.getCurrentSettings();
        boolean hasCompletePeriodData = currentSetting.getType() == OverviewSetting.Type.CASH_FLOW
                && currentSetting.getCashFlow() == OverviewSetting.CashFlow.NET_INCOMES;
        boolean hasPreviousPeriod = data.getPeriodCount() > 1 && hasCompletePeriodData;
        mComparisonContainer.setVisibility(hasPreviousPeriod ? View.VISIBLE : View.GONE);
        if (hasPreviousPeriod) {
            formatter.applyNotTinted(mIncomeChangeTextView, data.getLatestIncomeChange());
            formatter.applyNotTinted(mExpenseChangeTextView, data.getLatestExpenseChange());
            PeriodMoney previous = data.getPeriodMoney(data.getPeriodCount() - 2);
            PeriodMoney latest = data.getPeriodMoney(data.getPeriodCount() - 1);
            mIncomeChangePercentTextView.setText(formatPercentageChanges(
                    previous.getIncomes(), latest.getIncomes()));
            mExpenseChangePercentTextView.setText(formatPercentageChanges(
                    previous.getExpenses(), latest.getExpenses()));
            Money netChange = new Money();
            netChange.addMoney(latest.getNetIncomes());
            netChange.removeMoney(previous.getNetIncomes());
            formatter.applyNotTinted(mNetChangeTextView, netChange);
            mNetChangePercentTextView.setText(formatPercentageChanges(
                    previous.getNetIncomes(), latest.getNetIncomes()));
        }
    }

    /**
     * Shows the next three events on the overview without changing or copying event records.
     */
    private void updateUpcomingEvents() {
        if (getActivity() == null || mUpcomingEventsContainer == null) {
            return;
        }
        Uri uri = DataContentProvider.CONTENT_EVENTS;
        String[] projection = new String[] {
                Contract.Event.NAME,
                Contract.Event.END_DATE
        };
        Cursor cursor = null;
        StringBuilder upcoming = new StringBuilder();
        try {
            cursor = getActivity().getContentResolver().query(
                    uri,
                    projection,
                    "DATE(" + Contract.Event.END_DATE + ") >= DATE('now')",
                    null,
                    Contract.Event.END_DATE + " ASC");
            if (cursor != null && cursor.moveToFirst()) {
                int shown = 0;
                do {
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(Contract.Event.NAME));
                    String endDateValue = cursor.getString(cursor.getColumnIndexOrThrow(Contract.Event.END_DATE));
                    java.util.Date endDate = DateUtils.getDateFromSQLDateString(endDateValue);
                    if (upcoming.length() > 0) {
                        upcoming.append('\n');
                    }
                    upcoming.append(getString(R.string.overview_upcoming_event_item,
                            name, DateFormatter.getFormattedDate(endDate)));
                    shown++;
                } while (shown < 3 && cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        mUpcomingEventsTextView.setText(upcoming.toString());
        mUpcomingEventsContainer.setVisibility(upcoming.length() > 0 ? View.VISIBLE : View.GONE);
    }

    /**
     * Shows the next three unsettled debt or credit due dates without changing stored records.
     */
    private void updateUpcomingDebts() {
        if (getActivity() == null || mUpcomingDebtsContainer == null) {
            return;
        }
        String[] projection = new String[] {
                Contract.Debt.DESCRIPTION,
                Contract.Debt.EXPIRATION_DATE,
                Contract.Debt.MONEY,
                Contract.Debt.PROGRESS,
                Contract.Debt.WALLET_CURRENCY
        };
        Cursor cursor = null;
        StringBuilder upcoming = new StringBuilder();
        try {
            cursor = getActivity().getContentResolver().query(
                    DataContentProvider.CONTENT_DEBTS,
                    projection,
                    "DATE(" + Contract.Debt.EXPIRATION_DATE + ") >= DATE('now', 'localtime')"
                            + " AND " + Contract.Debt.ARCHIVED + " = 0"
                            + " AND " + Contract.Debt.MONEY + " > ABS(" + Contract.Debt.PROGRESS + ")",
                    null,
                    Contract.Debt.EXPIRATION_DATE + " ASC");
            if (cursor != null && cursor.moveToFirst()) {
                int shown = 0;
                do {
                    String description = cursor.getString(
                            cursor.getColumnIndexOrThrow(Contract.Debt.DESCRIPTION));
                    String expirationValue = cursor.getString(
                            cursor.getColumnIndexOrThrow(Contract.Debt.EXPIRATION_DATE));
                    long target = cursor.getLong(cursor.getColumnIndexOrThrow(Contract.Debt.MONEY));
                    long progress = MoneyProgressMath.safeAbs(
                            cursor.getLong(cursor.getColumnIndexOrThrow(Contract.Debt.PROGRESS)));
                    long remaining = MoneyProgressMath.remaining(target, progress);
                    String currencyCode = cursor.getString(
                            cursor.getColumnIndexOrThrow(Contract.Debt.WALLET_CURRENCY));
                    String remainingText = MoneyFormatter.getInstance().getNotTintedString(
                            CurrencyManager.getCurrency(currencyCode), remaining);
                    java.util.Date expirationDate = DateUtils.getDateFromSQLDateString(expirationValue);
                    if (upcoming.length() > 0) {
                        upcoming.append('\n');
                    }
                    upcoming.append(getString(R.string.overview_upcoming_debt_item,
                            description, DateFormatter.getFormattedDate(expirationDate), remainingText));
                    shown++;
                } while (shown < 3 && cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        mUpcomingDebtsTextView.setText(upcoming.toString());
        mUpcomingDebtsContainer.setVisibility(upcoming.length() > 0 ? View.VISIBLE : View.GONE);
    }

    private void updateSavingsRate(Money incomes, Money expenses) {
        Set<String> currencySet = new LinkedHashSet<>();
        for (String currency : incomes.getCurrencies()) {
            if (currency != null) currencySet.add(currency);
        }
        for (String currency : expenses.getCurrencies()) {
            if (currency != null) currencySet.add(currency);
        }
        List<String> currencies = new ArrayList<>(currencySet);
        Collections.sort(currencies);
        if (currencies.isEmpty()) {
            mSavingsRateContainer.setVisibility(View.GONE);
            return;
        }

        NumberFormat numberFormat = NumberFormat.getNumberInstance();
        numberFormat.setMinimumFractionDigits(1);
        numberFormat.setMaximumFractionDigits(1);
        StringBuilder result = new StringBuilder();
        for (String currency : currencies) {
            if (result.length() > 0) result.append('\n');
            BigDecimal rate = OverviewChangeMath.savingsRate(
                    incomes.getMoney(currency), expenses.getMoney(currency));
            String value = rate == null
                    ? getString(R.string.overview_savings_rate_unavailable)
                    : getString(R.string.overview_comparison_percent_value,
                            rate.signum() < 0 ? "−" : (rate.signum() > 0 ? "+" : ""),
                            numberFormat.format(rate.abs()));
            result.append(getString(R.string.overview_savings_rate_item, currency, value));
        }
        mSavingsRateTextView.setText(result.toString());
        mSavingsRateContainer.setVisibility(View.VISIBLE);
    }

    private String formatPercentageChanges(Money previous, Money current) {
        Set<String> currencySet = new LinkedHashSet<>();
        for (String currency : previous.getCurrencies()) {
            if (currency != null) currencySet.add(currency);
        }
        for (String currency : current.getCurrencies()) {
            if (currency != null) currencySet.add(currency);
        }
        List<String> currencies = new ArrayList<>(currencySet);
        Collections.sort(currencies);

        NumberFormat numberFormat = NumberFormat.getNumberInstance();
        numberFormat.setMinimumFractionDigits(1);
        numberFormat.setMaximumFractionDigits(1);
        StringBuilder result = new StringBuilder();
        for (String currency : currencies) {
            if (result.length() > 0) result.append('\n');
            BigDecimal change = OverviewChangeMath.percentageChange(
                    previous.getMoney(currency), current.getMoney(currency));
            String value;
            if (change == null) {
                value = getString(R.string.overview_comparison_new);
            } else {
                String sign = change.signum() > 0 ? "+" : (change.signum() < 0 ? "−" : "");
                value = getString(R.string.overview_comparison_percent_value,
                        sign, numberFormat.format(change.abs()));
            }
            result.append(getString(R.string.overview_comparison_percent_item, currency, value));
        }
        return result.toString();
    }

    @Override
    public void onLoaderReset(@NonNull Loader<OverviewData> loader) {
        // nothing to release
    }

    @Override
    public void onPeriodClick(PeriodMoney periodMoney) {
        Intent intent = new Intent(getActivity(), PeriodDetailActivity.class);
        intent.putExtra(PeriodDetailActivity.START_DATE, periodMoney.getStartDate());
        intent.putExtra(PeriodDetailActivity.END_DATE, periodMoney.getEndDate());
        startActivity(intent);
    }

    @Override
    public void onCurrentWalletChanged(long walletId) {
        LoaderManager.getInstance(this).restartLoader(LOADER_OVERVIEW_DATA, null, this);
    }
}