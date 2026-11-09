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

package com.oriondev.moneywallet.model

import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.RadarData

/**
 * The chart series and the currency-aware totals used by the overview summary.
 */
class OverviewData {

    private val mBarData: BarData?
    private val mLineData: LineData?
    private val mRadarData: RadarData?
    private val mPeriodMoneyList: MutableList<PeriodMoney>
    private val mTotalIncomes: Money
    private val mTotalExpenses: Money
    private val mTotalNetIncomes: Money
    private val mTotalTransfers: Money
    private val mTotalBalance: Money

    constructor(barData: BarData?, lineData: LineData?, radarData: RadarData?, periodMoneyList: MutableList<PeriodMoney>) :
            this(barData, lineData, radarData, periodMoneyList, Money(), Money(), Money(), Money(), Money())

    constructor(
            barData: BarData?,
            lineData: LineData?,
            radarData: RadarData?,
            periodMoneyList: MutableList<PeriodMoney>,
            totalIncomes: Money,
            totalExpenses: Money,
            totalNetIncomes: Money,
            totalTransfers: Money,
            totalBalance: Money
    ) {
        mBarData = barData
        mLineData = lineData
        mRadarData = radarData
        mPeriodMoneyList = periodMoneyList
        mTotalIncomes = totalIncomes
        mTotalExpenses = totalExpenses
        mTotalNetIncomes = totalNetIncomes
        mTotalTransfers = totalTransfers
        mTotalBalance = totalBalance
    }

    fun getBarData(): BarData? = mBarData

    fun getLineData(): LineData? = mLineData

    fun getRadarData(): RadarData? = mRadarData

    fun getPeriodMoney(index: Int): PeriodMoney = mPeriodMoneyList[index]

    fun getPeriodCount(): Int = mPeriodMoneyList.size

    fun getTotalIncomes(): Money = mTotalIncomes

    fun getTotalExpenses(): Money = mTotalExpenses

    fun getTotalNetIncomes(): Money = mTotalNetIncomes

    fun getTotalTransfers(): Money = mTotalTransfers

    fun getTotalBalance(): Money = mTotalBalance

    /**
     * Difference between the newest and preceding visible periods, kept separately by currency.
     * An empty result is returned when the overview contains fewer than two periods.
     */
    fun getLatestIncomeChange(): Money {
        val change = Money()
        if (mPeriodMoneyList.size >= 2) {
            val current = mPeriodMoneyList[mPeriodMoneyList.size - 1]
            val previous = mPeriodMoneyList[mPeriodMoneyList.size - 2]
            change.addMoney(current.getIncomes())
            change.removeMoney(previous.getIncomes())
        }
        return change
    }

    fun getLatestExpenseChange(): Money {
        val change = Money()
        if (mPeriodMoneyList.size >= 2) {
            val current = mPeriodMoneyList[mPeriodMoneyList.size - 1]
            val previous = mPeriodMoneyList[mPeriodMoneyList.size - 2]
            change.addMoney(current.getExpenses())
            change.removeMoney(previous.getExpenses())
        }
        return change
    }
}
