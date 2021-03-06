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
package org.hawkular.agent.monitor.extension;

import java.util.Arrays;
import java.util.Collection;

import org.hawkular.agent.monitor.dynamicprotocol.DynamicEndpointService;
import org.hawkular.agent.monitor.dynamicprotocol.DynamicProtocolService;
import org.hawkular.agent.monitor.dynamicprotocol.DynamicProtocolServices;
import org.hawkular.agent.monitor.service.MonitorService;
import org.hawkular.agent.monitor.util.Util;
import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry.Flag;
import org.jboss.dmr.ModelNode;

public class RemotePrometheusDefinition extends PersistentResourceDefinition {
    public static final RemotePrometheusDefinition INSTANCE = new RemotePrometheusDefinition();

    static final String REMOTE_PROMETHEUS = "remote-prometheus";

    private RemotePrometheusDefinition() {
        super(PathElement.pathElement(REMOTE_PROMETHEUS),
                SubsystemExtension.getResourceDescriptionResolver(ManagedServersDefinition.MANAGED_SERVERS,
                        REMOTE_PROMETHEUS),
                RemotePrometheusAdd.INSTANCE,
                RemotePrometheusRemove.INSTANCE,
                Flag.RESTART_NONE,
                Flag.RESTART_NONE);
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(RemotePrometheusAttributes.ATTRIBUTES);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        Util.registerOnlyRestartAttributes(resourceRegistration, getAttributes());

        resourceRegistration.registerReadWriteAttribute(RemotePrometheusAttributes.ENABLED, null,
                new MonitorServiceWriteAttributeHandler<Void>() {
                    @Override
                    protected boolean applyUpdateToRuntime(
                            OperationContext context,
                            ModelNode operation,
                            String attributeName,
                            ModelNode newValue,
                            ModelNode currentValue,
                            AbstractWriteAttributeHandler.HandbackHolder<Void> handbackHolder)
                                    throws OperationFailedException {

                        if (context.isBooting()) {
                            return false;
                        }

                        boolean currBool = RemotePrometheusAttributes.ENABLED.resolveValue(context, currentValue)
                                .asBoolean();
                        boolean newBool = RemotePrometheusAttributes.ENABLED.resolveValue(context, newValue)
                                .asBoolean();
                        if (currBool == newBool) {
                            return false; // don't know if this would ever happen, but if it does, nothing changed
                        }

                        MonitorService monitorService = getMonitorService(context);
                        DynamicProtocolService promService = monitorService.getDynamicProtocolServices()
                                .getPrometheusProtocolService();
                        String thisEndpointName = context.getCurrentAddressValue();

                        if (newBool) {
                            // add the endpoint so it begins to be monitored

                            // first get our subsystem config
                            MonitorServiceConfiguration config = Util.getMonitorServiceConfiguration(context);

                            // create a new endpoint service
                            DynamicProtocolServices newServices = monitorService.createDynamicProtocolServicesBuilder()
                                    .prometheusDynamicProtocolService(config.getPrometheusConfiguration(),
                                            monitorService.getHawkularMonitorContext())
                                    .build();
                            DynamicEndpointService endpointService = newServices.getPrometheusProtocolService()
                                    .getDynamicEndpointServices().get(thisEndpointName);

                            // put the new endpoint service in the original protocol services container
                            promService.add(endpointService);
                        } else {
                            // remove the endpoint so it is no longer monitored
                            promService.remove(thisEndpointName);
                        }

                        return false;
                    }

                    @Override
                    protected void revertUpdateToRuntime(
                            OperationContext context,
                            ModelNode operation,
                            String attributeName,
                            ModelNode originalValue,
                            ModelNode newBadValue,
                            Void handback)
                                    throws OperationFailedException {
                        // nothing to revert?
                    }
                });
    }

}
