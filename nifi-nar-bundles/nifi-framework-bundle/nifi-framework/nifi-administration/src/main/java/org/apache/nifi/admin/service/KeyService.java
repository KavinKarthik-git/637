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
package org.apache.nifi.admin.service;

/**
 * Supports retrieving and issues keys for signing user tokens.
 */
public interface KeyService {

    /**
     * Gets a key for the specified user identity. Returns null if the user has not had a key issued
     *
     * @param identity The user identity
     * @return The key or null
     */
    String getKey(String identity);

    /**
     * Gets a key for the specified user identity. If a key does not exist, one will be created.
     *
     * @param identity The user identity
     * @return The key
     * @throws AdministrationException if it failed to get/create the key
     */
    String getOrCreateKey(String identity);
}
