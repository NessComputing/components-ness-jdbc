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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import com.google.common.collect.Maps;
import com.nesscomputing.logging.Log;

/**
 * A basic invocation handler that allows to ignore and intercept calls to the underlying
 * object.
 */
abstract class AbstractProxyInvocationHandler implements InvocationHandler
{
    private static final Log LOG = Log.findLog();

    /** Marks a non existing method. */
    protected final MethodHolder DOES_NOT_EXIST = new MethodHolder(null);

    /** Marks a method that does not exist but can be ignored. */
    protected final MethodHolder IGNORE_THIS_METHOD = new MethodHolder(null);

    /** Map from "proxy method invoked" to "method to invoke". */
    private final Map<Method, MethodWrapper> methodMap = Maps.newConcurrentMap();

    private final Object obj;
    private final Class<?> objectClass;

    protected AbstractProxyInvocationHandler(final Object obj)
    {
        this.obj = obj;
        this.objectClass = obj.getClass();

        LOG.trace("Set up proxy handler for '%s'", objectClass.getSimpleName());
    }

    /**
     * To be overwritten by classes extending this proxy. This allows interception of
     * method calls. If no interception should happen, it must return the object passed
     * in.
     */
    protected Object intercept(final Method method, final Object obj) throws Throwable
    {
        LOG.trace("Intercepted '%s' on '%s'", method.getName(), obj);
        return obj;
    }

    /**
     * To be overwritten by classes extending this proxy. If this method returns true,
     * then the underlying method is not searched for and invoked.
     */
    protected boolean ignore(final Method method)
    {
        LOG.trace("Accepted '%s'", method.getName());
        return false;
    }

    /**
     * To be overwritten by classes extending this proxy. This method can return a special
     * method holder depending on the arguments.
     */
    protected MethodWrapper createMethodWrapper(final Class<?> objectClass, final Object proxy, final Method method, final Object[] args)
    {
        return new MethodHolder(method);
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable
    {
        MethodWrapper methodWrapper = methodMap.get(method);

        if (methodWrapper == null) {
            methodWrapper = createMethodWrapper(objectClass, proxy, method, args);
            methodMap.put(method, methodWrapper);
        }

        if (methodWrapper == DOES_NOT_EXIST) {
            throw new AbstractMethodError(String.format("Method '%s' does not exist in '%s'", method.getName(), objectClass.getSimpleName()));
        }
        else if (methodWrapper == IGNORE_THIS_METHOD) {
            LOG.trace("Ignored '%s'", method.getName());
            return null;
        }
        else {
            try {
                return intercept(method, methodWrapper.invoke(obj, args));
            }
            // Gets both AbstractMethodError and NoSuchMethodError
            catch (IncompatibleClassChangeError icce) {
                if (ignore(method)) {
                    LOG.warn(icce, "Could not invoke '%s' on '%s', ignoring it now.", methodWrapper, objectClass.getSimpleName());
                    methodMap.put(method, IGNORE_THIS_METHOD);
                    return null;
                }
                else {
                    LOG.warn(icce, "Could not invoke '%s' on '%s'", methodWrapper, objectClass.getSimpleName());
                    methodMap.put(method, DOES_NOT_EXIST);
                    throw icce;
                }
            }
        }
    }


    /**
     * Intercepts calls to a proxied method to allow wrapping, changing or additional operations when a method is invoked.
     */
    static interface MethodInterceptor<T>
    {
        /**
         * Intercept the return value from a method call.
         */
        T intercept(T parent) throws Throwable;
    }

    /**
     * Represents a method in the method map. Can be replaced with an arbitrary piece of code to redirect method calls.
     */
    static interface MethodWrapper
    {
        /**
         * Invoke the code represented by this wrapper.
         */
        Object invoke(Object proxy, Object[] args) throws Exception;
    }

    /**
     * Helper because a ConcurrentHashMap can not store a null value.
     */
    protected static final class MethodHolder implements MethodWrapper
    {
        private final Method method;

        protected MethodHolder(final Method method)
        {
            this.method = method;
        }

        @Override
        public String toString()
        {
            return method == null ? "<null>" : method.getName();
        }

        @Override
        public Object invoke(final Object proxy, final Object[] args)
            throws IllegalArgumentException, IllegalAccessException, InvocationTargetException
        {
            if (method != null) {
                return method.invoke(proxy, args);
            }
            else {
                throw new IllegalAccessException("no method was found!");
            }
        }
    }
}
