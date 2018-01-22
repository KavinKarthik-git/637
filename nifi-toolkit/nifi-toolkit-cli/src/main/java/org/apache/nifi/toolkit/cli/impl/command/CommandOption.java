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
package org.apache.nifi.toolkit.cli.impl.command;

import org.apache.commons.cli.Option;

/**
 * All possible options for commands.
 */
public enum CommandOption {

    // General
    URL("u", "baseUrl", "The URL to execute the command against", true),
    INPUT_FILE("i", "inputFile", "A file to read as input, must contain full path and filename", true),
    OUTPUT_FILE("o", "outputFile", "A file to write output to, must contain full path and filename", true),
    PROPERTIES("p", "properties", "A properties file to load arguments from, " +
            "command line values will override anything in the properties file, must contain full path to file", true),

    // Registry - Buckets
    BUCKET_ID("b", "bucketIdentifier", "A bucket identifier", true),
    BUCKET_NAME("bn", "bucketName", "A bucket name", true),
    BUCKET_DESC("bd", "bucketDesc", "A bucket description", true),

    // Registry - Flows
    FLOW_ID("f", "flowIdentifier", "A flow identifier", true),
    FLOW_NAME("fn", "flowName", "A flow name", true),
    FLOW_DESC("fd", "flowDesc", "A flow description", true),
    FLOW_VERSION("fv", "flowVersion", "A version of a flow", true),

    // NiFi - Registries
    REGISTRY_CLIENT_ID("rcid", "registryClientId", "The id of a registry client", true),
    REGISTRY_CLIENT_NAME("rcn", "registryClientName", "The name of the registry client", true),
    REGISTRY_CLIENT_URL("rcu", "registryClientUrl", "The url of the registry client", true),
    REGISTRY_CLIENT_DESC("rcd", "registryClientDesc", "The description of the registry client", true),

    // NiFi - PGs
    PG_ID("pgid", "processGroupId", "The id of a process group", true),
    PG_NAME("pgn", "processGroupName", "The name of a process group", true),

    // NiFi - Pos
    POS_X("px", "posX", "The X coordinate", true),
    POS_Y("py", "posY", "The Y coordinate", true),

    // Security related
    KEYSTORE("ks", "keystore", "A keystore to use for TLS/SSL connections", true),
    KEYSTORE_TYPE("kst", "keystoreType", "The type of key store being used (JKS or PKCS12)", true),
    KEYSTORE_PASSWORD("ksp", "keystorePasswd", "The password of the keystore being used", true),
    KEY_PASSWORD("kp", "keyPasswd", "The key password of the keystore being used", true),
    TRUSTSTORE("ts", "truststore", "A truststore to use for TLS/SSL connections", true),
    TRUSTSTORE_TYPE("tst", "truststoreType", "The type of trust store being used (JKS or PKCS12)", true),
    TRUSTSTORE_PASSWORD("tsp", "truststorePasswd", "The password of the truststore being used", true),
    PROXIED_ENTITY("pe", "proxiedEntity", "The identity of an entity to proxy", true),
    PROTOCOL("pro", "protocol", "The security protocol to use, such as TLSv.1.2", true),

    // Miscellaneous
    VERBOSE("verbose", "verbose", "Indicates that verbose output should be provided", false),
    HELP("h", "help", "Help", false)
    ;

    private final String shortName;
    private final String longName;
    private final String description;
    private final boolean hasArg;

    CommandOption(final String shortName, final String longName, final String description, final boolean hasArg) {
        this.shortName = shortName;
        this.longName = longName;
        this.description = description;
        this.hasArg = hasArg;
    }

    public String getShortName() {
        return shortName;
    }

    public String getLongName() {
        return longName;
    }

    public String getDescription() {
        return description;
    }

    public Option createOption() {
        return Option.builder(shortName).longOpt(longName).desc(description).hasArg(hasArg).build();
    }

}
