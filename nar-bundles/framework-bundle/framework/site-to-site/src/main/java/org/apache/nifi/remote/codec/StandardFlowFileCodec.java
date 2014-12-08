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
package org.apache.nifi.remote.codec;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.io.StreamUtils;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.io.InputStreamCallback;
import org.apache.nifi.processor.io.OutputStreamCallback;
import org.apache.nifi.remote.StandardVersionNegotiator;
import org.apache.nifi.remote.VersionNegotiator;
import org.apache.nifi.remote.exception.ProtocolException;

public class StandardFlowFileCodec implements FlowFileCodec {
	public static final int MAX_NUM_ATTRIBUTES = 25000;

    public static final String DEFAULT_FLOWFILE_PATH = "./";

    private final VersionNegotiator versionNegotiator;

    public StandardFlowFileCodec() {
        versionNegotiator = new StandardVersionNegotiator(1);
    }
    
    @Override
    public FlowFile encode(final FlowFile flowFile, final ProcessSession session, final OutputStream encodedOut) throws IOException {
        final DataOutputStream out = new DataOutputStream(encodedOut);
        
        final Map<String, String> attributes = flowFile.getAttributes();
        out.writeInt(attributes.size());
        for ( final Map.Entry<String, String> entry : attributes.entrySet() ) {
            writeString(entry.getKey(), out);
            writeString(entry.getValue(), out);
        }
        
        out.writeLong(flowFile.getSize());
        
        session.read(flowFile, new InputStreamCallback() {
            @Override
            public void process(final InputStream in) throws IOException {
                final byte[] buffer = new byte[8192];
                int len;
                while ( (len = in.read(buffer)) > 0 ) {
                    encodedOut.write(buffer, 0, len);
                }
                
                encodedOut.flush();
            }
        });
        
        return flowFile;
    }

    
    @Override
    public FlowFile decode(final InputStream stream, final ProcessSession session) throws IOException, ProtocolException {
        final DataInputStream in = new DataInputStream(stream);
        
        final int numAttributes;
        try {
            numAttributes = in.readInt();
        } catch (final EOFException e) {
            // we're out of data.
            return null;
        }
        
        // This is here because if the stream is not properly formed, we could get up to Integer.MAX_VALUE attributes, which will
        // generally result in an OutOfMemoryError.
        if ( numAttributes > MAX_NUM_ATTRIBUTES ) {
        	throw new ProtocolException("FlowFile exceeds maximum number of attributes with a total of " + numAttributes);
        }
        
        try {
            final Map<String, String> attributes = new HashMap<>(numAttributes);
            for (int i=0; i < numAttributes; i++) {
                final String attrName = readString(in);
                final String attrValue = readString(in);
                attributes.put(attrName, attrValue);
            }
            
            final long numBytes = in.readLong();
            
            FlowFile flowFile = session.create();
            flowFile = session.putAllAttributes(flowFile, attributes);
            flowFile = session.write(flowFile, new OutputStreamCallback() {
                @Override
                public void process(final OutputStream out) throws IOException {
                    int len;
                    long size = 0;
                    final byte[] buffer = new byte[8192];
                    
                    while ( size < numBytes && (len = in.read(buffer, 0, (int) Math.min(buffer.length, numBytes - size))) > 0 ) {
                        out.write(buffer, 0, len);
                        size += len;
                    }

                    if ( size != numBytes ) {
                        throw new EOFException("Expected " + numBytes + " bytes but received only " + size);
                    }
                }
            });

            return flowFile;
        } catch (final EOFException e) {
        	session.rollback();
        	
            // we throw the general IOException here because we did not expect to hit EOFException
            throw e;
        }
    }

    private void writeString(final String val, final DataOutputStream out) throws IOException {
        final byte[] bytes = val.getBytes("UTF-8");
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    
    private String readString(final DataInputStream in) throws IOException {
        final int numBytes = in.readInt();
        final byte[] bytes = new byte[numBytes];
        StreamUtils.fillBuffer(in, bytes, true);
        return new String(bytes, "UTF-8");
    }
    
    @Override
    public List<Integer> getSupportedVersions() {
        return versionNegotiator.getSupportedVersions();
    }

    @Override
    public VersionNegotiator getVersionNegotiator() {
        return versionNegotiator;
    }

    @Override
    public String toString() {
        return "Standard FlowFile Codec, Version " + versionNegotiator.getVersion();
    }

    @Override
    public String getResourceName() {
        return "StandardFlowFileCodec";
    }
}
