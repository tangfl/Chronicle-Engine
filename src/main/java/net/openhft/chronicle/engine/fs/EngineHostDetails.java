/*
 * Copyright 2016 higherfrequencytrading.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.openhft.chronicle.engine.fs;

import net.openhft.chronicle.core.io.Closeable;
import net.openhft.chronicle.network.TCPRegistry;
import net.openhft.chronicle.network.api.session.SessionDetails;
import net.openhft.chronicle.network.api.session.SessionProvider;
import net.openhft.chronicle.network.cluster.HostDetails;
import net.openhft.chronicle.network.connection.TcpChannelHub;
import net.openhft.chronicle.wire.Marshallable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static net.openhft.chronicle.network.VanillaSessionDetails.of;

public class EngineHostDetails extends HostDetails implements Marshallable, Closeable {

    private final Map<InetSocketAddress, TcpChannelHub> tcpChannelHubs = new ConcurrentHashMap<>();

    EngineHostDetails() {
        super();
    }

    public EngineHostDetails(int hostId, int tcpBufferSize, @NotNull String connectUri) {
        super();
        hostId(hostId);
        this.tcpBufferSize(tcpBufferSize);
        this.connectUri(connectUri);
    }

    @Override
    public void close() {
        tcpChannelHubs.values().forEach(Closeable::closeQuietly);
    }

    /**
     * @return the {@code TcpChannelHub} if it exists, otherwise {@code null}
     */
    public TcpChannelHub tcpChannelHub() {
        return tcpChannelHubs.get(TCPRegistry.lookup(connectUri()));
    }

    /**
     * implements SessionProvider but always returns the same session details regardless of thread
     */
    private class SimpleSessionProvider implements SessionProvider {
        @Nullable
        private final SessionDetails sessionDetails;

        SimpleSessionProvider(@Nullable SessionDetails sessionDetails) {
            this.sessionDetails = (sessionDetails == null) ? of("", "", "") : sessionDetails;
        }

        /**
         * @return the current session details
         */
        @Override
        @Nullable
        public SessionDetails get() {
            return sessionDetails;
        }

        /**
         * Replace the session details
         *
         * @param sessionDetails to set to
         */
        @Override
        public void set(@NotNull SessionDetails sessionDetails) {
            throw new UnsupportedOperationException();
        }

        /**
         * There is no longer any valid session details and get() will return null.
         */
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
