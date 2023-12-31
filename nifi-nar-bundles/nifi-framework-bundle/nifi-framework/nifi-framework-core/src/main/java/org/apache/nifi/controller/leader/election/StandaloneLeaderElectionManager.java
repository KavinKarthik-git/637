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

package org.apache.nifi.controller.leader.election;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * A LeaderElectionManager to use when running a standalone (un-clustered) NiFi instance
 * </p>
 */
public class StandaloneLeaderElectionManager implements LeaderElectionManager {

    @Override
    public void start() {
    }

    @Override
    public void register(final String roleName, final LeaderElectionStateChangeListener listener) {
    }

    @Override
    public void register(final String roleName, final LeaderElectionStateChangeListener listener, final String participantId) {
    }

    @Override
    public boolean isActiveParticipant(final String roleName) {
        return true;
    }

    @Override
    public Optional<String> getLeader(final String roleName) {
        return Optional.empty();
    }

    @Override
    public void unregister(final String roleName) {
    }

    @Override
    public boolean isLeader(final String roleName) {
        return false;
    }

    @Override
    public void stop() {
    }

    @Override
    public Map<String, Integer> getLeadershipChangeCount(final long duration, final TimeUnit timeUnit) {
        return Collections.emptyMap();
    }

    @Override
    public long getAveragePollTime(final TimeUnit timeUnit) {
        return -1L;
    }

    @Override
    public long getMinPollTime(final TimeUnit timeUnit) {
        return -1L;
    }

    @Override
    public long getMaxPollTime(final TimeUnit timeUnit) {
        return -1L;
    }

    @Override
    public long getPollCount() {
        return -1L;
    }
}
