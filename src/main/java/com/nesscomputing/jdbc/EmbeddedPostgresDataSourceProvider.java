package com.nesscomputing.jdbc;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Iterator;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.configuration.AbstractConfiguration;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Closeables;
import com.google.inject.Inject;
import com.nesscomputing.config.Config;
import com.nesscomputing.db.postgres.embedded.EmbeddedPostgreSQL;
import com.nesscomputing.lifecycle.LifecycleStage;
import com.nesscomputing.lifecycle.guice.AbstractLifecycleProvider;
import com.nesscomputing.lifecycle.guice.LifecycleAction;

/**
 * Provide a DatabaseModule with a DataSource from the embedded Postgres driver.
 * Configuration options under <code>ness.db.&lt;db-name&gt;</code> will be passed
 * to the postmaster.
 */
class EmbeddedPostgresDataSourceProvider extends AbstractLifecycleProvider<DataSource>
{
    private static final List<String> IGNORED_KEYS = ImmutableList.of("provider", "uri");

    private final String dbName;
    private volatile Config config;

    EmbeddedPostgresDataSourceProvider(String dbName, Annotation annotation)
    {
        this.dbName = dbName;
    }

    @Inject
    public void injectConfig(Config config)
    {
        this.config = config;
    }

    @Override
    protected DataSource internalGet()
    {
        AbstractConfiguration epgConfig = config.getConfiguration("ness.db." + dbName);

        EmbeddedPostgreSQL.Builder builder = EmbeddedPostgreSQL.builder();

        @SuppressWarnings("unchecked")
        Iterator<String> keys = epgConfig.getKeys();

        while (keys.hasNext()) {
            String key = keys.next();
            if (IGNORED_KEYS.contains(key)) {
                continue;
            }
            builder.setServerConfig(key, epgConfig.getString(key));
        }

        final EmbeddedPostgreSQL epg;
        try {
            epg = builder.start();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        addAction(LifecycleStage.STOP_STAGE, new LifecycleAction<DataSource>() {
            @Override
            public void performAction(DataSource obj)
            {
                Closeables.closeQuietly(epg);
            }
        });

        return epg.getPostgresDatabase();
    }
}
