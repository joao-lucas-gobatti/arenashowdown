package com.arenashowdown.tntrun.persistence;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/** Write-through journal: force the temporary file before exposing the final name. */
public final class AtomicFiles {
    private AtomicFiles() {}
    public static void write(Path target, String text, boolean replace) throws IOException {
        Files.createDirectories(target.getParent());
        if (!replace && Files.exists(target)) throw new FileAlreadyExistsException(target.toString());
        Path temp = Files.createTempFile(target.getParent(), ".tntrun-", ".tmp");
        try {
            try (FileChannel out = FileChannel.open(temp, StandardOpenOption.WRITE)) {
                ByteBuffer bytes = StandardCharsets.UTF_8.encode(text);
                while (bytes.hasRemaining()) out.write(bytes);
                out.force(true);
            }
            if (replace) {
                try { Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
                catch (AtomicMoveNotSupportedException e) { Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING); }
            } else {
                // No REPLACE_EXISTING: an older recovery file must never be overwritten.
                Files.move(temp, target);
            }
        } finally { Files.deleteIfExists(temp); }
    }
}
