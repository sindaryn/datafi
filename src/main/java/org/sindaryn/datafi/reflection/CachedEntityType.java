package org.sindaryn.datafi.reflection;

import org.sindaryn.datafi.annotations.NonApiUpdatable;
import org.sindaryn.datafi.annotations.NonApiUpdatables;
import org.sindaryn.datafi.annotations.NonNullable;

import javax.persistence.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import static org.sindaryn.datafi.StaticUtils.toPascalCase;

@lombok.Getter
public class CachedEntityType {

    private Class<?> clazz;
    private Object defaultInstance;
    private Map<String, CachedEntityField> fields;
    private List<Field> cascadeUpdatableFields;
    private Map<String, Method> publicMethods;

    public CachedEntityType(Class<?> clazz, Collection<Field> fields, Collection<Method> publicMethods) {
        this.clazz = clazz;
        this.fields = new HashMap<>();
        fields.forEach(field -> {
            boolean isCollectionOrMap =
                    Iterable.class.isAssignableFrom(field.getType()) ||
                            Map.class.isAssignableFrom(field.getType());
            boolean isNonApiUpdatable = isNonApiUpdatable(field);
            boolean isNonNullable = isNonNullableField(field);
            this.fields.put(field.getName(), new CachedEntityField(field, isCollectionOrMap, isNonApiUpdatable, isNonNullable));
        });
        this.publicMethods = new HashMap<>();
        publicMethods.forEach(publicMethod -> this.publicMethods.put(publicMethod.getName(), publicMethod));
        this.defaultInstance = genDefaultInstance(clazz);
        setCascadeUpdatableFields();
    }

    public Object invokeGetter(Object instance, String fieldName){
        try {
            return publicMethods.get("get" + toPascalCase(fieldName)).invoke(instance);
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public void invokeSetter(Object instance, String fieldName, Object value){
        try {
            publicMethods.get("set" + toPascalCase(fieldName)).invoke(instance, value);
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    private boolean isNonApiUpdatable(Field field) {
        return  field.isAnnotationPresent(NonApiUpdatable.class) ||
                isInNonCascadeUpdatables(field) ||
                field.isAnnotationPresent(Id.class) ||
                field.isAnnotationPresent(EmbeddedId.class) ||
                Iterable.class.isAssignableFrom(field.getType()) ||
                field.isAnnotationPresent(ElementCollection.class) ||
                field.isAnnotationPresent(CollectionTable.class) ||
                field.getType().equals(Map.class);
    }

    private boolean isInNonCascadeUpdatables(Field field) {
        NonApiUpdatables nonApiUpdatables = clazz.getAnnotation(NonApiUpdatables.class);
        if(nonApiUpdatables != null){
            for(String fieldName : nonApiUpdatables.value()){
                if(fieldName.equals(field.getName()))
                    return true;
            }
        }
        return false;
    }

    private boolean isNonNullableField(Field field) {
        return field.isAnnotationPresent(NonNullable.class) ||
                (field.isAnnotationPresent(Column.class) && !field.getAnnotation(Column.class).nullable()) ||
                (field.isAnnotationPresent(OneToOne.class) && !field.getAnnotation(OneToOne.class).optional()) ||
                (field.isAnnotationPresent(ManyToOne.class) && !field.getAnnotation(ManyToOne.class).optional());
    }

    public static Object genDefaultInstance(Class<?> clazz){
        Constructor[] cons = clazz.getDeclaredConstructors();
        try {
            for(Constructor constructor : cons){
                if(constructor.getParameterCount() == 0){
                    constructor.setAccessible(true);
                    return constructor.newInstance();
                }
            }
            throw new RuntimeException("No default constructor found for " + clazz.getSimpleName());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setCascadeUpdatableFields(){
        this.cascadeUpdatableFields = new ArrayList<>();
        fields.values().forEach(_field -> {
            if(!_field.isNonApiUpdatable())
                cascadeUpdatableFields.add(_field.getField());
        });
    }
}
