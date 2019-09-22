package org.sindaryn.datafi.generator;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import lombok.Data;
import lombok.NonNull;
import org.sindaryn.datafi.annotations.GetAllBy;
import org.sindaryn.datafi.annotations.GetBy;
import org.sindaryn.datafi.annotations.GetByUnique;
import org.sindaryn.datafi.persistence.GenericDao;
import org.springframework.stereotype.Repository;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.persistence.Column;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.squareup.javapoet.ParameterizedTypeName.get;
import static org.sindaryn.datafi.StaticUtils.*;
@Data
public class DaoFactory {
    @NonNull
    private ProcessingEnvironment processingEnv;
    /**
     * generate the actual '<entity name>Dao.java' jpa repository for a given entity
     * @param entity - the given data model entity / table
     * @param annotatedFieldsMap - a reference telling us whether this repository needs any custom
     * @param customResolversMap
     */
    protected void generateDao(
            TypeElement entity,
            Map<TypeElement, List<VariableElement>> annotatedFieldsMap,
            Map<TypeElement, List<MethodSpec>> customResolversMap,
            Map<TypeElement, MethodSpec> fuzzySearchMethods) {

        String className = entity.getQualifiedName().toString();
        int lastDot = className.lastIndexOf('.');
        String packageName = className.substring(0, lastDot);
        String simpleClassName = className.substring(lastDot + 1);
        String repositoryName = simpleClassName + "Dao";

        TypeSpec.Builder builder = TypeSpec.interfaceBuilder(repositoryName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Repository.class)
                .addSuperinterface(get(ClassName.get(GenericDao.class), getIdType(entity, processingEnv), ClassName.get(entity)));
        Collection<VariableElement> annotatedFields = annotatedFieldsMap.get(entity);
        if(annotatedFields != null)
            annotatedFields.forEach(annotatedField -> handleAnnotatedField(entity, builder, annotatedField));
        if(customResolversMap.get(entity) != null)
            customResolversMap.get(entity).forEach(builder::addMethod);
        if(fuzzySearchMethods.get(entity) != null)
            builder.addMethod(fuzzySearchMethods.get(entity));
        writeToJavaFile(entity.getSimpleName().toString(), packageName, builder, processingEnv, "JpaRepository");
    }
    private void handleAnnotatedField(TypeElement entity, TypeSpec.Builder builder, VariableElement annotatedField) {
        if(annotatedField.getAnnotation(GetBy.class) != null)
            handleGetBy(entity, builder, annotatedField);
        if(annotatedField.getAnnotation(GetAllBy.class) != null)
            handleGetAllBy(entity, builder, annotatedField);
        if(annotatedField.getAnnotation(GetByUnique.class) != null)
            handleGetByUnique(entity, builder, annotatedField);
    }

    private void handleGetByUnique(TypeElement entity, TypeSpec.Builder builder, VariableElement annotatedField) {
        if(annotatedField.getAnnotation(GetBy.class) != null){
            logCompilationError(processingEnv, annotatedField, "@GetBy and @GetByUnique cannot by definition be used together");
        }else if(annotatedField.getAnnotation(Column.class) == null || !annotatedField.getAnnotation(Column.class).unique()){
            logCompilationError(processingEnv, annotatedField, "In order to use @GetByUnique on a field, annotate the field as @Column(unique = true)");
        }
        else {
            builder
                    .addMethod(MethodSpec
                            .methodBuilder(
                                    "findBy" + toPascalCase(annotatedField.getSimpleName().toString()))
                            .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                            .addParameter(
                                    ClassName.get(annotatedField.asType()),
                                    annotatedField.getSimpleName().toString())
                            .returns(get(ClassName.get(Optional.class), ClassName.get(entity)))
                            .build());
        }
    }

    private void handleGetAllBy(TypeElement entity, TypeSpec.Builder builder, VariableElement annotatedField) {
        builder
                .addMethod(MethodSpec
                        .methodBuilder(
                                "findAllBy" + toPascalCase(annotatedField.getSimpleName().toString()) + "In")
                        .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                        .addParameter(
                                get(ClassName.get(List.class), ClassName.get(annotatedField.asType())),
                                toPlural(annotatedField.getSimpleName().toString()))
                        .returns(get(ClassName.get(List.class), ClassName.get(entity)))
                        .build());
    }

    private void handleGetBy(TypeElement entity, TypeSpec.Builder builder, VariableElement annotatedField) {
        builder
                .addMethod(MethodSpec
                        .methodBuilder(
                                "findBy" + toPascalCase(annotatedField.getSimpleName().toString()))
                        .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                        .addParameter(
                                ClassName.get(annotatedField.asType()),
                                annotatedField.getSimpleName().toString())
                        .returns(get(ClassName.get(List.class), ClassName.get(entity)))
                        .build());
    }
}
