/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates
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
package org.hawkular.agent.monitor.api;

public interface InventoryStorage {
    /**
     * @return a builder object suitable for wrapping inventory data in a proper payload
     *         message format to be sent to the storage backend.
     */
    InventoryDataPayloadBuilder createInventoryDataPayloadBuilder();

    /**
     * Stores the inventory data found in the given builder.
     *
     * @param payloadBuilder contains the inventory data to store
     */
    void store(InventoryDataPayloadBuilder payloadBuilder);
}