package org.jboss.resteasy.reactive.server.handlers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import org.jboss.resteasy.reactive.server.core.BlockingOperationSupport;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.core.multipart.FormData;
import org.jboss.resteasy.reactive.server.core.multipart.FormDataParser;
import org.jboss.resteasy.reactive.server.core.multipart.FormEncodedDataDefinition;
import org.jboss.resteasy.reactive.server.core.multipart.FormParserFactory;
import org.jboss.resteasy.reactive.server.core.multipart.MultiPartParserDefinition;
import org.jboss.resteasy.reactive.server.spi.GenericRuntimeConfigurableServerRestHandler;
import org.jboss.resteasy.reactive.server.spi.RuntimeConfiguration;

public class FormBodyHandler implements GenericRuntimeConfigurableServerRestHandler<RuntimeConfiguration> {

    private final boolean alsoSetInputStream;
    private final Supplier<Executor> executorSupplier;
    private final Set<String> fileFormNames;
    private volatile FormParserFactory formParserFactory;

    public FormBodyHandler(boolean alsoSetInputStream, Supplier<Executor> executorSupplier, Set<String> fileFormNames) {
        this.alsoSetInputStream = alsoSetInputStream;
        this.executorSupplier = executorSupplier;
        this.fileFormNames = fileFormNames;
    }

    @Override
    public Class<RuntimeConfiguration> getConfigurationClass() {
        return RuntimeConfiguration.class;
    }

    @Override
    public void configure(RuntimeConfiguration configuration) {
        formParserFactory = FormParserFactory.builder(false, executorSupplier)
                .addParser(new MultiPartParserDefinition(executorSupplier)
                        .setFileSizeThreshold(0)
                        .setMaxAttributeSize(configuration.limits().maxFormAttributeSize())
                        .setMaxEntitySize(configuration.limits().maxBodySize().orElse(-1L))
                        .setMaxParameters(configuration.limits().maxParameters())
                        .setDeleteUploadsOnEnd(configuration.body().deleteUploadedFilesOnEnd())
                        .setFileContentTypes(configuration.body().multiPart().fileContentTypes())
                        .setDefaultCharset(configuration.body().defaultCharset().name())
                        .setTempFileLocation(Path.of(configuration.body().uploadsDirectory())))

                .addParser(new FormEncodedDataDefinition()
                        .setMaxAttributeSize(configuration.limits().maxFormAttributeSize())
                        .setDefaultCharset(configuration.body().defaultCharset().name()))
                .build();

        try {
            Files.createDirectories(Paths.get(configuration.body().uploadsDirectory()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
        // in some cases, with sub-resource locators or via request filters,
        // it's possible we've already read the entity
        if (requestContext.getFormData() != null) {
            // let's not set it twice
            return;
        }
        FormData existingParsedForm = requestContext.serverRequest().getExistingParsedForm();
        if (existingParsedForm != null) {
            requestContext.setFormData(existingParsedForm);
            return;
        }
        FormDataParser factory = formParserFactory.createParser(requestContext, fileFormNames);
        if (factory == null) {
            return;
        }
        if (BlockingOperationSupport.isBlockingAllowed()) {
            //blocking IO approach
            CapturingInputStream cis = null;
            if (alsoSetInputStream) {
                // the TCK allows the body to be read as a form param and also as a body param
                // the spec is silent about this
                // TODO: this is really really horrible and hacky and needs to be fixed.
                cis = new CapturingInputStream(requestContext.getInputStream());
                requestContext.setInputStream(cis);
            }
            factory.parseBlocking();
            if (alsoSetInputStream) {
                requestContext.setInputStream(new ByteArrayInputStream(cis.baos.toByteArray()));
            }
        } else if (alsoSetInputStream) {
            requestContext.suspend();
            executorSupplier.get().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        CapturingInputStream cis = new CapturingInputStream(requestContext.getInputStream());
                        requestContext.setInputStream(cis);
                        factory.parseBlocking();
                        requestContext.setInputStream(new ByteArrayInputStream(cis.baos.toByteArray()));
                        requestContext.resume();
                    } catch (Throwable t) {
                        requestContext.resume(t);
                    }
                }
            });
        } else {
            //parse will auto resume
            factory.parse();
        }
    }

    static final class CapturingInputStream extends InputStream {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final InputStream delegate;

        CapturingInputStream(InputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public int read() throws IOException {
            int res = delegate.read();
            if (res != -1) {
                baos.write(res);
            }
            return res;
        }
    }

}
