package cn.queshw.autotextinputmethod;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.res.Configuration;
import android.inputmethodservice.InputMethodService;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.Toast;
import cn.queshw.autotextsetting.DBOperations;
import cn.queshw.autotextsetting.MethodItem;

@SuppressLint("SimpleDateFormat")
public class AutotextInputMethod extends InputMethodService {
	private int FROMEND = 1;
	private int FROMSTART = 0;
	private int TOASTDURATION = 50;
	private InputConnection mConnection;
	private EditorInfo mEditInfo;
	private int currentCursorStart = -1;// 当前光标开始位置，用于从onUpdateSelection中获最新光标位置
	private int currentCursorEnd = -1;// 当前光标结束位置，用于从onUpdateSelection中获最新光标位置
	private int offsetBefore;// 相对当前光标
	private int offsetAfter;// 相对当前光标

	private int mMetaState = 0;
	private long state = 0; // for metakeylistener

	private String mBeforeSubString;// 替换前的文本
	private StringBuilder mAfterSubString;// 替换后的文本
	private String mUndoSubString;// 删除的文本，用于undo功能
	private int mEnd;// 用于保存某个光标位置
	private int mStart;// 用于保存某个光标位置
	private int mFromWhichEnd = this.FROMEND;// 用于标记，选择光标移动是应该移动头（0）还是尾（1）
	// private int mPreEnd;// 保存光标位置，用于undo
	// private int mPreStart;//保存光标位置，用于undo
	ClipboardManager clipboard; // 用于复制，粘贴，undo等功能

	private Autotext autotext;// 用于记录替换信息
	private CursorOperator curOper;

	private DBOperations dboper;// 用于数据库操作
	private int selectedMethodPostion = 0;// 在spinner中选择默认输入法
	private int defaultMethodId;// 在spinner中选择默认输入法的id
	private CurrentStatusIcon curStatusIcon;// 当前的状态图标
	private int maxInputLength;// 表中最长的input的长度，用于在正向替换的时候，最长需要从光标前面取多长的文本

	private ArrayList<MethodItem> methodItemList;//

	// 用于标记功能键是否按下
	private boolean isCtrlPressed;
	private boolean isAltPressed;
	@SuppressWarnings("unused")
	private boolean isCapPressed;
	private boolean isSymPressed;
	private boolean switchToFullScreen = false;// 是否切换到全屏模式
	private boolean isInputStarted = false;// 输入是否已经开始
	private boolean isSelectModel = false;

	// 用于emoji表情的输入
	private int stickerStartPosition = 0;
	private SymBoard sb;
	private View candidateView;
	private boolean NEXT = true;
	private boolean PRE = false;

	// /////////////////////////////////////////////////////////////////////////
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.inputmethodservice.InputMethodService#onInitializeInterface()
	 */
	@Override
	public void onInitializeInterface() {
		// TODO Auto-generated method stub
		// Log.d("Here", "onInitializeInterface");
		super.onInitializeInterface();
		dboper = new DBOperations(this);
		clipboard = (ClipboardManager) this.getSystemService(CLIPBOARD_SERVICE);// 获得系统剪贴板
		candidateView = this.getLayoutInflater().inflate(R.layout.sym_keyboard_layout, null);
		this.setCandidatesView(candidateView);
		sb = new SymBoard(candidateView);
	}

	// //////////////////////////////////////////////////////////////////////////
	// 不进入全屏模式
	@Override
	public boolean onEvaluateFullscreenMode() {
		// TODO Auto-generated method stub
		return switchToFullScreen;
	}

	// ///////////////////////////////////////////////////////////////////////////
	@Override
	public void onStartInput(EditorInfo info, boolean restarting) {
//		Log.d("Here", "onStartInput()" + " | restart = " + String.valueOf(restarting) + " | initialStart = " + info.initialSelStart
//				+ " | initialEnd = " + info.initialSelEnd);
		super.onStartInput(info, restarting);
		// 初始化光标的位置，也可据此知道当前编辑器光标前已经有多少个字符了。
		mEditInfo = info;
		mStart = mEditInfo.initialSelStart;
		mEnd = mEditInfo.initialSelEnd;
		if (mStart >= 0 && mEnd >= 0) {
			isInputStarted = true;
		} else {
			isInputStarted = false;
			return;
		}
		
		mConnection = this.getCurrentInputConnection();
		autotext = new Autotext();
		curOper = new CursorOperator(mConnection);
		state = 0L;
		curStatusIcon = CurrentStatusIcon.NONE;

		// 处理EditorInfo的匹配
		if (mEditInfo.inputType == InputType.TYPE_CLASS_NUMBER || mEditInfo.inputType == InputType.TYPE_CLASS_PHONE
				|| mEditInfo.inputType == InputType.TYPE_CLASS_DATETIME) {
			state |= HandleMetaKey.META_ALT_LOCKED;
			// handleStatusIcon(HandleMetaKey.getMetaState(state));
		} else if (mEditInfo.inputType == InputType.TYPE_CLASS_TEXT && (mEditInfo.inputType & InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS) != 0) {
			state |= HandleMetaKey.META_CAP_LOCKED;
			// handleStatusIcon(HandleMetaKey.getMetaState(state));
		}
		handleStatusIcon(HandleMetaKey.getMetaState(this.state));

		// 获取默认的输入词库
		methodItemList = dboper.loadMethodsData();
		// 如果还没有词库，则提醒导入
		if (methodItemList.size() == 0) {
			Toast.makeText(this, this.getString(R.string.msg6), TOASTDURATION).show();
			return;
		}

		for (int i = 0; i < methodItemList.size(); i++) {
			MethodItem item = methodItemList.get(i);
			if (item.getIsDefault() == MethodItem.DEFAULT)
				selectedMethodPostion = i;
		}
		defaultMethodId = methodItemList.get(selectedMethodPostion).getId();
		Toast.makeText(this, methodItemList.get(selectedMethodPostion).getName(), TOASTDURATION).show();
		maxInputLength = dboper.getMaxInputLength(defaultMethodId);
		return;
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.inputmethodservice.InputMethodService#onEvaluateInputViewShown()
	 */
	@Override
	public boolean onEvaluateInputViewShown() {
		// TODO Auto-generated method stub
		return false;//不使用软键盘
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see android.inputmethodservice.InputMethodService#onFinishInput()
	 */
	@Override
	public void onFinishInput() {
		// TODO Auto-generated method stub
		//Log.d("Here", "onFinishInput");
		super.onFinishInput();
		this.hideStatusIcon();
		this.currentCursorEnd = -1;
		this.currentCursorStart = -1;
		this.isInputStarted = false;
		this.isSelectModel = false;

	}

	// /////////////////////////////////////////////////////////////////////////
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (!this.isInputStarted) {
			return super.onKeyDown(selectedMethodPostion, event);
		}
		this.mConnection.finishComposingText();
		this.mConnection.beginBatchEdit();
		mConnection.endBatchEdit();// 必须加上这个语句，要不然EditText可能会进入到BatchEdit模式，不会及时调用onSelectionupdate函数来更新光标信息，从而出错

		state = HandleMetaKey.handleKeyDown(state, keyCode, event);
		mMetaState = event.getMetaState() | HandleMetaKey.getMetaState(state);
		setMetaKeyStatus(mMetaState);
		if (keyCode != KeyEvent.KEYCODE_SHIFT_LEFT && keyCode != KeyEvent.KEYCODE_SHIFT_RIGHT && keyCode != KeyEvent.KEYCODE_ALT_LEFT
				&& keyCode != KeyEvent.KEYCODE_ALT_RIGHT && keyCode != KeyEvent.KEYCODE_SYM) {
			state = HandleMetaKey.adjustMetaAfterKeypress(state);
		}
		handleStatusIcon(mMetaState);

		if (currentCursorStart != -1) {// 说明当前光标的位置不是初始位置，那就使用
			mStart = currentCursorStart;// 把操作开始时的光标位置保存下来
			mEnd = currentCursorEnd;// 把操作开始时的光标位置保存下来
		}

		if (keyCode == ConstantList.SUBSTITUTION_TRIGGER) {// 触发正向替换字符
			isSelectModel = false;// 退出选择模式

			// 如果是短按，接下来准备正向替换
			mBeforeSubString = "";// 替换前的文本
			mAfterSubString = new StringBuilder();// 替换后的文本

			if (mStart != mEnd) {// 处于选择模式
				mConnection.commitText("", 1);
				mConnection.setSelection(mStart, mStart);
				mEnd = mStart;
			}

			// 如果mBeforeSubString不为空，说明之前有替换，那么首先就是要判断，现在光标是否正好在上次反向替换的结尾处：
			if (!TextUtils.isEmpty(autotext.beforeString)) {
				// 取光标前相应长度的字符，或者取到头
				String rawInput = mConnection.getTextBeforeCursor(autotext.beforeString.length(), 0).toString();
				if (mEnd == autotext.end && rawInput.equals(autotext.beforeString)) {
					mConnection.commitText(" ", 1);
					mConnection.setSelection(mEnd + 1, mEnd + 1);
					autotext.clear();
					return true;
				}
			}
			// 如果不是正好在反向替换后，那么就开始正常偿试替换的过程
			CharSequence candidateInput = mConnection.getTextBeforeCursor(maxInputLength + 1, 0);
			if (candidateInput.length() < 1) {

				mConnection.commitText(" ", 1);
				mConnection.setSelection(mEnd + 1, mEnd + 1);
				return true;
			}
			char c;
			for (offsetBefore = 1; offsetBefore <= candidateInput.length(); offsetBefore++) {// 从当前位置开始往前找
				c = candidateInput.charAt(candidateInput.length() - offsetBefore);
				if (c == ConstantList.SUBSTITUTION_SEPERRATOR || c == '\n') {// 如果找到了替换分隔符
					break;
				}
			}
			offsetBefore--;
			candidateInput = candidateInput.subSequence(candidateInput.length() - offsetBefore, candidateInput.length());
			// //Log.d("Here", "candidateInput=" + candidateInput + "|");

			String rawAutotext = dboper.searchRaw("autotext" + defaultMethodId, candidateInput.toString());// 在库中查找替换项
			if (rawAutotext == null) {// 如果没有找到替换项
				mConnection.commitText(" ", 1);
				// 如果没有找到替换项，手机提示一下
				// Vibrator vibrator = (Vibrator)
				// getSystemService(VIBRATOR_SERVICE);
				// vibrator.vibrate(50);
				return true;
			} else {// 如果找到了替换项
				// 开始扫描替换项
				int macroBnumber = 0;// 用于记录%B宏命令的个数
				for (int i = 0; i < rawAutotext.length(); i++) {
					c = rawAutotext.charAt(i);
					// 如果扫描到宏命令的字符%
					if (c == ConstantList.MACRO_ESCAPECHARACTER) {
						c = (i + 1 < rawAutotext.length()) ? rawAutotext.charAt(i + 1) : ConstantList.MACRO_ESCAPECHARACTER;// '%'不能是最后一个字符，否则就当作是普通字符
						switch (c) {
						case ConstantList.MACRO_DELETEBACK:// 在前面删除一个字符
							if (mAfterSubString.length() != 0) {
								mAfterSubString.deleteCharAt(mAfterSubString.length() - 1);
							} else {
								offsetBefore++;
							}
							break;
						case ConstantList.MACRO_DELETEWORD:// 删除刚替换的单词
							offsetBefore += autotext.end - autotext.start;
							break;
						case ConstantList.MACRO_DELETEFORWARD:// 在后面删除一个字符
							macroBnumber++;
							// 一般有宏%B的时候，都表示有重码，手机震动一下
							// Vibrator vibrator = (Vibrator)
							// getSystemService(VIBRATOR_SERVICE);
							// vibrator.vibrate(50);
							break;
						case ConstantList.MACRO_DATE:// date
							String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date(System.currentTimeMillis()));
							mAfterSubString.append(date);
							break;
						case ConstantList.MACRO_LONGDATE:// date
							String datetime = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(System.currentTimeMillis()));
							mAfterSubString.append(datetime);
							break;
						case ConstantList.MACRO_TIME:// time
							String time = new SimpleDateFormat("HH:mm").format(new Date(System.currentTimeMillis()));
							mAfterSubString.append(time);
							break;
						case ConstantList.MACRO_ESCAPECHARACTER:// for char '%'
							mAfterSubString.append(c);
							break;
						default:
							// mAfterSubString.append(ConstantList.MACRO_ESCAPECHARACTER);
							// mAfterSubString.append(c);
						}
						i++;
					} else {// 是普通字符
						mAfterSubString.append(c);
					}
				}

				// 接下来是替换过程
				// 首先，根据前面的扫描结果，获得要被替换的文本
				offsetAfter = macroBnumber <= 1 ? 0 : macroBnumber - 1;// 因为后面还有一个刚输入的空格需要考虑到
				offsetAfter = mConnection.getTextAfterCursor(offsetAfter, 0).length();
				offsetBefore = mConnection.getTextBeforeCursor(offsetBefore, 0).length();
				mBeforeSubString = mConnection.getTextBeforeCursor(offsetBefore, 0).toString()
						+ mConnection.getTextAfterCursor(offsetAfter, 0).toString();

				// 处理以换行符作为分隔的情况，相当于是换行符就是最开头
				final int length = mBeforeSubString.length();
				// Log.d("Here", "before = |" + this.mBeforeSubString + "|");
				for (int j = 0; j < length; ++j) {
					if (mBeforeSubString.charAt(length - 1 - j) == '\n') {
						mBeforeSubString = mBeforeSubString.substring(length - j);
						// Log.d("Here", "after = |" + mBeforeSubString + "|");
						offsetBefore = mBeforeSubString.length();
						break;
					}
				}

				// 第二，记录替换块的信息，不能放到后面记录，因为mAfterSubString会变，可能需要加上空格
				autotext.start = mStart - offsetBefore;
				autotext.end = autotext.start + mAfterSubString.length();
				autotext.beforeString = mBeforeSubString;
				autotext.afterString = mAfterSubString.toString();

				// 第三，构建用于替换的文本，看是否要加上空格
				mAfterSubString = macroBnumber == 0 ? mAfterSubString.append(ConstantList.SUBSTITUTION_SEPERRATOR) : mAfterSubString;

				// 第四，准备好了之后，最后才替换，同时强制更新光标位置

				mConnection.deleteSurroundingText(offsetBefore, offsetAfter);
				mConnection.commitText(mAfterSubString, 1);
				mConnection.setSelection(autotext.start + mAfterSubString.length(), autotext.start + mAfterSubString.length());

				return true;
			}
			// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		} else if (keyCode == ConstantList.SUBSTITUTION_TRIGGER_REVERSE) {// 触发反向替换的字符
			// //Log.d("Here", "SUBTRIGGER");
			isSelectModel = false;
			if (mStart != mEnd) {// 处于选择模式
				mConnection.setSelection(mStart, mStart);
				mUndoSubString = mConnection.getTextAfterCursor(Math.abs(mEnd - mStart), 0).toString();
				mConnection.deleteSurroundingText(0, Math.abs(mEnd - mStart));
				mEnd = mStart;
				return true;
			}

			// 触发反向替换的条件是：
			// 1、替换块不能为空
			// 2、当前光标前的字符与替换块的完全相同，并且光标的位置与替换块的结束标记相同
			// 下面是反向替换过程
			// 条件1:替换块不能为空
			if (TextUtils.isEmpty(autotext.afterString) || TextUtils.isEmpty(autotext.beforeString)) {
				mUndoSubString = mConnection.getTextBeforeCursor(1, 0).toString();
				mConnection.deleteSurroundingText(1, 0);
				// mStart = mStart - 1 < 0 ? 0 : mStart - 1;
				// mEnd = mStart;
				// mConnection.setSelection(mStart, mStart);
				return true;
			}
			// //Log.d("Here", "condition1");

			// 条件2 当前光标前的字符与替换块的完全相同，并且光标的位置与替换块的结束标记相同
			String rawInput = mConnection.getTextBeforeCursor(autotext.afterString.length(), 0).toString();
			if (mEnd != autotext.end || !rawInput.equals(autotext.afterString)) {
				mUndoSubString = mConnection.getTextBeforeCursor(1, 0).toString();
				mConnection.deleteSurroundingText(1, 0);
				// mStart = mStart - 1 < 0 ? 0 : mStart - 1;
				// mEnd = mStart;
				// mConnection.setSelection(mStart, mStart);
				return true;
			}
			// //Log.d("Here", "condition2");

			// 开始反向替换

			mConnection.deleteSurroundingText(autotext.end - autotext.start, 0);
			mConnection.commitText(autotext.beforeString, 1);

			// 记录替换块信息的变化，并强制更新光标位置
			autotext.start = mEnd - autotext.afterString.length();
			autotext.end = mEnd - autotext.afterString.length() + autotext.beforeString.length();
			mConnection.setSelection(autotext.end, autotext.end);

			return true;
			// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		} else if (isCtrlPressed && keyCode == ConstantList.EDIT_SELECTALL) {
			// 全选
			isSelectModel = true;
			mConnection.setSelection(0, mEnd + curOper.getAfterLength());
			if (this.mConnection.getSelectedText(0) == null) {
				this.isSelectModel = false;
			}
			mFromWhichEnd = FROMEND;
			return true;
		} else if (isCtrlPressed && keyCode == ConstantList.EDIT_SELECTLINE) {
			// 选一行
			isSelectModel = true;
			int toLineStart = this.curOper.getToLineStart(this.FROMSTART).length();
			CharSequence getToLineEnd = curOper.getToLineEnd(FROMEND);
			this.mConnection.setSelection(mStart - toLineStart, mEnd + getToLineEnd.length() - curOper.getInvisibleCharsNumber(getToLineEnd));
			if (this.mConnection.getSelectedText(0) == null) {
				this.isSelectModel = false;
			}
			mFromWhichEnd = FROMEND;
			return true;
		} else if (isCtrlPressed && keyCode == ConstantList.EDIT_SELECTMODEL) {
			// 切换选择模式
			if (isSelectModel) {// 退出选择模式
				isSelectModel = false;
				if (mFromWhichEnd == FROMSTART) {
					mConnection.setSelection(mEnd, mEnd);
				} else {
					mConnection.setSelection(mStart, mStart);
				}
			}// 接下来是进入选择模式
			else if (this.mStart != this.mEnd) {// 如果已经处于选择状态
				this.mFromWhichEnd = this.FROMEND;
				this.isSelectModel = true;
			} else if (this.mStart == 0 && this.curOper.getAfterLength() == 0) {// 如果当前内容为空，则不进入选择状态
				this.isSelectModel = false;
			} else if (this.curOper.getAfterLength() == 0) {// 如果
															// 有内容，同时后面已经没有内容了，则往前选一格标志进入选择状态
				this.mConnection.setSelection(this.mEnd - 1, this.mEnd);
				this.mFromWhichEnd = this.FROMSTART;
				this.isSelectModel = true;
			} else {// 如果有内容，并且 光标不在最后
				this.mConnection.setSelection(this.mStart, this.mStart + 1);
				this.mFromWhichEnd = this.FROMEND;
				this.isSelectModel = true;
			}
			return true;
		}
		// ////选择模式////////////////
		else if (!isCtrlPressed && isSelectModel) {
			if (keyCode == ConstantList.EDIT_SELECTALL) {
				// 全选
				mConnection.setSelection(0, mEnd + curOper.getAfterLength());
				if (this.mConnection.getSelectedText(0) == null) {
					this.isSelectModel = false;
				}
				mFromWhichEnd = FROMEND;
				return true;
			} else if (keyCode == ConstantList.EDIT_SELECTLINE) {
				// 选行
				int toLineStart = this.curOper.getToLineStart(this.FROMSTART).length();
				CharSequence getToLineEnd = curOper.getToLineEnd(FROMEND);
				this.mConnection.setSelection(mStart - toLineStart, mEnd + getToLineEnd.length() - curOper.getInvisibleCharsNumber(getToLineEnd));
				if (this.mConnection.getSelectedText(0) == null) {
					this.isSelectModel = false;
				}
				mFromWhichEnd = FROMEND;
				return true;
			} else if (keyCode == ConstantList.EDIT_UP) {
				// 选上一行
				final CharSequence preLine = this.curOper.getPreLine(this.mFromWhichEnd);
				if (!preLine.toString().equals("")) {
					selectedMethodPostion = this.curOper.getInvisibleCharsNumber(preLine);
					final int length2 = preLine.length();
					final int length3 = this.curOper.getToLineStart(this.mFromWhichEnd).length();
					if (this.mFromWhichEnd == this.FROMSTART) {
						if (length2 - selectedMethodPostion < length3) {
							this.mStart = this.mStart - length3 - selectedMethodPostion;
						} else {
							this.mStart -= length2;
						}
					} else if (this.mFromWhichEnd == this.FROMEND) {
						if (length2 - selectedMethodPostion < length3) {
							this.mEnd = this.mEnd - length3 - selectedMethodPostion;
						} else {
							this.mEnd -= length2;
						}
						if (this.mEnd < this.mStart) {
							selectedMethodPostion = this.mStart;
							this.mStart = this.mEnd;
							this.mEnd = selectedMethodPostion;
							this.mFromWhichEnd = this.FROMSTART;
						}
					}
					this.mConnection.setSelection(this.mStart, this.mEnd);
				}
				return true;
			} else if (keyCode == ConstantList.EDIT_DOWN) {
				// 选下一行
				final CharSequence nextLine = this.curOper.getNextLine(this.mFromWhichEnd);
				if (!nextLine.toString().equals("")) {
					selectedMethodPostion = this.curOper.getInvisibleCharsNumber(nextLine);
					final int length4 = nextLine.length();
					final int length5 = this.curOper.getToLineEnd(this.mFromWhichEnd).length();
					final int length6 = this.curOper.getToLineStart(this.mFromWhichEnd).length();
					if (this.mFromWhichEnd == this.FROMEND) {
						if (length4 - selectedMethodPostion < length6) {
							this.mEnd = this.mEnd + length5 + length4 - selectedMethodPostion;
						} else {
							this.mEnd = this.mEnd + length5 + length6;
						}
					} else if (this.mFromWhichEnd == this.FROMSTART) {
						if (length4 - selectedMethodPostion < length6) {
							this.mStart = this.mStart + length5 + length4 - selectedMethodPostion;
						} else {
							this.mStart = this.mStart + length5 + length6;
						}
						if (this.mStart > this.mEnd) {
							selectedMethodPostion = this.mEnd;
							this.mEnd = this.mStart;
							this.mStart = selectedMethodPostion;
							this.mFromWhichEnd = this.FROMEND;
						}
					}
					this.mConnection.setSelection(this.mStart, this.mEnd);
				}
				return true;
			} else if (keyCode == ConstantList.EDIT_BACK) {
				// // 往左选
				if (mStart != mEnd && mFromWhichEnd == 1) {// 如果光标处于选择状态，并且标记位表明应该移动结束位
					mEnd = (mEnd - 1) < 0 ? 0 : mEnd - 1;
				} else {
					mStart = (mStart - 1) < 0 ? 0 : mStart - 1;
					mFromWhichEnd = 0;
				}
				mConnection.setSelection(mStart, mEnd);
				return true;
			} else if (keyCode == ConstantList.EDIT_FORWARD) {
				// // 往右选
				int totalLength = curOper.getAfterLength();
				if (mStart != mEnd && mFromWhichEnd == 0) {// 如果光标处于选择状态，并且标记位表明应该移动开始位
					mStart = (mStart + 1) > mEnd + totalLength ? mEnd + totalLength : currentCursorStart + 1;
				} else {
					mEnd = (mEnd + 1) > mEnd + totalLength ? mEnd + totalLength : currentCursorEnd + 1;
					mFromWhichEnd = 1;
				}
				mConnection.setSelection(mStart, mEnd);
				return true;
			} else if (keyCode == ConstantList.EDIT_TOLINESTART) {
				// 选到行头
				if (this.mFromWhichEnd == this.FROMSTART) {
					this.mConnection.setSelection(this.mStart - this.curOper.getToLineStart(this.FROMSTART).length(), this.mEnd);
				} else {
					this.mConnection.setSelection(this.mStart - this.curOper.getToLineStart(this.FROMSTART).length(), this.mStart);
					this.mFromWhichEnd = this.FROMSTART;
				}
				return true;
			} else if (keyCode == ConstantList.EDIT_TOLINEEND) {
				// 选到行尾
				if (this.mFromWhichEnd == this.FROMSTART) {
					this.mConnection.setSelection(
							this.mEnd,
							this.mEnd + this.curOper.getToLineEnd(this.FROMEND).length()
									- this.curOper.getInvisibleCharsNumber(this.curOper.getToLineEnd(this.FROMEND)));
					this.mFromWhichEnd = this.FROMEND;
				} else {
					this.mConnection.setSelection(
							this.mStart,
							this.mEnd + this.curOper.getToLineEnd(this.FROMEND).length()
									- this.curOper.getInvisibleCharsNumber(this.curOper.getToLineEnd(this.FROMEND)));
					this.mFromWhichEnd = this.FROMEND;
				}
				return true;
			} else if (keyCode == ConstantList.EDIT_TOSTART) {
				// 选到头
				// mConnection.setSelection(0, mEnd);
				mConnection.setSelection(0, mEnd);
				mFromWhichEnd = 0;// 如果是往前选到头，接下来移动光标当然应该是移动开始位
				return true;
			} else if (keyCode == ConstantList.EDIT_TOEND) {
				// 选到尾
				mConnection.setSelection(mStart, mEnd + curOper.getAfterLength());
				mFromWhichEnd = 1;// 如果是往前选到头，接下来移动光标当然应该是移动结束位
				return true;
			}
		}
		// ////////选择模式结束//////////////////

		// ////////移动快捷键///////////////////
		else if (isCtrlPressed && keyCode == ConstantList.EDIT_UP) {
			// 向上
			isSelectModel = false;
			// Log.d("Here", "LINE START FROM START = " + "|" +
			// curOper.getToLineStart(FROMSTART) + "|");
			// Log.d("Here", "LINE START FROM END= " + "|" +
			// curOper.getToLineStart(FROMEND) + "|");
			// Log.d("Here", "UP FROMSTART = " + "|" +
			// curOper.getPreLine(FROMSTART) + "|");
			// Log.d("Here", "UP FROMEND = " + "|" + curOper.getPreLine(FROMEND)
			// + "|");
			final CharSequence preLine2 = this.curOper.getPreLine(this.mFromWhichEnd);
			if (!preLine2.toString().equals("")) {
				selectedMethodPostion = this.curOper.getInvisibleCharsNumber(preLine2);
				final int length7 = preLine2.length();
				final int length8 = this.curOper.getToLineStart(this.mFromWhichEnd).length();
				if (this.mFromWhichEnd == this.FROMSTART) {
					if (length7 - selectedMethodPostion < length8) {
						this.mStart = this.mStart - length8 - selectedMethodPostion;
					} else {
						this.mStart -= length7;
					}
					this.mConnection.setSelection(this.mStart, this.mStart);
				} else if (this.mFromWhichEnd == this.FROMEND) {
					if (length7 - selectedMethodPostion < length8) {
						this.mEnd = this.mEnd - length8 - selectedMethodPostion;
					} else {
						this.mEnd -= length7;
					}
					this.mConnection.setSelection(this.mEnd, this.mEnd);
				}
			}

			return true;
		} else if (isCtrlPressed && keyCode == ConstantList.EDIT_DOWN) {
			// 向下
			isSelectModel = false;
			// Log.d("Here", "LINE END FROM START = " + "|" +
			// curOper.getToLineEnd(FROMSTART) + "|");
			// Log.d("Here", "LINE END FROM END= " + "|" +
			// curOper.getToLineEnd(FROMEND) + "|");
			// Log.d("Here", "DOWN FROMSTART = " + "|" +
			// curOper.getNextLine(FROMSTART) + "|");
			// Log.d("Here", "DOWN FROMEND = " + "|" +
			// curOper.getNextLine(FROMEND) + "|");
			final CharSequence nextLine2 = this.curOper.getNextLine(this.mFromWhichEnd);
			if (!nextLine2.toString().equals("")) {
				selectedMethodPostion = this.curOper.getInvisibleCharsNumber(nextLine2);
				final int length9 = nextLine2.length();
				final int length10 = this.curOper.getToLineEnd(this.mFromWhichEnd).length();
				final int length11 = this.curOper.getToLineStart(this.mFromWhichEnd).length();
				if (this.mFromWhichEnd == this.FROMEND) {
					if (length9 - selectedMethodPostion < length11) {
						this.mEnd = this.mEnd + length10 + length9 - selectedMethodPostion;
					} else {
						this.mEnd = this.mEnd + length10 + length11;
					}
					this.mConnection.setSelection(this.mEnd, this.mEnd);
				} else if (this.mFromWhichEnd == this.FROMSTART) {
					if (length9 - selectedMethodPostion < length11) {
						this.mStart = this.mStart + length10 + length9 - selectedMethodPostion;
					} else {
						this.mStart = this.mStart + length10 + length11;
					}
					this.mConnection.setSelection(this.mStart, this.mStart);
				}
			}
			this.mConnection.setSelection(this.mEnd, this.mEnd);

			return true;
		} else if (isCtrlPressed && keyCode == ConstantList.EDIT_BACK) {
			// 向左移
			isSelectModel = false;
			int pos = mStart != mEnd ? mStart : mStart - 1;
			pos = pos < 0 ? 0 : pos;
			// Log.d("Here", "mStart=" + String.valueOf(mStart) + "|mEnd=" +
			// String.valueOf(mEnd));
			// curOper.moveCursorTo(pos, mStart, mEnd);
			mConnection.setSelection(pos, pos);
			return true;
		} else if (isCtrlPressed && keyCode == ConstantList.EDIT_FORWARD) {
			// 往右移
			isSelectModel = false;
			int totalLength = mEnd + curOper.getAfterLength();
			int pos = mStart != mEnd ? mEnd : mEnd + 1;
			pos = pos > totalLength ? totalLength : pos;
			// curOper.moveCursorTo(pos, mStart, mEnd);
			mConnection.setSelection(pos, pos);
			return true;
		} else if (isCtrlPressed && keyCode == ConstantList.EDIT_TOLINESTART) {
			// 移到行头
			isSelectModel = false;
			selectedMethodPostion = this.curOper.getToLineStart(this.FROMSTART).length();
			this.mConnection.setSelection(this.mStart - selectedMethodPostion, this.mStart - selectedMethodPostion);
			this.mFromWhichEnd = this.FROMEND;
			return true;
		} else if (isCtrlPressed && keyCode == ConstantList.EDIT_TOLINEEND) {
			// 移到行尾
			selectedMethodPostion = this.curOper.getToLineEnd(this.FROMEND).length();
			this.mEnd = this.mEnd + selectedMethodPostion - this.curOper.getInvisibleCharsNumber(this.curOper.getToLineEnd(this.FROMEND));
			this.mConnection.setSelection(this.mEnd, this.mEnd);
			this.mFromWhichEnd = this.FROMEND;
			isSelectModel = false;
			return true;
		} else if (isCtrlPressed && keyCode == ConstantList.EDIT_TOSTART) {
			// 移到头
			isSelectModel = false;
			// curOper.moveCursorTo(0, mStart, mEnd);
			mConnection.setSelection(0, 0);
			return true;
		} else if (isCtrlPressed && keyCode == ConstantList.EDIT_TOEND) {
			// 移到尾
			isSelectModel = false;
			int totalLength = mEnd + curOper.getAfterLength();
			// curOper.moveCursorTo(totalLength, mStart, mEnd);
			mConnection.setSelection(totalLength, totalLength);
			return true;
		}
		// /////////移动快捷键结束///////////////

		// ////////删除快捷键开始////////////////
		else if (isCtrlPressed && keyCode == ConstantList.EDIT_DELETEALL) {
			// // 删除全部内容OKKKKKKKKKKKKKK
			isSelectModel = false;
			mConnection.setSelection(mStart, mStart);// 这样可以确保光标不会处于选择状态
			int afterLength = curOper.getAfterLength();
			mUndoSubString = mConnection.getTextBeforeCursor(mStart + 1, 0).toString() + mConnection.getTextAfterCursor(afterLength, 0).toString();
			mConnection.deleteSurroundingText(mStart + 1, afterLength);
			mConnection.setSelection(0, 0);
			return true;
		} else if (isCtrlPressed && keyCode == ConstantList.EDIT_DELETELINE) {
			// 删除行
			isSelectModel = false;
			selectedMethodPostion = this.curOper.getToLineStart(this.FROMSTART).length();
			this.mConnection.setSelection(this.mStart - selectedMethodPostion, this.mEnd + this.curOper.getToLineEnd(this.FROMEND).length());
			if (this.mConnection.getSelectedText(0) == null) {
				this.isSelectModel = false;
			} else {
				mUndoSubString = (String) mConnection.getSelectedText(0);
			}
			return true;
		} else if (isCtrlPressed && keyCode == ConstantList.EDIT_DELETEFORWARD) {
			// 删除后面一个字符
			isSelectModel = false;
			mUndoSubString = mConnection.getTextAfterCursor(1, 0).toString();
			mConnection.deleteSurroundingText(0, 1);
			return true;
		}
		// /////////删除快捷键结束/////////////////

		// ////////其他快捷键开始//////////////////
		else if (isCtrlPressed && keyCode == ConstantList.EDIT_UNDO) {
			// undo功能
			isSelectModel = false;
			if (!mUndoSubString.isEmpty()) {
				mConnection.commitText(mUndoSubString, 1);
				mUndoSubString = "";
			}
			return true;
		} else if (isCtrlPressed && keyCode == ConstantList.EDIT_COPY) {
			// 复制OKKKKKKKKKKKKKKKKK
			isSelectModel = false;
			if (mStart != mEnd) {
				clipboard = (ClipboardManager) this.getSystemService(CLIPBOARD_SERVICE);
				ClipData clip = ClipData.newPlainText("AutotextInputMethod", mConnection.getSelectedText(InputConnection.GET_TEXT_WITH_STYLES));
				clipboard.setPrimaryClip(clip);
				mConnection.setSelection(mEnd, mEnd);

				Toast.makeText(this, this.getString(R.string.copyed), TOASTDURATION).show();
			}
			return true;
		} else if (isCtrlPressed && keyCode == ConstantList.EDIT_PASTE || isCtrlPressed && keyCode == ConstantList.EDIT_UNDO) {
			// 粘贴功能
			isSelectModel = false;
			clipboard = (ClipboardManager) this.getSystemService(CLIPBOARD_SERVICE);
			CharSequence pasteText = "";
			if (clipboard.hasPrimaryClip()) {// 如果剪贴板里有内容
				ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
				pasteText = item.getText();
				mConnection.commitText(pasteText, 1);
			}
			// mConnection.performContextMenuAction(android.R.id.paste);
			return true;
		} else if (isCtrlPressed && keyCode == ConstantList.EDIT_CUT) {
			// 剪切OKKKKKKKKKKKKKKKKKK
			isSelectModel = false;
			if (mStart != mEnd) {
				// ClipboardManager clipboard = (ClipboardManager)
				// this.getSystemService(CLIPBOARD_SERVICE);
				// ClipData clip = ClipData.newPlainText("AutotextInputMethod",
				// mConnection.getSelectedText(InputConnection.GET_TEXT_WITH_STYLES));
				// clipboard.setPrimaryClip(clip);
				// mConnection.commitText("", 1);
				int mUndoStart = mStart;
				int mUndoEnd = mEnd;
				mConnection.setSelection(mStart, mStart);
				mUndoSubString = mConnection.getTextAfterCursor(Math.abs(mEnd - mStart), 0).toString();
				mConnection.setSelection(mUndoStart, mUndoEnd);
				mConnection.performContextMenuAction(android.R.id.cut);
			}
			return true;
		} else if (isCtrlPressed && keyCode == ConstantList.SWITCH_INPUTMETHOD) {
			// 切换输入法的快捷键
			// InputMethodManager imm = (InputMethodManager)
			// this.getSystemService(Context.INPUT_METHOD_SERVICE);
			// List<InputMethodInfo> inputMethodList =
			// imm.getInputMethodList();//获得系统所有输入法列表
			// String curInputMethodId =
			// Settings.Secure.getString(this.getContentResolver(),
			// Settings.Secure.DEFAULT_INPUT_METHOD);//获得当前输入法的id
			// int listId =
			// inputMethodList.indexOf(curInputMethodId);//计算当前输入法在列表中是第几项
			// Log.d("Here", curInputMethodId + " is " +String.valueOf(listId));
			//
			// //如果当前输入法是本输入法，id为
			// cn.queshw.autotextinputmethod/.AutotextInputMethod，并且不是最后一个词库了
			// if(curInputMethodId == ConstantList.METHODID &&
			// selectedMethodPostion != methodItemList.size()){
			// selectedMethodPostion++;
			// defaultMethodId =
			// methodItemList.get(selectedMethodPostion).getId();
			// dboper.addOrSaveMethodItem(methodItemList.get(selectedMethodPostion).getName(),
			// MethodItem.DEFAULT, defaultMethodId);
			// Toast.makeText(this, "AutoText:" +
			// methodItemList.get(selectedMethodPostion).getName(),
			// TOASTDURATION).show();
			// }else{//如果是其他输入法，则往下一个输入法切换
			// listId = listId + 1 > inputMethodList.size()? 0: listId + 1;
			// this.switchInputMethod(inputMethodList.get(listId).getId());
			// Toast.makeText(this, inputMethodList.get(listId).getServiceName()
			// , TOASTDURATION).show();
			// }

			selectedMethodPostion = selectedMethodPostion + 1 < methodItemList.size() ? selectedMethodPostion + 1 : 0;
			defaultMethodId = methodItemList.get(selectedMethodPostion).getId();
			dboper.addOrSaveMethodItem(methodItemList.get(selectedMethodPostion).getName(), MethodItem.DEFAULT, defaultMethodId);
			Toast.makeText(this, methodItemList.get(selectedMethodPostion).getName(), TOASTDURATION).show();
			return true;
		}
		// ////////其他快捷键结束//////////////////
		else if (isSymPressed) {// 暂时什么都不做！！
			// Log.d("Here", "sym pressed there");
			// return false;
			isSelectModel = false;
			if (keyCode == KeyEvent.KEYCODE_0) {// 往前翻页
				this.symBoardTurn(stickerStartPosition, PRE);
				state |= KeyEvent.META_SYM_ON | HandleMetaKey.META_SYM_RELEASED;
				return true;
			}

			String temp = sb.getSticker(keyCode);
			if (!temp.equals("NONE")) {
				mConnection.commitText(temp, 1);
			}
			return true;
		} else if (keyCode == ConstantList.SUBSTITUTION_ENTER || keyCode == ConstantList.SUBSTITUTION_NUMPAD_ENTER) {// 如果输入回车健
			isSelectModel = false;
			mConnection.performEditorAction(mEditInfo.imeOptions & EditorInfo.IME_MASK_ACTION);
		} else {

			KeyCharacterMap kcm = event.getKeyCharacterMap();
			// //Log.d("Here", String.valueOf(kcm.getModifierBehavior()));
			if (kcm.isPrintingKey(keyCode)) {

				if (isCtrlPressed)// 说明正处于快捷命令模式中
					return true;

				isSelectModel = false;
				char c;
				if (event.getRepeatCount() == 0) {// 短按
					c = (char) kcm.get(keyCode, mMetaState);
				} else if (event.getRepeatCount() == 1) {// 长按大写
					c = (char) kcm.get(keyCode, KeyEvent.META_CAPS_LOCK_ON);
					mConnection.deleteSurroundingText(1, 0);
				} else {
					return true;
				}

				mConnection.commitText(String.valueOf(c), 1);
				return true;
			}
		}

		return super.onKeyDown(keyCode, event);
	}

	// /////////////////////////////////////////////////////////////////////////
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		// Log.d("Here",
		// "onKeyUp()" + "|Type=" +
		// String.valueOf(event.getKeyCharacterMap().getKeyboardType()) +
		// "|Mode="
		// + String.valueOf(event.getKeyCharacterMap().getModifierBehavior()) +
		// "|keycode=" + KeyEvent.keyCodeToString(keyCode)
		// + "|metastate=" +
		// String.valueOf(Integer.toBinaryString(event.getMetaState())) +
		// "|repeatcount="
		// + String.valueOf(event.getRepeatCount()) + "|flag=" +
		// String.valueOf(Integer.toBinaryString(event.getFlags())));

		state = HandleMetaKey.handleKeyUp(state, keyCode, event);

		if (isSymPressed && (state & HandleMetaKey.META_SYM_TURNPAGE) != 0 && (state & HandleMetaKey.META_SYM_RELEASED) != 0) {// 说明表情小键盘要翻页
			// 翻页
			this.symBoardTurn(stickerStartPosition, NEXT);
		}

		// Log.d("Here", "uphandled=" + Long.toBinaryString(state));
		mMetaState = HandleMetaKey.getMetaState(state) | event.getMetaState();
		handleStatusIcon(mMetaState);
		return super.onKeyUp(keyCode, event);
	}

	// ///////////////////////////////////////////////////////////////////////
	@Override
	public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd, int candidatesStart, int candidatesEnd) {
		// //Log.d("Here", "newSelS=" + String.valueOf(newSelStart) +
		// "|newSelE="
		// + String.valueOf(newSelEnd));
		// //Log.d("Here", "oldSelS=" + String.valueOf(oldSelStart) +
		// "|oldSelE="
		// + String.valueOf(oldSelEnd));
		currentCursorStart = newSelStart;
		currentCursorEnd = newSelEnd;

		super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd);
	}

	// /////////////////////////////////////////////
	private void setMetaKeyStatus(int mMetaState) {
		// Log.d("Here", "setMetaKeyStatus = " +
		// String.valueOf(Integer.toBinaryString(mMetaState)));

		if ((mMetaState & KeyEvent.META_ALT_ON) != 0) {// 按了alt键
			// Log.d("Here", "alt true");
			isAltPressed = true;
		} else {
			// Log.d("Here", "alt false");
			isAltPressed = false;
		}
		if ((mMetaState & KeyEvent.META_SHIFT_LEFT_ON) != 0) {// 按了左shift键，这里当作ctrl键
			// Log.d("Here", "ctrl true");
			isCtrlPressed = true;
		} else {
			// Log.d("Here", "ctrl false");
			isCtrlPressed = false;
		}
		if ((mMetaState & KeyEvent.META_SHIFT_RIGHT_ON) != 0) {// 按了右shift键
			// Log.d("Here", "cap true");
			isCapPressed = true;
		} else {
			// Log.d("Here", "cap false");
			isCapPressed = false;
		}
		if ((mMetaState & KeyEvent.META_SYM_ON) != 0) {// 按了sym键
			// Log.d("Here", "sym true");
			isSymPressed = true;

		} else {
			// Log.d("Here", "sym false");
			isSymPressed = false;

		}

		if ((state & HandleMetaKey.META_SYM_TURNPAGE) != 0) {// 说明表情小键盘要翻页
			this.symBoardTurn(stickerStartPosition, NEXT);
		}
	}

	// 用于表情键盘的翻页
	private void symBoardTurn(int stickerStartPosition2, boolean turnDirection) {
		if (turnDirection == PRE) {// 往前翻页
			stickerStartPosition -= 2 * sb.getStickerNumbersPerPage();
			if (stickerStartPosition < 0)
				stickerStartPosition = 0;
		}

		sb.setStickerKeyboard(stickerStartPosition);
		stickerStartPosition += sb.getStickerNumbersPerPage();
		if (stickerStartPosition >= sb.getEmojiNumbers())
			stickerStartPosition = 0;

		// 清空
		state &= ~HandleMetaKey.META_SYM_TURNPAGE;
	}

	// /////////////////////////////////////////////
	private void handleStatusIcon(int mMetaState) {
		// Log.d("Here", "handleStatusIcon = " +
		// String.valueOf(Integer.toBinaryString(mMetaState)));
		CurrentStatusIcon icon;
		if ((mMetaState & KeyEvent.META_SYM_ON) != 0) {// sym on
			icon = CurrentStatusIcon.SYM_ON;
			// Log.d("Here", String.valueOf(stickerStartPosition));
			this.setCandidatesViewShown(true);// 显示表情小键盘
		} else {
			this.setCandidatesViewShown(false);// 关闭表情小键盘
			stickerStartPosition = 0;
			if ((mMetaState & 0x100) != 0) {// cap locked
				icon = CurrentStatusIcon.CAP_LOCK;
			} else if ((mMetaState & KeyEvent.META_SHIFT_RIGHT_ON) != 0) {// cap
																			// on
				icon = CurrentStatusIcon.CAP_ON;
			} else if ((mMetaState & 0x200) != 0) {// alt locked
				icon = CurrentStatusIcon.ALT_LOCK;
			} else if ((mMetaState & KeyEvent.META_ALT_ON) != 0) {// alt on
				icon = CurrentStatusIcon.ALT_ON;
			} else if ((mMetaState & 0x400) != 0) {// sym locked
				icon = CurrentStatusIcon.SYM_LOCK;
			} else if ((mMetaState & 0x800) != 0) {// ctrl locked
				icon = CurrentStatusIcon.NEWSIM_LOCK;
			} else if ((mMetaState & KeyEvent.META_SHIFT_LEFT_ON) != 0) {// ctrl
																			// on
				icon = CurrentStatusIcon.NEWSIM_ON;
			} else {
				icon = CurrentStatusIcon.NORMAL;
			}
		}

		if (curStatusIcon != icon) {
			this.hideStatusIcon();
			this.showStatusIcon(icon.getIconId());
			curStatusIcon = icon;
		}
	}

	// 当前图标枚举标记
	enum CurrentStatusIcon {
		NORMAL(R.drawable.status_normal), CAP_ON(R.drawable.status_cap), CAP_LOCK(R.drawable.status_cap_lock), ALT_ON(R.drawable.status_alt), ALT_LOCK(
				R.drawable.status_alt_lock), SYM_ON(R.drawable.status_sym), SYM_LOCK(R.drawable.status_sym_lock), NEWSIM_ON(R.drawable.status_ctrl), NEWSIM_LOCK(
				R.drawable.status_ctrl_lock), NONE(-1);

		private int iconId;

		CurrentStatusIcon(int iconId) {
			this.iconId = iconId;
		}

		int getIconId() {
			return iconId;
		}
	}

}
