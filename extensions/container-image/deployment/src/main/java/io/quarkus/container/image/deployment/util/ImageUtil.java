
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
     * @param registry
     *        The registry.
     * @param repository
     *        The group.
     * @param name
     *        The name.
     * @param tag
     *        The tag.
     *
     * @return The image.
     */
    public static String getImage(Optional<String> registry, String group, String name, String tag) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Docker image name cannot be null!");
        }
        if (tag == null || tag.isEmpty()) {
            throw new IllegalArgumentException("Docker image tag cannot be null!");
        }
        StringBuilder sb = new StringBuilder();
        registry.ifPresent(r -> sb.append(r).append(SLASH));
        sb.append(group).append(SLASH);

        sb.append(name).append(COLN).append(tag);
        return sb.toString();
    }

    /**
     * Return the image registry.
     *
     * @param image
     *        The docker image.
     *
     * @return The image registry.
     */
    public static Optional<String> getRegistry(String image) {
        String[] parts = image.split(SLASH);
        if (parts.length <= 2) {
            // name:tag
            // group/name:tag
            return Optional.empty();
        }
        return Optional.ofNullable(parts[0]);
    }

    /**
     * Return the image group.
     *
     * @param image
     *        The docker image.
     *
     * @return The image group.
     */
    public static String getGroup(String image) {
        String[] parts = image.split(SLASH);
        if (parts.length <= 2) {
            // name:tag
            // group/name:tag
            return parts[0];
        }
        return parts[1];
    }

    /**
     * Return the docker image repository.
     *
     * @param image
     *        The docker image.
     *
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
     * @param image
     *        The docker image.
     *
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
            return tagged.substring(0, tagged.lastIndexOf(COLN));
        }
        return tagged;
    }

    /**
     * Return the tag of the image.
     *
     * @param image
     *        The docker image.
     *
     * @return The tag if present or null otherwise.
     */
    public static String getTag(String image) {
        if (image.contains(COLN)) {
            return image.substring(image.lastIndexOf(COLN) + 1);
        }
        return image;
    }

}
