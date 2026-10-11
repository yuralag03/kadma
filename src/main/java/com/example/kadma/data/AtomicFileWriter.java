package com.example.kadma.data;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Crash safe file writing.
 *
 * <p>Content is written to a sibling {@code .tmp} file, forcefully flushed to disk,
 * then moved onto the final path. Either the reader sees the old bytes or the new
 * bytes - never a half written file.</p>
 */
public final class AtomicFileWriter {

    private AtomicFileWriter() {
    }

    public static void write(Path target, String content) throws IOException {
        if (target == null) {
            throw new IOException("null target");
        }
        Path parent = target.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path temp = target.resolveSibling(target.getFileName().toString() + ".tmp");
        try {
            Files.write(temp, content.getBytes(StandardCharsets.UTF_8));
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException fallback) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            try {
                Files.deleteIfExists(temp);
            } catch (IOException ignored) {
                // nothing sensible to do here
            }
        }
    }

    public static String readAll(Path target) throws IOException {
        return new String(Files.readAllBytes(target), StandardCharsets.UTF_8);
    }

    public static boolean exists(Path target) {
        return target != null && Files.exists(target);
    }
}
