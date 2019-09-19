package org.sindaryn.datafi.generator;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.var;
import org.sindaryn.datafi.service.EntityTypeRuntimeResolver;
import org.springframework.stereotype.Service;

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
        var builder = TypeSpec.classBuilder(entityName + "EntityTypeRuntimeResolver")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Service.class)
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
        writeToJavaFile(entityName, packageName, builder, processingEnv, entityName + "EntityTypeRuntimeResolver");
    }
}
