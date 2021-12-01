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

import com.google.common.collect.ImmutableList;
import org.onosproject.drivers.odtn.impl.DeviceConnectionCache;
import org.onosproject.drivers.odtn.impl.FlowRuleParser;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleProgrammable;
import org.onosproject.netconf.DatastoreId;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.odtn.behaviour.OdtnDeviceDescriptionDiscovery.OC_OPTICAL_CHANNEL_NAME;

/**
 * Implementation of FlowRuleProgrammable interface for Cassini device running Ocnos v5.
 */
public class CassiniOcnos5FlowRuleProgrammable
        extends AbstractHandlerBehaviour implements FlowRuleProgrammable {

    private static final Logger log =
            LoggerFactory.getLogger(CassiniOcnos5FlowRuleProgrammable.class);

    private static final String RPC_TAG_NETCONF_BASE =
            "<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">";

    private static final String RPC_CLOSE_TAG = "</rpc>";


    /**
     * Apply the flow entries specified in the collection rules.
     *
     * @param rules A collection of Flow Rules to be applied
     * @return The collection of added Flow Entries
     */
    @Override
    public Collection<FlowRule> applyFlowRules(Collection<FlowRule> rules) {
        NetconfSession session = getNetconfSession();
        if (session == null) {
            ocnosError("null session");
            return ImmutableList.of();
        }
        List<FlowRule> added = new ArrayList<>();
        for (FlowRule r : rules) {
            try {
                String connectionId = applyFlowRule(session, r);
                getConnectionCache().add(did(), connectionId, r);
                added.add(r);
            } catch (Exception e) {
                ocnosError("Error {}", e);
                continue;
            }
        }
        ocnosLog("applyFlowRules added {}", added.size());
        return added;
    }

    /**
     * Get the flow entries that are present on the device.
     *
     * @return A collection of Flow Entries
     */
    @Override
    public Collection<FlowEntry> getFlowEntries() {
        DeviceConnectionCache cache = getConnectionCache();
        if (cache.get(did()) == null) {
            return ImmutableList.of();
        }

        List<FlowEntry> entries = new ArrayList<>();
        for (FlowRule r : cache.get(did())) {
            entries.add(
                    new DefaultFlowEntry(r, FlowEntry.FlowEntryState.ADDED, 0, 0, 0));
        }
        return entries;
    }

    /**
     * Remove the specified flow rules.
     *
     * @param rules A collection of Flow Rules to be removed
     * @return The collection of removed Flow Entries
     */
    @Override
    public Collection<FlowRule> removeFlowRules(Collection<FlowRule> rules) {
        NetconfSession session = getNetconfSession();
        if (session == null) {
            ocnosError("null session");
            return ImmutableList.of();
        }
        List<FlowRule> removed = new ArrayList<>();
        for (FlowRule r : rules) {
            try {
                String connectionId = removeFlowRule(session, r);
                getConnectionCache().remove(did(), connectionId);
                removed.add(r);
            } catch (Exception e) {
                ocnosError("Error {}", e);
                continue;
            }
        }
        ocnosLog("removedFlowRules removed {}", removed.size());
        return removed;
    }

    private DeviceConnectionCache getConnectionCache() {
        return DeviceConnectionCache.init();
    }

    /**
     * Helper method to get the device id.
     */
    private DeviceId did() {
        return data().deviceId();
    }

    /**
     * Helper method to log from this class adding DeviceId.
     */
    private void ocnosLog(String format, Object... arguments) {
        log.info("OCNOS5 {}: " + format, did(), arguments);
    }

    /**
     * Helper method to log an error from this class adding DeviceId.
     */
    private void ocnosError(String format, Object... arguments) {
        log.error("OCNOS5 {}: " + format, did(), arguments);
    }

    /**
     * Helper method to get the Netconf Session.
     */
    private NetconfSession getNetconfSession() {
        NetconfController controller =
                checkNotNull(handler().get(NetconfController.class));
        return controller.getNetconfDevice(did()).getSession();
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

    public void setOpticalChannelFrequency(NetconfSession session, FlowRuleParser r)
            throws NetconfException {
        StringBuilder sbSet = new StringBuilder();

        String slot = getOpticalChannel(r.getPortNumber());

        sbSet.append("<terminal-device xmlns='http://www.ipinfusion.com/yang/ocnos/ipi-platform-terminal-device'>"
                + "<coherent-module>"
                + "  <slot-index>" + slot + "</slot-index>"
                + "  <config>"
                + "    <slot-index>" + slot + "</slot-index>"
                + "  </config>"
                + "<network-interfaces>"
                + "<interface>"
                + "  <net-index>0</net-index>"
                + "  <config>"
                + "    <net-index>0</net-index>"
                + "    <frequency>" + r.getCentralFrequency().asHz() + "Hz</frequency>"
                + "  </config>"
                + "</interface>"
                + "</network-interfaces>"
                + "</coherent-module>"
                + "</terminal-device>");

        log.info("Configure optical channel {}", sbSet);

        boolean set = session.editConfig(DatastoreId.CANDIDATE, null, sbSet.toString());
        if (!set) {
            throw new NetconfException("error editing channel frequency");
        }

        log.info("Commit configure optical channel");
        boolean commit2 = session.commit();
        if (!commit2) {
            throw new NetconfException("error committing channel frequency");
        }
    };

    public void deleteOpticalChannelFrequency(NetconfSession session, FlowRuleParser r)
            throws NetconfException {
        StringBuilder sb = new StringBuilder();

        String slot = getOpticalChannel(r.getPortNumber());

        sb.append("<terminal-device xmlns='http://www.ipinfusion.com/yang/ocnos/ipi-platform-terminal-device'>"
                + "<coherent-module>"
                + "  <slot-index>" + slot + "</slot-index>"
                + "  <config>"
                + "    <slot-index>" + slot + "</slot-index>"
                + "  </config>"
                + "<network-interfaces>"
                + "<interface>"
                + "  <net-index>0</net-index>"
                + "  <config>"
                + "    <net-index>0</net-index>"
                + "    <frequency>" + r.getCentralFrequency().asHz() + "Hz</frequency>"
                + "  </config>"
                + "</interface>"
                + "</network-interfaces>"
                + "</coherent-module>"
                + "</terminal-device>");

        log.info("Disable service and delete optical channel {}", sb);
        boolean edit = session.editConfig(DatastoreId.CANDIDATE, null, sb.toString());
        if (!edit) {
            throw new NetconfException("error editing channel frequency");
        }

        log.info("Commit optical channel");
        boolean commit = session.commit();
        if (!commit) {
            throw new NetconfException("error committing channel frequency");
        }
    };


    /**
     * Apply the flowrule.
     *
     * @param session The Netconf session.
     * @param r       Flow Rules to be applied.
     * @return the optical channel + the frequency or just channel as identifier fo the config installed on the device
     * @throws NetconfException if exchange goes wrong
     */
    protected String applyFlowRule(NetconfSession session, FlowRule r)
            throws NetconfException {
        FlowRuleParser frp = new FlowRuleParser(r);

        setOpticalChannelFrequency(session, frp);
        return frp.getPortNumber() + ":" + frp.getCentralFrequency().asGHz();
    }


    protected String removeFlowRule(NetconfSession session, FlowRule r)
            throws NetconfException {
        FlowRuleParser frp = new FlowRuleParser(r);

        deleteOpticalChannelFrequency(session, frp);
        return frp.getPortNumber() + ":" + frp.getCentralFrequency().asGHz();
    }
}

