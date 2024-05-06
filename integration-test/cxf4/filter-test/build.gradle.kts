plugins {
    id("io.github.transitan.wsdl2java")
    id("com.github.bjornvester.wsdl2java.internal.java-conventions-cxf4")
}

wsdl2java {
    wsdlDir.set(layout.projectDirectory.dir("src/main/wsdl"))
    includes.set(
        listOf(
            "HelloWorldAbstractService.wsdl",
            "nested/HelloWorldNestedService.wsdl"
        )
    )
}
