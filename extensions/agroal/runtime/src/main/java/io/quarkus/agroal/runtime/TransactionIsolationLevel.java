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

package io.quarkus.agroal.runtime;

import io.agroal.api.configuration.AgroalConnectionFactoryConfiguration.TransactionIsolation;

public enum TransactionIsolationLevel {
    UNDEFINED(TransactionIsolation.UNDEFINED),
    NONE(TransactionIsolation.NONE),
    READ_UNCOMMITTED(TransactionIsolation.READ_UNCOMMITTED),
    READ_COMMITTED(TransactionIsolation.READ_COMMITTED),
    REPEATABLE_READ(TransactionIsolation.REPEATABLE_READ),
    SERIALIZABLE(TransactionIsolation.SERIALIZABLE);

    TransactionIsolation jdbcTransactionIsolationLevel;

    TransactionIsolationLevel(TransactionIsolation jdbcTransactionIsolationLevel) {
        this.jdbcTransactionIsolationLevel = jdbcTransactionIsolationLevel;
    }

    public static TransactionIsolationLevel of(String value) {
        switch (value) {
            case "none":
                return NONE;
            case "read-committed":
                return READ_COMMITTED;
            case "read-uncommitted":
                return READ_UNCOMMITTED;
            case "repeatable-read":
                return REPEATABLE_READ;
            case "serializable":
                return SERIALIZABLE;
            default:
                return UNDEFINED;
        }
    }
}