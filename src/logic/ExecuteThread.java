package logic;

import UI.panel.StatusPanel;
import com.opencsv.CSVWriter;
import logic.bean.Table;
import org.apache.log4j.Logger;
import tools.*;

import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ִ�����߳�
 *
 * @author Bob
 */
public class ExecuteThread extends Thread implements ExecuteThreadInterface {

    private static Logger logger = Logger.getLogger(ExecuteThread.class);
    // ��-�ֶ������ļ�����Map,key:Ŀ�����,value:��Ӧ��ϵ����List
    public static LinkedHashMap<String, ArrayList<String>> tableFieldMap;
    // TriggerMap,key:������,value:������
    public static LinkedHashMap<String, String[]> triggerMap;
    // ��Դ��Map,key:������,value:Table
    public static LinkedHashMap<String, Table> originalTablesMap;
    // Ŀ���Map,key:������,value:Table
    public static LinkedHashMap<String, Table> targetTablesMap;

    /**
     * ��ʼ������
     */
    public void init() {
        tableFieldMap = new LinkedHashMap<String, ArrayList<String>>();
        triggerMap = new LinkedHashMap<String, String[]>();
        originalTablesMap = new LinkedHashMap<String, Table>();
        targetTablesMap = new LinkedHashMap<String, Table>();
    }

    /**
     * ��������
     */
    public boolean testLink() {
        StatusLog.setStatusDetail(PropertyUtil.getProperty("ds.logic.testLinking"), LogLevel.INFO);
        StatusPanel.progressCurrent.setMaximum(2);
        StatusPanel.progressCurrent.setValue(0);
        boolean isLinkedPass = true;
        DbUtilSQLServer dbSQLServer = null;
        DbUtilMySQL dbMySQL = null;
        try {
            dbSQLServer = DbUtilSQLServer.getInstance();
            Connection connSQLServer = dbSQLServer.testConnection();
            StatusPanel.progressCurrent.setValue(1);
            dbMySQL = DbUtilMySQL.getInstance();
            Connection connMySQL = dbMySQL.testConnection();
            StatusPanel.progressCurrent.setValue(2);
            if (connSQLServer == null) {
                isLinkedPass = false;
                StatusLog.setStatusDetail("SQLServer " + PropertyUtil.getProperty("ds.logic.testLinkFail"), LogLevel.ERROR);
            }

            if (connMySQL == null) {
                isLinkedPass = false;
                StatusLog.setStatusDetail("connMySQL " + PropertyUtil.getProperty("ds.logic.testLinkFail"), LogLevel.ERROR);
            }

            StatusLog.setStatusDetail(PropertyUtil.getProperty("ds.logic.testLinkFinish"), LogLevel.INFO);

        } catch (Exception e) {

            StatusLog.setStatusDetail(PropertyUtil.getProperty("ds.logic.testLinkFail") + e.toString(), LogLevel.ERROR);
            isLinkedPass = false;
        } finally {
            if (dbSQLServer != null) {
                try {
                    dbSQLServer.close();
                } catch (SQLException e) {
                    StatusLog.setStatusDetail(PropertyUtil.getProperty("ds.logic.testLinkFail") + e.toString(), LogLevel.ERROR);
                    e.printStackTrace();
                }
            }
            if (dbMySQL != null) {
                try {
                    dbMySQL.close();
                } catch (SQLException e) {
                    StatusLog.setStatusDetail(PropertyUtil.getProperty("ds.logic.testLinkFail") + e.toString(), LogLevel.ERROR);
                    e.printStackTrace();
                }
            }

        }
        return isLinkedPass;
    }

    /**
     * ���������ļ�
     */
    public boolean analyseConfigFile() {
        StatusLog.setStatusDetail(PropertyUtil.getProperty("ds.logic.startAnalyse"), LogLevel.INFO);

        boolean isAnalyseSuccess = true;

        // ������-�ֶζ�Ӧ��ϵ�ļ�
        File tableFieldDir = null;
        // ��������ļ��в����ڣ��������ʼ�����ֶι�ϵ����
        File snapsDir = new File(ConstantsLogic.SNAPS_DIR);
        if (!snapsDir.exists()) {
            tableFieldDir = new File(ConstantsLogic.TABLE_FIELD_INIT_DIR);
        } else {
            tableFieldDir = new File(ConstantsLogic.TABLE_FIELD_DIR);
        }

        File tableFieldFiles[] = tableFieldDir.listFiles();
        ArrayList<String> list;

        StatusPanel.progressCurrent.setMaximum(tableFieldFiles.length);
        int progressValue = 0;
        StatusPanel.progressCurrent.setValue(progressValue);

        for (File file : tableFieldFiles) {

            String fileName = file.getName();
            if (!fileName.endsWith(".sql")) {
                progressValue++;
                StatusPanel.progressCurrent.setValue(progressValue);
                continue;
            } else {
                list = new ArrayList<String>();
                try {
                    // ��ȡ������-�ֶ�����sql�ļ�
                    list = FileUtils.getSqlFileContentList(file);

                    String key = fileName.substring(0, fileName.lastIndexOf("."));

                    // ����-�ֶ������ļ����ݴ�ŵ�Map,key:����,value:��Ӧ��ϵ����List
                    tableFieldMap.put(key, list);

                } catch (IOException e) {
                    isAnalyseSuccess = false;
                    StatusLog.setStatusDetail(PropertyUtil.getProperty("ds.logic.AnalyseFail") + e.toString(), LogLevel.ERROR);
                    e.printStackTrace();
                }
                progressValue++;
                StatusPanel.progressCurrent.setValue(progressValue);
            }
        }

        // ����Trigger�ļ�
        File triggerFile = new File(ConstantsLogic.TRIGGER_FILE);
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(triggerFile));
            String lineTxt = null;
            while ((lineTxt = reader.readLine()) != null) {
                lineTxt = lineTxt.trim();
                if ("".equals(lineTxt) || lineTxt.startsWith("//")) {
                    // ����ע�ͺͿ���
                    continue;
                } else {
                    if (lineTxt.contains("//")) {
                        // ȥ��ע��
                        lineTxt = lineTxt.substring(0, lineTxt.indexOf("//")).trim();
                    }
                    // ��]=<�ָ�����=�ָ�׼ȷ����Ϊ����������Ҳ���ܻ���=
                    String arr[] = lineTxt.split("\\]=\\<");
                    arr[0] = arr[0] + "]";// ���ϱ�split����"]"
                    // ȡ��Դ������Ŀ�����
                    String snapName = arr[0].substring(0, arr[0].indexOf(":"));
                    String tarTableNames[] = new String[arr.length - 1];

                    // ��ȡ��Դ��map<������,(����,����,�ֶ�,������������)>
                    Table tableOri = new Table();
                    // ƥ�����
                    Pattern p = Pattern.compile(ConstantsLogic.REGEX_TRIGGER_TABLE);
                    Matcher matcher = p.matcher(arr[0]);
                    while (matcher.find()) {
                        tableOri.setTableName(matcher.group(1));
                    }
                    // ƥ�������
                    p = Pattern.compile(ConstantsLogic.REGEX_TRIGGER_PRIM_KEY);
                    matcher = p.matcher(arr[0]);
                    while (matcher.find()) {
                        tableOri.setPrimKey(matcher.group(1));
                    }
                    // ƥ����ֶ�
                    p = Pattern.compile(ConstantsLogic.REGEX_TRIGGER_FIELDS);
                    matcher = p.matcher(arr[0]);
                    while (matcher.find()) {
                        tableOri.setFields(matcher.group(1));
                    }
                    // ƥ��������������
                    p = Pattern.compile(ConstantsLogic.REGEX_TRIGGER_OTHER);
                    matcher = p.matcher(arr[0]);
                    while (matcher.find()) {
                        tableOri.setOther(matcher.group(1));
                    }
                    originalTablesMap.put(snapName, tableOri);

                    for (int i = 1; i < arr.length; i++) {
                        arr[i] = "<" + arr[i];// ���ϱ�split����"<"
                        // ��ȡĿ���map<������,(����,����,�ֶ�,������������)>
                        Table tableTar = new Table();
                        // ƥ�����
                        p = Pattern.compile(ConstantsLogic.REGEX_TRIGGER_TABLE);
                        matcher = p.matcher(arr[i]);
                        while (matcher.find()) {
                            String temp = matcher.group(1);
                            tableTar.setTableName(temp);
                            tarTableNames[i - 1] = temp;
                        }

                        // ƥ�������
                        p = Pattern.compile(ConstantsLogic.REGEX_TRIGGER_PRIM_KEY);
                        matcher = p.matcher(arr[i]);
                        while (matcher.find()) {
                            tableTar.setPrimKey(matcher.group(1));
                        }
                        // ƥ����ֶ�
                        p = Pattern.compile(ConstantsLogic.REGEX_TRIGGER_FIELDS);
                        matcher = p.matcher(arr[i]);
                        while (matcher.find()) {
                            tableTar.setFields(matcher.group(1));
                        }
                        // ƥ��������������
                        p = Pattern.compile(ConstantsLogic.REGEX_TRIGGER_OTHER);
                        matcher = p.matcher(arr[i]);
                        while (matcher.find()) {
                            tableTar.setOther(matcher.group(1));
                        }
                        targetTablesMap.put(snapName, tableTar);
                    }
                    triggerMap.put(snapName, tarTableNames);
                }

            }

        } catch (FileNotFoundException e) {
            isAnalyseSuccess = false;
            StatusLog.setStatusDetail(PropertyUtil.getProperty("ds.logic.AnalyseTriggerFail") + e.toString(), LogLevel.ERROR);
            e.printStackTrace();
        } catch (IOException e) {
            isAnalyseSuccess = false;
            StatusLog.setStatusDetail(PropertyUtil.getProperty("ds.logic.AnalyseTriggerFail") + e.toString(), LogLevel.ERROR);
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    isAnalyseSuccess = false;
                    StatusLog.setStatusDetail(PropertyUtil.getProperty("ds.logic.AnalyseTriggerFail") + e.toString(), LogLevel.ERROR);
                    e.printStackTrace();
                }
            }
        }

        if (isAnalyseSuccess) {
            StatusLog.setStatusDetail(PropertyUtil.getProperty("ds.logic.finishAnalyse"), LogLevel.INFO);
        }

        return isAnalyseSuccess;

    }

    /**
     * ����
     */
    public void backUp() {
        StatusLog.setStatusDetail(PropertyUtil.getProperty("ds.logic.startBackUp"), LogLevel.INFO);

        StatusPanel.progressCurrent.setMaximum(100);
        String user = "";
        String password = "";
        try {
            DESPlus des = new DESPlus();
            user = des.decrypt(ConstantsTools.CONFIGER.getUserTo());
            password = des.decrypt(ConstantsTools.CONFIGER.getPasswordTo());
        } catch (Exception e) {
            logger.error(PropertyUtil.getProperty("ds.ui.database.to.err.decode") + e.toString());
            e.printStackTrace();
        }
        BackupManage.exportDatabase(ConstantsTools.CONFIGER.getHostTo(), user, password,
                ConstantsTools.CONFIGER.getNameTo());

        StatusLog.setStatusDetail(PropertyUtil.getProperty("ds.logic.finishBackUp"), LogLevel.INFO);
    }

    /**
     * �½�����
     */
    public boolean newSnap() {
        StatusLog.setStatusDetail(PropertyUtil.getProperty("ds.logic.startNewSnap"), LogLevel.INFO);
        boolean isSuccess = true;

        isSuccess = SnapManage.createSnap(originalTablesMap);
        if (isSuccess) {
            StatusLog.setStatusDetail(PropertyUtil.getProperty("ds.logic.finishNewSnap"), LogLevel.INFO);
        }

        return isSuccess;
    }

    /**
     * �Աȿ��գ�������SQL
     */
    public boolean diffSnap() {
        StatusLog.setStatusDetail(PropertyUtil.getProperty("ds.logic.startDiffSnap"), LogLevel.INFO);
        boolean isSuccess = true;

        isSuccess = SnapManage.diffSnap(originalTablesMap);

        if (isSuccess) {
            StatusLog.setStatusDetail(PropertyUtil.getProperty("ds.logic.finishDiffSnap"), LogLevel.INFO);
        } else {
            StatusLog.setStatusDetail(PropertyUtil.getProperty("ds.logic.diffSnapFail"), LogLevel.ERROR);
        }

        return isSuccess;
    }

    /**
     * ִ��SQL���
     *
     * @throws Exception
     */
    public boolean executeSQL() {
        StatusLog.setStatusDetail(PropertyUtil.getProperty("ds.logic.startRunSql"), LogLevel.INFO);

        boolean isSuccess = true;
        ArrayList<String> sqlList = SnapManage.sqlList;
        File logSqlFileDir = new File(ConstantsLogic.LOG_SQL_DIR);// sql��־Ŀ¼
        if (!logSqlFileDir.exists()) {
            logSqlFileDir.mkdirs();
        }
        File logSqlFile = new File(ConstantsLogic.LOG_SQL);// sql��־�ļ�
        CSVWriter csvWriter = null;
        int totalSqls = 0;// ��sql��
        if (sqlList.size() != 0) {
            if ("false".equals(ConstantsTools.CONFIGER.getDebugMode())) {
                String sqlForLog = "";
                try {
                    if (!logSqlFile.exists()) {
                        logSqlFile.createNewFile();
                    }

                    // �������������ɵ�����sql
                    StringBuffer sqlBuff = new StringBuffer();
                    for (String string : sqlList) {
                        String tempArr[] = string.split(";");
                        for (String tempSql : tempArr) {
                            if (!"".equals(tempSql.trim())) {
                                sqlBuff.append(tempSql).append(";");
                            }
                        }
                    }

                    String sqls[] = sqlBuff.toString().split(";");

                    StatusPanel.progressCurrent.setMaximum(sqls.length);

                    totalSqls = 0;// ��sql��
                    int affectedRecords = 0;// ��Ӱ��Ľ������
                    int progressValue = 0;
                    for (String string : sqls) {
                        if (!"".equals(string.trim())) {
                            totalSqls++;
                            sqlForLog = string;
                            int result = DbUtilMySQL.getInstance().executeUpdate(string + ";");
                            affectedRecords += result;

                            logger.warn("===" + string + ";===" + result);
                        }
                        progressValue++;
                        StatusPanel.progressCurrent.setValue(progressValue);
                    }

                    Writer writer = new FileWriter(logSqlFile, true);// �ڶ�������:true��ʾ���ļ���β׷��
                    csvWriter = new CSVWriter(writer, ',');
                    String logLine[] = new String[4];// log_sql��һ��:ϵͳʱ��,�ڶ���:��������Ҫִ�е�sql,������:ִ�н����Ӱ���������,������:�Ƿ�ɹ�
                    logLine[0] = Utils.getCurrentTime(); // log_sql��һ�У�ϵͳʱ��
                    logLine[1] = sqlBuff.toString();// log_sql�ڶ��У���������Ҫִ�е�sql
                    logLine[2] = String.valueOf(affectedRecords);// log_sql�����У�ִ�н����Ӱ���������

                    if ("true".equals(ConstantsTools.CONFIGER.getStrictMode())) {
                        if (affectedRecords == totalSqls) {
                            DbUtilMySQL.getInstance().getConnection().commit();
                            logLine[3] = "Success";// log_sql�����У�ִ�н���ɹ�
                        } else {
                            DbUtilMySQL.getInstance().getConnection().rollback();
                            logLine[3] = "Fail";// log_sql�����У�ִ�н��ʧ��
                            isSuccess = false;
                            StatusLog.setStatusDetail(PropertyUtil.getProperty("ds.logic.runSqlFail"), LogLevel.ERROR);
                            return false;
                        }
                    } else {
                        DbUtilMySQL.getInstance().getConnection().commit();
                        logLine[3] = "Success";// log_sql�����У�ִ�н���ɹ�
                    }
                    if (!"".equals(logLine[1].trim())) {
                        csvWriter.writeNext(logLine);
                        csvWriter.flush();
                    }

                    StatusLog.setLastTime(Utils.getCurrentTime());
                    StatusLog.setStatusDetail(PropertyUtil.getProperty("ds.logic.runSqlFinish"), LogLevel.INFO);
                    StatusLog.setStatusDetail(PropertyUtil.getProperty("ds.logic.syncFinish"), LogLevel.INFO);
                    return isSuccess;
                } catch (Exception e) {
                    try {
                        DbUtilMySQL.getInstance().getConnection().rollback();
                    } catch (SQLException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                    isSuccess = false;
                    StatusLog.setStatusDetail("!!!" + sqlForLog + PropertyUtil.getProperty("ds.logic.currentSqlFail") + e.toString(), LogLevel.ERROR);

                } finally {
                    if (csvWriter != null) {
                        try {
                            csvWriter.close();
                        } catch (IOException e1) {
                            // TODO Auto-generated catch block
                            e1.printStackTrace();
                        }
                    }
                    try {
                        DbUtilMySQL.getInstance().close();
                    } catch (SQLException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            } else {
                StatusPanel.progressCurrent.setMaximum(sqlList.size());
                int progressValue = 0;
                StatusPanel.progressCurrent.setValue(progressValue);
                for (String string : sqlList) {
                    logger.debug("sqls:" + string);
                    progressValue++;
                    StatusPanel.progressCurrent.setValue(progressValue);
                }

            }
        } else {
            StatusPanel.progressCurrent.setMaximum(1);
            StatusPanel.progressCurrent.setValue(1);
            StatusLog.setLastTime(Utils.getCurrentTime());
            StatusLog.setStatusDetail(PropertyUtil.getProperty("ds.logic.runSqlFinish") + totalSqls, LogLevel.INFO);
            StatusLog.setStatusDetail(PropertyUtil.getProperty("ds.logic.syncFinish"), LogLevel.INFO);
        }

        return isSuccess;

    }

    public void run() {
        StatusPanel.isRunning = true;
        this.setName("ExecuteThread");
        long enterTime = System.currentTimeMillis(); // ������
        StatusPanel.progressTotal.setMaximum(6);
        // ��ʼ������
        init();
        // ��������
        boolean isLinked = testLink();
        if (isLinked) {
            StatusPanel.progressTotal.setValue(1);
            // ���������ļ�
            boolean isAnalyseSuccess = analyseConfigFile();
            if (isAnalyseSuccess) {
                StatusPanel.progressTotal.setValue(2);
                // ����
                if ("true".equals(ConstantsTools.CONFIGER.getAutoBak())) {
                    backUp();
                }
                StatusPanel.progressTotal.setValue(3);
                // �����¿���
                boolean isSnapSuccess = newSnap();
                if (isSnapSuccess) {
                    StatusPanel.progressTotal.setValue(4);
                    // �Աȿ���,�����ݶԱȽ������SQL
                    boolean isDiffSuccess = diffSnap();
                    if (isDiffSuccess) {
                        StatusPanel.progressTotal.setValue(5);
                        // ִ��SQL
                        boolean isExecuteSuccess = executeSQL();
                        if (isExecuteSuccess) {
                            StatusPanel.progressTotal.setValue(6);

                            // �ָ���ť״̬
                            if (!StatusPanel.buttonStartSchedule.isEnabled()) {
                                StatusLog.setStatus(PropertyUtil.getProperty("ds.logic.runScheduleing"));
                            } else {
                                StatusLog.setStatus(PropertyUtil.getProperty("ds.logic.manuSyncFinish"));
                            }
                            StatusLog.setStatusDetail(PropertyUtil.getProperty("ds.logic.currentManuSyncFinish"), LogLevel.INFO);
                            // ���ó���ʱ��
                            long leaveTime = System.currentTimeMillis(); // ������
                            float minutes = (float) (leaveTime - enterTime) / 1000; // ����
                            StatusLog.setKeepTime(String.valueOf(minutes));
                            // ���óɹ�����+1
                            String success = String
                                    .valueOf((Long.parseLong(ConstantsTools.CONFIGER.getSuccessTime()) + 1));
                            StatusLog.setSuccess(success);
                        } else {
                            // �ָ����ձ���
                            SnapManage.recoverSnapBak();

                            String fail = String.valueOf((Long.parseLong(ConstantsTools.CONFIGER.getFailTime()) + 1));
                            StatusLog.setFail(fail);
                        }

                    } else {
                        // �ָ����ձ���
                        SnapManage.recoverSnapBak();

                        String fail = String.valueOf((Long.parseLong(ConstantsTools.CONFIGER.getFailTime()) + 1));
                        StatusLog.setFail(fail);
                    }

                } else {
                    // �ָ����ձ���
                    SnapManage.recoverSnapBak();

                    String fail = String.valueOf((Long.parseLong(ConstantsTools.CONFIGER.getFailTime()) + 1));
                    StatusLog.setFail(fail);
                }

            } else {
                String fail = String.valueOf((Long.parseLong(ConstantsTools.CONFIGER.getFailTime()) + 1));
                StatusLog.setFail(fail);
            }

        } else {
            String fail = String.valueOf((Long.parseLong(ConstantsTools.CONFIGER.getFailTime()) + 1));
            StatusLog.setFail(fail);
        }

        StatusPanel.buttonStartNow.setEnabled(true);
        StatusPanel.buttonStartSchedule.setEnabled(true);
        StatusPanel.isRunning = false;
    }
}
