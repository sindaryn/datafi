package org.sindaryn.datafi.reflection;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.Getter;
import lombok.NonNull;
import org.reflections.Reflections;
import org.sindaryn.datafi.generator.BasePackageResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

@Component
public class ReflectionCache {

    private Reflections reflections;
    @Getter
    private Map<String, CachedEntityType> entitiesCache;
    @Getter
    private Map<Map.Entry<String, Class<?>[]>, Method> resolversCache;

    @Autowired
    private BasePackageResolver basePackageResolver;
    @PostConstruct
    private void init() {
        reflections = new Reflections(basePackageResolver.getBasePackage());
        entitiesCache = new HashMap<>();
        resolversCache = new HashMap<>();
        Set<Class<?>> dataModelEntityTypes = getAnnotatedEntities();
        for (Class<?> currentType : dataModelEntityTypes) {
            if (isPersistableEntity(currentType))
                entitiesCache.put(
                        currentType.getSimpleName(),
                        new CachedEntityType(
                                currentType,
                                getClassFields(currentType),
                                getPublicMethodsOf(currentType)));
        }
    }

    private boolean isPersistableEntity(Class<?> currentType) {
        return currentType.isAnnotationPresent(Table.class) || currentType.isAnnotationPresent(Entity.class);
    }

    private Set<Class<?>> getAnnotatedEntities() {
        Set<Class<?>> entities = reflections.getTypesAnnotatedWith(Entity.class);
        entities.addAll(reflections.getTypesAnnotatedWith(Table.class));
        entities = Sets.newHashSet(entities);
        return entities;
    }

    private Collection<Method> getPublicMethodsOf(@NonNull Class<?> startClass) {
        List<Method> currentClassMethods = Lists.newArrayList(startClass.getMethods());
        Class<?> parentClass = startClass.getSuperclass();
        if (parentClass != null) {
            List<Method> parentClassFields =
                    (List<Method>) getPublicMethodsOf(parentClass);
            currentClassMethods.addAll(parentClassFields);
        }
        return currentClassMethods;
    }

    public static Collection<Field> getClassFields(@NonNull Class<?> startClass) {
        List<Field> currentClassFields = Lists.newArrayList(startClass.getDeclaredFields());
        Class<?> parentClass = startClass.getSuperclass();
        if (parentClass != null) {
            List<Field> parentClassFields =
                    (List<Field>) getClassFields(parentClass);
            currentClassFields.addAll(parentClassFields);
        }
        return currentClassFields;
    }
}
