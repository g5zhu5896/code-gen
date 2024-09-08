package com.zzz.jar;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.google.common.collect.Lists;
import com.zzz.utils.Utils;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.archiver.ArchivedFileSet;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.Manifest;
import org.codehaus.plexus.archiver.util.DefaultArchivedFileSet;
import org.codehaus.plexus.context.DefaultContext;

import java.io.File;
import java.util.List;

/**
 * @author zzz
 */
public class PlexusJar {
    /**
     * @param codePath 需要打包的class 路径
     * @param outPath  生成的jar路径，必须包含jar包文件名
     */
    public static void jar(String codePath, String outPath) {
        try {
            File jarFile = new File(outPath);
            JarArchiver archiver = new JarArchiver();
            archiver.setDestFile(jarFile);

            //添加项目编译后的代码
            archiver.addDirectory(new File(codePath));

            archiver.createArchive();
        } catch (Exception e) {
            throw new IllegalArgumentException("打包异常", e);
        }
    }

    /**
     * 打包包含依赖的jar包
     *
     * @param codePath           需要打包的class code 路径
     * @param outPath            生成的jar路径，必须包含jar包文件名
     * @param dependencyJarPaths 依赖的相关jar的路径,可以是文件夹也可以是jar文件
     */
    public static void jar(String codePath, String outPath, String... dependencyJarPaths) {
        try {
            File jarFile = new File(outPath);
            JarArchiver archiver = new JarArchiver();
            archiver.setDestFile(jarFile);

            //添加依赖
            List<String> allJars = Utils.getAllJars(dependencyJarPaths);

            //将依赖的jar包放到 生成的jar包的lib目录里
            for (String allJar : allJars) {
                File file = new File(allJar);
                String classPath = "lib/" + file.getName();
                archiver.addFile(file, classPath);
            }

            //添加项目编译后的代码
            archiver.addDirectory(new File(codePath));

            archiver.createArchive();
        } catch (Exception e) {
            throw new IllegalArgumentException("打包异常", e);
        }
    }

    /**
     * 打包成可执行jar包
     *
     * @param codePath           需要打包的class code 路径
     * @param outPath            生成的jar路径，必须包含jar包文件名
     * @param mainClass          main方法启动类所在路径
     * @param isNestedDependency true会把依赖的jar包里的文件放到最后生成的jar包里，
     *                           false则会配置manifest的class-path，然后把依赖的jar包放到生成jar包的同目录下的lib里
     * @param dependencyJarPaths 依赖的相关jar的路径,可以是文件夹也可以是jar文件
     */
    public static void jar(String codePath, String outPath, String mainClass, boolean isNestedDependency, String... dependencyJarPaths) {
        try {
            File jarFile = new File(outPath);
            JarArchiver archiver = new JarArchiver();
            archiver.setDestFile(jarFile);

            //添加依赖
            List<String> allJars = Utils.getAllJars(dependencyJarPaths);

            if (isNestedDependency) {
                DefaultContext defaultContext = new DefaultContext();
                defaultContext.put(PlexusConstants.PLEXUS_KEY, new DefaultPlexusContainer());
                archiver.contextualize(defaultContext);
                for (String allJar : allJars) {
                    File file = new File(allJar);
                    //将依赖jar包中的文件 放入当前要打包的jar中
                    ArchivedFileSet archivedFileSet = new DefaultArchivedFileSet(file);
                    archiver.addArchivedFileSet(archivedFileSet);
                }
            }

            //添加main路径
            Manifest manifest = new Manifest();
            Manifest.Attribute manAttr = new Manifest.Attribute("Main-Class", mainClass);
            manifest.addConfiguredAttribute(manAttr);
            archiver.addConfiguredManifest(manifest);

            if (!isNestedDependency) {
                //将依赖的jar添加到到manifest中的 class-path,如果是用这种方式，得把相关依赖jar放到最后生成的jar包的同步录的lib目录下
                List<String> classPaths = Lists.newArrayList();
                for (String allJar : allJars) {
                    File file = new File(allJar);
                    String classPath = "lib/" + file.getName();
                    classPaths.add(classPath);
                    //将依赖拷到最后生成的jar包目录里的lib目录里
                    FileUtil.copy(allJar, jarFile.getParent() + "/" + classPath, true);
                }
                Manifest.Attribute classPathAttr = new Manifest.Attribute("Class-Path", StrUtil.join(" ", classPaths));
                manifest.addConfiguredAttribute(classPathAttr);
            }

            //添加项目编译后的代码
            archiver.addDirectory(new File(codePath));

            archiver.createArchive();
        } catch (Exception e) {
            throw new IllegalArgumentException("打包异常", e);
        }
    }
}
