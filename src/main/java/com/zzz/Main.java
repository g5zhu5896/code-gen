package com.zzz;

import cn.hutool.core.io.FileUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.zzz.complier.JdkCompiler;
import com.zzz.gen.BeetlGen;
import com.zzz.gen.TemplateConfig;
import com.zzz.jar.ApacheCompressJar;

import java.util.List;
import java.util.Map;

/**
 * @author zzz
 */
public class Main {
    public static void main(String[] args) {
        String basePath = System.getProperty("user.dir");
        String projectName = "test-project";
        String outPath = basePath + "/out";
        String projectPath = outPath + "/" + projectName;
        //获取java代码的模板配置
        TemplateConfig javaCodeConfig = buildJavaCode("com.zzz", "Test", projectPath + "/src/main/java/com/zzz");
        //获取pom.xml的模板配置
        TemplateConfig pomConfig = buildPom(projectPath, projectName, "pom.xml");
        //代码项目生成
        BeetlGen beetlGen = new BeetlGen();
        beetlGen.gen(javaCodeConfig);
        beetlGen.gen(pomConfig);

        //将maven依赖库拷到项目里
        FileUtil.copy(basePath + "/repo", projectPath, true);

        //-- 到这一步上面的为一个maven项目的代码，可以直接提供别人进行开发。下面是会对上面生成的代码进行编译打包成jar，以jar把形式提供给别人开发 --
        String dependencyPath = projectPath + "//repo";
        //代码编译
        String compilerPath = outPath + "/target/classes";
        JdkCompiler.compiler(projectPath, compilerPath, dependencyPath);
//        PlexusCompiler.compiler(projectPath, compilerPath, dependencyPath);

        //代码打包
        String jarPath = basePath + "/out/target/" + projectName + ".jar";
        //打成jar包，此jar包不可执行
//        PlexusJar.jar(compilerPath, jarPath);
        ApacheCompressJar.jar(compilerPath, jarPath);

        //打成jar包，此jar包不可执行,但在jar包中会包含相关依赖
//        PlexusJar.jar(compilerPath, jarPath, dependencyPath);
//        ApacheCompressJar.jar(compilerPath, jarPath, dependencyPath);

        //打成可执行jar包, java -jar ${jarPath} 运行jar包可以执行指定的main方法
//        PlexusJar.jar(compilerPath, jarPath, "com.zzz.Test", true, dependencyPath);
//        ApacheCompressJar.jar(compilerPath, jarPath, "com.zzz.Test", true, dependencyPath);
//        PlexusJar.jar(compilerPath, jarPath, "com.zzz.Test", false, dependencyPath);
//        ApacheCompressJar.jar(compilerPath, jarPath, "com.zzz.Test", false, dependencyPath);
    }

    private static TemplateConfig buildJavaCode(String packageName, String className, String outPath) {
        Map<String, Object> params = Maps.newHashMap();
        params.put("packageName", packageName);
        params.put("className", className);
        List<Map<String, Object>> fields = Lists.newArrayList();
        params.put("fields", fields);
        Map<String, Object> field1 = Maps.newHashMap();
        field1.put("type", "Long");
        field1.put("name", "id");
        fields.add(field1);
        Map<String, Object> field2 = Maps.newHashMap();
        field2.put("type", "String");
        field2.put("name", "name");
        fields.add(field2);
        return TemplateConfig.builder()
                .templatePath("template/JavaCode.btl")
                .outPath(outPath)
                .fileName(className + ".java")
                .parameters(params)
                .build();
    }


    static TemplateConfig buildPom(String outPath, String projectName, String fileName) {
        Map<String, Object> params = Maps.newHashMap();
        params.put("groupId", "com.zzz");
        params.put("artifactId", projectName);
        params.put("version", "1.0");
        return TemplateConfig.builder()
                .templatePath("template/pom.btl")
                .outPath(outPath)
                .fileName(fileName)
                .parameters(params)
                .build();
    }
}
