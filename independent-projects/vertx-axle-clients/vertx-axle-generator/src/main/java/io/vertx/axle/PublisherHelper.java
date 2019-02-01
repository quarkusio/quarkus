package io.vertx.axle;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.Flowable;
import io.reactivex.FlowableTransformer;
import io.reactivex.plugins.RxJavaPlugins;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
import org.reactivestreams.Publisher;

import java.util.function.Function;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class PublisherHelper {

  /**
   * Adapts an RxJava {@link Flowable<T>} to a Vert.x {@link ReadStream<T>}. The returned
   * readstream will be subscribed to the {@link Flowable<T>}.<p>
   *
   * @param observable the observable to adapt
   * @return the adapted stream
   */
  public static <T> ReadStream<T> toReadStream(Flowable<T> observable) {
    return ReadStreamSubscriber.asReadStream(observable, Function.identity());
  }

  /**
   * Like {@link #toFlowable(ReadStream)} but with a {@code mapping} function
   */
  public static <T, U> Publisher<U> toPublisher(ReadStream<T> stream, Function<T, U> mapping) {
    return RxJavaPlugins.onAssembly(new FlowableReadStream<>(stream, FlowableReadStream.DEFAULT_MAX_BUFFER_SIZE, mapping));
  }

  /**
   * Like {@link #toFlowable(ReadStream)} but with a {@code mapping} function
   */
  public static <T, U> Flowable<U> toFlowable(ReadStream<T> stream, Function<T, U> mapping) {
    return RxJavaPlugins.onAssembly(new FlowableReadStream<>(stream, FlowableReadStream.DEFAULT_MAX_BUFFER_SIZE, mapping));
  }

  /**
   * Adapts a Vert.x {@link ReadStream<T>} to an RxJava {@link Flowable<T>}. After
   * the stream is adapted to a flowable, the original stream handlers should not be used anymore
   * as they will be used by the flowable adapter.<p>
   *
   * @param stream the stream to adapt
   * @return the adapted observable
   */
  public static <T> Publisher<T> toPublisher(ReadStream<T> stream) {
    return RxJavaPlugins.onAssembly(new FlowableReadStream<>(stream, FlowableReadStream.DEFAULT_MAX_BUFFER_SIZE, Function.identity()));
  }

  /**
   * Adapts a Vert.x {@link ReadStream<T>} to an RxJava {@link Flowable<T>}. After
   * the stream is adapted to a flowable, the original stream handlers should not be used anymore
   * as they will be used by the flowable adapter.<p>
   *
   * @param stream the stream to adapt
   * @return the adapted observable
   */
  public static <T> Flowable<T> toFlowable(ReadStream<T> stream) {
    return RxJavaPlugins.onAssembly(new FlowableReadStream<>(stream, FlowableReadStream.DEFAULT_MAX_BUFFER_SIZE, Function.identity()));
  }

  /**
   * Adapts a Vert.x {@link ReadStream<T>} to an RxJava {@link Flowable<T>}. After
   * the stream is adapted to a flowable, the original stream handlers should not be used anymore
   * as they will be used by the flowable adapter.<p>
   *
   * @param stream the stream to adapt
   * @return the adapted observable
   */
  public static <T> Flowable<T> toFlowable(ReadStream<T> stream, long maxBufferSize) {
    return RxJavaPlugins.onAssembly(new FlowableReadStream<>(stream, maxBufferSize, Function.identity()));
  }

  public static <T> FlowableTransformer<Buffer, T> unmarshaller(Class<T> mappedType) {
    return new FlowableUnmarshaller<>(Function.identity(), mappedType);
  }

  public static <T> FlowableTransformer<Buffer, T>unmarshaller(TypeReference<T> mappedTypeRef) {
    return new FlowableUnmarshaller<>(Function.identity(), mappedTypeRef);
  }

  public static <T> FlowableTransformer<Buffer, T> unmarshaller(Class<T> mappedType, ObjectMapper mapper) {
    return new FlowableUnmarshaller<>(Function.identity(), mappedType, mapper);
  }

  public static <T> FlowableTransformer<Buffer, T>unmarshaller(TypeReference<T> mappedTypeRef, ObjectMapper mapper) {
    return new FlowableUnmarshaller<>(Function.identity(), mappedTypeRef, mapper);
  }
}
