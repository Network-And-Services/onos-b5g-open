/*
 * Copyright 2017-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.net.optical.rest;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.topology.ClusterId;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.rest.AbstractWebResource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Iterator;
import java.util.Set;

/**
 * Query optical devices, ports and resources.
 */
@Path("nodes")
public class OpticalNodesWebResource extends AbstractWebResource {
    /**
     * Get the optical intents on the network.
     *
     * @return 200 OK
     */
    @GET
    @Path("opticalNodes")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLinksRegisteredChannels() {

        ArrayNode arrayLinks = mapper().createArrayNode();

        TopologyService topologyService = get(TopologyService.class);
        DeviceService deviceService = get(DeviceService.class);

        Set<DeviceId> devices = topologyService.getClusterDevices(
                topologyService.currentTopology(),
                topologyService.getCluster(topologyService.currentTopology(), ClusterId.clusterId(0)));

        Iterator deviceItr = devices.iterator();

        while (deviceItr.hasNext()) {

            Device device = deviceService.getDevice((DeviceId) deviceItr.next());

            if ((device.type() == Device.Type.FIBER_SWITCH) ||
                    (device.type() == Device.Type.ROADM) ||
                    (device.type() == Device.Type.ROADM_OTN) ||
                    (device.type() == Device.Type.TERMINAL_DEVICE) ||
                    (device.type() == Device.Type.OPTICAL_AMPLIFIER) ||
                    (device.type() == Device.Type.OTN)) {

                ObjectNode objectNode = mapper().createObjectNode();
                objectNode.put("id", device.id().toString());
                objectNode.put("type", device.type().toString());

                arrayLinks.add(objectNode);
            }
        }

        ObjectNode root = this.mapper().createObjectNode().putPOJO("Nodes", arrayLinks);
        return ok(root).build();
    }
}
