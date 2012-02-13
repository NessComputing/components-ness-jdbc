package com.nesscomputing.jdbc.wrappers;

import java.sql.Connection;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.IDBI;

import com.nesscomputing.testing.lessio.AllowDNSResolution;
import com.nesscomputing.testing.lessio.AllowNetworkAccess;

@AllowDNSResolution
@AllowNetworkAccess(endpoints={"127.0.0.1:*"})
public class TestClientInfoWrapper
{
    private IDBI testDbi;

    @Before
    public void setUp()
    {
        testDbi = new DBI("jdbc:postgresql://localhost/trumpet_test", "trumpet_test", "");
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
