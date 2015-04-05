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
package org.apache.nifi.controller.service;

import java.util.Set;

import org.apache.nifi.controller.ConfiguredComponent;
import org.apache.nifi.controller.ControllerService;

public interface ControllerServiceNode extends ConfiguredComponent {

    ControllerService getProxiedControllerService();
    
    ControllerService getControllerServiceImplementation();

    ControllerServiceState getState();
    void setState(ControllerServiceState state);
    
    ControllerServiceReference getReferences();

    void addReference(ConfiguredComponent referringComponent);

    void removeReference(ConfiguredComponent referringComponent);
    
    void setComments(String comment);
    String getComments();
    
    void verifyCanEnable();
    void verifyCanDisable();
    
    /**
     * Verifies that this Controller Service can be disabled if the provided set of 
     * services are also disabled. This is introduced because we can have an instance
     * where A references B, which references C, which references A and we want
     * to disable service C. In this case, the cycle needs to not cause us to fail, 
     * so we want to verify that C can be disabled if A and B also are. 
     * 
     * @param ignoredReferences
     */
    void verifyCanDisable(Set<ControllerServiceNode> ignoredReferences);
    
    /**
     * Verifies that this Controller Service can be enabled if the provided set of
     * services are also enabled. This is introduced because we can have an instance where
     * A reference B, which references C, which references A and we want to enable
     * Service A. In this case, the cycle needs to not cause us to fail, so we want to verify
     * that A can be enabled if A and B also are.
     * @param ignoredReferences
     */
    void verifyCanEnable(Set<ControllerServiceNode> ignoredReferences);
    
    void verifyCanDelete();
    void verifyCanUpdate();
}
