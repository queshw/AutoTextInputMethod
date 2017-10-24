package cn.queshw.autotextinputmethod;

import android.view.inputmethod.InputConnection;

public class CursorOperator {
	InputConnection mConnection;

	public CursorOperator(InputConnection mConnection) {
		this.mConnection = mConnection;
	}

/*	public void moveCursorTo(int pos, int mStart, int mEnd) {
		if (mStart == mEnd) {// 处于插入模式
			mConnection.commitText(" ", 1);
			mConnection.deleteSurroundingText(1, 0);// 头上这两行代码用于确保占击屏幕后出现大光标已经消失，处于正常的编辑模式
			mConnection.setSelection(0, 0);
		}
		if (pos > 0) {// 不是移动到头
			moveToPos(pos);
		} else {// 移动到头
			if (mStart != 0 || mEnd != 0)
				moveCursorToStart();
		}
	}

	// ////////////////////////////////////////////////////////////////////////////
	// 把光标移动到头上
	private void moveCursorToStart() {
		// 先把头一个字符复制一倍，这样在网页的输入框中光标位与selection位才能一致
		mConnection.setSelection(0, 1);
		SpannableStringBuilder tSpanable = new SpannableStringBuilder(mConnection.getSelectedText(InputConnection.GET_TEXT_WITH_STYLES));
		mConnection.commitText("", 1);
		mConnection.commitText(tSpanable.append(tSpanable), 0);
		// 再移动光标
		mConnection.setSelection(0, 1);
		CharSequence tSequence = mConnection.getSelectedText(InputConnection.GET_TEXT_WITH_STYLES);
		mConnection.commitText("", 1);
		mConnection.commitText(tSequence, 0);
		mConnection.deleteSurroundingText(1, 0);
		mConnection.setSelection(0, 0);
	}

	// 把光标移到某个位置，不能为头上，也就是说要确保这个位置之前还有一个字符，否则会出错。同时确保处于正常的编辑模式中（大蓝色光标不出现！）
	private void moveToPos(int pos) {
		mConnection.setSelection(pos - 1, pos);
		CharSequence tSequence = mConnection.getSelectedText(InputConnection.GET_TEXT_WITH_STYLES);
		mConnection.commitText("", 1);
		mConnection.commitText(tSequence, 0);
	}*/

	// ///////////////////////////////////////////////////////////////////////////
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

}
