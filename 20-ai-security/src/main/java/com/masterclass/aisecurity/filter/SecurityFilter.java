package com.masterclass.aisecurity.filter;

/**
 * Composable security filter applied by SecurityGateway in order.
 * Implementations must be stateless — they are singletons in the Spring context.
 */
@FunctionalInterface
public interface SecurityFilter {

    /**
     * Screen or transform the input context.
     * Throw {@link SecurityViolationException} to block the request entirely.
     * Return a (possibly mutated) context to continue the chain.
     */
    SecurityContext apply(SecurityContext context);
}
