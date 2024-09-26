package co.com.bancolombia.api.broadcast;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.SneakyThrows;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.pkcs.RSAPrivateKey;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.util.io.pem.PemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.KeyManagerFactory;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;

import static io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Component
public class BroadcastWebClientConfig {

    private final String certChainPath;
    private final String keyPath;
    private final int timeout;

    public BroadcastWebClientConfig(
            @Value("${broadcast.cert-chain}") String certChainPath, @Value("${broadcast.key}") String keyPath,
            @Value("${broadcast.timeout}") int timeout) {
        this.certChainPath = certChainPath;
        this.keyPath = keyPath;
        this.timeout = timeout;
    }

    public WebClient broadcastWebClient() {
        SslContext sslContext = getSslContext(certChainPath, keyPath);

        ReactorClientHttpConnector connector = new ReactorClientHttpConnector(HttpClient.create()
                .secure(sslContextSpec -> sslContextSpec.sslContext(sslContext))
                .compress(true)
                .keepAlive(true).option(CONNECT_TIMEOUT_MILLIS, timeout)
                .doOnConnected(connection -> {
                    connection.addHandlerLast(new ReadTimeoutHandler(timeout, MILLISECONDS));
                    connection.addHandlerLast(new WriteTimeoutHandler(timeout, MILLISECONDS));
                }));

        return WebClient.builder()
                .clientConnector(connector)
                .build();
    }

    @SneakyThrows
    private static SslContext getSslContext(String certChainPath, String keyPath) {
        X509Certificate cert = getX509Certificate(certChainPath);
        PrivateKey privateKey = convertPKCS1ToPKCS8(keyPath);

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        keyStore.setKeyEntry("key", privateKey, new char[0], new Certificate[]{cert});

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, new char[0]);

        return SslContextBuilder.forClient()
                .keyManager(kmf)
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();
    }

    @SneakyThrows
    private static X509Certificate getX509Certificate(String certChainPath) {
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        try (InputStream certChainInputStream = new FileInputStream(Paths.get(certChainPath).toFile())) {
            return (X509Certificate) certFactory.generateCertificate(certChainInputStream);
        }
    }

    @SneakyThrows
    private static PrivateKey convertPKCS1ToPKCS8(String pkcs1Path) {
        try (PemReader pemReader = new PemReader(new FileReader(pkcs1Path))) {
            PrivateKeyInfo privateKeyInfo = new PrivateKeyInfo(
                    new AlgorithmIdentifier(PKCSObjectIdentifiers.rsaEncryption),
                    RSAPrivateKey.getInstance(pemReader.readPemObject().getContent()));

            return KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(privateKeyInfo.getEncoded()));
        }
    }
}
