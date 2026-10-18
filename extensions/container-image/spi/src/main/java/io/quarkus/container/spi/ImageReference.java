/*
 * Copyright 2018 Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package io.quarkus.container.spi;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.quarkus.runtime.util.StringUtil;

/**
 * This is basically a simplified version of {@code com.google.cloud.tools.jib.api.ImageReference}
 */
public class ImageReference {

    public static final String DEFAULT_TAG = "latest";
    private static final String DOCKER_HUB_REGISTRY = "registry-1.docker.io";
    private static final String LIBRARY_REPOSITORY_PREFIX = "library/";

    /**
     * Matches all sequences of alphanumeric characters possibly separated by any number of dashes in
     * the middle.
     */
    private static final String REGISTRY_COMPONENT_REGEX = "(?:[a-zA-Z\\d]|(?:[a-zA-Z\\d][a-zA-Z\\d-]*[a-zA-Z\\d]))";

    /**
     * Matches sequences of {@code REGISTRY_COMPONENT_REGEX} separated by a dot, with an optional
     * {@code :port} at the end.
     */
    private static final String REGISTRY_REGEX = String.format("%s(?:\\.%s)*(?::\\d+)?", REGISTRY_COMPONENT_REGEX,
            REGISTRY_COMPONENT_REGEX);

    /**
     * Matches all sequences of alphanumeric characters separated by a separator.
     *
     * <p>
     * A separator is either an underscore, a dot, two underscores, or any number of dashes.
     */
    private static final String REPOSITORY_COMPONENT_REGEX = "[a-z\\d]+(?:(?:[_.]|__|-+)[a-z\\d]+)*";

    /** Matches all repetitions of {@code REPOSITORY_COMPONENT_REGEX} separated by a backslash. */
    private static final String REPOSITORY_REGEX = String.format("(?:%s/)*%s", REPOSITORY_COMPONENT_REGEX,
            REPOSITORY_COMPONENT_REGEX);

    /** Matches a tag of max length 128. */
    private static final String TAG_REGEX = "[\\w][\\w.-]{0,127}";

    /** Pattern matches a SHA-256 hash - 32 bytes in lowercase hexadecimal. */
    private static final String HASH_REGEX = String.format("[a-f0-9]{%d}", 64);

    /** The algorithm prefix for the digest string. */
    private static final String DIGEST_PREFIX = "sha256:";

    /** Pattern matches a SHA-256 digest - a SHA-256 hash prefixed with "sha256:". */
    private static final String DIGEST_REGEX = DIGEST_PREFIX + HASH_REGEX;

    /**
     * Matches a full image reference, which is the registry, repository, and tag/digest separated by
     * backslashes. The repository is required, but the registry and tag/digest are optional.
     */
    private static final String REFERENCE_REGEX = String.format(
            "^(?:(%s)/)?(%s)(?::(%s))?(?:@(%s))?$",
            REGISTRY_REGEX, REPOSITORY_REGEX, TAG_REGEX, DIGEST_REGEX);

    private static final Pattern REFERENCE_PATTERN = Pattern.compile(REFERENCE_REGEX);

    private final Optional<String> registry;
    private final String repository;
    private final String tag;
    private final String digest;

    /**
     * Returns {@code true} if {@code registry} is a valid registry string. For example, a valid
     * registry could be {@code gcr.io} or {@code localhost:5000}.
     *
     * @param registry the registry to check
     * @return {@code true} if is a valid registry; {@code false} otherwise
     */
    public static boolean isValidRegistry(String registry) {
        return registry.matches(REGISTRY_REGEX);
    }

    /**
     * Returns {@code true} if {@code repository} is a valid repository string. For example, a valid
     * repository could be {@code distroless} or {@code my/container-image/repository}.
     *
     * @param repository the repository to check
     * @return {@code true} if is a valid repository; {@code false} otherwise
     */
    public static boolean isValidRepository(String repository) {
        return repository.matches(REPOSITORY_REGEX);
    }

    /**
     * Returns {@code true} if {@code tag} is a valid tag string. For example, a valid tag could be
     * {@code v120.5-release}.
     *
     * @param tag the tag to check
     * @return {@code true} if is a valid tag; {@code false} otherwise
     */
    public static boolean isValidTag(String tag) {
        return tag.matches(TAG_REGEX);
    }

    /**
     * Parses a string {@code reference} into an {@link ImageReference}.
     *
     * <p>
     * Image references should generally be in the form: {@code <registry>/<repository>:<tag>} For
     * example, an image reference could be {@code gcr.io/distroless/java:debug}.
     *
     * <p>
     * See <a
     * href=
     * "https://docs.docker.com/engine/reference/commandline/tag/#extended-description">https://docs.docker.com/engine/reference/commandline/tag/#extended-description</a>
     * for a description of valid image reference format. Note, however, that the image reference is
     * referred confusingly as {@code tag} on that page.
     *
     * @param reference the string to parse
     * @return an {@link ImageReference} parsed from the string
     * @throws IllegalArgumentException if {@code reference} is formatted incorrectly
     */
    public static ImageReference parse(String reference) {

        Matcher matcher = REFERENCE_PATTERN.matcher(reference);

        if (!matcher.find() || matcher.groupCount() < 4) {
            throw new IllegalArgumentException("Reference " + reference + " is invalid");
        }

        String registry = matcher.group(1);
        String repository = matcher.group(2);
        String tag = matcher.group(3);
        String digest = matcher.group(4);

        if (StringUtil.isNullOrEmpty(repository)) {
            throw new IllegalArgumentException("Reference " + reference + " is invalid: The repository was not set");
        }

        if (StringUtil.isNullOrEmpty(registry)) {
            registry = null;
        } else if (!registry.contains(".") && !registry.contains(":") && !"localhost".equals(registry)) {
            /*
             * If a registry was matched but it does not contain any dots or colons, it should actually be
             * part of the repository unless it is "localhost".
             *
             * See
             * https://github.com/docker/distribution/blob/245ca4659e09e9745f3cc1217bf56e946509220c/reference/normalize.go#L62
             */
            repository = registry + "/" + repository;
            registry = null;
        } else if (DOCKER_HUB_REGISTRY.equals(registry) && repository.indexOf('/') < 0) {
            /*
             * For Docker Hub, if the repository is only one component, then it should be prefixed with
             * 'library/'.
             *
             * See https://docs.docker.com/engine/reference/commandline/pull/#pull-an-image-from-docker-hub
             */
            repository = LIBRARY_REPOSITORY_PREFIX + repository;
        }

        if (StringUtil.isNullOrEmpty(tag) && StringUtil.isNullOrEmpty(digest)) {
            tag = DEFAULT_TAG;
        }
        if (StringUtil.isNullOrEmpty(tag)) {
            tag = null;
        }
        if (StringUtil.isNullOrEmpty(digest)) {
            digest = null;
        }

        return new ImageReference(registry, repository, tag, digest);
    }

    private ImageReference(String registry, String repository, String tag, String digest) {
        this.registry = Optional.ofNullable(registry);
        this.repository = repository;
        this.tag = tag;
        this.digest = digest;
    }

    public Optional<String> getRegistry() {
        return registry;
    }

    public String getEffectiveRegistry() {
        return registry.orElse(DOCKER_HUB_REGISTRY);
    }

    public String getRepository() {
        return repository;
    }

    public String getTag() {
        return tag;
    }

    public String getDigest() {
        return digest;
    }
}
