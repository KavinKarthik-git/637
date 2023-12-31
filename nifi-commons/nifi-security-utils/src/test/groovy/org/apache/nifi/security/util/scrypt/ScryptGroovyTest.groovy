/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License") you may not use this file except in compliance with
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
package org.apache.nifi.security.util.scrypt

import org.apache.commons.codec.binary.Hex
import org.apache.nifi.security.util.crypto.scrypt.Scrypt
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.security.SecureRandom
import java.security.Security

import static org.junit.jupiter.api.Assertions.assertArrayEquals
import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue
import static org.junit.jupiter.api.Assumptions.assumeTrue

class ScryptGroovyTest {
    private static final Logger logger = LoggerFactory.getLogger(ScryptGroovyTest.class)

    private static final String PASSWORD = "shortPassword"
    private static final String SALT_HEX = "0123456789ABCDEFFEDCBA9876543210"
    private static final byte[] SALT_BYTES = Hex.decodeHex(SALT_HEX as char[])

    // Small values to test for correctness, not timing
    private static final int N = 2**4
    private static final int R = 1
    private static final int P = 1
    private static final int DK_LEN = 128
    private static final long TWO_GIGABYTES = 2048L * 1024 * 1024

    @BeforeAll
    static void setUpOnce() throws Exception {
        Security.addProvider(new BouncyCastleProvider())

        logger.metaClass.methodMissing = { String name, args ->
            logger.info("[${name?.toUpperCase()}] ${(args as List).join(" ")}")
        }
    }

    @Test
    void testDeriveScryptKeyShouldBeInternallyConsistent() throws Exception {
        // Arrange
        def allKeys = []
        final int RUNS = 10

        logger.info("Running with '${PASSWORD}', '${SALT_HEX}', $N, $R, $P, $DK_LEN")

        // Act
        RUNS.times {
            byte[] keyBytes = Scrypt.deriveScryptKey(PASSWORD.bytes, SALT_BYTES, N, R, P, DK_LEN)
            logger.info("Derived key: ${Hex.encodeHexString(keyBytes)}")
            allKeys << keyBytes
        }

        // Assert
        assertEquals(RUNS, allKeys.size())
        allKeys.forEach(key -> assertArrayEquals(allKeys.first(), key))
    }

    /**
     * This test ensures that the local implementation of Scrypt is compatible with the reference implementation from the Colin Percival paper.
     */
    @Test
    void testDeriveScryptKeyShouldMatchTestVectors() {
        // Arrange

        // These values are taken from Colin Percival's scrypt paper: https://www.tarsnap.com/scrypt/scrypt.pdf
        final byte[] HASH_2 = Hex.decodeHex("fdbabe1c9d3472007856e7190d01e9fe" +
                "7c6ad7cbc8237830e77376634b373162" +
                "2eaf30d92e22a3886ff109279d9830da" +
                "c727afb94a83ee6d8360cbdfa2cc0640" as char[])

        final byte[] HASH_3 = Hex.decodeHex("7023bdcb3afd7348461c06cd81fd38eb" +
                "fda8fbba904f8e3ea9b543f6545da1f2" +
                "d5432955613f0fcf62d49705242a9af9" +
                "e61e85dc0d651e40dfcf017b45575887" as char[])

        final def TEST_VECTORS = [
                // Empty password is not supported by JCE
                [password: "password",
                 salt    : "NaCl",
                 n       : 1024,
                 r       : 8,
                 p       : 16,
                 dkLen   : 64 * 8,
                 hash    : HASH_2],
                [password: "pleaseletmein",
                 salt    : "SodiumChloride",
                 n       : 16384,
                 r       : 8,
                 p       : 1,
                 dkLen   : 64 * 8,
                 hash    : HASH_3],
        ]

        // Act
        TEST_VECTORS.each { Map params ->
            logger.info("Running with '${params.password}', '${params.salt}', ${params.n}, ${params.r}, ${params.p}, ${params.dkLen}")
            long memoryInBytes = Scrypt.calculateExpectedMemory(params.n, params.r, params.p)
            logger.info("Expected memory usage: (128 * r * N + 128 * r * p) ${memoryInBytes} bytes")
            logger.info(" Expected ${Hex.encodeHexString(params.hash)}")

            byte[] calculatedHash = Scrypt.deriveScryptKey(params.password.bytes, params.salt.bytes, params.n, params.r, params.p, params.dkLen)
            logger.info("Generated ${Hex.encodeHexString(calculatedHash)}")

            // Assert
            assertArrayEquals(params.hash, calculatedHash)
        }
    }

    /**
     * This test ensures that the local implementation of Scrypt is compatible with the reference implementation from the Colin Percival paper. The test vector requires ~1GB {@code byte[]}
     * and therefore the Java heap must be at least 1GB. Because {@link nifi/pom.xml} has a {@code surefire} rule which appends {@code -Xmx1G}
     * to the Java options, this overrides any IDE options. To ensure the heap is properly set, using the {@code groovyUnitTest} profile will re-append {@code -Xmx3072m} to the Java options.
     */
    @Test
    void testDeriveScryptKeyShouldMatchExpensiveTestVector() {
        // Arrange
        long totalMemory = Runtime.getRuntime().totalMemory()
        logger.info("Required memory: ${TWO_GIGABYTES} bytes")
        logger.info("Max heap memory: ${totalMemory} bytes")
        assumeTrue(totalMemory >= TWO_GIGABYTES, "Test is being skipped due to JVM heap size. Please run with -Xmx3072m to set sufficient heap size")

        // These values are taken from Colin Percival's scrypt paper: https://www.tarsnap.com/scrypt/scrypt.pdf
        final byte[] HASH = Hex.decodeHex("2101cb9b6a511aaeaddbbe09cf70f881" +
                "ec568d574a2ffd4dabe5ee9820adaa47" +
                "8e56fd8f4ba5d09ffa1c6d927c40f4c3" +
                "37304049e8a952fbcbf45c6fa77a41a4" as char[])

        // This test vector requires 2GB heap space and approximately 10 seconds on a consumer machine
        String password = "pleaseletmein"
        String salt = "SodiumChloride"
        int n = 1048576
        int r = 8
        int p = 1
        int dkLen = 64 * 8

        // Act
        logger.info("Running with '${password}', '${salt}', ${n}, ${r}, ${p}, ${dkLen}")
        long memoryInBytes = Scrypt.calculateExpectedMemory(n, r, p)
        logger.info("Expected memory usage: (128 * r * N + 128 * r * p) ${memoryInBytes} bytes")
        logger.info(" Expected ${Hex.encodeHexString(HASH)}")

        byte[] calculatedHash = Scrypt.deriveScryptKey(password.bytes, salt.bytes, n, r, p, dkLen)
        logger.info("Generated ${Hex.encodeHexString(calculatedHash)}")

        // Assert
        assertArrayEquals(HASH, calculatedHash)
    }

    @EnabledIfSystemProperty(named = "nifi.test.unstable", matches = "true")
    @Test
    void testShouldCauseOutOfMemoryError() {
        SecureRandom secureRandom = new SecureRandom()
//        int i = 29
        (10..31).each { int i ->
            int length = 2**i
            byte[] bytes = new byte[length]
            secureRandom.nextBytes(bytes)
            logger.info("Successfully ran with byte[] of length ${length}")
            logger.info("${Hex.encodeHexString(bytes[0..<16] as byte[])}...")
        }
    }

    @Test
    void testDeriveScryptKeyShouldSupportExternalCompatibility() {
        // Arrange

        // These values can be generated by running `$ ./openssl_scrypt.rb` in the terminal
        final String EXPECTED_KEY_HEX = "a8efbc0a709d3f89b6bb35b05fc8edf5"
        String password = "thisIsABadPassword"
        String saltHex = "f5b8056ea6e66edb8d013ac432aba24a"
        int n = 1024
        int r = 8
        int p = 36
        int dkLen = 16 * 8

        // Act
        logger.info("Running with '${password}', ${saltHex}, ${n}, ${r}, ${p}, ${dkLen}")
        long memoryInBytes = Scrypt.calculateExpectedMemory(n, r, p)
        logger.info("Expected memory usage: (128 * r * N + 128 * r * p) ${memoryInBytes} bytes")
        logger.info(" Expected ${EXPECTED_KEY_HEX}")

        byte[] calculatedHash = Scrypt.deriveScryptKey(password.bytes, Hex.decodeHex(saltHex as char[]), n, r, p, dkLen)
        logger.info("Generated ${Hex.encodeHexString(calculatedHash)}")

        // Assert
        assertArrayEquals(Hex.decodeHex(EXPECTED_KEY_HEX as char[]), calculatedHash)
    }

    @Test
    void testScryptShouldBeInternallyConsistent() throws Exception {
        // Arrange
        def allHashes = []
        final int RUNS = 10

        logger.info("Running with '${PASSWORD}', '${SALT_HEX}', $N, $R, $P")

        // Act
        RUNS.times {
            String hash = Scrypt.scrypt(PASSWORD, SALT_BYTES, N, R, P, DK_LEN)
            logger.info("Hash: ${hash}")
            allHashes << hash
        }

        // Assert
        assertEquals(RUNS, allHashes.size())
        allHashes.forEach(hash -> assertEquals(allHashes.first(), hash))
    }

    @Test
    void testScryptShouldGenerateValidSaltIfMissing() {
        // Arrange

        // The generated salt should be byte[16], encoded as 22 Base64 chars
        final EXPECTED_SALT_PATTERN = /\$.+\$[0-9a-zA-Z\/\+]{22}\$.+/

        // Act
        String calculatedHash = Scrypt.scrypt(PASSWORD, N, R, P, DK_LEN)
        logger.info("Generated ${calculatedHash}")

        // Assert
        assertTrue((calculatedHash =~ EXPECTED_SALT_PATTERN).matches())
    }

    @Test
    void testScryptShouldNotAcceptInvalidN() throws Exception {
        // Arrange

        final int MAX_N = Integer.MAX_VALUE / 128 / R - 1

        // N must be a power of 2 > 1 and < Integer.MAX_VALUE / 128 / r
        final def INVALID_NS = [-2, 0, 1, 3, 4096 - 1, MAX_N + 1]

        // Act
        INVALID_NS.each { int invalidN ->
            logger.info("Using N: ${invalidN}")

            IllegalArgumentException iae = assertThrows(IllegalArgumentException.class,
                    () -> Scrypt.deriveScryptKey(PASSWORD.bytes, SALT_BYTES, invalidN, R, P, DK_LEN))

            // Assert
            assertTrue((iae.getMessage() =~ "N must be a power of 2 greater than 1|Parameter N is too large").matches())
        }
    }

    @Test
    void testScryptShouldAcceptValidR() throws Exception {
        // Arrange

        // Use a large p value to allow r to exceed MAX_R without normal N exceeding MAX_N
        int largeP = 2**10
        final int MAX_R = Math.ceil(Integer.MAX_VALUE / 128 / largeP) - 1

        // r must be in (0..Integer.MAX_VALUE / 128 / p)
        final def INVALID_RS = [0, MAX_R + 1]

        // Act
        INVALID_RS.each { int invalidR ->
            logger.info("Using r: ${invalidR}")

            IllegalArgumentException iae = assertThrows(IllegalArgumentException.class,
                    () -> Scrypt.deriveScryptKey(PASSWORD.bytes, SALT_BYTES, N, invalidR, largeP, DK_LEN))

            // Assert
            assertTrue((iae.getMessage() =~ "Parameter r must be 1 or greater|Parameter r is too large").matches())
        }
    }

    @Test
    void testScryptShouldNotAcceptInvalidP() throws Exception {
        // Arrange
        final int MAX_P = Math.ceil(Integer.MAX_VALUE / 128) - 1

        // p must be in (0..Integer.MAX_VALUE / 128)
        final def INVALID_PS = [0, MAX_P + 1]

        // Act
        INVALID_PS.each { int invalidP ->
            logger.info("Using p: ${invalidP}")

            IllegalArgumentException iae = assertThrows(IllegalArgumentException.class,
                    () -> Scrypt.deriveScryptKey(PASSWORD.bytes, SALT_BYTES, N, R, invalidP, DK_LEN))

            // Assert
            assertTrue((iae.getMessage() =~ "Parameter p must be 1 or greater|Parameter p is too large").matches())
        }
    }

    @Test
    void testCheckShouldValidateCorrectPassword() throws Exception {
        // Arrange
        final String PASSWORD = "thisIsABadPassword"
        final String EXPECTED_HASH = Scrypt.scrypt(PASSWORD, N, R, P, DK_LEN)
        logger.info("Password: ${PASSWORD} -> Hash: ${EXPECTED_HASH}")

        // Act
        boolean matches = Scrypt.check(PASSWORD, EXPECTED_HASH)
        logger.info("Check matches: ${matches}")

        // Assert
        assertTrue(matches)
    }

    @Test
    void testCheckShouldNotValidateIncorrectPassword() throws Exception {
        // Arrange
        final String PASSWORD = "thisIsABadPassword"
        final String EXPECTED_HASH = Scrypt.scrypt(PASSWORD, N, R, P, DK_LEN)
        logger.info("Password: ${PASSWORD} -> Hash: ${EXPECTED_HASH}")

        // Act
        boolean matches = Scrypt.check(PASSWORD.reverse(), EXPECTED_HASH)
        logger.info("Check matches: ${matches}")

        // Assert
        assertFalse(matches)
    }

    @Test
    void testCheckShouldNotAcceptInvalidPassword() throws Exception {
        // Arrange
        final String HASH = '$s0$a0801$abcdefghijklmnopqrstuv$abcdefghijklmnopqrstuv'

        // Even though the spec allows for empty passwords, the JCE does not, so extend enforcement of that to the user boundary
        final def INVALID_PASSWORDS = ['', null]

        // Act
        INVALID_PASSWORDS.each { String invalidPassword ->
            logger.info("Using password: ${invalidPassword}")
            IllegalArgumentException iae = assertThrows(IllegalArgumentException.class,
                    () -> Scrypt.check(invalidPassword, HASH))
            logger.expected(iae.getMessage())

            // Assert
            assertTrue(iae.getMessage().contains("Password cannot be empty"))
        }
    }

    @Test
    void testCheckShouldNotAcceptInvalidHash() throws Exception {
        // Arrange
        final String PASSWORD = "thisIsABadPassword"

        // Even though the spec allows for empty salts, the JCE does not, so extend enforcement of that to the user boundary
        final def INVALID_HASHES = ['', null, '$s0$a0801$', '$s0$a0801$abcdefghijklmnopqrstuv$']

        // Act
        INVALID_HASHES.each { String invalidHash ->
            logger.info("Using hash: ${invalidHash}")
            IllegalArgumentException iae = assertThrows(IllegalArgumentException.class,
                    () -> Scrypt.check(PASSWORD, invalidHash))
            logger.expected(iae.getMessage())

            // Assert
            assertTrue((iae.getMessage() =~ "Hash cannot be empty|Hash is not properly formatted").matches())
        }
    }

    @Test
    void testVerifyHashFormatShouldDetectValidHash() throws Exception {
        // Arrange
        final def VALID_HASHES = [
                "\$s0\$40801\$AAAAAAAAAAAAAAAAAAAAAA\$gLSh7ChbHdOIMvZ74XGjV6qF65d9qvQ8n75FeGnM8YM",
                "\$s0\$40801\$ABCDEFGHIJKLMNOPQRSTUQ\$hxU5g0eH6sRkBqcsiApI8jxvKRT+2QMCenV0GToiMQ8",
                "\$s0\$40801\$eO+UUcKYL2gnpD51QCc+gnywQ7Eg9tZeLMlf0XXr2zc\$99aTTB39TJo69aZCONQmRdyWOgYsDi+1MI+8D0EgMNM",
                "\$s0\$40801\$AAAAAAAAAAAAAAAAAAAAAA\$Gk7K9YmlsWbd8FS7e4RKVWnkg9vlsqYnlD593pJ71gg",
                "\$s0\$40801\$ABCDEFGHIJKLMNOPQRSTUQ\$Ri78VZbrp2cCVmGh2a9Nbfdov8LPnFb49MYyzPCaXmE",
                "\$s0\$40801\$eO+UUcKYL2gnpD51QCc+gnywQ7Eg9tZeLMlf0XXr2zc\$rZIrP2qdIY7LN4CZAMgbCzl3YhXz6WhaNyXJXqFIjaI",
                "\$s0\$40801\$AAAAAAAAAAAAAAAAAAAAAA\$GxH68bGykmPDZ6gaPIGOONOT2omlZ7cd0xlcZ9UsY/0",
                "\$s0\$40801\$ABCDEFGHIJKLMNOPQRSTUQ\$KLGZjWlo59sbCbtmTg5b4k0Nu+biWZRRzhPhN7K5kkI",
                "\$s0\$40801\$eO+UUcKYL2gnpD51QCc+gnywQ7Eg9tZeLMlf0XXr2zc\$6Ql6Efd2ac44ERoV31CL3Q0J3LffNZKN4elyMHux99Y",
                // Uncommon but technically valid
                "\$s0\$F0801\$AAAAAAAAAAA\$A",
                "\$s0\$40801\$ABCDEFGHIJKLMNOPABCDEFGHIJKLMNOPABCDEFGHIJKLMNOPABCDEFGHIJKLMNOP\$A",
                "\$s0\$40801\$ABCDEFGHIJKLMNOPABCDEFGHIJKLMNOPABCDEFGHIJKLMNOPABCDEFGHIJKLMNOP\$ABCDEFGHIJKLMNOPABCDEFGHIJKLMNOPABCDEFGHIJKLMNOPABCDEFGHIJKLMNOP",
                "\$s0\$40801\$ABCDEFGHIJKLMNOPABCDEFGHIJKLMNOPABCDEFGHIJKLMNOPABCDEFGHIJKLMNOP\$" +
                        "ABCDEFGHIJKLMNOPABCDEFGHIJKLMNOPABCDEFGHIJKLMNOPABCDEFGHIJKLMNOPABCDEFGHIJKLMNOPABCDEFGHIJKLMNOPABCDEFGHIJKLMNOPABCDEFGHIJKLMNOP",
                "\$s0\$F0801\$AAAAAAAAAAA\$A",
                "\$s0\$F0801\$AAAAAAAAAAA\$A",
                "\$s0\$F0801\$AAAAAAAAAAA\$A",
                "\$s0\$F0801\$AAAAAAAAAAA\$A",
                "\$s0\$F0801\$AAAAAAAAAAA\$A",
        ]

        // Act
        VALID_HASHES.each { String validHash ->
            logger.info("Using hash: ${validHash}")

            boolean isValidHash = Scrypt.verifyHashFormat(validHash)
            logger.info("Hash is valid: ${isValidHash}")

            // Assert
            assertTrue(isValidHash)
        }
    }

    @Test
    void testVerifyHashFormatShouldDetectInvalidHash() throws Exception {
        // Arrange

        // Even though the spec allows for empty salts, the JCE does not, so extend enforcement of that to the user boundary
        final def INVALID_HASHES = ['', null, '$s0$a0801$', '$s0$a0801$abcdefghijklmnopqrstuv$']

        // Act
        INVALID_HASHES.each { String invalidHash ->
            logger.info("Using hash: ${invalidHash}")

            boolean isValidHash = Scrypt.verifyHashFormat(invalidHash)
            logger.info("Hash is valid: ${isValidHash}")

            // Assert
            assertFalse(isValidHash)
        }
    }
}