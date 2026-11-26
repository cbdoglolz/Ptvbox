package com.cbdog.ptvbox

import android.content.Context
import java.io.ByteArrayInputStream
import java.math.BigInteger
import java.security.*
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder

/** Per-install TLS identity, encrypted at rest by the existing Android Keystore-backed store.
 * Use an ordinary TLS signing key: some Android emulator native bridges cannot wrap a
 * non-exportable AndroidKeyStore RSA handle in their server-side Conscrypt implementation.
 */
object LocalTlsIdentity {
    @Synchronized fun get(context:Context):Pair<PrivateKey,X509Certificate> {
        val store=SecretStore(context);var bundle=store.read("local_https_identity_v1")
        if(bundle.isBlank()){
            val random=SecureRandom();val pair=KeyPairGenerator.getInstance("RSA").apply{initialize(2048,random)}.generateKeyPair()
            val name=X500Name("CN=Ptvbox local settings")
            val cert=JcaX509v3CertificateBuilder(name,BigInteger(128,random),Date(System.currentTimeMillis()-86400000),Date(System.currentTimeMillis()+3650L*86400000),name,pair.public).build(JcaContentSignerBuilder("SHA256withRSA").build(pair.private))
            bundle=Base64.getEncoder().encodeToString(pair.private.encoded)+"."+Base64.getEncoder().encodeToString(cert.encoded)
            store.save("local_https_identity_v1",bundle)
        }
        val parts=bundle.split('.');check(parts.size==2)
        val key=KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(Base64.getDecoder().decode(parts[0])))
        val cert=CertificateFactory.getInstance("X.509").generateCertificate(ByteArrayInputStream(Base64.getDecoder().decode(parts[1]))) as X509Certificate
        cert.checkValidity();cert.verify(cert.publicKey)
        return key to cert
    }
}
