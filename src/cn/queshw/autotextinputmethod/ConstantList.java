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
	static final char MACRO_ESCAPECHARACTER = '%';

	// 编辑快捷皱键
	static final int EDIT_COPY = KeyEvent.KEYCODE_C;
	static final int EDIT_PASTE = KeyEvent.KEYCODE_V;
	static final int EDIT_CUT = KeyEvent.KEYCODE_X;

	static final int EDIT_SELECTALL = KeyEvent.KEYCODE_A;
	static final int EDIT_SELETETOHOME = KeyEvent.KEYCODE_Y;
	static final int EDIT_SELETEBACK = KeyEvent.KEYCODE_U;
	static final int EDIT_SELETEFORWARD = KeyEvent.KEYCODE_I;
	static final int EDIT_SELETETOEND = KeyEvent.KEYCODE_O;

	static final int EDIT_MOVETOHOME = KeyEvent.KEYCODE_H;
	static final int EDIT_MOVEBACK = KeyEvent.KEYCODE_J;
	static final int EDIT_MOVEFORWARD = KeyEvent.KEYCODE_K;
	static final int EDIT_MOVETOEND = KeyEvent.KEYCODE_L;

	static final int EDIT_DELETEALL = KeyEvent.KEYCODE_D;
	static final int EDIT_DELETETOHOME = KeyEvent.KEYCODE_B;
	static final int EDIT_DELETEFORWARD = KeyEvent.KEYCODE_N;
	static final int EDIT_DELETETOEND = KeyEvent.KEYCODE_M;
	//static final int EDIT_DELETETWORD = KeyEvent.KEYCODE_W;

	static final int EDIT_UNDO = KeyEvent.KEYCODE_Z;
	//static final int EDIT_REDO = KeyEvent.KEYCODE_R;

	static final int SWITCH_INPUTMETHOD = KeyEvent.KEYCODE_ENTER;// 与ＡＬt键搭配用于切换输入法

	// //////////////////////////////////////////////////////////////////////////
	/*
	 * 处理以下转义符，在输出时转为正常的字符 #SINGLE_QUOTATION# ' #SHARP# # #COMMA# ,
	 */
	public static String escape(String str) {//所有导入都需要应用此函数
		str = str.trim();
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			if (c == '\'')
				s.append("#SINGLE_QUOTATION#");
			else if (c == ',')
				s.append("#COMMA#");
			else if (c == '#')
				s.append("#SHARP#");
			else
				s.append(c);
		}
		return s.toString();
	}
	
	
	public static String recover(String str) {//所有从数据库中取值都需要应用此函数
		StringBuilder s = new StringBuilder();
		String[] item = str.split("#");
		for (int i = 0; i < item.length; i++) {
			if (item[i].equals("SINGLE_QUOTATION"))
				s.append("'");
			else if (item[i].equals("SHARP"))
				s.append("#");
			else if (item[i].equals("COMMA"))
				s.append(",");
			else
				s.append(item[i]);
		}
		return s.toString();
	}

}

