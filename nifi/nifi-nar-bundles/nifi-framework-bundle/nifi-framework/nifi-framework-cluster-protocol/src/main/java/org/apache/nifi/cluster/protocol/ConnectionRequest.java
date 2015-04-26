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
package org.apache.nifi.cluster.protocol;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.apache.nifi.cluster.protocol.jaxb.message.ConnectionRequestAdapter;

/**
 * A node's request to connect to the cluster. The request contains a proposed
 * identifier.
 *
 * @author unattributed
 */
@XmlJavaTypeAdapter(ConnectionRequestAdapter.class)
public class ConnectionRequest {

    private final NodeIdentifier proposedNodeIdentifier;

    public ConnectionRequest(final NodeIdentifier proposedNodeIdentifier) {
        if (proposedNodeIdentifier == null) {
            throw new IllegalArgumentException("Proposed node identifier may not be null.");
        }
        this.proposedNodeIdentifier = proposedNodeIdentifier;
    }

    public NodeIdentifier getProposedNodeIdentifier() {
        return proposedNodeIdentifier;
    }

}
