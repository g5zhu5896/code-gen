package com.zzz.utils;

import cn.hutool.core.io.FileUtil;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * @author zzz
 */
public class Utils {
    /**
     * @param basePaths 获取所有路径下的jar包，路径本身可以是jar文件
     * @return
     */
    public static List<String> getAllJars(String... basePaths) {
        List<String> dependencyJarPathList = Lists.newArrayList();
        for (String path : basePaths) {
            if (FileUtil.isDirectory(path)) {
                FileUtil.loopFiles(path).stream().filter(f -> {
                    if (f.getName().endsWith("-javadoc.jar") || f.getName().endsWith("-sources.jar")) {
                        System.out.println("警告：" + f.getAbsolutePath() + "怀疑是maven 仓库里的javadoc和source 的jar包");
                        return false;
                    }
                    return f.getName().endsWith(".jar");
                }).map(f -> {
                    return f.getAbsolutePath();
                }).forEach(dependencyJarPathList::add);
            } else if (path.endsWith(".jar")) {
                dependencyJarPathList.add(path);
            } else {
                System.out.println("警告：" + path + "路径不可用，不是jar包也不是文件夹，无法作为依赖");
            }
        }
        return dependencyJarPathList;
    }
}
