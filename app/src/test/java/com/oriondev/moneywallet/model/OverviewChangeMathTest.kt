package com.oriondev.moneywallet.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class OverviewChangeMathTest {

    @Test
    fun calculatesSavingsRateAndHandlesNoIncome() {
        assertEquals(BigDecimal("25.0"), OverviewChangeMath.savingsRate(400L, 300L))
        assertEquals(BigDecimal("-20.0"), OverviewChangeMath.savingsRate(100L, 120L))
        assertNull(OverviewChangeMath.savingsRate(0L, 50L))
        assertNull(OverviewChangeMath.savingsRate(-10L, 5L))
    }

    @Test
    fun calculatesSavingsRateWithoutLongOverflow() {
        assertEquals(BigDecimal("0.0"),
            OverviewChangeMath.savingsRate(Long.MAX_VALUE, Long.MAX_VALUE))
        assertEquals(BigDecimal("100.0"),
            OverviewChangeMath.savingsRate(Long.MAX_VALUE, 0L))
    }

    @Test
    fun calculatesCategoryShareAndHandlesMissingTotals() {
        assertEquals(BigDecimal("25.0"), OverviewChangeMath.expenseShare(250L, 1000L))
        assertEquals(BigDecimal("100.0"), OverviewChangeMath.expenseShare(1000L, 1000L))
        assertEquals(BigDecimal("0.0"), OverviewChangeMath.expenseShare(0L, 1000L))
        assertNull(OverviewChangeMath.expenseShare(10L, 0L))
        assertNull(OverviewChangeMath.expenseShare(10L, -1L))
        assertNull(OverviewChangeMath.expenseShare(-1L, 100L))
    }

    @Test
    fun calculatesCategoryShareWithoutLongOverflow() {
        assertEquals(BigDecimal("100.0"),
            OverviewChangeMath.expenseShare(Long.MAX_VALUE, Long.MAX_VALUE))
    }

    @Test
    fun calculatesPositiveNegativeAndUnchangedPercentages() {
        assertEquals(BigDecimal("50.0"), OverviewChangeMath.percentageChange(100L, 150L))
        assertEquals(BigDecimal("-50.0"), OverviewChangeMath.percentageChange(100L, 50L))
        assertEquals(BigDecimal("0.0"), OverviewChangeMath.percentageChange(0L, 0L))
    }

    @Test
    fun identifiesGrowthFromAZeroBaselineAsNewInsteadOfDividingByZero() {
        assertNull(OverviewChangeMath.percentageChange(0L, 25L))
    }

    @Test
    fun handlesLongBoundariesWithoutOverflow() {
        assertEquals(BigDecimal("200.0"),
            OverviewChangeMath.percentageChange(Long.MIN_VALUE, Long.MAX_VALUE))
        assertEquals(BigDecimal("-200.0"),
            OverviewChangeMath.percentageChange(Long.MAX_VALUE, Long.MIN_VALUE))
    }

    @Test
    fun supportsNegativeBaselinesUsingTheirMagnitude() {
        assertEquals(BigDecimal("50.0"), OverviewChangeMath.percentageChange(-100L, -50L))
    }
}
