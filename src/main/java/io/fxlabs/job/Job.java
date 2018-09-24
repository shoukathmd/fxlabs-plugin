package io.fxlabs.job;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fxlabs.job.AuthBuilder;
import io.fxlabs.job.HttpClientFactoryUtil;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.net.URL;

import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.TrustStrategy;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.springframework.http.*;

import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import javax.net.ssl.*;
import java.io.IOException;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import java.util.concurrent.TimeUnit;

/**
 * @author Mohammed Shoukath Ali
 */


public class Job
        extends DefaultTask {


    private String jobId;
    private String region;
    private String host;
    private String username;
    private String password;

    private static final String HOST_URL = "https://cloud.io.fxlabs.io";

    private static final String UAT_HOST_URL = "http://13.56.210.25";

    private static final String LOCAL_HOST_URL = "http://localhost:8080";

    private static final String RUN_JOB_API_ENDPOINT = "/api/v1/runs/job/";

    private static final String RUN_STATUS_JOB_API_ENDPOINT = "/api/v1/runs/";

//    public Job(String jobId, String username, String password, String region, String host) {
//        this.jobId = jobId;
//        this.username = username;
//        this.password = password;
//        this.region = region;
//        this.host = host;
//        if (StringUtils.isEmpty(host)) {
//            this.host = HOST_URL;
//        }
//    }

    @TaskAction
    public void fxjob() {
        try {
            System.out.println("Username : " + username);
            System.out.println("Job Id : " + jobId);

            if (!org.apache.commons.lang3.StringUtils.isEmpty(region)) {
                System.out.println("Region : " + region);
            }

            String hostUrl = HOST_URL;

            String jobRunUrl = host + RUN_JOB_API_ENDPOINT + jobId + "?region=" + region;
            String runStatusUrl = host + RUN_STATUS_JOB_API_ENDPOINT;

            RestTemplate restTemplate = new RestTemplate(HttpClientFactoryUtil.httpComponentsClientHttpRequestFactory());

            HttpHeaders httpHeaders = new HttpHeaders();

            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            httpHeaders.set("Accept", "application/json");


            httpHeaders.set("Authorization", AuthBuilder.createBasicAuth(username, password));

            HttpEntity<String> request = new HttpEntity<>("", httpHeaders);

            ResponseEntity<String> response = null;
            int statusCode = -1;
            String responseBody = null;
            HttpHeaders headers = null;

            System.out.println("calling " + jobRunUrl);
            response = restTemplate.exchange(jobRunUrl, HttpMethod.POST, request, String.class);

            String runId = getByKeyId(response.getBody(), "id", "");

            if (org.apache.commons.lang3.StringUtils.isEmpty(runId)) {
                throw new Exception("Invalid runid." + runId);
            }
            System.out.println("Run Id : " + runId);

            String status = getByKeyId(response.getBody(), "task", "status");

            if (org.apache.commons.lang3.StringUtils.isEmpty(status)) {
                throw new GradleException("Invalid status " + status);
            }

            System.out.println("status : " + status);

            do {
                try {
                    //getLog().info("Waiting for 5 secs ........");
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                System.out.println("Checking status ........");
                response = restTemplate.exchange(runStatusUrl + runId, HttpMethod.GET, request, String.class);
                String ciCdStatus = getByKeyId(response.getBody(), "ciCdStatus", "");

                if (org.apache.commons.lang3.StringUtils.isEmpty(ciCdStatus)) {
                    throw new GradleException("Failed to get status.");
                }

                String[] dataArray = ciCdStatus.split(":");

                status = dataArray[0];

                if (org.apache.commons.lang3.StringUtils.isEmpty(status)) {
                    throw new GradleException("Invalid status." + status);
                }

                System.out.println("Run Status: " + status + ".....");

                if ("COMPLETED".equalsIgnoreCase(status)) {
                    printStatus(dataArray);
                }

                if ("FAIL".equalsIgnoreCase(status)) {
                    System.out.println("Reason for Failure : " + dataArray[1]);
                }


                if ("TIMEOUT".equalsIgnoreCase(status)) {
                    printStatus(dataArray);

                    throw new GradleException("Job timeout.....");

                }


            } while ("WAITING".equalsIgnoreCase(status) || "PROCESSING".equalsIgnoreCase(status));

        } catch (HttpStatusCodeException statusCodeException) {
            throw new GradleException(statusCodeException.toString() + statusCodeException.getResponseBodyAsString() + statusCodeException.getResponseHeaders());
        } catch (Exception e) {
            throw new GradleException(e.getLocalizedMessage());
        }

    }

    private void printStatus(String[] dataArray) {

        System.out.println("----------------------------------------");
        System.out.println("Run detail's link " + host + dataArray[6]);
        System.out.println("----------------------------------------");
        System.out.println(dataArray[7]);
        System.out.println("----------------------------------------");
        System.out.println("Run No : " + dataArray[5]);
        System.out.println("----------------------------------------");
        System.out.println("Success : " + dataArray[1] + " % " + " -- Total Tests : " + dataArray[2] + " -- Failed Tests : " + dataArray[3]);
        System.out.println("----------------------------------------");
    }


    public String getByKeyId(String value, String key1, String key2) {

        if (org.apache.commons.lang3.StringUtils.isEmpty(value)) {
            return null;
        }

        ObjectMapper mapper = new ObjectMapper();
        //  jsonMapper.configure(DeserializationConfig.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)

        try {
            JsonNode rootNode = mapper.readTree(value);

            JsonNode nameNode = null;

            if (org.apache.commons.lang3.StringUtils.isEmpty(key2) && !org.apache.commons.lang3.StringUtils.isEmpty(key1)) {
                nameNode = rootNode.path("data").path(key1);
            } else if (!org.apache.commons.lang3.StringUtils.isEmpty(key2) && !org.apache.commons.lang3.StringUtils.isEmpty(key1)) {
                nameNode = rootNode.path("data").path(key1).path(key2);
            }

            return nameNode.textValue();
        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
        }
        return null;
    }

    public static void disableChecks() throws NoSuchAlgorithmException,
            KeyManagementException {

        try {
            new URL("https://0.0.0.0/").getContent();
        } catch (IOException e) {
            // This invocation will always fail, but it will register the
            // default SSL provider to the URL class.
        }

        try {
            SSLContext sslc;

            sslc = SSLContext.getInstance("TLS");

            TrustManager[] trustManagerArray = {new NullX509TrustManager()};
            sslc.init(null, trustManagerArray, null);

            HttpsURLConnection.setDefaultSSLSocketFactory(sslc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(new NullHostnameVerifier());

        } catch (Exception e) {
            e.printStackTrace();
        }


    }


    private static class NullX509TrustManager implements X509TrustManager {
        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            System.out.println();
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            System.out.println();
        }

        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

    /**
     * Host name verifier that does not perform nay checks.
     */
    private static class NullHostnameVerifier implements HostnameVerifier {
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }

    }



    @Input
    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    @Input
    public String getRegion() {
        return region;
    }


    public void setRegion(String region) {
        this.region = region;
    }

    @Input
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    @Input
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Input
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }




}
