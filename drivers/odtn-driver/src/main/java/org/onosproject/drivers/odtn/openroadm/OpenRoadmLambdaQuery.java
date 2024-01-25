/*
 * Copyright 2018-present Open Networking Foundation
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

 * This work was partially supported by EC H2020 project METRO-HAUL (761727).
 */
package org.onosproject.drivers.odtn.openroadm;

import java.util.Set;

import org.onosproject.net.*;

import org.onosproject.net.device.DeviceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lambda Query for OpenROADM ports (see Optical model).
 *
 * Parameters for LambdaQuery are obtained from the device datastore during
 * Discovery and stored into annotations. If missing (while being mandatory)
 * 50 GHz fixed grid case is modeled.
 *
 * Note that ONOS core doesn't support 3.125 GHz center freq granularity and
 * 6.25 GHz slot width granularity (and, hence, 768 possible slots). In this
 * case the method announces 6.25 GHz, 12.5 GHz and 384 lambdas respectively.
 *
 */
public class OpenRoadmLambdaQuery extends OpenRoadmCBandLambdaQuery {
    private final Logger log = LoggerFactory.getLogger(getClass());
    @Override
    public Set<OchSignal> queryLambdas(PortNumber port) {
        double centerFreqGranularity = 0;
        double slotWidthGranularity = 0;
        DeviceService deviceService = this.handler().get(DeviceService.class);
        Port p = deviceService.getPort(handler().data().deviceId(), port);
        String maxWavelengths = p.annotations().value("openroadm-port-max-wavelengths");
        String cfg = p.annotations().value("openroadm-port-center-freq-granularity");
        String swg = p.annotations().value("openroadm-port-slot-width-granularity");
        String minsl = p.annotations().value("openroadm-port-min-slots");
        String maxsl = p.annotations().value("openroadm-port-max-slots");
        /* From OpenROADM device whitepaper, node type is DWDM when
           min-slots = 1 and max-slots = 1 */
        try {
            int minSlots = Integer.parseInt(minsl);
            int maxSlots = Integer.parseInt(maxsl);
            if (minSlots == 1 && maxSlots == 1) {
                gridType = GridType.DWDM;
            } else {
                gridType = GridType.FLEX;
            }
        } catch (NumberFormatException e) {
            gridType = GridType.DWDM;     // default if missing
        }
        try {
            centerFreqGranularity = Double.parseDouble(cfg);
            if (centerFreqGranularity == 100.0) {
                channelSpacing = ChannelSpacing.CHL_100GHZ;
            } else if (centerFreqGranularity == 50.0) {
                channelSpacing = ChannelSpacing.CHL_50GHZ;
            } else if (centerFreqGranularity == 25.0) {
                channelSpacing = ChannelSpacing.CHL_25GHZ;
            } else if (centerFreqGranularity == 12.5) {
                channelSpacing = ChannelSpacing.CHL_12P5GHZ;
            } else if (centerFreqGranularity == 6.25) {
                channelSpacing = ChannelSpacing.CHL_6P25GHZ;
            } else if (centerFreqGranularity < 6.25) {
                channelSpacing = ChannelSpacing.CHL_6P25GHZ;
                log.info("[OPENROADM], {}, {} unsupported channelSpacing: falling back to 6.25 GHz",
                        handler().data().deviceId(), p);
            }
        } catch (NumberFormatException e) {
            channelSpacing = ChannelSpacing.CHL_50GHZ;
        }
        try {
            slotWidthGranularity = Double.parseDouble(swg);
            if (slotWidthGranularity < 12.5) {
                slotGranularity = 1;
                log.info("[OPENROADM], {}, {} unsupported slotWidthGranularity: falling back to 12.5 GHz",
                        handler().data().deviceId(), p);
            } else {
                slotGranularity = (int) (slotWidthGranularity / 12.5);
            }
        } catch (NumberFormatException e) {
            slotGranularity = 4;
        }

        // The OpenROADM 2.2 model have no max-wavelength leaf in SRG branch.
        // That leaf is mandatory for degrees, so if it's missing we are
        // dealing with an SRG port. We try to infer the number of wavelength
        // for SRG ports from slot-width-granularity
        try {
            lambdaCount = Integer.parseInt(maxWavelengths);
        } catch (NumberFormatException e) {
            if (slotWidthGranularity == 100) {
                lambdaCount = 48;
            } else if (slotWidthGranularity == 50) {
                lambdaCount = 96;
            } else if (slotWidthGranularity == 25) {
                lambdaCount = 192;
            } else if (slotWidthGranularity <= 12.5) {
                lambdaCount = 384;
            } else {
                lambdaCount = 96;       // 50 GHz Fixed grid case
            }
        }
        Set<OchSignal> set = super.queryLambdas(port);
        return set;
    }
}
