package io.fabric8.api;

/**
 * LifecycleException
 *
 * @author thomas.diesler@jboss.com
 * @since 26-Feb-2014
 */
public class LifecycleException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public LifecycleException(String message) {
        super(message);
    }

    public LifecycleException(String message, Throwable cause) {
        super(message, cause);
    }

}
