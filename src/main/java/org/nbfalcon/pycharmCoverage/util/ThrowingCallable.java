package org.nbfalcon.pycharmCoverage.util;

@FunctionalInterface
public interface ThrowingCallable<V, E extends Exception> {
    V call() throws E;
}