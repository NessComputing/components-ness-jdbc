package com.nesscomputing.jdbc;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.IDBI;
import org.skife.jdbi.v2.tweak.HandleCallback;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
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
        Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure()
            {
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
}
