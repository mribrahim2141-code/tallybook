package com.oriondev.moneywallet.model

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Percentage change between two values from the same currency.
 *
 * A zero baseline has no meaningful percentage unless the current value is also zero.
 * BigDecimal keeps the subtraction and division safe for the full Long range.
 */
object OverviewChangeMath {

    /**
     * Share of income left after expenses, calculated independently for each currency.
     * A percentage is undefined when there is no income in that currency.
     */
    @JvmStatic
    fun savingsRate(income: Long, expenses: Long): BigDecimal? {
        if (income <= 0L) return null
        val net = BigDecimal.valueOf(income).subtract(BigDecimal.valueOf(expenses))
        return net.multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(income), 1, RoundingMode.HALF_UP)
    }

    @JvmStatic
    fun percentageChange(previous: Long, current: Long): BigDecimal? {
        if (previous == 0L) {
            return if (current == 0L) BigDecimal.ZERO.setScale(1) else null
        }

        val difference = BigDecimal.valueOf(current).subtract(BigDecimal.valueOf(previous))
        return difference.multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(previous).abs(), 1, RoundingMode.HALF_UP)
    }
}
