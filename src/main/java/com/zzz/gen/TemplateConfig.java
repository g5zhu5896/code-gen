package com.zzz.gen;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * @author zzz
 */
@Data
@Builder
public class TemplateConfig {
    /**
     * 模板路径
     * 相对resource的路径
     */
    private String templatePath;
    /**
     * 生成文件的输出目录
     */
    private String outPath;
    /**
     * 生成文件的名称
     */
    private String fileName;

    /**
     * 模板文件中用到的参数变量
     */
    private Map<String, Object> parameters;

    public String getFullOutPath() {
        return outPath + "/" + fileName;
    }
}
