package io.quarkus.info.deployment;

class GitUtil {

    static String sanitizeRemoteUrl(String remoteUrl) {
        if (remoteUrl == null || remoteUrl.isBlank()) {
            return null;
        }

        String sanitizedRemoteUrl = remoteUrl.trim();
        if (sanitizedRemoteUrl.startsWith("https://")) {
            int atSign = sanitizedRemoteUrl.indexOf('@');
            if (atSign > 0) {
                sanitizedRemoteUrl = "https://" + sanitizedRemoteUrl.substring(atSign + 1);
            }
        } else if (sanitizedRemoteUrl.startsWith("http://")) {
            int atSign = sanitizedRemoteUrl.indexOf('@');
            if (atSign > 0) {
                sanitizedRemoteUrl = "http://" + sanitizedRemoteUrl.substring(atSign + 1);
            }
        } else {
            int atSign = sanitizedRemoteUrl.indexOf('@');
            if (atSign > 0) {
                sanitizedRemoteUrl = sanitizedRemoteUrl.substring(atSign + 1);
            }
        }

        return sanitizedRemoteUrl;
    }
}
