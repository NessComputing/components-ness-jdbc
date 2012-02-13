package com.nesscomputing.jdbc.wrappers;

import java.sql.Array;
import java.sql.Connection;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.IDBI;

import com.nesscomputing.testing.lessio.AllowDNSResolution;
import com.nesscomputing.testing.lessio.AllowNetworkAccess;

@AllowDNSResolution
@AllowNetworkAccess(endpoints={"127.0.0.1:*"})
public class TestCreateArrayOfWrapper
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
            final Array s = c.createArrayOf("varchar", new String [] { "a", "b" });
            Assert.assertNotNull(s);
        }
        finally {
            if (c != null) {
                c.close();
            }
        }
    }
}
