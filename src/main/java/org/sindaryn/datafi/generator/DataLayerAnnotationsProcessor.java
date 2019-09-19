package org.sindaryn.datafi.generator;


import com.google.auto.service.AutoService;
import com.google.common.collect.Sets;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import lombok.var;
import org.sindaryn.datafi.annotations.GetAllBy;
import org.sindaryn.datafi.annotations.GetBy;
import org.sindaryn.datafi.annotations.GetByUnique;
import org.sindaryn.datafi.annotations.WithResolver;
import org.sindaryn.datafi.persistence.GenericDao;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Repository;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.persistence.*;
import javax.tools.Diagnostic;
import java.util.*;

import static com.squareup.javapoet.ParameterizedTypeName.get;
import static org.sindaryn.datafi.StaticUtils.*;
import static org.sindaryn.datafi.generator.SqlQueryMethodParser.parseResolver;

/**
 * Takes care of generating all the source files needed for a jpa data access layer.
 */
@SuppressWarnings("unchecked")
@SupportedAnnotationTypes({/*"org.sindaryn.*/"*"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class DataLayerAnnotationsProcessor extends AbstractProcessor {
    private EntityTypeRuntimeResolverBeanFactory runtimeResolverBeanFactory;
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
        //fetch all entities annotated with @PersistableEntity
        Set<? extends TypeElement> entities = getPersistableEntities(annotations, roundEnvironment);
        Map<TypeElement, List<VariableElement>> annotatedFieldsMap = new HashMap<>();
        Map<TypeElement, List<MethodSpec>> customResolversMap = new HashMap<>();
        resolveCustomResolvers(entities, annotatedFieldsMap, customResolversMap);
        FuzzySearchMethodsFactory fuzzySearchMethodsFactory = new FuzzySearchMethodsFactory(processingEnv);
        Map<TypeElement, MethodSpec> fuzzySearchMethodsMap =
                fuzzySearchMethodsFactory.resolveFuzzySearchMethods(entities);
        runtimeResolverBeanFactory = new EntityTypeRuntimeResolverBeanFactory(processingEnv);
        //generate a custom jpa repository for each entity
        entities.forEach(entity -> generateDao(entity, annotatedFieldsMap, customResolversMap, fuzzySearchMethodsMap));
        /*
        create a configuration source file such that
        our spring beans are included within
        the target application context
        */
        setComponentScan(entities);
        //return false - these annotations are needed for the web-service layer as well
        return false;
    }

    public static void resolveCustomResolvers(Set<? extends TypeElement> entities, Map<TypeElement, List<VariableElement>> annotatedFieldsMap, Map<TypeElement, List<MethodSpec>> customResolversMap) {
        for (TypeElement entity : entities) {
            //check for @GetBy and / or @GetAllBy field level annotations
            List<VariableElement> annotatedFields = getAnnotatedFieldsOf(entity);
            WithResolver[] customResolvers = getResolvers(entity);
            if(customResolvers != null){
                List<MethodSpec> customResolversImpl = new ArrayList<>();
                for (int i = 0; i < customResolvers.length; i++) {
                    customResolversImpl.add(parseResolver(customResolvers[i], entity));
                }
                customResolversMap.put(entity, customResolversImpl);
            }
            //as such, store a map of which entities need which extra endpoints
            if (!annotatedFields.isEmpty()) annotatedFieldsMap.put(entity, annotatedFields);
        }
    }

    private static WithResolver[] getResolvers(TypeElement entity) {
        final WithResolver[] annotationsByType = entity.getAnnotationsByType(WithResolver.class);
        if(annotationsByType == null) return null;
        List<WithResolver> resolvers = Arrays.asList(annotationsByType);
        for(AnnotationMirror annotationType : entity.getAnnotationMirrors()){
            if(annotationType.getAnnotationType().getAnnotation(WithResolver.class) != null){
                resolvers.add(annotationType.getAnnotationType().getAnnotation(WithResolver.class));
            }
        }
        return (WithResolver[]) resolvers.toArray();
    }


    /**
     * generate the actual '<entity name>Dao.java' jpa repository for a given entity
     * @param entity - the given data model entity / table
     * @param annotatedFieldsMap - a reference telling us whether this repository needs any custom
     * @param customResolversMap
     */
    private void generateDao(
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
        runtimeResolverBeanFactory.generateEntityTypeRuntimeResolverBean(entity);
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

    /**
     * In order to generate a JpaRepository<T, ID>, we need the ID id type a given entity
     * @param entity
     * @return
     */
    public static ClassName getIdType(TypeElement entity, ProcessingEnvironment processingEnv) {
        for(Element field : entity.getEnclosedElements()){
            if(field.getKind() == ElementKind.FIELD &&
                    (
                        field.getAnnotation(Id.class) != null || field.getAnnotation(EmbeddedId.class) != null
                    )){
                return (ClassName) ClassName.get(field.asType());
            }
        }
        processingEnv
                .getMessager()
                .printMessage(Diagnostic.Kind.ERROR,
                        "No id type found for entity " + entity.getSimpleName().toString(), entity);
        return null;
    }

    /**
     * Compile and return a list of all fields within a given entity
     * which are annotated with '@GetBy' or '@GetAllBy'
     * @param entity
     * @return
     */
    public static List<VariableElement> getAnnotatedFieldsOf(TypeElement entity) {
        List<VariableElement> annotatedFields = new ArrayList<>();
        List<? extends Element> enclosedElements = entity.getEnclosedElements();
        for (Element enclosedElement : enclosedElements)
            if (isAnnotatedField(enclosedElement)) annotatedFields.add((VariableElement) enclosedElement);
        return annotatedFields;
    }

    /**
     * checks if a given field is annotated with '@GetBy' or '@GetAllBy'
     * @param enclosedElement
     * @return
     */
    private static boolean isAnnotatedField(Element enclosedElement) {
        return enclosedElement.getKind() == ElementKind.FIELD &&
                (enclosedElement.getAnnotation(GetBy.class) != null ||
                enclosedElement.getAnnotation(GetAllBy.class) != null) ||
                enclosedElement.getAnnotation(GetByUnique.class) != null;
    }

    /**
     * compile and return a list of all entities annotated
     * with @Entity or @Table, and as such
     * relevant to the code generator
     * @param roundEnvironment
     * @return
     */
    private Set<? extends TypeElement> getPersistableEntities(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
        Set<TypeElement> entities = new HashSet<>();
        entities.addAll((Collection<? extends TypeElement>) roundEnvironment.getElementsAnnotatedWith(Entity.class));
        entities.addAll((Collection<? extends TypeElement>) roundEnvironment.getElementsAnnotatedWith(Table.class));
        return Sets.newHashSet(entities);
    }

    private Collection<? extends TypeElement> getAnnotatedElements(RoundEnvironment roundEnvironment, TypeElement annotationType) {
        var result = (Collection<? extends TypeElement>) roundEnvironment.getElementsAnnotatedWith(annotationType);
        result.removeIf(element -> element.getKind().equals(ElementKind.ANNOTATION_TYPE));
        return result;
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
/*
    entity.getEnclosedElements().forEach(item -> {
            if(item.getKind().isField()){
                if(item.getSimpleName().toString().equals("teacherDummy")){
                    String typeName = item.asType().toString();
                    int start = typeName.lastIndexOf(".") + 1;
                    int end = typeName.lastIndexOf(">");
                    typeName = typeName.substring(start, end);
                    int i = 9;
                }
            }
        });
* */
