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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.Port;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.Device.Type;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.DeviceServiceAdapter;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.intent.FlowRuleIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentCompiler;
import org.onosproject.net.intent.IntentExtensionService;
import org.onosproject.net.intent.OpticalRoadmIntent;
import org.onosproject.net.intent.PathIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component(immediate = true)
public class OpticalRoadmIntentCompiler implements IntentCompiler<OpticalRoadmIntent> {

    private static final Logger log = LoggerFactory.getLogger(OpticalRoadmIntentCompiler.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected IntentExtensionService intentManager;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService = new DeviceServiceAdapter();

    private ApplicationId appId;
    // Devices which are wavelength transparent and thus do not require wavelength-based match/actions
    private static final Set<Type> TRANSPARENT_DEVICES =
            ImmutableSet.of(Type.OPTICAL_AMPLIFIER, Type.FIBER_SWITCH);
    // Devices which don't accept flow rules
    private static final Set<Type> NO_FLOWRULE_DEVICES =
            ImmutableSet.of(Type.OPTICAL_AMPLIFIER);

    @Activate
    public void activate() {
        appId = coreService.registerApplication("org.onosproject.net.intent");
        intentManager.registerCompiler(OpticalRoadmIntent.class, this);
    }

    @Deactivate
    public void deactivate() {
        intentManager.unregisterCompiler(OpticalRoadmIntent.class);
    }

    @Override
    public List<Intent> compile(OpticalRoadmIntent intent, List<Intent> installable) {
        log.debug("Compiling MediaChannelIntent between {} and {}", intent.src(), intent.dst());

        // Create rules for forward and reverse path
        List<FlowRule> rules = createRules(intent);
        if (intent.isBidirectional()) {
            rules.addAll(createReverseRules(intent));
        }

        return Collections.singletonList(
                new FlowRuleIntent(appId,
                        intent.key(),
                        rules,
                        intent.resources(),
                        PathIntent.ProtectionType.PRIMARY,
                        intent.resourceGroup()
                )
        );
    }

    /**
     * Create rules for the forward path of the intent.
     *
     * @param intent the intent
     * @return list of flow rules
     */
    private List<FlowRule> createRules(OpticalRoadmIntent intent) {

        List<FlowRule> rules = new LinkedList<>();

        /*
         * Special case for 0 hop when srcDeviceId = dstDeviceId
         * This is almost no-sense in optical - consider Error Message here
         */
        if (intent.src().deviceId().equals(intent.dst().deviceId())) {
            //Only one rule is needed
            TrafficSelector selector = DefaultTrafficSelector.builder()
                    .matchInPort(intent.src().port())
                    .add(Criteria.matchLambda(intent.lambda()))
                    .add(Criteria.matchOchSignalType(intent.signalType()))
                    .build();

            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .add(Instructions.modL0Lambda(intent.lambda()))
                    .setOutput(intent.dst().port())
                    .build();

            FlowRule rule = DefaultFlowRule.builder()
                    .forDevice(intent.src().deviceId())
                    .withSelector(selector)
                    .withTreatment(treatment)
                    .withPriority(intent.priority())
                    .fromApp(appId)
                    .makePermanent()
                    .build();

            rules.add(rule);

            return rules;
        }

        ConnectPoint currentConnectPoint = intent.src();

        //Build the rules for the first ROADM and all intermediate ROADMs
        for (Link link : intent.path().links()) {
            TrafficSelector selector = DefaultTrafficSelector.builder()
                    .matchInPort(currentConnectPoint.port())
                    .add(Criteria.matchLambda(intent.lambda()))
                    .add(Criteria.matchOchSignalType(intent.signalType()))
                    .build();

            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .add(Instructions.modL0Lambda(intent.lambda()))
                    .setOutput(link.src().port())
                    .build();

            FlowRule rule = DefaultFlowRule.builder()
                    .forDevice(currentConnectPoint.deviceId())
                    .withSelector(selector)
                    .withTreatment(treatment)
                    .withPriority(intent.priority())
                    .fromApp(appId)
                    .makePermanent()
                    .build();

            rules.add(rule);

            currentConnectPoint = link.dst();
        }

        //Build the rule for the last ROADM
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchInPort(currentConnectPoint.port())
                .add(Criteria.matchLambda(intent.lambda()))
                .add(Criteria.matchOchSignalType(intent.signalType()))
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .add(Instructions.modL0Lambda(intent.lambda()))
                .setOutput(intent.dst().port())
                .build();

        FlowRule rule = DefaultFlowRule.builder()
                .forDevice(currentConnectPoint.deviceId())
                .withSelector(selector)
                .withTreatment(treatment)
                .withPriority(intent.priority())
                .fromApp(appId)
                .makePermanent()
                .build();

        rules.add(rule);

        return rules;
    }

    /**
     * Create rules for the reverse path of the intent.
     *
     * @param intent the intent
     * @return list of flow rules
     */
    private List<FlowRule> createReverseRules(OpticalRoadmIntent intent) {

        List<FlowRule> rules = new LinkedList<>();

        /*
         * Special case for 0 hop when srcDeviceId = dstDeviceId
         * This is almost no-sense in optical - consider Error Message here
         */
        if (intent.src().deviceId().equals(intent.dst().deviceId())) {
            //Only one rule is needed
            TrafficSelector selector = DefaultTrafficSelector.builder()
                    .matchInPort(reversePort(intent.dst().deviceId(),intent.dst().port()))
                    .add(Criteria.matchLambda(intent.lambda()))
                    .add(Criteria.matchOchSignalType(intent.signalType()))
                    .build();

            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .add(Instructions.modL0Lambda(intent.lambda()))
                    .setOutput(reversePort(intent.src().deviceId(),intent.src().port()))
                    .build();

            FlowRule rule = DefaultFlowRule.builder()
                    .forDevice(intent.dst().deviceId())
                    .withSelector(selector)
                    .withTreatment(treatment)
                    .withPriority(intent.priority())
                    .fromApp(appId)
                    .makePermanent()
                    .build();

            rules.add(rule);

            return rules;
        }

        ConnectPoint current = intent.dst();

        for (Link link : Lists.reverse(intent.path().links())) {
            TrafficSelector selector = DefaultTrafficSelector.builder()
                    .matchInPort(reversePort(current.deviceId(),current.port()))
                    .add(Criteria.matchLambda(intent.lambda()))
                    .add(Criteria.matchOchSignalType(intent.signalType()))
                    .build();

            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .add(Instructions.modL0Lambda(intent.lambda()))
                    .setOutput(reversePort(link.dst().deviceId(),link.dst().port()))
                    .build();

            FlowRule rule = DefaultFlowRule.builder()
                    .forDevice(current.deviceId())
                    .withSelector(selector)
                    .withTreatment(treatment)
                    .withPriority(intent.priority())
                    .fromApp(appId)
                    .makePermanent()
                    .build();

            rules.add(rule);

            current = link.src();
        }

        //Build the rule for the last ROADM
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchInPort(reversePort(current.deviceId(),current.port()))
                .add(Criteria.matchLambda(intent.lambda()))
                .add(Criteria.matchOchSignalType(intent.signalType()))
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .add(Instructions.modL0Lambda(intent.lambda()))
                .setOutput(reversePort(intent.src().deviceId(),intent.src().port()))
                .build();

        FlowRule rule = DefaultFlowRule.builder()
                .forDevice(current.deviceId())
                .withSelector(selector)
                .withTreatment(treatment)
                .withPriority(intent.priority())
                .fromApp(appId)
                .makePermanent()
                .build();

        rules.add(rule);

        return rules;
    }

    /**
     * Returns the PortNum of reverse port if annotation is present, otherwise return PortNum of the port itself.
     * In the OpenROADM YANG models it is used the term "partner-port.
     *
     * @param portNumber the port
     * @return the PortNum of reverse port if annotation is present, otherwise PortNum of the port itself.
     */
    private PortNumber reversePort(DeviceId deviceId, PortNumber portNumber) {
        Port port = deviceService.getPort(deviceId, portNumber);

        String reversePort = port.annotations().value(OpticalRoadmIntent.REVERSE_PORT_ANNOTATION_KEY);
        if (reversePort != null) {
            PortNumber reversePortNumber = PortNumber.portNumber(reversePort);
            return reversePortNumber;
        } else {
            return portNumber;
        }
    }

    /**
     * Returns true if device does not accept flow rules, false otherwise.
     *
     * @param deviceId the device
     * @return true if device does not accept flow rule, false otherwise
     */
    private boolean isNoFlowRule(DeviceId deviceId) {
        return NO_FLOWRULE_DEVICES.contains(
                Optional.ofNullable(deviceService.getDevice(deviceId))
                        .map(Device::type)
                        .orElse(Type.OTHER));
    }

    /**
     * Returns true if device is wavelength transparent, false otherwise.
     *
     * @param deviceId the device
     * @return true if wavelength transparent, false otherwise
     */
    private boolean isTransparent(DeviceId deviceId) {
        return TRANSPARENT_DEVICES.contains(
                Optional.ofNullable(deviceService.getDevice(deviceId))
                        .map(Device::type)
                        .orElse(Type.OTHER));
    }
}
