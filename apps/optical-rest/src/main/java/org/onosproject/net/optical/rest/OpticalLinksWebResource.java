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
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.OpticalBandType;
import org.onosproject.net.OpticalBandUtils;
import org.onosproject.net.link.DefaultLinkDescription;
import org.onosproject.net.link.LinkDescription;
import org.onosproject.net.link.LinkProvider;
import org.onosproject.net.link.LinkProviderRegistry;
import org.onosproject.net.link.LinkProviderService;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.resource.DiscreteResourceId;
import org.onosproject.net.resource.Resource;
import org.onosproject.net.resource.ResourceService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.resource.Resources;
import org.onosproject.net.Link;
import org.onosproject.net.OchSignal;
import org.onosproject.net.DefaultOchSignalComparator;
import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.OpticalBandType.L_BAND;
import static org.onosproject.net.OpticalBandType.C_BAND;
import static org.onosproject.net.OpticalBandType.S_BAND;
import static org.onosproject.net.OpticalBandType.O_BAND;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Query optical links and resources.
 */
@Path("links")
public class OpticalLinksWebResource extends AbstractWebResource  {

    private static final Logger log = getLogger(OpticalLinksWebResource.class);

    private static final ProviderId PID = new ProviderId("rest", "org.onosproject.optical-rest");

    private static final Set<String> allowedKeys = new HashSet<>(Arrays.asList(
            AnnotationKeys.FIBER_LENGTH,
            AnnotationKeys.FIBER_DISPERSION,
            AnnotationKeys.FIBER_DISPERSION_SLOPE,
            AnnotationKeys.FIBER_LOSS,
            AnnotationKeys.FIBER_PMD,
            AnnotationKeys.FIBER_EFFECTIVE_AREA));

    private static final Random randomGenerator = new Random();

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

        LinkService linkService = get(LinkService.class);
        Iterable<Link> links = linkService.getLinks();
        Iterator linksItr = links.iterator();

        while (linksItr.hasNext()) {

            Link link = (Link) linksItr.next();

            if (link.type() == Link.Type.OPTICAL) {
                LinkCodec linkCodec = new LinkCodec();
                ObjectNode linkObjectNode = linkCodec.encode(link,this);

                ObjectNode lBand = mapper().createObjectNode();
                ObjectNode cBand = mapper().createObjectNode();
                ObjectNode sBand = mapper().createObjectNode();
                ObjectNode oBand = mapper().createObjectNode();

                lBand.put("available-channels", findAvailableLambdas(link, Optional.of(L_BAND)).toString());
                lBand.put("registered-channels", findRegisteredLambdas(link, Optional.of(L_BAND)).toString());
                linkObjectNode.set("L-band", lBand);

                cBand.put("available-channels", findAvailableLambdas(link, Optional.of(C_BAND)).toString());
                cBand.put("registered-channels", findRegisteredLambdas(link, Optional.of(C_BAND)).toString());
                linkObjectNode.set("C-band", cBand);

                sBand.put("available-channels", findAvailableLambdas(link, Optional.of(S_BAND)).toString());
                sBand.put("registered-channels", findRegisteredLambdas(link, Optional.of(S_BAND)).toString());
                linkObjectNode.set("S-band", sBand);

                oBand.put("available-channels", findAvailableLambdas(link, Optional.of(O_BAND)).toString());
                oBand.put("registered-channels", findRegisteredLambdas(link, Optional.of(O_BAND)).toString());
                linkObjectNode.set("O-band", oBand);

                arrayLinks.add(linkObjectNode);
            }
        }

        ObjectNode root = this.mapper().createObjectNode().putPOJO("Links", arrayLinks);
        return ok(root).build();
    }

    /**
     * Get details of the specified optical link.
     *
     * @param srcConnectPoint link source
     * @param dstConnectPoint link destination
     * @return 200 OK
     */
    @GET
    @Path("perBandChannels/link")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLinkChannels(@QueryParam("srcConnectPoint") String srcConnectPoint,
                                    @QueryParam("dstConnectPoint") String dstConnectPoint) {

        ArrayNode arrayLinks = mapper().createArrayNode();

        LinkService linkService = get(LinkService.class);
        ConnectPoint srcCP = ConnectPoint.deviceConnectPoint(srcConnectPoint);
        ConnectPoint dstCP = ConnectPoint.deviceConnectPoint(dstConnectPoint);
        Link link = linkService.getLink(srcCP, dstCP);

        if (link != null) {
            LinkCodec linkCodec = new LinkCodec();
            ObjectNode linkObjectNode = linkCodec.encode(link, this);

            ObjectNode lBand = mapper().createObjectNode();
            ObjectNode cBand = mapper().createObjectNode();
            ObjectNode sBand = mapper().createObjectNode();
            ObjectNode oBand = mapper().createObjectNode();

            lBand.put("available-channels", findAvailableLambdas(link, Optional.of(L_BAND)).toString());
            lBand.put("registered-channels", findRegisteredLambdas(link, Optional.of(L_BAND)).toString());
            linkObjectNode.set("L-band", lBand);

            cBand.put("available-channels", findAvailableLambdas(link, Optional.of(C_BAND)).toString());
            cBand.put("registered-channels", findRegisteredLambdas(link, Optional.of(C_BAND)).toString());
            linkObjectNode.set("C-band", cBand);

            sBand.put("available-channels", findAvailableLambdas(link, Optional.of(S_BAND)).toString());
            sBand.put("registered-channels", findRegisteredLambdas(link, Optional.of(S_BAND)).toString());
            linkObjectNode.set("S-band", sBand);

            oBand.put("available-channels", findAvailableLambdas(link, Optional.of(O_BAND)).toString());
            oBand.put("registered-channels", findRegisteredLambdas(link, Optional.of(O_BAND)).toString());
            linkObjectNode.set("O-band", oBand);

            arrayLinks.add(linkObjectNode);

            ObjectNode root = this.mapper().createObjectNode().putPOJO("Links", arrayLinks);
            return ok(root).build();
        } else {
            throw new IllegalArgumentException(
                    "Specified link does not exist");
        }
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

        LinkService linkService = get(LinkService.class);
        Iterable<Link> links = linkService.getLinks();
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

        LinkService linkService = get(LinkService.class);
        Iterable<Link> links = linkService.getLinks();
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

        LinkService linkService = get(LinkService.class);
        Iterable<Link> links = linkService.getLinks();
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
     * Set an annotation on an optical link.
     *
     * @param dstConnectPoint link source
     * @param srcConnectPoint link destination
     * @param key annotation key
     * @param value annotation value
     * @return 200 OK
    */
    @POST
    @Path("annotate/oneLink")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response annotateLink(@QueryParam("srcConnectPoint") String srcConnectPoint,
                                 @QueryParam("dstConnectPoint") String dstConnectPoint,
                                 @QueryParam("key") String key,
                                 @QueryParam("value") String value) {

        if (!allowedKeys.contains(value)) {
            throw new IllegalArgumentException("Specified key is not valid to annotate an optical link.");
        }

        LinkService linkService = get(LinkService.class);
        ConnectPoint srcCP = ConnectPoint.deviceConnectPoint(srcConnectPoint);
        ConnectPoint dstCP = ConnectPoint.deviceConnectPoint(dstConnectPoint);
        Link link = linkService.getLink(srcCP, dstCP);

        LinkProviderRegistry registry = get(LinkProviderRegistry.class);
        RestLinkProvider provider = new RestLinkProvider();
        LinkProviderService providerService = registry.register(provider);

        try {
            providerService.linkDetected(description(link, key, value));
        } finally {
            registry.unregister(provider);
        }

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
    @Path("annotate/allLinks")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response annotateLinks(@QueryParam("key") String key,
                                 @QueryParam("value") String value) {

        if (!allowedKeys.contains(value)) {
            throw new IllegalArgumentException("Specified key is not valid to annotate an optical link.");
        }

        LinkService linkService = get(LinkService.class);
        Iterable<Link> links = linkService.getLinks();
        Iterator linksItr = links.iterator();

        while (linksItr.hasNext()) {

            Link link = (Link) linksItr.next();

            if (link.type() == Link.Type.OPTICAL) {

                LinkProviderRegistry registry = get(LinkProviderRegistry.class);
                RestLinkProvider provider = new RestLinkProvider();
                LinkProviderService providerService = registry.register(provider);

                try {
                    providerService.linkDetected(description(link, key, value));
                } finally {
                    registry.unregister(provider);
                }
            }
        }

        ObjectNode root = mapper().createObjectNode();
        return Response.ok(root).build();
    }

    /**
     * Set the length (integer km) annotation on all optical links generating random values in a range.
     *
     * @param min minimum
     * @param max maximum
     * @return 200 OK
     */
    @POST
    @Path("annotate/allLinksLength")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response annotateLinks(@QueryParam("min") int min,
                                  @QueryParam("max") int max) {

        LinkService linkService = get(LinkService.class);
        Iterable<Link> links = linkService.getLinks();
        Iterator linksItr = links.iterator();

        while (linksItr.hasNext()) {

            Link link = (Link) linksItr.next();

            if (link.type() == Link.Type.OPTICAL) {

                LinkProviderRegistry registry = get(LinkProviderRegistry.class);
                RestLinkProvider provider = new RestLinkProvider();
                LinkProviderService providerService = registry.register(provider);

                int value = min + randomGenerator.nextInt(max - min + 1);

                try {
                    providerService.linkDetected(description(link, AnnotationKeys.FIBER_LENGTH, String.valueOf(value)));
                } finally {
                    registry.unregister(provider);
                }
            }
        }

        ObjectNode root = mapper().createObjectNode();
        return Response.ok(root).build();
    }

    private List<OchSignal> findAvailableLambdas(Link link, Optional<OpticalBandType> band) {
        //Available lambdas on a link: i.e., lambdas available on the dst port of the link
        //log.info("Link src {} band {}", link.src(), band);

        DeviceService deviceService = get(DeviceService.class);
        ResourceService resourceService = get(ResourceService.class);

        DiscreteResourceId resourceId = Resources.discrete(
                link.dst().deviceId(),
                deviceService.getPort(link.dst().deviceId(), link.dst().port()).number()).id();

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
        //Registered lambdas on a link: i.e., lambdas registered on the dst port of the link
        //log.info("Link src {} band {}", link.src(), band);

        DeviceService deviceService = get(DeviceService.class);
        ResourceService resourceService = get(ResourceService.class);

        DiscreteResourceId resourceId = Resources.discrete(
                link.dst().deviceId(),
                deviceService.getPort(link.dst().deviceId(), link.dst().port()).number()).id();

        Set<Resource> resources = resourceService.getRegisteredResources(resourceId);

        Set<OchSignal> ochSignals = resources.stream()
                .filter(resource -> resource.isTypeOf(OchSignal.class))
                .map(x -> x.valueAs(OchSignal.class))
                .flatMap(Tools::stream)
                .collect(Collectors.toSet());

        if (band.isPresent()) {
            ochSignals = ochSignals.stream()
                        .filter(x -> OpticalBandUtils.computeOpticalBand(x).equals(band.get()))
                        .collect(Collectors.toSet());
        }

        List<OchSignal> listOch = new ArrayList(ochSignals);
        listOch.sort(new DefaultOchSignalComparator());

        return listOch;
    }

    private static final class RestLinkProvider implements LinkProvider {
        @Override
        public ProviderId id() {
            return PID;
        }
    }

    private LinkDescription description(Link link, String key, String value) {
        checkNotNull(key, "Key cannot be null");
        DefaultAnnotations.Builder builder = DefaultAnnotations.builder();
        if (value != null) {
            builder.set(key, value);
        } else {
            builder.remove(key);
        }
        return new DefaultLinkDescription(link.src(),
                link.dst(),
                link.type(),
                link.isExpected(),
                builder.build());
    }
}
