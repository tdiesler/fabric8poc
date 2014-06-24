package io.fabric8.spi;

import java.beans.ConstructorProperties;

import org.jboss.gravia.utils.IllegalArgumentAssertion;

/**
 * An agent registration
 *
 * @author thomas.diesler@jboss.com
 * @since 07-Jun-2014
 *
 * @Immutable
 */
public final class AgentRegistration {

    private final AgentIdentity identity;
    private final String runtimeType;
    private final String targetHost;
    private final String serviceUrl;

    @ConstructorProperties( { "identity", "runtimeType", "targetHost", "serviceUrl" } )
    public AgentRegistration(AgentIdentity identity, String runtimeType, String targetHost, String serviceUrl) {
        IllegalArgumentAssertion.assertNotNull(identity, "identity");
        IllegalArgumentAssertion.assertNotNull(runtimeType, "runtimeType");
        IllegalArgumentAssertion.assertNotNull(targetHost, "targetHost");
        IllegalArgumentAssertion.assertNotNull(serviceUrl, "serviceUrl");
        this.identity = identity;
        this.runtimeType = runtimeType;
        this.targetHost = targetHost;
        this.serviceUrl = serviceUrl;
    }

    public AgentIdentity getIdentity() {
        return identity;
    }

    public String getRuntimeType() {
        return runtimeType;
    }

    public String getTargetHost() {
        return targetHost;
    }

    public String getServiceUrl() {
        return serviceUrl;
    }

    @Override
    public int hashCode() {
        return identity.hashCode();
    }

    public boolean equals(Object obj) {
        if (obj == this)return true;
        if (!(obj instanceof AgentRegistration)) return false;
        AgentRegistration other = (AgentRegistration) obj;
        return identity.equals(other.identity);
    }

    public String toString() {
        return "AgentRegistration[id=" + identity + ",type=" + runtimeType + ",host=" + targetHost + ",jmx=" + serviceUrl + "]";
    }
}