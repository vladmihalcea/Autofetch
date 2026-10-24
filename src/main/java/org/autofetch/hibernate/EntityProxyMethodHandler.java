/**
 * Copyright 2008 Ali Ibrahim
 * <p>
 * This file is part of Autofetch. Autofetch is free software: you can redistribute it and/or modify it under the terms of the Lesser GNU General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. Autofetch is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the Lesser GNU General Public License for more details. You should have received a copy of the Lesser GNU General Public
 * License along with Autofetch.  If not, see <http://www.gnu.org/licenses/>.
 * <p>
 * Hibernate 7 port (2026): a ByteBuddy delegation target instead of a Javassist MethodHandler; the dispatch is unchanged.
 */
package org.autofetch.hibernate;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.Callable;

public class EntityProxyMethodHandler implements Serializable {

	private static final long serialVersionUID = 1L;

	private final EntityTracker entityTracker;

	EntityProxyMethodHandler(
			Set<Property> persistentProperties,
			ExtentManager extentManager) {
		this.entityTracker = new EntityTracker( persistentProperties, extentManager );
	}

	@RuntimeType
	public Object intercept(
			@This Object obj,
			@Origin Method thisMethod,
			@AllArguments Object[] args,
			@SuperCall(nullIfImpossible = true) Callable<?> proceed) throws Exception {
		if ( args.length == 0 ) {
			if ( thisMethod.getName().equals( "disableTracking" ) ) {
				boolean oldValue = entityTracker.isTracking();
				entityTracker.setTracking( false );
				return oldValue;
			}
			else if ( thisMethod.getName().equals( "enableTracking" ) ) {
				boolean oldValue = entityTracker.isTracking();
				entityTracker.setTracking( true );
				return oldValue;
			}
			else if ( thisMethod.getName().equals( "isAccessed" ) ) {
				return entityTracker.isAccessed();
			}
		}
		else if ( args.length == 1 ) {
			if ( thisMethod.getName().equals( "addTracker" ) && thisMethod.getParameterTypes()[0].equals( Statistics.class ) ) {
				entityTracker.addTracker( (Statistics) args[0] );
				return null;
			}
			else if ( thisMethod.getName().equals( "addTrackers" ) && thisMethod.getParameterTypes()[0].equals( Set.class ) ) {
				@SuppressWarnings("unchecked")
				Set<Statistics> newTrackers = (Set<Statistics>) args[0];
				entityTracker.addTrackers( newTrackers );
				return null;
			}
			else if ( thisMethod.getName().equals( "extendProfile" ) && thisMethod.getParameterTypes()[0].equals( Statistics.class ) ) {
				entityTracker.extendProfile( (Statistics) args[0], obj );
				return null;
			}
			else if ( thisMethod.getName().equals( "removeTracker" ) && thisMethod.getParameterTypes()[0].equals( Statistics.class ) ) {
				entityTracker.removeTracker( (Statistics) args[0] );
				return null;
			}
		}
		if ( proceed == null ) {
			throw new AbstractMethodError( thisMethod.toString() );
		}
		entityTracker.trackAccess( obj );
		return proceed.call();
	}
}
