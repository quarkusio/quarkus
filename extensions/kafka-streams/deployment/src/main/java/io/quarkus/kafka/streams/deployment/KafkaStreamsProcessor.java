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

package io.quarkus.kafka.streams.deployment;

import java.io.IOException;

import org.apache.kafka.common.serialization.Serdes.ByteArraySerde;
import org.apache.kafka.streams.errors.DefaultProductionExceptionHandler;
import org.apache.kafka.streams.errors.LogAndFailExceptionHandler;
import org.apache.kafka.streams.processor.DefaultPartitionGrouper;
import org.apache.kafka.streams.processor.FailOnInvalidTimestamp;
import org.apache.kafka.streams.processor.internals.StreamsPartitionAssignor;
import org.rocksdb.util.Environment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.RuntimeReinitializedClassBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateResourceBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.kafka.streams.runtime.KafkaStreamsTemplate;

class KafkaStreamsProcessor {

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void build(RecorderContext recorder,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<RuntimeReinitializedClassBuildItem> reinitialized,
            BuildProducer<SubstrateResourceBuildItem> nativeLibs) throws IOException {

        feature.produce(new FeatureBuildItem(FeatureBuildItem.KAFKA_STREAMS));

        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, false, StreamsPartitionAssignor.class));
        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, false, DefaultPartitionGrouper.class));
        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, false, DefaultProductionExceptionHandler.class));
        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, false, LogAndFailExceptionHandler.class));
        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, false, ByteArraySerde.class));
        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, false, FailOnInvalidTimestamp.class));

        nativeLibs.produce(new SubstrateResourceBuildItem(Environment.getJniLibraryFileName("rocksdb")));

        // re-initializing RocksDB to enable load of native libs
        reinitialized.produce(new RuntimeReinitializedClassBuildItem("org.rocksdb.RocksDB"));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void build(KafkaStreamsTemplate template) {
        // Explicitly loading RocksDB native libs, as that's normally done from within
        // static initializers which already ran during build
        template.loadRocksDb();
    }
}
