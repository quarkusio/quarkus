package org.jboss.resteasy.reactive.server.providers.serialisers;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.common.providers.serialisers.FileBodyHandler;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyWriter;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

@Produces("*/*")
@Consumes("*/*")
public class ServerFileBodyHandler extends FileBodyHandler implements ServerMessageBodyWriter<File> {

    @Override
    public long getSize(File o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return o.length();
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, ResteasyReactiveResourceInfo target, MediaType mediaType) {
        return File.class.isAssignableFrom(type);
    }

    @Override
    public void writeResponse(File o, Type genericType, ServerRequestContext context) throws WebApplicationException {
        sendFile(o, context);
    }

    static void sendFile(File file, ServerRequestContext context) {
        ResteasyReactiveRequestContext ctx = ((ResteasyReactiveRequestContext) context);
        Object rangeObj = ctx.getHeader("Range", true);
        ByteRange byteRange = rangeObj == null ? null : ByteRange.parse(rangeObj.toString());
        long fileLength = file.length();
        if ((byteRange != null) && (byteRange.ranges.size() == 1)) {
            ByteRange.Range range = byteRange.ranges.get(0);

            ByteRange.Range fileRange = (range.getStart() == -1)
                    ? new ByteRange.Range(fileLength - range.getEnd(), fileLength - 1)
                    : new ByteRange.Range(range.getStart(), Math.min(fileLength - 1, range.getEnd()));

            if ((fileRange.getStart() >= 0) && (fileRange.getStart() <= fileRange.getEnd())) {
                String contentRange = "bytes " + fileRange.getStart() + "-" + fileRange.getEnd() + "/" + fileLength;
                long length = fileRange.getEnd() - fileRange.getStart() + 1;
                context.serverResponse()
                        .setStatusCode(Response.Status.PARTIAL_CONTENT.getStatusCode())
                        .setResponseHeader("Content-Range", contentRange)
                        .sendFile(file.getAbsolutePath(), fileRange.getStart(), length);
                return;
            }
        }
        context.serverResponse().sendFile(file.getAbsolutePath(), 0, fileLength);
    }

    /**
     * Represents a byte range for a range request
     *
     * @author Stuart Douglas
     *
     *         NOTE: copied from Quarkus HTTP
     */
    public static class ByteRange {

        private static final Logger log = Logger.getLogger(ByteRange.class);

        private final List<Range> ranges;

        public ByteRange(List<Range> ranges) {
            this.ranges = ranges;
        }

        public int getRanges() {
            return ranges.size();
        }

        /**
         * Gets the start of the specified range segment, of -1 if this is a suffix range segment
         *
         * @param range The range segment to get
         * @return The range start
         */
        public long getStart(int range) {
            return ranges.get(range).getStart();
        }

        /**
         * Gets the end of the specified range segment, or the number of bytes if this is a suffix range segment
         *
         * @param range The range segment to get
         * @return The range end
         */
        public long getEnd(int range) {
            return ranges.get(range).getEnd();
        }

        /**
         * Attempts to parse a range request. If the range request is invalid it will just return null so that
         * it may be ignored.
         *
         * @param rangeHeader The range spec
         * @return A range spec, or null if the range header could not be parsed
         */
        public static ByteRange parse(String rangeHeader) {
            if (rangeHeader == null || rangeHeader.length() < 7) {
                return null;
            }
            if (!rangeHeader.startsWith("bytes=")) {
                return null;
            }
            List<Range> ranges = new ArrayList<>();
            String[] parts = rangeHeader.substring(6).split(",");
            for (String part : parts) {
                try {
                    int index = part.indexOf('-');
                    if (index == 0) {
                        //suffix range spec
                        //represents the last N bytes
                        //internally we represent this using a -1 as the start position
                        long val = Long.parseLong(part.substring(1));
                        if (val < 0) {
                            log.debugf("Invalid range spec %s", rangeHeader);
                            return null;
                        }
                        ranges.add(new Range(-1, val));
                    } else {
                        if (index == -1) {
                            log.debugf("Invalid range spec %s", rangeHeader);
                            return null;
                        }
                        long start = Long.parseLong(part.substring(0, index));
                        if (start < 0) {
                            log.debugf("Invalid range spec %s", rangeHeader);
                            return null;
                        }
                        long end;
                        if (index + 1 < part.length()) {
                            end = Long.parseLong(part.substring(index + 1));
                        } else {
                            end = Long.MAX_VALUE;
                        }
                        ranges.add(new Range(start, end));
                    }
                } catch (NumberFormatException e) {
                    log.debugf("Invalid range spec %s", rangeHeader);
                    return null;
                }
            }
            if (ranges.isEmpty()) {
                return null;
            }
            return new ByteRange(ranges);
        }

        public static class Range {
            private final long start, end;

            public Range(long start, long end) {
                this.start = start;
                this.end = end;
            }

            public long getStart() {
                return start;
            }

            public long getEnd() {
                return end;
            }
        }
    }
}
