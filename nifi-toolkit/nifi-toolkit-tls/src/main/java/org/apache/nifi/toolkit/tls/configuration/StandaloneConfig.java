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

package org.apache.nifi.toolkit.tls.configuration;

import org.apache.nifi.toolkit.tls.properties.NiFiPropertiesWriterFactory;

import java.io.File;
import java.util.List;

public class StandaloneConfig extends TlsConfig {
    private File baseDir;
    private NiFiPropertiesWriterFactory niFiPropertiesWriterFactory;
    private List<String> hostnames;
    private List<String> keyStorePasswords;
    private List<String> keyPasswords;
    private List<String> trustStorePasswords;
    private List<String> clientDns;
    private List<String> clientPasswords;
    private boolean clientPasswordsGenerated;
    private int httpsPort;
    private boolean overwrite;

    public List<String> getClientDns() {
        return clientDns;
    }

    public void setClientDns(List<String> clientDns) {
        this.clientDns = clientDns;
    }

    public boolean isOverwrite() {
        return overwrite;
    }

    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    public File getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(File baseDir) {
        this.baseDir = baseDir;
    }

    public NiFiPropertiesWriterFactory getNiFiPropertiesWriterFactory() {
        return niFiPropertiesWriterFactory;
    }

    public void setNiFiPropertiesWriterFactory(NiFiPropertiesWriterFactory niFiPropertiesWriterFactory) {
        this.niFiPropertiesWriterFactory = niFiPropertiesWriterFactory;
    }

    public List<String> getHostnames() {
        return hostnames;
    }

    public void setHostnames(List<String> hostnames) {
        this.hostnames = hostnames;
    }

    public List<String> getKeyStorePasswords() {
        return keyStorePasswords;
    }

    public void setKeyStorePasswords(List<String> keyStorePasswords) {
        this.keyStorePasswords = keyStorePasswords;
    }

    public List<String> getKeyPasswords() {
        return keyPasswords;
    }

    public void setKeyPasswords(List<String> keyPasswords) {
        this.keyPasswords = keyPasswords;
    }

    public List<String> getTrustStorePasswords() {
        return trustStorePasswords;
    }

    public void setTrustStorePasswords(List<String> trustStorePasswords) {
        this.trustStorePasswords = trustStorePasswords;
    }

    public List<String> getClientPasswords() {
        return clientPasswords;
    }

    public void setClientPasswords(List<String> clientPasswords) {
        this.clientPasswords = clientPasswords;
    }

    public int getHttpsPort() {
        return httpsPort;
    }

    public void setHttpsPort(int httpsPort) {
        this.httpsPort = httpsPort;
    }

    public boolean isClientPasswordsGenerated() {
        return clientPasswordsGenerated;
    }

    public void setClientPasswordsGenerated(boolean clientPasswordsGenerated) {
        this.clientPasswordsGenerated = clientPasswordsGenerated;
    }
}
