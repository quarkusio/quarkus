package org.jboss.shamrock.undertow.runtime.graal;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.xnio.channels.StreamSourceChannel;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "org.xnio.channels.Channels")
public final class ChannelsSubstitution {

    @Alias()
    private static final ByteBuffer DRAIN_BUFFER = ByteBuffer.allocateDirect(16384);
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
        ByteBuffer buffer = null;
        for (;;) {
            if (count == 0L) return total;
            if (buffer == null) buffer = DRAIN_BUFFER.duplicate();
            if ((long) buffer.limit() > count) buffer.limit((int) count);
            ires = channel.read(buffer);
            buffer.clear();
            switch (ires) {
                case -1: return total == 0L ? -1L : total;
                case 0: return total;
                default: total += (long) ires; count -= (long) ires;
            }
        }
    }
}
