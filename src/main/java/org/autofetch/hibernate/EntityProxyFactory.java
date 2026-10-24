/**
 * Copyright 2008 Ali Ibrahim
 * <p>
 * This file is part of Autofetch. Autofetch is free software: you can redistribute it and/or modify it under the terms of the Lesser GNU General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. Autofetch is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the Lesser GNU General Public License for more details. You should have received a copy of the Lesser GNU General Public
 * License along with Autofetch.  If not, see <http://www.gnu.org/licenses/>.
 * <p>
 * Hibernate 7 port (2026): ByteBuddy instead of Javassist, and one factory per service instead of static state, because the
 * entity metadata now comes from AutofetchMappingContributor instead of the tuplizer.
 */
package org.autofetch.hibernate;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.loading.ClassInjector;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import org.hibernate.HibernateException;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static net.bytebuddy.matcher.ElementMatchers.isFinalizer;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static net.bytebuddy.matcher.ElementMatchers.takesNoArguments;

public class EntityProxyFactory implements Serializable {

    private static final long serialVersionUID = 1L;

    static final String HANDLER_FIELD = "$$_autofetch_handler";

    private static final ConcurrentMap<Class<?>, Class<?>> entityFactoryMap = new ConcurrentHashMap<>();

    private final ExtentManager extentManager;

    private final ConcurrentMap<String, EntityMetadata> entities = new ConcurrentHashMap<>();

    private final ConcurrentMap<Class<?>, String> entityNames = new ConcurrentHashMap<>();

    EntityProxyFactory(ExtentManager extentManager) {
        this.extentManager = extentManager;
    }

    void register(String entityName, Class<?> mappedClass, Set<Property> persistentProperties) {
        entities.put(entityName, new EntityMetadata(mappedClass, persistentProperties));
        entityNames.put(mappedClass, entityName);
    }

    /**
     * Returns the entity name of a tracking subclass instance's class.
     */
    String getEntityName(Class<?> proxyClass) {
        return entityNames.get(proxyClass.getSuperclass());
    }

    /**
     * Returns a new tracking instance of the entity, or {@code null} to let Hibernate use the default constructor,
     * which is the case for final classes, because they cannot be subclassed.
     */
    Object newInstance(String entityName) {
        EntityMetadata metadata = entities.get(entityName);
        if (metadata == null || Modifier.isFinal(metadata.mappedClass.getModifiers())) {
            return null;
        }
        try {
            Class<?> proxyClass = getProxyFactory(metadata.mappedClass);
            Object proxy = proxyClass.getDeclaredConstructor().newInstance();
            Field handler = proxyClass.getField(HANDLER_FIELD);
            handler.set(proxy, new EntityProxyMethodHandler(metadata.persistentProperties, extentManager));
            return proxy;
        } catch (ReflectiveOperationException e) {
            throw new HibernateException("Unable to instantiate class " + metadata.mappedClass.getName(), e);
        }
    }

    private static Class<?> getProxyFactory(Class<?> persistentClass) {
        return entityFactoryMap.computeIfAbsent(persistentClass, EntityProxyFactory::createProxyClass);
    }

    private static Class<?> createProxyClass(Class<?> persistentClass) {
        return new ByteBuddy()
                .subclass(persistentClass)
                .name(persistentClass.getName() + "$Autofetch")
                .implement(TrackableEntity.class)
                .defineField(HANDLER_FIELD, EntityProxyMethodHandler.class, Visibility.PUBLIC)
                // Like the Javassist proxies, intercept every overridable method except finalize
                .method(not(isFinalizer()).and(not(named("clone").and(takesNoArguments()))))
                .intercept(MethodDelegation.withDefaultConfiguration()
                        .filter(named("intercept"))
                        .toField(HANDLER_FIELD))
                .make()
                .load(persistentClass.getClassLoader(), classLoadingStrategy(persistentClass))
                .getLoaded();
    }

    private static ClassLoadingStrategy<ClassLoader> classLoadingStrategy(Class<?> persistentClass) {
        if (ClassInjector.UsingLookup.isAvailable()) {
            try {
                return ClassLoadingStrategy.UsingLookup.of(
                        MethodHandles.privateLookupIn(persistentClass, MethodHandles.lookup()));
            } catch (IllegalAccessException e) {
                // The entity's module does not open its package: fall back to a child class loader
            }
        }
        return ClassLoadingStrategy.Default.WRAPPER;
    }

    private static final class EntityMetadata implements Serializable {

        private final Class<?> mappedClass;

        private final Set<Property> persistentProperties;

        EntityMetadata(Class<?> mappedClass, Set<Property> persistentProperties) {
            this.mappedClass = mappedClass;
            this.persistentProperties = persistentProperties;
        }
    }
}
