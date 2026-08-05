package io.quarkus.devmcp.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.jboss.logging.Logger;

public class DevMcpFilterHelper {
    private static final Logger LOG = Logger.getLogger(DevMcpFilterHelper.class);

    public static List<Pattern> detectPatterns(List<String> hosts) {
        if (hosts != null && !hosts.isEmpty()) {
            List<Pattern> pat = new ArrayList<>();
            for (String h : hosts) {
                Pattern p = toPattern(h);
                if (p != null) {
                    pat.add(p);
                }
            }
            if (!pat.isEmpty()) {
                return pat;
            }
        }
        return null;
    }

    private static Pattern toPattern(String regex) {
        try {
            return Pattern.compile(regex);
        } catch (PatternSyntaxException e) {
            LOG.warnf("Invalid host pattern '%s': %s", regex, e.getMessage());
            return null;
        }
    }

}
