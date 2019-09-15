package org.sindaryn.datafi.generator;


import com.squareup.javapoet.*;
import org.sindaryn.datafi.annotations.WithResolver;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SqlQueryMethodParser {

    public static MethodSpec parseResolver(WithResolver resolver, TypeElement typeElement){
        String prefix = prefix(resolver, typeElement);
        String whereClause = whereClause(resolver, typeElement);
        ParameterSpec[] args = args(resolver, typeElement);
        String orderByClause = resolver.orderBy();
        return   MethodSpec.methodBuilder(resolver.name())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addAnnotation(AnnotationSpec.builder(Query.class)
                        .addMember("value", "$S", prefix + whereClause + orderByClause)
                        .build())
                .addParameters(Arrays.asList(args))
                .returns(returnType(resolver, typeElement))
                .build();
    }

    private static TypeName returnType(WithResolver resolver, TypeElement typeElement) {
        TypeName result;
        switch (resolver.type()){
            case SELECT_BY: result = ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(typeElement));
                break;
            default: throw new IllegalStateException("Unexpected value: " + resolver.type());
        }
        return result;
    }

    private static ParameterSpec[] args(WithResolver resolver, TypeElement typeElement){
        String[] args = resolver.args();
        ParameterSpec[] result = new ParameterSpec[args.length];
        Map<String, TypeName> fields = resolveFieldTypesOf(typeElement);
        for (int i = 0; i < args.length; i++){
            result[i] =
                    ParameterSpec
                    .builder(fields.get(args[i]), args[i])
                    .addAnnotation(
                            AnnotationSpec.builder(Param.class)
                                    .addMember("value", "$S", args[i])
                                    .build())
                    .build();
        }
        return result;
    }

    private static Map<String, TypeName> resolveFieldTypesOf(TypeElement typeElement) {
        Map<String, TypeName> result = new HashMap<>();
        for(Element field : typeElement.getEnclosedElements())
            if (field.getKind().isField())
                result.put(field.getSimpleName().toString(), ClassName.get(field.asType()));
        return result;
    }

    private static String prefix(WithResolver resolver, TypeElement typeElement){
        String entityName = resolveEntityName(typeElement);
        String placeholder = entityName.substring(0, 1).toLowerCase();
        switch (resolver.type()){
            case SELECT_BY: return "SELECT " + placeholder + " FROM " + entityName + " " + placeholder + " ";
            /*case AVG: return "SELECT AVG("  + placeholder +  ".__ARGS__" +  ") FROM " + entityName + " " + placeholder + " ";
            case COUNT: return "SELECT COUNT(" + "__ARGS__" + ") FROM " + entityName + " " + placeholder + " ";
            case MAX: return "SELECT MAX(" + placeholder + ".__ARGS__" + ") FROM " + entityName + " " + placeholder + " ";
            case MIN: return "SELECT MIN(" + placeholder + ".__ARGS__" + ") FROM " + entityName + " " + placeholder + " ";
            case SUM: return "SELECT SUM(" + placeholder + ".__ARGS__" + ") FROM " + entityName + " " + placeholder + " ";
            case UPDATE: return "UPDATE " + entityName + " " + placeholder +  " SET ";
            case DELETE: return "DELETE " + entityName + " " + placeholder + " ";*/
        }
        return null;
    }

    private static String whereClause(WithResolver resolver, TypeElement typeElement){
        String entityName = resolveEntityName(typeElement);
        String placeholder = entityName.substring(0, 1).toLowerCase();
        StringBuilder whereClause = new StringBuilder(resolver.where());
        String conditional = isPreDefinedConditional(whereClause);
        if(conditional != null){
            whereClause = new StringBuilder("WHERE");
            String[] args = resolver.args();
            for (int i = 0; i < args.length; i++) {
                whereClause.append(" ").append(placeholder).append(".").append(args[i]).append(" = :").append(args[i]);
                if((i + 1) != args.length){
                    whereClause.append(conditional);
                }
            }
        }else if(!whereClause.toString().toLowerCase().matches("^\\s*where\\s+")) {
            whereClause = new StringBuilder("WHERE " + whereClause.toString());
        }
        return whereClause.toString();
    }

    private static String isPreDefinedConditional(StringBuilder whereClause) {
        switch (whereClause.toString()){
            case "&&&": return " AND";
            case "|||": return " OR";
            default: return null;
        }
    }

    private static String resolveEntityName(TypeElement typeElement) {
        Entity entityAnnotation = typeElement.getAnnotation(Entity.class);
        Table tableAnnotation = typeElement.getAnnotation(Table.class);
        if(entityAnnotation != null && !entityAnnotation.name().equals("")){
            return entityAnnotation.name();
        }
        if(tableAnnotation != null && !tableAnnotation.name().equals("")){
            return tableAnnotation.name();
        }
        return typeElement.getSimpleName().toString();
    }
}
