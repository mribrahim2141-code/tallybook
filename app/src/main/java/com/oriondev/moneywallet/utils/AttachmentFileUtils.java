/*
 * Copyright (c) 2026.
 *
 * This file is part of MoneyWallet.
 *
 * MoneyWallet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.oriondev.moneywallet.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;

/** Resolves attachment names without allowing paths outside the attachment folder. */
public final class AttachmentFileUtils {

    private AttachmentFileUtils() {}

    /**
     * Resolve a flat attachment filename inside its owning folder.
     *
     * @return the canonical file, or null when the supplied name is not a safe flat filename.
     */
    @Nullable
    public static File resolve(@NonNull File attachmentFolder, @Nullable String fileName) throws IOException {
        if (fileName == null || fileName.isEmpty()
                || ".".equals(fileName) || "..".equals(fileName)
                || fileName.contains("/") || fileName.contains("\\")) {
            return null;
        }
        File canonicalFolder = attachmentFolder.getCanonicalFile();
        File candidate = new File(canonicalFolder, fileName).getCanonicalFile();
        if (!canonicalFolder.equals(candidate.getParentFile())) {
            return null;
        }
        return candidate;
    }
}
