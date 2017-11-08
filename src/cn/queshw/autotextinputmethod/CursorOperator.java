package cn.queshw.autotextinputmethod;

import android.view.inputmethod.InputConnection;

public class CursorOperator {

	private int FROMEND;
	private int FROMSTART;
	InputConnection mConnection;

	public CursorOperator(InputConnection mConnection) {
		this.mConnection = mConnection;
	}

	// 取得当前光标后还有多少文本
	public int getAfterLength() {
		int step = 50;
		int result = 0;
		for (int i = 1; true; i++) {
			CharSequence tSequence = mConnection.getTextAfterCursor(step * i, 0);
			if (tSequence.length() < step * i) {
				result = tSequence.length();
				break;
			}
		}
		return result;
	}

	// 获取字串中有多少回车或换行符
	int getInvisibleCharsNumber(CharSequence charSequence) {
		int result = 0;
		if (charSequence == "" || charSequence == null) {
			return 0;
		}
		for (int i = 1; i <= charSequence.length(); ++i) {
			if (charSequence.charAt(charSequence.length() - i) == '\n' || charSequence.charAt(charSequence.length() - i) == '\r') {
				result++;
			}
		}
		return result;
	}
	
	//获取本行光标后字串
	public CharSequence getToLineEnd(int mFromWhichEnd) {
		CharSequence result = "";
		return result;
	}

	//获取本行光标前字串
	public CharSequence getToLineStart(int mFromWhichEnd) {
		CharSequence result = "";
		return result;
	}
	
	//获取光标前一行的字串
	CharSequence getPreLine(int mFromWhichEnd) {
		CharSequence result = "";
		return result;
	}
	
	//获取光标下一行的字串
	CharSequence getNextLine(int mFromWhichEnd) {
		CharSequence result = "";
		return result;
	}

}
