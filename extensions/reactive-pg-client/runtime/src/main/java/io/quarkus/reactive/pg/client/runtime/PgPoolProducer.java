/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package io.quarkus.reactive.pg.client.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import io.reactiverse.pgclient.PgPool;

@ApplicationScoped
public class PgPoolProducer {

    private volatile PgPool pgPool;
    private volatile io.reactiverse.axle.pgclient.PgPool axlePgPool;
    private volatile io.reactiverse.reactivex.pgclient.PgPool rxPgPool;

    void initialize(PgPool pgPool) {
        this.pgPool = pgPool;
        this.axlePgPool = io.reactiverse.axle.pgclient.PgPool.newInstance(pgPool);
        this.rxPgPool = io.reactiverse.reactivex.pgclient.PgPool.newInstance(pgPool);
    }

    @Singleton
    @Produces
    public PgPool pgPool() {
        return pgPool;
    }

    @Singleton
    @Produces
    public io.reactiverse.axle.pgclient.PgPool axlePgPool() {
        return axlePgPool;
    }

    @Singleton
    @Produces
    public io.reactiverse.reactivex.pgclient.PgPool rxPgPool() {
        return rxPgPool;
    }
}
