/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.oak.plugins.mongomk;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.annotation.Nonnull;

import org.apache.jackrabbit.oak.plugins.mongomk.util.MergeSortedIterators;

import com.google.common.collect.Iterators;

/**
 * A value map contains the versioned values of a property. The key into this
 * map is the revision when the value was set.
 */
class ValueMap {

    static SortedMap<Revision, String> EMPTY = Collections.unmodifiableSortedMap(
            new TreeMap<Revision, String>(new StableRevisionComparator()));

    @Nonnull
    static Map<Revision, String> create(final @Nonnull NodeDocument doc,
                                        final @Nonnull String property) {
        final SortedMap<Revision, String> map = doc.getLocalMap(property);
        if (doc.getPreviousRanges().isEmpty()) {
            return map;
        }
        final Comparator<? super Revision> c = map.comparator();
        final Iterator<NodeDocument> docs = Iterators.concat(
                Iterators.singletonIterator(doc),
                doc.getPreviousDocs(null, property).iterator());
        final Set<Map.Entry<Revision, String>> entrySet
                = new AbstractSet<Map.Entry<Revision, String>>() {

            @Override
            @Nonnull
            public Iterator<Map.Entry<Revision, String>> iterator() {
                return new MergeSortedIterators<Map.Entry<Revision, String>>(
                        new Comparator<Map.Entry<Revision, String>>() {
                            @Override
                            public int compare(Map.Entry<Revision, String> o1,
                                               Map.Entry<Revision, String> o2) {
                                return c.compare(o1.getKey(), o2.getKey());
                            }
                        }
                ) {
                    @Override
                    public Iterator<Map.Entry<Revision, String>> nextIterator() {
                        return docs.hasNext() ? docs.next().getLocalMap(property).entrySet().iterator() : null;
                    }
                };
            }

            @Override
            public int size() {
                int size = map.size();
                for (NodeDocument prev : doc.getPreviousDocs(null, property)) {
                    size += prev.getLocalMap(property).size();
                }
                return size;
            }
        };

        return new AbstractMap<Revision, String>() {

            private final Map<Revision, String> map = doc.getLocalMap(property);

            @Override
            @Nonnull
            public Set<Entry<Revision, String>> entrySet() {
                return entrySet;
            }

            @Override
            public String get(Object key) {
                // first check values map of this document
                String value = map.get(key);
                if (value != null) {
                    return value;
                }
                Revision r = (Revision) key;
                for (NodeDocument prev : doc.getPreviousDocs(r, property)) {
                    value = prev.getLocalMap(property).get(key);
                    if (value != null) {
                        return value;
                    }
                }
                // not found
                return null;
            }

            @Override
            public boolean containsKey(Object key) {
                // can use get()
                // the values map does not have null values
                return get(key) != null;
            }
        };
    }
}
