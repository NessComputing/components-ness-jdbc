package com.nesscomputing.jdbc;

import java.sql.SQLException;
import java.util.TimeZone;

import javax.sql.DataSource;

import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.tweak.HandleCallback;
import org.skife.jdbi.v2.util.StringMapper;

import com.nesscomputing.logging.Log;

/**
 * Check that a pool is ready for use
 * @author steven
 */
class DatabaseChecker {
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    private static final Log LOG = Log.findLog();

    static void checkPool(final DataSource pool) throws SQLException {
        // Right now just check that the DB and JVM are UTC.  This caused me infinite pain
        // and as such we will refuse to set up such a misconfigured system.

        final DBI dbi = new DBI(pool);

        final String timeZone = dbi.withHandle(new HandleCallback<String>() {

            @Override
            public String withHandle(final Handle handle) throws Exception
            {
                final String productName = handle.getConnection().getMetaData().getDatabaseProductName();
                // right now, we only care about PG
                if (!"POSTGRESQL".equalsIgnoreCase(productName)) {
                    return "UTC";
                }
                return handle.createQuery("SHOW timezone").map(StringMapper.FIRST).first();
            }
        });

        if (!"UTC".equalsIgnoreCase(timeZone)) {
            throw new IllegalStateException(String.format("Postgres database time zone must be set to UTC but is %s. Please fix your database configuration.", timeZone));
        }
    }

    static void fixUTCTimezone()
    {
        if (!TimeZone.getDefault().equals(UTC)) {
            LOG.warn("JVM timezone is not UTC! Switching to UTC to ensure correct timestamps on Postgres!");
            TimeZone.setDefault(UTC);
        }
    }
}
