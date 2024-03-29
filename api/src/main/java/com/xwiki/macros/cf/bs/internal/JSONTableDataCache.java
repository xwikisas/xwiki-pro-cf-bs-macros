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

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.eviction.LRUEvictionConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Cache used to store parsed JSON nodes for the {@link JSONTableMacro}.
 *
 * @version $Id$
 * @since 1.0
 */
@Component(roles = JSONTableDataCache.class)
@Singleton
public class JSONTableDataCache implements Initializable, Disposable
{
    private static final String NAME = "cache.jsontable";

    @Inject
    private CacheManager cacheManager;

    private Cache<JsonNode> cache;

    @Override
    public void initialize() throws InitializationException
    {
        this.cache = newCache();
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        if (this.cache != null) {
            this.cache.dispose();
        }
    }

    /**
     * @param key the cache key
     * @return the corresponding {@link JsonNode}. Returns null if the node does not exist.
     */
    public JsonNode get(String key)
    {
        return this.cache.get(key);
    }

    /**
     * @param key the cache key
     * @param value the {@link JsonNode} to store
     */
    public void set(String key, JsonNode value)
    {
        this.cache.set(key, value);
    }

    /**
     * @param key the key to remove
     */
    public void remove(String key)
    {
        this.cache.remove(key);
    }

    private Cache<JsonNode> newCache() throws InitializationException
    {
        try {
            CacheConfiguration cacheConfiguration = new CacheConfiguration();
            cacheConfiguration.setConfigurationId(NAME);
            LRUEvictionConfiguration lru = new LRUEvictionConfiguration();
            lru.setMaxEntries(1000);
            // Make sure that JSON entries get updated every 500 seconds by default
            lru.setLifespan(500);
            cacheConfiguration.put(LRUEvictionConfiguration.CONFIGURATIONID, lru);
            return this.cacheManager.createNewCache(cacheConfiguration);
        } catch (CacheException e) {
            throw new InitializationException("Failed to initialize JSON Table cache", e);
        }
    }
}
