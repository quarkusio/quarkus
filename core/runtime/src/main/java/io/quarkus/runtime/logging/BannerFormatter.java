package io.quarkus.runtime.logging;

import java.nio.charset.Charset;
import java.util.function.Supplier;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.formatters.ColorPatternFormatter;
import org.jboss.logmanager.formatters.FormatStep;
import org.jboss.logmanager.formatters.PatternFormatter;
import org.wildfly.common.annotation.NotNull;

public class BannerFormatter extends ColorPatternFormatter {

    private final Supplier<String> bannerSupplier;
    private Formatter formatter;
    private boolean isColorPattern;

    BannerFormatter(@NotNull Formatter formatter, boolean isColorPattern, Supplier<String> bannerSupplier) {
        this.formatter = formatter;
        this.isColorPattern = isColorPattern;
        this.bannerSupplier = bannerSupplier;

        if (isColorPattern) {
            this.setPattern(((ColorPatternFormatter) formatter).getPattern());
        } else {
            this.setPattern(((PatternFormatter) formatter).getPattern());
        }
    }

    @Override
    public String formatMessage(LogRecord logRecord) {
        if (isColorPattern) {
            return super.formatMessage(logRecord);
        } else {
            return formatter.format(logRecord);
        }
    }

    @Override
    public void setSteps(FormatStep[] steps) {
        if (isColorPattern) {
            super.setSteps(steps);
        } else {
            ((PatternFormatter) formatter).setSteps(steps);
        }
    }

    @Override
    public String format(ExtLogRecord extLogRecord) {
        if (isColorPattern) {
            return ((ColorPatternFormatter) formatter).format(extLogRecord);
        } else {
            return ((PatternFormatter) formatter).format(extLogRecord);
        }
    }

    @Override
    public String getHead(Handler h) {
        final String banner = bannerSupplier.get();
        final String encoding = h.getEncoding();
        final Charset charset;
        try {
            charset = encoding == null ? Charset.defaultCharset() : Charset.forName(encoding);
            return charset.newEncoder().canEncode(banner) ? banner : "";
        } catch (IllegalArgumentException ignored) {
            return "";
        }
    }
}
