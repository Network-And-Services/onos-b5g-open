/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.net.intent.impl.compiler;

import com.google.common.collect.ImmutableSet;
import org.onosproject.net.*;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.OchSignalCriterion;
import org.onosproject.net.intent.*;
import org.onosproject.net.resource.Resource;
import org.onosproject.net.resource.ResourceAllocation;
import org.onosproject.net.resource.Resources;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.onosproject.net.intent.constraint.PartialFailureConstraint.intentAllowsPartialFailure;

@Component(immediate = true)
public class SinglePointToMultiPointIntentCompiler
        extends ConnectivityIntentCompiler<SinglePointToMultiPointIntent> {

    @Activate
    public void activate() {
        intentManager.registerCompiler(SinglePointToMultiPointIntent.class, this);
    }

    @Deactivate
    public void deactivate() {
        intentManager.unregisterCompiler(SinglePointToMultiPointIntent.class);
    }

    @Override
    public List<Intent> compile(SinglePointToMultiPointIntent intent,
                                List<Intent> installable) {
        Set<Link> links = new HashSet<>();

        final boolean allowMissingPaths = intentAllowsPartialFailure(intent);
        boolean hasPaths = false;
        boolean missingSomePaths = false;

        for (ConnectPoint egressPoint : intent.egressPoints()) {
            if (egressPoint.deviceId().equals(intent.ingressPoint().deviceId())) {
                // Do not need to look for paths, since ingress and egress
                // devices are the same.
                if (deviceService.isAvailable(egressPoint.deviceId())) {
                    hasPaths = true;
                } else {
                    missingSomePaths = true;
                }
                continue;
            }

            Path path = getPath(intent, intent.ingressPoint().deviceId(), egressPoint.deviceId());

            if (path != null) {
                hasPaths = true;
                links.addAll(path.links());
            } else {
                missingSomePaths = true;
            }
        }

        // Allocate bandwidth if a bandwidth constraint is set
        ConnectPoint ingressCP = intent.filteredIngressPoint().connectPoint();
        List<ConnectPoint> egressCPs =
                intent.filteredEgressPoints().stream()
                        .map(fcp -> fcp.connectPoint())
                        .collect(Collectors.toList());

        List<ConnectPoint> pathCPs =
                links.stream()
                     .flatMap(l -> Stream.of(l.src(), l.dst()))
                     .collect(Collectors.toList());

        pathCPs.add(ingressCP);
        pathCPs.addAll(egressCPs);

        allocateBandwidth(intent, pathCPs);

        allocateOpticalResources(intent, pathCPs);

        if (!hasPaths) {
            throw new IntentException("Cannot find any path between ingress and egress points.");
        } else if (!allowMissingPaths && missingSomePaths) {
            throw new IntentException("Missing some paths between ingress and egress points.");
        }

        Intent result = LinkCollectionIntent.builder()
                .appId(intent.appId())
                .key(intent.key())
                .selector(intent.selector())
                .treatment(intent.treatment())
                .links(links)
                .filteredIngressPoints(ImmutableSet.of(intent.filteredIngressPoint()))
                .filteredEgressPoints(intent.filteredEgressPoints())
                .priority(intent.priority())
                .applyTreatmentOnEgress(true)
                .constraints(intent.constraints())
                .resourceGroup(intent.resourceGroup())
                .build();

        return Collections.singletonList(result);
    }

    private void allocateOpticalResources(SinglePointToMultiPointIntent intent, List<ConnectPoint> pathCPs) {
        if (intent == null) {
            throw new IntentException("Intent cannot be null");
        }

        Criterion criterion = intent.selector().getCriterion(Criterion.Type.OCH_SIGID);
        if (!(criterion instanceof OchSignalCriterion)) {
            return;
        }

        OchSignal signal = ((OchSignalCriterion) criterion).lambda();
        List<OchSignal> channels = convertToFlexChannels(signal);
        if (channels.isEmpty()) {
            throw new IntentException("Unable to derive optical slots from signal " + signal);
        }

        Set<ConnectPoint> uniquePathCPs = new HashSet<>(pathCPs);

        Set<ConnectPoint> terminalCPs = new HashSet<>();
        terminalCPs.add(intent.ingressPoint());
        terminalCPs.addAll(intent.egressPoints());

        Set<Resource> resources = new HashSet<>();

        // 1) Allocate terminal OCH ports, if terminal ports are OCH
        for (ConnectPoint cp : terminalCPs) {
            Port port = deviceService.getPort(cp.deviceId(), cp.port());
            if (port == null) {
                throw new IntentException("Port not found for terminal connect point " + cp);
            }

            if (port.type() == Port.Type.OCH) {
                Resource portResource = Resources.discrete(cp.deviceId(), cp.port()).resource();
                if (!resourceService.isAvailable(portResource)) {
                    throw new IntentException("OCH port not available: " + cp);
                }
                resources.add(portResource);
            }
        }

        // 2) Allocate optical spectrum on all ports traversed by the intent tree
        for (ConnectPoint cp : uniquePathCPs) {
            Port port = deviceService.getPort(cp.deviceId(), cp.port());
            if (port == null) {
                throw new IntentException("Port not found for path connect point " + cp);
            }

            Resource portResource = Resources.discrete(cp.deviceId(), cp.port()).resource();

            for (OchSignal channel : channels) {
                Resource lambdaResource = portResource.child(channel);
                if (!resourceService.isAvailable(lambdaResource)) {
                    throw new IntentException("Optical slot not available on " + cp + ": " + channel);
                }
                resources.add(lambdaResource);
            }
        }

        List<ResourceAllocation> allocations =
                resourceService.allocate(intent.key(), new ArrayList<>(resources));

        if (allocations.isEmpty()) {
            throw new IntentException("Unable to allocate optical resources for intent " + intent.key());
        }
    }

    private List<OchSignal> convertToFlexChannels(OchSignal ochSignal) {
        if (ochSignal.gridType() == GridType.FLEX) {
            int startMultiplier = (int) (1 - ochSignal.slotGranularity() + ochSignal.spacingMultiplier());

            return IntStream.range(0, ochSignal.slotGranularity())
                    .mapToObj(x -> OchSignal.newFlexGridSlot(startMultiplier + (2 * x)))
                    .collect(Collectors.toList());

        } else if (ochSignal.gridType() == GridType.DWDM) {
            int startMultiplier = (int) (1 - ochSignal.slotGranularity()
                    + ochSignal.spacingMultiplier() * ochSignal.channelSpacing().frequency().asHz()
                    / ChannelSpacing.CHL_6P25GHZ.frequency().asHz());

            return IntStream.range(0, ochSignal.slotGranularity())
                    .mapToObj(x -> OchSignal.newFlexGridSlot(startMultiplier + (2 * x)))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}