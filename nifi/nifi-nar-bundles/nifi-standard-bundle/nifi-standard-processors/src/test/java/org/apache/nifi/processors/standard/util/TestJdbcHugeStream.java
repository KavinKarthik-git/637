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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Random;

import org.apache.avro.file.DataFileStream;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *  Test streaming using large number of result set rows.
 * 1. Read data from database.
 * 2. Create Avro schema from ResultSet meta data.
 * 3. Read rows from ResultSet and write rows to Avro writer stream 
 *    (Avro will create record for each row).
 * 4. And finally read records from Avro stream to verify all data is present in Avro stream. 
 *   
 *  
 * Sql query will return all combinations from 3 table.
 * For example when each table contain 1000 rows, result set will be 1 000 000 000 rows.
 *
 */
public class TestJdbcHugeStream {

    final static String DB_LOCATION = "target/db";

    @BeforeClass
    public static void setup() {
        System.setProperty("derby.stream.error.file", "target/derby.log");
    }

    /**	
     * 	In case of large record set this will fail with
     * java.lang.OutOfMemoryError: Java heap space
	 * at java.util.Arrays.copyOf(Arrays.java:2271)
	 * at java.io.ByteArrayOutputStream.grow(ByteArrayOutputStream.java:113)
	 * at java.io.ByteArrayOutputStream.ensureCapacity(ByteArrayOutputStream.java:93)
	 * at java.io.ByteArrayOutputStream.write(ByteArrayOutputStream.java:140)
	 * at org.apache.avro.file.DataFileWriter$BufferedFileOutputStream$PositionFilter.write(DataFileWriter.java:446)
     * 
     */    
//	@Test
	public void readSend2StreamHuge_InMemory() throws ClassNotFoundException, SQLException, IOException {
		
        // remove previous test database, if any
        File dbLocation = new File(DB_LOCATION);
        dbLocation.delete();

        Connection con = createConnection();
        loadTestData2Database(con, 150, 150, 150);
        System.out.println("test data loaded");
        
        Statement st = con.createStatement();
        
        // Notice!
        // Following select is deliberately invalid!
        // For testing we need huge amount of rows, so where part is not used.
        ResultSet resultSet = st.executeQuery("select "
        		+ "  PER.ID as PersonId, PER.NAME as PersonName, PER.CODE as PersonCode"
        		+ ", PRD.ID as ProductId,PRD.NAME as ProductName,PRD.CODE as ProductCode"
        		+ ", REL.ID as RelId,    REL.NAME as RelName,    REL.CODE as RelCode"
        		+ ", ROW_NUMBER() OVER () as rownr "
        		+ " from persons PER, products PRD, relationships REL");

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        long nrOfRows = JdbcCommon.convertToAvroStream(resultSet, outStream);
        System.out.println("total nr of rows in resultset: " + nrOfRows);

        byte[] serializedBytes = outStream.toByteArray();
        assertNotNull(serializedBytes);
        System.out.println("Avro serialized result size in bytes: " + serializedBytes.length);

        // Deserialize bytes to records
        
        InputStream instream = new ByteArrayInputStream(serializedBytes);
        
        DatumReader<GenericRecord> datumReader = new GenericDatumReader<GenericRecord>();
        DataFileStream<GenericRecord> dataFileReader = new DataFileStream<GenericRecord>(instream, datumReader);
        GenericRecord record = null;
        long recordsFromStream = 0;
        while (dataFileReader.hasNext()) {
        	// Reuse record object by passing it to next(). This saves us from
        	// allocating and garbage collecting many objects for files with many items.
        	record = dataFileReader.next(record);
//        	System.out.println(record);
        	recordsFromStream += 1;
        }
        System.out.println("total nr of records from stream: " + recordsFromStream);
        assertEquals(nrOfRows, recordsFromStream);
        st.close();
        con.close();
	}
		
	@Test
	public void readSend2StreamHuge_FileBased() throws ClassNotFoundException, SQLException, IOException {
		
        // remove previous test database, if any
        File dbLocation = new File(DB_LOCATION);
        dbLocation.delete();

        Connection con = createConnection();
        loadTestData2Database(con, 300, 300, 300);
        System.out.println("test data loaded");
        
        Statement st = con.createStatement();
        
        // Notice!
        // Following select is deliberately invalid!
        // For testing we need huge amount of rows, so where part is not used.
        ResultSet resultSet = st.executeQuery("select "
        		+ "  PER.ID as PersonId, PER.NAME as PersonName, PER.CODE as PersonCode"
        		+ ", PRD.ID as ProductId,PRD.NAME as ProductName,PRD.CODE as ProductCode"
        		+ ", REL.ID as RelId,    REL.NAME as RelName,    REL.CODE as RelCode"
        		+ ", ROW_NUMBER() OVER () as rownr "
        		+ " from persons PER, products PRD, relationships REL");

        OutputStream outStream = new FileOutputStream("target/data.avro");
        long nrOfRows = JdbcCommon.convertToAvroStream(resultSet, outStream);
        System.out.println("total nr of rows in resultset: " + nrOfRows);
/*
        byte[] serializedBytes = outStream.toByteArray();
        assertNotNull(serializedBytes);
        System.out.println("Avro serialized result size in bytes: " + serializedBytes.length);
*/
        // Deserialize bytes to records
        
        InputStream instream = new FileInputStream("target/data.avro");
        
        DatumReader<GenericRecord> datumReader = new GenericDatumReader<GenericRecord>();
        DataFileStream<GenericRecord> dataFileReader = new DataFileStream<GenericRecord>(instream, datumReader);
        GenericRecord record = null;
        long recordsFromStream = 0;
        while (dataFileReader.hasNext()) {
        	// Reuse record object by passing it to next(). This saves us from
        	// allocating and garbage collecting many objects for files with many items.
        	record = dataFileReader.next(record);
//        	System.out.println(record);
        	recordsFromStream += 1;
        }
        System.out.println("total nr of records from stream: " + recordsFromStream);
        assertEquals(nrOfRows, recordsFromStream);        
        st.close();
        con.close();
	}
		
	//================================================  helpers  ===============================================
	
    static String dropPersons		= "drop table persons";
    static String dropProducts		= "drop table products";
    static String dropRelationships= "drop table relationships";
    static String createPersons 		= "create table persons		(id integer, name varchar(100), code integer)";
    static String createProducts 		= "create table products	(id integer, name varchar(100), code integer)";
    static String createRelationships 	= "create table relationships(id integer,name varchar(100), code integer)";

	static public void loadTestData2Database(Connection con, int nrOfPersons, int nrOfProducts, int nrOfRels) throws ClassNotFoundException, SQLException {
		
		System.out.println(createRandomName());
		System.out.println(createRandomName());
		System.out.println(createRandomName());
		
        Statement st = con.createStatement();

        // tables may not exist, this is not serious problem.
        try { st.executeUpdate(dropPersons);
        } catch (Exception e) { }
        
        try { st.executeUpdate(dropProducts);
        } catch (Exception e) { }
        
        try { st.executeUpdate(dropRelationships);
        } catch (Exception e) { } 

        st.executeUpdate(createPersons);
        st.executeUpdate(createProducts);
        st.executeUpdate(createRelationships);
        
        for (int i = 0; i < nrOfPersons; i++)
     		loadPersons(st, i);
		
        for (int i = 0; i < nrOfProducts; i++)
        	loadProducts(st, i);
		
        for (int i = 0; i < nrOfRels; i++)
        	loadRelationships(st, i);

        st.close();
	}

	static Random rng = new Random(53495);

	static private void loadPersons(Statement st, int nr) throws SQLException {
		
        st.executeUpdate("insert into persons values (" + nr + ", '" + createRandomName() +  "', " + rng.nextInt(469946) + ")" );		
	}

	static private void loadProducts(Statement st, int nr) throws SQLException {
		
        st.executeUpdate("insert into products values (" + nr + ", '" + createRandomName() +  "', " + rng.nextInt(469946) + ")" );		
	}

	static private void loadRelationships(Statement st, int nr) throws SQLException {
		
        st.executeUpdate("insert into relationships values (" + nr + ", '" + createRandomName() +  "', " + rng.nextInt(469946) + ")" );		
	}

	static private String createRandomName() {
		return createRandomString() + " " + createRandomString();
	}
	
	static private String createRandomString() {
		
		int length = rng.nextInt(19);
		String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		
		char[] text = new char[length];
	    for (int i = 0; i < length; i++)
	    {
	        text[i] = characters.charAt(rng.nextInt(characters.length()));
	    }
	    return new String(text);			
	}
	
	private Connection createConnection() throws ClassNotFoundException, SQLException {
		
        Class.forName("org.apache.derby.jdbc.EmbeddedDriver");        
        Connection con = DriverManager.getConnection("jdbc:derby:" + DB_LOCATION + ";create=true");
		return con;
	}

}
