package io.fabric8.spi;

import java.io.Serializable;
import java.net.InetAddress;

/**
 * An agent registration
 *
 * @author thomas.diesler@jboss.com
 * @since 07-Jun-2014
 */
public final class AgentRegistration implements Serializable {

    private static final long serialVersionUID = -5413283194351021389L;

    private final AgentIdentity identity;
    private final InetAddress targetHost;
    private final String jolokiaAgentUrl;
    private final String jolokiaUsername;
    private final String jolokiaPassword;

    public AgentRegistration(AgentIdentity identity, InetAddress targetHost, String jolokiaAgentUrl, String jolokiaUsername, String jmxPassword) {
        this.identity = identity;
        this.targetHost = targetHost;
        this.jolokiaAgentUrl = jolokiaAgentUrl;
        this.jolokiaUsername = jolokiaUsername;
        this.jolokiaPassword = jmxPassword;
    }

    public AgentIdentity getIdentity() {
        return identity;
    }

    public InetAddress getTargetHost() {
        return targetHost;
    }

    public String getJolokiaAgentUrl() {
        return jolokiaAgentUrl;
    }

    public String getJolokiaUsername() {
        return jolokiaUsername;
    }

    public String getJolokiaPassword() {
        return jolokiaPassword;
    }

    @Override
    public int hashCode() {
        return identity.hashCode();
    }

    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof AgentRegistration))
            return false;
        AgentRegistration other = (AgentRegistration) obj;
        return identity.equals(other.identity);
    }

    public String toString() {
        return "AgentRegistration[id=" + identity + ",host=" + targetHost + ",jmx=" + jolokiaAgentUrl + "]";
    }
}