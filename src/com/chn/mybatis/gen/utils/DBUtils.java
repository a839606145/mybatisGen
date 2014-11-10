/**
 * MyBatisGenerator
 * @title DBUtils.java
 * @package com.chn.mybatis.gen.utils
 * @author lzxz1234<lzxz1234@gmail.com>
 * @date 2014��11��6��-����5:30:32
 * @version V1.0
 * Copyright (c) 2014 ChineseAll.com All Right Reserved
 */
package com.chn.mybatis.gen.utils;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.apache.log4j.Logger;

import com.chn.mybatis.gen.def.ColumnMetadata;
import com.chn.mybatis.gen.def.TableMetadata;
import com.chn.mybatis.gen.utils.ConfigUtils.Cfg;

/**
 * @class DBUtils
 * @author lzxz1234
 * @description 
 * @version v1.0
 */
public class DBUtils {

    public static final Logger log = Logger.getLogger(DBUtils.class);
    
    public static final String CONFIG_FILE = "config.properties";
    
    public static final String KEY_DRIVER = "jdbc.driver";
    public static final String KEY_URL = "jdbc.url";
    public static final String KEY_USERNAME = "jdbc.username";
    public static final String KEY_PASSWORD = "jdbc.password";
    
    public static Connection getConn() {
        
        log.info("��ʼ�����ݿ⡾��ʼ��");
        
        Connection conn = null;
        Cfg cfg = ConfigUtils.getCfg(CONFIG_FILE);
        String driver   = cfg.get(KEY_DRIVER);
        String url      = cfg.get(KEY_URL);
        String username = cfg.get(KEY_USERNAME);
        String password = cfg.get(KEY_PASSWORD);
        log.info(String.format("  ����������driver  ��%s", driver  ));
        log.info(String.format("  ����������url     ��%s", url     ));
        log.info(String.format("  ����������username��%s", username));
        log.info(String.format("  ����������password��%s", password));
        
        try {
            
            Class.forName(driver);
            conn = DriverManager.getConnection(url, username, password);
        } catch (ClassNotFoundException e) {
            
            throw new RuntimeException(String.format("[JDBC�������ش���][%s]", driver), e);
        } catch (SQLException e) {
            
            throw new RuntimeException(String.format("[���ݿ�����ʧ��]"), e);
        }
        log.info("��ʼ�����ݿ⡾��ɡ���");
        return conn;
    }
    
    public static DatabaseMetaData getDatabaseMetaData(Connection conn) {
        
        DatabaseMetaData dbmd = null;
        ResultSet rs = null;
        try {
            dbmd = conn.getMetaData();
            rs = dbmd.getTypeInfo();
            while (rs.next()) {
                log.info(String.format("�������ơ�%s�� JavaType��%s����󾫶ȡ�%s��",
                        rs.getString(1), SqlTypeUtils.decodeToJavaType(rs.getInt(2)), rs.getString(3)));
            }

            String dbType = dbmd.getDatabaseProductName();
            String dbVersion = dbmd.getDatabaseProductVersion();
            String driverName = dbmd.getDriverName();
            String driverVersion = dbmd.getDriverVersion();
            log.info(String.format("���ݿ����͡�%s�����ݿ�汾��%s�����ݿ��������ơ�%s�����ݿ���������汾��%s��", 
                    dbType, dbVersion, driverName, driverVersion));

        } catch (SQLException e) {
            throw new RuntimeException("��ȡԪ���ݳ���", e);
        }
        return dbmd;
    }
    
    public static IteratorableHashMap<String, TableMetadata> getTableList(DatabaseMetaData dbmd) {

        IteratorableHashMap<String, TableMetadata> tables = new IteratorableHashMap<>();
        String[] types = { "TABLE" };

        ResultSet rs = null;
        try {
            rs = dbmd.getTables(null, null, null, types);
            while (rs.next()) 
                solveTable(rs, tables);
            
            rs = dbmd.getColumns(null, null, null, null);
            while(rs.next()) 
                solveColumn(rs, tables);
            
            for(TableMetadata table : tables) {
                rs = dbmd.getPrimaryKeys(null, null, table.getTableName());
                while(rs.next())
                    solvePrimaryKey(rs, tables);
                rs = dbmd.getImportedKeys(null, null, table.getTableName());
                while(rs.next()) 
                    solveForeignKey(rs, tables);
            }
        } catch (SQLException e) {
            throw new RuntimeException("��ȡ����Ϣ����", e);
        }
        return tables;

    }
    
    private static void solveForeignKey(ResultSet rs, Map<String, TableMetadata> tables) 
            throws SQLException {
        
        String targetTableName = rs.getString("PKTABLE_NAME");
        String targetColumnName = rs.getString("PKCOLUMN_NAME");
        String tableName = rs.getString("FKTABLE_NAME");
        String columnName = rs.getString("FKCOLUMN_NAME");
        
        ColumnMetadata foreignColumn = tables.get(targetTableName).getColumn(targetColumnName);
        if(foreignColumn == null) foreignColumn = tables.get(targetTableName).getKey(targetColumnName);
        tables.get(tableName).addLink(columnName, foreignColumn);
        log.info(String.format("  ��%s���е��С�%s�����ñ�%s�����С�%s��", 
                tableName, columnName, targetTableName, targetColumnName));
    }
    
    private static void solvePrimaryKey(ResultSet rs, Map<String, TableMetadata> tables) 
            throws SQLException {
        
        String tableName = rs.getString("TABLE_NAME");
        String columnName = rs.getString("COLUMN_NAME");
        
        TableMetadata tableMetadata = tables.get(tableName);
        ColumnMetadata keyColumn = tableMetadata.getColumns().remove(columnName);
        tableMetadata.getKeys().put(columnName, keyColumn);
        log.debug(String.format("  ��%s���еġ�%s���б��Ϊ����", tableName, columnName));
    }
    
    private static void solveTable(ResultSet rs, Map<String, TableMetadata> tables)
            throws SQLException {
        
        TableMetadata table = new TableMetadata();
        table.setTableCat   (rs.getString("TABLE_CAT  ".trim()));
        table.setTableSchema(rs.getString("TABLE_SCHEM".trim()));
        table.setTableName  (rs.getString("TABLE_NAME ".trim()));
        table.setTableType  (rs.getString("TABLE_TYPE ".trim()));
        table.setRemarks    (rs.getString("REMARKS    ".trim()));
        tables.put(table.getTableName(), table);
        log.debug(String.format("  ���ֱ�%s��", table.getTableName()));
    }
    
    private static void solveColumn(ResultSet rs, Map<String, TableMetadata> tables) 
            throws SQLException {
        
        ColumnMetadata column = new ColumnMetadata();
        column.setTableCat         (rs.getString("TABLE_CAT         ".trim()));
        column.setTableSchema      (rs.getString("TABLE_SCHEM       ".trim()));
        column.setTableName        (rs.getString("TABLE_NAME        ".trim()));
        column.setColumnName       (rs.getString("COLUMN_NAME       ".trim()));
        column.setDataType         (rs.getInt   ("DATA_TYPE         ".trim()));
        column.setTypeName         (rs.getString("TYPE_NAME         ".trim()));
        column.setColumnSize       (rs.getInt   ("COLUMN_SIZE       ".trim()));
        column.setDecimalDigits    (rs.getInt   ("DECIMAL_DIGITS    ".trim()));
        column.setNumPrecRadix     (rs.getInt   ("NUM_PREC_RADIX    ".trim()));
        column.setNullable         (rs.getInt   ("NULLABLE          ".trim()));
        column.setRemarks          (rs.getString("REMARKS           ".trim()));
        column.setColumnDef        (rs.getString("COLUMN_DEF        ".trim()));
        column.setCharOctetLength  (rs.getInt   ("CHAR_OCTET_LENGTH ".trim()));
        column.setOrdinalPosition  (rs.getInt   ("ORDINAL_POSITION  ".trim()));
        column.setIsNullable       (rs.getString("IS_NULLABLE       ".trim()));
        column.setScopeCatalog     (rs.getString("SCOPE_CATALOG     ".trim()));
        column.setScopeSchema      (rs.getString("SCOPE_SCHEMA      ".trim()));
        column.setScopeTable       (rs.getString("SCOPE_TABLE       ".trim()));
        column.setSourceDataType   (rs.getShort ("SOURCE_DATA_TYPE  ".trim()));
        column.setIsAutoincrement  (rs.getString("IS_AUTOINCREMENT  ".trim()));
        TableMetadata targetTable = tables.get(column.getTableName());
        column.setTableMetadata(targetTable);
        targetTable.addColumn(column);
        log.debug(String.format("  ��%s�������С�%s����������Ϊ��%s��", 
                column.getTableName(), column.getColumnName(), column.getTypeName()));
    }
}
