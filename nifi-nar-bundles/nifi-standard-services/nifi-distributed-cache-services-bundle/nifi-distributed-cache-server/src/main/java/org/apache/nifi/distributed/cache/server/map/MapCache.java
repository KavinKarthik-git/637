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
package org.apache.nifi.distributed.cache.server.map;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

public interface MapCache {

    MapPutResult putIfAbsent(ByteBuffer key, ByteBuffer value) throws IOException;

    MapPutResult put(ByteBuffer key, ByteBuffer value) throws IOException;

    boolean containsKey(ByteBuffer key) throws IOException;

    ByteBuffer get(ByteBuffer key) throws IOException;

    ByteBuffer remove(ByteBuffer key) throws IOException;

    Map<ByteBuffer, ByteBuffer> removeByPattern(String regex) throws IOException;

    MapCacheRecord fetch(ByteBuffer key) throws IOException;

    MapPutResult replace(MapCacheRecord record) throws IOException;

    void shutdown() throws IOException;
}
