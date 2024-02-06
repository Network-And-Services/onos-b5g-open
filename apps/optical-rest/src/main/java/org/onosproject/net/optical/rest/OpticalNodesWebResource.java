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
import com.google.common.collect.Lists;
import org.onosproject.net.*;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.DeviceAnnotationConfig;
import org.onosproject.net.config.basics.PortAnnotationConfig;
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
import java.util.List;
import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Query optical devices, ports and resources.
 */
@Path("nodes")
public class OpticalNodesWebResource extends AbstractWebResource {

    private static final Logger log = getLogger(OpticalNodesWebResource.class);

    private static final Set<String> allowedFields = new HashSet<>(Arrays.asList(
            "id", "type", "annotations"));
    private static final Set<String> allowedAnnotationKeysNodes = new HashSet<>(Arrays.asList(
            AnnotationKeys.OPENCONFIG_OP_MODE));

    private static final Set<String> allowedAnnotationKeysPorts = new HashSet<>(Arrays.asList(
            AnnotationKeys.INTERDOMAIN_CONNECT_POINT));

    /**
     * Get the optical nodes on the network.
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
     * Set an annotation on an optical node.
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

        if (!allowedAnnotationKeysNodes.contains(key)) {
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
     * Set the annotation on all optical nodes.
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

        if (!allowedAnnotationKeysNodes.contains(key)) {
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
     * Gets ports of all infrastructure devices.
     * Returns port details of all infrastructure devices.
     *
     * @return 200 OK with a collection of ports for all devices
     */
    @GET
    @Path("opticalNodes/ports")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDevicesPorts() {
        DeviceService service = get(DeviceService.class);
        List<Port> result = Lists.newArrayList();
        service.getDevices().forEach(device -> {
            Optional<List<Port>> list = Optional.ofNullable(service.getPorts(device.id()));
            list.ifPresent(ports -> result.addAll(ports));
        });
        return ok(encodeArray(Port.class, "ports", result)).build();
    }

    /**
     * Set an annotation on a specific port.
     *
     * @param connectPoint connectPoint to be annotated
     * @param key annotation key (e.g., interdomain-connect-point)
     * @param value annotation value (e.g., remote connectPoint)
     * @return 200 OK
     */
    @POST
    @Path("annotate/onePort")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response annotatePort(@QueryParam("connectPoint") String connectPoint,
                                 @QueryParam("key") String key,
                                 @QueryParam("value") String value) {

        if (!allowedAnnotationKeysPorts.contains(key)) {
            throw new IllegalArgumentException("Specified key is not valid to annotate an optical port.");
        }
        NetworkConfigService netcfgService = get(NetworkConfigService.class);
        ConnectPoint cPort = ConnectPoint.deviceConnectPoint(connectPoint);

        DeviceService deviceService = get(DeviceService.class);
        Device device = deviceService.getDevice(cPort.deviceId());
        if (device == null) {
            throw new IllegalArgumentException("Specified device does not exist.");
        }
        Port port = deviceService.getPort(cPort);
        if (port == null) {
            throw new IllegalArgumentException("Specified port does not exist on device.");
        }

        PortAnnotationConfig cfg = netcfgService.getConfig(cPort, PortAnnotationConfig.class);
        if (cfg == null) {
            cfg = new PortAnnotationConfig(cPort);
        }
        cfg.annotation(key, value);
        netcfgService.applyConfig(cPort, PortAnnotationConfig.class, cfg.node());

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
