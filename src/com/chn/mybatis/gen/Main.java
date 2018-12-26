/**
 * MyBatisGenerator
 * @title Main.java
 * @package com.chn.mybatis.gen
 * @author lzxz1234<lzxz1234@gmail.com>
 * @date 2014年11月6日-下午6:42:09
 * @version V1.0
 * Copyright (c) 2014 ChineseAll.com All Right Reserved
 */
package com.chn.mybatis.gen;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

import org.apache.commons.io.FileUtils;
import org.beetl.core.Configuration;
import org.beetl.core.GroupTemplate;
import org.beetl.core.Template;
import org.beetl.core.resource.ClasspathResourceLoader;

import com.chn.mybatis.gen.def.TableMetadata;
import com.chn.mybatis.gen.trans.TableTrans;
import com.chn.mybatis.gen.utils.ConfigUtils;
import com.chn.mybatis.gen.utils.DBUtils;

/**
 * @class Main
 * @author lzxz1234
 * @description 
 * @version v1.0
 */
public class Main {
    
	
    public static String ROOT_FILE_PATH = Main.class.getProtectionDomain().getCodeSource().getLocation().getPath().replace("%20", " ");
    static {
    	if(ROOT_FILE_PATH.contains("jar")) {
    		System.out.println(ROOT_FILE_PATH);
    		ROOT_FILE_PATH=ROOT_FILE_PATH.substring(0,ROOT_FILE_PATH.lastIndexOf("/")+1);
    	}
    	
    }
    public static final String PACKAGE_PATH = ROOT_FILE_PATH + "com/chn/mybatis/gen/tpl/";
    public static final File GEN_FOLDER = new File(ROOT_FILE_PATH + "../gen");
    public static File VO_FOLDER;
    public static final String genPackage="genPackage";
    public static final String genPackagedao="genPackage.dao";
    public static final String genPackagevo="genPackage.vo";
    public static String GEN_PACKAGE_VO = "";
    public static String GEN_PACKAGE_DAO="";
    public static String GEN_PACKAGE="";
    public static GroupTemplate group;
    
    public static void main(String[] args) throws Exception {
    	
    	ClasspathResourceLoader loader=new ClasspathResourceLoader("com/chn/mybatis/gen/tpl/");
    	Configuration cfg = Configuration.defaultConfiguration();
    	group= new GroupTemplate(loader,cfg);
        Connection conn = DBUtils.getConn(args!=null&&args.length>0?args[0]:null);
        GEN_PACKAGE=ConfigUtils.getAlredy().get(genPackage);
        GEN_PACKAGE_VO=GEN_PACKAGE+"."+ConfigUtils.getAlredy().get(genPackagevo);
        GEN_PACKAGE_DAO=GEN_PACKAGE+"."+ConfigUtils.getAlredy().get(genPackagedao);
        VO_FOLDER=new File(ROOT_FILE_PATH, ConfigUtils.getAlredy().get(genPackagedao));
        DatabaseMetaData dbmd = DBUtils.getDatabaseMetaData(conn);
        
        String dbType = dbmd.getDatabaseProductName();
        DBUtils.loadMetadata(dbmd);
        
        for(String tableName : TableMetadata.getAllTables().keySet()) {
            generateXml(tableName, dbType);
            generateInterface(tableName, dbType);
            generateDomain(tableName, dbType);
        }
    }
    
    private static void generateXml(String tableName, String dbType) throws Exception {
        Template template = group.getTemplate("/"+dbType + "-mapper-xml.txt");
        if(template == null) throw new RuntimeException(String.format("未支持的数据库类型【%s】", dbType));
        
        TableTrans trans = TableTrans.find(tableName);
        template.binding("package", GEN_PACKAGE_DAO);
        template.binding("table", trans);
        FileUtils.write(new File(GEN_FOLDER,"/"+GEN_PACKAGE_DAO+"/"+ trans.getUpperStartClassName() + "Mapper.xml"), 
        		template.render());
    }
    
    private static void generateInterface(String tableName, String dbType) throws Exception {
        
        Template template = group.getTemplate("/"+dbType + "-mapper-java.txt");
        if(template == null) throw new RuntimeException(String.format("未支持的数据库类型【%s】", dbType));
        
        TableTrans trans = TableTrans.find(tableName);
        template.binding("package", GEN_PACKAGE_DAO);
        template.binding("packagevo",GEN_PACKAGE_VO);
        template.binding("table", trans);
        FileUtils.write(new File(GEN_FOLDER,"/"+GEN_PACKAGE_DAO+"/"+trans.getUpperStartClassName() + "Mapper.java"), 
                        template.render());
    }
    
    private static void generateDomain(String tableName, String dbType) throws Exception {
        
        Template template = group.getTemplate("/"+dbType + "-domain.txt");
        if(template == null) throw new RuntimeException(String.format("未支持的数据库类型【%s】", dbType));
        
        TableTrans trans = TableTrans.find(tableName);
        template.binding("package", GEN_PACKAGE_VO);
        template.binding("table", trans);
        FileUtils.write(new File(GEN_FOLDER, "/"+GEN_PACKAGE_VO+"/" + trans.getUpperStartClassName() + ".java"), 
        		template.render());
    }
}
