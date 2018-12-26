/**
 * MyBatisGenerator
 * @title ConfigUtils.java
 * @package com.chn.mybatis.gen.utils
 * @author lzxz1234<lzxz1234@gmail.com>
 * @date 2014年11月6日-下午5:31:19
 * @version V1.0
 * Copyright (c) 2014 ChineseAll.com All Right Reserved
 */
package com.chn.mybatis.gen.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.io.IOUtils;

/**
 * @class ConfigUtils
 * @author lzxz1234
 * @description 
 * @version v1.0
 */
public class ConfigUtils {
	private static Cfg cfg;
	
	public ConfigUtils(){
		
	}
	
	public static final Cfg getAlredy() {
		if(cfg!=null) {
    		return cfg;
    	}
		return null;
	}

    public static final Cfg getCfg(final String resourceName) {
    	
        cfg=new Cfg() {
            private Properties prop = new Properties();
            {
                InputStream is = null;
                try {
                    is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName);
                    if(is==null) {
                    	is=new FileInputStream(new File(resourceName));
                    }
                    prop.load(is);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    IOUtils.closeQuietly(is);
                }
            }
            @Override
            public String get(String key) {
                return prop.getProperty(key);
            }
        };
        return cfg;
    }
    
    public static abstract class Cfg {
        
        public abstract String get(String key);
        
    }
    
}
