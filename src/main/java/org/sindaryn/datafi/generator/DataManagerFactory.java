package org.sindaryn.datafi.generator;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import lombok.Data;
import lombok.NonNull;
import lombok.var;
import org.sindaryn.datafi.service.ArchivableDataManager;
import org.sindaryn.datafi.service.DataManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;

import static org.sindaryn.datafi.StaticUtils.camelCaseNameOf;
import static org.sindaryn.datafi.StaticUtils.writeToJavaFile;

@Data
public class DataManagerFactory {
    @NonNull
    private ProcessingEnvironment processingEnv;
    @NonNull
    private String basePackage;

    private TypeSpec.Builder dataManagersConfig = initDataManagerConfig();
    private final static ClassName dataManagerType = ClassName.get(DataManager.class);
    private final static ClassName archivableDataManagerType = ClassName.get(ArchivableDataManager.class);

    public void addDataManager(TypeElement entity){
        final ClassName entityType = ClassName.get(entity);
        var builder =
                MethodSpec
                .methodBuilder(camelCaseNameOf(entity) + "DataManager")
                .addAnnotation(Bean.class)
                .returns(ParameterizedTypeName.get(dataManagerType, entityType))
                .addStatement("return new $T($T.class)", dataManagerType, entityType);
        dataManagersConfig.addMethod(builder.build());
    }

    public void addArchivableDataManager(TypeElement entity){
        final ClassName entityType = ClassName.get(entity);
        var builder =
                MethodSpec
                        .methodBuilder(camelCaseNameOf(entity) + "ArchivableDataManager")
                        .addAnnotation(Bean.class)
                        .returns(ParameterizedTypeName.get(archivableDataManagerType, entityType))
                        .addStatement("return new $T($T.class)", archivableDataManagerType, entityType);
        dataManagersConfig.addMethod(builder.build());
    }

    private static TypeSpec.Builder initDataManagerConfig(){
        return TypeSpec.classBuilder("DataManagersConfig")
                .addAnnotation(Configuration.class)
                .addMethod(MethodSpec.methodBuilder("nullTypeDataManager")
                        .addAnnotation(Bean.class)
                        .addAnnotation(Primary.class)
                        .returns(dataManagerType)
                        .addStatement("return new $T()", dataManagerType)
                        .build())
                .addMethod(MethodSpec.methodBuilder("nullTypeArchivableDataManager")
                        .addAnnotation(Bean.class)
                        .addAnnotation(Primary.class)
                        .returns(archivableDataManagerType)
                        .addStatement("return new $T()", archivableDataManagerType)
                        .build());
    }
    public void writeToFile(){
        writeToJavaFile(
                "DataManagersConfig",
                basePackage,
                dataManagersConfig,
                processingEnv,
                "Data manager beans");
    }
}
