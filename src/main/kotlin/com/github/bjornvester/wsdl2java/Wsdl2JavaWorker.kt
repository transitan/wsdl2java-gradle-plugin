package com.github.bjornvester.wsdl2java

import org.apache.cxf.tools.common.ToolContext
import org.apache.cxf.tools.wsdlto.WSDLToJava
import org.gradle.api.GradleException
import org.gradle.workers.WorkAction
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class Wsdl2JavaWorker : WorkAction<Wsdl2JavaWorkerParams> {
    private val logger: Logger = LoggerFactory.getLogger(Wsdl2JavaWorker::class.java)

    override fun execute() {
        parameters.wsdlToArgs.forEach { (wsdlPath, args) ->
            logger.info("Running WSDLToJava tool on file {} with args: {}", wsdlPath, args)

            try {
                WSDLToJava(args.toTypedArray()).run(ToolContext())
            } catch (e: Exception) {
                // We can't propagate the exception as it might contain classes from CXF which are not available outside the worker execution context
                // Also, for some reason, we can't even log the error as it sometimes fails with:
                // java.io.StreamCorruptedException: invalid type code: 0C
                // Seems like a bug in Gradle, possible when the error message contain multiple lines
                // Until we have found the cause of it, we print directly to System.out
                logger.error("Failed to generate sources from WSDL:")
                e.printStackTrace()
                throw GradleException("Failed to generate Java sources from WSDL. See the log for details.")
            }
        }

        fixGeneratedAnnotations()
    }

    private fun fixGeneratedAnnotations() {
        if (parameters.generatedStyle !=  Wsdl2JavaPluginExtension.GENERATED_STYLE_DEFAULT || parameters.removeDateFromGeneratedAnnotation) {

            parameters.outputDir.asFileTree.forEach {
                logger.debug("Fixing the @Generated annotation in file {}", it)
                var source = it.readText()

                when (parameters.generatedStyle) {
                    Wsdl2JavaPluginExtension.GENERATED_STYLE_JDK8 -> {
                        source = source.replaceFirst("import jakarta.annotation.Generated", "import javax.annotation.Generated")
                    }
                    Wsdl2JavaPluginExtension.GENERATED_STYLE_JDK9 -> {
                        source = source.replaceFirst("import jakarta.annotation.Generated", "import javax.annotation.processing.Generated")
                        source = source.replaceFirst("import javax.annotation.Generated", "import javax.annotation.processing.Generated")
                    }
                    Wsdl2JavaPluginExtension.GENERATED_STYLE_JAKARTA -> {
                        source = source.replaceFirst("import javax.annotation.Generated", "jakarta.annotation.Generated")
                    }
                }

                if (parameters.removeDateFromGeneratedAnnotation) {
                    // Remove the "date" part from the @Generated annotation
                    // Input example: @Generated(value = "org.apache.cxf.tools.wsdlto.WSDLToJava", date = "2021-05-15T21:18:42.272+02:00", comments = "Apache CXF 3.4.3")
                    // Note that the 'value' property may contain classes in the 'com.sun.tools.xjc' namespace
                    // Also note that the date and comments field may be switched, depending on the version.
                    val generatedPattern = """(@Generated\(value = .*?"), date = "[^"]*"([^)]*\))"""
                    source = source.replace(Regex(generatedPattern), "$1$2")
                }

                it.writeText(source)
            }
        }
        if (parameters.shouldUseLombok) {
            parameters.outputDir.asFileTree.forEach {
                print("Using Lombok in file : "+it.path);
                var source = it.readText()
                var identifiers =  arrayOf("public class","public abstract class","public static class");
                var identifier = "" ;
                var i = -1;
                for(x in identifiers) {
                     i = source.indexOf(x);
                    if(i!=-1) {
                        identifier = x; break;
                    }
                }

                if(i!=-1) {
                    var j = source.indexOf("{", i);
                    var className = source.substring(i + identifier.length, j);
                    var actualClassName = className.trim();
                    if(actualClassName.contains(" extends ") || actualClassName.contains(" implements"))
                        actualClassName = actualClassName.substring(0, actualClassName.indexOf(' '));

                    if (!(className.lowercase()
                            .contains("service") && (className.contains(" extends ") || className.contains(" implements ")))
                    ) {
                        var annotationWithoutConstructor = "@lombok.Getter\n@lombok.Setter\n@lombok.Builder\n" + identifier;
                        var annotationWithoutConstructorSuperBuilder = "@lombok.Getter\n@lombok.Setter\n@lombok.experimental.SuperBuilder\n" + identifier;
                        var annotationWithConstructorSuperBuilder = "@lombok.Getter\n@lombok.Setter\n@lombok.NoArgsConstructor\n@lombok.experimental.SuperBuilder\n" + identifier;

                        //   var annotationWithConstructor= "@lombok.Getter\n@lombok.Setter\n@lombok.experimental.SuperBuilder\n@lombok.AllArgsConstructor\n@lombok.NoArgsConstructor\npublic class";
                        var classHasConstructor = source.substring(j).contains(" " + actualClassName.trim() + "() {");
                        var replacement = if (classHasConstructor) annotationWithoutConstructorSuperBuilder else annotationWithConstructorSuperBuilder;

                        if (!className.contains(" extends ") && !className.contains(" implements"))
                            source = source.replaceFirst(identifier, replacement);
                        else if (className.contains(" extends ") && !className.contains(" extends Exception"))
                            source = source.replaceFirst(identifier, replacement);

                        logger.debug("check class string : " + className + " actual class name: " + actualClassName + " has constructor :" + classHasConstructor);
                    }
                }
                it.writeText(source);
            }
        }
    }
}
