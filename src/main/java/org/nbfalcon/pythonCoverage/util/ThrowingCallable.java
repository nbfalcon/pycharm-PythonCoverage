package org.nbfalcon.pythonCoverage.util;

@FunctionalInterface
public interface ThrowingCallable<V, E extends Exception> {
    V call() throws E;
}