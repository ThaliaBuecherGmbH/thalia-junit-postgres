/*
 * (c) Copyright 2018 Thalia Bücher GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.thalia.junit.postgres;

import javax.sql.DataSource;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import lombok.Getter;

public class PostgresEmbedderRule implements TestRule {

    public static class Builder {

        private int portNumber;
        private PostgresEmbedder.PostgreVersion version;
        private boolean jvmSingleton;

        private Builder(final int portNumber) {
            this.portNumber = portNumber;
            this.version = PostgresEmbedder.PostgreVersion.V9_5_12;
            this.jvmSingleton = false;
        }

        public Builder withPort(final int portNumber) {
            this.portNumber = portNumber;
            return this;
        }

        public Builder withVersion(final PostgresEmbedder.PostgreVersion version) {
            this.version = version;
            return this;
        }

        public Builder asJVMSingleton() {
            jvmSingleton = true;
            return this;
        }

        public PostgresEmbedderRule build() {
            return new PostgresEmbedderRule(portNumber, version, jvmSingleton);
        }
    }

    public static Builder builder() {
        return new Builder(PostgresEmbedder.freePortNumber());
    }

    private final int portNumber;
    private final PostgresEmbedder.PostgreVersion version;
    private final boolean jvmSingleton;

    @Getter
    private DataSource dataSource;

    @Getter
    private PostgresEmbedder postgresEmbedder;

    private PostgresEmbedderRule(final int aPortNumber, final PostgresEmbedder.PostgreVersion version, final boolean asJVMSingleton) {
        this.portNumber = aPortNumber;
        this.version = version;
        this.jvmSingleton = asJVMSingleton;
    }

    @Override
    public Statement apply(final Statement aBase, final Description aDescription) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try (final PostgresEmbedder theEmbedder = new PostgresEmbedder(portNumber, jvmSingleton, version)) {
                    postgresEmbedder = theEmbedder;
                    dataSource = theEmbedder.getDataSource();
                    aBase.evaluate();
                }
            }
        };
    }
}