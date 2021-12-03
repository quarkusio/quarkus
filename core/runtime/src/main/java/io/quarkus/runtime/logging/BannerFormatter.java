package io.quarkus.runtime.logging;

import java.nio.charset.Charset;
import java.util.function.Supplier;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.formatters.ImmutablePatternFormatter;
import org.wildfly.common.annotation.NotNull;

public class BannerFormatter extends ImmutablePatternFormatter {

    private final Supplier<String> bannerSupplier;
    private final ImmutablePatternFormatter formatter;
    private final boolean isColorPattern;

    BannerFormatter(@NotNull ImmutablePatternFormatter formatter, boolean isColorPattern, Supplier<String> bannerSupplier) {
        super(formatter.getPattern());
        this.formatter = formatter;
        this.isColorPattern = isColorPattern;
        this.bannerSupplier = bannerSupplier;
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
    public String format(ExtLogRecord extLogRecord) {
        return formatter.format(extLogRecord);
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
