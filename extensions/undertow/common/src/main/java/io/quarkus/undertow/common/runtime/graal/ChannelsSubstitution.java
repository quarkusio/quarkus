package io.quarkus.undertow.common.runtime.graal;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;

import org.xnio.channels.StreamSourceChannel;

import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "org.xnio.channels.Channels")
public final class ChannelsSubstitution {

    @Delete()
    private static ByteBuffer DRAIN_BUFFER = null;

    /**
     * Attempt to drain the given number of bytes from the stream source channel.
     *
     * @param channel the channel to drain
     * @param count the number of bytes
     * @return the number of bytes drained, 0 if reading the channel would block, or -1 if the EOF was reached
     * @throws IOException if an error occurs
     */
    @Substitute
    public static long drain(StreamSourceChannel channel, long count) throws IOException {
        long total = 0L, lres;
        int ires;
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        for (;;) {
            if (count == 0L)
                return total;
            if ((long) buffer.limit() > count)
                buffer.limit((int) count);
            ires = channel.read(buffer);
            buffer.clear();
            switch (ires) {
                case -1:
                    return total == 0L ? -1L : total;
                case 0:
                    return total;
                default:
                    total += (long) ires;
                    count -= (long) ires;
            }
        }
    }

    /**
     * Attempt to drain the given number of bytes from the readable byte channel.
     *
     * @param channel the channel to drain
     * @param count the number of bytes
     * @return the number of bytes drained, 0 if reading the channel would block, or -1 if the EOF was reached
     * @throws IOException if an error occurs
     */
    @Substitute
    public static long drain(ReadableByteChannel channel, long count) throws IOException {
        if (channel instanceof StreamSourceChannel) {
            return drain((StreamSourceChannel) channel, count);
        } else {
            long total = 0L, lres;
            int ires;
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            for (;;) {
                if (count == 0L)
                    return total;
                if ((long) buffer.limit() > count)
                    buffer.limit((int) count);
                ires = channel.read(buffer);
                buffer.clear();
                switch (ires) {
                    case -1:
                        return total == 0L ? -1L : total;
                    case 0:
                        return total;
                    default:
                        total += (long) ires;
                        count -= (long) ires;
                }
            }
        }
    }

    /**
     * Attempt to drain the given number of bytes from the file channel. This does nothing more than force a
     * read of bytes in the file.
     *
     * @param channel the channel to drain
     * @param position the position to drain from
     * @param count the number of bytes
     * @return the number of bytes drained, 0 if reading the channel would block, or -1 if the EOF was reached
     * @throws IOException if an error occurs
     */
    @Substitute
    public static long drain(FileChannel channel, long position, long count) throws IOException {
        if (channel instanceof StreamSourceChannel) {
            return drain((StreamSourceChannel) channel, count);
        } else {
            long total = 0L, lres;
            int ires;
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            for (;;) {
                if (count == 0L)
                    return total;
                if ((long) buffer.limit() > count)
                    buffer.limit((int) count);
                ires = channel.read(buffer);
                buffer.clear();
                switch (ires) {
                    case -1:
                        return total == 0L ? -1L : total;
                    case 0:
                        return total;
                    default:
                        total += (long) ires;
                }
            }
        }
    }
}
