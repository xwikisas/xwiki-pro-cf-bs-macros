/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.xwiki.macros.cf.bs.internal.livedata;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.livedata.LiveDataEntryStore;
import org.xwiki.livedata.LiveDataPropertyDescriptorStore;
import org.xwiki.livedata.LiveDataSource;
import org.xwiki.livedata.WithParameters;

import com.fasterxml.jackson.databind.JsonNode;
import com.xwiki.macros.cf.bs.internal.JSONTableDataCache;

@Component
@Named(JSONTableLiveDataSource.ROLE_HINT)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class JSONTableLiveDataSource extends WithParameters implements LiveDataSource
{
    /**
     * The role hint that will be used for every jsonTable component.
     */
    public static final String ROLE_HINT = "jsonTable";

    @Inject
    @Named(ROLE_HINT)
    private LiveDataEntryStore entryStore;

    @Inject
    @Named(ROLE_HINT)
    private LiveDataPropertyDescriptorStore propertyStore;

    @Inject
    private JSONTableDataCache jsonTableDataCache;

    @Override
    public LiveDataEntryStore getEntries()
    {
        if (this.entryStore instanceof WithParameters) {
            ((WithParameters) this.entryStore).getParameters().put("node", getJsonNode());
            ((WithParameters) this.entryStore).getParameters().putAll(this.getParameters());
        }
        return this.entryStore;
    }

    @Override
    public LiveDataPropertyDescriptorStore getProperties()
    {
        if (this.propertyStore instanceof WithParameters) {
            ((WithParameters) this.entryStore).getParameters().put("node", getJsonNode());
            ((WithParameters) this.propertyStore).getParameters().putAll(this.getParameters());
        }
        return this.propertyStore;
    }

    private JsonNode getJsonNode()
    {
        return jsonTableDataCache.get((String) getParameters().get("cacheKey"));
    }
}
