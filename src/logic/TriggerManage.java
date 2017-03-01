package logic;

import org.springframework.security.authentication.encoding.Md5PasswordEncoder;
import tools.ConstantsTools;
import tools.DbUtilMySQL;
import tools.DbUtilSQLServer;
import tools.Utils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TriggerManage {
    public static DbUtilSQLServer SQLServer;
    public static DbUtilMySQL MySQL;

    public static String getSqlUpdate(String snapName, Map<String, String> primKeyAndValueMap, String[] headerNow,
                                      String[] recordsLineBefore, String[] recordsLineNow) throws SQLException {

        StringBuffer sql = new StringBuffer();

        // ��ǰrecord,Key:����Value:��ֵ
        Map<String, String> recordNowMap = new HashMap<>();
        for (int i = 0; i < headerNow.length; i++) {
            recordNowMap.put(headerNow[i], recordsLineNow[i]);
        }

        // ԭrecord,Key:����Value:��ֵ
        Map<String, String> recordBeforeMap = new HashMap<>();
        for (int i = 0; i < headerNow.length; i++) {
            recordBeforeMap.put(headerNow[i], recordsLineBefore[i]);
        }

        // ������Ŀ���
        String targetTables[] = ExecuteThread.triggerMap.get(snapName);
        for (String targetTable : targetTables) {
            ArrayList<String> list = ExecuteThread.tableFieldMap.get(targetTable + ".UPDATE");
            for (String string : list) {
                sql.append(repairSqlString(string, recordBeforeMap, recordNowMap) + " ");
            }
        }

        return sql.toString();
    }

    /**
     * @param primKeyAndValueMap
     * @param headerNow
     * @param recordsLineNow
     * @return
     * @throws SQLException
     */
    public static String getSqlInsert(String snapName, Map<String, String> primKeyAndValueMap, String[] headerNow,
                                      String[] recordsLineNow) throws SQLException {
        StringBuffer sql = new StringBuffer();

        // ��ǰrecord,Key:����Value:��ֵ
        Map<String, String> recordNowMap = new HashMap<>();
        for (int i = 0; i < headerNow.length; i++) {
            recordNowMap.put(headerNow[i], recordsLineNow[i]);
        }

        // ������Ŀ���
        String targetTables[] = ExecuteThread.triggerMap.get(snapName);
        for (String targetTable : targetTables) {
            ArrayList<String> list = null;
            list = ExecuteThread.tableFieldMap.get(targetTable + ".INSERT");
            for (String string : list) {
                sql.append(repairSqlString(string, null, recordNowMap) + " ");
            }
        }

        return sql.toString();
    }

    public static String getSqlDelete(String snapName, Map<String, String> primKeyAndValueMap, String[] headerNow,
                                      String[] recordsLineBefore) throws SQLException {
        StringBuffer sql = new StringBuffer();
        // ԭrecord,Key:����Value:��ֵ
        Map<String, String> recordBeforeMap = new HashMap<>();
        for (int i = 0; i < headerNow.length; i++) {
            recordBeforeMap.put(headerNow[i], recordsLineBefore[i]);
        }
        // ������Ŀ���
        String targetTables[] = ExecuteThread.triggerMap.get(snapName);
        for (String targetTable : targetTables) {
            ArrayList<String> list = ExecuteThread.tableFieldMap.get(targetTable + ".DELETE");
            for (String string : list) {
                sql.append(repairSqlString(string, recordBeforeMap, null) + " ");
            }
        }

        return sql.toString();
    }

    /**
     * ����sql�еĹ�ʽ
     *
     * @param string
     * @param recordNowMap
     * @return
     * @throws SQLException
     */
    private static String repairSqlString(String string, Map<String, String> recordBeforeMap,
                                          Map<String, String> recordNowMap) throws SQLException {

        SQLServer = DbUtilSQLServer.getInstance();

        if (string.contains("this.")) {
            // ��һ����ʽ���滻this.������
            // this.***��ȡ��ǰ���ո�����¼��ĳ�ֶ�ֵ
            // before.***��ȡԭ���ո�����¼��ĳ�ֶ�ֵ����UPDATE��DELETEʱ���õ���
            Pattern p = Pattern.compile("this\\.[.\\w]+");
            Matcher matcher = p.matcher(string);
            ArrayList<String> strs = new ArrayList<String>();
            while (matcher.find()) {
                strs.add(matcher.group(0));
            }
            for (String str : strs) {
                if (recordNowMap.containsKey(str.substring(str.indexOf(".") + 1))) {
                    string = string.replaceAll(str, recordNowMap.get(str.substring(str.indexOf(".") + 1)));
                }

            }
        }
        if (string.contains("before.")) {
            // ��һ����ʽ���滻before.������
            // this.***��ȡ��ǰ���ո�����¼��ĳ�ֶ�ֵ
            // before.***��ȡԭ���ո�����¼��ĳ�ֶ�ֵ����UPDATE��DELETEʱ���õ���
            Pattern p = Pattern.compile("before\\.[.\\w]+");
            Matcher matcher = p.matcher(string);
            ArrayList<String> strs = new ArrayList<String>();
            while (matcher.find()) {
                strs.add(matcher.group(0));
            }
            for (String str : strs) {
                if (recordBeforeMap.containsKey(str.substring(str.indexOf(".") + 1))) {
                    string = string.replaceAll(str, recordBeforeMap.get(str.substring(str.indexOf(".") + 1)));
                }

            }
        }
        if (string.contains("$")) {
            // �ڶ�����ʽ��
            // $INCREASE{POSITION_CODE}$�����������ڵĳ־û�����������*��config�ļ������ó־û�������
            // $PINYIN{}$�����������ڵĺ���תΪƴ��
            // $SYS_DATE_TIME$��ϵͳ����+ʱ�䣨2016-01-19 17:57:49��
            // $SQL{}$��ִ�д������ڵ��Ӳ�ѯsql�����ض�Ӧ�Ĳ�ѯ���������һ����
            Pattern p = Pattern.compile("\\$([^$]+)\\$");
            Matcher matcher = p.matcher(string);
            ArrayList<String> strs = new ArrayList<String>();
            while (matcher.find()) {
                strs.add(matcher.group(0));
            }
            for (String str : strs) {
                if (str.startsWith("$PINYIN")) {
                    p = Pattern.compile("\\{([^{}]+)\\}");
                    matcher = p.matcher(str);
                    while (matcher.find()) {
                        string = string.replace(str, Utils.getPingYin(matcher.group(1).trim()));
                    }
                } else if (str.startsWith("$SQL")) {
                    p = Pattern.compile("\\{([^{}]+)\\}");
                    matcher = p.matcher(str);
                    while (matcher.find()) {
                        String tempSql = matcher.group(1);
                        ResultSet rs = SQLServer.executeQuery(tempSql);
                        String temp = "";
                        int rowCount = 0;
                        while (rs.next()) {
                            temp = rs.getString(1);
                            rowCount++;
                        }
                        if (rowCount != 1 || temp == null) {
                            // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                            temp = "";
                        }
                        string = string.replace(str, temp);
                    }
                } else if (str.startsWith("$SYS_DATE_TIME")) {
                    string = string.replace(str, Utils.getCurrentTime());
                } else if (str.startsWith("$INCREASE")) {
                    p = Pattern.compile("\\{([^{}]+)\\}");
                    matcher = p.matcher(str);
                    String para = "";
                    while (matcher.find()) {
                        para = matcher.group(1);
                    }
                    if ("POSITION_CODE".equals(para)) {
                        String position_code = ConstantsTools.CONFIGER.getPositionCode();// ��config�ļ�ȡ���־û��Ĳ���ֵ

                        string = string.replace(str, String.format("%04d", Integer.parseInt(position_code))); // ����4λ��ǰ�油0

                        int p_code = Integer.parseInt(position_code) + 1;// ��1֮����г־û�
                        try {
                            ConstantsTools.CONFIGER.setPositionCode(String.valueOf(p_code));
                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    } else {
                        string = string.replace(str, "");
                    }

                }
            }
        }

        if (string.contains("@")) {
            // ��������ʽ��
            // @SUB(5,8){}@�����������ڵ��ַ����ӵ�5���ַ���ȡ����8���ַ�
            // @SUB(5,END){}@�����������ڵ��ַ����ӵ�5���ַ���ȡ�����
            // @SUB(0,LAST"."){}@�����������ڵ��ַ����ӵ�0���ַ���ȡ�����һ��"."
            // @SUB("a",LAST"a"){}@�����������ڵ��ַ����ӵ�1��"a"��ȡ�����һ��"a"��������,�����ң�
            Pattern p = Pattern.compile("\\@([^@]+)\\@");
            Matcher matcher = p.matcher(string);
            ArrayList<String> strs = new ArrayList<String>();
            while (matcher.find()) {
                strs.add(matcher.group(0));
            }
            for (String str : strs) {
                if (str.startsWith("@SUB")) {
                    // �Ȼ�ȡҪ��ȡ��index���ַ�������
                    String indexs[] = null;
                    String strContent = "";
                    p = Pattern.compile("\\(([^()]+)\\)");
                    matcher = p.matcher(str);
                    while (matcher.find()) {
                        indexs = matcher.group(1).split(",");
                    }
                    p = Pattern.compile("\\{([^{}]+)\\}");
                    matcher = p.matcher(str);
                    while (matcher.find()) {
                        strContent = matcher.group(1);
                    }
                    int beginIndex = 0;
                    int endIndex = 0;

                    p = Pattern.compile("[0-9]*");
                    if (p.matcher(indexs[0]).matches()) {
                        beginIndex = Integer.parseInt(indexs[0]);
                    } else {
                        beginIndex = strContent.indexOf(indexs[0].replaceAll("\"", "")) + 1;
                    }

                    if (p.matcher(indexs[1]).matches()) {
                        endIndex = Integer.parseInt(indexs[1]);
                    } else if ("END".equals(indexs[1])) {
                        endIndex = strContent.length();
                    } else if (indexs[1].startsWith("LAST")) {
                        // if (strContent.contains(indexs[1].substring(5,
                        // indexs[1].length() - 1))) {
                        endIndex = strContent.lastIndexOf(indexs[1].substring(5, indexs[1].length() - 1));
                        // } else {
                        // endIndex = strContent.length();
                        // }
                    }
                    string = string.replace(str, strContent.substring(beginIndex, endIndex));
                }
            }
        }

        if (string.contains("#")) {
            // ���ļ���ʽ��
            // #REPLACE("a","b"){}#�����������������е�"a"�滻Ϊ"b"
            // #CASE(a=b,c=d,e=f){}#�������������ߵ�������a�����滻Ϊb�����Ϊc�����滻Ϊd...
            Pattern p = Pattern.compile("\\#([^#]+)\\#");
            Matcher matcher = p.matcher(string);
            ArrayList<String> strs = new ArrayList<String>();
            while (matcher.find()) {
                strs.add(matcher.group(0));
            }
            for (String str : strs) {
                if (str.startsWith("#REPLACE")) {
                    // �Ȼ�ȡҪ�滻���¾��ַ���������
                    String indexs[] = null;
                    String strContent = "";
                    p = Pattern.compile("\\(([^()]+)\\)");
                    matcher = p.matcher(str);
                    while (matcher.find()) {
                        indexs = matcher.group(1).split(",");
                    }
                    p = Pattern.compile("\\{([^{}]+)\\}");
                    matcher = p.matcher(str);
                    while (matcher.find()) {
                        strContent = matcher.group(1);
                    }
                    String oldStr = "";
                    String newStr = "";

                    oldStr = indexs[0].replaceAll("\"", "");
                    newStr = indexs[1].replaceAll("\"", "");

                    String temp = strContent.replace(oldStr, newStr);
                    string = string.replace(str, temp);
                } else if (str.startsWith("#CASE")) {

                    String key_value[] = null;
                    String strContent = "";
                    p = Pattern.compile("\\(([^()]+)\\)");
                    matcher = p.matcher(str);
                    while (matcher.find()) {
                        key_value = matcher.group(1).split(",");
                    }
                    p = Pattern.compile("\\{([^{}]+)\\}");
                    matcher = p.matcher(str);
                    while (matcher.find()) {
                        strContent = matcher.group(1);
                    }

                    HashMap<String, String> map = new HashMap<String, String>();
                    for (String k_v : key_value) {
                        String temp[] = k_v.split("=");
                        map.put(temp[0], temp[1]);
                    }

                    if (map.containsKey(strContent)) {
                        string = string.replace(str, map.get(strContent));
                    } else {
                        string = string.replace(str, "");
                    }

                } else if (str.startsWith("#MD5PE")) {
                    String strContent = "";

                    p = Pattern.compile("\\{([^{}]+)\\}");
                    matcher = p.matcher(str);
                    while (matcher.find()) {
                        strContent = matcher.group(1);
                    }
                    string = string.replace(str, new Md5PasswordEncoder().encodePassword(strContent, null));
                }
            }
        }

        return string;
    }

}
