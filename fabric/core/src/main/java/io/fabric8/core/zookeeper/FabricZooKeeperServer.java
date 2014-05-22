/*
 * Copyright (C) 2010 - 2014 JBoss by Red Hat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 */

package io.fabric8.core.zookeeper;

import io.fabric8.spi.Configurer;
import io.fabric8.spi.scr.AbstractComponent;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.zookeeper.server.NIOServerCnxnFactory;
import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.ServerStats;
import org.apache.zookeeper.server.ZKDatabase;
import org.apache.zookeeper.server.ZooKeeperServer;
import org.apache.zookeeper.server.persistence.FileTxnSnapLog;
import org.apache.zookeeper.server.quorum.QuorumPeer;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;
import org.apache.zookeeper.server.quorum.QuorumStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import static io.fabric8.core.zookeeper.ZookeeperConstants.ZOOKEEPER_SERVER_PID;

@Component( label = "Fabric8 ZooKeeper Server", configurationPid = ZOOKEEPER_SERVER_PID, policy = ConfigurationPolicy.REQUIRE, immediate = true, metatype = true)
@org.apache.felix.scr.annotations.Properties({
        @Property(name = "clientPort", intValue = FabricZooKeeperServer.DEFAULT_CLIENT_PORT, label = "Client Port", description = "The port to listen for client connections"),
        @Property(name = "tickTime", intValue = ZooKeeperServer.DEFAULT_TICK_TIME, label = "Tick Time", description = "The basic time unit in milliseconds used by ZooKeeper. It is used to do heartbeats and the minimum session timeout will be twice the tickTime"),
        @Property(name = "dataDir", value = "${runtime.data}/zookeeper/00000/data", label = "Data Directory", description = "The location to store the in-memory database snapshots and, unless specified otherwise, the transaction log of updates to the database"),
        @Property(name = "initLimit", intValue = FabricZooKeeperServer.DEFAULT_INIT_LIMIT, label = "Init Limit", description = "The amount of time in ticks (see tickTime), to allow followers to connect and sync to a leader. Increased this value as needed, if the amount of data managed by ZooKeeper is large"),
        @Property(name = "syncLimit", intValue = FabricZooKeeperServer.DEFAULT_SYNC_LIMIT, label = "Sync Limit", description = "The amount of time, in ticks (see tickTime), to allow followers to sync with ZooKeeper. If followers fall too far behind a leader, they will be dropped"),
        @Property(name = "dataLogDir", value = "${runtime.data}/zookeeper/00000/data", label = "Data Log Directory", description = "This option will direct the machine to write the transaction log to the dataLogDir rather than the dataDir. This allows a dedicated log device to be used, and helps avoid competition between logging and snaphots"),

        @Property(name = "maxClientCnxns", intValue = FabricZooKeeperServer.DEFAULT_MAX_CLIENT_CNXNS, label = "Maximum Client Connections Per Host", description = "Limits the number of concurrent connections (at the socket level) that a single client, identified by IP address, may make to a single member of the ZooKeeper ensemble"),
        @Property(name = "clientPortAddress", value = FabricZooKeeperServer.DEFAULT_CLIENT_PORT_ADDRESS, label = "Client Port Address", description = "The address (ipv4, ipv6 or hostname) to listen for client connections; that is, the address that clients attempt to connect to"),
        @Property(name = "minSessionTimeout", intValue = FabricZooKeeperServer.DEFAULT_MINIMUM_SESSION_TIMEOUT, label = "Minimum Session Timeout", description = "The minimum session timeout in milliseconds that the server will allow the client to negotiate"),
        @Property(name = "maxSessionTimeout", intValue = FabricZooKeeperServer.DEFAULT_MAXIMUM_SESSION_TIMEOUT, label = "Maximum Session Timeout", description = "Limits the number of concurrent connections (at the socket level) that a single client, identified by IP address, may make to a single member of the ZooKeeper ensemble"),
}
)
public class FabricZooKeeperServer extends AbstractComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(FabricZooKeeperServer.class);

    public static final int DEFAULT_CLIENT_PORT = 2181;
    public static final int DEFAULT_INIT_LIMIT = 10;
    public static final int DEFAULT_SYNC_LIMIT = 5;

    public static final int DEFAULT_MAX_CLIENT_CNXNS = 60;
    public static final String DEFAULT_CLIENT_PORT_ADDRESS = "0.0.0.0";
    public static final int DEFAULT_MINIMUM_SESSION_TIMEOUT = 2 * ZooKeeperServer.DEFAULT_TICK_TIME;
    public static final int DEFAULT_MAXIMUM_SESSION_TIMEOUT = 20 * ZooKeeperServer.DEFAULT_TICK_TIME;

    static final String SERVER_ID = "server.id";
    static final String MY_ID = "myid";

    @Reference
    private Configurer configurer;
    private File dataDir;

    private Destroyable destroyable;

    @Activate
    void activate(Map<String, ?> configuration) throws Exception {
        configurer.configure(configuration, this);
        activateInternal(configuration);
        activateComponent();
    }

    @Modified
    void modified(Map<String, ?> configuration) throws Exception {
        deactivateInternal();
        destroyable = activateInternal(configuration);
    }

    @Deactivate
    void deactivate() throws Exception {
        deactivateComponent();
        deactivateInternal();
    }

    private Destroyable activateInternal(Map<String, ?> configuration) throws Exception {
        LOGGER.info("Creating zookeeper server with: {}", configuration);

        Properties props = new Properties();
        for (Map.Entry<String, ?> entry : configuration.entrySet()) {
            props.put(entry.getKey(), entry.getValue());
        }

        // Create myid file
        String serverId = (String) props.get(SERVER_ID);
        if (serverId != null) {
            props.remove(SERVER_ID);
            File myId = new File(dataDir, MY_ID);
            if (myId.exists() && !myId.delete()) {
                throw new IOException("Failed to delete " + myId);
            }
            if (myId.getParentFile() == null || (!myId.getParentFile().exists() && !myId.getParentFile().mkdirs())) {
                throw new IOException("Failed to create " + myId.getParent());
            }
            FileOutputStream fos = new FileOutputStream(myId);
            try {
                fos.write((serverId + "\n").getBytes());
            } finally {
                fos.close();
            }
        }

        QuorumPeerConfig peerConfig = getPeerConfig(props);

        if (!peerConfig.getServers().isEmpty()) {
            NIOServerCnxnFactory cnxnFactory = new NIOServerCnxnFactory();
            cnxnFactory.configure(peerConfig.getClientPortAddress(), peerConfig.getMaxClientCnxns());

            QuorumPeer quorumPeer = new QuorumPeer();
            quorumPeer.setClientPortAddress(peerConfig.getClientPortAddress());
            quorumPeer.setTxnFactory(new FileTxnSnapLog(new File(peerConfig.getDataLogDir()), new File(peerConfig.getDataDir())));
            quorumPeer.setQuorumPeers(peerConfig.getServers());
            quorumPeer.setElectionType(peerConfig.getElectionAlg());
            quorumPeer.setMyid(peerConfig.getServerId());
            quorumPeer.setTickTime(peerConfig.getTickTime());
            quorumPeer.setMinSessionTimeout(peerConfig.getMinSessionTimeout());
            quorumPeer.setMaxSessionTimeout(peerConfig.getMaxSessionTimeout());
            quorumPeer.setInitLimit(peerConfig.getInitLimit());
            quorumPeer.setSyncLimit(peerConfig.getSyncLimit());
            quorumPeer.setQuorumVerifier(peerConfig.getQuorumVerifier());
            quorumPeer.setCnxnFactory(cnxnFactory);
            quorumPeer.setZKDatabase(new ZKDatabase(quorumPeer.getTxnFactory()));
            quorumPeer.setLearnerType(peerConfig.getPeerType());

            try {
                LOGGER.debug("Starting quorum peer \"%s\" on address %s", quorumPeer.getMyid(), peerConfig.getClientPortAddress());
                quorumPeer.start();
                LOGGER.debug("Started quorum peer \"%s\"", quorumPeer.getMyid());
            } catch (Exception e) {
                LOGGER.warn(String.format("Failed to start quorum peer \"%s\", reason : %s ", quorumPeer.getMyid(), e.getMessage()));
                quorumPeer.shutdown();
                throw e;
            }

            // Register stats provider
            ClusteredServer server = new ClusteredServer(quorumPeer);
            return server;
        } else {
            ServerConfig serverConfig = getServerConfig(peerConfig);

            ZooKeeperServer zkServer = new ZooKeeperServer();
            FileTxnSnapLog ftxn = new FileTxnSnapLog(new File(serverConfig.getDataLogDir()), new File(serverConfig.getDataDir()));
            zkServer.setTxnLogFactory(ftxn);
            zkServer.setTickTime(serverConfig.getTickTime());
            zkServer.setMinSessionTimeout(serverConfig.getMinSessionTimeout());
            zkServer.setMaxSessionTimeout(serverConfig.getMaxSessionTimeout());
            NIOServerCnxnFactory cnxnFactory = new NIOServerCnxnFactory() {
                protected void configureSaslLogin() throws IOException {
                }
            };
            cnxnFactory.configure(serverConfig.getClientPortAddress(), serverConfig.getMaxClientCnxns());

            try {
                LOGGER.debug("Starting ZooKeeper server on address %s", peerConfig.getClientPortAddress());
                cnxnFactory.startup(zkServer);
                LOGGER.debug("Started ZooKeeper server");
            } catch (Exception e) {
                LOGGER.warn(String.format("Failed to start ZooKeeper server, reason : %s", e));
                cnxnFactory.shutdown();
                throw e;
            }

            // Register stats provider
            SimpleServer server = new SimpleServer(zkServer, cnxnFactory);

            return server;
        }
    }

    private void deactivateInternal() throws Exception {
        LOGGER.info("Destroying zookeeper server: {}", destroyable);
        if (destroyable != null) {
            destroyable.destroy();
            destroyable = null;
        }
    }

    private QuorumPeerConfig getPeerConfig(Properties props) throws IOException, QuorumPeerConfig.ConfigException {
        QuorumPeerConfig peerConfig = new QuorumPeerConfig();
        peerConfig.parseProperties(props);
        LOGGER.info("Created zookeeper peer configuration: {}", peerConfig);
        return peerConfig;
    }

    private ServerConfig getServerConfig(QuorumPeerConfig peerConfig) {
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.readFrom(peerConfig);
        LOGGER.info("Created zookeeper server configuration: {}", serverConfig);
        return serverConfig;
    }

    interface Destroyable {
        void destroy() throws Exception;
    }

     class SimpleServer implements Destroyable, ServerStats.Provider {
        private final ZooKeeperServer server;
        private final NIOServerCnxnFactory cnxnFactory;

        SimpleServer(ZooKeeperServer server, NIOServerCnxnFactory cnxnFactory) {
            this.server = server;
            this.cnxnFactory = cnxnFactory;
        }

        @Override
        public void destroy() throws Exception {
            cnxnFactory.shutdown();
            cnxnFactory.join();
        }

        @Override
        public long getOutstandingRequests() {
            return server.getOutstandingRequests();
        }

        @Override
        public long getLastProcessedZxid() {
            return server.getLastProcessedZxid();
        }

        @Override
        public String getState() {
            return server.getState();
        }

        @Override
        public int getNumAliveConnections() {
            return server.getNumAliveConnections();
        }
    }

     class ClusteredServer implements Destroyable, QuorumStats.Provider {
        private final QuorumPeer peer;

        ClusteredServer(QuorumPeer peer) {
            this.peer = peer;
        }

        @Override
        public void destroy() throws Exception {
            peer.shutdown();
            peer.join();
        }

        @Override
        public String[] getQuorumPeers() {
            return peer.getQuorumPeers();
        }

        @Override
        public String getServerState() {
            return peer.getServerState();
        }
    }
}
