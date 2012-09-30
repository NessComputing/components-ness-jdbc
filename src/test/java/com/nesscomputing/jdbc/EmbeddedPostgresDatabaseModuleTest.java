package com.nesscomputing.jdbc;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Test;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.IDBI;
import org.skife.jdbi.v2.tweak.HandleCallback;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Stage;
import com.google.inject.name.Named;
import com.nesscomputing.config.Config;
import com.nesscomputing.config.ConfigModule;
import com.nesscomputing.testing.lessio.AllowExternalProcess;
import com.nesscomputing.testing.lessio.AllowLocalFileAccess;
import com.nesscomputing.testing.lessio.AllowNetworkAccess;
import com.nesscomputing.testing.lessio.AllowNetworkListen;

@AllowExternalProcess
@AllowLocalFileAccess(paths= {"%TMP_DIR%"})
@AllowNetworkListen(ports= {0})
@AllowNetworkAccess(endpoints="127.0.0.1:*")
public class EmbeddedPostgresDatabaseModuleTest
{
    @Inject
    @Named("test")
    IDBI dbi;

    @Test
    public void testWorks() throws Exception
    {
        Guice.createInjector(Stage.PRODUCTION, new AbstractModule() {
            @Override
            protected void configure()
            {
                binder().disableCircularProxies();
                binder().requireExplicitBindings();

                install (new ConfigModule(Config.getFixedConfig(ImmutableMap.of(
                        "ness.db.test.provider", "EMBEDDED"
                    ))));
                install (new DatabaseModule("test"));
            }
        }).injectMembers(this);

        dbi.withHandle(new HandleCallback<Void>() {
            @Override
            public Void withHandle(Handle h) throws Exception
            {
                assertEquals(ImmutableMap.of("win", 1), Iterables.getOnlyElement(h.createQuery("SELECT 1 AS win").list()));
                return null;
            }
        });
    }

    @Test
    public void testSchemaLoad() throws Exception
    {
        final Map<String, String> config = Maps.newHashMap();
        config.put("ness.db.test.provider", "EMBEDDED");
        config.put("ness.db.test.schema-uri", "classpath:/test-sql");
        config.put("ness.db.test.schema", "test1,test2");

        Guice.createInjector(Stage.PRODUCTION, new AbstractModule() {
            @Override
            protected void configure()
            {
                binder().disableCircularProxies();
                binder().requireExplicitBindings();

                install (new ConfigModule(Config.getFixedConfig(config)));
                install (new DatabaseModule("test"));
            }
        }).injectMembers(this);

        dbi.withHandle(new HandleCallback<Void>() {
            @Override
            public Void withHandle(Handle h) throws Exception
            {
                assertEquals(0, Iterables.size(h.createQuery("SELECT * FROM test1, test2").list()));
                return null;
            }
        });
    }

    @Test
    public void testDefaultSchemaLoad() throws Exception
    {
        final Map<String, String> config = Maps.newHashMap();
        config.put("ness.db.test.provider", "EMBEDDED");
        config.put("ness.db.test.schema-uri", "classpath:/test-sql");
        config.put("ness.db.defaults.schema", "test1,test2");

        Guice.createInjector(Stage.PRODUCTION, new AbstractModule() {
            @Override
            protected void configure()
            {
                binder().disableCircularProxies();
                binder().requireExplicitBindings();

                install (new ConfigModule(Config.getFixedConfig(config)));
                install (new DatabaseModule("test"));
            }
        }).injectMembers(this);

        dbi.withHandle(new HandleCallback<Void>() {
            @Override
            public Void withHandle(Handle h) throws Exception
            {
                assertEquals(0, Iterables.size(h.createQuery("SELECT * FROM test1, test2").list()));
                return null;
            }
        });
    }
}
