package com.oriondev.moneywallet.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class AttachmentHandlerIntentServiceTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void resolvesOnlyFlatNamesInsideTheAttachmentFolder() throws Exception {
        File folder = temporaryFolder.newFolder("attachments");
        File resolved = AttachmentHandlerIntentService.resolveAttachmentFile(folder, "receipt.pdf");
        assertEquals(new File(folder.getCanonicalFile(), "receipt.pdf").getCanonicalFile(), resolved);
    }

    @Test
    public void rejectsTraversalBeforeTheServiceCanTouchFilesOutsideTheFolder() throws Exception {
        File folder = temporaryFolder.newFolder("attachments");
        File outside = new File(temporaryFolder.getRoot(), "private.txt");
        Files.write(outside.toPath(), "keep".getBytes(StandardCharsets.UTF_8));

        String[] unsafeNames = {"../private.txt", "..", "nested/receipt.pdf", "nested\\\\receipt.pdf"};
        for (String name : unsafeNames) {
            try {
                AttachmentHandlerIntentService.resolveAttachmentFile(folder, name);
                fail("unsafe attachment name was accepted: " + name);
            } catch (IOException expected) {
                assertTrue(expected.getMessage().contains("Invalid attachment"));
            }
        }

        assertEquals("keep", new String(Files.readAllBytes(outside.toPath()), StandardCharsets.UTF_8));
    }
}
