package com.oriondev.moneywallet.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Date

class OverviewDataTest {

    @Test
    fun latestPeriodChangesAreCalculatedPerCurrencyWithoutMutatingSourcePeriods() {
        val previous = PeriodMoney(Date(0), Date(1))
        previous.addIncome("SAR", 100L)
        previous.addExpense("SAR", 40L)
        previous.addIncome("USD", 10L)

        val latest = PeriodMoney(Date(2), Date(3))
        latest.addIncome("SAR", 150L)
        latest.addExpense("SAR", 60L)
        latest.addIncome("USD", 15L)

        val periods = arrayListOf(previous, latest)
        val data = OverviewData(null, null, null, periods)

        assertEquals(50L, data.getLatestIncomeChange().getMoney("SAR"))
        assertEquals(5L, data.getLatestIncomeChange().getMoney("USD"))
        assertEquals(20L, data.getLatestExpenseChange().getMoney("SAR"))
        assertEquals(100L, previous.getIncomes().getMoney("SAR"))
        assertEquals(150L, latest.getIncomes().getMoney("SAR"))
    }

    @Test
    fun comparisonIsZeroWhenThereIsNoPreviousPeriod() {
        val onlyPeriod = PeriodMoney(Date(0), Date(1))
        onlyPeriod.addIncome("SAR", 75L)
        val data = OverviewData(null, null, null, arrayListOf(onlyPeriod))

        assertEquals(0L, data.getLatestIncomeChange().getMoney("SAR"))
        assertEquals(0L, data.getLatestExpenseChange().getMoney("SAR"))
    }
}
