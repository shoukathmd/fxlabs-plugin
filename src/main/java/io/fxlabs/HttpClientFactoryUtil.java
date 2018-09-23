package io.fxlabs;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

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
}
