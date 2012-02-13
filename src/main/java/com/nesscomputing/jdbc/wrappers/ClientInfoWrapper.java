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
