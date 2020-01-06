package io.quarkus.mailer;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import io.quarkus.mailer.runtime.MutinyMailerImpl;
import io.smallrye.mutiny.Multi;
import io.vertx.core.file.FileSystemException;
import io.vertx.core.file.OpenOptions;
import io.vertx.mutiny.core.Vertx;

class AttachmentTest {

    private static final File LOREM = new File("src/test/resources/lorem-ipsum.txt");
    private static final String BEGINNING = "Sed ut perspiciatis unde omnis iste natus error sit";
    private static final String DESCRIPTION = "my lorem ipsum";
    private static Vertx vertx;

    @BeforeAll
    static void init() {
        vertx = Vertx.vertx();
    }

    @AfterAll
    static void closing() {
        vertx.close().await().indefinitely();
    }

    @Test
    void testAttachmentCreationFromFile() {
        Attachment attachment = new Attachment("lorem.txt", LOREM, "text/plain");
        assertThat(attachment.getFile()).isFile();
        assertThat(attachment.getName()).isEqualTo("lorem.txt");
        assertThat(attachment.getDisposition()).isEqualTo(Attachment.DISPOSITION_ATTACHMENT);
        assertThat(attachment.getContentId()).isNull();
        assertThat(attachment.getDescription()).isNull();
        assertThat(attachment.getContentType()).isEqualTo("text/plain");
        assertThat(attachment.getData()).isNull();

        String content = MutinyMailerImpl.getAttachmentStream(vertx, attachment)
                .onItem().apply(buffer -> buffer.toString("UTF-8"))
                .await().indefinitely();
        assertThat(content).startsWith(BEGINNING);
    }

    @Test
    void testInlineAttachmentCreationFromFile() {
        Attachment attachment = new Attachment("lorem.txt", LOREM, "text/plain", "my-file");
        assertThat(attachment.getFile()).isFile();
        assertThat(attachment.getName()).isEqualTo("lorem.txt");
        assertThat(attachment.getDisposition()).isEqualTo(Attachment.DISPOSITION_INLINE);
        assertThat(attachment.getContentId()).isEqualTo("<my-file>");
        assertThat(attachment.getDescription()).isNull();
        assertThat(attachment.getContentType()).isEqualTo("text/plain");
        assertThat(attachment.getData()).isNull();

        String content = MutinyMailerImpl.getAttachmentStream(vertx, attachment)
                .onItem().apply(buffer -> buffer.toString("UTF-8"))
                .await().indefinitely();
        assertThat(content).startsWith(BEGINNING);
    }

    @Test
    void testAttachmentCreationFromStream() {
        Publisher<Byte> publisher = getLoremContent();

        Attachment attachment = new Attachment("lorem.txt", publisher, "text/plain");
        assertThat(attachment.getFile()).isNull();
        assertThat(attachment.getName()).isEqualTo("lorem.txt");
        assertThat(attachment.getDisposition()).isEqualTo(Attachment.DISPOSITION_ATTACHMENT);
        assertThat(attachment.getContentId()).isNull();
        assertThat(attachment.getDescription()).isNull();
        assertThat(attachment.getContentType()).isEqualTo("text/plain");
        assertThat(attachment.getData()).isNotNull().isEqualTo(publisher);

        String content = MutinyMailerImpl.getAttachmentStream(vertx, attachment)
                .onItem().apply(buffer -> buffer.toString("UTF-8"))
                .await().indefinitely();
        assertThat(content).startsWith(BEGINNING);
    }

    @Test
    void testInlineAttachmentCreationFromStream() {
        Publisher<Byte> publisher = getLoremContent();

        Attachment attachment = new Attachment("lorem.txt", publisher, "text/plain", "<my-id>");
        assertThat(attachment.getFile()).isNull();
        assertThat(attachment.getName()).isEqualTo("lorem.txt");
        assertThat(attachment.getDisposition()).isEqualTo(Attachment.DISPOSITION_INLINE);
        assertThat(attachment.getContentId()).isEqualTo("<my-id>");
        assertThat(attachment.getDescription()).isNull();
        assertThat(attachment.getContentType()).isEqualTo("text/plain");
        assertThat(attachment.getData()).isNotNull().isEqualTo(publisher);

        String content = MutinyMailerImpl.getAttachmentStream(vertx, attachment)
                .onItem().apply(buffer -> buffer.toString("UTF-8"))
                .await().indefinitely();
        assertThat(content).startsWith(BEGINNING);
    }

    private Publisher<Byte> getLoremContent() {
        return vertx.fileSystem().open(LOREM.getAbsolutePath(), new OpenOptions().setRead(true))
                .onItem().produceMulti(af -> af.toMulti()
                        .onItem().produceMulti(buffer -> {
                            List<Byte> bytes = Arrays.asList(ArrayUtils.toObject(buffer.getBytes()));
                            return Multi.createFrom().iterable(bytes);
                        }).concatenate());
    }

    @Test
    void testAttachmentCreationWithDescription() {
        Publisher<Byte> publisher = getLoremContent();

        Attachment attachment = new Attachment("lorem.txt", publisher, "text/plain",
                DESCRIPTION, Attachment.DISPOSITION_ATTACHMENT);
        assertThat(attachment.getFile()).isNull();
        assertThat(attachment.getName()).isEqualTo("lorem.txt");
        assertThat(attachment.getDisposition()).isEqualTo(Attachment.DISPOSITION_ATTACHMENT);
        assertThat(attachment.getContentId()).isNull();
        assertThat(attachment.getDescription()).isEqualTo(DESCRIPTION);
        assertThat(attachment.getContentType()).isEqualTo("text/plain");
        assertThat(attachment.getData()).isNotNull().isEqualTo(publisher);

        String content = MutinyMailerImpl.getAttachmentStream(vertx, attachment)
                .onItem().apply(buffer -> buffer.toString("UTF-8"))
                .await().indefinitely();
        assertThat(content).startsWith(BEGINNING);
    }

    @Test
    void testInlineAttachmentCreationWithDescription() {
        Publisher<Byte> publisher = getLoremContent();

        Attachment attachment = new Attachment("lorem.txt", publisher, "text/plain",
                DESCRIPTION, Attachment.DISPOSITION_INLINE);
        assertThat(attachment.getFile()).isNull();
        assertThat(attachment.getName()).isEqualTo("lorem.txt");
        assertThat(attachment.getDisposition()).isEqualTo(Attachment.DISPOSITION_INLINE);
        assertThat(attachment.getContentId()).isNull();
        assertThat(attachment.getDescription()).isEqualTo(DESCRIPTION);
        assertThat(attachment.getContentType()).isEqualTo("text/plain");
        assertThat(attachment.getData()).isNotNull().isEqualTo(publisher);

        String content = MutinyMailerImpl.getAttachmentStream(vertx, attachment)
                .onItem().apply(buffer -> buffer.toString("UTF-8"))
                .await().indefinitely();
        assertThat(content).startsWith(BEGINNING);
    }

    @Test
    void testAttachmentCreationWithByteArray() {
        String payload = UUID.randomUUID().toString();
        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);

        Attachment attachment = new Attachment("lorem.txt", bytes, "text/plain",
                DESCRIPTION, Attachment.DISPOSITION_ATTACHMENT);
        assertThat(attachment.getFile()).isNull();
        assertThat(attachment.getName()).isEqualTo("lorem.txt");
        assertThat(attachment.getDisposition()).isEqualTo(Attachment.DISPOSITION_ATTACHMENT);
        assertThat(attachment.getContentId()).isNull();
        assertThat(attachment.getDescription()).isEqualTo(DESCRIPTION);
        assertThat(attachment.getContentType()).isEqualTo("text/plain");
        assertThat(attachment.getData()).isNotNull();

        String content = MutinyMailerImpl.getAttachmentStream(vertx, attachment)
                .onItem().apply(buffer -> buffer.toString("UTF-8"))
                .await().indefinitely();
        assertThat(content).isEqualTo(payload);
    }

    @Test
    void testCreationWithEmptyContent() {
        Attachment attachment1 = new Attachment("attachment-1", (byte[]) null, "text/plain");
        Attachment attachment2 = new Attachment("attachment-2", new byte[0], "text/plain");
        Attachment attachment3 = new Attachment("attachment-3", ReactiveStreams.<Byte> empty().buildRs(), "text/plain");

        assertThat(ReactiveStreams.fromPublisher(attachment1.getData()).findFirst().run().toCompletableFuture().join())
                .isEmpty();
        assertThat(ReactiveStreams.fromPublisher(attachment2.getData()).findFirst().run().toCompletableFuture().join())
                .isEmpty();
        assertThat(ReactiveStreams.fromPublisher(attachment3.getData()).findFirst().run().toCompletableFuture().join())
                .isEmpty();
    }

    @Test
    void testCreationFromMissingFile() {
        File missing = new File("missing");
        Attachment attachment = new Attachment("missing", missing, "text/plain");
        Assertions.assertThrows(FileSystemException.class, () -> {
            MutinyMailerImpl.getAttachmentStream(vertx, attachment).await().indefinitely();
        });
    }

}
