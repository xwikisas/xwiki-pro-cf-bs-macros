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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.livedata.LiveData;
import org.xwiki.livedata.LiveDataEntryStore;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.LiveDataQuery;

import org.xwiki.livedata.WithParameters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xwiki.macros.cf.bs.internal.JSONTableDataHelper;

/**
 * Live data entry store for the {@link JSONTableLiveDataSource}.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Named(JSONTableLiveDataSource.ROLE_HINT)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class JSONTableLiveDataEntryStore extends WithParameters implements LiveDataEntryStore
{
    @Inject
    private JSONTableDataHelper jsonTableDataHelper;

    @Override
    public Optional<Map<String, Object>> get(Object entryId) throws LiveDataException
    {
        return Optional.empty();
    }

    @Override
    public LiveData get(LiveDataQuery query) throws LiveDataException
    {
        String path = (String) this.getParameters().get("path");
        List<String> fieldPaths = (List<String>) this.getParameters().get("fieldPaths");

        LiveData liveData = new LiveData();

        // For now, we only consider the first path in the list of paths
        List<JsonNode> nodes = Collections.list(
            jsonTableDataHelper.applyPath(path, (JsonNode) this.getParameters().get("node")));
        liveData.setCount(nodes.size());

        // TODO : calculate offset
        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> entries = new ArrayList<>();
        for (JsonNode rootNode : nodes) {
            Map<String, Object> entry = new HashMap<>();

            // Start by resolving the field paths
            // TODO: This operation can be very expensive (especially because it's done multiple times), we should
            //  probably look at a way to cache the result
            for (String fieldPath : fieldPaths) {
                Enumeration<JsonNode> matchingNodes = jsonTableDataHelper.applyPath(fieldPath, rootNode);
                if (matchingNodes.hasMoreElements()) {
                    entry.put(fieldPath, mapper.convertValue(matchingNodes.nextElement(), String.class));
                }
            }

            if (matchesFilters(query.getFilters(), entry)) {
                entries.add(entry);
            }
        }

        /*
        for (LiveDataQuery.SortEntry sortEntry : query.getSort()) {
            entries.sort();
        }*/

        liveData.getEntries().addAll(entries);
        return liveData;
    }

    private boolean matchesFilters(List<LiveDataQuery.Filter> filters, Map<String, Object> entry)
    {
        boolean matchesFilters = true;

        for (LiveDataQuery.Filter filter : filters) {
            String nodeValue = (String) entry.get(filter.getProperty());

            for (LiveDataQuery.Constraint constraint : filter.getConstraints()) {
                if (constraint.getOperator().equals("contains")) {
                    matchesFilters &= nodeValue.contains((String) constraint.getValue());
                } else if (constraint.getOperator().equals("equals")) {
                    matchesFilters &= nodeValue.equals(constraint.getValue());
                } else if (constraint.getOperator().equals("startsWith")) {
                    matchesFilters &= nodeValue.startsWith((String) constraint.getValue());
                }
            }

            if (!matchesFilters) {
                break;
            }
        }

        return matchesFilters;
    }
}
