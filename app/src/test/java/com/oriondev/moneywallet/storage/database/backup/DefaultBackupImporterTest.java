package com.oriondev.moneywallet.storage.database.backup;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.oriondev.moneywallet.storage.database.ImportException;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(RobolectricTestRunner.class)
public class DefaultBackupImporterTest {

    @Test
    public void duplicateAttachmentEntriesAreRejectedWithoutChangingLiveAttachments() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        File root = new File(context.getCacheDir(), "duplicate-attachment-restore");
        FileUtils.deleteQuietly(root);
        File liveFolder = new File(root, "attachments");
        assertTrue(liveFolder.mkdirs());

        byte[] original = "existing attachment".getBytes(StandardCharsets.UTF_8);
        File existing = new File(liveFolder, "receipt.jpg");
        FileUtils.writeByteArrayToFile(existing, original);

        File backup = new File(root, "duplicate.mwbx");
        ZipFile zip = new ZipFile(backup);
        addAttachmentEntry(zip, "first attachment".getBytes(StandardCharsets.UTF_8));
        addAttachmentEntry(zip, "replacement attachment".getBytes(StandardCharsets.UTF_8));

        try {
            new DefaultBackupImporter(context, backup, null).prepareAttachments(liveFolder);
            fail("duplicate attachment names must be rejected");
        } catch (ImportException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("Duplicate attachment"));
        }

        assertArrayEquals(original, FileUtils.readFileToByteArray(existing));
        assertTrue(liveFolder.isDirectory());
        assertTrue(new File(root, "attachments.restore-staging").exists() == false);
    }

    private static void addAttachmentEntry(ZipFile zip, byte[] content) throws Exception {
        ZipParameters parameters = new ZipParameters();
        parameters.setSourceExternalStream(true);
        parameters.setFileNameInZip("attachments/receipt.jpg");
        parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
        zip.addStream(new ByteArrayInputStream(content), parameters);
    }
}
