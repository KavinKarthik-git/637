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
package org.apache.nifi.processors.standard.util;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.JsonProvider;
import net.minidev.json.JSONValue;
import org.apache.nifi.components.ValidationContext;
import org.apache.nifi.components.ValidationResult;
import org.apache.nifi.components.Validator;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.io.InputStreamCallback;
import org.apache.nifi.stream.io.BufferedInputStream;
import org.apache.nifi.util.BooleanHolder;
import org.apache.nifi.util.ObjectHolder;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

/**
 * Provides utilities for interacting with JSON elements and JsonPath expressions and results
 *
 * @see <a href="http://json.org">http://json.org</a>
 * @see <a href="https://github.com/jayway/JsonPath">https://github.com/jayway/JsonPath</a>
 */
public class JsonUtils {

    static final JsonProvider JSON_PROVIDER = Configuration.defaultConfiguration().jsonProvider();

    public static final Validator JSON_PATH_VALIDATOR = new Validator() {
        @Override
        public ValidationResult validate(final String subject, final String input, final ValidationContext context) {
            String error = null;
            try {
                JsonPath compile = JsonPath.compile(input);
            } catch (InvalidPathException ipe) {
                error = ipe.toString();
            }
            return new ValidationResult.Builder().valid(error == null).explanation(error).build();
        }
    };

    public static DocumentContext validateAndEstablishJsonContext(ProcessSession processSession, FlowFile flowFile) {

        final BooleanHolder validJsonHolder = new BooleanHolder(false);
        processSession.read(flowFile, new InputStreamCallback() {
            @Override
            public void process(InputStream in) throws IOException {
                validJsonHolder.set(JsonUtils.isValidJson(in));
            }
        });

        // Parse the document once into an associated context to support multiple path evaluations if specified
        final ObjectHolder<DocumentContext> contextHolder = new ObjectHolder<>(null);

        if (validJsonHolder.get()) {
            processSession.read(flowFile, new InputStreamCallback() {
                @Override
                public void process(InputStream in) throws IOException {
                    try (BufferedInputStream bufferedInputStream = new BufferedInputStream(in)) {
                        DocumentContext ctx = JsonPath.parse(in);
                        contextHolder.set(ctx);
                    }
                }
            });
        }

        return contextHolder.get();
    }

    /**
     * JSONValue#isValidJson is permissive to the degree of the Smart JSON definition, accordingly a strict JSON approach
     * is preferred in determining whether or not a document is valid.
     * Performs a validation of the provided stream according to RFC 4627 as implemented by {@link net.minidev.json.parser.JSONParser#MODE_RFC4627}
     *
     * @param inputStream of content to be validated as JSON
     * @return true, if the content is valid within the bounds of the strictness specified; false otherwise
     * @throws IOException
     */
    public static boolean isValidJson(InputStream inputStream) throws IOException {
        try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream)) {
            return JSONValue.isValidJsonStrict(inputStreamReader);
        }
    }

    /**
     * Determines the context by which JsonSmartJsonProvider would treat the value.  {@link java.util.Map} and
     * {@link java.util.List} objects can be rendered as JSON elements, everything else is treated as a scalar.
     *
     * @param obj item to be inspected if it is a scalar or a JSON element
     * @return false, if the object is a supported type; true otherwise
     */
    public static boolean isJsonScalar(Object obj) {
        // For the default provider, JsonSmartJsonProvider, a Map or List is able to be handled as a JSON entity
        return !(obj instanceof Map || obj instanceof List);
    }


    public static String getResultRepresentation(Object jsonPathResult) {
        if (JsonUtils.isJsonScalar(jsonPathResult)) {
            return jsonPathResult.toString();
        }
        return JSON_PROVIDER.toJson(jsonPathResult);
    }

}
