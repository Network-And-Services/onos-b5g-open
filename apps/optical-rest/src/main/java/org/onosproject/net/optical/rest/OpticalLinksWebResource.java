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
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.OpticalBandType;
import org.onosproject.net.OpticalBandUtils;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.link.LinkService;
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

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.onosproject.net.OpticalBandType.L_BAND;
import static org.onosproject.net.OpticalBandType.C_BAND;
import static org.onosproject.net.OpticalBandType.S_BAND;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Query optical links and resources.
 */
@Path("links")
public class OpticalLinksWebResource extends AbstractWebResource  {

    private static final Logger log = getLogger(OpticalLinksWebResource.class);

    @Context
    private UriInfo uriInfo;

    /**
     * Get the details of optical links.
     *
     * @return 200 OK
     */
    @GET
    @Path("perBandChannels")
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
                LinkCodec linkCodec = new LinkCodec();
                ObjectNode linkObjectNode = linkCodec.encode(link,this);

                ObjectNode lBand = mapper().createObjectNode();
                ObjectNode cBand = mapper().createObjectNode();
                ObjectNode sBand = mapper().createObjectNode();

                //linkObjectNode.put("L-band-avail", findAvailableLambdas(link, Optional.of(L_BAND)).toString());
                //linkObjectNode.put("L-band-regis", findRegisteredLambdas(link, Optional.of(L_BAND)).toString());
                //linkObjectNode.put("C-band-avail", findAvailableLambdas(link, Optional.of(C_BAND)).toString());
                //linkObjectNode.put("C-band-regis", findRegisteredLambdas(link, Optional.of(C_BAND)).toString());
                //linkObjectNode.put("S-band-avail", findAvailableLambdas(link, Optional.of(S_BAND)).toString());
                //linkObjectNode.put("S-band-regis", findRegisteredLambdas(link, Optional.of(S_BAND)).toString());

                lBand.put("available-channels", findAvailableLambdas(link, Optional.of(L_BAND)).toString());
                lBand.put("registered-channels", findRegisteredLambdas(link, Optional.of(L_BAND)).toString());
                linkObjectNode.set("L-band", lBand);

                cBand.put("available-channels", findAvailableLambdas(link, Optional.of(C_BAND)).toString());
                cBand.put("registered-channels", findRegisteredLambdas(link, Optional.of(C_BAND)).toString());
                linkObjectNode.set("C-band", cBand);

                sBand.put("available-channels", findAvailableLambdas(link, Optional.of(S_BAND)).toString());
                sBand.put("registered-channels", findRegisteredLambdas(link, Optional.of(S_BAND)).toString());
                linkObjectNode.set("S-band", sBand);

                arrayLinks.add(linkObjectNode);
            }
        }

        ObjectNode root = this.mapper().createObjectNode().putPOJO("Links", arrayLinks);
        return ok(root).build();
    }

    /**
     * Get details of the specified optical link.
     *
     * @return 200 OK
     */
    @GET
    @Path("perBandChannels/device/{deviceId}/port/{portId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLinkChannels(@PathParam("deviceId") String deviceId,
                                    @PathParam("portId") String portId) {

        ArrayNode arrayLinks = mapper().createArrayNode();

        TopologyService topologyService = get(TopologyService.class);
        Set<Link> links = topologyService.getClusterLinks(
                topologyService.currentTopology(),
                topologyService.getCluster(topologyService.currentTopology(), ClusterId.clusterId(0)));

        Iterator linksItr = links.iterator();

        while (linksItr.hasNext()) {

            Link link = (Link) linksItr.next();

            if (link.src().equals(ConnectPoint.deviceConnectPoint(deviceId + "/" + portId))) {

                LinkCodec linkCodec = new LinkCodec();
                ObjectNode linkObjectNode = linkCodec.encode(link,this);

                ObjectNode lBand = mapper().createObjectNode();
                ObjectNode cBand = mapper().createObjectNode();
                ObjectNode sBand = mapper().createObjectNode();

                //linkObjectNode.put("L-band-avail", findAvailableLambdas(link, Optional.of(L_BAND)).toString());
                //linkObjectNode.put("L-band-regis", findRegisteredLambdas(link, Optional.of(L_BAND)).toString());
                //linkObjectNode.put("C-band-avail", findAvailableLambdas(link, Optional.of(C_BAND)).toString());
                //linkObjectNode.put("C-band-regis", findRegisteredLambdas(link, Optional.of(C_BAND)).toString());
                //linkObjectNode.put("S-band-avail", findAvailableLambdas(link, Optional.of(S_BAND)).toString());
                //linkObjectNode.put("S-band-regis", findRegisteredLambdas(link, Optional.of(S_BAND)).toString());

                lBand.put("available-channels", findAvailableLambdas(link, Optional.of(L_BAND)).toString());
                lBand.put("registered-channels", findRegisteredLambdas(link, Optional.of(L_BAND)).toString());
                linkObjectNode.set("L-band", lBand);

                cBand.put("available-channels", findAvailableLambdas(link, Optional.of(C_BAND)).toString());
                cBand.put("registered-channels", findRegisteredLambdas(link, Optional.of(C_BAND)).toString());
                linkObjectNode.set("C-band", cBand);

                sBand.put("available-channels", findAvailableLambdas(link, Optional.of(S_BAND)).toString());
                sBand.put("registered-channels", findRegisteredLambdas(link, Optional.of(S_BAND)).toString());
                linkObjectNode.set("S-band", sBand);

                arrayLinks.add(linkObjectNode);
            }
        }

        ObjectNode root = this.mapper().createObjectNode().putPOJO("Links", arrayLinks);
        return ok(root).build();
    }

    /**
     * Get available channels on optical links.
     *
     * @return 200 OK
     */
    @GET
    @Path("availableChannels")
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
            List<OchSignal> ochSignals = findAvailableLambdas(link, Optional.empty());

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
     * Get registered channels on optical links.
     *
     * @return 200 OK
     */
    @GET
    @Path("registeredChannels")
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
            List<OchSignal> ochSignals = findRegisteredLambdas(link, Optional.empty());

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

    /**
     * Get annotations on optical links.
     *
     * @return 200 OK
     */
    @GET
    @Path("annotations")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLinksAnnotations() {

        ArrayNode arrayLinks = mapper().createArrayNode();

        TopologyService topologyService = get(TopologyService.class);
        Set<Link> links = topologyService.getClusterLinks(
                topologyService.currentTopology(),
                topologyService.getCluster(topologyService.currentTopology(), ClusterId.clusterId(0)));

        Iterator linksItr = links.iterator();

        while (linksItr.hasNext()) {

            Link link = (Link) linksItr.next();

            if (link.type() == Link.Type.OPTICAL) {
                LinkCodec linkCodec = new LinkCodec();
                ObjectNode objectNode = linkCodec.encode(link,this);

                arrayLinks.add(objectNode);
            }
        }

        ObjectNode root = this.mapper().createObjectNode().putPOJO("Links", arrayLinks);
        return ok(root).build();
    }

    /**
     * Get annotations on optical links.
     *
     * @return 200 OK

    @POST
    @Path("annotations")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response annotateLink(@QueryParam("linkString") String link,
                                    @QueryParam("portId") String portId) {
        LinkService service = get(LinkService.class);

        service.getLink()
        DeviceService deviceService = get(DeviceService.class);

        ObjectNode root = mapper().createObjectNode();

        return Response.ok(root).build();
    }*/

    private List<OchSignal> findAvailableLambdas(Link link, Optional<OpticalBandType> band) {
        //Available lambdas on a link: i.e., lambdas available on the src port of the link

        DeviceService deviceService = get(DeviceService.class);
        ResourceService resourceService = get(ResourceService.class);

        DiscreteResourceId resourceId = Resources.discrete(
                link.src().deviceId(),
                deviceService.getPort(link.src().deviceId(), link.src().port()).number()).id();

        Set<OchSignal> ochSignals = resourceService.getAvailableResourceValues(resourceId, OchSignal.class);

        if (band.isPresent()) {
            ochSignals= ochSignals.stream()
                    .filter(x -> OpticalBandUtils.computeOpticalBand(x).equals(band.get()))
                    .collect(Collectors.toSet());
        }

        List<OchSignal> listOch = new ArrayList(ochSignals);
        listOch.sort(new DefaultOchSignalComparator());

        return listOch;
    }

    private List<OchSignal> findRegisteredLambdas(Link link, Optional<OpticalBandType> band) {
        //Registered lambdas on a link: i.e., lambdas registered on the src port of the link

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

        if (band.isPresent()) {
            ochSignals= ochSignals.stream()
                    .filter(x -> OpticalBandUtils.computeOpticalBand(x).equals(band.get()))
                    .collect(Collectors.toSet());
        }

        List<OchSignal> listOch = new ArrayList(ochSignals);
        listOch.sort(new DefaultOchSignalComparator());

        return listOch;
    }
}
