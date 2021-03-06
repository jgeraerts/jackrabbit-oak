/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.oak.plugins.index.solr.embedded;

import org.apache.jackrabbit.oak.spi.state.NodeState;
import org.apache.jackrabbit.oak.spi.state.NodeStore;

/**
 * A {@link OakSolrNodeStateConfiguration} whose {@link org.apache.jackrabbit.oak.spi.state.NodeState} is retrieved
 * via the {@link org.apache.jackrabbit.oak.spi.state.NodeStore} and a given <code>String</code> path.
 */
public class UpToDateNodeStateConfiguration extends OakSolrNodeStateConfiguration {

    private final NodeStore store;
    private final String path;

    public UpToDateNodeStateConfiguration(NodeStore store, String path) {
        this.store = store;
        this.path = path;
    }

    @Override
    protected NodeState getConfigurationNodeState() {
        NodeState currentState = store.getRoot();
        for (String child : path.split("/")) {
            currentState = currentState.getChildNode(child);
        }
        return currentState;
    }
}
