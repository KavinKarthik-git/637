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
package org.apache.nifi.security.util;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.net.ssl.SSLException;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DLSequence;
import org.bouncycastle.asn1.pkcs.Attribute;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.AttributeTypeAndValue;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CertificateUtils {
    private static final Logger logger = LoggerFactory.getLogger(CertificateUtils.class);
    private static final Map<ASN1ObjectIdentifier, Integer> dnOrderMap = createDnOrderMap();

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * The time in milliseconds that the last unique serial number was generated
     */
    private static long lastSerialNumberMillis = 0L;

    /**
     * An incrementor to add uniqueness to serial numbers generated in the same millisecond
     */
    private static int serialNumberIncrementor = 0;

    /**
     * BigInteger value to use for the base of the unique serial number
     */
    private static BigInteger millisecondBigInteger;

    private static Map<ASN1ObjectIdentifier, Integer> createDnOrderMap() {
        Map<ASN1ObjectIdentifier, Integer> orderMap = new HashMap<>();
        int count = 0;
        orderMap.put(BCStyle.CN, count++);
        orderMap.put(BCStyle.L, count++);
        orderMap.put(BCStyle.ST, count++);
        orderMap.put(BCStyle.O, count++);
        orderMap.put(BCStyle.OU, count++);
        orderMap.put(BCStyle.C, count++);
        orderMap.put(BCStyle.STREET, count++);
        orderMap.put(BCStyle.DC, count++);
        orderMap.put(BCStyle.UID, count++);
        return Collections.unmodifiableMap(orderMap);
    }

    /**
     * Extracts the username from the specified DN. If the username cannot be extracted because the CN is in an unrecognized format, the entire CN is returned. If the CN cannot be extracted because
     * the DN is in an unrecognized format, the entire DN is returned.
     *
     * @param dn the dn to extract the username from
     * @return the exatracted username
     */
    public static String extractUsername(String dn) {
        String username = dn;

        // ensure the dn is specified
        if (StringUtils.isNotBlank(dn)) {
            // determine the separate
            final String separator = StringUtils.indexOfIgnoreCase(dn, "/cn=") > 0 ? "/" : ",";

            // attempt to locate the cd
            final String cnPattern = "cn=";
            final int cnIndex = StringUtils.indexOfIgnoreCase(dn, cnPattern);
            if (cnIndex >= 0) {
                int separatorIndex = StringUtils.indexOf(dn, separator, cnIndex);
                if (separatorIndex > 0) {
                    username = StringUtils.substring(dn, cnIndex + cnPattern.length(), separatorIndex);
                } else {
                    username = StringUtils.substring(dn, cnIndex + cnPattern.length());
                }
            }

            /*
                https://tools.ietf.org/html/rfc5280#section-4.1.2.6

                Legacy implementations exist where an electronic mail address is
                embedded in the subject distinguished name as an emailAddress
                attribute [RFC2985].  The attribute value for emailAddress is of type
                IA5String to permit inclusion of the character '@', which is not part
                of the PrintableString character set.  emailAddress attribute values
                are not case-sensitive (e.g., "subscriber@example.com" is the same as
                "SUBSCRIBER@EXAMPLE.COM").
             */
            final String emailPattern = "/emailAddress=";
            final int index = StringUtils.indexOfIgnoreCase(username, emailPattern);
            if (index >= 0) {
                String[] dnParts = username.split(emailPattern);
                if (dnParts.length > 0) {
                    // only use the actual CN
                    username = dnParts[0];
                }
            }
        }

        return username;
    }

    /**
     * Returns a list of subject alternative names. Any name that is represented as a String by X509Certificate.getSubjectAlternativeNames() is converted to lowercase and returned.
     *
     * @param certificate a certificate
     * @return a list of subject alternative names; list is never null
     * @throws CertificateParsingException if parsing the certificate failed
     */
    public static List<String> getSubjectAlternativeNames(final X509Certificate certificate) throws CertificateParsingException {

        final Collection<List<?>> altNames = certificate.getSubjectAlternativeNames();
        if (altNames == null) {
            return new ArrayList<>();
        }

        final List<String> result = new ArrayList<>();
        for (final List<?> generalName : altNames) {
            /**
             * generalName has the name type as the first element a String or byte array for the second element. We return any general names that are String types.
             *
             * We don't inspect the numeric name type because some certificates incorrectly put IPs and DNS names under the wrong name types.
             */
            final Object value = generalName.get(1);
            if (value instanceof String) {
                result.add(((String) value).toLowerCase());
            }

        }

        return result;
    }

    /**
     * Accepts an abstract {@link java.security.cert.Certificate} and returns an {@link X509Certificate}. Because {@code sslSocket.getSession().getPeerCertificates()} returns an array of the
     * abstract certificates, they must be translated to X.509 to replace the functionality of {@code sslSocket.getSession().getPeerCertificateChain()}.
     *
     * @param abstractCertificate the {@code java.security.cert.Certificate}
     * @return a new {@code java.security.cert.X509Certificate}
     * @throws CertificateException if there is an error generating the new certificate
     */
    public static X509Certificate convertAbstractX509Certificate(java.security.cert.Certificate abstractCertificate) throws CertificateException {
        if (abstractCertificate == null || !(abstractCertificate instanceof X509Certificate)) {
            throw new IllegalArgumentException("The certificate cannot be null and must be an X.509 certificate");
        }

        try {
            return formX509Certificate(abstractCertificate.getEncoded());
        } catch (java.security.cert.CertificateEncodingException e) {
            throw new CertificateException(e);
        }
    }

    private static X509Certificate formX509Certificate(byte[] encodedCertificate) throws CertificateException {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            ByteArrayInputStream bais = new ByteArrayInputStream(encodedCertificate);
            return (X509Certificate) cf.generateCertificate(bais);
        } catch (CertificateException e) {
            logger.error("Error converting the certificate", e);
            throw e;
        }
    }

    /**
     * Reorders DN to the order the elements appear in the RFC 2253 table
     * <p>
     * https://www.ietf.org/rfc/rfc2253.txt
     * <p>
     * String  X.500 AttributeType
     * ------------------------------
     * CN      commonName
     * L       localityName
     * ST      stateOrProvinceName
     * O       organizationName
     * OU      organizationalUnitName
     * C       countryName
     * STREET  streetAddress
     * DC      domainComponent
     * UID     userid
     *
     * @param dn a possibly unordered DN
     * @return the ordered dn
     */
    public static String reorderDn(String dn) {
        RDN[] rdNs = new X500Name(dn).getRDNs();
        Arrays.sort(rdNs, new Comparator<RDN>() {
            @Override
            public int compare(RDN o1, RDN o2) {
                AttributeTypeAndValue o1First = o1.getFirst();
                AttributeTypeAndValue o2First = o2.getFirst();

                ASN1ObjectIdentifier o1Type = o1First.getType();
                ASN1ObjectIdentifier o2Type = o2First.getType();

                Integer o1Rank = dnOrderMap.get(o1Type);
                Integer o2Rank = dnOrderMap.get(o2Type);
                if (o1Rank == null) {
                    if (o2Rank == null) {
                        int idComparison = o1Type.getId().compareTo(o2Type.getId());
                        if (idComparison != 0) {
                            return idComparison;
                        }
                        return String.valueOf(o1Type).compareTo(String.valueOf(o2Type));
                    }
                    return 1;
                } else if (o2Rank == null) {
                    return -1;
                }
                return o1Rank - o2Rank;
            }
        });
        return new X500Name(rdNs).toString();
    }

    /**
     * Reverses the X500Name in order make the certificate be in the right order
     * [see http://stackoverflow.com/questions/7567837/attributes-reversed-in-certificate-subject-and-issuer/12645265]
     *
     * @param x500Name the X500Name created with the intended order
     * @return the X500Name reversed
     */
    private static X500Name reverseX500Name(X500Name x500Name) {
        List<RDN> rdns = Arrays.asList(x500Name.getRDNs());
        Collections.reverse(rdns);
        return new X500Name(rdns.toArray(new RDN[rdns.size()]));
    }

    /**
     * Generates a unique serial number by using the current time in milliseconds left shifted 32 bits (to make room for incrementor) with an incrementor added
     *
     * @return a unique serial number (technically unique to this classloader)
     */
    protected static synchronized BigInteger getUniqueSerialNumber() {
        final long currentTimeMillis = System.currentTimeMillis();
        final int incrementorValue;

        if (lastSerialNumberMillis != currentTimeMillis) {
            // We can only get into this block once per millisecond
            millisecondBigInteger = BigInteger.valueOf(currentTimeMillis).shiftLeft(32);
            lastSerialNumberMillis = currentTimeMillis;
            incrementorValue = 0;
            serialNumberIncrementor = 1;
        } else {
            // Already created at least one serial number this millisecond
            incrementorValue = serialNumberIncrementor++;
        }

        return millisecondBigInteger.add(BigInteger.valueOf(incrementorValue));
    }

    /**
     * Generates a self-signed {@link X509Certificate} suitable for use as a Certificate Authority.
     *
     * @param keyPair                 the {@link KeyPair} to generate the {@link X509Certificate} for
     * @param dn                      the distinguished name to user for the {@link X509Certificate}
     * @param signingAlgorithm        the signing algorithm to use for the {@link X509Certificate}
     * @param certificateDurationDays the duration in days for which the {@link X509Certificate} should be valid
     * @return a self-signed {@link X509Certificate} suitable for use as a Certificate Authority
     * @throws CertificateException if there is an generating the new certificate
     */
    public static X509Certificate generateSelfSignedX509Certificate(KeyPair keyPair, String dn, String signingAlgorithm, int certificateDurationDays)
            throws CertificateException {
        return generateSelfSignedX509Certificate(keyPair, dn, signingAlgorithm, certificateDurationDays, null);
    }

    /**
     * Generates a self-signed {@link X509Certificate} suitable for use as a Certificate Authority.
     *
     * @param keyPair                 the {@link KeyPair} to generate the {@link X509Certificate} for
     * @param dn                      the distinguished name to user for the {@link X509Certificate}
     * @param signingAlgorithm        the signing algorithm to use for the {@link X509Certificate}
     * @param certificateDurationDays the duration in days for which the {@link X509Certificate} should be valid
     * @param dnsSubjectAlternativeNames An optional array of dnsName SANs
     * @return a self-signed {@link X509Certificate} suitable for use as a Certificate Authority
     * @throws CertificateException if there is an generating the new certificate
     */
    public static X509Certificate generateSelfSignedX509Certificate(KeyPair keyPair, String dn, String signingAlgorithm, int certificateDurationDays,
                                                                    String[] dnsSubjectAlternativeNames)
            throws CertificateException {
        try {
            ContentSigner sigGen = new JcaContentSignerBuilder(signingAlgorithm).setProvider(BouncyCastleProvider.PROVIDER_NAME).build(keyPair.getPrivate());
            SubjectPublicKeyInfo subPubKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());
            Date startDate = new Date();
            Date endDate = new Date(startDate.getTime() + TimeUnit.DAYS.toMillis(certificateDurationDays));

            X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(
                    reverseX500Name(new X500Name(dn)),
                    getUniqueSerialNumber(),
                    startDate, endDate,
                    reverseX500Name(new X500Name(dn)),
                    subPubKeyInfo);

            // Set certificate extensions
            // (1) digitalSignature extension
            certBuilder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment | KeyUsage.dataEncipherment
                    | KeyUsage.keyAgreement | KeyUsage.nonRepudiation | KeyUsage.cRLSign | KeyUsage.keyCertSign));

            certBuilder.addExtension(Extension.basicConstraints, false, new BasicConstraints(true));

            certBuilder.addExtension(Extension.subjectKeyIdentifier, false, new JcaX509ExtensionUtils().createSubjectKeyIdentifier(keyPair.getPublic()));

            certBuilder.addExtension(Extension.authorityKeyIdentifier, false, new JcaX509ExtensionUtils().createAuthorityKeyIdentifier(keyPair.getPublic()));

            // (2) extendedKeyUsage extension
            certBuilder.addExtension(Extension.extendedKeyUsage, false, new ExtendedKeyUsage(new KeyPurposeId[]{KeyPurposeId.id_kp_clientAuth, KeyPurposeId.id_kp_serverAuth}));

            // (3) subjectAlternativeName extension. Include CN as a SAN entry if it exists.
            final String cn = getCommonName(dn);
            List<GeneralName> generalNames = new ArrayList<>();
            if (StringUtils.isNotBlank(cn)) {
                generalNames.add(new GeneralName(GeneralName.dNSName, cn));
            }
            if (dnsSubjectAlternativeNames != null) {
                for (String subjectAlternativeName : dnsSubjectAlternativeNames) {
                    if (StringUtils.isNotBlank(subjectAlternativeName)) {
                        generalNames.add(new GeneralName(GeneralName.dNSName, subjectAlternativeName));
                    }
                }
            }
            if (!generalNames.isEmpty()) {
                certBuilder.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(generalNames.toArray(
                        new GeneralName[generalNames.size()])));
            }

            // Sign the certificate
            X509CertificateHolder certificateHolder = certBuilder.build(sigGen);
            return new JcaX509CertificateConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME).getCertificate(certificateHolder);
        } catch (CertIOException | NoSuchAlgorithmException | OperatorCreationException e) {
            throw new CertificateException(e);
        }
    }

    /**
     * Generates an issued {@link X509Certificate} from the given issuer certificate and {@link KeyPair}
     *
     * @param dn               the distinguished name to use
     * @param publicKey        the public key to issue the certificate to
     * @param issuer           the issuer's certificate
     * @param issuerKeyPair    the issuer's keypair
     * @param signingAlgorithm the signing algorithm to use
     * @param days             the number of days it should be valid for
     * @return an issued {@link X509Certificate} from the given issuer certificate and {@link KeyPair}
     * @throws CertificateException if there is an error issuing the certificate
     */
    public static X509Certificate generateIssuedCertificate(String dn, PublicKey publicKey, X509Certificate issuer, KeyPair issuerKeyPair, String signingAlgorithm, int days)
            throws CertificateException {
        return generateIssuedCertificate(dn, publicKey, null, issuer, issuerKeyPair, signingAlgorithm, days);
    }

    /**
     * Generates an issued {@link X509Certificate} from the given issuer certificate and {@link KeyPair}
     *
     * @param dn               the distinguished name to use
     * @param publicKey        the public key to issue the certificate to
     * @param extensions       extensions extracted from the CSR
     * @param issuer           the issuer's certificate
     * @param issuerKeyPair    the issuer's keypair
     * @param signingAlgorithm the signing algorithm to use
     * @param days             the number of days it should be valid for
     * @return an issued {@link X509Certificate} from the given issuer certificate and {@link KeyPair}
     * @throws CertificateException if there is an error issuing the certificate
     */
    public static X509Certificate generateIssuedCertificate(String dn, PublicKey publicKey, Extensions extensions, X509Certificate issuer, KeyPair issuerKeyPair, String signingAlgorithm, int days)
            throws CertificateException {
        try {
            ContentSigner sigGen = new JcaContentSignerBuilder(signingAlgorithm).setProvider(BouncyCastleProvider.PROVIDER_NAME).build(issuerKeyPair.getPrivate());
            SubjectPublicKeyInfo subPubKeyInfo = SubjectPublicKeyInfo.getInstance(publicKey.getEncoded());
            Date startDate = new Date();
            Date endDate = new Date(startDate.getTime() + TimeUnit.DAYS.toMillis(days));

            X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(
                    reverseX500Name(new X500Name(issuer.getSubjectX500Principal().getName())),
                    getUniqueSerialNumber(),
                    startDate, endDate,
                    reverseX500Name(new X500Name(dn)),
                    subPubKeyInfo);

            certBuilder.addExtension(Extension.subjectKeyIdentifier, false, new JcaX509ExtensionUtils().createSubjectKeyIdentifier(publicKey));

            certBuilder.addExtension(Extension.authorityKeyIdentifier, false, new JcaX509ExtensionUtils().createAuthorityKeyIdentifier(issuerKeyPair.getPublic()));
            // Set certificate extensions
            // (1) digitalSignature extension
            certBuilder.addExtension(Extension.keyUsage, true,
                    new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment | KeyUsage.dataEncipherment | KeyUsage.keyAgreement | KeyUsage.nonRepudiation));

            certBuilder.addExtension(Extension.basicConstraints, false, new BasicConstraints(false));

            // (2) extendedKeyUsage extension
            certBuilder.addExtension(Extension.extendedKeyUsage, false, new ExtendedKeyUsage(new KeyPurposeId[]{KeyPurposeId.id_kp_clientAuth, KeyPurposeId.id_kp_serverAuth}));

            // (3) subjectAlternativeName
            if (extensions != null && extensions.getExtension(Extension.subjectAlternativeName) != null) {
                certBuilder.addExtension(Extension.subjectAlternativeName, false, extensions.getExtensionParsedValue(Extension.subjectAlternativeName));
            }

            X509CertificateHolder certificateHolder = certBuilder.build(sigGen);
            return new JcaX509CertificateConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME).getCertificate(certificateHolder);
        } catch (CertIOException | NoSuchAlgorithmException | OperatorCreationException e) {
            throw new CertificateException(e);
        }
    }

    /**
     * Returns true if the two provided DNs are equivalent, regardless of the order of the elements. Returns false if one or both are invalid DNs.
     * <p>
     * Example:
     * <p>
     * CN=test1, O=testOrg, C=US compared to CN=test1, O=testOrg, C=US -> true
     * CN=test1, O=testOrg, C=US compared to O=testOrg, CN=test1, C=US -> true
     * CN=test1, O=testOrg, C=US compared to CN=test2, O=testOrg, C=US -> false
     * CN=test1, O=testOrg, C=US compared to O=testOrg, CN=test2, C=US -> false
     * CN=test1, O=testOrg, C=US compared to                           -> false
     * compared to                           -> true
     *
     * @param dn1 the first DN to compare
     * @param dn2 the second DN to compare
     * @return true if the DNs are equivalent, false otherwise
     */
    public static boolean compareDNs(String dn1, String dn2) {
        if (dn1 == null) {
            dn1 = "";
        }

        if (dn2 == null) {
            dn2 = "";
        }

        if (StringUtils.isEmpty(dn1) || StringUtils.isEmpty(dn2)) {
            return dn1.equals(dn2);
        }
        try {
            List<Rdn> rdn1 = new LdapName(dn1).getRdns();
            List<Rdn> rdn2 = new LdapName(dn2).getRdns();

            return rdn1.size() == rdn2.size() && rdn1.containsAll(rdn2);
        } catch (InvalidNameException e) {
            logger.warn("Cannot compare DNs: {} and {} because one or both is not a valid DN", dn1, dn2);
            return false;
        }
    }

    /**
     * Extract extensions from CSR object
     */
    public static Extensions getExtensionsFromCSR(JcaPKCS10CertificationRequest csr) {
        Attribute[] attributess = csr.getAttributes(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest);
        for (Attribute attribute : attributess) {
            ASN1Set attValue = attribute.getAttrValues();
            if (attValue != null) {
                ASN1Encodable extension = attValue.getObjectAt(0);
                if (extension instanceof Extensions) {
                    return (Extensions) extension;
                } else if (extension instanceof DERSequence || extension instanceof DLSequence) {
                    return Extensions.getInstance(extension);
                }
            }
        }
        return null;
    }

    /**
     * Returns {@code true} if this exception is due to a TLS problem (either directly or because of its cause, if present). Traverses the cause chain recursively.
     *
     * @param e the exception to evaluate
     * @return true if the direct or indirect cause of this exception was TLS-related
     */
    public static boolean isTlsError(Throwable e) {
        if (e == null) {
            return false;
        } else {
            if (e instanceof CertificateException || e instanceof TlsException || e instanceof SSLException) {
                return true;
            } else if (e.getCause() != null) {
                return isTlsError(e.getCause());
            } else {
                return false;
            }
        }
    }

    /**
     * Extracts the common name from the given DN.
     *
     * @param dn the distinguished name to evaluate
     * @return the common name if it exists, null otherwise.
     */
    public static String getCommonName(final String dn) {
        RDN[] rdns = new X500Name(dn).getRDNs(BCStyle.CN);
        if (rdns.length == 0) {
            return null;
        }
        return  IETFUtils.valueToString(rdns[0].getFirst().getValue());
    }

    private CertificateUtils() {
    }
}
