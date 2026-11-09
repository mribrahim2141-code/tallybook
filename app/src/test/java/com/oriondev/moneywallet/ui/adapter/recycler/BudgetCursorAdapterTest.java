package com.oriondev.moneywallet.ui.adapter.recycler;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BudgetCursorAdapterTest {

    @Test
    public void usagePercentHandlesNormalAndNearLimitValues() {
        assertEquals(0, BudgetCursorAdapter.calculateUsagePercent(10000L, 0L));
        assertEquals(42, BudgetCursorAdapter.calculateUsagePercent(10000L, 4200L));
        assertEquals(80, BudgetCursorAdapter.calculateUsagePercent(10000L, 8000L));
        assertEquals(100, BudgetCursorAdapter.calculateUsagePercent(10000L, 10000L));
    }

    @Test
    public void usagePercentCapsOverBudgetAndHandlesInvalidTarget() {
        assertEquals(100, BudgetCursorAdapter.calculateUsagePercent(10000L, 12500L));
        assertEquals(0, BudgetCursorAdapter.calculateUsagePercent(0L, 100L));
        assertEquals(0, BudgetCursorAdapter.calculateUsagePercent(-1L, 100L));
    }
}
