package io.quarkus.maven.config.doc.generator;

import java.util.List;
import java.util.regex.Pattern;

import io.quarkus.annotation.processor.documentation.config.merger.JavadocRepository;
import io.quarkus.annotation.processor.documentation.config.model.ConfigProperty;
import io.quarkus.annotation.processor.documentation.config.model.Extension;
import io.quarkus.annotation.processor.documentation.config.model.JavadocFormat;
import io.quarkus.maven.config.doc.GenerateConfigDocMojo.Context;

final class AsciidocFormatter extends AbstractFormatter {

    private static final String TOOLTIP_MACRO = "tooltip:%s[%s]";
    private static final String MORE_INFO_ABOUT_TYPE_FORMAT = "link:#%s[icon:question-circle[title=More information about the %s format]]";

    private static final String CORE_EXTENSION_BASE_URL = "https://quarkus.io/guides/";
    private static final String ADOC_SUFFIX = ".adoc";
    private static final String SOURCE_BLOCK_PREFIX = "[source";
    private static final String SOURCE_BLOCK_DELIMITER = "--";
    private static final Pattern XREF_PATTERN = Pattern.compile("xref:([^\\[]+)\\[");
    private static final Pattern ANGLE_BRACKETS_WITHOUT_DESCRIPTION_PATTERN = Pattern.compile("<<([a-z0-9_\\-#\\.]+?)>>",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern ANGLE_BRACKETS_WITH_DESCRIPTION_PATTERN = Pattern.compile("<<([a-z0-9_\\-#\\.]+?),([^>]+?)>>",
            Pattern.CASE_INSENSITIVE);

    AsciidocFormatter(GenerationReport generationReport, JavadocRepository javadocRepository, boolean enableEnumTooltips) {
        super(generationReport, javadocRepository, enableEnumTooltips);
    }

    @Override
    protected JavadocFormat javadocFormat() {
        return JavadocFormat.ASCIIDOC;
    }

    protected String moreInformationAboutType(Context context, String anchorRoot, String type) {
        return String.format(MORE_INFO_ABOUT_TYPE_FORMAT, anchorRoot + "-" + (context != null ? context.summaryTableId() : ""),
                type);
    }

    protected String tooltip(String value, String javadocDescription) {
        return String.format(TOOLTIP_MACRO, value, cleanTooltipContent(javadocDescription));
    }

    /**
     * Note that this is extremely brittle. Apparently, colons breaks the tooltips but if escaped with \, the \ appears in the
     * output.
     * <p>
     * We should probably have some warnings/errors as to what is accepted in enum Javadoc.
     */
    private String cleanTooltipContent(String tooltipContent) {
        return tooltipContent.replace("<p>", "").replace("</p>", "").replace("\n+\n", " ").replace("\n", " ")
                .replace(":", "\\:").replace("[", "\\]").replace("]", "\\]");
    }

    protected String link(String href, String description) {
        return String.format("link:%s[%s]", href, description);
    }

    @Override
    public String formatDescription(ConfigProperty configProperty, Extension extension, Context context) {
        String description = formatDescription(configProperty);

        if (description == null || extension == null || extension.guideUrl() == null || context == null ||
                !context.allConfig()) {
            return description;
        }

        // we need to rewrite the links relative to the extension
        String baseGuideUrl;
        if (extension.guideUrl().startsWith(CORE_EXTENSION_BASE_URL)) {
            baseGuideUrl = extension.guideUrl().substring(CORE_EXTENSION_BASE_URL.length()) + ADOC_SUFFIX;
        } else {
            baseGuideUrl = extension.guideUrl();
        }

        String configPropertyAnchorPrefix = extension.artifactId() + "_";

        return rewriteAnchors(configProperty.getPath().property(), description, baseGuideUrl,
                configPropertyAnchorPrefix) + "\n\n";
    }

    private static String rewriteAnchors(String propertyPath, String description, String baseGuideUrl,
            String configPropertyAnchorPrefix) {
        List<String> lines = description.lines().toList();

        StringBuilder rewrittenGuide = new StringBuilder();
        StringBuilder currentBuffer = new StringBuilder();
        boolean inSourceBlock = false;
        boolean findDelimiter = false;
        String currentSourceBlockDelimiter = "----";
        int lineNumber = 0;

        for (String line : lines) {
            lineNumber++;

            if (inSourceBlock) {
                if (findDelimiter) {
                    rewrittenGuide.append(line + "\n");
                    if (line.isBlank() || line.startsWith(".")) {
                        continue;
                    }
                    if (!line.startsWith(SOURCE_BLOCK_DELIMITER)) {
                        throw new IllegalStateException("Unable to find source block delimiter for property "
                                + propertyPath + " at line " + lineNumber);
                    }
                    currentSourceBlockDelimiter = line.stripTrailing();
                    findDelimiter = false;
                    continue;
                }

                if (line.stripTrailing().equals(currentSourceBlockDelimiter)) {
                    inSourceBlock = false;
                }
                rewrittenGuide.append(line + "\n");
                continue;
            }
            if (line.startsWith(SOURCE_BLOCK_PREFIX)) {
                inSourceBlock = true;
                findDelimiter = true;

                if (currentBuffer.length() > 0) {
                    rewrittenGuide.append(rewriteAnchors(currentBuffer.toString(), baseGuideUrl, configPropertyAnchorPrefix));
                    currentBuffer.setLength(0);
                }
                rewrittenGuide.append(line + "\n");
                continue;
            }

            currentBuffer.append(line + "\n");
        }

        if (currentBuffer.length() > 0) {
            rewrittenGuide.append(rewriteAnchors(currentBuffer.toString(), baseGuideUrl, configPropertyAnchorPrefix));
        }

        return rewrittenGuide.toString().trim();
    }

    private static String rewriteAnchors(String content, String baseGuideUrl, String configPropertyAnchorPrefix) {
        content = XREF_PATTERN.matcher(content).replaceAll(mr -> {
            String reference = getQualifiedReference(mr.group(1), baseGuideUrl, configPropertyAnchorPrefix);
            return "xref:" + reference + "[";
        });

        content = ANGLE_BRACKETS_WITHOUT_DESCRIPTION_PATTERN.matcher(content).replaceAll(mr -> {
            String reference = getQualifiedReference(mr.group(1), baseGuideUrl, configPropertyAnchorPrefix);
            return "<<" + reference + ">>";
        });

        content = ANGLE_BRACKETS_WITH_DESCRIPTION_PATTERN.matcher(content).replaceAll(mr -> {
            String reference = getQualifiedReference(mr.group(1), baseGuideUrl, configPropertyAnchorPrefix);
            return "<<" + reference + "," + mr.group(2) + ">>";
        });

        return content;
    }

    private static String getQualifiedReference(String reference, String baseGuideUrl, String configPropertyAnchorPrefix) {
        if (reference.contains(ADOC_SUFFIX)) {
            return reference;
        }
        if (reference.startsWith("#")) {
            reference = reference.substring(1);
        }
        if (reference.startsWith(configPropertyAnchorPrefix)) {
            return reference;
        }

        return baseGuideUrl + "#" + reference;
    }
}
