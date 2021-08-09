package cn.queshw.autotextsetting;

public class MethodItem {
	public static final int DEFAULT = 0;//如果是默认输入法
	public static final int NOTDEFAULT = 1;//如果不是默认输入法
	
	private int id;
	private String name;
	private int isDefault;
	
	////////////////////////////////////////////
	//构造函数
	public MethodItem() {
	}
	
	/////////////////////////////////////////////
	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the isDefault
	 */
	public int getIsDefault() {
		return isDefault;
	}

	/**
	 * @param isDefault the isDefault to set
	 */
	public void setIsDefault(int isDefault) {
		this.isDefault = isDefault;
	}



}
