package io.quarkus.registry.catalog.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.type.CollectionType;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.immutables.value.Value.Immutable;

@Immutable
@JsonDeserialize(as = ImmutableRepository.class)
public abstract class Repository {

    public abstract List<Extension> getIndividualExtensions();

    public abstract List<Platform> getPlatforms();

    /**
     * - Match all files ending with '.json' inside an extensions directory
     * - Match platforms.json
     */
    public static Repository parse(Path rootPath, ObjectMapper mapper) {
        ObjectReader reader = mapper.reader()
                .with(mapper.getDeserializationConfig().with(PropertyNamingStrategies.KEBAB_CASE));
        return ImmutableRepository.builder()
                .addAllPlatforms(parse(rootPath.resolve("platforms.json"), Platform.class, reader))
                .addAllIndividualExtensions(parse(rootPath.resolve("extensions"), Extension.class, reader))
                .build();
    }

    private static <T> Set<T> parse(Path root, Class<? extends T> type, ObjectReader reader) {
        final Set<T> result = new HashSet<>();
        if (Files.isDirectory(root)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(root, "*.json")) {
                for (Path path : stream) {
                    result.add(reader.forType(type).readValue(path.toFile()));
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            CollectionType collectionType = reader.getTypeFactory().constructCollectionType(List.class, type);
            try {
                result.addAll(reader.forType(collectionType).readValue(root.toFile()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

}
