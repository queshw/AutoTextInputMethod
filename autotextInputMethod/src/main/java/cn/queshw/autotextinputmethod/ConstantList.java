package cn.queshw.autotextinputmethod;

import android.view.KeyEvent;

public class ConstantList {
	//本输入法在系统中的id
	static final String METHODID = "cn.queshw.autotextinputmethod/.AutotextInputMethod";
	
	// 特殊字符
	static final int SUBSTITUTION_TRIGGER = KeyEvent.KEYCODE_SPACE;// 正向替换触发字符
	static final int SUBSTITUTION_TRIGGER_REVERSE = KeyEvent.KEYCODE_DEL;// 反向替换触发字符
	static final int SUBSTITUTION_ENTER = KeyEvent.KEYCODE_ENTER;//回车字符
	static final int SUBSTITUTION_NUMPAD_ENTER = KeyEvent.KEYCODE_NUMPAD_ENTER;
	static final char SUBSTITUTION_SEPERRATOR = ' ';// 替换分隔符

	// 宏命令
	static final char MACRO_DELETEBACK = 'b';
	static final char MACRO_DELETEFORWARD = 'B';
	static final char MACRO_DELETEWORD = 'w';
	static final char MACRO_DATE = 'd';
	static final char MACRO_LONGDATE = 'D';
	static final char MACRO_TIME = 't';
	static final char MACRO_MACROCHARACTER = '%';

	// 编辑快捷皱键
	static final int EDIT_COPY = KeyEvent.KEYCODE_C;
	static final int EDIT_PASTE = KeyEvent.KEYCODE_V;
	static final int EDIT_CUT = KeyEvent.KEYCODE_X;
	static final int EDIT_UNDO = KeyEvent.KEYCODE_Z;
	
	static final int EDIT_SELECTMODEL = KeyEvent.KEYCODE_S;
	static final int EDIT_SELECTALL = KeyEvent.KEYCODE_A;
	static final int EDIT_SELECTLINE = KeyEvent.KEYCODE_H;
	
	static final int EDIT_UP = KeyEvent.KEYCODE_I;
    static final int EDIT_DOWN = KeyEvent.KEYCODE_K;
    static final int EDIT_BACK = KeyEvent.KEYCODE_J;
    static final int EDIT_FORWARD = KeyEvent.KEYCODE_L;    

    static final int EDIT_TOLINESTART = KeyEvent.KEYCODE_U;
    static final int EDIT_TOLINEEND = KeyEvent.KEYCODE_O;    
    static final int EDIT_TOSTART = KeyEvent.KEYCODE_Y;
    static final int EDIT_TOEND = KeyEvent.KEYCODE_P;
	
	static final int EDIT_DELETEALL = KeyEvent.KEYCODE_D;
	static final int EDIT_DELETEFORWARD = KeyEvent.KEYCODE_N;
	static final int EDIT_DELETELINE = KeyEvent.KEYCODE_M;
    	
	static final int SWITCH_INPUTMETHOD = KeyEvent.KEYCODE_ENTER;// 与ＡＬt键搭配用于切换输入法

	// //////////////////////////////////////////////////////////////////////////
	/*
	 * 处理select时的特殊字符
	 */
	public static String searchEscape(String str) {//所有导入都需要应用此函数
		str = str.trim();
		str = str.replace("/", "//");
		str = str.replace("'", "''");
		str = str.replace("[", "/[");
		str = str.replace("]", "/]");
		str = str.replace("%", "/%");
		str = str.replace("&","/&");
		str = str.replace("_", "/_");
		str = str.replace("(", "/(");
		str = str.replace(")", "/)");
		return str;
	}

	///////////////////////////////////////////////////////////////////////
	//处理特殊字符，在insert 或者 update 语句中，把单此号转为两个单引号
	public static String insertUpdateEscape(String str) {
		str = str.trim();
		str = str.replace("'", "''");
		return str;
	}

	//替换两个特殊字符 逗号和百分号
	public static String commaPercentEscape(String str){
		str = str.trim();
		str = str.replace("%", "%p");
		str = str.replace(",", "%c");
		return str;
	}

	//恢复两个特殊字符 逗号和百分号
	public static String commaPercentRecover(String str){
		str = str.trim();
		str = str.replace("%c", ",");
		str = str.replace("%p", "%");
		return str;
	}
}

