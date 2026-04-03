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

package org.onosproject.net.optical.util;

import org.onosproject.net.*;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.intent.*;
import org.onosproject.net.optical.OduCltPort;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.optical.OchPort;

import static org.onosproject.net.Device.Type;
import static org.onosproject.net.optical.device.OpticalDeviceServiceView.opticalView;

import org.onosproject.net.optical.OmsPort;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Utility class for optical intents.
 */
public final class OpticalIntentUtility {

    private static final Logger log = getLogger(OpticalIntentUtility.class);

    private OpticalIntentUtility() {
    }

    /**
     * Returns a new optical intent created from the method parameters.
     *
     * @param ingress ingress description (device/port)
     * @param egress egress description (device/port)
     * @param deviceService device service
     * @param key intent key
     * @param appId application id
     * @param bidirectional if this argument is true, the optical link created
     * will be bidirectional, otherwise the link will be unidirectional.
     * @param signal optical signal
     * @param suggestedPath suggested path
     *
     * @return created intent
     */
    public static Intent createOpticalIntent(ConnectPoint ingress, ConnectPoint
            egress, DeviceService deviceService, Key key, ApplicationId appId, boolean
            bidirectional, OchSignal signal, Path suggestedPath) {

        Intent intent = null;

        if (ingress == null || egress == null) {
            log.debug("Invalid endpoint(s); could not create optical intent");
            return intent;
        }

        DeviceService ds = opticalView(deviceService);

        Port srcPort = ds.getPort(ingress.deviceId(), ingress.port());
        Port dstPort = ds.getPort(egress.deviceId(), egress.port());

        if (srcPort instanceof OduCltPort && dstPort instanceof OduCltPort) {
            Device srcDevice = ds.getDevice(ingress.deviceId());
            Device dstDevice = ds.getDevice(egress.deviceId());

            // continue only if both OduClt port's Devices are of the same type
            if (!(srcDevice.type().equals(dstDevice.type()))) {
                log.debug("Devices without same deviceType: SRC {} and DST={}", srcDevice.type(), dstDevice.type());
                return intent;
            }

            CltSignalType signalType = ((OduCltPort) srcPort).signalType();
            if (Type.ROADM.equals(srcDevice.type()) ||
                    Type.ROADM_OTN.equals(srcDevice.type()) ||
                    Type.OLS.equals(srcDevice.type()) ||
                    Type.TERMINAL_DEVICE.equals(srcDevice.type())) {
                intent = OpticalCircuitIntent.builder()
                        .appId(appId)
                        .key(key)
                        .src(ingress)
                        .dst(egress)
                        .signalType(signalType)
                        .bidirectional(bidirectional)
                        .ochSignal(Optional.ofNullable(signal))
                        .suggestedPath(Optional.ofNullable(suggestedPath))
                        .build();
            } else if (Type.OTN.equals(srcDevice.type())) {
                intent = OpticalOduIntent.builder()
                        .appId(appId)
                        .key(key)
                        .src(ingress)
                        .dst(egress)
                        .signalType(signalType)
                        .bidirectional(bidirectional)
                        .build();
            } else {
                log.debug("Wrong Device Type for connect points {} and {}", ingress, egress);
            }
        } else if (srcPort instanceof OchPort && dstPort instanceof OchPort) {

            log.info("Creating an optical intent between connect points {} and {}", ingress, egress);

            OduSignalType signalType = ((OchPort) srcPort).signalType();
            intent = OpticalConnectivityIntent.builder()
                    .appId(appId)
                    .key(key)
                    .src(ingress)
                    .dst(egress)
                    .signalType(signalType)
                    .bidirectional(bidirectional)
                    .ochSignal(Optional.ofNullable(signal))
                    .suggestedPath(Optional.ofNullable(suggestedPath))
                    .build();
        } else {
            log.debug("Unable to create optical intent between connect points {} and {}", ingress, egress);
        }

        return intent;
    }

    /**
     * Returns a new optical intent created from the method parameters, strict suggestedPath is specified.
     *
     * @param ingress ingress description (device/port)
     * @param egress egress description (device/port)
     * @param deviceService device service
     * @param key intent key
     * @param appId application id
     * @param bidirectional if this argument is true, the optical link created
     * will be bidirectional, otherwise the link will be unidirectional.
     * @param signal optical signal
     * @param suggestedPath suggested path for the intent
     *
     * @return created intent
     */
    public static Intent createExplicitOpticalIntent(ConnectPoint ingress, ConnectPoint
            egress, DeviceService deviceService, Key key, ApplicationId appId, boolean
                                                     bidirectional, OchSignal signal, Path suggestedPath) {

        Intent intent = null;

        if (ingress == null || egress == null) {
            log.error("Invalid endpoint(s); could not create optical intent");
            return null;
        }

        DeviceService ds = opticalView(deviceService);

        Port srcPort = ds.getPort(ingress.deviceId(), ingress.port());
        Port dstPort = ds.getPort(egress.deviceId(), egress.port());

        if (srcPort instanceof OduCltPort && dstPort instanceof OduCltPort) {

            log.info("Creating an optical intent between ODU connect points {} and {}", ingress, egress);

            Device srcDevice = ds.getDevice(ingress.deviceId());
            Device dstDevice = ds.getDevice(egress.deviceId());

            // continue only if both OduClt port's Devices are of the same type
            if (!(srcDevice.type().equals(dstDevice.type()))) {
                log.debug("Devices without same deviceType: SRC={} and DST={}", srcDevice.type(), dstDevice.type());
                return null;
            }

            CltSignalType signalType = ((OduCltPort) srcPort).signalType();
            if (Type.ROADM.equals(srcDevice.type()) ||
                    Type.ROADM_OTN.equals(srcDevice.type()) ||
                    Type.OLS.equals(srcDevice.type()) ||
                    Type.TERMINAL_DEVICE.equals(srcDevice.type())) {
                intent = OpticalCircuitIntent.builder()
                        .appId(appId)
                        .key(key)
                        .src(ingress)
                        .dst(egress)
                        .signalType(signalType)
                        .bidirectional(bidirectional)
                        .ochSignal(Optional.ofNullable(signal))
                        .suggestedPath(Optional.ofNullable(suggestedPath))
                        .build();
            } else if (Type.OTN.equals(srcDevice.type())) {
                intent = OpticalOduIntent.builder()
                        .appId(appId)
                        .key(key)
                        .src(ingress)
                        .dst(egress)
                        .signalType(signalType)
                        .bidirectional(bidirectional)
                        .build();
            } else {
                log.error("Wrong Device Type for connect points: " +
                        "ingress {} of type {}; egress {} of type {}",
                        ingress, srcDevice.type(), egress, dstDevice.type());
            }

            return intent;
        }

        if (srcPort instanceof OchPort && dstPort instanceof OchPort) {

            log.info("Creating an optical intent between OCH connect points {} and {}", ingress, egress);

            OduSignalType signalType = ((OchPort) srcPort).signalType();
            intent = OpticalConnectivityIntent.builder()
                    .appId(appId)
                    .key(key)
                    .src(ingress)
                    .dst(egress)
                    .signalType(signalType)
                    .bidirectional(bidirectional)
                    .ochSignal(Optional.ofNullable(signal))
                    .suggestedPath(Optional.ofNullable(suggestedPath))
                    .build();

            return intent;
        }

        if (srcPort instanceof OmsPort && dstPort instanceof OmsPort) {

            log.info("Creating an optical intent between OMS connect points {} and {}", ingress, egress);

            OduSignalType signalType = OduSignalType.ODU4;
            intent = OpticalConnectivityIntent.builder()
                    .appId(appId)
                    .key(key)
                    .src(ingress)
                    .dst(egress)
                    .signalType(signalType)
                    .bidirectional(bidirectional)
                    .ochSignal(Optional.ofNullable(signal))
                    .suggestedPath(Optional.ofNullable(suggestedPath))
                    .build();

            return intent;
        }

        log.error("Unable to create explicit optical intent {} and {}, types {} and {}",
                    ingress, egress,
                    srcPort.type(), dstPort.type());

        return null;
    }

    public static Intent createP2mpOpticalIntent(ConnectPoint ingress, Set<ConnectPoint> egressSet,
                                                 DeviceService deviceService, Key key, ApplicationId appId,
                                                 OchSignal signal) {
        Intent intent = null;

        if (ingress == null) {
            throw new IllegalArgumentException("Ingress connect point cannot be null");
        }
        if (egressSet == null || egressSet.isEmpty()) {
            throw new IllegalArgumentException("Egress connect point set cannot be null or empty");
        }
        if (deviceService == null) {
            throw new IllegalArgumentException("DeviceService cannot be null");
        }

        if (!connectPointExists(ingress, deviceService)) {
            throw new IllegalArgumentException("Ingress connect point does not exist: " + ingress);
        }

        for (ConnectPoint egress : egressSet) {
            if (egress == null) {
                throw new IllegalArgumentException("Egress connect point cannot be null");
            }
            if (!connectPointExists(egress, deviceService)) {
                throw new IllegalArgumentException("Egress connect point does not exist: " + egress);
            }
        }

        if (egressSet.contains(ingress)) {
            throw new IllegalArgumentException("Egress set must not contain ingress");
        }

        log.info("Creating P2MP intent - ingress: {}, egressList: {}", ingress, egressSet);

        FilteredConnectPoint filteredIngress = new FilteredConnectPoint(ingress);

        Set<FilteredConnectPoint> filteredEgressSet = egressSet.stream()
                .map(FilteredConnectPoint::new)
                .collect(Collectors.toSet());

        OchSignalType signalType;

        switch (signal.gridType()) {
            case FLEX:
                signalType = OchSignalType.FLEX_GRID;
                break;
            case DWDM:
                signalType = OchSignalType.FIXED_GRID;
                break;
            default:
                throw new IllegalArgumentException("Unsupported grid type: " + signal.gridType());
        }

        TrafficSelector selector = DefaultTrafficSelector.builder()
                .add(Criteria.matchLambda(signal))
                .add(Criteria.matchOchSignalType(signalType))
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .add(Instructions.modL0Lambda(signal))
                .build();

        intent = SinglePointToMultiPointIntent.builder()
                .appId(appId)
                .key(key)
                .filteredIngressPoint(filteredIngress)
                .filteredEgressPoints(filteredEgressSet)
                .selector(selector)
                //.treatment(treatment)
                .build();

        return intent;
    }

    //TODO: to be checked and tested, REST API to be implemented
    public static Intent createMp2pOpticalIntent(Set<ConnectPoint> ingressSet, ConnectPoint egress,
                                                 DeviceService deviceService, Key key, ApplicationId appId,
                                                 OchSignal signal) {
        Intent intent = null;

        if (ingressSet == null || ingressSet.isEmpty()) {
            throw new IllegalArgumentException("Ingress connect point set cannot be null or empty");
        }
        if (egress == null) {
            throw new IllegalArgumentException("Egress connect point cannot be null");
        }
        if (deviceService == null) {
            throw new IllegalArgumentException("DeviceService cannot be null");
        }
        if (signal == null) {
            throw new IllegalArgumentException("OchSignal cannot be null");
        }

        if (!connectPointExists(egress, deviceService)) {
            throw new IllegalArgumentException("Egress connect point does not exist: " + egress);
        }

        for (ConnectPoint ingress : ingressSet) {
            if (ingress == null) {
                throw new IllegalArgumentException("Ingress connect point cannot be null");
            }
            if (!connectPointExists(ingress, deviceService)) {
                throw new IllegalArgumentException("Ingress connect point does not exist: " + ingress);
            }
        }

        if (ingressSet.contains(egress)) {
            throw new IllegalArgumentException("Ingress set must not contain egress");
        }

        log.info("Creating M2SP intent - ingressSet: {}, egress: {}", ingressSet, egress);

        Set<FilteredConnectPoint> filteredIngressSet = ingressSet.stream()
                .map(FilteredConnectPoint::new)
                .collect(Collectors.toSet());

        FilteredConnectPoint filteredEgress = new FilteredConnectPoint(egress);

        OchSignalType signalType;
        switch (signal.gridType()) {
            case FLEX:
                signalType = OchSignalType.FLEX_GRID;
                break;
            case DWDM:
                signalType = OchSignalType.FIXED_GRID;
                break;
            default:
                throw new IllegalArgumentException("Unsupported grid type: " + signal.gridType());
        }

        TrafficSelector selector = DefaultTrafficSelector.builder()
                .add(Criteria.matchLambda(signal))
                .add(Criteria.matchOchSignalType(signalType))
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .add(Instructions.modL0Lambda(signal))
                .build();

        intent = MultiPointToSinglePointIntent.builder()
                .appId(appId)
                .key(key)
                .filteredIngressPoints(filteredIngressSet)
                .filteredEgressPoint(filteredEgress)
                .selector(selector)
                //.treatment(treatment)
                .build();

        return intent;
    }

    private static boolean connectPointExists(ConnectPoint cp, DeviceService deviceService) {
        return deviceService.getDevice(cp.deviceId()) != null &&
                deviceService.getPort(cp.deviceId(), cp.port()) != null;
    }
}
