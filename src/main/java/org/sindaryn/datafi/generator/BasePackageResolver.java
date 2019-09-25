package org.sindaryn.datafi.generator;

import lombok.Data;
import lombok.NonNull;

@Data
public class BasePackageResolver {
    @NonNull
    private String basePackage;
}
