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

package org.apache.nifi.controller.state;

import org.apache.nifi.components.state.StateMap;
import org.junit.jupiter.api.Test;
import org.wali.UpdateType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestStateMapSerDe {

    @Test
    public void testCreateRoundTrip() throws IOException {
        final String componentId = "1234";

        final StateMapSerDe serde = new StateMapSerDe();
        final Map<String, String> stateValues = new HashMap<>();
        stateValues.put("abc", "xyz");
        stateValues.put("cba", "zyx");

        String version = "3";
        final StateMap stateMap = new StandardStateMap(stateValues, Optional.of(version));
        final StateMapUpdate record = new StateMapUpdate(stateMap, componentId, UpdateType.CREATE);

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (final DataOutputStream out = new DataOutputStream(baos)) {
            serde.serializeRecord(record, out);
        }

        final StateMapUpdate update;
        final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        try (final DataInputStream in = new DataInputStream(bais)) {
            update = serde.deserializeRecord(in, serde.getVersion());
        }

        assertNotNull(update);
        assertEquals(componentId, update.getComponentId());
        assertEquals(UpdateType.CREATE, update.getUpdateType());
        final StateMap recoveredStateMap = update.getStateMap();

        final Optional<String> stateVersion = recoveredStateMap.getStateVersion();
        assertTrue(stateVersion.isPresent());
        assertEquals(version, stateVersion.get());
        assertEquals(stateValues, recoveredStateMap.toMap());
    }
}
