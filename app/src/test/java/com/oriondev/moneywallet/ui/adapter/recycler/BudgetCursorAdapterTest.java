package com.oriondev.moneywallet.ui.adapter.recycler;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;

public class BudgetCursorAdapterTest {

    @Test
    public void projectedUsageScalesCurrentPaceAcrossTheBudgetPeriod() {
        Date start = new Date(10L * 86400000L);
        Date end = new Date(19L * 86400000L);
        Date today = new Date(14L * 86400000L);
        assertEquals(2000L, BudgetCursorAdapter.calculateProjectedUsage(1000L, start, end, today));
    }

    @Test
    public void projectedUsageRejectsDatesOutsideTheBudgetPeriod() {
        Date start = new Date(10L * 86400000L);
        Date end = new Date(19L * 86400000L);
        assertEquals(-1L, BudgetCursorAdapter.calculateProjectedUsage(1000L, start, end, new Date(9L * 86400000L)));
        assertEquals(-1L, BudgetCursorAdapter.calculateProjectedUsage(1000L, start, end, new Date(20L * 86400000L)));
    }

    @Test
    public void usagePercentHandlesNormalAndNearLimitValues() {
        assertEquals(0, BudgetCursorAdapter.calculateUsagePercent(10000L, 0L));
        assertEquals(42, BudgetCursorAdapter.calculateUsagePercent(10000L, 4200L));
        assertEquals(80, BudgetCursorAdapter.calculateUsagePercent(10000L, 8000L));
        assertEquals(100, BudgetCursorAdapter.calculateUsagePercent(10000L, 10000L));
    }

    @Test
    public void usagePercentAllowsOverBudgetAndHandlesInvalidTarget() {
        assertEquals(125, BudgetCursorAdapter.calculateUsagePercent(10000L, 12500L));
        assertEquals(0, BudgetCursorAdapter.calculateUsagePercent(0L, 100L));
        assertEquals(0, BudgetCursorAdapter.calculateUsagePercent(-1L, 100L));
    }
}
