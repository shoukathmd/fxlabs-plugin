package io.fxlabs.job;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.TrustStrategy;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

public class HttpClientFactoryUtil {

    private static HttpComponentsClientHttpRequestFactory FACTORY = null;

    public static final HttpComponentsClientHttpRequestFactory getInstance() {

        if (FACTORY != null) {
            return  FACTORY;
        }

        CloseableHttpClient httpClient
                = HttpClients.custom()
                .setSSLHostnameVerifier(new NoopHostnameVerifier())
                .build();

        FACTORY = new HttpComponentsClientHttpRequestFactory();

        FACTORY.setHttpClient(httpClient);

        int timeout = 15000;
        FACTORY.setConnectTimeout(timeout);
        FACTORY.setConnectionRequestTimeout(timeout);
        FACTORY.setReadTimeout(timeout);

        return FACTORY;
    }

    public static HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;

        SSLContext sslContext = org.apache.http.ssl.SSLContexts.custom()
                .loadTrustMaterial(null, acceptingTrustStrategy)
                .build();

        SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext);

        CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLSocketFactory(csf)
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory =
                new HttpComponentsClientHttpRequestFactory();

        requestFactory.setHttpClient(httpClient);
        // RestTemplate restTemplate = new RestTemplate(requestFactory);
        return requestFactory;
    }
}
