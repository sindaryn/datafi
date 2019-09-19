package org.sindaryn.datafi.generator;

import com.squareup.javapoet.*;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.var;
import org.sindaryn.datafi.service.EntityTypeRuntimeResolver;
import org.springframework.stereotype.Component;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.util.HashMap;
import java.util.Map;

import static org.sindaryn.datafi.StaticUtils.writeToJavaFile;
@Data
public class EntityTypeRuntimeResolverBeanFactory {
    private Map<TypeElement, TypeSpec> alreadyCreated = new HashMap<>();
    @NonNull
    private ProcessingEnvironment processingEnv;

    public void generateEntityTypeRuntimeResolverBean(TypeElement entity){
        if(alreadyCreated.get(entity) != null) return;
        String entityName = entity.getSimpleName().toString();
        ClassName entityClassname = ClassName.get(entity);
        String entityRuntimeTypeResolverName = entityName + "EntityTypeRuntimeResolver";
        var builder = TypeSpec.classBuilder(entityRuntimeTypeResolverName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec.builder(Component.class)
                        .addMember("value", "$S", entityRuntimeTypeResolverName)
                        .build())
                .addSuperinterface(
                        ParameterizedTypeName.get(ClassName.get(EntityTypeRuntimeResolver.class), entityClassname)
                )
                .addField(FieldSpec.builder(
                        ParameterizedTypeName.get(ClassName.get(Class.class), entityClassname),
                        "type",
                        Modifier.PRIVATE, Modifier.FINAL)
                        .addAnnotation(Getter.class)
                        .initializer("$T.class", entityClassname)
                        .build());

        TypeSpec result = builder.build();
        alreadyCreated.put(entity, result);
        int lastDot = entity.getQualifiedName().toString().lastIndexOf('.');
        String packageName = entity.getQualifiedName().toString().substring(0, lastDot);
        writeToJavaFile(entityName, packageName, builder, processingEnv, entityRuntimeTypeResolverName);
    }
}
