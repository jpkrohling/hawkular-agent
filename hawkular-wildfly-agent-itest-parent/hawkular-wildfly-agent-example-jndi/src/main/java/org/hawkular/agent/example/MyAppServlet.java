/*
 * Copyright 2015-2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hawkular.agent.example;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hawkular.agent.monitor.api.Avail;
import org.hawkular.agent.monitor.api.AvailDataPayloadBuilder;
import org.hawkular.agent.monitor.api.AvailStorage;
import org.hawkular.agent.monitor.api.HawkularWildFlyAgentContext;
import org.hawkular.agent.monitor.api.InventoryEvent;
import org.hawkular.agent.monitor.api.MetricDataPayloadBuilder;
import org.hawkular.agent.monitor.api.MetricStorage;
import org.hawkular.agent.monitor.inventory.ID;
import org.hawkular.agent.monitor.inventory.Name;
import org.hawkular.agent.monitor.inventory.Resource;
import org.hawkular.agent.monitor.inventory.ResourceType;
import org.hawkular.metrics.client.common.MetricType;

public class MyAppServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @EJB
    private HawkularWildFlyAgentProvider hawkularAgent;

    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String newResourceId = request.getParameter("newResourceID");
        String oldResourceId = request.getParameter("oldResourceID");
        String metricKey = request.getParameter("metricKey");
        String availKey = request.getParameter("availKey");

        if (newResourceId == null && oldResourceId == null && metricKey == null && availKey == null) {
            throw new ServletException("Don't know what to do!");
        }

        if (newResourceId != null) {
            createNewResource(request, response, newResourceId);
        } else if (oldResourceId != null) {
            removeOldResource(request, response, oldResourceId);
        } else if (metricKey != null) {
            Double metricValue = Double.valueOf(request.getParameter("metricValue"));
            sendMetric(request, response, metricKey, metricValue);
        } else if (availKey != null) {
            Avail availValue = Avail.valueOf(request.getParameter("availValue"));
            sendAvail(request, response, availKey, availValue);
        }
    }

    private void sendAvail(HttpServletRequest request, HttpServletResponse response, String availKey,
            Avail availValue) {
        try {
            HawkularWildFlyAgentContext hawkularWildFlyAgent = hawkularAgent.getHawkularWildFlyAgent();
            AvailStorage availStorage = hawkularWildFlyAgent.getAvailStorage();

            AvailDataPayloadBuilder payloadBuilder = availStorage.createAvailDataPayloadBuilder();
            payloadBuilder.addDataPoint(availKey, System.currentTimeMillis(), availValue);
            availStorage.store(payloadBuilder, 0);

            String results = String.format("<h1>Send Avail</h1>\n<p>Avail Key=%s</p>\n<p>Avail Value=%s</p>",
                    availKey, availValue);
            printResults(response, results);
        } catch (Exception e) {
            printResults(response, "sendAvail failure: " + e);
        }
    }

    private void sendMetric(HttpServletRequest request, HttpServletResponse response, String metricKey,
            Double metricValue) {
        try {
            HawkularWildFlyAgentContext hawkularWildFlyAgent = hawkularAgent.getHawkularWildFlyAgent();
            MetricStorage metricStorage = hawkularWildFlyAgent.getMetricStorage();

            MetricDataPayloadBuilder payloadBuilder = metricStorage.createMetricDataPayloadBuilder();
            payloadBuilder.addDataPoint(metricKey, System.currentTimeMillis(), metricValue, MetricType.GAUGE);
            metricStorage.store(payloadBuilder, 0);

            String results = String.format("<h1>Send Metric</h1>\n<p>Metric Key=%s</p>\n<p>Metric Value=%s</p>",
                    metricKey, metricValue);
            printResults(response, results);
        } catch (Exception e) {
            printResults(response, "sendMetric failure: " + e);
        }
    }

    private void createNewResource(HttpServletRequest request, HttpServletResponse response, String resourceId) {
        try {
            MyAppSamplingService myAppSamplingService = hawkularAgent.getSamplingService();
            HawkularWildFlyAgentContext hawkularWildFlyAgent = hawkularAgent.getHawkularWildFlyAgent();
            Resource<MyAppNodeLocation> resource = instantiateResource(resourceId);
            List<Resource<MyAppNodeLocation>> resources = Arrays.asList(resource);
            InventoryEvent<MyAppNodeLocation> event = new InventoryEvent<>(myAppSamplingService, resources);
            hawkularWildFlyAgent.getInventoryStorage().resourcesAdded(event);

            String results = String.format("<h1>Create New Resource</h1>\n<p>Resource=%s</p>", resource);
            printResults(response, results);
        } catch (Exception e) {
            printResults(response, "createNewResource failure: " + e);
        }
    }

    private void removeOldResource(HttpServletRequest request, HttpServletResponse response, String resourceId) {
        try {
            MyAppSamplingService myAppSamplingService = hawkularAgent.getSamplingService();
            HawkularWildFlyAgentContext hawkularWildFlyAgent = hawkularAgent.getHawkularWildFlyAgent();
            Resource<MyAppNodeLocation> resource = instantiateResource(resourceId);
            List<Resource<MyAppNodeLocation>> resources = Arrays.asList(resource);
            InventoryEvent<MyAppNodeLocation> event = new InventoryEvent<>(myAppSamplingService, resources);
            hawkularWildFlyAgent.getInventoryStorage().resourcesRemoved(event);
            String results = String.format("<h1>Removed Old Resource</h1>\n<p>Resource=%s</p>", resource);
            printResults(response, results);
        } catch (Exception e) {
            printResults(response, "removeOldResource failure: " + e);
        }
    }

    private Resource<MyAppNodeLocation> instantiateResource(String resourceId) {
        ResourceType<MyAppNodeLocation> resourceType = ResourceType.<MyAppNodeLocation> builder()
                .id(new ID("MyAppResourceType"))
                .name(new Name("My App Resource Type"))
                .parent(null)
                .location(new MyAppNodeLocation("/"))
                .build();

        Resource<MyAppNodeLocation> resource = Resource.<MyAppNodeLocation> builder()
                .type(resourceType)
                .id(new ID(resourceId))
                .name(new Name("My App Resource " + resourceId))
                .parent(null)
                .location(new MyAppNodeLocation("/" + resourceId))
                .build();
        return resource;
    }

    private void printResults(HttpServletResponse response, String msg) {
        try {
            PrintWriter out = response.getWriter();
            out.println(msg);
        } catch (IOException e) {
            log("Cannot print results: " + msg, e);
        }
    }
}
