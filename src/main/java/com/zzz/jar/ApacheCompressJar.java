package com.zzz.jar;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.google.common.collect.Lists;
import com.zzz.utils.Utils;
import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveInputStream;
import org.apache.commons.compress.archivers.jar.JarArchiveOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * @author zzz
 */
public class ApacheCompressJar {
    /**
     * @param codePath 需要打包的class 路径
     * @param outPath  生成的jar路径，必须包含jar包文件名
     */
    public static void jar(String codePath, String outPath) {
        File jarFile = new File(outPath);
        try (JarArchiveOutputStream jarOutput = new JarArchiveOutputStream(new FileOutputStream(jarFile))) {
            //添加MANIFEST.MF
            JarArchiveEntry entry = new JarArchiveEntry("META-INF/MANIFEST.MF");
            jarOutput.putArchiveEntry(entry);
            Manifest manifest = new Manifest();
            Attributes mainAttributes = manifest.getMainAttributes();
            mainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
            manifest.write(jarOutput);
            jarOutput.closeArchiveEntry();

            //添加项目编译后的代码
            File codeFile = new File(codePath);
            for (File file : FileUtil.loopFiles(codeFile)) {
                if (file.isFile()) {
                    String entryName = StrUtil.removePrefix(StrUtil.removePrefix(file.getAbsolutePath(), codeFile.getAbsolutePath()), File.separator);
                    addFile(jarOutput, file, entryName);
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("打包异常", e);
        }
    }

    /**
     * * 打包包含依赖的jar包
     *
     * @param codePath           需要打包的class code 路径
     * @param outPath            生成的jar路径，必须包含jar包文件名
     * @param dependencyJarPaths 依赖的相关jar的路径,可以是文件夹也可以是jar文件
     */
    public static void jar(String codePath, String outPath, String... dependencyJarPaths) {
        File jarFile = new File(outPath);
        try (JarArchiveOutputStream jarOutput = new JarArchiveOutputStream(new FileOutputStream(jarFile))) {
            //添加依赖
            List<String> allJars = Utils.getAllJars(dependencyJarPaths);
            //将依赖的jar包放到 生成的jar包的lib目录里
            for (String allJar : allJars) {
                File file = new File(allJar);
                String classPath = "lib/" + file.getName();
                addFile(jarOutput, file, classPath);
            }

            Manifest manifest = new Manifest();
            Attributes mainAttributes = manifest.getMainAttributes();
            mainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
            //添加MANIFEST.MF
            JarArchiveEntry entry = new JarArchiveEntry("META-INF/MANIFEST.MF");
            jarOutput.putArchiveEntry(entry);
            manifest.write(jarOutput);
            jarOutput.closeArchiveEntry();

            //添加项目编译后的代码
            File codeFile = new File(codePath);
            for (File file : FileUtil.loopFiles(codeFile)) {
                if (file.isFile()) {
                    String entryName = StrUtil.removePrefix(StrUtil.removePrefix(file.getAbsolutePath(), codeFile.getAbsolutePath()), File.separator);
                    addFile(jarOutput, file, entryName);
                }
            }

        } catch (
                Exception e) {
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
        File jarFile = new File(outPath);
        try (JarArchiveOutputStream jarOutput = new JarArchiveOutputStream(new FileOutputStream(jarFile))) {
            Manifest manifest = new Manifest();
            Attributes mainAttributes = manifest.getMainAttributes();

            //添加依赖
            List<String> allJars = Utils.getAllJars(dependencyJarPaths);
            if (isNestedDependency) {
                for (String allJar : allJars) {
                    //遍历jar包内的文件
                    JarArchiveInputStream jarInput = new JarArchiveInputStream(FileUtil.getInputStream(allJar));
                    JarArchiveEntry entry = null;
                    while ((entry = jarInput.getNextEntry()) != null) {
                        if (!jarInput.canReadEntryData(entry)) {
                            // log something?
                            continue;
                        }

                        if (!entry.isDirectory()) {
                            if (entry.getName().endsWith("MANIFEST.MF")) {
                                //将MANIFEST.MF内容放到jar包的MANIFEST.MF里
                                Manifest inputManifest = new Manifest();
                                inputManifest.read(jarInput);
                                mainAttributes.putAll(inputManifest.getMainAttributes());
                            } else {
                                //将jar包中的文件加入 生成的jar包
                                addInputStream(jarOutput, jarInput, entry.getName());
                            }
                        }
//                        JarArchiveEntry entry = jarInput.getNextJarEntry();
//                        byte[] content = new byte[entry.getSize()];
//                        LOOP UNTIL entry.getSize() HAS BEEN READ {
//                            jarInput.read(content, offset, content.length - offset);
                    }
                }
            } else {
                //将依赖的jar添加到到manifest中的 class-path,如果是用这种方式，得把相关依赖jar放到最后生成的jar包的同步录的lib目录下
                List<String> classPaths = Lists.newArrayList();
                for (String allJar : allJars) {
                    File file = new File(allJar);
                    String classPath = "lib/" + file.getName();
//                    addFile(jarOutput, file, classPath);  //将依赖的jar包放到 生成的jar包的lib目录里
                    classPaths.add(classPath);
                    //将依赖拷到最后生成的jar包目录里的lib目录里
                    FileUtil.copy(allJar, jarFile.getParent() + "/" + classPath, true);
                }
                mainAttributes.put(Attributes.Name.CLASS_PATH, StrUtil.join(" ", classPaths));
            }
            mainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
            mainAttributes.put(Attributes.Name.MAIN_CLASS, mainClass);

            //添加MANIFEST.MF
            JarArchiveEntry entry = new JarArchiveEntry("META-INF/MANIFEST.MF");
            jarOutput.putArchiveEntry(entry);
            manifest.write(jarOutput);
            jarOutput.closeArchiveEntry();

            //添加项目编译后的代码
            File codeFile = new File(codePath);
            for (File file : FileUtil.loopFiles(codeFile)) {
                if (file.isFile()) {
                    String entryName = StrUtil.removePrefix(StrUtil.removePrefix(file.getAbsolutePath(), codeFile.getAbsolutePath()), File.separator);
                    addFile(jarOutput, file, entryName);
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("打包异常", e);
        }

    }

    /**
     * 将文件添加到生成的jar包中
     *
     * @param jarOutput
     * @param file
     * @param entryName
     * @throws IOException
     */
    private static void addFile(JarArchiveOutputStream jarOutput, File file, String entryName) throws IOException {
        JarArchiveEntry entry = new JarArchiveEntry(entryName);
        jarOutput.putArchiveEntry(entry);
        FileUtils.copyFile(file, jarOutput);
        jarOutput.closeArchiveEntry();
    }

    /**
     * 将文件流添加到生成的jar包中
     *
     * @param jarOutput
     * @param inputStream
     * @param entryName
     * @throws IOException
     */
    private static void addInputStream(JarArchiveOutputStream jarOutput, InputStream inputStream, String entryName) throws IOException {
        JarArchiveEntry entry = new JarArchiveEntry(entryName);
        jarOutput.putArchiveEntry(entry);
        IOUtils.copy(inputStream, jarOutput);
        jarOutput.closeArchiveEntry();
    }
}
