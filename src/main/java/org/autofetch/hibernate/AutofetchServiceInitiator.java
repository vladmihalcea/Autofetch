/**
 * Copyright 2008 Ali Ibrahim
 * <p>
 * This file is part of Autofetch. Autofetch is free software: you can redistribute it and/or modify it under the terms of the Lesser GNU General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. Autofetch is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the Lesser GNU General Public License for more details. You should have received a copy of the Lesser GNU General Public
 * License along with Autofetch.  If not, see <http://www.gnu.org/licenses/>.
 * <p>
 * Hibernate 7 port (2026): typed configuration map.
 */
package org.autofetch.hibernate;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import java.util.Map;

final class AutofetchServiceInitiator implements StandardServiceInitiator<AutofetchService> {

    static final AutofetchServiceInitiator INSTANCE = new AutofetchServiceInitiator();

    @Override
    public AutofetchService initiateService(Map<String, Object> configurationValues, ServiceRegistryImplementor registry) {
        return new AutofetchServiceImpl();
    }

    @Override
    public Class<AutofetchService> getServiceInitiated() {
        return AutofetchService.class;
    }
}
