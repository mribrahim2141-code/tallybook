package com.oriondev.moneywallet.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class OverviewChangeMathTest {

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
