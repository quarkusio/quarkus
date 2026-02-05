package io.quarkus.jlink.deployment;

import java.io.Writer;
import java.util.concurrent.locks.ReentrantLock;

import org.jboss.logging.Logger;

final class LogWriter extends Writer {
    private final ReentrantLock lock = new ReentrantLock();
    private final StringBuilder b = new StringBuilder();
    private final Logger logger;
    private final Logger.Level level;
    private boolean cr;

    LogWriter(Logger logger, Logger.Level level) {
        this.logger = logger;
        this.level = level;
    }

    public void write(final int c) {
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            write0(c);
        } finally {
            lock.unlock();
        }
    }

    private void write0(final int c) {
        switch (c) {
            case '\r' -> {
                cr = true;
                flushLine();
            }
            case '\n' -> {
                if (cr) {
                    // already flushed
                    cr = false;
                } else {
                    flushLine();
                }
            }
            default -> {
                cr = false;
                b.append((char) c);
                // prevent it from getting too big
                if (b.length() == 1024) {
                    flushLine();
                }
            }
        }
    }

    public void write(final char[] cbuf, final int off, final int len) {
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            for (int i = 0; i < len; i++) {
                write0(cbuf[off + i]);
            }
        } finally {
            lock.unlock();
        }
    }

    public void write(final String str, final int off, final int len) {
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            for (int i = 0; i < len; i++) {
                write0(str.charAt(off + i));
            }
        } finally {
            lock.unlock();
        }
    }

    public void write(final char[] cbuf) {
        write(cbuf, 0, cbuf.length);
    }

    public void write(final String str) {
        write(str, 0, str.length());
    }

    public Writer append(final CharSequence csq) {
        return append(csq, 0, csq.length());
    }

    public Writer append(final CharSequence csq, final int start, final int end) {
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            for (int i = start; i < end; i++) {
                write0(csq.charAt(i));
            }
        } finally {
            lock.unlock();
        }
        return this;
    }

    public Writer append(final char c) {
        write(c);
        return this;
    }

    private void flushLine() {
        if (!b.isEmpty()) {
            logger.log(level, b.toString());
            b.setLength(0);
        }
    }

    public void flush() {
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            flushLine();
        } finally {
            lock.unlock();
        }
    }

    public void close() {
    }
}
