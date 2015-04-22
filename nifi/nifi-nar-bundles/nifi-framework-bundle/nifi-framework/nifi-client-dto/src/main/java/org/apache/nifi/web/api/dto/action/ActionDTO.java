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
package org.apache.nifi.web.api.dto.action;

import java.util.Date;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.apache.nifi.web.api.dto.action.component.details.ComponentDetailsDTO;
import org.apache.nifi.web.api.dto.action.details.ActionDetailsDTO;
import org.apache.nifi.web.api.dto.util.DateTimeAdapter;

/**
 * An action performed in this NiFi.
 */
@XmlType(name = "action")
public class ActionDTO {

    private Integer id;
    private String userDn;
    private String userName;
    private Date timestamp;

    private String sourceId;
    private String sourceName;
    private String sourceType;
    private ComponentDetailsDTO componentDetails;

    private String operation;
    private ActionDetailsDTO actionDetails;

    /**
     * @return action id
     */
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * @return user dn who perform this action
     */
    public String getUserDn() {
        return userDn;
    }

    public void setUserDn(String userDn) {
        this.userDn = userDn;
    }

    /**
     * @return user name who perform this action
     */
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * @return action's timestamp
     */
    @XmlJavaTypeAdapter(DateTimeAdapter.class)
    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * @return id of the source component of this action
     */
    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    /**
     * @return name of the source component of this action
     */
    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    /**
     * @return type of the source component of this action
     */
    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    /**
     * @return component details (if any) for this action
     */
    public ComponentDetailsDTO getComponentDetails() {
        return componentDetails;
    }

    public void setComponentDetails(ComponentDetailsDTO componentDetails) {
        this.componentDetails = componentDetails;
    }

    /**
     * @return operation being performed in this action
     */
    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    /**
     * @return action details (if any) for this action
     */
    public ActionDetailsDTO getActionDetails() {
        return actionDetails;
    }

    public void setActionDetails(ActionDetailsDTO actionDetails) {
        this.actionDetails = actionDetails;
    }

}
