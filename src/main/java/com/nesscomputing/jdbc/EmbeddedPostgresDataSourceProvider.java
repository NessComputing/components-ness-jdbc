package com.nesscomputing.jdbc;

import java.lang.annotation.Annotation;
import java.net.URI;
import javax.sql.DataSource;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.mchange.v2.c3p0.DriverManagerDataSource;
import com.nesscomputing.config.Config;
import com.nesscomputing.db.postgres.embedded.EmbeddedPostgreSQLController;
import com.nesscomputing.lifecycle.guice.AbstractLifecycleProvider;

/**
 * Provide a DatabaseModule with a DataSource from the embedded Postgres driver.
 * Schemas may be pre-loaded for drop-in replacement of "normal" databases.  Example
 * configuration:
 * <pre>
 * ness.db.mydb.schema-uri=classpath:/test-sql
 * ness.db.mydb.schema=test1,test2
 * </pre>
 */
class EmbeddedPostgresDataSourceProvider extends AbstractLifecycleProvider<DataSource>
{

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
        final DatabaseConfig epgConfig = config.getBean(DatabaseConfig.class, ImmutableMap.of("dbName", dbName));

        final URI baseUri = epgConfig.getSchemaUri();
        final String[] personalities = epgConfig.getSchemas().toArray(new String[epgConfig.getSchemas().size()]);

        final EmbeddedPostgreSQLController controller = new EmbeddedPostgreSQLController(baseUri, personalities);

        final DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setJdbcUrl(controller.getJdbcUri());
        ds.setUser("postgres");
        return ds;
    }
}
