package io.fxlabs;

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
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.TaskAction;
import org.gradle.internal.impldep.org.apache.commons.lang.StringUtils;
import org.springframework.http.*;

import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;
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

    @Inject
    Job(String jobId, String username, String password, String region, String host) {
        this.jobId = jobId;
        this.username = username;
        this.password = password;
        this.region = region;
        this.host = host;
        if (StringUtils.isEmpty(host)) {
            this.host = HOST_URL;
        }
    }

    @TaskAction
    public void invoke() {
       System.out.println("Username : " + username);
        System.out.println("Job Id : " + jobId);

        if (!StringUtils.isEmpty(region)) {
            System.out.println("Region : " + region);
        }

        String hostUrl = HOST_URL;

        String jobRunUrl = host + RUN_JOB_API_ENDPOINT + jobId + "?region=" + region;
        String runStatusUrl = host + RUN_STATUS_JOB_API_ENDPOINT;

        RestTemplate restTemplate = new RestTemplate(HttpClientFactoryUtil.getInstance());
        HttpHeaders httpHeaders = new HttpHeaders();

        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.set("Accept", "application/json");


        httpHeaders.set("Authorization", AuthBuilder.createBasicAuth(username, password));

        HttpEntity<String> request = new HttpEntity<>("", httpHeaders);

        ResponseEntity<String> response = null;
        int statusCode = -1;
        String responseBody = null;
        HttpHeaders headers = null;
        try {
            System.out.println("calling " + jobRunUrl);
            response = restTemplate.exchange(jobRunUrl, HttpMethod.POST, request, String.class);

            String runId = getByKeyId(response.getBody(), "id", "");

            if (StringUtils.isEmpty(runId)) {
                throw new Exception("Invalid runid." + runId);
            }
            System.out.println("Run Id : " + runId);

            String status = getByKeyId(response.getBody(), "task", "status");

            if (StringUtils.isEmpty(status)) {
                throw new Exception("Invalid status " + status);
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

                if (StringUtils.isEmpty(ciCdStatus)) {
                    throw new GradleException("Failed to get status.");
                }

                String[] dataArray = ciCdStatus.split(":");

                status = dataArray[0];

                if (StringUtils.isEmpty(status)) {
                    throw new GradleException("Invalid status." + status);
                }

                System.out.println("Run Status: " + status + ".....");

                if ("COMPLETED".equalsIgnoreCase(status) ) {
                    printStatus(dataArray);
                }

                if ("FAIL".equalsIgnoreCase(status) ) {
                    getLogger().info("Reason for Failure : " +  dataArray[1]);
                }


                if ("TIMEOUT".equalsIgnoreCase(status) ) {
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

        getLogger().info("----------------------------------------");
        getLogger().info("Run detail's link " + host + dataArray[6]);
        getLogger().info("----------------------------------------");
        getLogger().info(dataArray[7]);
        getLogger().info("----------------------------------------");
        getLogger().info("Run No : " + dataArray[5] );
        getLogger().info("----------------------------------------");
        getLogger().info("Success : " + dataArray[1] + " % " + " -- Total Tests : " + dataArray[2] + " -- Failed Tests : " + dataArray[3]);
        getLogger().info("----------------------------------------");
    }


    public String getByKeyId(String value, String key1, String key2) {

        if (StringUtils.isEmpty(value)) {
            return null;
        }

        ObjectMapper mapper = new ObjectMapper();
        //  jsonMapper.configure(DeserializationConfig.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)

        try {
            JsonNode rootNode = mapper.readTree(value);

            JsonNode nameNode = null;

            if (StringUtils.isEmpty(key2) && !StringUtils.isEmpty(key1)) {
                nameNode = rootNode.path("data").path(key1);
            } else if (!StringUtils.isEmpty(key2) && !StringUtils.isEmpty(key1)) {
                nameNode = rootNode.path("data").path(key1).path(key2);
            }

            return nameNode.textValue();
        } catch (Exception e) {
            getLogger().info(e.getLocalizedMessage());
        }
        return null;
    }

}
