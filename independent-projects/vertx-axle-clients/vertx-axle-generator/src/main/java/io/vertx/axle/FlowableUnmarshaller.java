package io.vertx.axle;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.Flowable;
import io.reactivex.FlowableTransformer;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.annotations.NonNull;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import org.reactivestreams.Publisher;

import java.io.IOException;

import static java.util.Objects.nonNull;

/**
 * An operator to unmarshall json to pojos.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class FlowableUnmarshaller<T, B> implements FlowableTransformer<B, T> {

  private final java.util.function.Function<B, Buffer> unwrap;
  private final Class<T> mappedType;
  private final TypeReference<T> mappedTypeRef;
  private final ObjectMapper mapper;

  public FlowableUnmarshaller(java.util.function.Function<B, Buffer> unwrap, Class<T> mappedType) {
    this(unwrap, mappedType, null, Json.mapper);
  }

  public FlowableUnmarshaller(java.util.function.Function<B, Buffer> unwrap, TypeReference<T> mappedTypeRef) {
    this(unwrap, null, mappedTypeRef, Json.mapper);
  }

  public FlowableUnmarshaller(java.util.function.Function<B, Buffer> unwrap, Class<T> mappedType, ObjectMapper mapper) {
    this(unwrap, mappedType, null, mapper);
  }

  public FlowableUnmarshaller(java.util.function.Function<B, Buffer> unwrap, TypeReference<T> mappedTypeRef, ObjectMapper mapper) {
    this(unwrap, null, mappedTypeRef, mapper);
  }

  private FlowableUnmarshaller(java.util.function.Function<B, Buffer> unwrap, Class<T> mappedType, TypeReference<T> mappedTypeRef, ObjectMapper mapper) {
    this.unwrap = unwrap;
    this.mappedType = mappedType;
    this.mappedTypeRef = mappedTypeRef;
    this.mapper = mapper;
  }

  @Override
  public Publisher<T> apply(@NonNull Flowable<B> upstream) {
    Flowable<Buffer> unwrapped = upstream.map(unwrap::apply);
    Single<Buffer> aggregated = unwrapped.collect(Buffer::buffer, Buffer::appendBuffer);
    Maybe<T> unmarshalled = aggregated.toMaybe().concatMap(buffer -> {
      if (buffer.length() > 0) {
        try {
          T obj = nonNull(mappedType) ? mapper.readValue(buffer.getBytes(), mappedType) :
            mapper.readValue(buffer.getBytes(), mappedTypeRef);
          return Maybe.just(obj);
        } catch (IOException e) {
          return Maybe.error(e);
        }
      } else {
        return Maybe.empty();
      }
    });
    return unmarshalled.toFlowable();
  }
}
