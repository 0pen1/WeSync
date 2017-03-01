package logic.init;

import UI.panel.StatusPanel;
import com.opencsv.CSVWriter;
import tools.DbUtilMySQL;
import tools.DbUtilSQLServer;
import tools.LogLevel;
import tools.StatusLog;

import java.io.File;

public class Init4pxp2p {

    public static boolean init() {
        StatusLog.setStatusDetail("��ʼ��ʼ����һ�ο��գ������ĵȴ�����", LogLevel.INFO);

        boolean isSuccess = true;
        DbUtilMySQL mySql = DbUtilMySQL.getInstance();
        DbUtilSQLServer SQLServer = DbUtilSQLServer.getInstance();
        CSVWriter csvWriterRole = null;
        CSVWriter csvWriterUser = null;
        File snapsDir = null;
        StatusPanel.progressCurrent.setMaximum(7);
        int progressValue = 0;
        StatusPanel.progressCurrent.setValue(progressValue);

		/*Do Sth you need to init*/
        return isSuccess;
    }

}
