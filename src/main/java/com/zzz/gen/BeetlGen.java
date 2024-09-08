package com.zzz.gen;

import cn.hutool.core.io.FileUtil;
import org.beetl.core.Configuration;
import org.beetl.core.GroupTemplate;
import org.beetl.core.Template;
import org.beetl.core.resource.ClasspathResourceLoader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

/**
 * @author zzz
 */
public class BeetlGen {

    public void gen(TemplateConfig templateConfig) {
        try {
            ClasspathResourceLoader resourceLoader = new ClasspathResourceLoader();
            Configuration cfg = null;

            cfg = Configuration.defaultConfiguration();
            GroupTemplate gt = new GroupTemplate(resourceLoader, cfg);
            Template t = gt.getTemplate(templateConfig.getTemplatePath());
            //绑定变量参数
            t.binding(templateConfig.getParameters());
            FileUtil.mkParentDirs(templateConfig.getFullOutPath());
            FileWriter fileWriter = new FileWriter(templateConfig.getFullOutPath());
            t.renderTo(fileWriter);
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("代码生成异常", e);
        }
    }
}
