package org.jboss.resteasy.reactive;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.NoContentException;
import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.core.Response.StatusType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Variant;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.RuntimeDelegate;

/**
 * Defines the contract between a returned instance and the runtime when
 * an application needs to provide meta-data to the runtime.
 * <p>
 * An application class should not extend this class directly. {@code Response} class is
 * reserved for an extension by API implementation providers. An application should use one
 * of the static methods to create a {@code Response} instance using a ResponseBuilder.
 * </p>
 * <p>
 * Several methods have parameters of type URI, {@link UriBuilder} provides
 * convenient methods to create such values as does {@link URI#create(java.lang.String)}.
 * </p>
 *
 * @see RestResponse.ResponseBuilder
 * @since 1.0
 */
public abstract class RestResponse<T> implements AutoCloseable {

    /**
     * Protected constructor, use one of the static methods to obtain a
     * {@link ResponseBuilder} instance and obtain a RestResponse from that.
     */
    protected RestResponse() {
    }

    /**
     * Get the status code associated with the response.
     *
     * @return the response status code.
     */
    public abstract int getStatus();

    /**
     * Get the complete status information associated with the response.
     *
     * @return the response status information. The returned value is never
     *         {@code null}.
     */
    public abstract StatusType getStatusInfo();

    /**
     * Get the message entity Java instance. Returns {@code null} if the message
     * does not contain an entity body.
     * <p>
     * If the entity is represented by an un-consumed {@link InputStream input stream}
     * the method will return the input stream.
     * </p>
     *
     * @return the message entity or {@code null} if message does not contain an
     *         entity body (i.e. when {@link #hasEntity()} returns {@code false}).
     * @throws IllegalStateException if the entity was previously fully consumed
     *         as an {@link InputStream input stream}, or
     *         if the response has been {@link #close() closed}.
     */
    public abstract T getEntity();

    /**
     * Read the message entity input stream as an instance of specified Java type
     * using a {@link javax.ws.rs.ext.MessageBodyReader} that supports mapping the
     * message entity stream onto the requested type.
     * <p>
     * Method throws an {@link ProcessingException} if the content of the
     * message cannot be mapped to an entity of the requested type and
     * {@link IllegalStateException} in case the entity is not backed by an input
     * stream or if the original entity input stream has already been consumed
     * without {@link #bufferEntity() buffering} the entity data prior consuming.
     * </p>
     * <p>
     * A message instance returned from this method will be cached for
     * subsequent retrievals via {@link #getEntity()}. Unless the supplied entity
     * type is an {@link java.io.InputStream input stream}, this method automatically
     * {@link #close() closes} the an unconsumed original response entity data stream
     * if open. In case the entity data has been buffered, the buffer will be reset
     * prior consuming the buffered data to enable subsequent invocations of
     * {@code readEntity(...)} methods on this response.
     * </p>
     *
     * @param <OtherT> entity instance Java type.
     * @param entityType the type of entity.
     * @return the message entity; for a zero-length response entities returns a corresponding
     *         Java object that represents zero-length data. In case no zero-length representation
     *         is defined for the Java type, a {@link ProcessingException} wrapping the
     *         underlying {@link NoContentException} is thrown.
     * @throws ProcessingException if the content of the message cannot be
     *         mapped to an entity of the requested type.
     * @throws IllegalStateException if the entity is not backed by an input stream,
     *         the response has been {@link #close() closed} already,
     *         or if the entity input stream has been fully consumed already and has
     *         not been buffered prior consuming.
     * @see javax.ws.rs.ext.MessageBodyReader
     */
    public abstract <OtherT> OtherT readEntity(Class<OtherT> entityType);

    /**
     * Read the message entity input stream as an instance of specified Java type
     * using a {@link javax.ws.rs.ext.MessageBodyReader} that supports mapping the
     * message entity stream onto the requested type.
     * <p>
     * Method throws an {@link ProcessingException} if the content of the
     * message cannot be mapped to an entity of the requested type and
     * {@link IllegalStateException} in case the entity is not backed by an input
     * stream or if the original entity input stream has already been consumed
     * without {@link #bufferEntity() buffering} the entity data prior consuming.
     * </p>
     * <p>
     * A message instance returned from this method will be cached for
     * subsequent retrievals via {@link #getEntity()}. Unless the supplied entity
     * type is an {@link java.io.InputStream input stream}, this method automatically
     * {@link #close() closes} the an unconsumed original response entity data stream
     * if open. In case the entity data has been buffered, the buffer will be reset
     * prior consuming the buffered data to enable subsequent invocations of
     * {@code readEntity(...)} methods on this response.
     * </p>
     *
     * @param <OtherT> entity instance Java type.
     * @param entityType the type of entity; may be generic.
     * @return the message entity; for a zero-length response entities returns a corresponding
     *         Java object that represents zero-length data. In case no zero-length representation
     *         is defined for the Java type, a {@link ProcessingException} wrapping the
     *         underlying {@link NoContentException} is thrown.
     * @throws ProcessingException if the content of the message cannot be
     *         mapped to an entity of the requested type.
     * @throws IllegalStateException if the entity is not backed by an input stream,
     *         the response has been {@link #close() closed} already,
     *         or if the entity input stream has been fully consumed already and has
     *         not been buffered prior consuming.
     * @see javax.ws.rs.ext.MessageBodyReader
     */
    public abstract <OtherT> OtherT readEntity(GenericType<OtherT> entityType);

    /**
     * Read the message entity input stream as an instance of specified Java type
     * using a {@link javax.ws.rs.ext.MessageBodyReader} that supports mapping the
     * message entity stream onto the requested type.
     * <p>
     * Method throws an {@link ProcessingException} if the content of the
     * message cannot be mapped to an entity of the requested type and
     * {@link IllegalStateException} in case the entity is not backed by an input
     * stream or if the original entity input stream has already been consumed
     * without {@link #bufferEntity() buffering} the entity data prior consuming.
     * </p>
     * <p>
     * A message instance returned from this method will be cached for
     * subsequent retrievals via {@link #getEntity()}. Unless the supplied entity
     * type is an {@link java.io.InputStream input stream}, this method automatically
     * {@link #close() closes} the an unconsumed original response entity data stream
     * if open. In case the entity data has been buffered, the buffer will be reset
     * prior consuming the buffered data to enable subsequent invocations of
     * {@code readEntity(...)} methods on this response.
     * </p>
     *
     * @param <OtherT> entity instance Java type.
     * @param entityType the type of entity.
     * @param annotations annotations that will be passed to the {@link MessageBodyReader}.
     * @return the message entity; for a zero-length response entities returns a corresponding
     *         Java object that represents zero-length data. In case no zero-length representation
     *         is defined for the Java type, a {@link ProcessingException} wrapping the
     *         underlying {@link NoContentException} is thrown.
     * @throws ProcessingException if the content of the message cannot be
     *         mapped to an entity of the requested type.
     * @throws IllegalStateException if the entity is not backed by an input stream,
     *         the response has been {@link #close() closed} already,
     *         or if the entity input stream has been fully consumed already and has
     *         not been buffered prior consuming.
     * @see javax.ws.rs.ext.MessageBodyReader
     */
    public abstract <OtherT> OtherT readEntity(Class<OtherT> entityType, Annotation[] annotations);

    /**
     * Read the message entity input stream as an instance of specified Java type
     * using a {@link javax.ws.rs.ext.MessageBodyReader} that supports mapping the
     * message entity stream onto the requested type.
     * <p>
     * Method throws an {@link ProcessingException} if the content of the
     * message cannot be mapped to an entity of the requested type and
     * {@link IllegalStateException} in case the entity is not backed by an input
     * stream or if the original entity input stream has already been consumed
     * without {@link #bufferEntity() buffering} the entity data prior consuming.
     * </p>
     * <p>
     * A message instance returned from this method will be cached for
     * subsequent retrievals via {@link #getEntity()}. Unless the supplied entity
     * type is an {@link java.io.InputStream input stream}, this method automatically
     * {@link #close() closes} the an unconsumed original response entity data stream
     * if open. In case the entity data has been buffered, the buffer will be reset
     * prior consuming the buffered data to enable subsequent invocations of
     * {@code readEntity(...)} methods on this response.
     * </p>
     *
     * @param <OtherT> entity instance Java type.
     * @param entityType the type of entity; may be generic.
     * @param annotations annotations that will be passed to the {@link MessageBodyReader}.
     * @return the message entity; for a zero-length response entities returns a corresponding
     *         Java object that represents zero-length data. In case no zero-length representation
     *         is defined for the Java type, a {@link ProcessingException} wrapping the
     *         underlying {@link NoContentException} is thrown.
     * @throws ProcessingException if the content of the message cannot be
     *         mapped to an entity of the requested type.
     * @throws IllegalStateException if the entity is not backed by an input stream,
     *         the response has been {@link #close() closed} already,
     *         or if the entity input stream has been fully consumed already and has
     *         not been buffered prior consuming.
     * @see javax.ws.rs.ext.MessageBodyReader
     */
    public abstract <OtherT> OtherT readEntity(GenericType<OtherT> entityType, Annotation[] annotations);

    /**
     * Check if there is an entity available in the response. The method returns
     * {@code true} if the entity is present, returns {@code false} otherwise.
     * <p>
     * Note that the method may return {@code true} also for response messages with
     * a zero-length content, in case the <code>{@value javax.ws.rs.core.HttpHeaders#CONTENT_LENGTH}</code> and
     * <code>{@value javax.ws.rs.core.HttpHeaders#CONTENT_TYPE}</code> headers are specified in the message.
     * In such case, an attempt to read the entity using one of the {@code readEntity(...)}
     * methods will return a corresponding instance representing a zero-length entity for a
     * given Java type or produce a {@link ProcessingException} in case no such instance
     * is available for the Java type.
     * </p>
     *
     * @return {@code true} if there is an entity present in the message,
     *         {@code false} otherwise.
     * @throws IllegalStateException in case the response has been {@link #close() closed}.
     */
    public abstract boolean hasEntity();

    /**
     * Buffer the message entity data.
     * <p>
     * In case the message entity is backed by an unconsumed entity input stream,
     * all the bytes of the original entity input stream are read and stored in a
     * local buffer. The original entity input stream is consumed and automatically
     * closed as part of the operation and the method returns {@code true}.
     * </p>
     * <p>
     * In case the response entity instance is not backed by an unconsumed input stream
     * an invocation of {@code bufferEntity} method is ignored and the method returns
     * {@code false}.
     * </p>
     * <p>
     * This operation is idempotent, i.e. it can be invoked multiple times with
     * the same effect which also means that calling the {@code bufferEntity()}
     * method on an already buffered (and thus closed) message instance is legal
     * and has no further effect. Also, the result returned by the {@code bufferEntity()}
     * method is consistent across all invocations of the method on the same
     * {@code RestResponse} instance.
     * </p>
     * <p>
     * Buffering the message entity data allows for multiple invocations of
     * {@code readEntity(...)} methods on the response instance. Note however, that
     * once the response instance itself is {@link #close() closed}, the implementations
     * are expected to release the buffered message entity data too. Therefore any subsequent
     * attempts to read a message entity stream on such closed response will result in an
     * {@link IllegalStateException} being thrown.
     * </p>
     *
     * @return {@code true} if the message entity input stream was available and
     *         was buffered successfully, returns {@code false} if the entity stream
     *         was not available.
     * @throws ProcessingException if there was an error while buffering the entity
     *         input stream.
     * @throws IllegalStateException in case the response has been {@link #close() closed}.
     */
    public abstract boolean bufferEntity();

    /**
     * Close the underlying message entity input stream (if available and open)
     * as well as releases any other resources associated with the response
     * (e.g. {@link #bufferEntity() buffered message entity data}).
     * <p>
     * This operation is idempotent, i.e. it can be invoked multiple times with the
     * same effect which also means that calling the {@code close()} method on an
     * already closed message instance is legal and has no further effect.
     * </p>
     * <p>
     * The {@code close()} method should be invoked on all instances that
     * contain an un-consumed entity input stream to ensure the resources associated
     * with the instance are properly cleaned-up and prevent potential memory leaks.
     * This is typical for client-side scenarios where application layer code
     * processes only the response headers and ignores the response entity.
     * </p>
     * <p>
     * Any attempts to manipulate (read, get, buffer) a message entity on a closed response
     * will result in an {@link IllegalStateException} being thrown.
     * </p>
     *
     * @throws ProcessingException if there is an error closing the response.
     */
    @Override
    public abstract void close();

    /**
     * Get the media type of the message entity.
     *
     * @return the media type or {@code null} if there is no response entity.
     */
    public abstract MediaType getMediaType();

    /**
     * Get the language of the message entity.
     *
     * @return the language of the entity or null if not specified.
     */
    public abstract Locale getLanguage();

    /**
     * Get Content-Length value.
     *
     * @return Content-Length as integer if present and valid number. In other
     *         cases returns {@code -1}.
     */
    public abstract int getLength();

    /**
     * Get the allowed HTTP methods from the Allow HTTP header.
     *
     * @return the allowed HTTP methods, all methods will returned as upper case
     *         strings.
     */
    public abstract Set<String> getAllowedMethods();

    /**
     * Get any new cookies set on the response message.
     *
     * @return a read-only map of cookie name (String) to Cookie.
     */
    public abstract Map<String, NewCookie> getCookies();

    /**
     * Get the entity tag.
     *
     * @return the entity tag, otherwise {@code null} if not present.
     */
    public abstract EntityTag getEntityTag();

    /**
     * Get message date.
     *
     * @return the message date, otherwise {@code null} if not present.
     */
    public abstract Date getDate();

    /**
     * Get the last modified date.
     *
     * @return the last modified date, otherwise {@code null} if not present.
     */
    public abstract Date getLastModified();

    /**
     * Get the location.
     *
     * @return the location URI, otherwise {@code null} if not present.
     */
    public abstract URI getLocation();

    /**
     * Get the links attached to the message as headers. Any links in the message
     * that are relative must be resolved with respect to the actual request URI
     * that produced this response. Note that request URIs may be updated by
     * filters, so the actual request URI may differ from that in the original
     * invocation.
     *
     * @return links, may return empty {@link Set} if no links are present. Does
     *         not return {@code null}.
     */
    public abstract Set<Link> getLinks();

    /**
     * Check if link for relation exists.
     *
     * @param relation link relation.
     * @return {@code true} if the link for the relation is present in the
     *         {@link #getHeaders() message headers}, {@code false} otherwise.
     */
    public abstract boolean hasLink(String relation);

    /**
     * Get the link for the relation. A relative link is resolved with respect
     * to the actual request URI that produced this response. Note that request
     * URIs may be updated by filters, so the actual request URI may differ from
     * that in the original invocation.
     *
     * @param relation link relation.
     * @return the link for the relation, otherwise {@code null} if not present.
     */
    public abstract Link getLink(String relation);

    /**
     * Convenience method that returns a {@link Link.Builder} for the relation.
     * See {@link #getLink} for more information.
     *
     * @param relation link relation.
     * @return the link builder for the relation, otherwise {@code null} if not
     *         present.
     */
    public abstract Link.Builder getLinkBuilder(String relation);

    /**
     * See {@link #getHeaders()}.
     *
     * This method is considered deprecated. Users are encouraged to switch their
     * code to use the {@code getHeaders()} method instead. The method may be annotated
     * as {@link Deprecated &#64;Deprecated} in a future release of the API.
     *
     * @return response headers as a multivalued map.
     */
    public abstract MultivaluedMap<String, Object> getMetadata();

    /**
     * Get view of the response headers and their object values.
     *
     * The underlying header data may be subsequently modified by the runtime on the
     * server side. Changes in the underlying header data are reflected in this view.
     * <p>
     * On the server-side, when the message is sent, the non-string values will be serialized
     * using a {@link javax.ws.rs.ext.RuntimeDelegate.HeaderDelegate} if one is available via
     * {@link javax.ws.rs.ext.RuntimeDelegate#createHeaderDelegate(java.lang.Class)} for the
     * class of the value or using the values {@code toString} method if a header delegate is
     * not available.
     * </p>
     * <p>
     * On the client side, the returned map is identical to the one returned by
     * {@link #getStringHeaders()}.
     * </p>
     *
     * @return response headers as an object view of header values.
     * @see #getStringHeaders()
     * @see #getHeaderString
     */
    public MultivaluedMap<String, Object> getHeaders() {
        return getMetadata();
    }

    /**
     * Get view of the response headers and their string values.
     *
     * The underlying header data may be subsequently modified by the runtime on
     * the server side. Changes in the underlying header data are reflected in this view.
     *
     * @return response headers as a string view of header values.
     * @see #getHeaders()
     * @see #getHeaderString
     */
    public abstract MultivaluedMap<String, String> getStringHeaders();

    /**
     * Get a message header as a single string value.
     *
     * Each single header value is converted to String using a
     * {@link javax.ws.rs.ext.RuntimeDelegate.HeaderDelegate} if one is available
     * via {@link javax.ws.rs.ext.RuntimeDelegate#createHeaderDelegate(java.lang.Class)}
     * for the header value class or using its {@code toString} method if a header
     * delegate is not available.
     *
     * @param name the message header.
     * @return the message header value. If the message header is not present then
     *         {@code null} is returned. If the message header is present but has no
     *         value then the empty string is returned. If the message header is present
     *         more than once then the values of joined together and separated by a ','
     *         character.
     * @see #getHeaders()
     * @see #getStringHeaders()
     */
    public abstract String getHeaderString(String name);

    /**
     * Create a new ResponseBuilder by performing a shallow copy of an
     * existing RestResponse.
     * <p>
     * The returned builder has its own {@link #getHeaders() response headers}
     * but the header values are shared with the original {@code RestResponse} instance.
     * The original response entity instance reference is set in the new response
     * builder.
     * </p>
     * <p>
     * Note that if the entity is backed by an un-consumed input stream, the
     * reference to the stream is copied. In such case make sure to
     * {@link #bufferEntity() buffer} the entity stream of the original response
     * instance before passing it to this method.
     * </p>
     *
     * @param response a RestResponse from which the status code, entity and
     *        {@link #getHeaders() response headers} will be copied.
     * @return a new response builder.
     */
    public static <T> RestResponse<T> fromResponse(RestResponse<T> response) {
        @SuppressWarnings("unchecked")
        ResponseBuilder<T> b = (ResponseBuilder<T>) ResponseBuilder.create(response.getStatus());
        if (response.hasEntity()) {
            b.entity(response.getEntity());
        }
        for (String headerName : response.getHeaders().keySet()) {
            List<Object> headerValues = response.getHeaders().get(headerName);
            for (Object headerValue : headerValues) {
                b.header(headerName, headerValue);
            }
        }
        return b.build();
    }

    /**
     * Create a new RestResponse with the supplied status.
     *
     * @param status the response status.
     * @return a new response.
     * @throws IllegalArgumentException if status is {@code null}.
     */
    public static RestResponse<Void> status(StatusType status) {
        return ResponseBuilder.create(status).build();
    }

    /**
     * Create a new RestResponse with the supplied status.
     *
     * @param status the response status.
     * @return a new response.
     * @throws IllegalArgumentException if status is {@code null}.
     */
    public static <T> RestResponse<T> status(StatusType status, T entity) {
        return ResponseBuilder.create(status, entity).build();
    }

    /**
     * Create a new RestResponse with the supplied status.
     *
     * @param status the response status.
     * @return a new response.
     * @throws IllegalArgumentException if status is {@code null}.
     */
    public static RestResponse<Void> status(Status status) {
        return ResponseBuilder.create(status).build();
    }

    /**
     * Create a new RestResponse with the supplied status.
     *
     * @param status the response status.
     * @return a new response.
     * @throws IllegalArgumentException if status is {@code null}.
     */
    public static <T> RestResponse<T> status(Status status, T entity) {
        return ResponseBuilder.create(status, entity).build();
    }

    /**
     * Create a new RestResponse with the supplied status.
     *
     * @param status the response status.
     * @return a new response.
     * @throws IllegalArgumentException if status is less than {@code 100} or greater
     *         than {@code 599}.
     */
    public static RestResponse<Void> status(int status) {
        return ResponseBuilder.create(status).build();
    }

    /**
     * Create a new RestResponse with the supplied status and reason phrase.
     *
     * @param status the response status.
     * @param reasonPhrase the reason phrase.
     * @return a new response.
     * @throws IllegalArgumentException if status is less than {@code 100} or greater
     *         than {@code 599}.
     */
    public static RestResponse<Void> status(int status, String reasonPhrase) {
        return ResponseBuilder.create(status, reasonPhrase).build();
    }

    /**
     * Create a new RestResponse with an OK status.
     *
     * @return a new response.
     */
    public static RestResponse<Void> ok() {
        return ResponseBuilder.ok().build();
    }

    /**
     * Create a new RestResponse that contains a representation. It is the
     * callers responsibility to wrap the actual entity with
     * {@link GenericEntity} if preservation of its generic type is required.
     *
     * @param entity the representation entity data.
     * @return a new response.
     */
    public static <T> RestResponse<T> ok(T entity) {
        return ResponseBuilder.ok(entity).build();
    }

    /**
     * Create a new RestResponse that contains a representation. It is the
     * callers responsibility to wrap the actual entity with
     * {@link GenericEntity} if preservation of its generic type is required.
     *
     * @param entity the representation entity data.
     * @param type the media type of the entity.
     * @return a new response.
     */
    public static <T> RestResponse<T> ok(T entity, MediaType type) {
        return ResponseBuilder.ok(entity, type).build();
    }

    /**
     * Create a new RestResponse that contains a representation. It is the
     * callers responsibility to wrap the actual entity with
     * {@link GenericEntity} if preservation of its generic type is required.
     *
     * @param entity the representation entity data.
     * @param type the media type of the entity.
     * @return a new response.
     */
    public static <T> RestResponse<T> ok(T entity, String type) {
        return ResponseBuilder.ok(entity, type).build();
    }

    /**
     * Create a new RestResponse that contains a representation. It is the
     * callers responsibility to wrap the actual entity with
     * {@link GenericEntity} if preservation of its generic type is required.
     *
     * @param entity the representation entity data.
     * @param variant representation metadata.
     * @return a new response.
     */
    public static <T> RestResponse<T> ok(T entity, Variant variant) {
        return ResponseBuilder.ok(entity, variant).build();
    }

    /**
     * Create a new RestResponse with an server error status.
     *
     * @return a new response.
     */
    public static RestResponse<Void> serverError() {
        return ResponseBuilder.serverError().build();
    }

    /**
     * Create a new RestResponse for a created resource, set the location
     * header using the supplied value.
     *
     * @param location the URI of the new resource. If a relative URI is
     *        supplied it will be converted into an absolute URI by resolving it
     *        relative to the request URI (see {@link UriInfo#getRequestUri}).
     * @return a new response.
     * @throws java.lang.IllegalArgumentException
     *         if location is {@code null}.
     */
    public static RestResponse<Void> created(URI location) {
        return ResponseBuilder.created(location).build();
    }

    /**
     * Create a new RestResponse with an ACCEPTED status.
     *
     * @return a new response.
     */
    public static RestResponse<Void> accepted() {
        return ResponseBuilder.accepted().build();
    }

    /**
     * Create a new RestResponse with an ACCEPTED status that contains
     * a representation. It is the callers responsibility to wrap the actual entity with
     * {@link GenericEntity} if preservation of its generic type is required.
     *
     * @param entity the representation entity data.
     * @return a new response.
     */
    public static <T> RestResponse<T> accepted(T entity) {
        return ResponseBuilder.accepted(entity).build();
    }

    /**
     * Create a new RestResponse for an empty response.
     *
     * @return a new response.
     */
    public static RestResponse<Void> noContent() {
        return ResponseBuilder.noContent().build();
    }

    /**
     * Create a new RestResponse with a not-modified status.
     *
     * @return a new response.
     */
    public static RestResponse<Void> notModified() {
        return ResponseBuilder.notModified().build();
    }

    /**
     * Create a new RestResponse with a not-modified status.
     *
     * @param tag a tag for the unmodified entity.
     * @return a new response.
     * @throws java.lang.IllegalArgumentException
     *         if tag is {@code null}.
     */
    public static RestResponse<Void> notModified(EntityTag tag) {
        return ResponseBuilder.notModified(tag).build();
    }

    /**
     * Create a new RestResponse with a not-modified status
     * and a strong entity tag. This is a shortcut
     * for <code>notModified(new EntityTag(<i>value</i>))</code>.
     *
     * @param tag the string content of a strong entity tag. The
     *        runtime will quote the supplied value when creating the
     *        header.
     * @return a new response.
     * @throws IllegalArgumentException if tag is {@code null}.
     */
    public static RestResponse<Void> notModified(String tag) {
        return ResponseBuilder.notModified(tag).build();
    }

    /**
     * Create a new RestResponse for a redirection. Used in the
     * redirect-after-POST (aka POST/redirect/GET) pattern.
     *
     * @param location the redirection URI. If a relative URI is
     *        supplied it will be converted into an absolute URI by resolving it
     *        relative to the base URI of the application (see
     *        {@link UriInfo#getBaseUri}).
     * @return a new response.
     * @throws java.lang.IllegalArgumentException
     *         if location is {@code null}.
     */
    public static RestResponse<Void> seeOther(URI location) {
        return ResponseBuilder.seeOther(location).build();
    }

    /**
     * Create a new RestResponse for a temporary redirection.
     *
     * @param location the redirection URI. If a relative URI is
     *        supplied it will be converted into an absolute URI by resolving it
     *        relative to the base URI of the application (see
     *        {@link UriInfo#getBaseUri}).
     * @return a new response.
     * @throws java.lang.IllegalArgumentException
     *         if location is {@code null}.
     */
    public static RestResponse<Void> temporaryRedirect(URI location) {
        return ResponseBuilder.temporaryRedirect(location).build();
    }

    /**
     * Create a new RestResponse for a not acceptable response.
     *
     * @param variants list of variants that were available, a null value is
     *        equivalent to an empty list.
     * @return a new response.
     */
    public static RestResponse<Void> notAcceptable(List<Variant> variants) {
        return ResponseBuilder.notAcceptable(variants).build();
    }

    /**
     * Create a new RestResponse for a not found response.
     *
     * @return a new response.
     */
    public static RestResponse<Void> notFound() {
        return ResponseBuilder.notFound().build();
    }

    /**
     * A class used to build RestResponse instances that contain metadata instead
     * of or in addition to an entity. An initial instance may be obtained via
     * static methods of the RestResponse.ResponseBuilder class, instance methods provide the
     * ability to set metadata. E.g. to create a response that indicates the
     * creation of a new resource:
     * 
     * <pre>
     * &#64;POST
     * RestResponse&lt;Void&gt; addWidget(...) {
     *   Widget w = ...
     *   URI widgetId = UriBuilder.fromResource(Widget.class)...
     *   return RestResponse.ResponseBuilder.created(widgetId).build();
     * }
     * </pre>
     *
     * <p>
     * Several methods have parameters of type URI, {@link UriBuilder} provides
     * convenient methods to create such values as does {@code URI.create()}.
     * </p>
     *
     * <p>
     * Where multiple variants of the same method are provided, the type of
     * the supplied parameter is retained in the metadata of the built
     * {@code RestResponse}.
     * </p>
     */
    public static abstract class ResponseBuilder<T> {

        /**
         * Protected constructor, use one of the static methods of
         * {@code RestResponse} to obtain an instance.
         */
        protected ResponseBuilder() {
        }

        /**
         * Create a new builder instance.
         *
         * @return a new response builder.
         */
        protected static <T> ResponseBuilder<T> newInstance() {
            return ((org.jboss.resteasy.reactive.common.jaxrs.RuntimeDelegateImpl) RuntimeDelegate.getInstance())
                    .createRestResponseBuilder();
        }

        /**
         * Create a RestResponse instance from the current ResponseBuilder. The builder
         * is reset to a blank state equivalent to calling the ok method.
         *
         * @return a RestResponse instance.
         */
        public abstract RestResponse<T> build();

        /**
         * {@inheritDoc}
         * <p>
         * Create a copy of the ResponseBuilder preserving its state.
         * </p>
         *
         * @return a copy of the ResponseBuilder.
         */
        @Override
        public abstract ResponseBuilder<T> clone();

        /**
         * Set the status on the ResponseBuilder.
         *
         * @param status the response status.
         * @return the updated response builder.
         * @throws IllegalArgumentException if status is less than {@code 100} or greater
         *         than {@code 599}.
         */
        public abstract <Ret extends T> ResponseBuilder<Ret> status(int status);

        /**
         * Set the status on the ResponseBuilder.
         *
         * @param status the response status.
         * @param reasonPhrase the reason phrase.
         * @return the updated response builder.
         * @throws IllegalArgumentException if status is less than {@code 100} or greater
         *         than {@code 599}.
         */
        public abstract <Ret extends T> ResponseBuilder<Ret> status(int status, String reasonPhrase);

        /**
         * Set the status on the ResponseBuilder.
         *
         * @param status the response status.
         * @return the updated response builder.
         * @throws IllegalArgumentException if status is {@code null}.
         */
        public <Ret extends T> ResponseBuilder<Ret> status(StatusType status) {
            if (status == null) {
                throw new IllegalArgumentException();
            }
            return status(status.getStatusCode(), status.getReasonPhrase());
        }

        /**
         * Set the status on the ResponseBuilder.
         *
         * @param status the response status.
         * @return the updated response builder.
         * @throws IllegalArgumentException if status is {@code null}.
         */
        public ResponseBuilder<T> status(Status status) {
            return status((StatusType) status);
        }

        /**
         * Set the response entity in the builder.
         * <p />
         * Any Java type instance for a response entity, that is supported by the
         * runtime can be passed. It is the callers responsibility to wrap the
         * actual entity with {@link GenericEntity} if preservation of its generic
         * type is required. Note that the entity can be also set as an
         * {@link java.io.InputStream input stream}.
         * <p />
         * A specific entity media type can be set using one of the {@code type(...)}
         * methods.
         *
         * @param entity the request entity.
         * @return updated response builder instance.
         * @see #entity(java.lang.Object, java.lang.annotation.Annotation[])
         * @see #type(javax.ws.rs.core.MediaType)
         * @see #type(java.lang.String)
         */
        public abstract ResponseBuilder<T> entity(T entity);

        /**
         * Set the response entity in the builder.
         * <p />
         * Any Java type instance for a response entity, that is supported by the
         * runtime can be passed. It is the callers responsibility to wrap the
         * actual entity with {@link GenericEntity} if preservation of its generic
         * type is required. Note that the entity can be also set as an
         * {@link java.io.InputStream input stream}.
         * <p />
         * A specific entity media type can be set using one of the {@code type(...)}
         * methods.
         *
         * @param entity the request entity.
         * @param annotations annotations that will be passed to the {@link MessageBodyWriter},
         *        (in addition to any annotations declared directly on a resource
         *        method that returns the built response).
         * @return updated response builder instance.
         * @see #entity(java.lang.Object)
         * @see #type(javax.ws.rs.core.MediaType)
         * @see #type(java.lang.String)
         */
        public abstract ResponseBuilder<T> entity(T entity, Annotation[] annotations);

        /**
         * Set the list of allowed methods for the resource. Any duplicate method
         * names will be truncated to a single entry.
         *
         * @param methods the methods to be listed as allowed for the resource,
         *        if {@code null} any existing allowed method list will be removed.
         * @return the updated response builder.
         */
        public abstract ResponseBuilder<T> allow(String... methods);

        /**
         * Set the list of allowed methods for the resource.
         *
         * @param methods the methods to be listed as allowed for the resource,
         *        if {@code null} any existing allowed method list will be removed.
         * @return the updated response builder.
         */
        public abstract ResponseBuilder<T> allow(Set<String> methods);

        /**
         * Set the cache control data of the message.
         *
         * @param cacheControl the cache control directives, if {@code null}
         *        any existing cache control directives will be removed.
         * @return the updated response builder.
         */
        public abstract ResponseBuilder<T> cacheControl(CacheControl cacheControl);

        /**
         * Set the message entity content encoding.
         *
         * @param encoding the content encoding of the message entity,
         *        if {@code null} any existing value for content encoding will be
         *        removed.
         * @return the updated response builder.
         */
        public abstract ResponseBuilder<T> encoding(String encoding);

        /**
         * Add an arbitrary header.
         *
         * @param name the name of the header
         * @param value the value of the header, the header will be serialized
         *        using a {@link javax.ws.rs.ext.RuntimeDelegate.HeaderDelegate} if
         *        one is available via {@link javax.ws.rs.ext.RuntimeDelegate#createHeaderDelegate(java.lang.Class)}
         *        for the class of {@code value} or using its {@code toString} method
         *        if a header delegate is not available. If {@code value} is {@code null}
         *        then all current headers of the same name will be removed.
         * @return the updated response builder.
         */
        public abstract ResponseBuilder<T> header(String name, Object value);

        /**
         * Replaces all existing headers with the newly supplied headers.
         *
         * @param headers new headers to be set, if {@code null} all existing
         *        headers will be removed.
         * @return the updated response builder.
         */
        public abstract ResponseBuilder<T> replaceAll(MultivaluedMap<String, Object> headers);

        /**
         * Set the message entity language.
         *
         * @param language the language of the message entity, if {@code null} any
         *        existing value for language will be removed.
         * @return the updated response builder.
         */
        public abstract ResponseBuilder<T> language(String language);

        /**
         * Set the message entity language.
         *
         * @param language the language of the message entity, if {@code null} any
         *        existing value for type will be removed.
         * @return the updated response builder.
         */
        public abstract ResponseBuilder<T> language(Locale language);

        /**
         * Set the message entity media type.
         *
         * @param type the media type of the message entity. If {@code null}, any
         *        existing value for type will be removed.
         * @return the updated response builder.
         */
        public abstract ResponseBuilder<T> type(MediaType type);

        /**
         * Set the message entity media type.
         *
         * @param type the media type of the message entity. If {@code null}, any
         *        existing value for type will be removed.
         * @return the updated response builder.
         */
        public abstract ResponseBuilder<T> type(String type);

        /**
         * Set message entity representation metadata.
         * <p/>
         * Equivalent to setting the values of content type, content language,
         * and content encoding separately using the values of the variant properties.
         *
         * @param variant metadata of the message entity, a {@code null} value is
         *        equivalent to a variant with all {@code null} properties.
         * @return the updated response builder.
         * @see #encoding(java.lang.String)
         * @see #language(java.util.Locale)
         * @see #type(javax.ws.rs.core.MediaType)
         */
        public abstract ResponseBuilder<T> variant(Variant variant);

        /**
         * Set the content location.
         *
         * @param location the content location. Relative or absolute URIs
         *        may be used for the value of content location. If {@code null} any
         *        existing value for content location will be removed.
         * @return the updated response builder.
         */
        public abstract ResponseBuilder<T> contentLocation(URI location);

        /**
         * Add cookies to the response message.
         *
         * @param cookies new cookies that will accompany the response. A {@code null}
         *        value will remove all cookies, including those added via the
         *        {@link #header(java.lang.String, java.lang.Object)} method.
         * @return the updated response builder.
         */
        public abstract ResponseBuilder<T> cookie(NewCookie... cookies);

        /**
         * Set the response expiration date.
         *
         * @param expires the expiration date, if {@code null} removes any existing
         *        expires value.
         * @return the updated response builder.
         */
        public abstract ResponseBuilder<T> expires(Date expires);

        /**
         * Set the response entity last modification date.
         *
         * @param lastModified the last modified date, if {@code null} any existing
         *        last modified value will be removed.
         * @return the updated response builder.
         */
        public abstract ResponseBuilder<T> lastModified(Date lastModified);

        /**
         * Set the location.
         *
         * @param location the location. If a relative URI is supplied it will be
         *        converted into an absolute URI by resolving it relative to the
         *        base URI of the application (see {@link UriInfo#getBaseUri}).
         *        If {@code null} any existing value for location will be removed.
         * @return the updated response builder.
         */
        public abstract ResponseBuilder<T> location(URI location);

        /**
         * Set a response entity tag.
         *
         * @param tag the entity tag, if {@code null} any existing entity tag
         *        value will be removed.
         * @return the updated response builder.
         */
        public abstract ResponseBuilder<T> tag(EntityTag tag);

        /**
         * Set a strong response entity tag.
         * <p/>
         * This is a shortcut for <code>tag(new EntityTag(<i>value</i>))</code>.
         *
         * @param tag the string content of a strong entity tag. The
         *        runtime will quote the supplied value when creating the header.
         *        If {@code null} any existing entity tag value will be removed.
         * @return the updated response builder.
         */
        public abstract ResponseBuilder<T> tag(String tag);

        /**
         * Add a Vary header that lists the available variants.
         *
         * @param variants a list of available representation variants, a {@code null}
         *        value will remove an existing value for Vary header.
         * @return the updated response builder.
         */
        public abstract ResponseBuilder<T> variants(Variant... variants);

        /**
         * Add a Vary header that lists the available variants.
         *
         * @param variants a list of available representation variants, a {@code null}
         *        value will remove an existing value for Vary header.
         * @return the updated response builder.
         */
        public abstract ResponseBuilder<T> variants(List<Variant> variants);

        /**
         * Add one or more link headers.
         *
         * @param links links to be added to the message as headers, a {@code null}
         *        value will remove any existing Link headers.
         * @return the updated response builder.
         */
        public abstract ResponseBuilder<T> links(Link... links);

        /**
         * Add a link header.
         *
         * @param uri underlying URI for link header.
         * @param rel value of "rel" parameter.
         * @return the updated response builder.
         */
        public abstract ResponseBuilder<T> link(URI uri, String rel);

        /**
         * Add a link header.
         *
         * @param uri underlying URI for link header.
         * @param rel value of "rel" parameter.
         * @return the updated response builder.
         */
        public abstract ResponseBuilder<T> link(String uri, String rel);

        /**
         * Create a new ResponseBuilder by performing a shallow copy of an
         * existing Response.
         * <p>
         * The returned builder has its own {@link #getHeaders() response headers}
         * but the header values are shared with the original {@code RestResponse} instance.
         * The original response entity instance reference is set in the new response
         * builder.
         * </p>
         * <p>
         * Note that if the entity is backed by an un-consumed input stream, the
         * reference to the stream is copied. In such case make sure to
         * {@link #bufferEntity() buffer} the entity stream of the original response
         * instance before passing it to this method.
         * </p>
         *
         * @param response a Response from which the status code, entity and
         *        {@link #getHeaders() response headers} will be copied.
         * @return a new response builder.
         */
        public static <T> ResponseBuilder<T> fromResponse(RestResponse<T> response) {
            @SuppressWarnings("unchecked")
            ResponseBuilder<T> b = (ResponseBuilder<T>) create(response.getStatus());
            if (response.hasEntity()) {
                b.entity(response.getEntity());
            }
            for (String headerName : response.getHeaders().keySet()) {
                List<Object> headerValues = response.getHeaders().get(headerName);
                for (Object headerValue : headerValues) {
                    b.header(headerName, headerValue);
                }
            }
            return b;
        }

        /**
         * Create a new ResponseBuilder with the supplied status.
         *
         * @param status the response status.
         * @return a new response builder.
         * @throws IllegalArgumentException if status is {@code null}.
         */
        public static ResponseBuilder<Void> create(StatusType status) {
            return ResponseBuilder.newInstance().status(status);
        }

        /**
         * Create a new ResponseBuilder with the supplied status.
         *
         * @param status the response status.
         * @return a new response builder.
         * @throws IllegalArgumentException if status is {@code null}.
         */
        public static <T> ResponseBuilder<T> create(StatusType status, T entity) {
            return ResponseBuilder.<T> newInstance().status(status).entity(entity);
        }

        /**
         * Create a new ResponseBuilder with the supplied status.
         *
         * @param status the response status.
         * @return a new response builder.
         * @throws IllegalArgumentException if status is {@code null}.
         */
        public static ResponseBuilder<Void> create(Status status) {
            return create((StatusType) status);
        }

        /**
         * Create a new ResponseBuilder with the supplied status.
         *
         * @param status the response status.
         * @return a new response builder.
         * @throws IllegalArgumentException if status is {@code null}.
         */
        public static <T> ResponseBuilder<T> create(Status status, T entity) {
            return create((StatusType) status, entity);
        }

        /**
         * Create a new ResponseBuilder with the supplied status.
         *
         * @param status the response status.
         * @return a new response builder.
         * @throws IllegalArgumentException if status is less than {@code 100} or greater
         *         than {@code 599}.
         */
        public static ResponseBuilder<Void> create(int status) {
            return ResponseBuilder.<Void> newInstance().status(status);
        }

        /**
         * Create a new ResponseBuilder with the supplied status and reason phrase.
         *
         * @param status the response status.
         * @param reasonPhrase the reason phrase.
         * @return the updated response builder.
         * @throws IllegalArgumentException if status is less than {@code 100} or greater
         *         than {@code 599}.
         */
        public static ResponseBuilder<Void> create(int status, String reasonPhrase) {
            return ResponseBuilder.newInstance().status(status, reasonPhrase);
        }

        /**
         * Create a new ResponseBuilder with an OK status.
         *
         * @return a new response builder.
         */
        public static ResponseBuilder<Void> ok() {
            return create(Status.OK);
        }

        /**
         * Create a new ResponseBuilder that contains a representation. It is the
         * callers responsibility to wrap the actual entity with
         * {@link GenericEntity} if preservation of its generic type is required.
         *
         * @param entity the representation entity data.
         * @return a new response builder.
         */
        public static <T> ResponseBuilder<T> ok(T entity) {
            return create(Status.OK, entity);
        }

        /**
         * Create a new ResponseBuilder that contains a representation. It is the
         * callers responsibility to wrap the actual entity with
         * {@link GenericEntity} if preservation of its generic type is required.
         *
         * @param entity the representation entity data.
         * @param type the media type of the entity.
         * @return a new response builder.
         */
        public static <T> ResponseBuilder<T> ok(T entity, MediaType type) {
            return ok(entity).type(type);
        }

        /**
         * Create a new ResponseBuilder that contains a representation. It is the
         * callers responsibility to wrap the actual entity with
         * {@link GenericEntity} if preservation of its generic type is required.
         *
         * @param entity the representation entity data.
         * @param type the media type of the entity.
         * @return a new response builder.
         */
        public static <T> ResponseBuilder<T> ok(T entity, String type) {
            return ok(entity).type(type);
        }

        /**
         * Create a new ResponseBuilder that contains a representation. It is the
         * callers responsibility to wrap the actual entity with
         * {@link GenericEntity} if preservation of its generic type is required.
         *
         * @param entity the representation entity data.
         * @param variant representation metadata.
         * @return a new response builder.
         */
        public static <T> ResponseBuilder<T> ok(T entity, Variant variant) {
            return ok(entity).variant(variant);
        }

        /**
         * Create a new ResponseBuilder with an server error status.
         *
         * @return a new response builder.
         */
        public static ResponseBuilder<Void> serverError() {
            return create(Status.INTERNAL_SERVER_ERROR);
        }

        /**
         * Create a new ResponseBuilder for a created resource, set the location
         * header using the supplied value.
         *
         * @param location the URI of the new resource. If a relative URI is
         *        supplied it will be converted into an absolute URI by resolving it
         *        relative to the request URI (see {@link UriInfo#getRequestUri}).
         * @return a new response builder.
         * @throws java.lang.IllegalArgumentException
         *         if location is {@code null}.
         */
        public static ResponseBuilder<Void> created(URI location) {
            return create(Status.CREATED).location(location);
        }

        /**
         * Create a new ResponseBuilder with an ACCEPTED status.
         *
         * @return a new response builder.
         */
        public static ResponseBuilder<Void> accepted() {
            return create(Status.ACCEPTED);
        }

        /**
         * Create a new ResponseBuilder with an ACCEPTED status that contains
         * a representation. It is the callers responsibility to wrap the actual entity with
         * {@link GenericEntity} if preservation of its generic type is required.
         *
         * @param entity the representation entity data.
         * @return a new response builder.
         */
        public static <T> ResponseBuilder<T> accepted(T entity) {
            return create(Status.ACCEPTED, entity);
        }

        /**
         * Create a new ResponseBuilder for an empty response.
         *
         * @return a new response builder.
         */
        public static ResponseBuilder<Void> noContent() {
            return create(Status.NO_CONTENT);
        }

        /**
         * Create a new ResponseBuilder with a not-modified status.
         *
         * @return a new response builder.
         */
        public static ResponseBuilder<Void> notModified() {
            return create(Status.NOT_MODIFIED);
        }

        /**
         * Create a new ResponseBuilder with a not-modified status.
         *
         * @param tag a tag for the unmodified entity.
         * @return a new response builder.
         * @throws java.lang.IllegalArgumentException
         *         if tag is {@code null}.
         */
        public static ResponseBuilder<Void> notModified(EntityTag tag) {
            return notModified().tag(tag);
        }

        /**
         * Create a new ResponseBuilder with a not-modified status
         * and a strong entity tag. This is a shortcut
         * for <code>notModified(new EntityTag(<i>value</i>))</code>.
         *
         * @param tag the string content of a strong entity tag. The
         *        runtime will quote the supplied value when creating the
         *        header.
         * @return a new response builder.
         * @throws IllegalArgumentException if tag is {@code null}.
         */
        public static ResponseBuilder<Void> notModified(String tag) {
            return notModified().tag(tag);
        }

        /**
         * Create a new ResponseBuilder for a redirection. Used in the
         * redirect-after-POST (aka POST/redirect/GET) pattern.
         *
         * @param location the redirection URI. If a relative URI is
         *        supplied it will be converted into an absolute URI by resolving it
         *        relative to the base URI of the application (see
         *        {@link UriInfo#getBaseUri}).
         * @return a new response builder.
         * @throws java.lang.IllegalArgumentException
         *         if location is {@code null}.
         */
        public static ResponseBuilder<Void> seeOther(URI location) {
            return create(Status.SEE_OTHER).location(location);
        }

        /**
         * Create a new ResponseBuilder for a temporary redirection.
         *
         * @param location the redirection URI. If a relative URI is
         *        supplied it will be converted into an absolute URI by resolving it
         *        relative to the base URI of the application (see
         *        {@link UriInfo#getBaseUri}).
         * @return a new response builder.
         * @throws java.lang.IllegalArgumentException
         *         if location is {@code null}.
         */
        public static ResponseBuilder<Void> temporaryRedirect(URI location) {
            return create(Status.TEMPORARY_REDIRECT).location(location);
        }

        /**
         * Create a new ResponseBuilder for a not acceptable response.
         *
         * @param variants list of variants that were available, a null value is
         *        equivalent to an empty list.
         * @return a new response builder.
         */
        public static ResponseBuilder<Void> notAcceptable(List<Variant> variants) {
            return create(Status.NOT_ACCEPTABLE).variants(variants);
        }

        /**
         * Create a new ResponseBuilder for a not found response.
         *
         * @return a new response builder.
         */
        public static ResponseBuilder<Void> notFound() {
            return create(Status.NOT_FOUND);
        }
    }

    /**
     * Commonly used status codes defined by HTTP, see
     * {@link <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10">HTTP/1.1 documentation</a>}
     * for the complete list. Additional status codes can be added by applications
     * by creating an implementation of {@link StatusType}.
     */
    public enum Status implements StatusType {

        /**
         * 100 Continue, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.2.1">HTTP/1.1 documentation</a>}.
         */
        CONTINUE(StatusCode.CONTINUE, "Continue"),

        /**
         * 101 Switching Protocols, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.2.2">HTTP/1.1
         * documentation</a>}.
         */
        SWITCHING_PROTOCOLS(StatusCode.SWITCHING_PROTOCOLS, "Switching Protocols"),

        /**
         * 200 OK, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.3.1">HTTP/1.1 documentation</a>}.
         */
        OK(StatusCode.OK, "OK"),
        /**
         * 201 Created, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.3.2">HTTP/1.1 documentation</a>}.
         */
        CREATED(StatusCode.CREATED, "Created"),
        /**
         * 202 Accepted, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.3.3">HTTP/1.1 documentation</a>}.
         */
        ACCEPTED(StatusCode.ACCEPTED, "Accepted"),
        /**
         * 203 Non-Authoritative Information, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.3.4">HTTP/1.1
         * documentation</a>}.
         */
        NON_AUTHORITATIVE_INFORMATION(StatusCode.NON_AUTHORITATIVE_INFORMATION, "Non-Authoritative Information"),
        /**
         * 204 No Content, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.3.5">HTTP/1.1 documentation</a>}.
         */
        NO_CONTENT(StatusCode.NO_CONTENT, "No Content"),
        /**
         * 205 Reset Content, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.3.5">HTTP/1.1
         * documentation</a>}.
         */
        RESET_CONTENT(StatusCode.RESET_CONTENT, "Reset Content"),
        /**
         * 206 Reset Content, see {@link <a href="https://tools.ietf.org/html/rfc7233#section-4.1">HTTP/1.1 documentation</a>}.
         */
        PARTIAL_CONTENT(StatusCode.PARTIAL_CONTENT, "Partial Content"),
        /**
         * 300 Multiple Choices, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.4.1">HTTP/1.1
         * documentation</a>}.
         */
        MULTIPLE_CHOICES(StatusCode.MULTIPLE_CHOICES, "Multiple Choices"),
        /**
         * 301 Moved Permanently, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.4.2">HTTP/1.1
         * documentation</a>}.
         */
        MOVED_PERMANENTLY(StatusCode.MOVED_PERMANENTLY, "Moved Permanently"),
        /**
         * 302 Found, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.4.3">HTTP/1.1 documentation</a>}.
         */
        FOUND(StatusCode.FOUND, "Found"),
        /**
         * 303 See Other, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.4.4">HTTP/1.1 documentation</a>}.
         */
        SEE_OTHER(StatusCode.SEE_OTHER, "See Other"),
        /**
         * 304 Not Modified, see {@link <a href="https://tools.ietf.org/html/rfc7232#section-4.1">HTTP/1.1 documentation</a>}.
         */
        NOT_MODIFIED(StatusCode.NOT_MODIFIED, "Not Modified"),
        /**
         * 305 Use Proxy, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.4.5">HTTP/1.1 documentation</a>}.
         */
        USE_PROXY(StatusCode.USE_PROXY, "Use Proxy"),
        /**
         * 307 Temporary Redirect, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.4.7">HTTP/1.1
         * documentation</a>}.
         */
        TEMPORARY_REDIRECT(StatusCode.TEMPORARY_REDIRECT, "Temporary Redirect"),
        /**
         * 308 Permanent Redirect, see {@link <a href="https://tools.ietf.org/html/rfc7238#section-3">HTTP/1.1
         * documentation</a>}.
         */
        PERMANENT_REDIRECT(StatusCode.PERMANENT_REDIRECT, "Permanent Redirect"),
        /**
         * 400 Bad Request, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.5.1">HTTP/1.1 documentation</a>}.
         */
        BAD_REQUEST(StatusCode.BAD_REQUEST, "Bad Request"),
        /**
         * 401 Unauthorized, see {@link <a href="https://tools.ietf.org/html/rfc7235#section-3.1">HTTP/1.1 documentation</a>}.
         */
        UNAUTHORIZED(StatusCode.UNAUTHORIZED, "Unauthorized"),
        /**
         * 402 Payment Required, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.5.2">HTTP/1.1
         * documentation</a>}.
         */
        PAYMENT_REQUIRED(StatusCode.PAYMENT_REQUIRED, "Payment Required"),
        /**
         * 403 Forbidden, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.5.3">HTTP/1.1 documentation</a>}.
         */
        FORBIDDEN(StatusCode.FORBIDDEN, "Forbidden"),
        /**
         * 404 Not Found, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.5.4">HTTP/1.1 documentation</a>}.
         */
        NOT_FOUND(StatusCode.NOT_FOUND, "Not Found"),
        /**
         * 405 Method Not Allowed, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.5.5">HTTP/1.1
         * documentation</a>}.
         */
        METHOD_NOT_ALLOWED(StatusCode.METHOD_NOT_ALLOWED, "Method Not Allowed"),
        /**
         * 406 Not Acceptable, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.5.6">HTTP/1.1
         * documentation</a>}.
         */
        NOT_ACCEPTABLE(StatusCode.NOT_ACCEPTABLE, "Not Acceptable"),
        /**
         * 407 Proxy Authentication Required, see {@link <a href="https://tools.ietf.org/html/rfc7235#section-3.2">HTTP/1.1
         * documentation</a>}.
         */
        PROXY_AUTHENTICATION_REQUIRED(StatusCode.PROXY_AUTHENTICATION_REQUIRED, "Proxy Authentication Required"),
        /**
         * 408 Request Timeout, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.5.7">HTTP/1.1
         * documentation</a>}.
         */
        REQUEST_TIMEOUT(StatusCode.REQUEST_TIMEOUT, "Request Timeout"),
        /**
         * 409 Conflict, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.5.8">HTTP/1.1 documentation</a>}.
         */
        CONFLICT(StatusCode.CONFLICT, "Conflict"),
        /**
         * 410 Gone, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.5.9">HTTP/1.1 documentation</a>}.
         */
        GONE(StatusCode.GONE, "Gone"),
        /**
         * 411 Length Required, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.5.10">HTTP/1.1
         * documentation</a>}.
         */
        LENGTH_REQUIRED(StatusCode.LENGTH_REQUIRED, "Length Required"),
        /**
         * 412 Precondition Failed, see {@link <a href="https://tools.ietf.org/html/rfc7232#section-4.2">HTTP/1.1
         * documentation</a>}.
         */
        PRECONDITION_FAILED(StatusCode.PRECONDITION_FAILED, "Precondition Failed"),
        /**
         * 413 Payload Too Large, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.5.11">HTTP/1.1
         * documentation</a>}.
         */
        PAYLOAD_TOO_LARGE(StatusCode.PAYLOAD_TOO_LARGE, "Payload Too Large"),
        /**
         * 414 URI Too Long, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.5.12">HTTP/1.1
         * documentation</a>}.
         */
        URI_TOO_LONG(StatusCode.URI_TOO_LONG, "URI Too Long"),
        /**
         * 415 Unsupported Media Type, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.5.13">HTTP/1.1
         * documentation</a>}.
         */
        UNSUPPORTED_MEDIA_TYPE(StatusCode.UNSUPPORTED_MEDIA_TYPE, "Unsupported Media Type"),
        /**
         * 416 Requested Range Not Satisfiable, see {@link <a href="https://tools.ietf.org/html/rfc7233#section-4.4">HTTP/1.1
         * documentation</a>}.
         */
        REQUESTED_RANGE_NOT_SATISFIABLE(StatusCode.REQUESTED_RANGE_NOT_SATISFIABLE, "Requested Range Not Satisfiable"),
        /**
         * 417 Expectation Failed, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.5.14">HTTP/1.1
         * documentation</a>}.
         */
        EXPECTATION_FAILED(StatusCode.EXPECTATION_FAILED, "Expectation Failed"),
        /**
         * 426 Upgrade Required, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.5.15">HTTP/1.1
         * documentation</a>}.
         */
        UPGRADE_REQUIRED(StatusCode.UPGRADE_REQUIRED, "Upgrade Required"),
        /**
         * 428 Precondition required, see {@link <a href="https://tools.ietf.org/html/rfc6585#section-3">RFC 6585: Additional
         * HTTP Status Codes</a>}.
         */
        PRECONDITION_REQUIRED(StatusCode.PRECONDITION_REQUIRED, "Precondition Required"),
        /**
         * 429 Too Many Requests, see {@link <a href="https://tools.ietf.org/html/rfc6585#section-4">RFC 6585: Additional HTTP
         * Status Codes</a>}.
         */
        TOO_MANY_REQUESTS(StatusCode.TOO_MANY_REQUESTS, "Too Many Requests"),
        /**
         * 431 Request Header Fields Too Large, see {@link <a href="https://tools.ietf.org/html/rfc6585#section-5">RFC 6585:
         * Additional HTTP Status Codes</a>}.
         */
        REQUEST_HEADER_FIELDS_TOO_LARGE(StatusCode.REQUEST_HEADER_FIELDS_TOO_LARGE, "Request Header Fields Too Large"),
        /**
         * 500 Internal Server Error, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.6.1">HTTP/1.1
         * documentation</a>}.
         */
        INTERNAL_SERVER_ERROR(StatusCode.INTERNAL_SERVER_ERROR, "Internal Server Error"),
        /**
         * 501 Not Implemented, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.6.2">HTTP/1.1
         * documentation</a>}.
         */
        NOT_IMPLEMENTED(StatusCode.NOT_IMPLEMENTED, "Not Implemented"),
        /**
         * 502 Bad Gateway, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.6.3">HTTP/1.1 documentation</a>}.
         */
        BAD_GATEWAY(StatusCode.BAD_GATEWAY, "Bad Gateway"),
        /**
         * 503 Service Unavailable, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.6.4">HTTP/1.1
         * documentation</a>}.
         */
        SERVICE_UNAVAILABLE(StatusCode.SERVICE_UNAVAILABLE, "Service Unavailable"),
        /**
         * 504 Gateway Timeout, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.6.5">HTTP/1.1
         * documentation</a>}.
         */
        GATEWAY_TIMEOUT(StatusCode.GATEWAY_TIMEOUT, "Gateway Timeout"),
        /**
         * 505 HTTP Version Not Supported, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.6.6">HTTP/1.1
         * documentation</a>}.
         */
        HTTP_VERSION_NOT_SUPPORTED(StatusCode.HTTP_VERSION_NOT_SUPPORTED, "Http Version Not Supported"),
        /**
         * 511 Network Authentication Required, see {@link <a href="https://tools.ietf.org/html/rfc6585#section-6">RFC 6585:
         * Additional HTTP Status Codes</a>}.
         */
        NETWORK_AUTHENTICATION_REQUIRED(StatusCode.NETWORK_AUTHENTICATION_REQUIRED, "Network Authentication Required"),
        ;

        private final int code;
        private final String reason;
        private final Family family;

        Status(final int statusCode, final String reasonPhrase) {
            this.code = statusCode;
            this.reason = reasonPhrase;
            this.family = Family.familyOf(statusCode);
        }

        /**
         * Get the class of status code.
         *
         * @return the class of status code.
         */
        @Override
        public Family getFamily() {
            return family;
        }

        /**
         * Get the associated status code.
         *
         * @return the status code.
         */
        @Override
        public int getStatusCode() {
            return code;
        }

        /**
         * Get the reason phrase.
         *
         * @return the reason phrase.
         */
        @Override
        public String getReasonPhrase() {
            return toString();
        }

        /**
         * Get the reason phrase.
         *
         * @return the reason phrase.
         */
        @Override
        public String toString() {
            return reason;
        }

        /**
         * Convert a numerical status code into the corresponding Status.
         *
         * @param statusCode the numerical status code.
         * @return the matching Status or null is no matching Status is defined.
         */
        public static Status fromStatusCode(final int statusCode) {
            for (Status s : Status.values()) {
                if (s.code == statusCode) {
                    return s;
                }
            }
            return null;
        }
    }

    /**
     * Commonly used status codes defined by HTTP, see
     * {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.2">HTTP/1.1 documentation</a>}
     * for the complete list.
     */
    public static class StatusCode {

        /**
         * 100 Continue, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.2.1">HTTP/1.1 documentation</a>}.
         */
        public final static int CONTINUE = 100;

        /**
         * 101 Switching Protocols, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.2.2">HTTP/1.1
         * documentation</a>}.
         */
        public final static int SWITCHING_PROTOCOLS = 101;

        /**
         * 200 OK, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.3.1">HTTP/1.1 documentation</a>}.
         */
        public final static int OK = 200;
        /**
         * 201 Created, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.3.2">HTTP/1.1 documentation</a>}.
         */
        public final static int CREATED = 201;
        /**
         * 202 Accepted, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.3.3">HTTP/1.1 documentation</a>}.
         */
        public final static int ACCEPTED = 202;
        /**
         * 203 Non-Authoritative Information, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.3.4">HTTP/1.1
         * documentation</a>}.
         */
        public final static int NON_AUTHORITATIVE_INFORMATION = 203;
        /**
         * 204 No Content, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.3.5">HTTP/1.1 documentation</a>}.
         */
        public final static int NO_CONTENT = 204;
        /**
         * 205 Reset Content, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.3.5">HTTP/1.1
         * documentation</a>}.
         */
        public final static int RESET_CONTENT = 205;
        /**
         * 206 Reset Content, see {@link <a href="https://tools.ietf.org/html/rfc7233#section-4.1">HTTP/1.1 documentation</a>}.
         */
        public final static int PARTIAL_CONTENT = 206;
        /**
         * 300 Multiple Choices, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.4.1">HTTP/1.1
         * documentation</a>}.
         */
        public final static int MULTIPLE_CHOICES = 300;
        /**
         * 301 Moved Permanently, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.4.2">HTTP/1.1
         * documentation</a>}.
         */
        public final static int MOVED_PERMANENTLY = 301;
        /**
         * 302 Found, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.4.3">HTTP/1.1 documentation</a>}.
         */
        public final static int FOUND = 302;
        /**
         * 303 See Other, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.4.4">HTTP/1.1 documentation</a>}.
         */
        public final static int SEE_OTHER = 303;
        /**
         * 304 Not Modified, see {@link <a href="https://tools.ietf.org/html/rfc7232#section-4.1">HTTP/1.1 documentation</a>}.
         */
        public final static int NOT_MODIFIED = 304;
        /**
         * 305 Use Proxy, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.4.5">HTTP/1.1 documentation</a>}.
         */
        public final static int USE_PROXY = 305;
        /**
         * 307 Temporary Redirect, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.4.7">HTTP/1.1
         * documentation</a>}.
         */
        public final static int TEMPORARY_REDIRECT = 307;
        /**
         * 308 Permanent Redirect, see {@link <a href="https://tools.ietf.org/html/rfc7238#section-3">HTTP/1.1
         * documentation</a>}.
         */
        public final static int PERMANENT_REDIRECT = 308;
        /**
         * 400 Bad Request, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.5.1">HTTP/1.1 documentation</a>}.
         */
        public final static int BAD_REQUEST = 400;
        /**
         * 401 Unauthorized, see {@link <a href="https://tools.ietf.org/html/rfc7235#section-3.1">HTTP/1.1 documentation</a>}.
         */
        public final static int UNAUTHORIZED = 401;
        /**
         * 402 Payment Required, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.5.2">HTTP/1.1
         * documentation</a>}.
         */
        public final static int PAYMENT_REQUIRED = 402;
        /**
         * 403 Forbidden, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.5.3">HTTP/1.1 documentation</a>}.
         */
        public final static int FORBIDDEN = 403;
        /**
         * 404 Not Found, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.5.4">HTTP/1.1 documentation</a>}.
         */
        public final static int NOT_FOUND = 404;
        /**
         * 405 Method Not Allowed, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.5.5">HTTP/1.1
         * documentation</a>}.
         */
        public final static int METHOD_NOT_ALLOWED = 405;
        /**
         * 406 Not Acceptable, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.5.6">HTTP/1.1
         * documentation</a>}.
         */
        public final static int NOT_ACCEPTABLE = 406;
        /**
         * 407 Proxy Authentication Required, see {@link <a href="https://tools.ietf.org/html/rfc7235#section-3.2">HTTP/1.1
         * documentation</a>}.
         */
        public final static int PROXY_AUTHENTICATION_REQUIRED = 407;
        /**
         * 408 Request Timeout, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.5.7">HTTP/1.1
         * documentation</a>}.
         */
        public final static int REQUEST_TIMEOUT = 408;
        /**
         * 409 Conflict, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.5.8">HTTP/1.1 documentation</a>}.
         */
        public final static int CONFLICT = 409;
        /**
         * 410 Gone, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.5.9">HTTP/1.1 documentation</a>}.
         */
        public final static int GONE = 410;
        /**
         * 411 Length Required, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.5.10">HTTP/1.1
         * documentation</a>}.
         */
        public final static int LENGTH_REQUIRED = 411;
        /**
         * 412 Precondition Failed, see {@link <a href="https://tools.ietf.org/html/rfc7232#section-4.2">HTTP/1.1
         * documentation</a>}.
         */
        public final static int PRECONDITION_FAILED = 412;
        /**
         * 413 Payload Too Large, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.5.11">HTTP/1.1
         * documentation</a>}.
         */
        public final static int PAYLOAD_TOO_LARGE = 413;
        /**
         * 414 URI Too Long, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.5.12">HTTP/1.1
         * documentation</a>}.
         */
        public final static int URI_TOO_LONG = 414;
        /**
         * 415 Unsupported Media Type, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.5.13">HTTP/1.1
         * documentation</a>}.
         */
        public final static int UNSUPPORTED_MEDIA_TYPE = 415;
        /**
         * 416 Requested Range Not Satisfiable, see {@link <a href="https://tools.ietf.org/html/rfc7233#section-4.4">HTTP/1.1
         * documentation</a>}.
         */
        public final static int REQUESTED_RANGE_NOT_SATISFIABLE = 416;
        /**
         * 417 Expectation Failed, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.5.14">HTTP/1.1
         * documentation</a>}.
         */
        public final static int EXPECTATION_FAILED = 417;
        /**
         * 426 Upgrade Required, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.5.15">HTTP/1.1
         * documentation</a>}.
         */
        public final static int UPGRADE_REQUIRED = 426;
        /**
         * 428 Precondition required, see {@link <a href="https://tools.ietf.org/html/rfc6585#section-3">RFC 6585: Additional
         * HTTP Status Codes</a>}.
         */
        public final static int PRECONDITION_REQUIRED = 428;
        /**
         * 429 Too Many Requests, see {@link <a href="https://tools.ietf.org/html/rfc6585#section-4">RFC 6585: Additional HTTP
         * Status Codes</a>}.
         */
        public final static int TOO_MANY_REQUESTS = 429;
        /**
         * 431 Request Header Fields Too Large, see {@link <a href="https://tools.ietf.org/html/rfc6585#section-5">RFC 6585:
         * Additional HTTP Status Codes</a>}.
         */
        public final static int REQUEST_HEADER_FIELDS_TOO_LARGE = 431;
        /**
         * 500 Internal Server Error, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.6.1">HTTP/1.1
         * documentation</a>}.
         */
        public final static int INTERNAL_SERVER_ERROR = 500;
        /**
         * 501 Not Implemented, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.6.2">HTTP/1.1
         * documentation</a>}.
         */
        public final static int NOT_IMPLEMENTED = 501;
        /**
         * 502 Bad Gateway, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.6.3">HTTP/1.1 documentation</a>}.
         */
        public final static int BAD_GATEWAY = 502;
        /**
         * 503 Service Unavailable, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.6.4">HTTP/1.1
         * documentation</a>}.
         */
        public final static int SERVICE_UNAVAILABLE = 503;
        /**
         * 504 Gateway Timeout, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.6.5">HTTP/1.1
         * documentation</a>}.
         */
        public final static int GATEWAY_TIMEOUT = 504;
        /**
         * 505 HTTP Version Not Supported, see {@link <a href="https://tools.ietf.org/html/rfc7231#section-6.6.6">HTTP/1.1
         * documentation</a>}.
         */
        public final static int HTTP_VERSION_NOT_SUPPORTED = 505;
        /**
         * 511 Network Authentication Required, see {@link <a href="https://tools.ietf.org/html/rfc6585#section-6">RFC 6585:
         * Additional HTTP Status Codes</a>}.
         */
        public final static int NETWORK_AUTHENTICATION_REQUIRED = 511;
    }
}
