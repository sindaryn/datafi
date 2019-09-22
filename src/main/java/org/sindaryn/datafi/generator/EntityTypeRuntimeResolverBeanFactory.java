package org.sindaryn.datafi.generator;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.var;
import org.sindaryn.datafi.service.EntityTypeRuntimeResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.util.HashMap;
import java.util.Map;
@Data
public class EntityTypeRuntimeResolverBeanFactory {
    private Map<TypeElement, MethodSpec> alreadyCreated = new HashMap<>();
    @NonNull
    private ProcessingEnvironment processingEnv;
    @Getter
    private TypeSpec.Builder configClassSpec =
            TypeSpec.classBuilder("EntityTypeRuntimeResolvers")
            .addAnnotation(Configuration.class)
            .addMethod(nullTypeResolver());

    private static MethodSpec nullTypeResolver() {
        return MethodSpec.methodBuilder("nullEntityTypeRuntimeResolver")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Bean.class)
                .returns(ParameterizedTypeName.get(ClassName.get(EntityTypeRuntimeResolver.class)))
                .addStatement("return null").build();
    }

    public void generateEntityTypeRuntimeResolverBean(TypeElement entity){
        if(alreadyCreated.get(entity) != null) return;
        String entityName = entity.getSimpleName().toString();
        ClassName entityClassname = ClassName.get(entity);
        String entityRuntimeTypeResolverName = entityName + "EntityTypeRuntimeResolver";
        var builder = MethodSpec.methodBuilder(entityRuntimeTypeResolverName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Bean.class)
                .returns(ParameterizedTypeName.get(ClassName.get(EntityTypeRuntimeResolver.class), entityClassname))
                .addStatement("return () -> $T.class", entityClassname);

        MethodSpec result = builder.build();
        alreadyCreated.put(entity, result);
        configClassSpec.addMethod(result);
        /*int lastDot = entity.getQualifiedName().toString().lastIndexOf('.');
        String packageName = entity.getQualifiedName().toString().substring(0, lastDot);
        writeToJavaFile(entityName, packageName, builder, processingEnv, entityRuntimeTypeResolverName);*/
    }
}
