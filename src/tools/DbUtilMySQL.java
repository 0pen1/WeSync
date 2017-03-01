package tools;

import org.apache.log4j.Logger;

import java.sql.*;

/**
 * MySQL���ݿ⹤�ߣ��������־�����
 *
 * @author Bob
 */
public class DbUtilMySQL {
    private Connection connection = null;
    private Statement statement = null;
    private ResultSet resultSet = null;
    private static String DBClassName = null;
    private static String DBUrl = null;
    private static String DBName = null;
    private static String DBUser = null;
    private static String DBPassword = null;

    private static DbUtilMySQL instance = null;

    private static Logger logger = Logger.getLogger(DbUtilMySQL.class);

    /**
     * ˽�еĹ���
     */
    private DbUtilMySQL() {
        loadConfig();
    }

    /**
     * ��ȡʵ�����̰߳�ȫ
     *
     * @return
     */
    public static synchronized DbUtilMySQL getInstance() {

        if (instance == null) {
            instance = new DbUtilMySQL();
        }
        return instance;
    }

    /**
     * �������ļ������������ݿ���Ϣ
     */
    private void loadConfig() {
        try {
            DBClassName = "com.mysql.jdbc.Driver";
            DBUrl = ConstantsTools.CONFIGER.getHostTo();
            DBName = ConstantsTools.CONFIGER.getNameTo();
            DBUser = ConstantsTools.CONFIGER.getUserTo();
            DBPassword = ConstantsTools.CONFIGER.getPasswordTo();

            Class.forName(DBClassName);
        } catch (Exception e) {

            logger.error(e.toString());
            e.printStackTrace();
        }
    }

    /**
     * ��ȡ���ӣ��̰߳�ȫ
     *
     * @return
     * @throws SQLException
     */
    public synchronized Connection getConnection() throws SQLException {
        String user = "";
        String password = "";
        try {
            DESPlus des = new DESPlus();
            user = des.decrypt(DBUser);
            password = des.decrypt(DBPassword);
        } catch (Exception e) {
            logger.error(PropertyUtil.getProperty("ds.ui.database.to.err.decode") + e.toString());
            e.printStackTrace();
        }
        // ��DB���ñ��ʱ���»�ȡ
        if (!ConstantsTools.CONFIGER.getHostTo().equals(DBUrl) || !ConstantsTools.CONFIGER.getNameTo().equals(DBName)
                || !ConstantsTools.CONFIGER.getUserTo().equals(DBUser)
                || !ConstantsTools.CONFIGER.getPasswordTo().equals(DBPassword)) {
            loadConfig();
            // "jdbc:mysql://localhost/pxp2p_branch"
            connection = DriverManager.getConnection(
                    "jdbc:mysql://" + DBUrl + "/" + DBName + "?useUnicode=true&characterEncoding=utf8", user, password);
            // �������ύ��ʽ��Ϊ�ֹ��ύ
            connection.setAutoCommit(false);
        }

        // ��connectionʧЧʱ���»�ȡ
        if (connection == null || connection.isValid(10) == false) {
            // "jdbc:mysql://localhost/pxp2p_branch"
            connection = DriverManager.getConnection(
                    "jdbc:mysql://" + DBUrl + "/" + DBName + "?useUnicode=true&characterEncoding=utf8", user, password);
            // �������ύ��ʽ��Ϊ�ֹ��ύ
            connection.setAutoCommit(false);
        }

        if (connection == null) {
            logger.error("Can not load MySQL jdbc and get connection.");
        }
        return connection;
    }

    /**
     * �������ӣ��̰߳�ȫ �����������ļ���ȡ
     *
     * @return
     * @throws SQLException
     */
    public synchronized Connection testConnection() throws SQLException {
        loadConfig();
        // "jdbc:mysql://localhost/pxp2p_branch"
        String user = "";
        String password = "";
        try {
            DESPlus des = new DESPlus();
            user = des.decrypt(DBUser);
            password = des.decrypt(DBPassword);
        } catch (Exception e) {
            logger.error(PropertyUtil.getProperty("ds.ui.database.to.err.decode") + e.toString());
            e.printStackTrace();
        }
        connection = DriverManager.getConnection("jdbc:mysql://" + DBUrl + "/" + DBName, user, password);
        // �������ύ��ʽ��Ϊ�ֹ��ύ
        connection.setAutoCommit(false);

        if (connection == null) {
            logger.error("Can not load MySQL jdbc and get connection.");
        }
        return connection;
    }

    /**
     * �������ӣ��̰߳�ȫ ��������λ�ȡ
     *
     * @return
     * @throws SQLException
     */
    public synchronized Connection testConnection(String DBUrl, String DBName, String DBUser, String DBPassword)
            throws SQLException {
        loadConfig();
        // "jdbc:mysql://localhost/pxp2p_branch"
        connection = DriverManager.getConnection("jdbc:mysql://" + DBUrl + "/" + DBName, DBUser, DBPassword);
        // �������ύ��ʽ��Ϊ�ֹ��ύ
        connection.setAutoCommit(false);

        if (connection == null) {
            logger.error("Can not load MySQL jdbc and get connection.");
        }
        return connection;
    }

    /**
     * ��ȡ���ݿ�������˽�У��̰߳�ȫ
     *
     * @throws SQLException
     */
    private synchronized void getStatement() throws SQLException {
        getConnection();
        // ����statementʧЧʱ�����´���
        if (statement == null || statement.isClosed() == true) {
            statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        }
    }

    /**
     * �رգ�����������������ӣ����̰߳�ȫ
     *
     * @throws SQLException
     */
    public synchronized void close() throws SQLException {
        if (resultSet != null) {
            resultSet.close();
            resultSet = null;
        }
        if (statement != null) {
            statement.close();
            statement = null;
        }
        if (connection != null) {
            connection.close();
            connection = null;
        }
    }

    /**
     * ִ�в�ѯ���̰߳�ȫ
     *
     * @param sql
     * @return
     * @throws SQLException
     */
    public synchronized ResultSet executeQuery(String sql) throws SQLException {
        getStatement();
        if (resultSet != null && resultSet.isClosed() == false) {
            resultSet.close();
        }
        resultSet = null;
        resultSet = statement.executeQuery(sql);
        return resultSet;
    }

    /**
     * ִ�и��£��̰߳�ȫ
     *
     * @param sql
     * @return
     * @throws SQLException
     */
    public synchronized int executeUpdate(String sql) throws SQLException {
        int result = 0;
        getStatement();
        result = statement.executeUpdate(sql);
        return result;
    }

}
