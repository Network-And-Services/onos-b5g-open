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
import org.onosproject.drivers.utilities.XmlConfigParser;
import org.onosproject.net.DeviceId;
import org.onosproject.net.ModulationScheme;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.ModulationConfig;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.netconf.DatastoreId;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfDevice;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.odtn.behaviour.OdtnDeviceDescriptionDiscovery.OC_OPTICAL_CHANNEL_NAME;

/**
 * Implementation of ModulationConfig interface for Cassini device running Ocnos v5.
 */
public class CassiniOcnos5Modulation<T> extends AbstractHandlerBehaviour
        implements ModulationConfig<T> {

    private static final String RPC_TAG_NETCONF_BASE =
            "<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">";

    private static final String RPC_CLOSE_TAG = "</rpc>";

    private static final Logger log = LoggerFactory.getLogger(CassiniOcnos5Modulation.class);

    /**
     * Returns the NetconfSession with the device for which the method was called.
     *
     * @param deviceId device indetifier
     * @return The netconf session or null
     */
    private NetconfSession getNetconfSession(DeviceId deviceId) {
        NetconfController controller = handler().get(NetconfController.class);
        NetconfDevice ncdev = controller.getDevicesMap().get(deviceId);
        if (ncdev == null) {
            log.trace("No netconf device, returning null session");
            return null;
        }
        return ncdev.getSession();
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

    /*
     *
     * Get the deviceId for which the methods apply.
     *
     * @return The deviceId as contained in the handler data
     */
    private DeviceId did() {
        return handler().data().deviceId();
    }

    private String getOpModeFilter(String slotName) {
        StringBuilder sb = new StringBuilder();

        sb.append("<terminal-device xmlns='http://www.ipinfusion.com/yang/ocnos/ipi-platform-terminal-device'>"
                + "<coherent-module>"
                + "  <slot-index>" + slotName + "</slot-index>"
                + "<network-interfaces>"
                + "<interface>"
                + "  <net-index>0</net-index>"
                + "  <state>"
                + "    <modulation-format/>"
                + "  </state>"
                + "</interface>"
                + "</network-interfaces>"
                + "</coherent-module>"
                + "</terminal-device>");

        return sb.toString();
    }

    /*Parse filtering string from port and component.
     *
     * @param portNumber Port Number
     * @param modulation
     * @return filtering string in xml format
     */
    private String setOpModeFilter(PortNumber portNumber, ModulationScheme modulation) {

        String operationalMode = getOperationalMode(modulation);
        String slotName = getOpticalChannel(portNumber);

        StringBuilder sb = new StringBuilder();

        sb.append("<terminal-device xmlns='http://www.ipinfusion.com/yang/ocnos/ipi-platform-terminal-device'>"
                + "<coherent-module>"
                + "  <slot-index>" + slotName + "</slot-index>"
                + "  <config>"
                + "    <slot-index>" + slotName + "</slot-index>"
                + "  </config>"
                + "<network-interfaces>"
                + "<interface>"
                + "  <net-index>0</net-index>"
                + "  <config>"
                + "    <net-index>0</net-index>"
                + "    <modulation-format>" + operationalMode + "</modulation-format>"
                + "  </config>"
                + "</interface>"
                + "</network-interfaces>"
                + "</coherent-module>"
                + "</terminal-device>");

        return sb.toString();
    }

    /**
     * Get the target Modulation Scheme on the component.
     *
     * @param port      the port
     * @param component the port component
     * @return ModulationScheme as per bitRate value
     **/
    @Override
    public Optional<ModulationScheme> getModulationScheme(PortNumber port, T component) {
        if (checkPortComponent(port, component)) {
            return getOcnosModulationScheme(port, component);
        }
        return Optional.empty();
    }

    /**
     * Set the target Modulation Scheme on the component.
     *
     * @param port      the port
     * @param component the port component
     * @param bitRate   bit rate in bps
     **/
    @Override
    public void setModulationScheme(PortNumber port, T component, long bitRate) {
        if (checkPortComponent(port, component)) {
            setOcnosModulationScheme(port, component, bitRate);
        }
    }

    /**
     * Set the target Modulation Scheme on the component.
     *
     * @param port      the port
     * @param component the port component
     * @param modulationScheme   selecetd modulation
     **/
    @Override
    public void setModulationScheme(PortNumber port, T component, ModulationScheme modulationScheme) {
        if (checkPortComponent(port, component)) {
            setOcnosModulationScheme(port, component, modulationScheme);
        }
    }

    private String filteredEditConfigBuilder(String filterEditConfig) {
        StringBuilder rpc = new StringBuilder();
        rpc.append(RPC_TAG_NETCONF_BASE);
        rpc.append("<edit-config>");
        rpc.append("<target><" + DatastoreId.CANDIDATE + "/></target>");
        rpc.append("<config>");
        rpc.append(filterEditConfig);
        rpc.append("</config>");
        rpc.append("</edit-config>");
        rpc.append(RPC_CLOSE_TAG);

        return rpc.toString();
    }

    private String filteredGetBuilder(String filter) {
        StringBuilder rpc = new StringBuilder();
        rpc.append(RPC_TAG_NETCONF_BASE);
        rpc.append("<get>");
        rpc.append("<filter type='subtree'>");
        rpc.append(filter);
        rpc.append("</filter>");
        rpc.append("</get>");
        rpc.append(RPC_CLOSE_TAG);
        return rpc.toString();
    }

    /**
     * Set the ComponentType to invoke proper methods for different template T.
     *
     * @param port the component.
     * @param component the component.
     */
    private Boolean checkPortComponent(PortNumber port, Object component) {

        //Check componenet
        String clsName = component.getClass().getName();
        switch (clsName) {
            case "org.onosproject.net.Direction":
                break;
            case "org.onosproject.net.OchSignal":
                break;
            default:
                log.error("Cannot parse the component type {}.", clsName);
                log.error("The component content is {}.", component.toString());
                return false;
        }

        //Check that port has an associated optical channel
        if (getOpticalChannel(port) == null) {
            return false;
        }

        //Checks are ok
        return true;
    }

    /*
     * Set modulation scheme.
     *
     * @param port port
     * @param component component
     * @param power target value
     */
    void setOcnosModulationScheme(PortNumber port, Object component, ModulationScheme modulationScheme) {
        NetconfSession session = getNetconfSession(did());
        checkNotNull(session);

        //log.info("Setting modulation scheme {}", modulationScheme);

        String filter = setOpModeFilter(port, modulationScheme);
        String rpcReq = filteredEditConfigBuilder(filter);

        try {
            session.rpc(rpcReq);
        } catch (Exception e) {
            log.error("Error writing operational mode on CANDIDATE", e);
        }

        //log.info("Modulation config sent {}", rpcReq);

        try {
            session.commit();
        } catch (NetconfException e) {
            log.error("Error committing operational mode", e);
        }
    }

    /*
     * Get modulation scheme.
     *
     * @param port port
     * @param component component
     * @return target modulation
     */
    Optional<ModulationScheme> getOcnosModulationScheme(PortNumber port, Object component) {
        NetconfSession session = getNetconfSession(did());
        checkNotNull(session);

        String filter = getOpModeFilter(getOpticalChannel(port));

        String reply;
        try {
            reply = session.get(filteredGetBuilder(filter));
        } catch (Exception e) {
            throw new IllegalStateException(new NetconfException("Failed to retrieve opMode.", e));
        }

        //log.info("REPLY from device: {}", reply);

        XMLConfiguration xconf = (XMLConfiguration) XmlConfigParser.loadXmlString(reply);
        if (xconf == null) {
            log.error("Error in executing get");
            return Optional.empty();
        }

        String opModeString = xconf.getString(("data." +
                "terminal-device.coherent-module.network-interfaces.interface." +
                "state.modulation-format"));

        //log.info("Modulation format mode from device: {}", opModeString);

        if (opModeString == null) {
            return Optional.empty();
        }

        ModulationScheme modulation;
        if (opModeString.equals("dp-8-qam")) {
            modulation = ModulationScheme.DP_8QAM;
        } else if (opModeString.equals("dp-16-qam")) {
            modulation = ModulationScheme.DP_16QAM;
        } else if (opModeString.equals("dp-qpsk")) {
            modulation = ModulationScheme.DP_QPSK;
        } else {
            log.error("Current operational mode not supported by the driver");
            return Optional.empty();
        }
        return Optional.of(modulation);
    }

    /*
     * Set modulation scheme using bitrate.
     *
     * @param port port
     * @param component component
     * @param power target value
     */
    void setOcnosModulationScheme(PortNumber port, Object component, long bitRate) {
        NetconfSession session = getNetconfSession(did());
        checkNotNull(session);

        ModulationScheme modulationScheme;
        if (bitRate == 200) {
            modulationScheme = ModulationScheme.DP_8QAM;
        } else { // check if bitrate is greater than 100 Gig
            modulationScheme = ModulationScheme.DP_QPSK;
        }

        String filter = setOpModeFilter(port, modulationScheme);
        String rpcReq = filteredEditConfigBuilder(filter);

        try {
            session.rpc(rpcReq);
        } catch (Exception e) {
            log.error("Error writing operational mode on CANDIDATE", e);
        }

        log.info("Modulation config sent {}", rpcReq);

        try {
            session.commit();
        } catch (NetconfException e) {
            log.error("Error committing channel power", e);
        }
    }

    private String getOperationalMode(ModulationScheme modulation) {
        if (modulation.equals(ModulationScheme.DP_QPSK)) {
            return "dp-qpsk";
        }
        if (modulation.equals(ModulationScheme.DP_16QAM)) {
            return "dp-16-qam";
        }
        if (modulation.equals(ModulationScheme.DP_8QAM)) {
            return "dp-8-qam";
        }
        log.error("Modulation scheme is not supported.");
        return null;
    }
}