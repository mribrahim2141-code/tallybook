package com.oriondev.moneywallet.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.oriondev.moneywallet.storage.preference.PreferenceManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MoneyFormatterPrivacyTest {

    @Before
    fun setUp() {
        PreferenceManager.initialize(ApplicationProvider.getApplicationContext<Context>())
        PreferenceManager.setBalancesHiddenEnabled(false)
    }

    @After
    fun tearDown() {
        PreferenceManager.setBalancesHiddenEnabled(false)
    }

    @Test
    fun masksDisplayedAmountsWhenPrivacyModeIsEnabled() {
        PreferenceManager.setBalancesHiddenEnabled(true)

        assertEquals("••••", MoneyFormatter.getInstance().getNotTintedString(null, 12345L))
    }

    @Test
    fun keepsTransactionAmountEntryVisibleWhenPrivacyModeIsEnabled() {
        PreferenceManager.setBalancesHiddenEnabled(true)

        assertNotEquals("••••", MoneyFormatter.getInstance().getNotTintedStringForInput(null, 12345L))
    }

    @Test
    fun showsDisplayedAmountsAgainWhenPrivacyModeIsDisabled() {
        PreferenceManager.setBalancesHiddenEnabled(false)

        assertNotEquals("••••", MoneyFormatter.getInstance().getNotTintedString(null, 12345L))
    }
}
