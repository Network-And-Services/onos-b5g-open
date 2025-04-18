/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.net.optical.intent.impl.compiler;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.onosproject.net.*;
import org.onosproject.net.intent.*;
import org.onosproject.net.optical.OmsPort;
import org.onosproject.net.intent.OpticalConnectivityIntent;
import org.onosproject.net.resource.Resource;
import org.onosproject.net.resource.ResourceAllocation;
import org.onosproject.net.resource.ResourceService;
import org.onosproject.net.resource.Resources;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onlab.graph.ScalarWeight;
import org.onlab.graph.Weight;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.optical.OchPort;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.topology.LinkWeigher;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Collection;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static org.onosproject.net.optical.device.OpticalDeviceServiceView.opticalView;

/**
 * An intent compiler for {@link OpticalConnectivityIntent}.
 */
@Component(immediate = true)
public class OpticalConnectivityIntentCompiler implements IntentCompiler<OpticalConnectivityIntent> {

    private static final Logger log = LoggerFactory.getLogger(OpticalConnectivityIntentCompiler.class);
    // By default, allocate 50 GHz lambdas (4 slots of 12.5 GHz) for each intent.
    private static final int DEFAULT_SLOT_GRANULARITY = 4;
    private static final GridType DEFAULT_GRID_TYPE = GridType.DWDM;
    private static final ChannelSpacing DEFAULT_CHANNEL_SPACING = ChannelSpacing.CHL_50GHZ;
    private static final ProviderId PROVIDER_ID = new ProviderId("opticalConnectivityIntent",
            "org.onosproject.net.optical.intent");

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected IntentExtensionService intentManager;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ResourceService resourceService;

    @Activate
    public void activate() {
        deviceService = opticalView(deviceService);
        intentManager.registerCompiler(OpticalConnectivityIntent.class, this);
    }

    @Deactivate
    public void deactivate() {
        intentManager.unregisterCompiler(OpticalConnectivityIntent.class);
    }

    /**
     * Path computation and spectrum assignment.
     * Supports fixed and flex grids.
     *
     * Path and signal can be provided within the intent.
     * If not provided they are computed in this function.
     * If signal is not provided a default channel is allocated with width 50 GHz, on the 50 GHz spacing.
     *
     * @param intent this intent (used for resource tracking)
     * @param installable intents
     * @return list of optical path intents (one per direction)
     */
    @Override
    public List<Intent> compile(OpticalConnectivityIntent intent, List<Intent> installable) {
        // Check if source and destination are optical OCh ports
        ConnectPoint src = intent.getSrc();
        ConnectPoint dst = intent.getDst();

        List<Resource> ochPortResources = new LinkedList<>();

        log.info("Compiling optical connectivity intent between {} and {}", src, dst);

        // Release of intent resources here is only a temporary solution for handling the
        // case of recompiling due to intent restoration (when intent state is FAILED).
        // TODO: try to release intent resources in IntentManager.
        resourceService.release(intent.key());

        // In case of intent between OCH ports, check OCH src and dst port availability
        // If ports are not available, compilation fails
        // Else add port to resource reservation list
        // This is not executed for OMS ports because OMS ports remain available on other channels
        if ((deviceService.getPort(src.deviceId(), src.port()) instanceof OchPort) &&
             deviceService.getPort(dst.deviceId(), dst.port()) instanceof OchPort) {

            Resource srcPortResource = Resources.discrete(src.deviceId(), src.port()).resource();
            Resource dstPortResource = Resources.discrete(dst.deviceId(), dst.port()).resource();

            if (!Stream.of(srcPortResource, dstPortResource).allMatch(resourceService::isAvailable)) {
                log.error("Ports OCH for the intent are not available. Intent: {}", intent);
                throw new OpticalIntentCompilationException("Ports for the intent are not available. Intent: " + intent);
            }
            ochPortResources.add(srcPortResource);
            ochPortResources.add(dstPortResource);
        }

        if ((deviceService.getPort(src.deviceId(), src.port()) instanceof OmsPort) &&
                deviceService.getPort(dst.deviceId(), dst.port()) instanceof OmsPort) {
            //TODO in case of OMS ports allocate the used channel in the resources to be reserved
            log.warn("Intent between OMS ports, availability of channel on src and dst ports is not checked TODO.");
        }

        // If there is a valid suggestedPath, use this path without further checking
        // Otherwise trigger path computation
        List<Path> paths;
        if (intent.suggestedPath().isPresent()) {
            paths = List.of(intent.suggestedPath().get());
            log.info("Using suggested path {}", paths.size());
        } else {
            paths = getOpticalPaths(intent);
            log.info("Computed paths {}", paths.size());
        }

        Optional<Map.Entry<Path, List<OchSignal>>> found = Optional.empty();
        for (Path path : paths) {

            boolean attempt = false;
            if (intent.ochSignal().isPresent()) {
                List<Resource> resourcesLocal =  new LinkedList<>();
                resourcesLocal.addAll(ochPortResources);
                resourcesLocal.addAll(convertToResources(path, convertToFlexChannels(intent.ochSignal().get())));
                log.debug("Current resources {}", resourcesLocal);
                attempt = allocateResourcesAttempt(intent, resourcesLocal);
            } else {
                log.error("This request do not include a suggested lambda");
                //TODO use findCommonLambdas to support this case
                throw new OpticalIntentCompilationException("This path has not a suggested lambda for intent " + intent);
            }

            if (attempt) {
                log.info("This path has common lambdas");
                found = Optional.of(Maps.immutableEntry(path, findFirstAvailableLambda(intent, path)));
            } else {
                log.info("This path has NO common lambdas");
            }
        }

        //Evaluate lambdas availability
        /*Optional<Map.Entry<Path, List<OchSignal>>> found = Optional.empty();
        for (Path path : paths) {
            Set<OchSignal> commonLambdas = findCommonLambdas(path);
            if (!commonLambdas.isEmpty()) {
                log.info("This path has common lambdas");
                found = Optional.of(Maps.immutableEntry(path, findFirstAvailableLambda(intent, path)));
            } else {
                log.info("This path has NO common lambdas");
            }
        }*/
        // Find first path that has the required resources
        /*Optional<Map.Entry<Path, List<OchSignal>>> found = paths.stream()
                .map(path -> Maps.immutableEntry(path, findFirstAvailableLambda(intent, path)))
                .filter(entry -> !entry.getValue().isEmpty())
                .filter(entry -> convertToResources(entry.getKey(),
                        entry.getValue()).stream().allMatch(resourceService::isAvailable))
                .findFirst();*/

        // Allocate resources and create optical path intent
        if (found.isPresent()) {
            log.info("Suitable path and lambdas FOUND for intent {}", intent);
            //resources.addAll(convertToResources(found.get().getKey(), found.get().getValue()));
            //allocateResources(intent, resources);

            //If och signal is specified use FLEX grid or map it on specified spacing
            if (intent.ochSignal().isPresent()) {
                if (intent.ochSignal().get().gridType() == GridType.FLEX) {
                    return ImmutableList.of(createIntent(intent, found.get().getKey(), intent.ochSignal().get()));
                } else {
                    OchSignal ochSignal = OchSignal
                            .toFixedGrid(found.get().getValue(), intent.ochSignal().get().channelSpacing());

                    return ImmutableList.of(createIntent(intent, found.get().getKey(), ochSignal));
                }
            }

            //If och signal is not specified a 50 GHz slot is assumed
            OchSignal ochSignal = OchSignal
                    .toFixedGrid(found.get().getValue(), DEFAULT_CHANNEL_SPACING);

            return ImmutableList.of(createIntent(intent, found.get().getKey(), ochSignal));
        } else {
            log.error("Unable to find suitable lightpath for intent {}", intent);
            throw new OpticalIntentCompilationException("Unable to find suitable lightpath for intent " + intent);
        }
    }

    /**
     * Create installable optical path intent.
     * Supports fixed and flex grids.
     *
     * @param parentIntent this intent (used for resource tracking)
     * @param path         the path to use
     * @param lambda       the lambda to use
     * @return optical path intent
     */
    private Intent createIntent(OpticalConnectivityIntent parentIntent, Path path, OchSignal lambda) {
        OchSignalType signalType;
        if (lambda.gridType().equals(GridType.FLEX)) {
            signalType = OchSignalType.FLEX_GRID;
        } else {
            signalType = OchSignalType.FIXED_GRID;
        }

        Device.Type srcDeviceType = deviceService.getDevice(parentIntent.getSrc().deviceId()).type();
        Device.Type dstDeviceType = deviceService.getDevice(parentIntent.getDst().deviceId()).type();
        if (!srcDeviceType.equals(dstDeviceType)) {
            log.error("OpticalConnectivityIntent requested between two devices of different type");
        }

        if (srcDeviceType.equals(Device.Type.ROADM) && dstDeviceType.equals(Device.Type.ROADM)) {
            log.warn("OpticalConnectivityIntent requested between two ROADMs");
            log.warn("OpticalMediaChannelIntent building...");
            return OpticalRoadmIntent.builder()
                    .appId(parentIntent.appId())
                    .key(parentIntent.key())
                    .priority(parentIntent.priority())
                    .src(parentIntent.getSrc())
                    .dst(parentIntent.getDst())
                    .path(path)
                    .lambda(lambda)
                    .signalType(signalType)
                    .bidirectional(parentIntent.isBidirectional())
                    .resourceGroup(parentIntent.resourceGroup())
                    .build();
        }
        log.warn("OpticalConnectivityIntent requested between two Transponders");
        log.warn("OpticalPathIntent building...");

        return OpticalPathIntent.builder()
                .appId(parentIntent.appId())
                .key(parentIntent.key())
                .priority(parentIntent.priority())
                .src(parentIntent.getSrc())
                .dst(parentIntent.getDst())
                .path(path)
                .lambda(lambda)
                .signalType(signalType)
                .bidirectional(parentIntent.isBidirectional())
                .resourceGroup(parentIntent.resourceGroup())
                .build();
    }

    /**
     * Convert given lambda as discrete resource of all path ports.
     *
     * @param path   the path
     * @param lambda the lambda
     * @return list of discrete resources
     */
    private List<Resource> convertToResources(Path path, Collection<OchSignal> lambda) {
        return path.links().stream()
                .flatMap(x -> Stream.of(
                        Resources.discrete(x.src().deviceId(),
                                deviceService.getPort(x.src().deviceId(), x.src().port()).number()).resource(),
                        Resources.discrete(x.dst().deviceId(),
                                deviceService.getPort(x.dst().deviceId(), x.dst().port()).number()).resource()
                ))
                .flatMap(x -> lambda.stream().map(x::child))
                .collect(Collectors.toList());
    }

    /**
     * Reserve all required resources for this intent.
     *
     * @param intent    the intent
     * @param resources list of resources to reserve
     */
    private void allocateResources(Intent intent, List<Resource> resources) {
        List<ResourceAllocation> allocations = resourceService.allocate(intent.key(), resources);
        if (allocations.isEmpty()) {
            log.error("Resource allocation for {} failed (resource request: {})", intent.key(), resources);
            if (log.isDebugEnabled()) {
                log.debug("requested resources:\n\t{}", resources.stream()
                        .map(Resource::toString)
                        .collect(Collectors.joining("\n\t")));
            }
            throw new OpticalIntentCompilationException("Unable to allocate resources: " + resources);
        }
    }

    private boolean allocateResourcesAttempt(Intent intent, List<Resource> resources) {
        List<ResourceAllocation> allocations = resourceService.allocate(intent.key(), resources);
        if (allocations.isEmpty()) {
            return false;
        }
        return true;
    }

    private List<OchSignal> convertToFlexChannels(OchSignal ochSignal) {
        //create lambdas w.r.t. slotGanularity/slotWidth
        if (ochSignal.gridType() == GridType.FLEX) {
            int startMultiplier = (int) (1 - ochSignal.slotGranularity() + ochSignal.spacingMultiplier());

            List<OchSignal> channels = IntStream.range(0, ochSignal.slotGranularity())
                    .mapToObj(x -> OchSignal.newFlexGridSlot(startMultiplier + (2 * x)))
                    .collect(Collectors.toList());

            log.info("Grid type {} channels {}", ochSignal.gridType(), channels);
            return channels;
        } else if (ochSignal.gridType() == GridType.DWDM) {
            int startMultiplier = (int) (1 - ochSignal.slotGranularity() +
                    ochSignal.spacingMultiplier() * ochSignal.channelSpacing().frequency().asHz() /
                            ChannelSpacing.CHL_6P25GHZ.frequency().asHz());

            List<OchSignal> channels = IntStream.range(0, ochSignal.slotGranularity())
                    .mapToObj(x -> OchSignal.newFlexGridSlot(startMultiplier + (2 * x)))
                    .collect(Collectors.toList());

            log.info("Grid type {} channels {}", ochSignal.gridType(), channels);
            return channels;
        }

        log.error("Grid type: {} is not supported", ochSignal.gridType());
        return Collections.emptyList();
    }

    /**
     * Find the first available lambda on the given path by checking all the port resources.
     *
     * @param path the path
     * @return list of consecutive and available OChSignals
     */
    private List<OchSignal> findFirstAvailableLambda(OpticalConnectivityIntent intent, Path path) {
        log.debug("Spectrum research for path {}", path.links());

        /*Set<OchSignal> lambdas = findCommonLambdas(path);
        if (lambdas.isEmpty()) {
            return Collections.emptyList();
        }*/

        if (intent.ochSignal().isPresent()) {
            //create lambdas w.r.t. slotGanularity/slotWidth
            OchSignal ochSignal = intent.ochSignal().get();
            if (ochSignal.gridType() == GridType.FLEX) {
                int startMultiplier = (int) (1 - ochSignal.slotGranularity() + ochSignal.spacingMultiplier());

                List<OchSignal> channels = IntStream.range(0, ochSignal.slotGranularity())
                        .mapToObj(x -> OchSignal.newFlexGridSlot(startMultiplier + (2 * x)))
                        .collect(Collectors.toList());

                //if (lambdas.containsAll(channels)) {
                    log.info("Selected suggested lambdas FLEX", channels);
                    return channels;
                //} else {
                    //log.info("This path does not contain specified signal FLEX {}", channels);
                    //log.info("List of available channels FLEX {}", lambdas);
                    //return Collections.emptyList();
                //}
            } else if (ochSignal.gridType() == GridType.DWDM) {
                int startMultiplier = (int) (1 - ochSignal.slotGranularity() +
                        ochSignal.spacingMultiplier() * ochSignal.channelSpacing().frequency().asHz() /
                                ChannelSpacing.CHL_6P25GHZ.frequency().asHz());

                List<OchSignal> channels = IntStream.range(0, ochSignal.slotGranularity())
                        .mapToObj(x -> OchSignal.newFlexGridSlot(startMultiplier + (2 * x)))
                        .collect(Collectors.toList());

                //if (lambdas.containsAll(channels)) {
                    log.info("Selected suggested lambdas DWDM", channels);
                    return channels;
                //} else {
                    //log.info("This path does not contain specified signal DWDM {}", channels);
                    //log.info("List of available channels DWDM {}", lambdas);
                    //return Collections.emptyList();
                //}
            }
            //TODO: add support for other gridTypes
            log.error("Grid type: {} not supported for user defined signal intents", ochSignal.gridType());
            return Collections.emptyList();
        } else {
            return findFirstLambda(findCommonLambdas(path), DEFAULT_SLOT_GRANULARITY);
        }
    }

    /**
     * Find common lambdas on all ports that compose the path.
     *
     * @param path the path
     * @return set of common lambdas
     */
    private Set<OchSignal> findCommonLambdas(Path path) {

        Set<OchSignal> ochSignals = path.links().stream()
                .flatMap(x -> Stream.of(
                        Resources.discrete(x.src().deviceId(),
                                deviceService.getPort(x.src().deviceId(), x.src().port()).number()).id(),
                        Resources.discrete(x.dst().deviceId(),
                                deviceService.getPort(x.dst().deviceId(), x.dst().port()).number()).id()
                ))
                .map(x -> resourceService.getAvailableResourceValues(x, OchSignal.class))
                .map(x -> (Set<OchSignal>) ImmutableSet.copyOf(x))
                .reduce(Sets::intersection)
                .orElse(Collections.emptySet());

        if (ochSignals.isEmpty()) {
            log.warn("Common lambdas not found");
        } else {
            log.debug("Common lambdas found {}", ochSignals);
        }

        return ochSignals;
    }

    /**
     * Returns list of consecutive resources in given set of lambdas.
     *
     * @param lambdas list of lambdas
     * @param count   number of consecutive lambdas to return
     * @return list of consecutive lambdas
     */
    private List<OchSignal> findFirstLambda(Set<OchSignal> lambdas, int count) {
        // Sort available lambdas
        List<OchSignal> lambdaList = new ArrayList<>(lambdas);
        lambdaList.sort(new DefaultOchSignalComparator());
        //Means there is only exactly one set of lambdas available
        if (lambdaList.size() == count) {
            return lambdaList;
        }
        // Look ahead by count and ensure spacing multiplier is as expected (i.e., no gaps)
        for (int i = 0; i < lambdaList.size() - count; i++) {
            if (lambdaList.get(i).spacingMultiplier() + 2 * count ==
                    lambdaList.get(i + count).spacingMultiplier()) {
                return lambdaList.subList(i, i + count);
            }
        }

        return Collections.emptyList();
    }

    private ConnectPoint staticPort(ConnectPoint connectPoint) {
        Port port = deviceService.getPort(connectPoint.deviceId(), connectPoint.port());

        String staticPort = port.annotations().value(AnnotationKeys.STATIC_PORT);

        // FIXME: need a better way to match the port
        if (staticPort != null) {
            for (Port p : deviceService.getPorts(connectPoint.deviceId())) {
                if (staticPort.equals(p.number().name())) {
                    return new ConnectPoint(p.element().id(), p.number());
                }
            }
        }

        return null;
    }

    /**
     * Calculates optical paths in WDM topology.
     *
     * @param intent optical connectivity intent
     * @return set of paths in WDM topology
     */
    private List<Path> getOpticalPaths(OpticalConnectivityIntent intent) {
        // Route in WDM topology
        Topology topology = topologyService.currentTopology();
        //TODO: refactor with LinkWeigher class Implementation
        LinkWeigher weight = new LinkWeigher() {

            @Override
            public Weight getInitialWeight() {
                return ScalarWeight.toWeight(0.0);
            }

            @Override
            public Weight getNonViableWeight() {
                return ScalarWeight.NON_VIABLE_WEIGHT;
            }

            /**
             *
             * @param edge edge to be weighed
             * @return the metric retrieved from the annotations otherwise 1
             */
            @Override
            public Weight weight(TopologyEdge edge) {

                log.debug("Link {} metric {}", edge.link(), edge.link().annotations().value("metric"));

                // Disregard inactive or non-optical links
                if (edge.link().state() == Link.State.INACTIVE) {
                    return ScalarWeight.toWeight(-1);
                }
                if (edge.link().type() != Link.Type.OPTICAL) {
                    return ScalarWeight.toWeight(-1);
                }
                // Adhere to static port mappings
                DeviceId srcDeviceId = edge.link().src().deviceId();
                if (srcDeviceId.equals(intent.getSrc().deviceId())) {
                    ConnectPoint srcStaticPort = staticPort(intent.getSrc());
                    if (srcStaticPort != null) {
                        return ScalarWeight.toWeight(srcStaticPort.equals(edge.link().src()) ? 1 : -1);
                    }
                }
                DeviceId dstDeviceId = edge.link().dst().deviceId();
                if (dstDeviceId.equals(intent.getDst().deviceId())) {
                    ConnectPoint dstStaticPort = staticPort(intent.getDst());
                    if (dstStaticPort != null) {
                        return ScalarWeight.toWeight(dstStaticPort.equals(edge.link().dst()) ? 1 : -1);
                    }
                }

                Annotations annotations = edge.link().annotations();
                if (annotations != null &&
                        annotations.value("metric") != null && !annotations.value("metric").isEmpty()) {
                    double metric = Double.parseDouble(annotations.value("metric"));
                    return ScalarWeight.toWeight(metric);
                } else {
                    return ScalarWeight.toWeight(1);
                }
            }
        };

        ConnectPoint start = intent.getSrc();
        ConnectPoint end = intent.getDst();

        // 0 hop case
        if (start.deviceId().equals(end.deviceId())) {
            log.debug("install optical intent for 0 hop i.e srcDeviceId=dstDeviceId");
            DefaultLink defaultLink = DefaultLink.builder()
                    .providerId(PROVIDER_ID)
                    .src(start)
                    .dst(end)
                    .state(Link.State.ACTIVE)
                    .type(Link.Type.DIRECT)
                    .isExpected(true)
                    .build();
            List<Link> links = ImmutableList.<Link>builder().add(defaultLink).build();
            Annotations annotations = DefaultAnnotations.builder().build();
            DefaultPath defaultPath = new DefaultPath(PROVIDER_ID, links, null, annotations);
            return ImmutableList.<Path>builder().add(defaultPath).build();
        }

        //head link's src port should be same as intent src port and tail link dst port
        //should be same as intent dst port in the path.
        //by alessio... just removed this contraint to allow intents between OCH ports of ROADMs
        Stream<Path> paths = topologyService.getKShortestPaths(topology,
                start.deviceId(),
                end.deviceId(),
                weight);
        //.filter(p -> p.links().get(0).src().port().equals(start.port()) &&
        //        p.links().get(p.links().size() - 1).dst().port().equals(end.port()));
        List<Path> pathList = paths.collect(Collectors.toList());

        return pathList;
    }
}
