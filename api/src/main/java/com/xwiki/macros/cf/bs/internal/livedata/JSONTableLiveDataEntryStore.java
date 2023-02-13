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
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.compare.ComparableUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.livedata.LiveData;
import org.xwiki.livedata.LiveDataEntryStore;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.LiveDataQuery;

import org.xwiki.livedata.WithParameters;
import org.xwiki.text.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.xwiki.macros.cf.bs.internal.JSONTableDataManager;

@Component
@Named(JSONTableLiveDataSource.ROLE_HINT)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class JSONTableLiveDataEntryStore extends WithParameters implements LiveDataEntryStore
{
    @Inject
    private JSONTableDataManager manager;

    @Override
    public Optional<Map<String, Object>> get(Object entryId) throws LiveDataException
    {
        return Optional.empty();
    }

    @Override
    public LiveData get(LiveDataQuery query) throws LiveDataException
    {
        List<String> paths = (List<String>) this.getParameters().get("paths");
        List<String> fieldPaths = (List<String>) this.getParameters().get("fieldPaths");

        LiveData liveData = new LiveData();

        // For now, we only consider the first path in the list of paths
        List<JsonNode> nodes = Collections.list(
            manager.applyPath(paths.get(0), (JsonNode) this.getParameters().get("node")));
        liveData.setCount(nodes.size());

        // TODO : calculate offset
        // TODO : apply filters
        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> entries = new ArrayList<>();
        for (JsonNode node : nodes) {
            boolean matchesFilters = true;

            for (LiveDataQuery.Filter filter : query.getFilters()) {
                String property = filter.getProperty();

                if (node.get(property) instanceof TextNode) {
                    String nodeValue = node.get(property).textValue();

                    for (LiveDataQuery.Constraint constraint : filter.getConstraints()) {
                        if (constraint.getOperator().equals("contains")) {
                            matchesFilters &= nodeValue.contains((String) constraint.getValue());
                        } else if (constraint.getOperator().equals("equals")) {
                            matchesFilters &= nodeValue.equals(constraint.getValue());
                        } else if (constraint.getOperator().equals("startsWith")) {
                            matchesFilters &= nodeValue.startsWith((String) constraint.getValue());
                        }
                    }
                }

                if (!matchesFilters) {
                    break;
                }
            }

            if (matchesFilters) {
                entries.add(mapper.convertValue(node, new TypeReference<Map<String, Object>>(){}));
            }
        }

        // TODO : Sort
        /*for (LiveDataQuery.SortEntry sortEntry : query.getSort()) {
            entries.sort();
        }
        entries.stream().sorted()
        liveData.getEntries()*/

        liveData.getEntries().addAll(entries);
        return liveData;
    }
}
