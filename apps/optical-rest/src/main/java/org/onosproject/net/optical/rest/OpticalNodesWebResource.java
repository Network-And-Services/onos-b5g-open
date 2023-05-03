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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.DeviceAnnotationConfig;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.topology.ClusterId;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Query optical devices, ports and resources.
 */
@Path("nodes")
public class OpticalNodesWebResource extends AbstractWebResource {

    private static final Logger log = getLogger(OpticalNodesWebResource.class);

    private static final Set<String> allowedFields = new HashSet<>(Arrays.asList(
            "id", "type", "annotations"));
    private static final Set<String> allowedAnnotationKeys = new HashSet<>(Arrays.asList(
            AnnotationKeys.OPENCONFIG_OP_MODE));

    /**
     * Get the optical intents on the network.
     *
     * @return 200 OK
     */
    @GET
    @Path("opticalNodes")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOpticalNodes() {

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

                ObjectNode objectNodeDevice = encode(device,Device.class);

                Iterator<String> fieldNames = objectNodeDevice.fieldNames();
                while (fieldNames.hasNext()) {
                    String fieldName = fieldNames.next();

                    if (!allowedFields.contains(fieldName)) {
                        fieldNames.remove();
                    }
                }

                arrayLinks.add(objectNodeDevice);
            }
        }

        ObjectNode root = this.mapper().createObjectNode().putPOJO("Nodes", arrayLinks);
        return ok(root).build();
    }

    /**
     * Set an annotation on an optical link.
     *
     * @param deviceId device
     * @param key annotation key
     * @param value annotation value
     * @return 200 OK
     */
    @POST
    @Path("annotate/oneNode")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response annotateNode(@QueryParam("deviceId") String deviceId,
                                 @QueryParam("key") String key,
                                 @QueryParam("value") String value) {

        if (!allowedAnnotationKeys.contains(value)) {
            throw new IllegalArgumentException("Specified key is not valid to annotate an optical device.");
        }

        DeviceId device = DeviceId.deviceId(deviceId);

        NetworkConfigService netcfgService = get(NetworkConfigService.class);
        DeviceAnnotationConfig cfg = netcfgService.getConfig(device, DeviceAnnotationConfig.class);

        if (cfg == null) {
            cfg = new DeviceAnnotationConfig(device);
        }
        cfg.annotation(key, value);
        netcfgService.applyConfig(device, DeviceAnnotationConfig.class, cfg.node());

        ObjectNode root = mapper().createObjectNode();
        return Response.ok(root).build();
    }

    /**
     * Set the annotation on all optical links.
     *
     * @param key annotation key
     * @param value annotation value
     * @return 200 OK
     */
    @POST
    @Path("annotate/allNodes")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response annotateNodes(@QueryParam("key") String key,
                                  @QueryParam("value") String value) {

        if (!allowedAnnotationKeys.contains(value)) {
            throw new IllegalArgumentException("Specified key is not valid to annotate an optical device.");
        }

        TopologyService topologyService = get(TopologyService.class);
        Set<DeviceId> devices = topologyService.getClusterDevices(
                topologyService.currentTopology(),
                topologyService.getCluster(topologyService.currentTopology(), ClusterId.clusterId(0)));

        Iterator devicesItr = devices.iterator();

        while (devicesItr.hasNext()) {

            DeviceId device = (DeviceId) devicesItr.next();

            NetworkConfigService netcfgService = get(NetworkConfigService.class);
            DeviceAnnotationConfig cfg = netcfgService.getConfig(device, DeviceAnnotationConfig.class);

            if (cfg == null) {
                cfg = new DeviceAnnotationConfig(device);
            }
            cfg.annotation(key, value);
            netcfgService.applyConfig(device, DeviceAnnotationConfig.class, cfg.node());
        }

        ObjectNode root = mapper().createObjectNode();
        return Response.ok(root).build();
    }

    /**
     * Add a supported opmode to a device.
     *
     * @param deviceId deviceId of node to annotate
     * @param opModes supported opmodes, integer separated by commas
     * @return 200 OK
     */
    @POST
    @Path("annotate/opMode")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response annotateNode(@QueryParam("deviceId") String deviceId,
                                 @QueryParam("opModeId") String opModes) {

        OperationalModesManager manager = get(OperationalModesManager.class);

        //Check if all specified opmodes are known by the manager
        String[] opModesString = opModes.split(",");
        for(String mode: opModesString) {
            if (!manager.isRegisteredMode(Integer.valueOf(mode))) {
                return Response.notModified("Specified op-modes are not registered").build();
            }
        }

        String key = AnnotationKeys.OPENCONFIG_OP_MODE;

        DeviceId device = DeviceId.deviceId(deviceId);

        NetworkConfigService netcfgService = get(NetworkConfigService.class);
        DeviceAnnotationConfig cfg = netcfgService.getConfig(device, DeviceAnnotationConfig.class);

        if (cfg == null) {
            cfg = new DeviceAnnotationConfig(device);
        }
        cfg.annotation(key, opModes);
        netcfgService.applyConfig(device, DeviceAnnotationConfig.class, cfg.node());

        ObjectNode root = mapper().createObjectNode();
        return Response.ok(root).build();
    }
}
