package io.quarkus.qute;

public interface ErrorInitializer {

    /**
     *
     * @param message
     * @return a new initialized {@link TemplateException} builder instance
     */
    default TemplateException.Builder error(String message) {
        return TemplateException.builder()
                .message("Rendering error{#if origin.hasNonGeneratedTemplateId??} in{origin}{/if}: " + message);
    }

}
