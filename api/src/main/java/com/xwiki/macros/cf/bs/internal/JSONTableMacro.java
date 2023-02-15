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

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.transformation.MacroTransformationContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.xwiki.macros.cf.bs.JSONTableMacroParameters;
import com.xwiki.macros.cf.bs.internal.livedata.JSONTableLiveDataSource;

/**
 * Allows to insert dynamic tables from a JSON source.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Singleton
@Named("json-table")
public class JSONTableMacro extends AbstractMacro<JSONTableMacroParameters>
{
    private static final String ID = "id";

    private static final String STRING = "String";

    private static final String SORTABLE = "sortable";

    private static final String EDITABLE = "editable";

    private static final String FILTERABLE = "filterable";

    @Inject
    private JSONTableDataCache jsonTableDataCache;

    @Inject
    private JSONTableDataHelper jsonTableDataHelper;

    private ObjectMapper objectMapper;

    /**
     * Create a new {@link JSONTableMacro}.
     */
    public JSONTableMacro()
    {
        super("JSON Table", "Adds a JSON Table", JSONTableMacroParameters.class);
    }

    @Override
    public void initialize() throws InitializationException
    {
        this.objectMapper = new ObjectMapper();
        super.initialize();
    }

    @Override
    public List<Block> execute(JSONTableMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        try {
            // The action of building live data parameters actually requires a lot of heavy lifting to make sure that
            // the livedata that will be generated doesn't end up being corrupted. We'll especially need to load and
            // parse the JSON to display in order to  extract some information.
            // TODO : The "paths" parameter is supposed to generate one live data table per path. Currently, we only
            //  consider the first parameter and ignore the others.
            Pair<String, JsonNode> jsonNodePair = getJsonNode(parameters, content);
            return Collections.singletonList(new MacroBlock("liveData", Collections.emptyMap(),
                buildLiveDataParameters(parameters, jsonNodePair), false));
        } catch (JsonProcessingException e) {
            throw new MacroExecutionException("Failed to build parameters for the LiveData macro", e);
        }
    }

    @Override
    public boolean supportsInlineMode()
    {
        return true;
    }

    private String buildLiveDataParameters(JSONTableMacroParameters parameters, Pair<String, JsonNode> jsonNodePair)
        throws JsonProcessingException
    {
        List<String> fieldPaths = getFieldPaths(parameters, jsonNodePair.getValue());
        List<Map<String, Object>> propertyTypes = new ArrayList<>();
        propertyTypes.add(new HashMap<String, Object>() {{
                put(ID, STRING);
                put(SORTABLE, true);
                put(FILTERABLE, true);
                put(EDITABLE, true);
                put("filter", "text");
            }});

        Map<String, Object> result = new HashMap<String, Object>() {{
                put("query", new HashMap<String, Object>() {{
                        put("properties", fieldPaths);
                        put("source", new HashMap<String, Object>() {{
                                putAll(parameters.getParameterMap());
                                put(ID, JSONTableLiveDataSource.ROLE_HINT);
                                put("cacheKey", jsonNodePair.getKey());
                                // We are actually overwriting the value passed initially as part of
                                // JSONTableMacroParameters#getParametersMap() to include any detected field path
                                put("fieldPaths", fieldPaths);
                                put("path", parameters.getPathsList().get(0));
                            }});
                    }});
                put("meta", new HashMap<String, Object>() {{
                        put("propertyDescriptors", getPropertyDescriptors(parameters, fieldPaths));
                        put("propertyTypes", propertyTypes);
                    }});
            }};

        return new ObjectMapper().writeValueAsString(result);
    }

    private List<String> getFieldPaths(JSONTableMacroParameters parameters, JsonNode nodes)
    {
        if (!parameters.getFieldPathsList().isEmpty()) {
            return orderFieldPaths(parameters, parameters.getFieldPathsList());
        } else {
            // When field paths are not defined as parameters, we need to "guess" them from the JSONNodes that we
            // get, while considering that field paths can be ordered by the parameter fieldOrderRegexPatterns.
            // For now, the strategy is to use the fields available in the first entry that we get when applying the
            // JSON path given as a parameter of the macro. This is not ideal as the first entry returned may miss
            // some parameters, that are simply not defined. However, this will allow us to avoid having to iterate
            // through the whole macro.
            Enumeration<JsonNode> results = jsonTableDataHelper.applyPath(parameters.getPathsList().get(0), nodes);
            if (results.hasMoreElements()) {
                List<String> fieldPaths = new ArrayList<>();

                JsonNode node = results.nextElement();
                for (Iterator<String> it = node.fieldNames(); it.hasNext();) {
                    String field = it.next();
                    if (node.get(field) instanceof ValueNode) {
                        fieldPaths.add(field);
                    }
                }

                return orderFieldPaths(parameters, fieldPaths);
            } else {
                return Collections.emptyList();
            }

        }
    }

    private List<String> orderFieldPaths(JSONTableMacroParameters parameters, List<String> fieldPaths)
    {
        // In case the field order regex patterns are defined, then we'll need to apply them in order to sort
        // the computed list of field paths.
        if (!parameters.getFieldOrderRegexPatternsList().isEmpty()) {
            List<String> orderedFieldPaths = new ArrayList<>();

            for (String regexPattern : parameters.getFieldOrderRegexPatternsList()) {
                for (String fieldPath : fieldPaths) {
                    if (!orderedFieldPaths.contains(fieldPath) && fieldPath.matches(regexPattern)) {
                        orderedFieldPaths.add(fieldPath);
                    }
                }
            }

            // Add the remaining field paths that were not detected to the ordered field paths
            for (String fieldPath : fieldPaths) {
                if (!orderedFieldPaths.contains(fieldPath)) {
                    orderedFieldPaths.add(fieldPath);
                }
            }

            return orderedFieldPaths;
        } else {
            return fieldPaths;
        }
    }

    private List<Map<String, Object>> getPropertyDescriptors(JSONTableMacroParameters parameters,
        List<String> fieldPaths)
    {
        List<Map<String, Object>> propertyDescriptors = new ArrayList<>();
        for (String fieldPath : fieldPaths) {
            propertyDescriptors.add(new HashMap<String, Object>() {{
                    put(ID, fieldPath);
                    put("name", getPropertyName(fieldPath, parameters));
                    put("type", STRING);
                    put(EDITABLE, false);
                    put(SORTABLE, true);
                    put(FILTERABLE, true);
                }});
        }
        return propertyDescriptors;
    }

    private String getPropertyName(String fieldPath, JSONTableMacroParameters parameters)
    {
        String result = fieldPath;
        if (parameters.getStripQualifiers()) {
            String[] qualifiers = result.split("\\.");
            result = qualifiers[qualifiers.length - 1];
        }
        
        return (parameters.getCapitalize()) ? StringUtils.capitalize(result) : result;
    }

    private Pair<String, JsonNode> getJsonNode(JSONTableMacroParameters parameters, String content)
        throws MacroExecutionException
    {
        JsonNode result;
        String key;

        if (parameters.getUrl() != null) {
            key = DigestUtils.sha256Hex(parameters.getUrl().toString());
            result = jsonTableDataCache.get(key);

            if (result == null) {
                result = getJsonNodeFromURL(parameters.getUrl());
                jsonTableDataCache.set(key, result);
            }
        } else {
            key = DigestUtils.sha256Hex(content);
            result = jsonTableDataCache.get(key);

            if (result == null) {
                result = getJsonNodeFromContent(content);
                jsonTableDataCache.set(key, result);
            }
        }

        return new ImmutablePair<>(key, result);
    }

    private JsonNode getJsonNodeFromContent(String content) throws MacroExecutionException
    {
        try {
            return objectMapper.readTree(content);
        } catch (JsonProcessingException e) {
            throw new MacroExecutionException(String.format("Failed to parse JSON content [%s]", content));
        }
    }

    private JsonNode getJsonNodeFromURL(URL url) throws MacroExecutionException
    {
        HttpClientBuilder builder = HttpClientBuilder.create();
        builder.useSystemProperties();

        try (CloseableHttpClient httpClient = builder.build()) {
            CloseableHttpResponse response = httpClient.execute(new HttpGet(url.toURI()));

            if (response.getStatusLine().getStatusCode() >= 200 && response.getStatusLine().getStatusCode() < 300) {
                JsonNode node = objectMapper.readTree(response.getEntity().getContent());
                response.close();
                return node;
            } else {
                throw new MacroExecutionException(
                    String.format("Got invalid HTTP response when fetching source contents for JSON Table [%s]", url));
            }
        } catch (IOException | URISyntaxException e) {
            throw new MacroExecutionException(
                String.format("Failed to get source contents for JSON Table [%s]", url));
        }
    }
}
