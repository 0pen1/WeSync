package logic.bean;

/**
 * ��object��
 * 
 * @author Bob
 *
 */
public class Table {
	// ����
	private String tableName;
	// ����
	private String primKey;
	// �ֶ�
	private String fields;

	// ��������/����
	private String other;

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public String getPrimKey() {
		return primKey;
	}

	public void setPrimKey(String primKey) {
		this.primKey = primKey;
	}

	public String getFields() {
		return fields;
	}

	public void setFields(String fields) {
		this.fields = fields;
	}

	public String getOther() {
		return other;
	}

	public void setOther(String other) {
		this.other = other;
	}

}
