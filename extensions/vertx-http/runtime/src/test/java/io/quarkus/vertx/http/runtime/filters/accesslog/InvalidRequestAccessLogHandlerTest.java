package io.quarkus.vertx.http.runtime.filters.accesslog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.TooLongHttpHeaderException;
import io.netty.handler.codec.http.TooLongHttpLineException;

/**
 * Guards the mapping from Netty decoder failures to HTTP status codes. If Netty renames or removes these
 * typed exceptions, this test fails so we notice and adapt (see review on the invalid-request access log).
 */
class InvalidRequestAccessLogHandlerTest {

    @Test
    void tooLongLineMapsToUriTooLong() {
        assertThat(InvalidRequestAccessLogHandler.statusCodeForCause(new TooLongHttpLineException("boom")))
                .isEqualTo(HttpResponseStatus.REQUEST_URI_TOO_LONG.code());
    }

    @Test
    void tooLongHeaderMapsToHeaderFieldsTooLarge() {
        assertThat(InvalidRequestAccessLogHandler.statusCodeForCause(new TooLongHttpHeaderException("boom")))
                .isEqualTo(HttpResponseStatus.REQUEST_HEADER_FIELDS_TOO_LARGE.code());
    }

    @Test
    void otherCausesMapToBadRequest() {
        assertThat(InvalidRequestAccessLogHandler.statusCodeForCause(new RuntimeException("boom")))
                .isEqualTo(HttpResponseStatus.BAD_REQUEST.code());
        assertThat(InvalidRequestAccessLogHandler.statusCodeForCause(null))
                .isEqualTo(HttpResponseStatus.BAD_REQUEST.code());
    }
}
