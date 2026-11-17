package com.iotlight.generator.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

/**
 * 读取代码生成相关配置
 * 
 * @author ruoyi
 */
@Component
@ConfigurationProperties(prefix = "gen")
@PropertySource(value = { "classpath:generator.properties" })
public class GenConfig
{
    /** 作者 */
    public static String author;

    /** 生成包路径 */
    public static String packageName;

    /** 自动去除表前缀 */
    public static boolean autoRemovePre;

    /** 表前缀 */
    public static String tablePrefix;

    /** 是否允许生成文件覆盖到本地（自定义路径） */
    public static boolean allowOverwrite;

    public static String getAuthor()
    {
        return author;
    }

    @Value("${gen.author}")
    public void setAuthor(String author)
    {
        GenConfig.author = author;
    }

    public static String getPackageName()
    {
        return packageName;
    }

    @Value("${gen.packageName}")
    public void setPackageName(String packageName)
    {
        GenConfig.packageName = packageName;
    }

    public static boolean getAutoRemovePre()
    {
        return autoRemovePre;
    }

    @Value("${gen.autoRemovePre}")
    public void setAutoRemovePre(boolean autoRemovePre)
    {
        GenConfig.autoRemovePre = autoRemovePre;
    }

    public static String getTablePrefix()
    {
        return tablePrefix;
    }

    @Value("${gen.tablePrefix}")
    public void setTablePrefix(String tablePrefix)
    {
        GenConfig.tablePrefix = tablePrefix;
    }

    public static boolean isAllowOverwrite()
    {
        return allowOverwrite;
    }

    @Value("${gen.allowOverwrite}")
    public void setAllowOverwrite(boolean allowOverwrite)
    {
        GenConfig.allowOverwrite = allowOverwrite;
    }
}
