package com.nesscomputing.jdbc;

import java.lang.annotation.Annotation;
import java.util.Set;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.IDBI;
import org.skife.jdbi.v2.tweak.HandleCallback;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.PrivateModule;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.nesscomputing.lifecycle.LifecycleStage;
import com.nesscomputing.lifecycle.guice.OnStage;
import com.nesscomputing.logging.Log;

/**
 * Module which will clean out a specified set of tables from your
 * test database on test start / stop.
 *
 * If you install this outside of tests, you will be really sorry.
 * I promise! :-)
 */
public class DatabaseCleanerModule extends AbstractModule {
    private static final Log LOG = Log.findLog();
    private final Annotation annotation;
    private final String[] tables;
    private final String schemaNameToDrop;

    /**
     * @param name the database annotation name (same as DatabaseModule)
     * @param tables the tables to remove all rows from
     */
    public DatabaseCleanerModule(String schemaNameToDrop, String name, String... tables) {
        this(schemaNameToDrop, Names.named(name), tables);
    }

    public DatabaseCleanerModule(String schemaNameToDrop, Annotation annotation, String... tables) {
        this.schemaNameToDrop = schemaNameToDrop;
        this.annotation = annotation;
        this.tables = tables;
    }


    @Override
    protected void configure() {
        install (new PrivateModule() {
            @Override
            protected void configure() {
                bind (Annotation.class).toInstance(annotation);
                bindConstant().annotatedWith(Names.named("SchemaName")).to(ObjectUtils.toString(schemaNameToDrop));
                bind (new TypeLiteral<Set<String>>() {}).toInstance(ImmutableSet.copyOf(tables));
                bind (Cleaner.class).asEagerSingleton();
            }
        });
    }

    static class Cleaner {
        private final IDBI db;
        private final Set<String> tables;
        private final String schemaNameToDrop;
        @Inject
        Cleaner(Annotation annotation, Injector injector, Set<String> tables, @Named("SchemaName") String schemaNameToDrop) {
            this.tables = tables;
            this.schemaNameToDrop = schemaNameToDrop;
            this.db = injector.getInstance(Key.get(IDBI.class, annotation));
        }

        @OnStage(LifecycleStage.START)
        void spinUp() {
            LOG.info("Cleaning tables %s", tables);
            db.withHandle(new HandleCallback<Void>() {
                @Override
                public Void withHandle(Handle handle) throws Exception {
                    for (String table : tables) {
                        Preconditions.checkArgument(table.matches("[a-zA-Z0-9_]+"), "bad table name \"" + table + "\"");
                        handle.createStatement("DELETE FROM " + table).execute();
                    }
                    return null;
                }
            });
        }

        @OnStage(LifecycleStage.STOP)
        void spinDown() {

            if (StringUtils.isBlank(schemaNameToDrop)) {
                return;
            }

            db.withHandle(new HandleCallback<Void>() {
                @Override
                public Void withHandle(Handle handle) throws Exception {
                    handle.createStatement(String.format("DROP SCHEMA IF EXISTS \"%s\" CASCADE", schemaNameToDrop)).execute();
                    return null;
                }
            });
        }
    }
}
