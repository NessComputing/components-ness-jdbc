package com.nesscomputing.jdbc;

import java.lang.annotation.Annotation;

import javax.sql.DataSource;

import com.google.inject.Provider;

public enum DatabaseProviderType
{
    C3P0 {
        @Override
        Provider<DataSource> create(String dbName, Annotation annotation)
        {
            return new C3P0DataSourceProvider(dbName, annotation);
        }
    },
    EMBEDDED {
        @Override
        Provider<DataSource> create(String dbName, Annotation annotation)
        {
            return new EmbeddedPostgresDataSourceProvider(dbName, annotation);
        }
    };

    abstract Provider<DataSource> create(String dbName, Annotation annotation);
}
