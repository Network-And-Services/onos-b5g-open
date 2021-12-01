/*
 * Copyright 2022-present Open Networking Foundation
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
 * This work was partially supported by EC H2020 project B5G-OPEN (101016663).
 */

package org.onosproject.drivers.odtn;

import org.apache.commons.configuration.XMLConfiguration;
import org.onosproject.drivers.odtn.util.NetconfSessionUtility;
import org.onosproject.drivers.utilities.XmlConfigParser;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.BitErrorRateState;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.odtn.behaviour.OdtnDeviceDescriptionDiscovery.OC_OPTICAL_CHANNEL_NAME;

/**
 * Implementation of BitErrorRateState interface for Cassini device running Ocnos v5.
 */
public class CassiniOcnos5BitErrorRate
        extends AbstractHandlerBehaviour implements BitErrorRateState {

    private static final Logger log = LoggerFactory.getLogger(CassiniOcnos5BitErrorRate.class);

    private static final String RPC_TAG_NETCONF_BASE =
            "<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">";

    private static final String RPC_CLOSE_TAG = "</rpc>";
    private static final String PRE_FEC_BER_TAG = "current-pre-fec-ber";
    private static final String POST_FEC_BER_TAG = "current-post-fec-ber";
    private static final String PRE_FEC_BER_FILTER =
            "data.terminal-device.coherent-module.network-interfaces.interface.ber.state." + PRE_FEC_BER_TAG;
    private static final String POST_FEC_BER_FILTER =
            "data..terminal-device.coherent-module.network-interfaces.interface.ber.state." + POST_FEC_BER_TAG;

    /*
     * This method returns the instance of NetconfController from DriverHandler.
     */
    private NetconfController getController() {
        return handler().get(NetconfController.class);
    }

    /**
     * Get the BER value pre FEC.
     *
     * @param deviceId the device identifier
     * @param port     the port identifier
     * @return the decimal value of BER
     */
    @Override
    public Optional<Double> getPreFecBer(DeviceId deviceId, PortNumber port) {
        NetconfSession session = NetconfSessionUtility
                .getNetconfSession(deviceId, getController());
        checkNotNull(session);

        String slotIndex = getOpticalChannel(port);

        if (slotIndex != null) {

            String reply;
            try {
                reply = session.get(getBerFilter(slotIndex, PRE_FEC_BER_TAG));
            } catch (Exception e) {
                throw new IllegalStateException(new NetconfException("Failed to retrieve getPreFecBer info.", e));
            }

            log.debug("REPLY from device: {}", reply);

            XMLConfiguration xconf = (XMLConfiguration) XmlConfigParser.loadXmlString(reply);
            if (xconf == null) {
                log.error("Error in executing RPC");
                return Optional.empty();
            }

            String powerString = xconf.getString((PRE_FEC_BER_FILTER));

            log.debug("currentPreFecBer from device: {}", powerString);

            if (powerString == null) {
                return Optional.empty();
            }

            Double rational = 1e18;
            return Optional.of(Double.valueOf(powerString) / rational);
        }

        return Optional.empty();
    }

    /**
     * Get the OpenConfig component name for the OpticalChannel component.
     *
     * @param portNumber ONOS port number of the Line port ().
     * @return the channel component name or null
     */
    protected String getOpticalChannel(PortNumber portNumber) {
        Port clientPort = handler().get(DeviceService.class).getPort(did(), portNumber);
        return clientPort.annotations().value(OC_OPTICAL_CHANNEL_NAME);
    }

    /**
     * Get the deviceId for which the methods apply.
     *
     * @return The deviceId as contained in the handler data
     */
    private DeviceId did() {
        return handler().data().deviceId();
    }

    /**
     * Get the BER value post FEC.
     *
     * @param deviceId the device identifier
     * @param port     the port identifier
     * @return the decimal value of BER
     */
    @Override
    public Optional<Double> getPostFecBer(DeviceId deviceId, PortNumber port) {
        NetconfSession session = NetconfSessionUtility
                .getNetconfSession(deviceId, getController());
        checkNotNull(session);

        String slotIndex = getOpticalChannel(port);

        if (slotIndex != null) {

            String reply;
            try {
                reply = session.get(getBerFilter(slotIndex, POST_FEC_BER_TAG));
            } catch (Exception e) {
                throw new IllegalStateException(new NetconfException("Failed to retrieve getPostFecBer info.", e));
            }

            log.debug("REPLY from device: {}", reply);

            XMLConfiguration xconf = (XMLConfiguration) XmlConfigParser.loadXmlString(reply);
            if (xconf == null) {
                log.error("Error in executing RPC");
                return Optional.empty();
            }

            String powerString = xconf.getString(POST_FEC_BER_FILTER);

            log.debug("currentPostFecBer from device: {}", powerString);

            if (powerString == null) {
                return Optional.empty();
            }

            Double rational = 1e18;
            return Optional.of(Double.valueOf(powerString) / rational);
        }

        return Optional.empty();
    }

    private String getBerFilter(String slotNumber, String filterBer) {
        StringBuilder filter = new StringBuilder();

        filter.append("<terminal-device xmlns='http://www.ipinfusion.com/yang/ocnos/ipi-platform-terminal-device'>"
                + "<coherent-module>"
                + "  <slot-index>" + slotNumber + "</slot-index>"
                + "<network-interfaces>"
                + "<interface>"
                + "  <net-index>0</net-index>"
                + "  <ber>"
                + "    <state>"
                + "      <" + filterBer + "/>"
                + "    </state>"
                + "  </ber>"
                + "</interface>"
                + "</network-interfaces>"
                + "</coherent-module>"
                + "</terminal-device>");

        return filteredGetBuilder(filter.toString());
    }

    private String filteredGetBuilder(String filter) {
        StringBuilder rpc = new StringBuilder(RPC_TAG_NETCONF_BASE);
        rpc.append("<get>");
        rpc.append("<filter type='subtree'>");
        rpc.append(filter);
        rpc.append("</filter>");
        rpc.append("</get>");
        rpc.append(RPC_CLOSE_TAG);
        return rpc.toString();
    }
}
