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

package com.oriondev.moneywallet.storage.database.backup;

import android.content.ContentResolver;
import android.content.Context;
import androidx.annotation.NonNull;

import com.oriondev.moneywallet.storage.database.DataContentProvider;
import com.oriondev.moneywallet.storage.database.ImportException;
import com.oriondev.moneywallet.storage.database.SQLDatabaseImporter;
import com.oriondev.moneywallet.storage.preference.PreferenceManager;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

/**
 * Created by andrea on 25/10/18.
 */
public abstract class AbstractBackupImporter {

    /**
     * The aside copy the design before this one made, kept only long enough to roll a failed
     * restore back. Nothing writes it any more.
     *
     * It is still deleted here because deleting it was the job of the method that made it, which
     * this design removed along with the rollback. A user whose process died part way through a
     * restore on an older build has one of these sitting in the databases folder, the size of
     * their whole ledger, and after that removal nothing in the app would ever collect it.
     */
    private static final String ABANDONED_BACKUP_COPY = "database.bk";

    private final Context mContext;
    private final File mBackupFile;

    /*package-local*/ AbstractBackupImporter(Context context, File backupFile) {
        mContext = context;
        mBackupFile = backupFile;
    }

    /**
     * This method imports all the data included in the backup file inside the main database.
     *
     * The import is built in a staging file beside the live database and put in place only once
     * every row is in, so a failure leaves the live database untouched. Nothing has to be rolled
     * back and no copy of the ledger is kept aside while this runs, which is what the design
     * before this one needed and this one does not.
     *
     * @param temporaryFolder folder where temporary data can be stored.
     * @param databaseFolder folder where the database is located.
     * @throws ImportException if an error occur while importing the backup file.
     *
     * Only this thread is sent to the staging file. The rest of the app keeps reading and
     * writing the live ledger while the import runs, so a screen open during a restore shows
     * the old data until the swap and then redraws, instead of watching rows appear.
     *
     * A write made from another thread while this runs therefore goes into the ledger that is
     * about to be replaced, and is lost. That is what this app already did and it is deliberate.
     * Sending every thread to the staging file instead would keep such a write, by putting it in
     * a half imported database whose ids name different rows with foreign keys on, and it would
     * make the live file name movable, which costs a lock on every read in the app. Refusing
     * those writes was also built and reverted. The refusal reaches the caller as an exception,
     * no caller on any of these write paths catches it, and injecting that throw into the
     * recurrence job on an emulator ended the run with FATAL EXCEPTION on the AsyncTask that
     * JobIntentService dispatches on, with the process gone. Every component here shares that
     * one process, so the restore died with it.
     *
     * What this does NOT cover is the attachments. BackupHandlerIntentService runs importDatabase
     * and then importAttachments, and importAttachments empties the attachment folder before it
     * extracts anything, so a failure there leaves the new ledger live and the files gone. True
     * before this change and not made worse by it. No sentence anywhere may call the restore
     * atomic without naming it.
     */
    private File mPreparedDatabase;
    private File mPreparedDatabaseFolder;
    private File mPreparedAttachmentFolder;
    private File mStagedAttachmentFolder;
    private File mPreviousAttachmentFolder;
    private boolean mHadPreviousAttachments;
    private boolean mAttachmentsCommitted;

    public void importDatabase(@NonNull File temporaryFolder, @NonNull File databaseFolder) throws ImportException {
        prepareDatabase(temporaryFolder, databaseFolder);
        commitDatabase();
        finishDatabaseRestore();
    }

    /**
     * Build the replacement database without making it live. The caller can prepare the
     * attachment directory too, then commit both resources with rollback if either commit fails.
     */
    public void prepareDatabase(@NonNull File temporaryFolder, @NonNull File databaseFolder) throws ImportException {
        File staged = new File(databaseFolder, SQLDatabaseImporter.STAGING_DATABASE_NAME);
        try {
            importDatabase(temporaryFolder);
            DataContentProvider.closeStagingDatabase();
            mPreparedDatabase = staged;
            mPreparedDatabaseFolder = databaseFolder;
        } catch (ImportException | RuntimeException e) {
            discardStagedDatabase(staged);
            throw e;
        } finally {
            DataContentProvider.closeStagingDatabase();
        }
    }

    /** Promote a database previously prepared by prepareDatabase. */
    public void commitDatabase() throws ImportException {
        if (mPreparedDatabase == null || mPreparedDatabaseFolder == null) {
            throw new ImportException("No prepared database restore");
        }
        promoteStagedDatabase(mPreparedDatabaseFolder, mPreparedDatabase);
        mPreparedDatabase = null;
        mPreparedDatabaseFolder = null;
    }

    /** Remove a prepared database when another part of the restore could not be prepared. */
    public void discardPreparedDatabase() {
        if (mPreparedDatabase != null) {
            discardStagedDatabase(mPreparedDatabase);
            mPreparedDatabase = null;
            mPreparedDatabaseFolder = null;
        }
    }

    /** Apply post-commit preferences only after the database and attachments are both installed. */
    public void finishDatabaseRestore() {
        PreferenceManager.setCollapsedCategories(Collections.<String>emptySet());
    }

    protected abstract void importDatabase(@NonNull File temporaryFolder) throws ImportException;

    /**
     * Restore attachments into a sibling staging directory first. The current folder is not
     * touched until every entry has been extracted successfully, so a bad archive, wrong
     * password, or full device cannot erase the user's existing attachments.
     *
     * The previous folder is kept until the staging directory has taken its place. If the
     * process dies between the two renames, the next restore recovers that previous folder
     * before starting. Both directories are siblings, so the renames stay on the same volume.
     */
    /**
     * Extract attachments into a sibling staging directory without touching the live folder.
     */
    public void prepareAttachments(@NonNull File attachmentFolder) throws ImportException {
        File parent = attachmentFolder.getAbsoluteFile().getParentFile();
        File staging = new File(parent, attachmentFolder.getName() + ".restore-staging");
        File previous = new File(parent, attachmentFolder.getName() + ".restore-previous");
        try {
            // Recover a folder left aside by an interrupted earlier restore.
            if (previous.exists()) {
                if (attachmentFolder.exists()) {
                    FileUtils.deleteDirectory(previous);
                } else if (!previous.renameTo(attachmentFolder)) {
                    throw new IOException("Cannot recover the previous attachment folder");
                }
            }
            FileUtils.deleteQuietly(staging);
            FileUtils.forceMkdir(staging);
            importAttachmentFiles(staging);
            mPreparedAttachmentFolder = attachmentFolder;
            mStagedAttachmentFolder = staging;
            mPreviousAttachmentFolder = previous;
        } catch (IOException | ImportException e) {
            FileUtils.deleteQuietly(staging);
            throw new ImportException(e.getMessage());
        }
    }

    /**
     * Swap in prepared attachments but retain the old folder until the database commit succeeds.
     */
    public void commitAttachments() throws ImportException {
        if (mPreparedAttachmentFolder == null || mStagedAttachmentFolder == null) {
            throw new ImportException("No prepared attachment restore");
        }
        try {
            mHadPreviousAttachments = mPreparedAttachmentFolder.exists();
            if (mHadPreviousAttachments && !mPreparedAttachmentFolder.renameTo(mPreviousAttachmentFolder)) {
                throw new IOException("Cannot move the current attachment folder");
            }
            if (!mStagedAttachmentFolder.renameTo(mPreparedAttachmentFolder)) {
                throw new IOException("Cannot install restored attachments");
            }
            mStagedAttachmentFolder = null;
            mAttachmentsCommitted = true;
        } catch (IOException e) {
            throw new ImportException(e.getMessage());
        }
    }

    /** Restore the previous attachment folder if the database commit did not complete. */
    public void rollbackAttachments() throws ImportException {
        if (mPreparedAttachmentFolder == null) {
            return;
        }
        try {
            boolean previousExists = mPreviousAttachmentFolder != null && mPreviousAttachmentFolder.exists();
            if (previousExists) {
                if (mPreparedAttachmentFolder.exists()) {
                    FileUtils.deleteDirectory(mPreparedAttachmentFolder);
                }
                if (!mPreviousAttachmentFolder.renameTo(mPreparedAttachmentFolder)) {
                    throw new IOException("Cannot restore the previous attachment folder");
                }
            } else if (mAttachmentsCommitted && !mHadPreviousAttachments && mPreparedAttachmentFolder.exists()) {
                FileUtils.deleteDirectory(mPreparedAttachmentFolder);
            }
        } catch (IOException e) {
            throw new ImportException(e.getMessage());
        } finally {
            clearAttachmentRestoreState();
        }
    }

    /** Discard the retained old folder after both restore components have committed. */
    public void finishAttachmentRestore() {
        if (mPreviousAttachmentFolder != null) {
            FileUtils.deleteQuietly(mPreviousAttachmentFolder);
        }
        clearAttachmentRestoreState();
    }

    /** Existing one-step API retained for legacy callers. */
    public void importAttachments(@NonNull File attachmentFolder) throws ImportException {
        prepareAttachments(attachmentFolder);
        try {
            commitAttachments();
            finishAttachmentRestore();
        } catch (ImportException e) {
            rollbackAttachments();
            throw e;
        } finally {
            if (mStagedAttachmentFolder != null) {
                FileUtils.deleteQuietly(mStagedAttachmentFolder);
            }
        }
    }

    private void clearAttachmentRestoreState() {
        if (mStagedAttachmentFolder != null) {
            FileUtils.deleteQuietly(mStagedAttachmentFolder);
        }
        mPreparedAttachmentFolder = null;
        mStagedAttachmentFolder = null;
        mPreviousAttachmentFolder = null;
        mHadPreviousAttachments = false;
        mAttachmentsCommitted = false;
    }

    protected abstract void importAttachmentFiles(File attachmentFolder) throws IOException, ImportException;

    /*package-local*/ File getBackupFile() {
        return mBackupFile;
    }

    protected ContentResolver getContentResolver() {
        return mContext.getContentResolver();
    }

    /**
     * Sends this thread to the staging file, from here to the end of the import. Each importer
     * calls it once it has opened its backup file and read enough of it to be worth trusting,
     * and before the first row goes in.
     *
     * A staging file already on disk belongs to a restore that died, so it is deleted before the
     * helper opens, instead of being left for this import to fill on top of.
     *
     * Nothing is announced here, and the design before this one had to announce twice at this
     * point because it had already renamed the live database away. Nothing has changed for
     * anyone yet, the ledger every other thread reads is the one it was reading a moment ago.
     * The announcement belongs after the swap, and BackupHandlerIntentService makes it there,
     * between this import and the attachments.
     */
    /*package-local*/ void beginStagedImport() {
        deleteDatabaseAndJournal(mContext.getDatabasePath(SQLDatabaseImporter.STAGING_DATABASE_NAME));
        DataContentProvider.openStagingDatabase(mContext, SQLDatabaseImporter.STAGING_DATABASE_NAME);
    }

    /**
     * Puts the staged import in place. The staging helper closes first, and the rename then runs
     * in the slot where the live helper is closed too and no DataContentProvider write is in
     * flight. That is the whole of what the swap lock excludes; SyncContentProvider writes and
     * every query take no lock at all and can be running while the file moves, which resetShared
     * says in full. Today the only writers on that provider are the importers themselves, on this
     * thread. An open connection does not stop the rename itself, rename replaces a directory
     * entry and does not care what has the file open; it is the connection carried ACROSS the
     * rename that breaks, which resetShared also describes.
     *
     * The rename goes straight over the live database and nothing is deleted ahead of it, so a
     * rename that fails leaves the ledger where it was and the staged file is the only thing
     * thrown away. Deleting first would mean a failure here destroyed both.
     *
     * The journal beside the live name belongs to the database this just replaced and would be
     * read as the new one if it stayed, so it goes once the rename has happened. Neither file is
     * in write ahead logging and both were closed above, so a journal beside either of them is a
     * leftover and never state the database still needs.
     */
    private void promoteStagedDatabase(@NonNull File databaseFolder, @NonNull File staged) throws ImportException {
        File database = new File(databaseFolder, SQLDatabaseImporter.DATABASE_NAME);
        DataContentProvider.closeStagingDatabase();
        boolean[] promoted = new boolean[1];
        DataContentProvider.replaceDatabaseFile(mContext, () -> {
            promoted[0] = staged.renameTo(database);
            if (promoted[0]) {
                FileUtils.deleteQuietly(journalOf(database));
                FileUtils.deleteQuietly(journalOf(staged));
            }
        });
        if (!promoted[0]) {
            throw new ImportException("Cannot put the imported database in place");
        }
        // swept here and not at the start of the import, because until this rename it may be the
        // only whole ledger the user has. The design before this one renamed the live database
        // onto that name, so deleting a stale one cost nothing; here there is no replacement, and
        // an import that fails after the delete would leave the user with neither
        deleteDatabaseAndJournal(new File(databaseFolder, ABANDONED_BACKUP_COPY));
    }

    /**
     * Sends this thread back to the live database and throws the staged import away.
     *
     * No lock and no swap. A restore that failed part way never touched the live file, so there
     * is nothing to put back and nothing for another thread to be told about.
     */
    private void discardStagedDatabase(@NonNull File staged) {
        DataContentProvider.closeStagingDatabase();
        deleteDatabaseAndJournal(staged);
    }

    private static void deleteDatabaseAndJournal(@NonNull File database) {
        FileUtils.deleteQuietly(database);
        FileUtils.deleteQuietly(journalOf(database));
    }

    private static File journalOf(@NonNull File database) {
        return new File(database.getPath() + "-journal");
    }
}