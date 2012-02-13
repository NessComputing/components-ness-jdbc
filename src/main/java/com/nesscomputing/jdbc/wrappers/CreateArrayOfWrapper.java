package com.nesscomputing.jdbc.wrappers;

import java.sql.Connection;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Singleton;

/**
 * Provides a pooled C3P0 connection with the necessary magic to allow {@link java.sql.Connection#createArrayOf(String, Object[])} to succeed.
 */
@Singleton
public final class CreateArrayOfWrapper extends AbstractC3P0ConnectionWrapper
{
    public CreateArrayOfWrapper() throws NoSuchMethodException
    {
        super(ImmutableSet.of(Connection.class.getMethod("createArrayOf", new Class<?>[] { String.class, Object [].class})));
    }
}
