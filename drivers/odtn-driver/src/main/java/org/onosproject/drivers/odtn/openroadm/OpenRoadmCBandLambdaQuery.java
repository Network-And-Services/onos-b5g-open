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

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import java.util.stream.IntStream;

import org.onlab.util.Frequency;
import org.onlab.util.Spectrum;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.GridType;
import org.onosproject.net.OchSignal;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.LambdaQuery;
import org.onosproject.net.driver.AbstractHandlerBehaviour;

/**
 * Abstract C-band DWDM plan lambda query.
 *
 */
/**
 * Abstract C-band DWDM plan lambda query.
 *
 */
public abstract class OpenRoadmCBandLambdaQuery
        extends AbstractHandlerBehaviour implements LambdaQuery {
    protected ChannelSpacing channelSpacing;
    protected int lambdaCount;
    protected int slotGranularity;
    protected GridType gridType;
    /*
     * The following 2 values are not specified by the OpenROADM standard,
     * but they are a reasonable default for a tunable C-band, defined from
     * Channel C1 at 191.35 to C96 at 196.10 GHz (for a spacing at 50GHz)
     */
    public static final Frequency C_BAND_FIRST_CENTER_FREQ = Frequency.ofGHz(191_350);
    public static final Frequency C_BAND_LAST_CENTER_FREQ = Frequency.ofGHz(196_100);
    @Override
    public Set<OchSignal> queryLambdas(PortNumber port) {
        // 193.1 THz corresponds to C36 instead of C48 (halfway between 1 and 96)

        //Questo codice non funziona, ritorna null
        long minFreq = C_BAND_FIRST_CENTER_FREQ.asHz();
        long centerFreq = Spectrum.CENTER_FREQUENCY.asHz();
        int offset = (int) ((centerFreq - minFreq) / (channelSpacing.frequency().asHz()));
        return IntStream.range(0, lambdaCount)
                .mapToObj(x -> new OchSignal(gridType, channelSpacing, x - offset, slotGranularity))
                .collect(ImmutableSet.toImmutableSet());
    }
}
