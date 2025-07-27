package tslc.beihaiyun.lyra.debug;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 数据库模式调试测试
 */
@SpringBootTest
@ActiveProfiles("test")
public class DatabaseSchemaTest {

    @Autowired
    private DataSource dataSource;

    @Test
    public void checkUserRolesTableSchema() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            
            System.out.println("=== 检查 user_roles 表结构 ===");
            
            // 检查表是否存在
            try (ResultSet tables = metaData.getTables(null, null, "user_roles", null)) {
                if (tables.next()) {
                    System.out.println("✓ user_roles 表存在");
                } else {
                    System.out.println("✗ user_roles 表不存在");
                    return;
                }
            }
            
            // 检查列结构
            try (ResultSet columns = metaData.getColumns(null, null, "user_roles", null)) {
                System.out.println("\n列结构:");
                while (columns.next()) {
                    String columnName = columns.getString("COLUMN_NAME");
                    String dataType = columns.getString("TYPE_NAME");
                    int columnSize = columns.getInt("COLUMN_SIZE");
                    String nullable = columns.getString("IS_NULLABLE");
                    String defaultValue = columns.getString("COLUMN_DEF");
                    
                    System.out.printf("  %s: %s(%d), nullable=%s, default=%s%n", 
                        columnName, dataType, columnSize, nullable, defaultValue);
                }
            }
            
            // 特别检查 is_deleted 列
            try (ResultSet columns = metaData.getColumns(null, null, "user_roles", "is_deleted")) {
                if (columns.next()) {
                    System.out.println("\n✓ is_deleted 列存在");
                    String dataType = columns.getString("TYPE_NAME");
                    System.out.println("  数据类型: " + dataType);
                } else {
                    System.out.println("\n✗ is_deleted 列不存在");
                }
            }
        }
    }
    
    @Test
    public void checkAllTables() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            
            System.out.println("=== 所有表列表 ===");
            try (ResultSet tables = metaData.getTables(null, null, null, new String[]{"TABLE"})) {
                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");
                    System.out.println("  " + tableName);
                }
            }
        }
    }
}
