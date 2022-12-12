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
import org.onlab.util.Tools;
import org.onosproject.codec.impl.LinkCodec;
import org.onosproject.net.optical.json.OchSignalCodec;
import org.onosproject.net.resource.DiscreteResourceId;
import org.onosproject.net.resource.Resource;
import org.onosproject.net.resource.ResourceService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.resource.Resources;
import org.onosproject.net.Link;
import org.onosproject.net.OchSignal;
import org.onosproject.net.DefaultOchSignalComparator;
import org.onosproject.net.topology.ClusterId;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Query optical links and resources.
 */
@Path("links")
public class OpticalLinksWebResource extends AbstractWebResource  {

    private static final Logger log = getLogger(OpticalIntentsWebResource.class);

    @Context
    private UriInfo uriInfo;

    /**
     * Get the optical intents on the network.
     *
     * @return 200 OK
     */
    @GET
    @Path("opticalChannels")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLinksChannels() {

        ArrayNode arrayLinks = mapper().createArrayNode();

        TopologyService topologyService = get(TopologyService.class);
        Set<Link> links = topologyService.getClusterLinks(
                topologyService.currentTopology(),
                topologyService.getCluster(topologyService.currentTopology(), ClusterId.clusterId(0)));

        Iterator linksItr = links.iterator();

        while (linksItr.hasNext()) {

            Link link = (Link) linksItr.next();

            if (link.type() == Link.Type.OPTICAL) {
                List<OchSignal> ochSignalsAvailable = findAvailableLambdas(link);
                List<OchSignal> ochSignalsRegistered = findRegisteredLambdas(link);

                ObjectNode objectNode = mapper().createObjectNode();
                //LinkCodec linkCodec = new LinkCodec();
                //ObjectNode objectNode = linkCodec.encode(link,this);

                objectNode.put("src", link.src().toString());
                objectNode.put("dst", link.dst().toString());
                objectNode.put("type", link.type().toString());
                objectNode.put("state", link.state().toString());
                objectNode.put("ava-channels", ochSignalsAvailable.toString());
                objectNode.put("reg-channels", ochSignalsRegistered.toString());

                arrayLinks.add(objectNode);
            }
        }

        ObjectNode root = this.mapper().createObjectNode().putPOJO("Links", arrayLinks);
        return ok(root).build();
    }

    /**
     * Get the optical intents on the network.
     *
     * @return 200 OK
     */
    @GET
    @Path("availableOpticalChannels")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLinksAvailableChannels() {

        ArrayNode arrayLinks = mapper().createArrayNode();

        TopologyService topologyService = get(TopologyService.class);
        Set<Link> links = topologyService.getClusterLinks(
                topologyService.currentTopology(),
                topologyService.getCluster(topologyService.currentTopology(), ClusterId.clusterId(0)));

        Iterator linksItr = links.iterator();

        while (linksItr.hasNext()) {

            Link link = (Link) linksItr.next();
            List<OchSignal> ochSignals = findAvailableLambdas(link);

            ObjectNode objectNode = mapper().createObjectNode();
            objectNode.put("src", link.src().toString());
            objectNode.put("dst", link.dst().toString());
            objectNode.put("type", link.type().toString());
            objectNode.put("state", link.state().toString());
            objectNode.put("available-channels", ochSignals.toString());

            arrayLinks.add(objectNode);
        }

        ObjectNode root = this.mapper().createObjectNode().putPOJO("Links", arrayLinks);
        return ok(root).build();
    }

    /**
     * Get the optical intents on the network.
     *
     * @return 200 OK
     */
    @GET
    @Path("registeredOpticalChannels")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLinksRegisteredChannels() {

        ArrayNode arrayLinks = mapper().createArrayNode();

        TopologyService topologyService = get(TopologyService.class);
        Set<Link> links = topologyService.getClusterLinks(
                topologyService.currentTopology(),
                topologyService.getCluster(topologyService.currentTopology(), ClusterId.clusterId(0)));

        Iterator linksItr = links.iterator();

        while (linksItr.hasNext()) {

            Link link = (Link) linksItr.next();
            List<OchSignal> ochSignals = findRegisteredLambdas(link);

            ObjectNode objectNode = mapper().createObjectNode();
            objectNode.put("src", link.src().toString());
            objectNode.put("dst", link.dst().toString());
            objectNode.put("type", link.type().toString());
            objectNode.put("state", link.state().toString());
            objectNode.put("registered-channels", ochSignals.toString());

            arrayLinks.add(objectNode);
        }

        ObjectNode root = this.mapper().createObjectNode().putPOJO("Links", arrayLinks);
        return ok(root).build();
    }

    private List<OchSignal> findAvailableLambdas(Link link) {
        //Available lambdas on a link are considered the lambdas available on the src port of the link

        DeviceService deviceService = get(DeviceService.class);
        ResourceService resourceService = get(ResourceService.class);

        DiscreteResourceId resourceId = Resources.discrete(
                link.src().deviceId(),
                deviceService.getPort(link.src().deviceId(), link.src().port()).number()).id();

        Set<OchSignal> ochSignals = resourceService.getAvailableResourceValues(resourceId, OchSignal.class);

        List<OchSignal> listOch = new ArrayList(ochSignals);
        listOch.sort(new DefaultOchSignalComparator());

        return listOch;
    }

    private List<OchSignal> findRegisteredLambdas(Link link) {
        //Registered lambdas on a link are considered the lambdas registered on the src port of the link

        DeviceService deviceService = get(DeviceService.class);
        ResourceService resourceService = get(ResourceService.class);

        DiscreteResourceId resourceId = Resources.discrete(
                link.src().deviceId(),
                deviceService.getPort(link.src().deviceId(), link.src().port()).number()).id();

        Set<Resource> resources = resourceService.getRegisteredResources(resourceId);
        Set<OchSignal> ochSignals = resources.stream()
                .filter(resource -> resource.isTypeOf(OchSignal.class))
                .map(x -> x.valueAs(OchSignal.class))
                .flatMap(Tools::stream)
                .collect(Collectors.toSet());

        List<OchSignal> listOch = new ArrayList(ochSignals);
        listOch.sort(new DefaultOchSignalComparator());

        return listOch;
    }



}
