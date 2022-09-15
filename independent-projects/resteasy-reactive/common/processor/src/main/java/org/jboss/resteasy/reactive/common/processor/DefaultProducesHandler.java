package org.jboss.resteasy.reactive.common.processor;

import jakarta.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;
import org.jboss.resteasy.reactive.common.ResteasyReactiveConfig;

public interface DefaultProducesHandler {

    List<MediaType> handle(Context context);

    interface Context {
        Type nonAsyncReturnType();

        DotName httpMethod();

        IndexView index();

        ResteasyReactiveConfig config();
    }

    class Noop implements DefaultProducesHandler {

        public static Noop INSTANCE = new Noop();

        private Noop() {
        }

        @Override
        public List<MediaType> handle(Context context) {
            return Collections.emptyList();
        }
    }

    class DelegatingDefaultProducesHandler implements DefaultProducesHandler {
        private final List<DefaultProducesHandler> delegates;

        public DelegatingDefaultProducesHandler(List<DefaultProducesHandler> delegates) {
            this.delegates = Objects.requireNonNull(delegates);
        }

        @Override
        public List<MediaType> handle(Context context) {
            for (DefaultProducesHandler delegate : delegates) {
                List<MediaType> result = delegate.handle(context);
                if ((result != null) && !result.isEmpty()) {
                    return result;
                }
            }
            return Collections.emptyList();
        }
    }
}
