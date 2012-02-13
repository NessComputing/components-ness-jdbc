package com.nesscomputing.jdbc;

import java.net.URI;

public final class ImmutableDatabaseConfig extends DatabaseConfig
{
    private final URI dbUri;

    public ImmutableDatabaseConfig(final URI dbUri)
    {
        this.dbUri = dbUri;
    }

    @Override
    public URI getDbUri()
    {
        return dbUri;
    }
}
