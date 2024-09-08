package com.zzz.complier;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.google.common.collect.Lists;
import com.sun.tools.javac.api.JavacTool;
import com.zzz.utils.Utils;
import sun.awt.OSInfo;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.util.List;

/**
 * 用jdk自带的工具编译
 *
 * @author zzz
 */
public class JdkCompiler {
    private static final String SEPARATOR = OSInfo.getOSType().equals(OSInfo.OSType.WINDOWS) ? ";" : ":";

    /**
     * @param sourceFolderPath   源代码文件夹路径
     * @param outputFolderPath   目标文件夹路径（编译后的 .class 文件将存放在这里）
     * @param dependencyJarPaths 依赖的相关jar的路径,可以是文件夹也可以是jar文件
     */
    public static boolean compiler(String sourceFolderPath, String outputFolderPath, String... dependencyJarPaths) {
        // 创建一个 Java 编译器实例
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            try {
                //在有些环境如docker 容器内，可能在classpath下不包括tools.jar，就会导致获取不到编译器。所以不存在时需要自己new
//                compiler = new JavacTool();
                compiler = JavacTool.class.asSubclass(JavaCompiler.class).newInstance();
            } catch (Exception e) {
                throw new IllegalArgumentException("创建编译器失败");
            }
        }
        if (compiler == null) {
            throw new IllegalArgumentException("找不到编译器");
        }
        FileUtil.mkdir(outputFolderPath);
        // 获取一个标准文件管理器
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);) {

            // 从源文件夹中收集所有的 .java 文件
//            File sourceFolder = new File(sourceFolderPath);
//            File[] javaFiles = sourceFolder.listFiles((dir, name) -> name.endsWith(".java"));
            File[] javaFiles = FileUtil.loopFiles(sourceFolderPath).stream().filter(file -> {
                return file.getName().endsWith(".java");
            }).toArray(File[]::new);
            if (javaFiles == null || javaFiles.length == 0) {
                System.out.println("No Java files found in the source folder.");
                return true;
            }

            // 创建一个 Iterable 用于编译任务
            Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjects(javaFiles);

            // 设置编译选项，例如输出目录
            List<String> optionList = Lists.newArrayList("-d", outputFolderPath);
            //输出编译的详细信息
//            optionList.add("-verbose");

            //遍历所有依赖路径的 jar包
            List<String> dependencyJarPathList = Utils.getAllJars(dependencyJarPaths);
            if (!dependencyJarPathList.isEmpty()) {
                optionList.add("-cp");
                optionList.add(StrUtil.join(SEPARATOR, dependencyJarPathList));
            }

            // 创建编译任务
            JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, null, optionList, null, compilationUnits);

            return task.call();
        } catch (Exception e) {
            throw new IllegalArgumentException("编译错误", e);
        }
    }
}
