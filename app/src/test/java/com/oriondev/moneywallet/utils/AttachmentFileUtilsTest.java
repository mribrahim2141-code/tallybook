package com.oriondev.moneywallet.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

public class AttachmentFileUtilsTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void resolvesAFlatAttachmentNameInsideItsFolder() throws Exception {
        File folder = temporaryFolder.newFolder("attachments");
        File resolved = AttachmentFileUtils.resolve(folder, "receipt-123.pdf");
        assertNotNull(resolved);
        assertEquals(new File(folder.getCanonicalFile(), "receipt-123.pdf").getCanonicalFile(), resolved);
    }

    @Test
    public void rejectsParentDirectoryTraversal() throws Exception {
        File folder = temporaryFolder.newFolder("attachments");
        assertNull(AttachmentFileUtils.resolve(folder, "../private.txt"));
        assertNull(AttachmentFileUtils.resolve(folder, ".."));
        assertNull(AttachmentFileUtils.resolve(folder, "."));
    }

    @Test
    public void rejectsUnixAndWindowsPathSeparators() throws Exception {
        File folder = temporaryFolder.newFolder("attachments");
        assertNull(AttachmentFileUtils.resolve(folder, "nested/receipt.pdf"));
        assertNull(AttachmentFileUtils.resolve(folder, "nested\\\\receipt.pdf"));
    }

    @Test
    public void rejectsNullAndEmptyNames() throws Exception {
        File folder = temporaryFolder.newFolder("attachments");
        assertNull(AttachmentFileUtils.resolve(folder, null));
        assertNull(AttachmentFileUtils.resolve(folder, ""));
    }
}
