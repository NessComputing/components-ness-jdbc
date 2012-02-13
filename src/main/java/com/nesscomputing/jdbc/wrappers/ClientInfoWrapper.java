package com.nesscomputing.jdbc.wrappers;

import java.sql.Connection;
import java.util.Properties;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Singleton;

/**
 * Provides a pooled C3P0 connection with the necessary magic to allow the various setClientInfo and getClientInfo methods to succeed.
 */
@Singleton
public final class ClientInfoWrapper extends AbstractC3P0ConnectionWrapper
{
    public ClientInfoWrapper() throws NoSuchMethodException
    {
        super(ImmutableSet.of(
                  Connection.class.getMethod("getClientInfo", new Class<?>[] { String.class }),
                  Connection.class.getMethod("getClientInfo", new Class<?>[0]),
                  Connection.class.getMethod("setClientInfo", new Class<?>[] { String.class, String.class }),
                  Connection.class.getMethod("setClientInfo", new Class<?>[] { Properties.class })));
    }
}
