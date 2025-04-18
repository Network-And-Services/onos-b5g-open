package org.onosproject.net.optical.intents;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.OchSignal;
import org.onosproject.net.OduSignalType;
import org.onosproject.net.ResourceGroup;
import org.onosproject.net.Path;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.OpticalConnectivityIntent;
import org.onosproject.net.OcOperationalMode;
import java.util.Collections;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An optical layer intent for connectivity between two OCh ports.
 * No traffic selector or traffic treatment are needed.
 * OchSignal and suggestedPath are optional.
 */
@Beta
public final class OpticalConnectIntent extends Intent {
    private final ConnectPoint src;
    private final ConnectPoint dst;
    private final OduSignalType signalType;
    private final boolean isBidirectional;
    private final Optional<OchSignal> ochSignal;
    private final Optional<Path> suggestedPath;

    public Optional<OcOperationalMode> operationalMode = Optional.empty();
    public Optional<Double> targetOutputPower = Optional.empty();

    /**
     * Creates an optical connectivity intent between the specified
     * connection points.
     *
     * @param appId application identification
     * @param key intent key
     * @param src the source transponder port
     * @param dst the destination transponder port
     * @param signalType signal type
     * @param isBidirectional indicates if intent is unidirectional
     * @param ochSignal optional OCh signal
     * @param suggestedPath optional suggested path
     * @param priority priority to use for flows from this intent
     * @param resourceGroup resource group of this intent
     */
    protected OpticalConnectIntent(ApplicationId appId,
                                        Key key,
                                        ConnectPoint src,
                                        ConnectPoint dst,
                                        OduSignalType signalType,
                                        boolean isBidirectional,
                                        Optional<OchSignal> ochSignal,
                                        Optional<Path> suggestedPath,
                                        int priority,
                                        ResourceGroup resourceGroup) {
        super(appId, key, Collections.emptyList(), priority, resourceGroup);
        this.src = checkNotNull(src);
        this.dst = checkNotNull(dst);
        this.signalType = checkNotNull(signalType);
        this.isBidirectional = isBidirectional;
        this.ochSignal = ochSignal;
        this.suggestedPath = suggestedPath;
    }

    /**
     * Returns a new optical connectivity intent builder.
     *
     * @return host to host intent builder
     */
    public static OpticalConnectivityIntent.Builder builder() {
        return new OpticalConnectivityIntent.Builder();
    }


    /**
     * Builder for optical connectivity intents.
     */
    public static class Builder extends Intent.Builder {
        private ConnectPoint src;
        private ConnectPoint dst;
        private OduSignalType signalType;
        private boolean isBidirectional;
        private Optional<OchSignal> ochSignal = Optional.empty();
        private Optional<Path> suggestedPath = Optional.empty();

        @Override
        public OpticalConnectivityIntent.Builder appId(ApplicationId appId) {
            return (OpticalConnectivityIntent.Builder) super.appId(appId);
        }

        @Override
        public OpticalConnectivityIntent.Builder key(Key key) {
            return (OpticalConnectivityIntent.Builder) super.key(key);
        }

        @Override
        public OpticalConnectivityIntent.Builder priority(int priority) {
            return (OpticalConnectivityIntent.Builder) super.priority(priority);
        }

        @Override
        public OpticalConnectivityIntent.Builder resourceGroup(ResourceGroup resourceGroup) {
            return (OpticalConnectivityIntent.Builder) super.resourceGroup(resourceGroup);
        }

        /**
         * Sets the source for the intent that will be built.
         *
         * @param src source to use for built intent
         * @return this builder
         */
        public OpticalConnectIntent.Builder src(ConnectPoint src) {
            this.src = src;
            return this;
        }

        /**
         * Sets the destination for the intent that will be built.
         *
         * @param dst dest to use for built intent
         * @return this builder
         */
        public OpticalConnectIntent.Builder dst(ConnectPoint dst) {
            this.dst = dst;
            return this;
        }

        /**
         * Sets the ODU signal type for the intent that will be built.
         *
         * @param signalType ODU signal type
         * @return this builder
         */
        public OpticalConnectIntent.Builder signalType(OduSignalType signalType) {
            this.signalType = signalType;
            return this;
        }

        /**
         * Sets the directionality of the intent.
         *
         * @param isBidirectional true if bidirectional, false if unidirectional
         * @return this builder
         */
        public OpticalConnectIntent.Builder bidirectional(boolean isBidirectional) {
            this.isBidirectional = isBidirectional;
            return this;
        }

        /**
         * Sets the OCh signal of the intent.
         *
         * @param ochSignal the lambda
         * @return this builder
         */
        public OpticalConnectIntent.Builder ochSignal(Optional<OchSignal> ochSignal) {
            this.ochSignal = ochSignal;
            return this;
        }

        /**
         * Sets the suggestedPath of the intent.
         *
         * @param suggestedPath the path
         * @return this builder
         */
        public OpticalConnectIntent.Builder suggestedPath(Optional<Path> suggestedPath) {
            this.suggestedPath = suggestedPath;
            return this;
        }

        /**
         * Builds an optical connectivity intent from the accumulated parameters.
         *
         * @return point to point intent
         */
        public OpticalConnectIntent build() {

            return new OpticalConnectIntent(
                    appId,
                    key,
                    src,
                    dst,
                    signalType,
                    isBidirectional,
                    ochSignal,
                    suggestedPath,
                    priority,
                    resourceGroup
            );
        }
    }

    /**
     * Constructor for serializer.
     */
    protected OpticalConnectIntent() {
        super();
        this.src = null;
        this.dst = null;
        this.signalType = null;
        this.isBidirectional = false;
        this.ochSignal = null;
        this.suggestedPath = null;
    }

    /**
     * Returns the source transponder port.
     *
     * @return source transponder port
     */
    public ConnectPoint getSrc() {
        return src;
    }

    /**
     * Returns the destination transponder port.
     *
     * @return source transponder port
     */
    public ConnectPoint getDst() {
        return dst;
    }

    /**
     * Returns the ODU signal type.
     *
     * @return ODU signal type
     */
    public OduSignalType getSignalType() {
        return signalType;
    }

    /**
     * Returns the directionality of the intent.
     *
     * @return true if bidirectional, false if unidirectional
     */
    public boolean isBidirectional() {
        return isBidirectional;
    }

    /**
     * Returns the OCh signal of the intent.
     *
     * @return the lambda
     */

    public Optional<OchSignal> ochSignal() {
        return ochSignal;
    }

    /**
     * Returns the suggestedPath of the intent.
     *
     * @return the suggestedPath
     */
    public Optional<Path> suggestedPath() {
        return suggestedPath;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id())
                .add("key", key())
                .add("appId", appId())
                .add("priority", priority())
                .add("resources", resources())
                .add("src", src)
                .add("dst", dst)
                .add("signalType", signalType)
                .add("isBidirectional", isBidirectional)
                .add("ochSignal", ochSignal)
                .add("suggestedPath", suggestedPath)
                .add("resourceGroup", resourceGroup())
                .toString();
    }
}

