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
package org.apache.nifi.integration.accesscontrol;

import org.apache.commons.io.FileUtils;
import org.apache.nifi.bundle.Bundle;
import org.apache.nifi.integration.util.NiFiTestServer;
import org.apache.nifi.integration.util.NiFiTestUser;
import org.apache.nifi.nar.ExtensionDiscoveringManager;
import org.apache.nifi.nar.ExtensionManagerHolder;
import org.apache.nifi.nar.NarClassLoadersHolder;
import org.apache.nifi.nar.NarUnpacker;
import org.apache.nifi.nar.StandardExtensionDiscoveringManager;
import org.apache.nifi.nar.SystemBundle;
import org.apache.nifi.security.util.SslContextFactory;
import org.apache.nifi.util.NiFiProperties;
import org.apache.nifi.web.util.WebUtils;

import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Access control test for the dfm user.
 */
public class OneWaySslAccessControlHelper {

    private NiFiTestUser user;

    private static final String CONTEXT_PATH = "/nifi-api";

    private NiFiTestServer server;
    private String baseUrl;
    private String flowXmlPath;

    public OneWaySslAccessControlHelper() throws Exception {
        this("src/test/resources/access-control/nifi.properties");
    }

    public OneWaySslAccessControlHelper(final String nifiPropertiesPath) throws Exception {
        // configure the location of the nifi properties
        File nifiPropertiesFile = new File(nifiPropertiesPath);
        System.setProperty(NiFiProperties.PROPERTIES_FILE_PATH, nifiPropertiesFile.getAbsolutePath());

        NiFiProperties props = NiFiProperties.createBasicNiFiProperties(nifiPropertiesPath, null);
        flowXmlPath = props.getProperty(NiFiProperties.FLOW_CONFIGURATION_FILE);

        // delete the database directory to avoid issues with re-registration in testRequestAccessUsingToken
        FileUtils.deleteDirectory(props.getDatabaseRepositoryPath().toFile());

        final File libTargetDir = new File("target/test-classes/access-control/lib");
        libTargetDir.mkdirs();

        final File libSourceDir = new File("src/test/resources/lib");
        for (final File libFile : libSourceDir.listFiles()) {
            final File libDestFile = new File(libTargetDir, libFile.getName());
            Files.copy(libFile.toPath(), libDestFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        final Bundle systemBundle = SystemBundle.create(props);
        NarUnpacker.unpackNars(props, systemBundle);
        NarClassLoadersHolder.getInstance().init(props.getFrameworkWorkingDirectory(), props.getExtensionsWorkingDirectory());

        // load extensions
        final ExtensionDiscoveringManager extensionManager = new StandardExtensionDiscoveringManager();
        extensionManager.discoverExtensions(systemBundle, NarClassLoadersHolder.getInstance().getBundles());
        ExtensionManagerHolder.init(extensionManager);

        // start the server
        server = new NiFiTestServer("src/main/webapp", CONTEXT_PATH, props);
        server.startServer();
        server.loadFlow();

        // get the base url
        baseUrl = server.getBaseUrl() + CONTEXT_PATH;

        // create the user
        final Client client = WebUtils.createClient(null, createTrustContext(props));
        user = new NiFiTestUser(client, null);
    }

    public NiFiTestUser getUser() {
        return user;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    private static SSLContext createTrustContext(final NiFiProperties props) throws Exception {
        return SslContextFactory.createTrustSslContext(props.getProperty(NiFiProperties.SECURITY_TRUSTSTORE),
                props.getProperty(NiFiProperties.SECURITY_TRUSTSTORE_PASSWD).toCharArray(),
                props.getProperty(NiFiProperties.SECURITY_TRUSTSTORE_TYPE), "TLS");
    }

    public void cleanup() throws Exception {
        // shutdown the server
        server.shutdownServer();
        server = null;

        // look for the flow.xml and toss it
        File flow = new File(flowXmlPath);
        if (flow.exists()) {
            flow.delete();
        }
    }
}
