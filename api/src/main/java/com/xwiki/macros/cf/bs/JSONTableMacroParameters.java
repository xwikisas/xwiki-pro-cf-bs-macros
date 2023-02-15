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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xwiki.properties.annotation.PropertyMandatory;
import org.xwiki.properties.annotation.PropertyName;

/**
 * Parameters for the JSON Table macro.
 *
 * @version $Id$
 * @since 1.0
 */
public class JSONTableMacroParameters
{
    /**
     * The paths parameter name.
     */
    public static final String PATHS = "paths";

    /**
     * The field paths parameter name.
     */
    public static final String FIELD_PATHS = "fieldPaths";

    /**
     * The URL parameter name.
     */
    public static final String URL = "url";

    /**
     * The capitalize parameter name.
     */
    public static final String CAPITALIZE = "capitalize";

    /**
     * The strip qualifiers parameter name.
     */
    public static final String STRIP_QUALIFIERS = "stripQualifiers";

    private static final String PATH_SPLIT_CHAR = ",";

    private List<String> paths = Collections.EMPTY_LIST;

    private List<String> fieldPaths = Collections.EMPTY_LIST;

    private URL url;

    private boolean stripQualifiers;

    private boolean capitalize = true;

    /**
     * @return a map of the parameters of this macro.
     */
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
     * @return the paths
     */
    public List<String> getPathsList()
    {
        return this.paths;
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

    /**
     * @param stripQualifiers whether the leading qualifiers should be stripped from generated headings
     */
    @PropertyName("Strip leading qualifiers from generated headings")
    public void setStripQualifiers(boolean stripQualifiers)
    {
        this.stripQualifiers = stripQualifiers;
    }

    /**
     * @return true if qualifiers should be stripped on generated headings
     */
    public boolean getStripQualifiers()
    {
        return this.stripQualifiers;
    }

    /**
     * @param capitalize whether the first character of generated headings should be capitalized.
     */
    @PropertyName("Capitalize the first characters of headings")
    public void setCapitalize(boolean capitalize)
    {
        this.capitalize = capitalize;
    }

    /**
     * @return true if the first character of headings should be capitalized.
     */
    public boolean getCapitalize()
    {
        return this.capitalize;
    }
}
