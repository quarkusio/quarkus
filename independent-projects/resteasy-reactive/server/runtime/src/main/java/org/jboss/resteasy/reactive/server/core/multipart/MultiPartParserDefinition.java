package org.jboss.resteasy.reactive.server.core.multipart;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.CompletionCallback;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.common.headers.HeaderUtil;
import org.jboss.resteasy.reactive.common.util.CaseInsensitiveMap;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.ServerHttpRequest;

/**
 * @author Stuart Douglas
 */
public class MultiPartParserDefinition implements FormParserFactory.ParserDefinition<MultiPartParserDefinition> {

    private static final Logger log = Logger.getLogger(MultiPartParserDefinition.class);

    public static final String MULTIPART_FORM_DATA = "multipart/form-data";

    private final Supplier<Executor> executorSupplier;

    private Path tempFileLocation;

    private String defaultCharset = StandardCharsets.UTF_8.displayName();

    private boolean deleteUploadsOnEnd = true;

    private long maxIndividualFileSize = -1;

    private long fileSizeThreshold;

    private long maxAttributeSize = 2048;
    private long maxEntitySize = -1;

    public MultiPartParserDefinition(Supplier<Executor> executorSupplier) {
        this.executorSupplier = executorSupplier;
        tempFileLocation = Paths.get(System.getProperty("java.io.tmpdir"));
    }

    public MultiPartParserDefinition(Supplier<Executor> executorSupplier, final Path tempDir) {
        this.executorSupplier = executorSupplier;
        tempFileLocation = tempDir;
    }

    @Override
    public FormDataParser create(final ResteasyReactiveRequestContext exchange) {
        String mimeType = exchange.serverRequest().getRequestHeader(HttpHeaders.CONTENT_TYPE);
        if (mimeType != null && mimeType.startsWith(MULTIPART_FORM_DATA)) {
            String boundary = HeaderUtil.extractQuotedValueFromHeader(mimeType, "boundary");
            if (boundary == null) {
                log.debugf(
                        "Could not find boundary in multipart request with ContentType: %s, multipart data will not be available",
                        mimeType);
                return null;
            }
            final MultiPartUploadHandler parser = new MultiPartUploadHandler(exchange, boundary, maxIndividualFileSize,
                    fileSizeThreshold, defaultCharset, mimeType, maxAttributeSize, maxEntitySize);
            exchange.registerCompletionCallback(new CompletionCallback() {
                @Override
                public void onComplete(Throwable throwable) {
                    try {
                        parser.close();
                    } catch (IOException e) {
                        log.error("Failed to close multipart parser", e);
                    }
                }
            });
            return parser;

        }
        return null;
    }

    public long getMaxAttributeSize() {
        return maxAttributeSize;
    }

    public MultiPartParserDefinition setMaxAttributeSize(long maxAttributeSize) {
        this.maxAttributeSize = maxAttributeSize;
        return this;
    }

    public boolean isDeleteUploadsOnEnd() {
        return deleteUploadsOnEnd;
    }

    public MultiPartParserDefinition setDeleteUploadsOnEnd(boolean deleteUploadsOnEnd) {
        this.deleteUploadsOnEnd = deleteUploadsOnEnd;
        return this;
    }

    public Path getTempFileLocation() {
        return tempFileLocation;
    }

    public MultiPartParserDefinition setTempFileLocation(Path tempFileLocation) {
        this.tempFileLocation = tempFileLocation;
        return this;
    }

    public String getDefaultCharset() {
        return defaultCharset;
    }

    public MultiPartParserDefinition setDefaultCharset(final String defaultCharset) {
        this.defaultCharset = defaultCharset;
        return this;
    }

    public long getMaxIndividualFileSize() {
        return maxIndividualFileSize;
    }

    public MultiPartParserDefinition setMaxIndividualFileSize(final long maxIndividualFileSize) {
        this.maxIndividualFileSize = maxIndividualFileSize;
        return this;
    }

    public MultiPartParserDefinition setFileSizeThreshold(long fileSizeThreshold) {
        this.fileSizeThreshold = fileSizeThreshold;
        return this;
    }

    public long getMaxEntitySize() {
        return maxEntitySize;
    }

    public MultiPartParserDefinition setMaxEntitySize(long maxEntitySize) {
        this.maxEntitySize = maxEntitySize;
        return this;
    }

    private final class MultiPartUploadHandler implements FormDataParser, MultipartParser.PartHandler {

        private final ResteasyReactiveRequestContext exchange;
        private final FormData data;
        private final List<Path> createdFiles = new ArrayList<>();
        private final long maxIndividualFileSize;
        private final long fileSizeThreshold;
        private final long maxAttributeSize;
        private final long maxEntitySize;
        private String defaultEncoding;

        private final ByteArrayOutputStream contentBytes = new ByteArrayOutputStream();
        private String currentName;
        private String fileName;
        private Path file;
        private FileChannel fileChannel;
        private CaseInsensitiveMap<String> headers;
        private long currentFileSize;
        private long currentEntitySize;
        private final MultipartParser.ParseState parser;

        private MultiPartUploadHandler(final ResteasyReactiveRequestContext exchange, final String boundary,
                final long maxIndividualFileSize, final long fileSizeThreshold, final String defaultEncoding,
                String contentType, long maxAttributeSize, long maxEntitySize) {
            this.exchange = exchange;
            this.maxIndividualFileSize = maxIndividualFileSize;
            this.defaultEncoding = defaultEncoding;
            this.fileSizeThreshold = fileSizeThreshold;
            this.maxAttributeSize = maxAttributeSize;
            this.maxEntitySize = maxEntitySize;
            int maxParameters = 1000;
            this.data = new FormData(maxParameters);
            String charset = defaultEncoding;
            if (contentType != null) {
                String value = HeaderUtil.extractQuotedValueFromHeader(contentType, "charset");
                if (value != null) {
                    charset = value;
                }
            }
            this.parser = MultipartParser.beginParse(this, boundary.getBytes(StandardCharsets.US_ASCII), charset);

        }

        @Override
        public void parse() throws Exception {
            if (exchange.getFormData() != null) {
                return;
            }
            //we need to delegate to a thread pool
            //as we parse with blocking operations
            exchange.suspend();
            exchange.serverRequest().setReadListener(new NonBlockingParseTask(executorSupplier.get()));
            exchange.serverRequest().resumeRequestInput();
        }

        @Override
        public FormData parseBlocking() throws Exception {
            final FormData existing = exchange.getFormData();
            if (existing != null) {
                return existing;
            }
            try (InputStream inputStream = exchange.getInputStream()) {
                byte[] buf = new byte[1024];
                int c;
                while ((c = inputStream.read(buf)) > 0) {
                    parser.parse(ByteBuffer.wrap(buf, 0, c));
                }
                if (!parser.isComplete()) {
                    throw new IOException("Connection terminated parsing multipart request");
                }
                exchange.setFormData(data);
            }
            return data;
        }

        @Override
        public void beginPart(final CaseInsensitiveMap<String> headers) {
            this.currentFileSize = 0;
            this.headers = headers;
            final String disposition = headers.getFirst(HttpHeaders.CONTENT_DISPOSITION);
            if (disposition != null) {
                if (disposition.startsWith("form-data")) {
                    currentName = HeaderUtil.extractQuotedValueFromHeader(disposition, "name");
                    fileName = HeaderUtil.extractQuotedValueFromHeaderWithEncoding(disposition, "filename");
                    if (fileName != null && fileSizeThreshold == 0) {
                        try {
                            if (tempFileLocation != null) {
                                file = Files.createTempFile(tempFileLocation, "resteasy-reactive", "upload");
                            } else {
                                file = Files.createTempFile("resteasy-reactive", "upload");
                            }
                            createdFiles.add(file);
                            fileChannel = FileChannel.open(file, StandardOpenOption.READ, StandardOpenOption.WRITE);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }

        @Override
        public void data(final ByteBuffer buffer) throws IOException {
            this.currentFileSize += buffer.remaining();
            this.currentEntitySize += buffer.remaining();
            if (maxEntitySize > 0 && currentEntitySize > maxEntitySize) {
                data.deleteFiles();
                throw new WebApplicationException(Response.Status.REQUEST_ENTITY_TOO_LARGE);
            }
            if (this.maxIndividualFileSize > 0 && this.currentFileSize > this.maxIndividualFileSize) {
                data.deleteFiles();
                throw new WebApplicationException(Response.Status.REQUEST_ENTITY_TOO_LARGE);
            }
            if (file == null && fileName != null && fileSizeThreshold < this.currentFileSize) {
                try {
                    if (tempFileLocation != null) {
                        file = Files.createTempFile(tempFileLocation, "resteasy-reactive", "upload");
                    } else {
                        file = Files.createTempFile("resteasy-reactive", "upload");
                    }
                    createdFiles.add(file);

                    FileOutputStream fileOutputStream = new FileOutputStream(file.toFile());
                    contentBytes.writeTo(fileOutputStream);

                    fileChannel = fileOutputStream.getChannel();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            if (file == null) {
                while (buffer.hasRemaining()) {
                    contentBytes.write(buffer.get());
                }
                if (maxAttributeSize > 0 && contentBytes.size() > maxAttributeSize) {
                    data.deleteFiles();
                    throw new WebApplicationException(Response.Status.REQUEST_ENTITY_TOO_LARGE);
                }
            } else {
                fileChannel.write(buffer);
            }
        }

        @Override
        public void endPart() {
            if (file != null) {
                data.add(currentName, file, fileName, headers);
                file = null;
                contentBytes.reset();
                try {
                    fileChannel.close();
                    fileChannel = null;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else if (fileName != null) {
                data.add(currentName, Arrays.copyOf(contentBytes.toByteArray(), contentBytes.size()), fileName, headers);
                contentBytes.reset();
            } else {

                try {
                    String charset = defaultEncoding;
                    String contentType = headers.getFirst(HttpHeaders.CONTENT_TYPE);
                    if (contentType != null) {
                        String cs = HeaderUtil.extractQuotedValueFromHeader(contentType, "charset");
                        if (cs != null) {
                            charset = cs;
                        }
                    }

                    data.add(currentName, contentBytes.toString(charset), charset, headers);
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
                contentBytes.reset();
            }
        }

        public List<Path> getCreatedFiles() {
            return createdFiles;
        }

        @Override
        public void close() throws IOException {
            if (fileChannel != null) {
                fileChannel.close();
            }
            //we have to dispatch this, as it may result in file IO
            if (deleteUploadsOnEnd) {
                deleteFiles();
            }
        }

        private void deleteFiles() {
            final List<Path> files = new ArrayList<>(getCreatedFiles());
            executorSupplier.get().execute(new Runnable() {
                @Override
                public void run() {
                    for (final Path file : files) {
                        if (Files.exists(file)) {
                            try {
                                Files.delete(file);
                            } catch (NoSuchFileException e) { // ignore
                            } catch (IOException e) {
                                log.error("Cannot remove uploaded file " + file, e);
                            }
                        }
                    }
                }

            });
        }

        @Override
        public void setCharacterEncoding(final String encoding) {
            this.defaultEncoding = encoding;
            parser.setCharacterEncoding(encoding);
        }

        private final class NonBlockingParseTask implements ServerHttpRequest.ReadCallback {

            private final Executor executor;

            private NonBlockingParseTask(Executor executor) {
                this.executor = executor;
            }

            @Override
            public void done() {
                if (parser.isComplete()) {
                    exchange.setFormData(data);
                    exchange.resume();
                } else {
                    exchange.resume(new IOException("Connection terminated reading multipart data"));
                }
            }

            @Override
            public void data(ByteBuffer data) {
                exchange.serverRequest().pauseRequestInput();
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            parser.parse(data);
                            exchange.serverRequest().resumeRequestInput();
                        } catch (Throwable t) {
                            exchange.resume(t);
                        }
                    }
                });
            }
        }
    }

    public static class FileTooLargeException extends IOException {

        public FileTooLargeException() {
            super();
        }

        public FileTooLargeException(String message) {
            super(message);
        }

        public FileTooLargeException(String message, Throwable cause) {
            super(message, cause);
        }

        public FileTooLargeException(Throwable cause) {
            super(cause);
        }
    }

}
