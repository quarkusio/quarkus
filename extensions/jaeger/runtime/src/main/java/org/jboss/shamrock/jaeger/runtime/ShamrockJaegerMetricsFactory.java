/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.shamrock.jaeger.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;

import io.jaegertracing.internal.metrics.Counter;
import io.jaegertracing.internal.metrics.Gauge;
import io.jaegertracing.internal.metrics.Timer;
import io.jaegertracing.spi.MetricsFactory;
import io.smallrye.metrics.MetricRegistries;

public class ShamrockJaegerMetricsFactory implements MetricsFactory {

    private static final String METRICS_PREFIX = "jaeger_tracer_";

    MetricRegistry registry = MetricRegistries.get(MetricRegistry.Type.BASE);
    
    @Override
    public Counter createCounter(final String name, final Map<String, String> tags) {
      org.eclipse.microprofile.metrics.Counter counter=
            registry.counter(meta(name, tags, MetricType.COUNTER));

      return new Counter() {
        @Override
        public void inc(long delta) {
          counter.inc(delta);
        }
      };
    }
  
    @Override
    public Timer createTimer(final String name, final Map<String, String> tags) {
      org.eclipse.microprofile.metrics.Timer timer=
            registry.timer(meta(name, tags, MetricType.TIMER));

      return new Timer() {
        @Override
        public void durationMicros(long time) {
          timer.update(time, TimeUnit.MICROSECONDS);
        }
      };
    }
  
    @Override
    public Gauge createGauge(final String name, final Map<String, String> tags) {
      JaegerGauge gauge=registry.register(meta(name, tags, MetricType.GAUGE), new JaegerGauge());

      return new Gauge() {
        @Override
        public void update(long amount) {
          gauge.update(amount);
        }
      };
    }

    static Metadata meta(String name, final Map<String, String> tags, MetricType type) {
      if (!name.startsWith(METRICS_PREFIX)) {
        // Add default prefix
        name = METRICS_PREFIX + name;
      }
      Metadata meta = new Metadata(name, type);
      meta.setDisplayName(name);
      meta.setUnit("none");
      meta.setDescription(name);
      meta.setTags(new HashMap<String,String>(tags));
      meta.setReusable(true);
      return meta;
    }
  
    static class JaegerGauge implements org.eclipse.microprofile.metrics.Gauge<Long> {
      private AtomicLong value=new AtomicLong();
  
      public void update(long value) {
        this.value.set(value);
      }

      @Override
      public Long getValue() {
        return value.get();
      }
    }
}

