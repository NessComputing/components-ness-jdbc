package com.nesscomputing.jdbc;

import java.net.URI;

import org.skife.config.Config;

public abstract class DatabaseConfig
{
    @Config({"ness.db.${dbName}.uri","ness.db.defaults.uri"})
    public abstract URI getDbUri();
}
