package cn.queshw.autotextinputmethod;


//用于管理单词替换过程中的几个变量
final class Autotext {

	int start;
	int end;
	String beforeString;
	String afterString;
	
	public Autotext() {
		start = -1;
		end = -1;
		beforeString = "";
		afterString = "";
	}

	public void clear() {
		start = -1;
		end = -1;
		beforeString = "";
		afterString = "";
	}
}
