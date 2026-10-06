package com.example.backend.service;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/** Counts actual bytes, including unknown/chunked content length, before parser buffering. */
final class LimitedInputStream extends FilterInputStream {
    static final class SizeLimitExceededException extends IOException {
        SizeLimitExceededException() { super("CIF size limit exceeded"); }
    }
    private final long limit;
    private long read;

    LimitedInputStream(InputStream input, long limit) { super(input); this.limit = limit; }

    @Override public int read() throws IOException {
        int value = in.read();
        if (value != -1 && ++read > limit) throw new SizeLimitExceededException();
        return value;
    }

    @Override public int read(byte[] bytes, int offset, int length) throws IOException {
        if (length == 0) return 0;
        if (read > limit) throw new SizeLimitExceededException();
        int count = in.read(bytes, offset, (int) Math.min(length, limit - read + 1));
        if (count > 0 && (read += count) > limit) throw new SizeLimitExceededException();
        return count;
    }

    @Override public void close() throws IOException {
        // Apache S3 close can drain the unread body. Abort oversize responses instead.
        if (read > limit && in instanceof software.amazon.awssdk.core.ResponseInputStream<?> response) {
            response.abort();
        } else {
            super.close();
        }
    }

    @Override public long skip(long count) throws IOException {
        // Skipped bytes must not bypass the limit.
        byte[] buffer = new byte[8192];
        long skipped = 0;
        while (skipped < count) {
            int n = read(buffer, 0, (int) Math.min(buffer.length, count - skipped));
            if (n == -1) break;
            skipped += n;
        }
        return skipped;
    }

    @Override public boolean markSupported() { return false; }
    @Override public synchronized void reset() throws IOException { throw new IOException("Reset unsupported"); }
}
