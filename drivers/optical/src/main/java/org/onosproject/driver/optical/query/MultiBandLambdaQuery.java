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

package org.onosproject.driver.optical.query;

import org.onlab.util.Frequency;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.GridType;
import org.onosproject.net.OchSignal;
import org.onosproject.net.OpticalBandType;
import org.onosproject.net.OpticalBandUtils;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.LambdaQuery;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.optical.util.OpticalChannelUtility;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;

import static org.onosproject.net.OpticalBandType.L_BAND;
import static org.onosproject.net.OpticalBandType.C_BAND;
import static org.onosproject.net.OpticalBandType.S_BAND;
import static org.onosproject.net.OpticalBandType.O_BAND;
import static org.slf4j.LoggerFactory.getLogger;


/**
 * Abstract multi-band WDM plan lambda query, this is a base class NOT meant to be used as driver.
 *
 * It generates a fixed number of channel per band.
 *
 */
public class MultiBandLambdaQuery extends AbstractHandlerBehaviour implements LambdaQuery {
    protected static final Logger log = getLogger(MultiBandLambdaQuery.class);

    protected ChannelSpacing channelSpacing;
    protected int slotGranularity;

    protected int lBandLambdaCount;
    protected int cBandLambdaCount;
    protected int sBandLambdaCount;
    protected int oBandLambdaCount;

    @Override
    public Set<OchSignal> queryLambdas(PortNumber port) {

        Set<OchSignal> lambdas = new HashSet<>();

        log.debug("Device {} - Quering L_Band", data().deviceId());
        addChannels(lambdas, L_BAND, lBandLambdaCount);

        log.debug("Device {} - Quering C_Band", data().deviceId());
        addChannels(lambdas, C_BAND, cBandLambdaCount);

        log.debug("Device {} - Quering S_Band", data().deviceId());
        addChannels(lambdas, S_BAND, sBandLambdaCount);

        log.debug("Device {} - Quering O_Band", data().deviceId());
        addChannels(lambdas, O_BAND, oBandLambdaCount);

        return lambdas;
    }

    /**
     * Adds a number lambdaCount of new channels in band bandType to the set lambdas.
     *
     * @param lambdas
     * @param bandType
     * @param lambdaCount
     */
    private void addChannels(Set<OchSignal> lambdas, OpticalBandType bandType, int lambdaCount) {

        Frequency startFreq = OpticalBandUtils.startFrequency(bandType);

        for (int i = 0; i < lambdaCount; i++) {
            OchSignal ochSignal = OpticalChannelUtility.createOchSignalFromBounds(
                    startFreq.add(channelSpacing.frequency().multiply(i)),
                    startFreq.add(channelSpacing.frequency().multiply(i + 1)),
                    GridType.DWDM,
                    channelSpacing);

            log.debug("Created OchSignal {}", ochSignal);

            lambdas.add(ochSignal);
        }
    }
}
