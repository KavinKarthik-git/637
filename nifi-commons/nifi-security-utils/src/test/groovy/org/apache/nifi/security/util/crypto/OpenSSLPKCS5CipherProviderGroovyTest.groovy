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
package org.apache.nifi.security.util.crypto

import org.apache.commons.codec.binary.Hex
import org.apache.nifi.security.util.EncryptionMethod
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.PBEParameterSpec
import java.security.Security

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue
import static org.junit.jupiter.api.Assertions.fail

class OpenSSLPKCS5CipherProviderGroovyTest {
    private static final Logger logger = LoggerFactory.getLogger(OpenSSLPKCS5CipherProviderGroovyTest.class)

    private static List<EncryptionMethod> pbeEncryptionMethods = new ArrayList<>()
    private static List<EncryptionMethod> limitedStrengthPbeEncryptionMethods = new ArrayList<>()

    private static final String PROVIDER_NAME = "BC"
    private static final int ITERATION_COUNT = 0

    @BeforeAll
    static void setUpOnce() throws Exception {
        Security.addProvider(new BouncyCastleProvider())

        pbeEncryptionMethods = EncryptionMethod.values().findAll { it.algorithm.toUpperCase().startsWith("PBE") }
        limitedStrengthPbeEncryptionMethods = pbeEncryptionMethods.findAll { !it.isUnlimitedStrength() }
    }

    private static Cipher getLegacyCipher(String password, byte[] salt, String algorithm) {
        try {
            final PBEKeySpec pbeKeySpec = new PBEKeySpec(password.toCharArray())
            final SecretKeyFactory factory = SecretKeyFactory.getInstance(algorithm, PROVIDER_NAME)
            SecretKey tempKey = factory.generateSecret(pbeKeySpec)

            final PBEParameterSpec parameterSpec = new PBEParameterSpec(salt, ITERATION_COUNT)
            Cipher cipher = Cipher.getInstance(algorithm, PROVIDER_NAME)
            cipher.init(Cipher.ENCRYPT_MODE, tempKey, parameterSpec)
            return cipher
        } catch (Exception e) {
            logger.error("Error generating legacy cipher", e)
            fail(e.getMessage())
        }

        return null
    }

    @Test
    void testGetCipherShouldBeInternallyConsistent() throws Exception {
        // Arrange
        OpenSSLPKCS5CipherProvider cipherProvider = new OpenSSLPKCS5CipherProvider()

        final String PASSWORD = "short"
        final byte[] SALT = Hex.decodeHex("aabbccddeeff0011".toCharArray())

        final String plaintext = "This is a plaintext message."

        // Act
        for (EncryptionMethod em : limitedStrengthPbeEncryptionMethods) {
            logger.info("Using algorithm: {}", em.getAlgorithm())

            if (!CipherUtility.passwordLengthIsValidForAlgorithmOnLimitedStrengthCrypto(PASSWORD.length(), em)) {
                logger.warn("This test is skipped because the password length exceeds the undocumented limit BouncyCastle imposes on a JVM with limited strength crypto policies")
                continue
            }

            // Initialize a cipher for encryption
            Cipher cipher = cipherProvider.getCipher(em, PASSWORD, SALT, true)

            byte[] cipherBytes = cipher.doFinal(plaintext.getBytes("UTF-8"))
            logger.info("Cipher text: {} {}", Hex.encodeHexString(cipherBytes), cipherBytes.length)

            cipher = cipherProvider.getCipher(em, PASSWORD, SALT, false)
            byte[] recoveredBytes = cipher.doFinal(cipherBytes)
            String recovered = new String(recoveredBytes, "UTF-8")

            // Assert
            assertEquals(plaintext, recovered)
        }
    }

    @Test
    void testGetCipherWithUnlimitedStrengthShouldBeInternallyConsistent() throws Exception {
        // Arrange
        OpenSSLPKCS5CipherProvider cipherProvider = new OpenSSLPKCS5CipherProvider()

        final String PASSWORD = "shortPassword"
        final byte[] SALT = Hex.decodeHex("aabbccddeeff0011".toCharArray())

        final String plaintext = "This is a plaintext message."

        // Act
        for (EncryptionMethod em : pbeEncryptionMethods) {
            logger.info("Using algorithm: {}", em.getAlgorithm())

            // Initialize a cipher for encryption
            Cipher cipher = cipherProvider.getCipher(em, PASSWORD, SALT, true)

            byte[] cipherBytes = cipher.doFinal(plaintext.getBytes("UTF-8"))
            logger.info("Cipher text: {} {}", Hex.encodeHexString(cipherBytes), cipherBytes.length)

            cipher = cipherProvider.getCipher(em, PASSWORD, SALT, false)
            byte[] recoveredBytes = cipher.doFinal(cipherBytes)
            String recovered = new String(recoveredBytes, "UTF-8")

            // Assert
            assertEquals(plaintext, recovered)
        }
    }

    @Test
    void testGetCipherShouldSupportLegacyCode() throws Exception {
        // Arrange
        OpenSSLPKCS5CipherProvider cipherProvider = new OpenSSLPKCS5CipherProvider()

        final String PASSWORD = "shortPassword"
        final byte[] SALT = Hex.decodeHex("0011223344556677".toCharArray())

        final String plaintext = "This is a plaintext message."

        // Act
        for (EncryptionMethod em : limitedStrengthPbeEncryptionMethods) {
            logger.info("Using algorithm: {}", em.getAlgorithm())

            if (!CipherUtility.passwordLengthIsValidForAlgorithmOnLimitedStrengthCrypto(PASSWORD.length(), em)) {
                logger.warn("This test is skipped because the password length exceeds the undocumented limit BouncyCastle imposes on a JVM with limited strength crypto policies")
                continue
            }

            // Initialize a legacy cipher for encryption
            Cipher legacyCipher = getLegacyCipher(PASSWORD, SALT, em.getAlgorithm())

            byte[] cipherBytes = legacyCipher.doFinal(plaintext.getBytes("UTF-8"))
            logger.info("Cipher text: {} {}", Hex.encodeHexString(cipherBytes), cipherBytes.length)

            Cipher providedCipher = cipherProvider.getCipher(em, PASSWORD, SALT, false)
            byte[] recoveredBytes = providedCipher.doFinal(cipherBytes)
            String recovered = new String(recoveredBytes, "UTF-8")

            // Assert
            assertEquals(plaintext, recovered)
        }
    }

    @Test
    void testGetCipherWithoutSaltShouldSupportLegacyCode() throws Exception {
        // Arrange
        OpenSSLPKCS5CipherProvider cipherProvider = new OpenSSLPKCS5CipherProvider()

        final String PASSWORD = "short"
        final byte[] SALT = new byte[0]

        final String plaintext = "This is a plaintext message."

        // Act
        for (EncryptionMethod em : limitedStrengthPbeEncryptionMethods) {
            logger.info("Using algorithm: {}", em.getAlgorithm())

            if (!CipherUtility.passwordLengthIsValidForAlgorithmOnLimitedStrengthCrypto(PASSWORD.length(), em)) {
                logger.warn("This test is skipped because the password length exceeds the undocumented limit BouncyCastle imposes on a JVM with limited strength crypto policies")
                continue
            }

            // Initialize a legacy cipher for encryption
            Cipher legacyCipher = getLegacyCipher(PASSWORD, SALT, em.getAlgorithm())

            byte[] cipherBytes = legacyCipher.doFinal(plaintext.getBytes("UTF-8"))
            logger.info("Cipher text: {} {}", Hex.encodeHexString(cipherBytes), cipherBytes.length)

            Cipher providedCipher = cipherProvider.getCipher(em, PASSWORD, false)
            byte[] recoveredBytes = providedCipher.doFinal(cipherBytes)
            String recovered = new String(recoveredBytes, "UTF-8")

            // Assert
             assertEquals(plaintext, recovered)
        }
    }

    @Test
    void testGetCipherShouldIgnoreKeyLength() throws Exception {
        // Arrange
        OpenSSLPKCS5CipherProvider cipherProvider = new OpenSSLPKCS5CipherProvider()

        final String PASSWORD = "shortPassword"
        final byte[] SALT = Hex.decodeHex("aabbccddeeff0011".toCharArray())

        final String plaintext = "This is a plaintext message."

        final def KEY_LENGTHS = [-1, 40, 64, 128, 192, 256]

        // Initialize a cipher for encryption
        EncryptionMethod encryptionMethod = EncryptionMethod.MD5_128AES
        final Cipher cipher128 = cipherProvider.getCipher(encryptionMethod, PASSWORD, SALT, true)
        byte[] cipherBytes = cipher128.doFinal(plaintext.getBytes("UTF-8"))
        logger.info("Cipher text: {} {}", Hex.encodeHexString(cipherBytes), cipherBytes.length)

        // Act
        KEY_LENGTHS.each { int keyLength ->
            logger.info("Decrypting with 'requested' key length: ${keyLength}")

            Cipher cipher = cipherProvider.getCipher(encryptionMethod, PASSWORD, SALT, keyLength, false)
            byte[] recoveredBytes = cipher.doFinal(cipherBytes)
            String recovered = new String(recoveredBytes, "UTF-8")

            // Assert
             assertEquals(plaintext, recovered)
        }
    }

    @Test
    void testGetCipherShouldRequireEncryptionMethod() throws Exception {
        // Arrange
        OpenSSLPKCS5CipherProvider cipherProvider = new OpenSSLPKCS5CipherProvider()

        final String PASSWORD = "shortPassword"
        final byte[] SALT = Hex.decodeHex("0011223344556677".toCharArray())

        // Act
        logger.info("Using algorithm: null")

        IllegalArgumentException iae = assertThrows(IllegalArgumentException.class,
                () -> cipherProvider.getCipher(null, PASSWORD, SALT, false))

        // Assert
        assertTrue(iae.getMessage().contains("The encryption method must be specified"))
    }

    @Test
    void testGetCipherShouldRequirePassword() throws Exception {
        // Arrange
        OpenSSLPKCS5CipherProvider cipherProvider = new OpenSSLPKCS5CipherProvider()

        final byte[] SALT = Hex.decodeHex("0011223344556677".toCharArray())
        EncryptionMethod encryptionMethod = EncryptionMethod.MD5_128AES

        // Act
        logger.info("Using algorithm: ${encryptionMethod}")

        IllegalArgumentException iae = assertThrows(IllegalArgumentException.class,
                () ->  cipherProvider.getCipher(encryptionMethod, "", SALT, false))

        // Assert
        assertTrue(iae.getMessage().contains("Encryption with an empty password is not supported"))
    }

    @Test
    void testGetCipherShouldValidateSaltLength() throws Exception {
        // Arrange
        OpenSSLPKCS5CipherProvider cipherProvider = new OpenSSLPKCS5CipherProvider()

        final String PASSWORD = "shortPassword"
        final byte[] SALT = Hex.decodeHex("00112233445566".toCharArray())
        EncryptionMethod encryptionMethod = EncryptionMethod.MD5_128AES

        // Act
        logger.info("Using algorithm: ${encryptionMethod}")
        IllegalArgumentException iae = assertThrows(IllegalArgumentException.class,
                () -> cipherProvider.getCipher(encryptionMethod, PASSWORD, SALT, false))

        // Assert
        assertTrue(iae.getMessage().contains("Salt must be 8 bytes US-ASCII encoded"))
    }

    @Test
    void testGenerateSaltShouldProvideValidSalt() throws Exception {
        // Arrange
        PBECipherProvider cipherProvider = new OpenSSLPKCS5CipherProvider()

        // Act
        byte[] salt = cipherProvider.generateSalt()
        logger.info("Checking salt ${Hex.encodeHexString(salt)}")

        // Assert
        assertEquals(cipherProvider.getDefaultSaltLength(), salt.length)
        byte [] notExpected = new byte [cipherProvider.defaultSaltLength]
        Arrays.fill(notExpected, 0x00 as byte)
        assertFalse(Arrays.equals(notExpected, salt))
    }
}