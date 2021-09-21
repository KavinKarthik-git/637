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

package org.apache.nifi.processors.mqtt;

import io.moquette.proto.messages.PublishMessage;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processors.mqtt.common.MQTTQueueMessage;
import org.apache.nifi.processors.mqtt.common.MqttTestClient;
import org.apache.nifi.processors.mqtt.common.TestConsumeMqttCommon;
import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.security.util.KeyStoreUtils;
import org.apache.nifi.security.util.SslContextFactory;
import org.apache.nifi.security.util.TlsConfiguration;
import org.apache.nifi.security.util.TlsException;
import org.apache.nifi.ssl.SSLContextService;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.security.GeneralSecurityException;
import java.util.concurrent.BlockingQueue;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class TestConsumeMQTT extends TestConsumeMqttCommon {
    private static TlsConfiguration tlsConfiguration;

    public MqttTestClient mqttTestClient;

    public class UnitTestableConsumeMqtt extends ConsumeMQTT {

        public UnitTestableConsumeMqtt(){
            super();
        }

        @Override
        public IMqttClient createMqttClient(String broker, String clientID, MemoryPersistence persistence) throws MqttException {
            mqttTestClient =  new MqttTestClient(broker, clientID, MqttTestClient.ConnectType.Subscriber);
            return mqttTestClient;
        }
    }

    @BeforeClass
    public static void setTlsConfiguration() throws IOException, GeneralSecurityException  {
        tlsConfiguration = KeyStoreUtils.createTlsConfigAndNewKeystoreTruststore();
        new File(tlsConfiguration.getKeystorePath()).deleteOnExit();
        new File(tlsConfiguration.getTruststorePath()).deleteOnExit();
    }

    @Before
    public void init() {
        PUBLISH_WAIT_MS = 0;

        broker = "tcp://localhost:1883";
        UnitTestableConsumeMqtt proc = new UnitTestableConsumeMqtt();
        testRunner = TestRunners.newTestRunner(proc);
        testRunner.setProperty(ConsumeMQTT.PROP_BROKER_URI, broker);
        testRunner.setProperty(ConsumeMQTT.PROP_CLIENTID, "TestClient");
        testRunner.setProperty(ConsumeMQTT.PROP_TOPIC_FILTER, "testTopic");
        testRunner.setProperty(ConsumeMQTT.PROP_MAX_QUEUE_SIZE, "100");
    }

    @Test
    public void testSslContextService() throws InitializationException, TlsException {
        String brokerURI = "ssl://localhost:8883";
        TestRunner runner = TestRunners.newTestRunner(ConsumeMQTT.class);
        runner.setVariable("brokerURI", brokerURI);
        runner.setProperty(ConsumeMQTT.PROP_BROKER_URI, "${brokerURI}");
        runner.setProperty(ConsumeMQTT.PROP_CLIENTID, "TestClient");
        runner.setProperty(ConsumeMQTT.PROP_TOPIC_FILTER, "testTopic");
        runner.setProperty(ConsumeMQTT.PROP_MAX_QUEUE_SIZE, "100");

        final SSLContextService sslContextService = mock(SSLContextService.class);
        final String identifier = SSLContextService.class.getSimpleName();
        when(sslContextService.getIdentifier()).thenReturn(identifier);
        final SSLContext sslContext = SslContextFactory.createSslContext(tlsConfiguration);
        when(sslContextService.createContext()).thenReturn(sslContext);

        runner.addControllerService(identifier, sslContextService);
        runner.enableControllerService(sslContextService);
        runner.setProperty(ConsumeMQTT.PROP_SSL_CONTEXT_SERVICE, identifier);

        ConsumeMQTT processor = (ConsumeMQTT) runner.getProcessor();
        processor.onScheduled(runner.getProcessContext());
    }

    /**
     * If the session.commit() fails, we should not remove the unprocessed message
     */
    @Test
    public void testMessageNotConsumedOnCommitFail() throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException {
        testRunner.run(1, false);
        ConsumeMQTT processor = (ConsumeMQTT) testRunner.getProcessor();
        MQTTQueueMessage mock = mock(MQTTQueueMessage.class);
        when(mock.getPayload()).thenReturn(new byte[0]);
        when(mock.getTopic()).thenReturn("testTopic");
        BlockingQueue<MQTTQueueMessage> mqttQueue = getMqttQueue(processor);
        mqttQueue.add(mock);
        try {
            ProcessSession session = testRunner.getProcessSessionFactory().createSession();
            transferQueue(processor,
                    (ProcessSession) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { ProcessSession.class }, (proxy, method, args) -> {
                        if (method.getName().equals("commitAsync")) {
                            throw new RuntimeException();
                        } else {
                            return method.invoke(session, args);
                        }
                    }));
            fail("Expected runtime exception");
        } catch (InvocationTargetException e) {
            assertTrue("Expected generic runtime exception, not " + e, e.getCause() instanceof RuntimeException);
        }
        assertTrue("Expected mqttQueue to contain uncommitted message.", mqttQueue.contains(mock));
    }

    @After
    public void tearDown() {
        if (MQTT_server != null) {
            MQTT_server.stopServer();
        }
        final File folder =  new File("./target");
        final File[] files = folder.listFiles( new FilenameFilter() {
            @Override
            public boolean accept( final File dir,
                                   final String name ) {
                return name.matches( "moquette_store.mapdb.*" );
            }
        } );
        for ( final File file : files ) {
            if ( !file.delete() ) {
                System.err.println( "Can't remove " + file.getAbsolutePath() );
            }
        }
    }

    @Override
    public void internalPublish(PublishMessage publishMessage) {
        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setPayload(publishMessage.getPayload().array());
        mqttMessage.setRetained(publishMessage.isRetainFlag());
        mqttMessage.setQos(publishMessage.getQos().ordinal());

        try {
            mqttTestClient.publish(publishMessage.getTopicName(), mqttMessage);
        } catch (MqttException e) {
            fail("Should never get an MqttException when publishing using test client");
        }
    }
}
