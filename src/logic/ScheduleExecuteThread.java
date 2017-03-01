package logic;

import UI.panel.StatusPanel;
import tools.*;

/**
 * ��ʱ����ִ�������̳���ִ�����߳���
 *
 * @author Bob
 */
public class ScheduleExecuteThread extends ExecuteThread {

    public void run() {
        if (StatusPanel.isRunning == false) {
            StatusPanel.isRunning = true;
            this.setName("ScheduleExecuteThread");
            StatusPanel.buttonStartNow.setEnabled(false);
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

                                // ���ó���ʱ��
                                long leaveTime = System.currentTimeMillis(); // ������
                                float minutes = (float) (leaveTime - enterTime) / 1000; // ����
                                StatusLog.setKeepTime(String.valueOf(minutes));
                                // ���óɹ�����+1
                                String success = String
                                        .valueOf((Long.parseLong(ConstantsTools.CONFIGER.getSuccessTime()) + 1));
                                StatusLog.setSuccess(success);
                                StatusLog.setStatusDetail(PropertyUtil.getProperty("ds.logic.currentSyncFinish"), LogLevel.INFO);
                            } else {
                                // �ָ����ձ���
                                SnapManage.recoverSnapBak();

                                String fail = String
                                        .valueOf((Long.parseLong(ConstantsTools.CONFIGER.getFailTime()) + 1));
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
            // ������ʾ��һ��ִ��ʱ��
            StatusPanel.labelNextTime.setText(PropertyUtil.getProperty("ds.ui.schedule.nextTime") + Utils.getNextSyncTime());
            StatusPanel.isRunning = false;
        }
    }
}
