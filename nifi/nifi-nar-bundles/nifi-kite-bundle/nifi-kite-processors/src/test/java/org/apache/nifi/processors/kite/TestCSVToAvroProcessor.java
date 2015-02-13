/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.nifi.processors.kite;

import java.io.IOException;
import java.nio.charset.Charset;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.Assert;
import org.junit.Test;

import static org.apache.nifi.processors.kite.TestUtil.streamFor;

public class TestCSVToAvroProcessor {

  public static final Schema SCHEMA = SchemaBuilder.record("Test").fields()
      .requiredLong("id")
      .requiredString("color")
      .optionalDouble("price")
      .endRecord();

  public static final String CSV_CONTENT = "" +
      "1,green\n" +
      ",blue,\n" + // invalid, ID is missing
      "2,grey,12.95";

  @Test
  public void testBasicConversion() throws IOException {
    TestRunner runner = TestRunners.newTestRunner(ConvertCSVToAvro.class);
    runner.assertNotValid();
    runner.setProperty(ConvertCSVToAvro.SCHEMA, SCHEMA.toString());
    runner.assertValid();

    runner.enqueue(streamFor(CSV_CONTENT));
    runner.run();

    long converted = runner.getCounterValue("Converted records");
    long errors = runner.getCounterValue("Conversion errors");
    Assert.assertEquals("Should convert 2 rows", 2, converted);
    Assert.assertEquals("Should reject 1 row", 1, errors);

    runner.assertAllFlowFilesTransferred("success", 1);
  }

  @Test
  public void testAlternateCharset() throws IOException {
    TestRunner runner = TestRunners.newTestRunner(ConvertCSVToAvro.class);
    runner.setProperty(ConvertCSVToAvro.SCHEMA, SCHEMA.toString());
    runner.setProperty(ConvertCSVToAvro.CHARSET, "utf16");
    runner.assertValid();

    runner.enqueue(streamFor(CSV_CONTENT, Charset.forName("UTF-16")));
    runner.run();

    long converted = runner.getCounterValue("Converted records");
    long errors = runner.getCounterValue("Conversion errors");
    Assert.assertEquals("Should convert 2 rows", 2, converted);
    Assert.assertEquals("Should reject 1 row", 1, errors);

    runner.assertAllFlowFilesTransferred("success", 1);
  }

  @Test
  public void testCSVProperties() throws IOException {
    TestRunner runner = TestRunners.newTestRunner(ConvertCSVToAvro.class);
    ConvertCSVToAvro processor = new ConvertCSVToAvro();
    ProcessContext context = runner.getProcessContext();

    // check defaults
    processor.createCSVProperties(context);
    Assert.assertEquals("Charset should match",
        "utf8", processor.props.charset);
    Assert.assertEquals("Delimiter should match",
        ",", processor.props.delimiter);
    Assert.assertEquals("Quote should match",
        "\"", processor.props.quote);
    Assert.assertEquals("Escape should match",
        "\\", processor.props.escape);
    Assert.assertEquals("Header flag should match",
        false, processor.props.useHeader);
    Assert.assertEquals("Lines to skip should match",
        0, processor.props.linesToSkip);

    runner.setProperty(ConvertCSVToAvro.CHARSET, "utf16");
    runner.setProperty(ConvertCSVToAvro.DELIMITER, "|");
    runner.setProperty(ConvertCSVToAvro.QUOTE, "'");
    runner.setProperty(ConvertCSVToAvro.ESCAPE, "\u2603");
    runner.setProperty(ConvertCSVToAvro.HAS_HEADER, "true");
    runner.setProperty(ConvertCSVToAvro.LINES_TO_SKIP, "2");

    // check updates
    processor.createCSVProperties(context);
    Assert.assertEquals("Charset should match",
        "utf16", processor.props.charset);
    Assert.assertEquals("Delimiter should match",
        "|", processor.props.delimiter);
    Assert.assertEquals("Quote should match",
        "'", processor.props.quote);
    Assert.assertEquals("Escape should match",
        "\u2603", processor.props.escape);
    Assert.assertEquals("Header flag should match",
        true, processor.props.useHeader);
    Assert.assertEquals("Lines to skip should match",
        2, processor.props.linesToSkip);
  }
}
