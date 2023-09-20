/*
 * Copyright 2023-present Open Networking Foundation
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
 *
 * This Work is contributed by CNR within the B5G-OPEN project.
 */

package org.onosproject.drivers.odtn.openconfig;

import org.onosproject.driver.optical.query.MultiBandLambdaQuery;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.OchSignal;
import org.onosproject.net.PortNumber;
import org.slf4j.Logger;

import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Example lambda query for multi-band optical devices.
 */
public class MultiBand10LambdaQuery extends MultiBandLambdaQuery {

    protected static final Logger log = getLogger(TerminalDeviceLambdaQuery.class);

    @Override
    public Set<OchSignal> queryLambdas(PortNumber port) {
        log.debug("OPENCONFIG: queried lambdas for port {}", port);

        channelSpacing = ChannelSpacing.CHL_50GHZ;
        slotGranularity = 4;

        //Generates 10 channels on each band, each channel is 50 GHz width
        lBandLambdaCount = 10;
        cBandLambdaCount = 10;
        sBandLambdaCount = 10;

        return super.queryLambdas(port);
    }
}

