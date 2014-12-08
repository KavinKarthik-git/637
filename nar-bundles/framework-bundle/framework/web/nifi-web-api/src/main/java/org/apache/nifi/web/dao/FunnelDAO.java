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
package org.apache.nifi.web.dao;

import java.util.Set;

import org.apache.nifi.connectable.Funnel;
import org.apache.nifi.web.api.dto.FunnelDTO;

/**
 *
 */
public interface FunnelDAO {

    /**
     * Determines if the specified funnel exists in the specified group.
     *
     * @param groupId
     * @param funnelId
     * @return
     */
    boolean hasFunnel(String groupId, String funnelId);

    /**
     * Creates a funnel in the specified group.
     *
     * @param groupId
     * @param funnelDTO The funnel DTO
     * @return The funnel
     */
    Funnel createFunnel(String groupId, FunnelDTO funnelDTO);

    /**
     * Gets the specified funnel in the specified group.
     *
     * @param groupId
     * @param funnelId The funnel id
     * @return The funnel
     */
    Funnel getFunnel(String groupId, String funnelId);

    /**
     * Gets all of the funnels in the specified group.
     *
     * @param groupId
     * @return The funnels
     */
    Set<Funnel> getFunnels(String groupId);

    /**
     * Updates the specified funnel in the specified group.
     *
     * @param groupId
     * @param funnelDTO The funnel DTO
     * @return The funnel
     */
    Funnel updateFunnel(String groupId, FunnelDTO funnelDTO);

    /**
     * Determines whether this funnel can be removed.
     *
     * @param groupId
     * @param funnelId
     */
    void verifyDelete(String groupId, String funnelId);

    /**
     * Deletes the specified Funnel in the specified group.
     *
     * @param groupId
     * @param funnelId The funnel id
     */
    void deleteFunnel(String groupId, String funnelId);
}
