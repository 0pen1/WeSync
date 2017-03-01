package logic;

import UI.panel.StatusPanel;
import com.opencsv.CSVWriter;
import logic.bean.Table;
import logic.init.Init4pxp2p;
import tools.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;

/**
 * ���չ�����
 * 
 * @author Bob
 *
 */
public class SnapManage {

	public static ArrayList<String> sqlList;

	/**
	 * ��������
	 * 
	 * @param tableMap����Դ��Map
	 * @return
	 */
	public static boolean createSnap(Map<String, Table> tableMap) {
		boolean isSuccess = true;

		// ��������ļ��в����ڣ�����г�ʼ��
		File snapsDir = new File(ConstantsLogic.SNAPS_DIR);
		File snapsBakDir = new File(ConstantsLogic.SNAPS_BAK_DIR);
		if (!snapsDir.exists()) {
			boolean isInitSuccess = Init4pxp2p.init();
			if (!isInitSuccess) {
				return false;
			}
		}
		if (!snapsBakDir.exists()) {
			snapsBakDir.mkdirs();
		}
		// ���ݵ�ǰ�����ļ��У��Ա��������ʧ��ʱ�ָ�
		try {
			// ��տ��ձ����ļ���
			FileUtils.clearDirectiory(ConstantsLogic.SNAPS_BAK_DIR);
			// ���������ձ����ļ���
			FileUtils.copyDirectiory(ConstantsLogic.SNAPS_DIR, ConstantsLogic.SNAPS_BAK_DIR);
		} catch (IOException e1) {
			isSuccess = false;

			StatusLog.setStatusDetail(PropertyUtil.getProperty("ds.logic.currentSnapDirBackFail") + e1.toString(), LogLevel.ERROR);
			e1.printStackTrace();
			return isSuccess;
		}
		StatusPanel.progressCurrent.setMaximum(tableMap.keySet().size());
		int progressValue = 0;
		StatusPanel.progressCurrent.setValue(progressValue);

		// ������Դ��Map����������
		for (String snapName : tableMap.keySet()) {
			if (!"".equals(snapName.trim())) {

				// ��һ�ο���
				File snapBefore = new File(ConstantsLogic.SNAPS_DIR + File.separator + snapName + "_before.csv");
				// ��ǰ����
				File snapNow = new File(ConstantsLogic.SNAPS_DIR + File.separator + snapName + ".csv");

				// �ж���һ�εĿ����Ƿ���ڣ���������ڣ����½�һ���յ�
				if (!snapBefore.exists()) {
					try {
						snapBefore.createNewFile();
					} catch (IOException e) {
						StatusLog.setStatusDetail(PropertyUtil.getProperty("ds.logic.ifLastSnapExistFail") + e.toString(), LogLevel.ERROR);
						e.printStackTrace();
					}
				} else {
					// ɾ��ԭ���գ�����ǰ���ձ��ԭ����
					snapBefore.delete();
					snapNow.renameTo(new File(ConstantsLogic.SNAPS_DIR + File.separator + snapName + "_before.csv"));
				}
				// ������ǰ���գ��Ա���ԭ���նԱ�
				// �¿����ļ�
				CSVWriter csvWriter = null;
				DbUtilSQLServer sqlServer = null;
				try {
					snapNow.createNewFile();
					Writer writer = new FileWriter(snapNow);
					csvWriter = new CSVWriter(writer, ',');

					sqlServer = DbUtilSQLServer.getInstance();// ��ȡSQLServer����ʵ��

					String querySql = "SELECT " + tableMap.get(snapName).getFields() + " FROM "
							+ tableMap.get(snapName).getTableName();
					if (tableMap.get(snapName).getOther() != null) {
						querySql += (" " + tableMap.get(snapName).getOther());
					}
					ResultSet rs = sqlServer.executeQuery(querySql);// ���ѯ
					ResultSetMetaData m = null;// ��ȡ����Ϣ
					m = rs.getMetaData();

					List<String> list = new ArrayList<String>();

					int columns = m.getColumnCount();
					// ��ı�ͷ
					for (int i = 1; i <= columns; i++) {
						list.add(m.getColumnName(i));
					}

					int size = list.size();
					String[] arr = (String[]) list.toArray(new String[size]);
					csvWriter.writeNext(arr);
					// �������
					while (rs.next()) {
						list.clear();
						for (int i = 1; i <= columns; i++) {
							list.add(rs.getString(i));
						}

						size = list.size();
						String[] arr2 = (String[]) list.toArray(new String[size]);
						csvWriter.writeNext(arr2);
					}

				} catch (IOException e) {
					StatusLog.setStatusDetail(PropertyUtil.getProperty("ds.logic.newSnapFileFail") + e.toString(), LogLevel.ERROR);
					e.printStackTrace();
				} catch (SQLException e) {
					StatusLog.setStatusDetail(PropertyUtil.getProperty("ds.logic.newSnapFileFail") + e.toString(), LogLevel.ERROR);
					e.printStackTrace();
				} finally {
					if (csvWriter != null) {
						try {
							csvWriter.close();
						} catch (IOException e) {
							StatusLog.setStatusDetail(PropertyUtil.getProperty("ds.logic.newSnapFileFail") + e.toString(), LogLevel.ERROR);
							e.printStackTrace();
						}
					}
					if (sqlServer != null) {
						try {
							sqlServer.close();
						} catch (SQLException e) {
							StatusLog.setStatusDetail(PropertyUtil.getProperty("ds.logic.newSnapFileFail") + e.toString(), LogLevel.ERROR);
							e.printStackTrace();
						}
					}

				}

			}
			progressValue++;
			StatusPanel.progressCurrent.setValue(progressValue);
		}

		return isSuccess;
	}

	/**
	 * �Աȿ���
	 * 
	 * @param tableMap����Դ��Map
	 * @return
	 */
	public static boolean diffSnap(Map<String, Table> tableMap) {
		StatusLog.setStatusDetail(PropertyUtil.getProperty("ds.logic.startDiffSnap"), LogLevel.INFO);
		StatusPanel.progressCurrent.setMaximum(tableMap.keySet().size());
		int progressValue = 0;
		StatusPanel.progressCurrent.setValue(progressValue);
		boolean isSuccess = true;
		// ��ʼ��Ŀ��sqlList
		sqlList = new ArrayList<String>();
		// ������Դ��Map���Աȿ���
		for (String snapName : tableMap.keySet()) {
			if (!"".equals(snapName.trim())) {
				// ��һ�ο���
				File snapBefore = new File(ConstantsLogic.SNAPS_DIR + File.separator + snapName + "_before.csv");
				// ��ǰ����
				File snapNow = new File(ConstantsLogic.SNAPS_DIR + File.separator + snapName + ".csv");
				// MD5�ȶ������ļ�
				String snapMD5Before = FileUtils.getFileMD5(snapBefore);
				String snapMD5Now = FileUtils.getFileMD5(snapNow);
				if (snapMD5Before == null) {
					StatusLog.setStatusDetail(PropertyUtil.getProperty("ds.logic.beforeSnapMd5Fail") + snapBefore.getName(), LogLevel.ERROR);
					return false;
				}
				if (snapMD5Now == null) {
					StatusLog.setStatusDetail(PropertyUtil.getProperty("ds.logic.nowSnapMd5Fail") + snapNow.getName(), LogLevel.ERROR);
					return false;
				}
				if (snapMD5Before.equals(snapMD5Now)) {
					progressValue++;
					StatusPanel.progressCurrent.setValue(progressValue);
					continue;
				} else {
					try {
						isSuccess = diffCsvLineByLine(snapBefore, snapNow);
					} catch (Exception e) {
						StatusLog.setStatusDetail(PropertyUtil.getProperty("ds.logic.lineByLineDiffFail") + e.toString(), LogLevel.ERROR);
						e.printStackTrace();
						return false;
					} finally {
						if (TriggerManage.SQLServer != null) {
							try {
								TriggerManage.SQLServer.close();
							} catch (SQLException e) {
								StatusLog.setStatusDetail(PropertyUtil.getProperty("ds.logic.lineByLineDiffFail") + e.toString(), LogLevel.ERROR);
								e.printStackTrace();
							}
						}
					}
				}
			}
			progressValue++;
			StatusPanel.progressCurrent.setValue(progressValue);
		}
		StatusLog.setStatusDetail(PropertyUtil.getProperty("ds.logic.finishDiffSnap"), LogLevel.INFO);
		return isSuccess;
	}

	/**
	 * �ָ����ձ���
	 */
	public static void recoverSnapBak() {

		// ��տ����ļ���
		FileUtils.clearDirectiory(ConstantsLogic.SNAPS_DIR);
		// �������ձ��ݵ������ļ���
		try {
			FileUtils.copyDirectiory(ConstantsLogic.SNAPS_BAK_DIR, ConstantsLogic.SNAPS_DIR);
		} catch (IOException e) {
			StatusLog.setStatusDetail(PropertyUtil.getProperty("ds.logic.restoreSnapBackFail") + e.toString(), LogLevel.ERROR);
			e.printStackTrace();
		}
	}

	/**
	 * ���жԱ�csv�ļ�
	 * 
	 * @param snapBefore
	 * @param snapNow
	 * @throws IOException
	 */
	public static boolean diffCsvLineByLine(File snapBefore, File snapNow) throws Exception {

		boolean isSuccess = true;

		ArrayList<String[]> snapBeforeList = FileUtils.getCsvFileContentList(snapBefore);
		ArrayList<String[]> snapNowList = FileUtils.getCsvFileContentList(snapNow);

		// ȥ��ĩβ���У�����еĻ�
		// if (snapBeforeList.get(snapBeforeList.size() - 1)[0] == null) {
		// snapBeforeList.remove(snapBeforeList.size() - 1);
		// }
		//
		// if (snapNowList.get(snapNowList.size() - 1)[0] == null) {
		// snapNowList.remove(snapNowList.size() - 1);
		// }

		// ��ȡ�ÿ�����
		String snapName = snapNow.getName().substring(0, snapNow.getName().indexOf(".csv"));

		// ��ȡ��ͷ
		String headerBefore[];
		String headerNow[] = snapNowList.get(0);
		if (snapBeforeList.size() == 0) {
			// ���ԭ�����ǿյģ�˵���ǵ�һ�ν��п��գ����Խ�ԭ���ռ�һ���¿��ձ�ͷ����
			headerBefore = headerNow;
		} else {
			headerBefore = snapBeforeList.get(0);
		}

		// �Աȱ�ͷ������ṹ�Ƿ��б�
		// Arrays.sort(headerBefore);
		// Arrays.sort(headerNow);
		if (!Arrays.equals(headerBefore, headerNow)) {
			StatusLog.setStatusDetail(snapName + PropertyUtil.getProperty("ds.logic.tableChanged"), LogLevel.ERROR);
			return false;
		}

		// ��ȡ����
		String primKeys = ExecuteThread.originalTablesMap.get(snapName).getPrimKey();
		String primKeysArr[] = primKeys.split(",");

		// ��ȡ����index
		int prinKeyIndex[] = new int[primKeysArr.length];
		for (int i = 0; i < primKeysArr.length; i++) {
			prinKeyIndex[i] = Utils.getStrArrIndex(headerNow, primKeysArr[i]);
		}

		// �����Ƚ�+��������+�α���Ʒ�

		// ��ȡԭ���պ��¿���������ֵSet,�������ֵ�ö��ŷָ�
		LinkedHashSet<String> primKeyValuesSetBefore = new LinkedHashSet<String>();
		LinkedHashSet<String> primKeyValuesSetNow = new LinkedHashSet<String>();
		for (int i = 0; i < snapBeforeList.size(); i++) {
			String recordsLineBefore[] = snapBeforeList.get(i);
			StringBuffer keyValues = new StringBuffer();
			for (int j = 0; j < primKeysArr.length; j++) {
				keyValues.append(recordsLineBefore[prinKeyIndex[j]]);
				if (j < primKeysArr.length - 1) {
					keyValues.append(",");
				}
			}
			primKeyValuesSetBefore.add(keyValues.toString());
		}
		for (int i = 0; i < snapNowList.size(); i++) {
			String recordsLineNow[] = snapNowList.get(i);
			StringBuffer keyValues = new StringBuffer();
			for (int j = 0; j < primKeysArr.length; j++) {
				keyValues.append(recordsLineNow[prinKeyIndex[j]]);
				if (j < primKeysArr.length - 1) {
					keyValues.append(",");
				}
			}
			primKeyValuesSetNow.add(keyValues.toString());
		}
		if (snapBeforeList.size() > snapNowList.size()) {
			StatusPanel.progressCurrent.setMaximum(snapBeforeList.size());
		} else {
			StatusPanel.progressCurrent.setMaximum(snapNowList.size());
		}
		int progressValue = 0;
		StatusPanel.progressCurrent.setValue(progressValue);

		for (int flagBefore = 1, flagNow = 1; flagBefore < snapBeforeList.size() || flagNow < snapNowList.size();) {

			if (!(snapNowList.size() > flagNow)) {
				// ˵���¿��յ��˽�β������û��������
				String recordsLineBefore[] = snapBeforeList.get(flagBefore);

				// ����ԭ���ո�����¼����map��key:��������value:����ֵ
				Map<String, String> primKeyAndValueMapBefore = new LinkedHashMap<>();
				for (int j = 0; j < primKeysArr.length; j++) {
					primKeyAndValueMapBefore.put(primKeysArr[j], recordsLineBefore[prinKeyIndex[j]]);
				}

				StringBuffer primKeyValuesBefore = new StringBuffer();
				int k = 0;
				for (String key : primKeyAndValueMapBefore.keySet()) {
					primKeyValuesBefore.append(primKeyAndValueMapBefore.get(key));
					if (k < primKeyAndValueMapBefore.keySet().size() - 1) {
						primKeyValuesBefore.append(",");
					}
					k++;
				}

				boolean isNowContainsBefore = primKeyValuesSetNow.contains(primKeyValuesBefore.toString());

				if (!isNowContainsBefore) {
					// ��������map��key:��������value:����ֵ
					Map<String, String> primKeyAndValueMap = new HashMap<>();

					for (k = 0; k < primKeysArr.length; k++) {
						primKeyAndValueMap.put(primKeysArr[k], recordsLineBefore[prinKeyIndex[k]]);
					}

					String sql = TriggerManage.getSqlDelete(snapName, primKeyAndValueMap, headerNow, recordsLineBefore);
					sqlList.add(sql);
					StatusLog.setStatusDetail(sql, LogLevel.DEBUG);
				}

				flagBefore++;
			} else if (!(snapBeforeList.size() > flagBefore)) {
				// ˵��ԭ���յ��˽�β������û��������
				String recordsLineNow[] = snapNowList.get(flagNow);
				// �����¿��ո�����¼����map��key:��������value:����ֵ
				Map<String, String> primKeyAndValueMapNow = new LinkedHashMap<>();
				for (int j = 0; j < primKeysArr.length; j++) {
					primKeyAndValueMapNow.put(primKeysArr[j], recordsLineNow[prinKeyIndex[j]]);
				}

				// ���¼�¼������ֵ��ԭ��������ֵset���Ƿ����
				StringBuffer primKeyValuesNow = new StringBuffer();
				int k = 0;
				for (String key : primKeyAndValueMapNow.keySet()) {
					primKeyValuesNow.append(primKeyAndValueMapNow.get(key));
					if (k < primKeyAndValueMapNow.keySet().size() - 1) {
						primKeyValuesNow.append(",");
					}
					k++;
				}
				boolean isBeforeContainsNow = primKeyValuesSetBefore.contains(primKeyValuesNow.toString());
				if (!isBeforeContainsNow) {
					// ��������map��key:��������value:����ֵ
					Map<String, String> primKeyAndValueMap = new HashMap<>();
					for (k = 0; k < primKeysArr.length; k++) {
						primKeyAndValueMap.put(primKeysArr[k], recordsLineNow[prinKeyIndex[k]]);
					}

					String sql = TriggerManage.getSqlInsert(snapName, primKeyAndValueMap, headerNow, recordsLineNow);
					sqlList.add(sql);
					StatusLog.setStatusDetail(sql, LogLevel.DEBUG);
				}

				flagNow++;
			} else {

				String recordsLineBefore[] = snapBeforeList.get(flagBefore);
				String recordsLineNow[] = snapNowList.get(flagNow);

				// ����ԭ���ո�����¼����map��key:��������value:����ֵ
				Map<String, String> primKeyAndValueMapBefore = new LinkedHashMap<>();
				for (int j = 0; j < primKeysArr.length; j++) {
					primKeyAndValueMapBefore.put(primKeysArr[j], recordsLineBefore[prinKeyIndex[j]]);
				}
				// �����¿��ո�����¼����map��key:��������value:����ֵ
				Map<String, String> primKeyAndValueMapNow = new LinkedHashMap<>();
				for (int j = 0; j < primKeysArr.length; j++) {
					primKeyAndValueMapNow.put(primKeysArr[j], recordsLineNow[prinKeyIndex[j]]);
				}

				if (Arrays.equals(recordsLineNow, recordsLineBefore)) {
					// ����ȫһ�£��������һ��
					flagBefore++;
					flagNow++;
					continue;
				} else {
					// �ȿ�ԭ���ո�����¼���¿��ո�����¼�����Ƿ�һ��
					boolean isPrimKeyTheSame = Utils.mapCompare4PrimKey(primKeyAndValueMapBefore,
							primKeyAndValueMapNow);

					if (isPrimKeyTheSame) {
						// ��һ�£�˵��������¼�������޸ģ���Ҫ����Update
						// ͨ��Trigger����UpdateSQL
						String sql = TriggerManage.getSqlUpdate(snapName, primKeyAndValueMapNow, headerNow,
								recordsLineBefore, recordsLineNow);
						sqlList.add(sql);
						StatusLog.setStatusDetail(sql, LogLevel.DEBUG);
					} else {
						// ����һ�£���ԭ��¼������ֵ���¿�������ֵset���Ƿ����
						StringBuffer primKeyValuesBefore = new StringBuffer();
						int k = 0;
						for (String key : primKeyAndValueMapBefore.keySet()) {
							primKeyValuesBefore.append(primKeyAndValueMapBefore.get(key));
							if (k < primKeyAndValueMapBefore.keySet().size() - 1) {
								primKeyValuesBefore.append(",");
							}
							k++;
						}

						// ���¼�¼������ֵ��ԭ��������ֵset���Ƿ����
						StringBuffer primKeyValuesNow = new StringBuffer();
						k = 0;
						for (String key : primKeyAndValueMapNow.keySet()) {
							primKeyValuesNow.append(primKeyAndValueMapNow.get(key));
							if (k < primKeyAndValueMapNow.keySet().size() - 1) {
								primKeyValuesNow.append(",");
							}
							k++;
						}
						boolean isNowContainsBefore = primKeyValuesSetNow.contains(primKeyValuesBefore.toString());
						boolean isBeforeContainsNow = primKeyValuesSetBefore.contains(primKeyValuesNow.toString());

						if (isNowContainsBefore && !isBeforeContainsNow) {
							// ˵�������˼�¼���򴥷�Insert
							// ͨ��Trigger����InsertSQL
							String sql = TriggerManage.getSqlInsert(snapName, primKeyAndValueMapNow, headerNow,
									recordsLineNow);
							sqlList.add(sql);
							StatusLog.setStatusDetail(sql, LogLevel.DEBUG);

							// ���
							String recordsLineNowTemp[] = snapNowList.get(
									Utils.getIndexInLinkedHashSet(primKeyValuesSetNow, primKeyValuesBefore.toString()));
							if (!Arrays.equals(recordsLineNowTemp, recordsLineBefore)) {
								// ������ȫһ�£���Update
								sql = TriggerManage.getSqlUpdate(snapName, primKeyAndValueMapBefore, headerNow,
										recordsLineBefore, recordsLineNowTemp);
								sqlList.add(sql);
								snapNowList.set(Utils.getIndexInLinkedHashSet(primKeyValuesSetNow,
										primKeyValuesBefore.toString()), recordsLineBefore);
								StatusLog.setStatusDetail(sql, LogLevel.DEBUG);
							}
						} else if (!isNowContainsBefore && isBeforeContainsNow) {
							// ˵��ԭ��¼��ɾ���ˣ��򴥷�Delete
							// ͨ��Trigger����DeleteSQL
							String sql = TriggerManage.getSqlDelete(snapName, primKeyAndValueMapBefore, headerNow,
									recordsLineBefore);
							sqlList.add(sql);
							StatusLog.setStatusDetail(sql, LogLevel.DEBUG);

							// �ұ�
							String recordsLineBeforeTemp[] = snapBeforeList.get(
									Utils.getIndexInLinkedHashSet(primKeyValuesSetBefore, primKeyValuesNow.toString()));
							if (!Arrays.equals(recordsLineNow, recordsLineBeforeTemp)) {
								// ������ȫһ�£���Update
								sql = TriggerManage.getSqlUpdate(snapName, primKeyAndValueMapNow, headerNow,
										recordsLineBeforeTemp, recordsLineNow);
								sqlList.add(sql);
								snapBeforeList.set(Utils.getIndexInLinkedHashSet(primKeyValuesSetBefore,
										primKeyValuesNow.toString()), recordsLineNow);
								StatusLog.setStatusDetail(sql, LogLevel.DEBUG);
							}
						} else if (isNowContainsBefore && isBeforeContainsNow) {
							// ˵����������¼���п��ܱ������ˣ���Ҫ�����ó����Ƚ�һ��

							// ���
							String recordsLineNowTemp[] = snapNowList.get(
									Utils.getIndexInLinkedHashSet(primKeyValuesSetNow, primKeyValuesBefore.toString()));
							if (!Arrays.equals(recordsLineNowTemp, recordsLineBefore)) {
								// ������ȫһ�£���Update
								String sql = TriggerManage.getSqlUpdate(snapName, primKeyAndValueMapBefore, headerNow,
										recordsLineBefore, recordsLineNowTemp);
								sqlList.add(sql);
								snapNowList.set(Utils.getIndexInLinkedHashSet(primKeyValuesSetNow,
										primKeyValuesBefore.toString()), recordsLineBefore);
								// snapBeforeList.remove(flagBefore);
								StatusLog.setStatusDetail(sql, LogLevel.DEBUG);
							}

							// �ұ�
							String recordsLineBeforeTemp[] = snapBeforeList.get(
									Utils.getIndexInLinkedHashSet(primKeyValuesSetBefore, primKeyValuesNow.toString()));
							if (!Arrays.equals(recordsLineNow, recordsLineBeforeTemp)) {
								// ������ȫһ�£���Update
								String sql = TriggerManage.getSqlUpdate(snapName, primKeyAndValueMapNow, headerNow,
										recordsLineBeforeTemp, recordsLineNow);
								sqlList.add(sql);
								snapBeforeList.set(Utils.getIndexInLinkedHashSet(primKeyValuesSetBefore,
										primKeyValuesNow.toString()), recordsLineNow);
								// snapNowList.remove(flagNow);
								StatusLog.setStatusDetail(sql, LogLevel.DEBUG);
							}

						} else if (!isNowContainsBefore && !isBeforeContainsNow) {
							// ��˵��ԭ��¼��ɾ���ˣ��򴥷�Delete
							// ��˵���¼�¼�������ˣ��򴥷�Insert
							// ͨ��Trigger����SQL
							String sql = TriggerManage.getSqlDelete(snapName, primKeyAndValueMapBefore, headerNow,
									recordsLineBefore);
							sqlList.add(sql);
							StatusLog.setStatusDetail(sql, LogLevel.DEBUG);

							sql = TriggerManage.getSqlInsert(snapName, primKeyAndValueMapNow, headerNow,
									recordsLineNow);
							sqlList.add(sql);
							StatusLog.setStatusDetail(sql, LogLevel.DEBUG);
						}
					}
					flagBefore++;
					flagNow++;
				}
			}
			progressValue++;
			StatusPanel.progressCurrent.setValue(progressValue);
		}
		return isSuccess;
	}

}
