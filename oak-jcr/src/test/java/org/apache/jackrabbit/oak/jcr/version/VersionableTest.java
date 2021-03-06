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
package org.apache.jackrabbit.oak.jcr.version;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionManager;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;

/**
 * <code>VersionableTest</code> contains tests for method relevant to
 * versionable nodes.
 */
public class VersionableTest extends AbstractJCRTest {

    public void testGetTypeOfPredecessors() throws RepositoryException {
        Node node = testRootNode.addNode(nodeName1, testNodeType);
        node.addMixin(mixVersionable);
        superuser.save();
        VersionManager vMgr = superuser.getWorkspace().getVersionManager();
        vMgr.checkin(node.getPath());
        assertEquals(PropertyType.nameFromValue(PropertyType.REFERENCE),
                PropertyType.nameFromValue(node.getProperty(jcrPredecessors).getType()));
    }

    public void testReadOnlyAfterCheckin() throws RepositoryException {
        Node node = testRootNode.addNode(nodeName1, testNodeType);
        node.addMixin(mixVersionable);
        superuser.save();
        VersionManager vMgr = superuser.getWorkspace().getVersionManager();
        vMgr.checkin(node.getPath());
        try {
            node.setProperty(propertyName1, "value");
            fail("setProperty() must fail on a checked-in node");
        } catch (VersionException e) {
            // expected
        }
    }

    public void testReferenceableChild() throws RepositoryException {
        Node node = testRootNode.addNode(nodeName1, ntUnstructured);
        node.addMixin(mixVersionable);
        Node child = node.addNode(nodeName2, ntUnstructured);
        child.addMixin(mixReferenceable);
        superuser.save();
        VersionManager vMgr = superuser.getWorkspace().getVersionManager();
        vMgr.checkin(node.getPath());
    }

    /**
     * Test from Jackrabbit: JCR-3635 (OAK-940)
     *
     * Tests the case when a node already has a manual set
     * JcrConstants.JCR_FROZENUUID property and is versioned. The manual set
     * frozenUuid will overwrite the one that is automatically assigned by the
     * VersionManager, which should not happen
     */
    public void testCopyFrozenUuidProperty() throws Exception {
        Node firstNode = testRootNode.addNode(nodeName1);
        firstNode.setPrimaryType(JcrConstants.NT_UNSTRUCTURED);
        firstNode.addMixin(JcrConstants.MIX_VERSIONABLE);
        firstNode.getSession().save();

        // create version for the node
        Version firstNodeVersion = firstNode.checkin();
        firstNode.checkout();

        Node secondNode = testRootNode.addNode(nodeName2);
        secondNode.setPrimaryType(JcrConstants.NT_UNSTRUCTURED);
        secondNode.addMixin(JcrConstants.MIX_VERSIONABLE);
        Property firstNodeVersionFrozenUuid = firstNodeVersion.getFrozenNode().getProperty(JcrConstants.JCR_FROZENUUID);
        secondNode.setProperty(JcrConstants.JCR_FROZENUUID, firstNodeVersionFrozenUuid.getValue());
        secondNode.getSession().save();

        // create version of the second node
        Version secondNodeVersion = secondNode.checkin();
        secondNode.checkout();

        // frozenUuid from the second node version node should not be the same as the one from the first node version
        Property secondBodeVersionFrozenUuid = secondNodeVersion.getFrozenNode().getProperty(JcrConstants.JCR_FROZENUUID);
        assertFalse(JcrConstants.JCR_FROZENUUID + " should not be the same for two different versions of different nodes! ",
                secondBodeVersionFrozenUuid.getValue().equals(firstNodeVersionFrozenUuid.getValue()));
    }

    public void testCheckoutWithPendingChanges() throws Exception {
        Node node = testRootNode.addNode(nodeName1, testNodeType);
        node.addMixin(mixVersionable);
        superuser.save();
        node.checkin();
        Node newNode = testRootNode.addNode(nodeName2, testNodeType);
        assertTrue(newNode.isNew());
        node.checkout();
        assertTrue(node.isCheckedOut());
        assertTrue(newNode.isNew());
    }
}
