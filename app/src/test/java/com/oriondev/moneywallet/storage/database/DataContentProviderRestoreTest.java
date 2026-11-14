/*
 * Copyright (c) 2026.
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

package com.oriondev.moneywallet.storage.database;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.oriondev.moneywallet.storage.preference.PreferenceManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class DataContentProviderRestoreTest {

    @Test
    public void aRestoreClearsTheDefaultWallet() {
        Context context = ApplicationProvider.getApplicationContext();
        PreferenceManager.initialize(context);
        TestDatabases.useFreshDatabase(context);
        PreferenceManager.setDefaultWallet(7L);
        DataContentProvider.notifyDatabaseIsChanged(context);
        assertEquals(PreferenceManager.NO_DEFAULT_WALLET, PreferenceManager.getDefaultWallet());
    }
    @Test
    public void writesFromOtherThreadsWaitUntilRestoreFinishes() throws Exception {
        final Context context = ApplicationProvider.getApplicationContext();
        final java.util.concurrent.ExecutorService executor =
                java.util.concurrent.Executors.newSingleThreadExecutor();
        final java.util.concurrent.CountDownLatch attemptingWrite =
                new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.Future<Boolean> write;

        DataContentProvider.beginRestoreBarrier();
        try {
            write = executor.submit(() -> {
                attemptingWrite.countDown();
                return SQLDatabase.inSharedTransaction(context, database -> true);
            });
            org.junit.Assert.assertTrue(attemptingWrite.await(5,
                    java.util.concurrent.TimeUnit.SECONDS));
            // The write must not enter the shared database while the restore owns the barrier.
            org.junit.Assert.assertFalse(write.isDone());
        } finally {
            DataContentProvider.endRestoreBarrier();
        }

        try {
            org.junit.Assert.assertTrue(write.get(5, java.util.concurrent.TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

}
