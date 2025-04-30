package io.quarkus.devui.runtime.readme;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.Optional;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import io.smallrye.mutiny.subscription.Cancellable;

@ApplicationScoped
public class ReadmeJsonRPCService {
    private WatchService watchService = null;
    private Cancellable cancellable;
    private Path path = null;
    private final BroadcastProcessor<String> readmeStream = BroadcastProcessor.create();

    @PostConstruct
    public void init() {
        this.path = getPath("README.md")
                .orElse(getPath("readme.md")
                        .orElse(null));
        if (this.path != null) {
            this.path = this.path.toAbsolutePath();
            Path parentDir = this.path.getParent();
            try {
                watchService = FileSystems.getDefault().newWatchService();
                parentDir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE,
                        StandardWatchEventKinds.ENTRY_MODIFY);

                this.cancellable = Multi.createFrom().emitter(emitter -> {
                    while (!Thread.currentThread().isInterrupted()) {
                        WatchKey key;
                        try {
                            key = watchService.take();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                        List<WatchEvent<?>> events = key.pollEvents();
                        for (WatchEvent<?> event : events) {
                            WatchEvent.Kind<?> kind = event.kind();
                            Path changed = parentDir.resolve((Path) event.context());

                            if (changed.equals(this.path)) {
                                emitter.emit(event);
                            }
                        }
                        boolean valid = key.reset();
                        if (!valid) {
                            emitter.complete();
                            break;
                        }
                    }
                }).runSubscriptionOn(Infrastructure.getDefaultExecutor())
                        .onItem().transform(event -> {
                            readmeStream.onNext(getContent());
                            return this.path;
                        }).subscribe().with((t) -> {

                        });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @PreDestroy
    public void cleanup() {
        if (cancellable != null) {
            cancellable.cancel();
        }
        try {
            if (watchService != null) {
                watchService.close();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String getContent() {
        try {
            return Files.readString(this.path);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public Multi<String> streamReadme() {
        return readmeStream;
    }

    private Optional<Path> getPath(String name) {
        Path p = Path.of(name);
        if (Files.exists(p)) {
            return Optional.of(p);
        }
        return Optional.empty();
    }

}
