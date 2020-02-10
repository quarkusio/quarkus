
package io.quarkus.container.image.deployment.util;

import java.util.Optional;

public final class ImageUtil {

    private static final String SLASH = "/";
    private static final String COLN = ":";

    private ImageUtil() {
    }

    /**
     * Create an image from the individual parts.
     * 
     * @param registry The registry.
     * @param repository The repository.
     * @param name The name.
     * @param tag The tag.
     * @return The image.
     */
    public static String getImage(Optional<String> registry, String repository, String name, String tag) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Docker image name cannot be null!");
        }
        if (tag == null || tag.isEmpty()) {
            throw new IllegalArgumentException("Docker image tag cannot be null!");
        }
        StringBuilder sb = new StringBuilder();
        registry.ifPresent(r -> sb.append(r).append(SLASH));
        sb.append(repository).append(SLASH);

        sb.append(name).append(COLN).append(tag);
        return sb.toString();
    }

    /**
     * Return the registry part of the docker image.
     * 
     * @param image The actual docker image.
     * @return The registry or null, if not registry was found.
     */
    public static String getRegistry(String image) {
        String[] parts = image.split(SLASH);
        if (parts.length <= 2) {
            return null;
        } else {
            return parts[0];
        }
    }

    /**
     * Return the docker image repository.
     * 
     * @param image The docker image.
     * @return The image repository.
     */
    public static String getRepository(String image) {
        String[] parts = image.split(SLASH);
        String tagged = image;
        if (parts.length <= 2) {
            tagged = image;
        } else if (parts.length == 3) {
            tagged = parts[1] + SLASH + parts[2];
        }

        if (tagged.contains(COLN)) {
            return tagged.substring(0, tagged.indexOf(COLN));
        }
        return tagged;
    }

    /**
     * Return the docker image name.
     * 
     * @param image The docker image.
     * @return The image name.
     */
    public static String getName(String image) {
        String[] parts = image.split(SLASH);
        String tagged = image;
        if (parts.length == 1) {
            tagged = image;
        } else {
            tagged = parts[parts.length - 1];
        }

        if (tagged.contains(COLN)) {
            return tagged.substring(0, tagged.indexOf(COLN));
        }
        return tagged;
    }

    /**
     * Return the tag of the image.
     * 
     * @param image The docker image.
     * @return The tag if present or null otherwise.
     */
    public static String getTag(String image) {
        if (image.contains(COLN)) {
            return image.substring(image.indexOf(COLN) + 1);
        }
        return image;
    }

}
