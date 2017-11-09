package cn.queshw.autotextinputmethod;

import android.view.inputmethod.InputConnection;

public class CursorOperator {

	private int FROMEND = 1;
	private int FROMSTART = 0;
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
		if (charSequence == "" || charSequence == null)
			return 0;
		for (int i = 1; i <= charSequence.length(); ++i) {
			if (charSequence.charAt(charSequence.length() - i) == '\n' || charSequence.charAt(charSequence.length() - i) == '\r') {
				result++;
			}
		}
		return result;
	}

	// 获取本行光标前字串
	public CharSequence getToLineStart(int mFromWhichEnd) {
		CharSequence result = "";
		if (mFromWhichEnd == FROMEND) {// 如果从结束的光标算起
			result = mConnection.getSelectedText(0);
			if (result.equals(""))
				result = "";// 如果什么都没有取到，则把结果置为空字串
		}
		int len = result.length();
		int len2 = 0;
		for (int i = 1; true; i++) {
			if (i <= len) {// 如果循环的次数还小于已经选择的字符个数，则先在这个字串中取字符
				result = result.subSequence(len - i, len);
			} else {// 要在光标前取新的字符了
				result = mConnection.getTextBeforeCursor(i - len, 0).toString() + result.toString();
			}
			// 接下来是退出条件
			len2 = result.length();// 再次计算长度，因为 result的值可能已经变了
			if (len2 < i)
				break;// 表示已经取到头了
			if (result.charAt(0) == '\n') {// 表示已经取到上一行的换行符了，则去掉最头上的这个换行符就是结果
				result = result.subSequence(1, len2);
				break;
			}
		}

		// if (mFromWhichEnd == FROMSTART) {// 如果从开始的光标算起
		// for (int i = 1; true; i++) {
		// result = mConnection.getTextBeforeCursor(i, 0);
		// if (result.equals(""))
		// return "";// 如果一个字符都取不到，说明光标的最头上，直接返回空字串
		// // 接下来是退出条件
		// int len = result.length();
		// if (len < i)
		// break;// 已经取到头，还没有取到一个换行符，说明光标就在第一行上
		// if (result.charAt(0) == '\n') {// 如果取到了换行符，说明已经取到上一行的尾巴
		// result = result.subSequence(1, len);
		// break;
		// }
		// }
		// } else if (mFromWhichEnd == FROMEND) {// 如果从结束的光标算起
		// result = mConnection.getSelectedText(0);
		// if (result.equals(""))
		// result = "";// 如果什么都没有取到，则把结果置为空字串
		// int len = result.length();
		// for (int i = 1; true; i++) {
		// if (i <= len) {// 如果循环的次数还小于已经选择的字符个数，则先在这个字串中取字符
		// result = result.subSequence(len - i, len);
		// } else {// 要在光标前取新的字符了
		// result = mConnection.getTextBeforeCursor(i - len, 0).toString() +
		// result.toString();
		// }
		// // 接下来是退出条件
		// len = result.length();// 再次计算长度，因为 result的值可能已经变了
		// if (len < i)
		// break;// 表示已经取到头了
		// if (result.charAt(0) == '\n') {// 表示已经取到上一行的换行符了，则去掉最头上的这个换行符就是结果
		// result = result.subSequence(1, len);
		// break;
		// }
		// }
		// }
		return result;
	}

	// 获取本行光标后字串
	public CharSequence getToLineEnd(int mFromWhichEnd) {
		CharSequence result = "";
		if (mFromWhichEnd == FROMSTART) {// 如果从结束的光标算起
			result = mConnection.getSelectedText(0);
			if (result.equals(""))
				result = "";// 如果什么都没有取到，则把结果置为空字串
		}
		int len = result.length();
		int len2 = 0;
		for (int i = 1; true; i++) {
			if (i <= len) {// 如果循环的次数还小于已经选择的字符个数，则先在这个字串中取字符
				result = result.subSequence(0, i);
			} else {// 要在光标前取新的字符了
				result = result.toString() + mConnection.getTextAfterCursor(i - len, 0).toString();
			}
			// 接下来是退出条件
			len2 = result.length();// 再次计算长度，因为 result的值可能已经变了
			if (len2 < i)
				break;// 表示已经取到头了
			if (result.charAt(0) == '\n') {// 表示已经取到上一行的换行符了，则去掉最头上的这个换行符就是结果
				result = result.subSequence(0, len2 - 1);
				break;
			}
		}
		return result;
	}

	// 获取光标前一行的字串
	CharSequence getPreLine(int mFromWhichEnd) {
		CharSequence result = "";
		int lineBreaks = 0;// 用于记录取到换行符的个数

		if (mFromWhichEnd == FROMEND) {// 如果从结束的光标算起
			result = mConnection.getSelectedText(0);
			if (result.equals(""))
				result = "";// 如果什么都没有取到，则把结果置为空字串
		}

		int len = result.length();
		int len2 = 0;
		for (int i = 1; true; i++) {
			if (i <= len) {// 如果循环的次数还小于已经选择的字符个数，则先在这个字串中取字符
				result = result.subSequence(len - i, len);
			} else {// 要在光标前取新的字符了
				result = mConnection.getTextBeforeCursor(i - len, 0).toString() + result.toString();
			}

			if (result.charAt(0) == '\n') // 表示已经取到上一行的换行符了，则去掉最头上的这个换行符就是结果
				lineBreaks++;
			// 接下来是退出条件
			len2 = result.length();// 再次计算长度，因为 result的值可能已经变了
			if (len2 < i) {// 表示已经取到头了
				if (lineBreaks == 0) {//已经了到头，但是没有一个换行符，表示光标就在第一行上，则上一行不存在
					return "";
				} else if (lineBreaks == 1) {//已经到头了，但是只取到一个换行符，说明光标前面只有一行
					result = result.subSequence(0, len2 - getToLineStart(mFromWhichEnd).length());
					break;
				}
			} else if (lineBreaks == 2) {//如果取到了两个换行符，说明光标前面有两行以上
				result = result.subSequence(1, len2 - getToLineStart(mFromWhichEnd).length());
				break;
			}
		}
		return result;
	}

	// 获取光标下一行的字串
	CharSequence getNextLine(int mFromWhichEnd) {
		CharSequence result = "";
		int lineBreaks = 0;// 用于记录取到换行符的个数

		if (mFromWhichEnd == FROMSTART) {// 如果从结束的光标算起
			result = mConnection.getSelectedText(0);
			if (result.equals(""))
				result = "";// 如果什么都没有取到，则把结果置为空字串
		}

		int len = result.length();
		int len2 = 0;
		for (int i = 1; true; i++) {
			if (i <= len) {// 如果循环的次数还小于已经选择的字符个数，则先在这个字串中取字符
				result = result.subSequence(0, i);
			} else {// 要在光标前取新的字符了
				result = result.toString() + mConnection.getTextAfterCursor(i - len, 0).toString();
			}

			len2 = result.length();// 再次计算长度，因为 result的值可能已经变了
			if (result.charAt(len2 -1) == '\n') // 表示已经取到上一行的换行符了，则去掉最头上的这个换行符就是结果
				lineBreaks++;
			// 接下来是退出条件			
			if (len2 < i) {// 表示已经取到尾了
				if (lineBreaks == 0) {//已经了到尾，但是没有一个换行符，表示光标就在最后一行上，则再下一行不存在
					return "";
				} else if (lineBreaks == 1) {//已经到尾了，但是只取到一个换行符，说明光标后面只有一行
					result = result.subSequence(getToLineEnd(mFromWhichEnd).length(), len2);
					break;
				}
			} else if (lineBreaks == 2) {//如果取到了两个换行符，说明光标后面有两行以上
				result = result.subSequence(getToLineEnd(mFromWhichEnd).length(), len2 - 1);
				break;
			}
		}
		return result;
	}

}
