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
package com.xwiki.macros.cf.bs;

import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xwiki.properties.annotation.PropertyMandatory;
import org.xwiki.properties.annotation.PropertyName;

public class JSONTableMacroParameters
{
    private static final String PATH_SPLIT_CHAR = ",";

    public static final String PATHS = "paths";

    public static final String FIELD_PATHS = "fieldPaths";

    public static final String URL = "url";

    private List<String> paths;

    private List<String> fieldPaths;

    private URL url;

    public Map<String, Object> getParameterMap()
    {
        return new HashMap<String, Object>() {{
            put(PATHS, paths);
            put(FIELD_PATHS, fieldPaths);
            put(URL, url);
        }};
    }

    /**
     * @param paths the JSON paths
     */
    @PropertyMandatory
    @PropertyName("Comma-separated list of JSONPaths to fields")
    public void setPaths(String paths)
    {
        this.paths = Arrays.asList(paths.split(PATH_SPLIT_CHAR));
    }

    /**
     * @return the paths separated by commas
     */
    public String getPaths()
    {
        return String.join(PATH_SPLIT_CHAR, this.paths);
    }

    /**
     * @param fieldPaths the field paths
     */
    @PropertyName("Paths to fields to be included")
    public void setFieldPaths(String fieldPaths)
    {
        this.fieldPaths = Arrays.asList(fieldPaths.split(PATH_SPLIT_CHAR));
    }

    /**
     * @return the field paths separated by commas
     */
    public String getFieldPaths()
    {
        return String.join(PATH_SPLIT_CHAR, this.fieldPaths);
    }

    /**
     * @return the field paths
     */
    public List<String> getFieldPathsList()
    {
        return this.fieldPaths;
    }

    /**
     * @param url the URL to the JSON data
     */
    @PropertyName("URL to the JSON data")
    public void setUrl(URL url)
    {
        this.url = url;
    }

    /**
     * @return the URL to the JSON data
     */
    public URL getUrl()
    {
        return this.url;
    }
}
