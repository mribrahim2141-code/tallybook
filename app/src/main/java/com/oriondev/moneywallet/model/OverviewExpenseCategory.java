package com.oriondev.moneywallet.model;

/** One category's expense total in exactly one currency. */
public final class OverviewExpenseCategory {
    private final long mCategoryId;
    private final String mName;
    private final String mCurrency;
    private long mAmount;

    public OverviewExpenseCategory(long categoryId, String name, String currency, long amount) {
        mCategoryId = categoryId;
        mName = name;
        mCurrency = currency;
        mAmount = amount;
    }

    public long getCategoryId() {
        return mCategoryId;
    }

    public String getName() {
        return mName;
    }

    public String getCurrency() {
        return mCurrency;
    }

    public long getAmount() {
        return mAmount;
    }

    public void addAmount(long amount) {
        mAmount += amount;
    }
}
