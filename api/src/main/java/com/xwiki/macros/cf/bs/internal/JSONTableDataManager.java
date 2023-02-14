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
package com.xwiki.macros.cf.bs.internal;

import java.util.Enumeration;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Helper for the {@link JSONTableMacro}.
 *
 * @version $Id$
 * @since 1.0
 */
@Component(roles = JSONTableDataManager.class)
@Singleton
public class JSONTableDataManager
{
    /**
     * Apply a given JSONPath to the given node and retuns the matching nodes.
     *
     * @param path the JSON Path
     * @param node the node to filter on
     * @return the matching nodes
     */
    public Enumeration<JsonNode> applyPath(String path, JsonNode node)
    {
        scala.collection.Iterator<JsonNode> res =
            io.gatling.jsonpath.JsonPath$.MODULE$.query(path, node).right().get();
        return scala.collection.JavaConverters$.MODULE$.asJavaEnumeration(res);
    }
}
