package cn.queshw.autotextsetting;

public class RawItem {
	private int id;
	private String code;//替换前的字串
	private String candidate;//替换后的字串
	private int twolevel;

	public RawItem() {
		twolevel = 0;
	}
	public RawItem(int id, String code, String candidate, int twolevel) {
		this.id = id;
		this.code = code;
		this.candidate = candidate;
		this.twolevel = twolevel;
	}

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
	 * @return the input
	 */
	public String getCode() {
		return code;
	}

	/**
	 * @param input the input to set
	 */
	public void setCode(String code) {
		this.code = code;
	}

	/**
	 * @return the autotext
	 */
	public String getCandidate() {
		return candidate;
	}

	/**
	 * @param autotext the autotext to set
	 */
	public void setCandidate(String candicate) {
		this.candidate = candicate;
	}
	
	public int getTwolevel(){
		return twolevel;
	}
	
	public void setTwolevel(int twolevel){
		this.twolevel = twolevel;
	}

}
