package org.sindaryn.datafi.generator;


import com.google.auto.service.AutoService;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.sindaryn.datafi.StaticUtils.*;
import static org.sindaryn.datafi.generator.CustomResolversFactory.resolveCustomResolvers;

/**
 * Takes care of generating all the source files needed for a jpa data access layer.
 */
@SuppressWarnings("unchecked")
@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class DataLayerAnnotationsProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
        Set<? extends TypeElement> entities = getPersistableEntities(roundEnvironment);
        if(entities.isEmpty()) return false;
        Map<TypeElement, List<VariableElement>> annotatedFieldsMap = new HashMap<>();
        Map<TypeElement, List<MethodSpec>> customResolversMap = new HashMap<>();
        resolveCustomResolvers(entities, annotatedFieldsMap, customResolversMap);
        FuzzySearchMethodsFactory fuzzySearchMethodsFactory = new FuzzySearchMethodsFactory(processingEnv);
        Map<TypeElement, MethodSpec> fuzzySearchMethodsMap =
                fuzzySearchMethodsFactory.resolveFuzzySearchMethods(entities);
        //generate a custom jpa repository for each entity
        DaoFactory daoFactory = new DaoFactory(processingEnv);
        DataManagerFactory dataManagerFactory = new DataManagerFactory(processingEnv, getBasePackage(roundEnvironment));
        entities.forEach(entity -> {
            daoFactory.generateDao(entity, annotatedFieldsMap, customResolversMap, fuzzySearchMethodsMap);
            dataManagerFactory.addDataManager(entity);
            if(isArchivable(entity, processingEnv))
                dataManagerFactory.addArchivableDataManager(entity);
            dataManagerFactory.addBasePackageResolver();
        });
        dataManagerFactory.writeToFile();
        /*
        create a configuration source file such that
        our spring beans are included within
        the target application context
        */
        setComponentScan(entities);
        //return false - these annotations are needed for the web-service layer as well
        return false;

    }

    /**
     * compile and return a list of all entities annotated
     * with @Entity or @Table, and as such
     * relevant to the code generator
     * @param roundEnvironment
     * @return
     */
    private Set<? extends TypeElement> getPersistableEntities(RoundEnvironment roundEnvironment) {
        return getEntitiesSet(roundEnvironment);
    }

    private void setComponentScan(Set<? extends TypeElement> entities) {
        if(!entities.isEmpty()){
            String className = entities.iterator().next().getQualifiedName().toString();
            int lastdot = className.lastIndexOf('.');
            String basePackageName = className.substring(0, lastdot);
            String simpleClassName = "SindarynClasspathConfiguration";
            TypeSpec.Builder builder = TypeSpec.classBuilder(simpleClassName)
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Configuration.class)
                    .addAnnotation(AnnotationSpec.builder(ComponentScan.class)
                            .addMember(
                                    "basePackages",
                                    "{$S}",
                                    "org.sindaryn")
                            .build());
            writeToJavaFile(simpleClassName, basePackageName, builder, processingEnv, "configuration source file");
        }
    }
}
