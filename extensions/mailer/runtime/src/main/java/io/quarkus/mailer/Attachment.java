package io.quarkus.mailer;

import java.io.File;

/**
 * Defines an attachment.
 * <p>
 * Instances of this class are not thread-safe.
 */
public class Attachment {

    /**
     * Disposition for inline attachments.
     */
    public static final String DISPOSITION_INLINE = "inline";

    /**
     * Disposition for attachments.
     */
    public static final String DISPOSITION_ATTACHMENT = "attachment";

    private String name;
    private File file;
    private String description;
    private String disposition;
    private byte[] data;
    private String contentType;
    private String contentId;

    /**
     * Creates a new {@link Attachment}. Disposition is set to {@code attachment}.
     *
     * @param name the name
     * @param file the file
     * @param contentType the content type
     */
    public Attachment(String name, File file, String contentType) {
        setName(name)
                .setFile(file)
                .setContentType(contentType)
                .setDisposition(DISPOSITION_ATTACHMENT);
    }

    /**
     * Creates a new {@link Attachment}. Disposition is set to {@code inline}.
     *
     * @param name the name
     * @param file the file
     * @param contentType the content type
     * @param contentId the content id
     */
    public Attachment(String name, File file, String contentType, String contentId) {
        setName(name)
                .setFile(file)
                .setContentType(contentType)
                .setContentId(contentId)
                .setDisposition(DISPOSITION_INLINE);
    }

    /**
     * Creates a new {@link Attachment}. Disposition is set to {@code attachment}.
     *
     * @param name the name
     * @param data the data
     * @param contentType the content type
     */
    public Attachment(String name, byte[] data, String contentType) {
        setName(name)
                .setData(data)
                .setContentType(contentType)
                .setDisposition(DISPOSITION_ATTACHMENT);
    }

    /**
     * Creates a new {@link Attachment}. Disposition is set to {@code inline}.
     *
     * @param name the name
     * @param data the data
     * @param contentType the content type
     * @param contentId the content id
     */
    public Attachment(String name, byte[] data, String contentType, String contentId) {
        setName(name)
                .setData(data)
                .setContentType(contentType)
                .setContentId(contentId)
                .setDisposition(DISPOSITION_INLINE);
    }

    /**
     * Creates a new {@link Attachment}.
     *
     * @param name the name
     * @param data the data
     * @param contentType the content type
     * @param description the description
     * @param disposition the disposition
     */
    public Attachment(String name, byte[] data, String contentType, String description, String disposition) {
        setName(name)
                .setData(data)
                .setContentType(contentType)
                .setDescription(description)
                .setDisposition(disposition);
    }

    public String getName() {
        return name;
    }

    public Attachment setName(String name) {
        this.name = name;
        return this;
    }

    public File getFile() {
        return file;
    }

    public Attachment setFile(File file) {
        this.file = file;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Attachment setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getDisposition() {
        return disposition;
    }

    public Attachment setDisposition(String disposition) {
        this.disposition = disposition;
        return this;
    }

    public byte[] getData() {
        return data;
    }

    public Attachment setData(byte[] data) {
        this.data = data;
        return this;
    }

    public String getContentType() {
        return contentType;
    }

    public Attachment setContentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    public String getContentId() {
        return contentId;
    }

    public Attachment setContentId(String contentId) {
        if (contentId != null && !(contentId.startsWith("<") && contentId.endsWith(">"))) {
            // Auto-wrap the content id between < and >.
            this.contentId = "<" + contentId + ">";
        } else {
            this.contentId = contentId;
        }
        return this;
    }

    /**
     * @return {@code true} if the disposition is set to {@code inline}.
     */
    public boolean isInlineAttachment() {
        return DISPOSITION_INLINE.equalsIgnoreCase(disposition);
    }
}
