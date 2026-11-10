package com.oriondev.moneywallet.ui.adapter.recycler;

import com.oriondev.moneywallet.utils.MoneyProgressMath;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MoneyProgressMathTest {

    @Test
    public void savingsBalanceSaturatesInsteadOfWrapping() {
        assertEquals(Long.MAX_VALUE, MoneyProgressMath.saturatedAdd(Long.MAX_VALUE, 1L));
        assertEquals(Long.MIN_VALUE, MoneyProgressMath.saturatedAdd(Long.MIN_VALUE, -1L));
        assertEquals(150L, MoneyProgressMath.saturatedAdd(100L, 50L));
    }

    @Test
    public void savingsNeededAmountDoesNotOverflow() {
        assertEquals(0L, MoneyProgressMath.amountNeeded(100L, Long.MAX_VALUE));
        assertEquals(Long.MAX_VALUE, MoneyProgressMath.amountNeeded(Long.MAX_VALUE, Long.MIN_VALUE));
        assertEquals(40L, MoneyProgressMath.amountNeeded(100L, 60L));
    }

    @Test
    public void debtRemainingHandlesMinimumLongValue() {
        assertEquals(Long.MAX_VALUE, MoneyProgressMath.safeAbs(Long.MIN_VALUE));
        assertEquals(Long.MAX_VALUE, MoneyProgressMath.remaining(Long.MIN_VALUE, 0L));
        assertEquals(40L, MoneyProgressMath.remaining(100L, 60L));
        assertEquals(0L, MoneyProgressMath.remaining(100L, 120L));
    }

    @Test
    public void subtractionSaturatesAtBothBounds() {
        assertEquals(Long.MAX_VALUE, MoneyProgressMath.saturatedSubtract(Long.MAX_VALUE, -1L));
        assertEquals(Long.MIN_VALUE, MoneyProgressMath.saturatedSubtract(Long.MIN_VALUE, 1L));
    }
}
