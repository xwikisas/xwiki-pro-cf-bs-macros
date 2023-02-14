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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.codec.digest.DigestUtils;
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
            Pair<String, JsonNode> jsonNodePair = getJsonNode(parameters, content);
            return Collections.singletonList(new MacroBlock("liveData", Collections.emptyMap(),
                buildLiveDataParameters(parameters, jsonNodePair.getKey()), false));
        } catch (JsonProcessingException e) {
            throw new MacroExecutionException("Failed to build parameters for the LiveData macro", e);
        }
    }

    @Override
    public boolean supportsInlineMode()
    {
        return true;
    }

    private String buildLiveDataParameters(JSONTableMacroParameters parameters, String cacheKey)
        throws JsonProcessingException
    {
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
                        put("properties", parameters.getFieldPathsList());
                        put("source", new HashMap<String, Object>() {{
                                put(ID, JSONTableLiveDataSource.ROLE_HINT);
                                putAll(parameters.getParameterMap());
                                put("cacheKey", cacheKey);
                            }});
                    }});
                put("meta", new HashMap<String, Object>() {{
                        put("propertyDescriptors", getPropertyDescriptors(parameters));
                        put("propertyTypes", propertyTypes);
                    }});
            }};

        return new ObjectMapper().writeValueAsString(result);
    }

    private List<Map<String, Object>> getPropertyDescriptors(JSONTableMacroParameters parameters)
    {
        List<Map<String, Object>> propertyDescriptors = new ArrayList<>();
        for (String fieldPath : parameters.getFieldPathsList()) {
            propertyDescriptors.add(new HashMap<String, Object>() {{
                    put(ID, fieldPath);
                    put("name", fieldPath);
                    put("type", STRING);
                    put(EDITABLE, false);
                    put(SORTABLE, true);
                    put(FILTERABLE, true);
                }});
        }
        return propertyDescriptors;
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
