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

package org.onosproject.net;

import org.onlab.util.Frequency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OpticalBandUtils {

    private static final Logger log = LoggerFactory.getLogger(OpticalBandUtils.class);
    private static final Frequency L_BAND_START_FREQ = Frequency.ofGHz(184500);
    private static final Frequency L_BAND_STOP_FREQ = Frequency.ofGHz(191500);
    private static final Frequency C_BAND_START_FREQ = Frequency.ofGHz(191500);
    private static final Frequency C_BAND_STOP_FREQ = Frequency.ofGHz(195500);
    private static final Frequency S_BAND_START_FREQ = Frequency.ofGHz(195500);
    private static final Frequency S_BAND_STOP_FREQ = Frequency.ofGHz(205300);

    // prohibit instantiation
    private OpticalBandUtils() {}

    public static OpticalBandType computeOpticalBand(OchSignal ochSignal) {
        if (L_BAND_START_FREQ.isLessThan(ochSignal.centralFrequency())
                && L_BAND_STOP_FREQ.isGreaterThan(ochSignal.centralFrequency())) {
            return OpticalBandType.L_BAND;
        }
        if (C_BAND_START_FREQ.isLessThan(ochSignal.centralFrequency())
                && C_BAND_STOP_FREQ.isGreaterThan(ochSignal.centralFrequency())) {
            return OpticalBandType.C_BAND;
        }
        if (S_BAND_START_FREQ.isLessThan(ochSignal.centralFrequency())
                && S_BAND_STOP_FREQ.isGreaterThan(ochSignal.centralFrequency())) {
            return OpticalBandType.S_BAND;
        }
        return null;
    }

    public static Frequency startFrequency(OpticalBandType bandType) {
        switch (bandType) {
            case L_BAND:
                return L_BAND_START_FREQ;
            case C_BAND:
                return C_BAND_START_FREQ;
            case S_BAND:
                return S_BAND_START_FREQ;
            default:
                log.error("Unsupported OpticalBandType {}", bandType);
                return null;
        }
    }

    public static Frequency stopFrequency(OpticalBandType bandType) {
        switch (bandType) {
            case L_BAND:
                return L_BAND_STOP_FREQ;
            case C_BAND:
                return C_BAND_STOP_FREQ;
            case S_BAND:
                return S_BAND_STOP_FREQ;
            default:
                log.error("Unsupported OpticalBandType {}", bandType);
                return null;
        }
    }
}
