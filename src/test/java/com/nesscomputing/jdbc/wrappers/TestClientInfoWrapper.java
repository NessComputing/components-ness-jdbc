/**
 * Copyright (C) 2012 Ness Computing, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nesscomputing.jdbc.wrappers;

import java.sql.Connection;
import java.util.Properties;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.skife.jdbi.v2.IDBI;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.google.inject.name.Named;
import com.google.inject.util.Modules;
import com.nesscomputing.config.ConfigModule;
import com.nesscomputing.db.postgres.junit.LocalPostgresControllerTestRule;
import com.nesscomputing.db.postgres.junit.PostgresRules;
import com.nesscomputing.jdbc.DatabaseModule;
import com.nesscomputing.lifecycle.Lifecycle;
import com.nesscomputing.lifecycle.LifecycleStage;
import com.nesscomputing.lifecycle.guice.LifecycleModule;
import com.nesscomputing.testing.lessio.AllowDNSResolution;
import com.nesscomputing.testing.lessio.AllowNetworkAccess;

/**
 * This test requires a local postgres database and a "postgres" user that can connect to the database without a password.
 */
@AllowDNSResolution
@AllowNetworkAccess(endpoints={"127.0.0.1:5432"})
public class TestClientInfoWrapper
{
    @Rule
    public static final LocalPostgresControllerTestRule DATABASE = PostgresRules.databaseControllerRule();

    @Inject
    @Named("test")
    private IDBI testDbi;

    @Inject
    private Lifecycle lifecycle;

    // As this test wants to test a wrapper that is only available with the database module, it can not use the
    // DBI available from DATABASE.getDbi() directly (that would be a direct postgres connection). The following
    // Modules.override integrates the Database module with the Guice module from ness-pg so that it connects
    // to the temporary database but still uses the C3P0 pooling.
    //
    // For any unit tests that wants to test deeper features inside the ness-jdbc code, this is the way to do it.
    @Before
    public void setUp()
    {
        final Injector inj = Guice.createInjector(Stage.PRODUCTION,
                                                  Modules.override(DATABASE.getGuiceModule("test")).with(new DatabaseModule("test")),
                                                  ConfigModule.forTesting(),
                                                  new LifecycleModule());

        inj.injectMembers(this);

        Assert.assertNotNull(testDbi);
        Assert.assertNotNull(lifecycle);

        lifecycle.executeTo(LifecycleStage.START_STAGE);
    }

    @After
    public void tearDown()
    {
        Assert.assertNotNull(lifecycle);

        lifecycle.executeTo(LifecycleStage.STOP_STAGE);
    }

    @Test
    public void testSimple() throws Exception
    {
        Connection c = null;
        try {
            c = testDbi.open().getConnection();
            final Properties p = c.getClientInfo();
            Assert.assertNotNull(p);
        }
        finally {
            if (c != null) {
                c.close();
            }
        }
    }

    @Test
    public void testSetAndGetProps() throws Exception
    {
        Connection c = null;
        try {
            c = testDbi.open().getConnection();
            final Properties test = new Properties();
            test.setProperty("ApplicationName", "Bar");
            c.setClientInfo(test);

            final Properties p = c.getClientInfo();
            Assert.assertEquals("Bar", p.getProperty("ApplicationName"));

        }
        finally {
            if (c != null) {
                c.close();
            }
        }
    }

    @Test
    public void testSetAndGetSingle() throws Exception
    {
        Connection c = null;
        try {
            c = testDbi.open().getConnection();
            c.setClientInfo("ApplicationName", "Blo");

            Assert.assertEquals("Blo", c.getClientInfo("ApplicationName"));

        }
        finally {
            if (c != null) {
                c.close();
            }
        }
    }
}
