package org.sindaryn.datafi.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;
/**
 * When a field within a given entity is annotated with @GetBy, resolvers are generated in both the
 * data and web layers to fetch the given entity by the value of the annotated field (passed as an argument).
 */
@Target({FIELD, TYPE})
@Retention(SOURCE)
public @interface GetBy {
}
