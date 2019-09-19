package org.sindaryn.datafi.service;

import org.springframework.stereotype.Service;

@Service
public interface EntityTypeRuntimeResolver<T> {
    Class<T> getType();
}
