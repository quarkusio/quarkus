package io.quarkus.agroal.test;

import java.io.FileNotFoundException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.SimpleFormatter;
import java.util.stream.Collectors;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.h2.api.DatabaseEventListener;
import org.jboss.logmanager.Logger;
import org.jboss.logmanager.handlers.FileHandler;

public class QueryLoggingH2DBEventListener implements DatabaseEventListener {

    Logger logger = Logger.getLogger(QueryLoggingH2DBEventListener.class.getName());

    @Override
    public void init(String url) {
        int index = url.indexOf("?");
        if (index == -1) {
            throw new IllegalArgumentException("jdbc uri doesn't contain queryLog param: " + url);
        }
        String query = url.substring(index + 1);
        Map<String, String> params = URLEncodedUtils.parse(query, StandardCharsets.UTF_8).stream()
                .collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));
        String filePath = params.get("queryLog");
        if (filePath == null) {
            throw new IllegalArgumentException("jdbc uri doesn't contain queryLog param: " + url);
        }

        try {
            Handler handler = new FileHandler(filePath, false);
            handler.setFormatter(new SimpleFormatter());
            logger.addHandler(handler);
        } catch (FileNotFoundException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void setProgress(int state, String name, long x, long max) {
        if (state == DatabaseEventListener.STATE_STATEMENT_START) {
            logger.info(name);
        }
    }
}
