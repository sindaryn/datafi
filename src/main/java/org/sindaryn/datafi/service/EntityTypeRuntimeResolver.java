package org.sindaryn.datafi.service;

public interface EntityTypeRuntimeResolver<T> {
    Class<T> getType();
}
