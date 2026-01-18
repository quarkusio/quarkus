package io.quarkus.infinispan.client.runtime.jfr.event;

public sealed interface InfinispanEventRecorder permits InfinispanEventRecorder.CacheWideEventRecorder,
        InfinispanEventRecorder.MultiEntriesEventRecorder, InfinispanEventRecorder.SingleEntryEventRecorder {

    InfinispanEventRecorder createAndCommitStartEvent();

    InfinispanEventRecorder createPeriodEvent();

    InfinispanEventRecorder commitPeriodEvent();

    InfinispanEventRecorder createAndCommitEndEvent();

    static InfinispanEventRecorder createSingleEntryEventRecorder(String traceId, String spanId, String methodName,
            String cacheName, String clusterName) {
        return new SingleEntryEventRecorder(traceId, spanId, methodName, cacheName, clusterName);
    }

    static InfinispanEventRecorder createMultiEntryEventRecorder(String traceId, String spanId, String methodName,
            String cacheName, String clusterName, int size) {
        return new MultiEntriesEventRecorder(traceId, spanId, methodName, cacheName, clusterName, size);
    }

    static InfinispanEventRecorder createCacheWideEventRecorder(String traceId, String spanId, String methodName,
            String cacheName, String clusterName) {
        return new CacheWideEventRecorder(traceId, spanId, methodName, cacheName, clusterName);
    }

    final class SingleEntryEventRecorder implements InfinispanEventRecorder {

        SingleEntryPeriodEvent periodEvent = null;
        private final String traceId;
        private final String spanId;
        private final String methodName;
        private final String cacheName;
        private final String clusterName;

        public SingleEntryEventRecorder(String traceId, String spanId, String methodName, String cacheName,
                String clusterName) {
            this.traceId = traceId;
            this.spanId = spanId;
            this.methodName = methodName;
            this.cacheName = cacheName;
            this.clusterName = clusterName;
        }

        @Override
        public InfinispanEventRecorder createAndCommitStartEvent() {
            SingleEntryStartEvent startEvent = new SingleEntryStartEvent();
            if (startEvent.shouldCommit()) {
                populateCacheEventInfo(startEvent);
                startEvent.commit();
            }
            return this;
        }

        @Override
        public InfinispanEventRecorder createPeriodEvent() {
            periodEvent = new SingleEntryPeriodEvent();
            periodEvent.begin();
            return this;
        }

        @Override
        public InfinispanEventRecorder commitPeriodEvent() {
            if (periodEvent != null) {
                periodEvent.end();
                if (periodEvent.shouldCommit()) {
                    populateCacheEventInfo(periodEvent);
                    periodEvent.commit();
                }
            }
            return this;
        }

        @Override
        public InfinispanEventRecorder createAndCommitEndEvent() {
            SingleEntryEndEvent endEvent = new SingleEntryEndEvent();
            if (endEvent.shouldCommit()) {
                populateCacheEventInfo(endEvent);
                endEvent.commit();
            }
            return this;
        }

        private void populateCacheEventInfo(AbstractCacheEvent event) {
            event.traceId = traceId;
            event.spanId = spanId;
            event.clusterName = clusterName;
            event.cacheName = cacheName;
            event.method = methodName;
        }
    }

    final class MultiEntriesEventRecorder implements InfinispanEventRecorder {

        MultiEntryPeriodEvent periodEvent = null;
        private final String traceId;
        private final String spanId;
        private final String methodName;
        private final String cacheName;
        private final String clusterName;
        private final int size;

        public MultiEntriesEventRecorder(String traceId, String spanId, String methodName, String cacheName, String clusterName,
                int size) {
            this.traceId = traceId;
            this.spanId = spanId;
            this.methodName = methodName;
            this.cacheName = cacheName;
            this.clusterName = clusterName;
            this.size = size;
        }

        @Override
        public InfinispanEventRecorder createAndCommitStartEvent() {
            MultiEntryStartEvent startEvent = new MultiEntryStartEvent();
            if (startEvent.shouldCommit()) {
                populateCacheEventInfo(startEvent);
                startEvent.commit();
            }
            return this;
        }

        @Override
        public InfinispanEventRecorder createPeriodEvent() {
            periodEvent = new MultiEntryPeriodEvent();
            periodEvent.begin();
            return this;
        }

        @Override
        public InfinispanEventRecorder commitPeriodEvent() {
            if (periodEvent != null) {
                periodEvent.end();
                if (periodEvent.shouldCommit()) {
                    populateCacheEventInfo(periodEvent);
                    periodEvent.commit();
                }
            }
            return this;
        }

        @Override
        public InfinispanEventRecorder createAndCommitEndEvent() {
            MultiEntryEndEvent endEvent = new MultiEntryEndEvent();
            if (endEvent.shouldCommit()) {
                populateCacheEventInfo(endEvent);
                endEvent.commit();
            }
            return this;
        }

        private void populateCacheEventInfo(AbstractMultiEntryEvent event) {
            event.traceId = traceId;
            event.spanId = spanId;
            event.clusterName = clusterName;
            event.cacheName = cacheName;
            event.method = methodName;
            event.elementCount = size;
        }
    }

    final class CacheWideEventRecorder implements InfinispanEventRecorder {

        CacheWidePeriodEvent periodEvent = null;
        private final String traceId;
        private final String spanId;
        private final String methodName;
        private final String cacheName;
        private final String clusterName;

        public CacheWideEventRecorder(String traceId, String spanId, String methodName, String cacheName, String clusterName) {
            this.traceId = traceId;
            this.spanId = spanId;
            this.methodName = methodName;
            this.cacheName = cacheName;
            this.clusterName = clusterName;
        }

        @Override
        public InfinispanEventRecorder createAndCommitStartEvent() {
            CacheWideStartEvent startEvent = new CacheWideStartEvent();
            if (startEvent.shouldCommit()) {
                populateCacheEventInfo(startEvent);
                startEvent.commit();
            }
            return this;
        }

        @Override
        public InfinispanEventRecorder createPeriodEvent() {
            periodEvent = new CacheWidePeriodEvent();
            periodEvent.begin();
            return this;
        }

        @Override
        public InfinispanEventRecorder commitPeriodEvent() {
            if (periodEvent != null) {
                periodEvent.end();
                if (periodEvent.shouldCommit()) {
                    populateCacheEventInfo(periodEvent);
                    periodEvent.commit();
                }
            }
            return this;
        }

        @Override
        public InfinispanEventRecorder createAndCommitEndEvent() {
            CacheWideEndEvent endEvent = new CacheWideEndEvent();
            if (endEvent.shouldCommit()) {
                populateCacheEventInfo(endEvent);
                endEvent.commit();
            }
            return this;
        }

        private void populateCacheEventInfo(AbstractCacheEvent event) {
            event.traceId = traceId;
            event.spanId = spanId;
            event.clusterName = clusterName;
            event.cacheName = cacheName;
            event.method = methodName;
        }
    }
}
