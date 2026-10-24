/**
 * Copyright 2008 Ali Ibrahim
 * <p>
 * This file is part of Autofetch. Autofetch is free software: you can redistribute it and/or modify it under the terms of the Lesser GNU General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. Autofetch is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the Lesser GNU General Public License for more details. You should have received a copy of the Lesser GNU General Public
 * License along with Autofetch.  If not, see <http://www.gnu.org/licenses/>.
 * <p>
 * Hibernate 7 port (2026): replaces AutofetchMetadataContributor. Hibernate 7 has no tuplizers, so instead of installing
 * AutofetchTuplizer this contributor registers every entity class with the EntityProxyFactory, whose tracking
 * subclasses AutofetchInterceptor instantiates. The collection types are replaced as before, except identifier
 * bags, which keep their standard type.
 */
package org.autofetch.hibernate;

import org.hibernate.boot.ResourceStreamLocator;
import org.hibernate.boot.spi.AdditionalMappingContributions;
import org.hibernate.boot.spi.AdditionalMappingContributor;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.PersistentClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class AutofetchMappingContributor implements AdditionalMappingContributor {

	private static final Logger log = LoggerFactory.getLogger( AutofetchMappingContributor.class );

	@Override
	public String getContributorName() {
		return "autofetch";
	}

	@Override
	public void contribute(
			AdditionalMappingContributions contributions,
			InFlightMetadataCollector metadataCollector,
			ResourceStreamLocator resourceStreamLocator,
			MetadataBuildingContext buildingContext) {
		EntityProxyFactory entityProxyFactory = buildingContext.getBootstrapContext()
				.getServiceRegistry()
				.requireService( AutofetchService.class )
				.getEntityProxyFactory();
		// Replace the collection types first: Property.getType() resolves and caches a collection's type
		for ( PersistentClass persistentClass : metadataCollector.getEntityBindingMap().values() ) {
			for ( org.hibernate.mapping.Property property : persistentClass.getProperties() ) {
				if ( property.getValue() instanceof org.hibernate.mapping.Collection ) {
					replaceCollection( property, persistentClass );
				}
			}
		}
		for ( PersistentClass persistentClass : metadataCollector.getEntityBindingMap().values() ) {
			Set<Property> persistentProperties = new HashSet<>();
			for ( org.hibernate.mapping.Property property : persistentClass.getPropertyClosure() ) {
				persistentProperties.add( new Property( property ) );
			}
			if ( persistentClass.getMappedClass() != null ) {
				entityProxyFactory.register(
						persistentClass.getEntityName(),
						persistentClass.getMappedClass(),
						persistentProperties
				);
			}
		}
	}

	private static void replaceCollection(org.hibernate.mapping.Property collectionProperty, PersistentClass owner) {
		org.hibernate.mapping.Collection value = (org.hibernate.mapping.Collection) collectionProperty.getValue();

		if ( value instanceof org.hibernate.mapping.Bag ) {
			value.setTypeName( AutofetchBagType.class.getName() );
		}
		else if ( value instanceof org.hibernate.mapping.IdentifierBag ) {
			log.warn( "Identifier bags are not tracked by the Hibernate 7 port: {}.{}",
					owner.getEntityName(), collectionProperty.getName() );
		}
		else if ( value instanceof org.hibernate.mapping.List ) {
			value.setTypeName( AutofetchListType.class.getName() );
		}
		else if ( value instanceof org.hibernate.mapping.Set ) {
			value.setTypeName( AutofetchSetType.class.getName() );
		}
		else {
			throw new UnsupportedOperationException( "Collection type not supported: " + value.getClass() );
		}
	}
}
