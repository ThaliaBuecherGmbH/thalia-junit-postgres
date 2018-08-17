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

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.sql.SQLException;
import java.util.Collections;

import javax.sql.DataSource;

import org.apache.commons.lang3.SystemUtils;
import org.postgresql.ds.PGSimpleDataSource;

import de.flapdoodle.embed.process.config.IRuntimeConfig;
import lombok.extern.slf4j.Slf4j;
import ru.yandex.qatools.embed.postgresql.EmbeddedPostgres;
import ru.yandex.qatools.embed.postgresql.distribution.Version;

/**
 * Hilfsklasse für das Hoch- und Runterfahren von PostgreSQL.
 */
@Slf4j
public class PostgresEmbedder implements AutoCloseable {

    public enum PostgreVersion {

        V10_3(Version.V10_3),
        V9_6_8(Version.V9_6_8),
        V9_5_12(Version.V9_5_12);

        private final Version version;

        private PostgreVersion(final Version version) {
            this.version = version;
        }
    }

    public static int freePortNumber() {
        try {
            try (final ServerSocket socket = new ServerSocket(0)) {
                socket.setReuseAddress(true);
                return socket.getLocalPort();
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public interface PostgresInstance {

        DataSource getDataSource();

        void close();
    }

    public static class PerTestPostgresInstance implements PostgresInstance {

        private final EmbeddedPostgres postgres;
        private final DataSource dataSource;

        public PerTestPostgresInstance(final int aPortNumber, final PostgreVersion version) throws IOException, SQLException {

            final Version internalVersion = version.version;

            postgres = new EmbeddedPostgres(internalVersion);

            final File theUserHome = SystemUtils.getUserHome();
            final File thePGCacheDir = new File(theUserHome, "embeddedpostgres" + internalVersion.asInDownloadPath());
            thePGCacheDir.mkdirs();

            log.info("Benutze Cache Verzeichnis {}", thePGCacheDir);

            final IRuntimeConfig theRuntimeConfig = EmbeddedPostgres.cachedRuntimeConfig(thePGCacheDir.toPath());
            final String theJDBCURL = postgres.start(theRuntimeConfig,"localhost", aPortNumber, "testdb", "test", "test", Collections.emptyList());

            log.info("Embedded PostgreSQL ist über URL {} erreichbar", theJDBCURL);

            final PGSimpleDataSource source = new PGSimpleDataSource();
            source.setURL(theJDBCURL);

            dataSource = source;
        }

        @Override
        public DataSource getDataSource() {
            return dataSource;
        }

        @Override
        public void close() {
            try {
                postgres.stop();
            } catch (final Exception e) {
                log.warn("Fehler beim Herunterfahren der Datenbank", e);
            }
        }
    }

    public static class PerJVMPostgresInstance implements PostgresInstance {

        private static PostgresInstance GLOBAL;

        public PerJVMPostgresInstance(final int aPortNumber, final PostgreVersion version) throws SQLException, IOException {
            if (GLOBAL == null) {
                GLOBAL = new PerTestPostgresInstance(aPortNumber, version);
                Runtime.getRuntime().addShutdownHook(new Thread(() -> GLOBAL.close()));
            }
        }

        @Override
        public DataSource getDataSource() {
            return GLOBAL.getDataSource();
        }

        @Override
        public void close() {
        }
    }

    private final PostgresInstance postgresInstance;

    public PostgresEmbedder(final int aPortNumber, final boolean aCreatePerJVMInstance, final PostgreVersion version) throws SQLException, IOException {
        if (aCreatePerJVMInstance) {
            postgresInstance = new PerJVMPostgresInstance(aPortNumber, version);
        } else {
            postgresInstance = new PerTestPostgresInstance(aPortNumber, version);
        }
    }

    public PostgresEmbedder(final int aPortNumber, final PostgreVersion version) throws SQLException, IOException {
        this(aPortNumber, true, version);
    }

    public DataSource getDataSource() {
        return postgresInstance.getDataSource();
    }

    @Override
    public void close() throws Exception {
        postgresInstance.close();
    }
}