package com.zzz.complier;

import com.sun.tools.javac.api.JavacTool;
import com.zzz.utils.Utils;
import org.codehaus.plexus.compiler.Compiler;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.CompilerException;
import org.codehaus.plexus.compiler.CompilerMessage;
import org.codehaus.plexus.compiler.javac.InProcessCompiler;
import org.codehaus.plexus.compiler.javac.JavacCompiler;
import org.codehaus.plexus.compiler.javac.JavaxToolsCompiler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.util.List;

/**
 * 用 Plexus 进行编译
 *
 * @author zzz
 */
public class PlexusCompiler {

    /**
     * @param sourceFolderPath   源代码文件夹路径
     * @param outputFolderPath   目标文件夹路径（编译后的 .class 文件将存放在这里）
     * @param dependencyJarPaths 依赖的相关jar的路径,可以是文件夹也可以是jar文件
     */
    public static boolean compiler(String sourceFolderPath, String outputFolderPath, String... dependencyJarPaths) {
        try {
            // 创建一个编译器管理器实例
            Compiler compiler = new JavacCompiler() {
                @Override
                protected InProcessCompiler inProcessCompiler() {
                    return new JavaxToolsCompiler() {
                        @Override
                        protected JavaCompiler newJavaCompiler() {
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
                            return compiler;
                        }
                    };
                }
            };

            // 设置编译器配置
            CompilerConfiguration config = new CompilerConfiguration();
            config.setSourceVersion("1.8");
            config.setTargetVersion("1.8");

            // 设置源文件路径和输出目录
            config.addSourceLocation(sourceFolderPath);
            config.setOutputLocation(outputFolderPath);
            //遍历所有依赖路径的 jar包
            List<String> dependencyJarPathList = Utils.getAllJars(dependencyJarPaths);
            for (
                    String s : dependencyJarPathList) {
                config.addClasspathEntry(s);
            }

            // 执行编译
            List<CompilerMessage> messages = null;

            messages = compiler.performCompile(config).

                    getCompilerMessages();

            // 输出编译信息
            for (
                    CompilerMessage message : messages) {
                System.out.println(message.toString());
            }
            return true;
        } catch (CompilerException e) {
            throw new IllegalArgumentException("编译错误", e);
        }
    }
}
