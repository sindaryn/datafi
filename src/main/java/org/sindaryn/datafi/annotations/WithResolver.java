package org.sindaryn.datafi.annotations;

import org.sindaryn.datafi.generator.QueryType;

import java.lang.annotation.*;

@Repeatable(WithResolvers.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface WithResolver {
    QueryType type() default QueryType.SELECT_BY;
    //what the jpa method name will be
    String name();
    //the conditions in the generated sql query
    String where() default "&&&";
    //args - MUST BE VALID FIELD NAMES
    String[] args();
    //order
    String orderBy() default "";
    //TODO - Figure out a way to further customize the availability of custom annotations here
}
