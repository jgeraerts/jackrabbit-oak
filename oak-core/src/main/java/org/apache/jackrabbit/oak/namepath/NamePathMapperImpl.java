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
package org.apache.jackrabbit.oak.namepath;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.oak.commons.PathUtils;
import org.apache.jackrabbit.oak.namepath.JcrPathParser.Listener;
import org.apache.jackrabbit.oak.core.IdentifierManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO document
 */
public class NamePathMapperImpl implements NamePathMapper {

    /**
     * logger instance
     */
    private static final Logger log = LoggerFactory.getLogger(NamePathMapperImpl.class);

    private final NameMapper nameMapper;
    private final IdentifierManager idManager;

    public NamePathMapperImpl(NameMapper nameMapper) {
        this.nameMapper = nameMapper;
        this.idManager = null;
    }

    public NamePathMapperImpl(NameMapper nameMapper, IdentifierManager idManager) {
        this.nameMapper = nameMapper;
        this.idManager = idManager;
    }

    //---------------------------------------------------------< NameMapper >---
    @Override
    public String getOakNameOrNull(String jcrName) {
        return nameMapper.getOakNameOrNull(jcrName);
    }

    @Nonnull
    @Override
    public String getOakName(@Nonnull String jcrName) throws RepositoryException {
        return nameMapper.getOakName(jcrName);
    }

    @Override
    public String getJcrName(String oakName) {
        return nameMapper.getJcrName(oakName);
    }

    @Override
    public boolean hasSessionLocalMappings() {
        return nameMapper.hasSessionLocalMappings();
    }

    //---------------------------------------------------------< PathMapper >---
    @Override
    public String getOakPath(String jcrPath) {
        return getOakPath(jcrPath, false);
    }

    @Override
    public String getOakPathKeepIndex(String jcrPath) {
        return getOakPath(jcrPath, true);
    }


    @Override
    @Nonnull
    public String getJcrPath(final String oakPath) {
        if ("/".equals(oakPath)) {
            // avoid the need to special case the root path later on
            return "/";
        }

        PathListener listener = new PathListener() {
            @Override
            public boolean current() {
                // nothing to do here
                return false;
            }

            @Override
            public void error(String message) {
                throw new IllegalArgumentException(message);
            }

            @Override
            public boolean name(String name, int index) {
                String p = nameMapper.getJcrName(name);
                if (index == 0) {
                    elements.add(p);
                } else {
                    elements.add(p + '[' + index + ']');
                }
                return true;
            }
        };

        JcrPathParser.parse(oakPath, listener);

        // empty path: map to "."
        if (listener.elements.isEmpty()) {
            return ".";
        }

        StringBuilder jcrPath = new StringBuilder();
        for (String element : listener.elements) {
            if (element.isEmpty()) {
                // root
                jcrPath.append('/');
            }
            else {
                jcrPath.append(element);
                jcrPath.append('/');
            }
        }

        jcrPath.deleteCharAt(jcrPath.length() - 1);
        return jcrPath.toString();
    }

    private String getOakPath(final String jcrPath, final boolean keepIndex) {
        if ("/".equals(jcrPath)) {
            // avoid the need to special case the root path later on
            return "/";
        }

        int length = jcrPath.length();

        // identifier path?
        if (length > 0 && jcrPath.charAt(0) == '[') {
            if (jcrPath.charAt(length - 1) != ']') {
                log.debug("Could not parse path " + jcrPath + ": unterminated identifier");
                return null;
            }
            if (this.idManager == null) {
                log.debug("Could not parse path " + jcrPath + ": could not resolve identifier");
                return null;
            }
            return this.idManager.getPath(jcrPath.substring(1, length - 1));
        }

        // Shortcut iff the JCR path does not start with a dot, does not contain any of
        // {}[]/ and if it contains a colon the session does not have local re-mappings.
        boolean hasLocalMappings = hasSessionLocalMappings();
        boolean shortcut = length > 0 && jcrPath.charAt(0) != '.';
        for (int i = 0; shortcut && i < length; i++) {
            char c = jcrPath.charAt(i);
            if (c == '{' || c == '}' || c == '[' || c == ']' || c == '/') {
                shortcut = false;
            } else if (c == ':') {
                shortcut = !hasLocalMappings;
            }
        }

        if (shortcut) {
            return jcrPath;
        }

        final StringBuilder parseErrors = new StringBuilder();

        PathListener listener = new PathListener() {
            @Override
            public void error(String message) {
                parseErrors.append(message);
            }

            @Override
            public boolean name(String name, int index) {
                if (!keepIndex && index > 1) {
                    error("index > 1");
                    return false;
                }
                String p = nameMapper.getOakNameOrNull(name);
                if (p == null) {
                    error("Invalid name: " + name);
                    return false;
                }
                if (keepIndex && index > 0) {
                    p += "[" + index + ']';
                }
                elements.add(p);
                return true;
            }
        };

        JcrPathParser.parse(jcrPath, listener);
        if (parseErrors.length() != 0) {
            log.debug("Could not parse path " + jcrPath + ": " + parseErrors.toString());
            return null;
        }

        // Empty path maps to ""
        if (listener.elements.isEmpty()) {
            return "";
        }

        StringBuilder oakPath = new StringBuilder();
        for (String element : listener.elements) {
            if (element.isEmpty()) {
                // root
                oakPath.append('/');
            }
            else {
                oakPath.append(element);
                oakPath.append('/');
            }
        }

        // root path is special-cased early on so it does not need to
        // be considered here
        oakPath.deleteCharAt(oakPath.length() - 1);
        return oakPath.toString();
    }

    //------------------------------------------------------------< PathListener >---

    private abstract static class PathListener implements Listener {
        final List<String> elements = new ArrayList<String>();

        @Override
        public boolean root() {
            if (!elements.isEmpty()) {
                error("/ on non-empty path");
                return false;
            }
            elements.add("");
            return true;
        }

        @Override
        public boolean current() {
            // nothing to do here
            return true;
        }

        @Override
        public boolean parent() {
            int prevIdx = elements.size() - 1;
            String prevElem = prevIdx >= 0 ? elements.get(prevIdx) : null;

            if (prevElem == null || PathUtils.denotesParent(prevElem)) {
                elements.add("..");
                return true;
            }
            if (prevElem.isEmpty()) {
                error("Absolute path escapes root");
                return false;
            }

            elements.remove(prevElem);
            return true;
        }
    }

}
