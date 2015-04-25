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
package org.apache.nifi.web.api.dto;

import java.util.Date;
import java.util.List;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.apache.nifi.web.api.dto.util.DateTimeAdapter;

/**
 * Details of a node within this NiFi.
 */
@XmlType(name = "node")
public class NodeDTO {

    private String nodeId;
    private String address;
    private Integer apiPort;
    private String status;
    private Date heartbeat;
    private Date connectionRequested;
    private Boolean primary;
    private Integer activeThreadCount;
    private String queued;
    private List<NodeEventDTO> events;
    private Date nodeStartTime;

    /**
     * @return node's last heartbeat timestamp
     */
    @XmlJavaTypeAdapter(DateTimeAdapter.class)
    public Date getHeartbeat() {
        return heartbeat;
    }

    public void setHeartbeat(Date heartbeat) {
        this.heartbeat = heartbeat;
    }

    /**
     * @return time of the node's last connection request
     */
    @XmlJavaTypeAdapter(DateTimeAdapter.class)
    public Date getConnectionRequested() {
        return connectionRequested;
    }

    public void setConnectionRequested(Date connectionRequested) {
        this.connectionRequested = connectionRequested;
    }

    /**
     * The active thread count.
     *
     * @return The active thread count
     */
    public Integer getActiveThreadCount() {
        return activeThreadCount;
    }

    public void setActiveThreadCount(Integer activeThreadCount) {
        this.activeThreadCount = activeThreadCount;
    }

    /**
     * @return queue for the controller
     */
    public String getQueued() {
        return queued;
    }

    public void setQueued(String queued) {
        this.queued = queued;
    }

    /**
     * @return node's host/IP address
     */
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * @return node ID
     */
    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    /**
     * @return port the node is listening for API requests
     */
    public Integer getApiPort() {
        return apiPort;
    }

    public void setApiPort(Integer port) {
        this.apiPort = port;
    }

    /**
     * @return node's status
     */
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * @return node's events
     */
    public List<NodeEventDTO> getEvents() {
        return events;
    }

    public void setEvents(List<NodeEventDTO> events) {
        this.events = events;
    }

    /**
     * @return whether this node is the primary node within the cluster
     */
    public Boolean isPrimary() {
        return primary;
    }

    public void setPrimary(Boolean primary) {
        this.primary = primary;
    }

    /**
     * @return time at which this Node was last restarted
     */
    @XmlJavaTypeAdapter(DateTimeAdapter.class)
    public Date getNodeStartTime() {
        return nodeStartTime;
    }

    public void setNodeStartTime(Date nodeStartTime) {
        this.nodeStartTime = nodeStartTime;
    }
}
