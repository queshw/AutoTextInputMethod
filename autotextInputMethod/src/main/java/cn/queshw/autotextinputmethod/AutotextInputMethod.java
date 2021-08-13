package cn.queshw.autotextinputmethod;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.res.Configuration;
import android.hardware.input.InputManager;
import android.inputmethodservice.InputMethodService;
import android.text.InputType;
import android.text.TextUtils;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import cn.queshw.autotextsetting.DBOperations;
import cn.queshw.autotextsetting.MethodItem;

@SuppressLint("SimpleDateFormat")
public class AutotextInputMethod extends InputMethodService implements View.OnClickListener, View.OnLongClickListener {
    private final int FROMEND = 1;
    private final int FROMSTART = 0;

    private InputConnection mConnection;
    private EditorInfo mEditInfo;
    private int currentCursorStart = -1;// 当前光标开始位置，用于从onUpdateSelection中获最新光标位置
    private int currentCursorEnd = -1;// 当前光标结束位置，用于从onUpdateSelection中获最新光标位置
    private int offsetBefore;// 相对当前光标

    private String inputString;// 要被替换的文本，不一定是编码，比如有%b这些宏的时候，可能是删除前面的字符
    private StringBuilder autotextString;// 用于替换的文本
    private int mEnd;// 用于保存某个光标位置
    private int mStart;// 用于保存某个光标位置
    private int mFromWhichEnd = this.FROMEND;// 用于标记，选择光标移动是应该移动头（0）还是尾（1）
    ClipboardManager clipboard; // 用于复制，粘贴，undo等功能

    private Autotext autotext;// 用于记录替换信息
    private Autotext undoAutotext;// 用于记录undo的替换块信息
    private CursorOperator curOper;

    private DBOperations dboper;// 用于数据库操作
    private int defaultMethodId;// 默认输入法的id

    //词库相关
    private ArrayList<MethodItem> methodItemList;//保存着所有词库的数据
    private int methodItemNum;//现有词库在methodItemList中的序号
    private String methodName;//现有词库的名字

    // 用于标记功能键是否按下，其实其状态可以使用meatstate的标记位来判断，但是为了方便设置了以下的变量
    private boolean switchToFullScreen = false;// 是否切换到全屏模式
    private boolean isInputStarted = false;// 输入是否已经开始
    private boolean isSelectModel = false;

    //键盘相关
    BbKeyBoard bbKeyboard;//软键盘
    boolean hasHardKeyboard = false;//手机是否有物理键盘
    private int mMetaState = 0;//功能键状态码
    private long state = 0; // 长状态码 for metakeylistener

    // 用于emoji表情的输入
    private int stickerStartPosition = 0;
    private EmojiBoard emojiBoard;


    // /////////////////////////////////////////////////////////////////////////
    /*
     * (non-Javadoc)
     *
     * @see
     * android.inputmethodservice.InputMethodService#onInitializeInterface()
     */
    @Override
    public void onInitializeInterface() {
        super.onInitializeInterface();
        dboper = new DBOperations(this);
        clipboard = (ClipboardManager) this.getSystemService(CLIPBOARD_SERVICE);// 获得系统剪贴板

        //设置emoji键盘
        emojiBoard = new EmojiBoard(this);
        this.setCandidatesView(emojiBoard.getEmojiboardView());

        //设置软键盘
        bbKeyboard = new BbKeyBoard(this, this, this);
        //判断手机上有没有带物理键盘，决定是否要显示软键盘的按键部分
        InputManager mIm = (InputManager) this.getSystemService(INPUT_SERVICE);
        final int[] devices = InputDevice.getDeviceIds();
        for (int i = 0; i < devices.length; i++) {
            InputDevice device = InputDevice.getDevice(devices[i]);
            if (device != null && !device.isVirtual() && ((device.getSources() & InputDevice.SOURCE_KEYBOARD) != 0) && (device.getKeyboardType() == InputDevice.KEYBOARD_TYPE_ALPHABETIC)) {
                hasHardKeyboard = true;
            }
        }
        bbKeyboard.set_keyonly_view_visibile(hasHardKeyboard);
        this.setInputView(bbKeyboard.getbbKeyboardView());

    }

    public void onConfigurationChanged(Configuration newConfig) {
        if (newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO) {//有物理键盘
            hasHardKeyboard = true;
        } else {//没有物理健盘
            hasHardKeyboard = false;
        }
        bbKeyboard.set_keyonly_view_visibile(hasHardKeyboard);
        this.updateInputViewShown();
    }

    // //////////////////////////////////////////////////////////////////////////
    // 不进入全屏模式
    @Override
    public boolean onEvaluateFullscreenMode() {
        return switchToFullScreen;
    }

    // ///////////////////////////////////////////////////////////////////////////
    @Override
    public void onStartInput(EditorInfo info, boolean restarting) {
        super.onStartInput(info, restarting);
        // 初始化光标的位置，也可据此知道当前编辑器光标前已经有多少个字符了。
        mEditInfo = info;
        mStart = mEditInfo.initialSelStart;
        mEnd = mEditInfo.initialSelEnd;
        if (mStart >= 0 && mEnd >= 0) {
            isInputStarted = true;
        } else {
            isInputStarted = false;
            mStart = 0;
            mEnd = 0;
            return;
        }

        mConnection = this.getCurrentInputConnection();
        autotext = new Autotext();
        undoAutotext = new Autotext();
        curOper = new CursorOperator(mConnection);
        state = 0L;

        // 获取默认的输入词库
        methodItemList = dboper.loadMethodsData();
        // 如果还没有词库，则提醒导入
        if (methodItemList.size() == 0) {
            Toast.makeText(this, this.getString(R.string.msg6), Toast.LENGTH_SHORT).show();
            return;
        }
        //获得黙认词库的id
        methodItemNum = 0;//先把第一个词库的id作为默认词库
        for (int i = 0; i < methodItemList.size(); i++) {
            if (methodItemList.get(i).getIsDefault() == MethodItem.DEFAULT)
                methodItemNum = i;
        }
        defaultMethodId = methodItemList.get(methodItemNum).getId();
        methodName = methodItemList.get(methodItemNum).getName();

        // 处理EditorInfo的匹配
        if (mEditInfo.inputType == InputType.TYPE_CLASS_NUMBER || mEditInfo.inputType == InputType.TYPE_CLASS_PHONE
                || mEditInfo.inputType == InputType.TYPE_CLASS_DATETIME) {
            state |= HandleMetaKey.META_ALT_LOCKED;
            // handleStatusIcon(HandleMetaKey.getMetaState(state));
        } else if (mEditInfo.inputType == InputType.TYPE_CLASS_TEXT && (mEditInfo.inputType & InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS) != 0) {
            state |= HandleMetaKey.META_CAP_LOCKED;
        }

        //根据上面获得的信息设置软键盘状态栏
        bbKeyboard.set_status_line_view(HandleMetaKey.getMetaState(this.state), methodName);
    }


    @Override
    public boolean onEvaluateInputViewShown() {
        super.onEvaluateInputViewShown();
        return true;
    }

    @Override
    public void onFinishInput() {
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
            return super.onKeyDown(0, event);
        }

        this.mConnection.finishComposingText();
        this.mConnection.beginBatchEdit();
        mConnection.endBatchEdit();// 必须加上这个语句，要不然EditText可能会进入到BatchEdit模式，不会及时调用onSelectionupdate函数来更新光标信息，从而出错

        //检测功能键的状态，并进行相应的设置，比如软件盘中的状态栏设置，emoji小键盘是否要打开等
        state = HandleMetaKey.handleKeyDown(state, keyCode);
        mMetaState = event.getMetaState() | HandleMetaKey.getMetaState(state);//获得最终的功能键状态码
        bbKeyboard.set_status_line_view(mMetaState, methodName);

        if (currentCursorStart != -1) {// 说明当前光标的位置不是初始位置，那就使用
            mStart = currentCursorStart;// 把操作开始时的光标位置保存下来
            mEnd = currentCursorEnd;// 把操作开始时的光标位置保存下来
        }

        //如果emoji键盘已经开始。要优先处理，要不然会先处理其他字符，比如space 和 backspace 键
        if (bbKeyboard.isSymOn() && !bbKeyboard.isCapOn() && !bbKeyboard.isCtrlOn() && !bbKeyboard.isAltOn() & !isSelectModel) {//用于表情键盘的往前翻页
            emojiBoard.turnEmojiKeyboard(EmojiBoard.TURN_DOWN);
            this.setCandidatesViewShown(true);
            if (keyCode == KeyEvent.KEYCODE_0) {// 往前翻页
                emojiBoard.turnEmojiKeyboard(EmojiBoard.TURN_UP);
            } else {//如果是其他键，刚获得对应的emoji表情
                mConnection.commitText(emojiBoard.getSticker(keyCode), 1);
                setCandidatesViewShown(false);
                //清空sym状态
                state = (state | HandleMetaKey.META_SYM_ALL) ^ state;
            }
            return true;
        }

        //如果进入命令模式
        if (!bbKeyboard.isSymOn() && !bbKeyboard.isCapOn() && bbKeyboard.isCtrlOn() && !bbKeyboard.isAltOn()) {
            this.isSelectModel = false;//不管现在是不是在选择模式中，再按一次ctrl后，选择模式都将失效
            if (keyCode == ConstantList.EDIT_SELECTALL) {
                // 全选
                mConnection.setSelection(0, mEnd + curOper.getAfterLength());
                if (this.mConnection.getSelectedText(0) == null) {
                    this.isSelectModel = false;
                }
                mFromWhichEnd = FROMEND;
                return true;
            } else if (keyCode == ConstantList.EDIT_SELECTLINE) {
                // 选一行
                int toLineStart = this.curOper.getToLineStart(this.FROMSTART).length();
                CharSequence getToLineEnd = curOper.getToLineEnd(FROMEND);
                this.mConnection.setSelection(mStart - toLineStart, mEnd + getToLineEnd.length() - curOper.getInvisibleCharsNumber(getToLineEnd));
                if (this.mConnection.getSelectedText(0) == null) {
                    this.isSelectModel = false;
                }
                mFromWhichEnd = FROMEND;
                return true;
            } else if (keyCode == ConstantList.EDIT_SELECTMODEL) {
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
                } else if (this.curOper.getAfterLength() == 0) {// 如果后面没有内容
                    this.mConnection.setSelection(this.mEnd, this.mEnd);
                    this.mFromWhichEnd = this.FROMSTART;
                    this.isSelectModel = true;
                } else {// 如果后面有内容
                    this.mConnection.setSelection(this.mStart, this.mStart);
                    this.mFromWhichEnd = this.FROMEND;
                    this.isSelectModel = true;
                }
                state = 0L;
                bbKeyboard.set_status_line_view(HandleMetaKey.getMetaState(state), methodName);
                bbKeyboard.set_status_line_view_for_selectmode(isSelectModel, methodName);
                return true;
            }
            // ////////移动快捷键///////////////////
            else if (keyCode == ConstantList.EDIT_UP) {
                // 向上
                final CharSequence preLine2 = this.curOper.getPreLine(this.mFromWhichEnd);
                if (!preLine2.toString().equals("")) {
                    int tempNum = this.curOper.getInvisibleCharsNumber(preLine2);
                    final int length7 = preLine2.length();
                    final int length8 = this.curOper.getToLineStart(this.mFromWhichEnd).length();
                    if (this.mFromWhichEnd == this.FROMSTART) {
                        if (length7 - tempNum < length8) {
                            this.mStart = this.mStart - length8 - tempNum;
                        } else {
                            this.mStart -= length7;
                        }
                        this.mConnection.setSelection(this.mStart, this.mStart);
                    } else if (this.mFromWhichEnd == this.FROMEND) {
                        if (length7 - tempNum < length8) {
                            this.mEnd = this.mEnd - length8 - tempNum;
                        } else {
                            this.mEnd -= length7;
                        }
                        this.mConnection.setSelection(this.mEnd, this.mEnd);
                    }
                }

                return true;
            } else if (keyCode == ConstantList.EDIT_DOWN) {
                // 向下
                final CharSequence nextLine2 = this.curOper.getNextLine(this.mFromWhichEnd);
                if (!nextLine2.toString().equals("")) {
                    int tempNum = this.curOper.getInvisibleCharsNumber(nextLine2);
                    final int length9 = nextLine2.length();
                    final int length10 = this.curOper.getToLineEnd(this.mFromWhichEnd).length();
                    final int length11 = this.curOper.getToLineStart(this.mFromWhichEnd).length();
                    if (this.mFromWhichEnd == this.FROMEND) {
                        if (length9 - tempNum < length11) {
                            this.mEnd = this.mEnd + length10 + length9 - tempNum;
                        } else {
                            this.mEnd = this.mEnd + length10 + length11;
                        }
                        this.mConnection.setSelection(this.mEnd, this.mEnd);
                    } else if (this.mFromWhichEnd == this.FROMSTART) {
                        if (length9 - tempNum < length11) {
                            this.mStart = this.mStart + length10 + length9 - tempNum;
                        } else {
                            this.mStart = this.mStart + length10 + length11;
                        }
                        this.mConnection.setSelection(this.mStart, this.mStart);
                    }
                }
                this.mConnection.setSelection(this.mEnd, this.mEnd);

                return true;
            } else if (keyCode == ConstantList.EDIT_BACK) {
                // 向左移
                int pos = mStart != mEnd ? mStart : mStart - 1;
                pos = Math.max(pos, 0);
                mConnection.setSelection(pos, pos);
                return true;
            } else if (keyCode == ConstantList.EDIT_FORWARD) {
                // 往右移
                int totalLength = mEnd + curOper.getAfterLength();
                int pos = mStart != mEnd ? mEnd : mEnd + 1;
                pos = Math.min(pos, totalLength);
                mConnection.setSelection(pos, pos);
                return true;
            } else if (keyCode == ConstantList.EDIT_TOLINESTART) {
                // 移到行头
                int tempNum = this.curOper.getToLineStart(this.FROMSTART).length();
                this.mConnection.setSelection(this.mStart - tempNum, this.mStart - tempNum);
                this.mFromWhichEnd = this.FROMEND;
                return true;
            } else if (keyCode == ConstantList.EDIT_TOLINEEND) {
                // 移到行尾
                int tempNum = this.curOper.getToLineEnd(this.FROMEND).length();
                this.mEnd = this.mEnd + tempNum - this.curOper.getInvisibleCharsNumber(this.curOper.getToLineEnd(this.FROMEND));
                this.mConnection.setSelection(this.mEnd, this.mEnd);
                this.mFromWhichEnd = this.FROMEND;
                return true;
            } else if (keyCode == ConstantList.EDIT_TOSTART) {
                // 移到头
                mConnection.setSelection(0, 0);
                return true;
            } else if (keyCode == ConstantList.EDIT_TOEND) {
                // 移到尾
                int totalLength = mEnd + curOper.getAfterLength();
                mConnection.setSelection(totalLength, totalLength);
                return true;
            }
            // /////////移动快捷键结束///////////////

            // ////////删除快捷键开始////////////////
            else if (keyCode == ConstantList.EDIT_DELETEALL) {
                // // 删除全部内容OKKKKKKKKKKKKKK
                mConnection.setSelection(mStart, mStart);// 这样可以确保光标不会处于选择状态
                int afterLength = curOper.getAfterLength();
                undoAutotext.update(0, 0, mConnection.getTextBeforeCursor(mStart + 1, 0).toString() + mConnection.getTextAfterCursor(afterLength, 0).toString(), "", Autotext.DEL);
                mConnection.deleteSurroundingText(mStart + 1, afterLength);
                mConnection.setSelection(0, 0);
                return true;
            } else if (keyCode == ConstantList.EDIT_DELETELINE) {
                // 删除行
                int tempNum = this.curOper.getToLineStart(this.FROMSTART).length();
                int ddd = this.curOper.getToLineEnd(this.FROMEND).length();
                this.mConnection.setSelection(this.mStart - tempNum, this.mEnd + this.curOper.getToLineEnd(this.FROMEND).length());
                if (this.mConnection.getSelectedText(0) == null) {
                    this.isSelectModel = false;
                } else {
                    undoAutotext.update(this.mStart - tempNum, this.mStart - tempNum, (String) mConnection.getSelectedText(0), "", Autotext.DEL);
                    mConnection.commitText("", 1);
                }
                return true;
            } else if (keyCode == ConstantList.EDIT_DELETEFORWARD) {
                // 删除后面一个字符
                undoAutotext.update(mEnd, mEnd, mConnection.getTextAfterCursor(1, 0).toString(), "", Autotext.DEL);
                mConnection.deleteSurroundingText(0, 1);
                return true;
            }
            // /////////删除快捷键结束/////////////////

            // ////////其他快捷键开始//////////////////
            else if (keyCode == ConstantList.EDIT_UNDO) {
                // undo功能
                // 其实现是把每次替换用一个Autotext对象“undoAutotext”记录下来
                // 然后把unAutotext对象中的input字符按位置放回去
                mConnection.setSelection(undoAutotext.getStart(), undoAutotext.getEnd());
                mConnection.commitText(undoAutotext.getInput(), 1);
                undoAutotext.update(undoAutotext.getStart(), undoAutotext.getStart() + undoAutotext.getInput().length(), undoAutotext.getAutotext(), undoAutotext.getInput(), Autotext.UNDO);
                return true;
            } else if (keyCode == ConstantList.EDIT_COPY) {
                // 复制OKKKKKKKKKKKKKKKKK
                if (mStart != mEnd) {
                    clipboard = (ClipboardManager) this.getSystemService(CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("AutotextInputMethod", mConnection.getSelectedText(InputConnection.GET_TEXT_WITH_STYLES));
                    clipboard.setPrimaryClip(clip);
                    mConnection.setSelection(mEnd, mEnd);

                    Toast.makeText(this, this.getString(R.string.copyed), Toast.LENGTH_SHORT).show();
                }
                return true;
            } else if (keyCode == ConstantList.EDIT_PASTE) {
                // 粘贴功能
                clipboard = (ClipboardManager) this.getSystemService(CLIPBOARD_SERVICE);
                CharSequence pasteText = "";
                ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
                pasteText = item.getText();
                if (mStart != mEnd) {
                    undoAutotext.update(mStart, mStart + pasteText.length(), mConnection.getSelectedText(0).toString(), pasteText.toString(), Autotext.DEL);
                } else {
                    undoAutotext.update(mStart, mStart + pasteText.length(), "", pasteText.toString(), Autotext.DEL);
                }
                mConnection.commitText(pasteText, 1);
                return true;
            } else if (keyCode == ConstantList.EDIT_CUT) {
                // 剪切OKKKKKKKKKKKKKKKKKK
                if (mStart != mEnd) {
                    undoAutotext.update(mStart, mStart, mConnection.getSelectedText(0).toString(), "", Autotext.DEL);
                    mConnection.performContextMenuAction(android.R.id.cut);
                }
                return true;
            } else if (keyCode == ConstantList.SWITCH_INPUTMETHOD) {
                //c-enter 切换输入词库
                methodItemNum = methodItemNum + 1 < methodItemList.size() ? methodItemNum + 1 : 0;
                defaultMethodId = methodItemList.get(methodItemNum).getId();
                methodName = methodItemList.get(methodItemNum).getName();
                dboper.addOrUpdateMethodItem(methodName, MethodItem.DEFAULT, defaultMethodId);
                bbKeyboard.setInputMethodName(methodName);
                return true;
            }
            //如果是其他没有定议的快捷键，则什么都不做
            return true;
        }

        //////选择模式////////////////
        if (!bbKeyboard.isSymOn() && !bbKeyboard.isCapOn() && !bbKeyboard.isCtrlOn() && !bbKeyboard.isAltOn() & isSelectModel) {
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
                    int tempNum = this.curOper.getInvisibleCharsNumber(preLine);
                    final int length2 = preLine.length();
                    final int length3 = this.curOper.getToLineStart(this.mFromWhichEnd).length();
                    if (this.mFromWhichEnd == this.FROMSTART) {
                        if (length2 - tempNum < length3) {
                            this.mStart = this.mStart - length3 - tempNum;
                        } else {
                            this.mStart -= length2;
                        }
                    } else if (this.mFromWhichEnd == this.FROMEND) {
                        if (length2 - tempNum < length3) {
                            this.mEnd = this.mEnd - length3 - tempNum;
                        } else {
                            this.mEnd -= length2;
                        }
                        if (this.mEnd < this.mStart) {
                            tempNum = this.mStart;
                            this.mStart = this.mEnd;
                            this.mEnd = tempNum;
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
                    int tempNum = this.curOper.getInvisibleCharsNumber(nextLine);
                    final int length4 = nextLine.length();
                    final int length5 = this.curOper.getToLineEnd(this.mFromWhichEnd).length();
                    final int length6 = this.curOper.getToLineStart(this.mFromWhichEnd).length();
                    if (this.mFromWhichEnd == this.FROMEND) {
                        if (length4 - tempNum < length6) {
                            this.mEnd = this.mEnd + length5 + length4 - tempNum;
                        } else {
                            this.mEnd = this.mEnd + length5 + length6;
                        }
                    } else if (this.mFromWhichEnd == this.FROMSTART) {
                        if (length4 - tempNum < length6) {
                            this.mStart = this.mStart + length5 + length4 - tempNum;
                        } else {
                            this.mStart = this.mStart + length5 + length6;
                        }
                        if (this.mStart > this.mEnd) {
                            tempNum = this.mEnd;
                            this.mEnd = this.mStart;
                            this.mStart = tempNum;
                            this.mFromWhichEnd = this.FROMEND;
                        }
                    }
                    this.mConnection.setSelection(this.mStart, this.mEnd);
                }
                return true;
            } else if (keyCode == ConstantList.EDIT_BACK) {
                // // 往左选
                if (mStart != mEnd && mFromWhichEnd == 1) {// 如果光标处于选择状态，并且标记位表明应该移动结束位
                    mEnd = Math.max((mEnd - 1), 0);
                } else {
                    mStart = Math.max((mStart - 1), 0);
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

        //核心功能，准备替换
        if (keyCode == ConstantList.SUBSTITUTION_TRIGGER) {// 触发正向替换字符
            isSelectModel = false;// 退出选择模式

            //一进入输入状态的时候光标就处于选择模式的处理分为三部分
            // 这里是第一部分，当输入空格的时候的处理
            // 第二部分，是输入backspace键时的处理
            // 第三部分，其他输入时候的处理
            // 基本的思路是把当前处于选择模式下的字符保存下来，在undo的时候用
            if (mStart != mEnd) {
                undoAutotext.update(mStart, mStart, mConnection.getSelectedText(0).toString(), " ", Autotext.SELECT_DEL);
                mConnection.commitText(" ", 1);
                return true;
            }

            // 如果是短按，接下来准备正向替换
            inputString = "";// 要被替换的文本
            autotextString = new StringBuilder();// 用于替换的文本

            // 判断是否是刚反向替换完
            if (autotext.getStat() == Autotext.REVERSE_AFTER) {//刚反向替换完
                // 取光标前相应长度的字符，或者取到头
                String rawInput = mConnection.getTextBeforeCursor(autotext.getAutotext().length(), 0).toString();
                //如果光标处于上次替换的结尾处，并且光标前的字符与上次输入的编码一样，那么就不替换，直接输出一个空格
                if (rawInput.equals(autotext.getAutotext()) && mEnd == autotext.getEnd()) {
                    mConnection.commitText(" ", 1);
                    autotext.clear();
                    return true;
                }
            }

            // 如果不是正好在反向替换后，那么就开始正常偿试替换的过程
            int maxInputLength = dboper.getMaxInputLength(defaultMethodId);
            ;// 表中最长的input的长度，用于在正向替换的时候，最长需要从光标前面取多长的文本
            CharSequence candidateInput = mConnection.getTextBeforeCursor(maxInputLength + 1, 0);

            //如果不在最开头，或者词库中有条目
            char c;
            for (offsetBefore = 1; offsetBefore <= candidateInput.length(); offsetBefore++) {// 从当前位置开始往前找
                c = candidateInput.charAt(candidateInput.length() - offsetBefore);
                if (c == ConstantList.SUBSTITUTION_SEPERRATOR || c == '\n') {// 如果找到了替换分隔符或者行首
                    break;
                }
            }
            offsetBefore--;//因为上面的for循环最后会再加1
            candidateInput = candidateInput.subSequence(candidateInput.length() - offsetBefore, candidateInput.length());
            //到此已经要输入的编码找出来了，不包含之前的空格。如果字串中没有空格或行首，则candidateInput现在包含取出的所有字符，比词库中最长的input还多一位

            String rawAutotext = dboper.searchAutotext("autotext" + defaultMethodId, candidateInput.toString());// 在库中查找替换项
            if (rawAutotext == null) {// 如果没有找到替换项，替换失败
                mConnection.commitText(" ", 1);
                //把失败的字串保存起来，后面%w宏命令要用到
                autotext.update(mEnd - candidateInput.length(), mEnd + 1, candidateInput.toString(), candidateInput.toString() + " ", Autotext.FAIL);
                return true;
            } else {// 如果找到了替换项
                //开始扫描替换项
                //处理宏的过程就是处理两个变量，一个是offsetBefore ，它表示要替换当前光标前的几个字符，获得input字串
                //另一个是rawAutotext，它根据宏来变化，最后获得autotext字串
                // 先把触发替换的空格加上，这样扫描到最的一个字符就肯定不是宏字符%
                rawAutotext = rawAutotext + " ";
                int macroBnumber = 0; //记录%B的数量
                for (int i = 0; i < rawAutotext.length(); i++) {
                    c = rawAutotext.charAt(i);
                    if (c == ConstantList.MACRO_MACROCHARACTER) {// 如果扫描到宏命令的字符%
                        char nextC = rawAutotext.charAt(i + 1);//因为在rawAutotext中预添加了一个空格，所以肯定这里不会出错，因为不可能最后一个字符是%
                        switch (nextC) {
                            case ConstantList.MACRO_DELETEBACK:// %b 在前面删除一个字符
                                offsetBefore++;
                                i++;
                                break;
                            case ConstantList.MACRO_DELETEWORD:// %w 删除刚替换的单词，或者错误的输入
                                offsetBefore += autotext.getAutotext().length();
                                offsetBefore--;
                                CharSequence tempS = mConnection.getTextBeforeCursor(candidateInput.length() + autotext.getAutotext().length() + 1, 0);
                                //下面的条件是：上一次替换不成功，或者已经取到了最开头，或者上一次替换是一行的开头
                                if (autotext.getStat() != Autotext.AFTER || tempS.length() != candidateInput.length() + autotext.getAutotext().length() + 1 || tempS.charAt(0) == '\n') {
                                    macroBnumber++;
                                }
                                i++;
                                break;
                            case ConstantList.MACRO_DELETEFORWARD:// %B 在后面删除一个字符
                                macroBnumber++;
                                i++;
                                break;
                            case ConstantList.MACRO_DATE:// %d   date
                                String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date(System.currentTimeMillis()));
                                autotextString.append(date);
                                i++;
                                break;
                            case ConstantList.MACRO_LONGDATE:// %D  date and time
                                String datetime = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(System.currentTimeMillis()));
                                autotextString.append(datetime);
                                i++;
                                break;
                            case ConstantList.MACRO_TIME:// %t  time
                                String time = new SimpleDateFormat("HH:mm").format(new Date(System.currentTimeMillis()));
                                autotextString.append(time);
                                i++;
                                break;
                            default:
                                autotextString.append(ConstantList.MACRO_MACROCHARACTER);
                        }
                    } else {// 是普通字符
                        autotextString.append(c);
                    }
                }

                // 接下来是替换过程
                // 首先，根据前面的扫描结果，获得inputString 和  autotextString
                inputString = mConnection.getTextBeforeCursor(offsetBefore, 0).toString();
                if (autotextString.length() - macroBnumber < 0)
                    macroBnumber = autotextString.length();
                autotextString.delete(autotextString.length() - macroBnumber, autotextString.length());

                // 如果将要被替换的字串中有换行符，处理以换行符作为分隔的情况，相当于是换行符就是最开头
                int j = inputString.lastIndexOf('\n');
                if (j != -1) inputString = inputString.substring(j + 1);
                final int length = inputString.length();


                // 第二，记录替换块的信息，不能放到后面记录，因为mAfterSubString会变，可能需要加上空格
                autotext.update(mStart - length, mStart - length + autotextString.length(), inputString, autotextString.toString(), Autotext.AFTER);
                undoAutotext.update(mStart - length, mStart - length + autotextString.length(), inputString, autotextString.toString(), Autotext.AFTER);
                // 第三，准备好了之后，最后才替换，同时强制更新光标位置
                mConnection.setSelection(Math.max(mEnd - inputString.length(), 0), mEnd);
                mConnection.commitText(autotextString, 1);

                return true;
            }
            // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        } else if (keyCode == ConstantList.SUBSTITUTION_TRIGGER_REVERSE) {// 触发反向替换的字符，为"backspace"键
            isSelectModel = false;
            //一进入输入状态的时候光标就处于选择模式的处理分为三部分
            // 第一部分，当输入空格的时候的处理
            // 这里是第二部分，是输入backspace键时的处理
            // 第三部分，其他输入时候的处理
            // 基本的思路是把当前处于选择模式下的字符保存下来，在undo的时候用
            if (mStart != mEnd) {
                undoAutotext.update(mStart, mStart, mConnection.getSelectedText(0).toString(), "", Autotext.SELECT_DEL);
                mConnection.commitText("", 1);
                return true;
            }

            // 触发反向替换的条件是：
            // 1、Autotext对象的状态为 Autotext.AFTER ，即替换成功状态
            // 2、Autotext对象中，autotext 不能为空白字串。首先，input不可能为空，要不也不会上一次替换成功。
            //      如果autotext为空白字串，则说明上次替换是类似于删除的动作，如果使用反向替换功能的话，会出现在空格上按删除键，反而出现很多字符的怪现象。
            //      如果想恢复原来输入的字符，可以用undo功能来实现
            // 3、光标正好位于autotext字串trim后的位置
            // 4、光标前的字符与autotext字串trim后相同
            //
            //
            // 下面是反向替换过程
            // 先把autotext后面的空格删掉
            String tempA = autotext.getAutotext();
            for (int i = 1; i <= tempA.length(); i++) {
                if (tempA.charAt(tempA.length() - i) != ' ') {
                    tempA = tempA.substring(0, tempA.length() - i + 1);
                    break;
                }
            }

            if ((autotext.getStat() == Autotext.AFTER) &&
                    !TextUtils.isEmpty(autotext.getAutotext().replaceAll("[ \\t\\n\\x0B\\f\\r]", "")) &&
                    mEnd == autotext.getEnd() - (autotext.getAutotext().length() - tempA.length()) &&
                    mConnection.getTextBeforeCursor(tempA.length(), 0).toString().equals(tempA)
            ) {
                // 开始反向替换
                mConnection.setSelection(mEnd - tempA.length(), mEnd);
                mConnection.commitText(autotext.getInput(), 1);

                // 记录替换块信息的变化，位置更新，字串也反过来了
                autotext.update(mStart - tempA.length(), mStart - tempA.length() + autotext.getInput().length(),
                        tempA, autotext.getInput(), Autotext.REVERSE_AFTER);
                undoAutotext.update(mStart - tempA.length(), mStart - tempA.length() + autotext.getAutotext().length(),
                        tempA, autotext.getAutotext(), Autotext.REVERSE_AFTER);//注意：在上一行中，autotext的内容已经改变了
                return true;
            } else {// 删除光标前一个字符
                undoAutotext.update(mStart - 1, mStart - 1, mConnection.getTextBeforeCursor(1, 0).toString(), "", Autotext.DEL);
                mConnection.deleteSurroundingText(1, 0);
                return true;
            }

        }
        // ////////其他快捷键结束//////////////////
        else {
            if (keyCode == ConstantList.SUBSTITUTION_ENTER || keyCode == ConstantList.SUBSTITUTION_NUMPAD_ENTER) {// 如果输入回车健
                isSelectModel = false;
                //如果是多行的输入框，刚回车表示换行
                if ((mEditInfo.inputType & InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0) {
                    mConnection.commitText("\n", 1);
                } else {//如果是单行的输入框，回车表示执行输入框定义的动作，比如go search等
                    mConnection.performEditorAction(mEditInfo.imeOptions & EditorInfo.IME_MASK_ACTION);
                }
            }
            else {
                KeyCharacterMap kcm = event.getKeyCharacterMap();
                char c;
                if (event.getRepeatCount() == 0) {// 短按 的处理
                    if (!hasHardKeyboard && bbKeyboard.getCharactor(keyCode, false, false) != '\0'){
                        c = bbKeyboard.getCharactor(keyCode, false, false);
                    }
                    else if(hasHardKeyboard && kcm.isPrintingKey(keyCode)){
                        c = (char) kcm.get(keyCode, mMetaState);
                    }
                } else if (event.getRepeatCount() == 1) {//长按 的处理
                    if (!hasHardKeyboard && bbKeyboard.getCharactor(keyCode, false, true) != '\0') {
                        c = bbKeyboard.getCharactor(keyCode, false, true);
                    }
                    else if(hasHardKeyboard && kcm.isPrintingKey(keyCode)){
                        c = (char) kcm.get(keyCode, KeyEvent.META_CAPS_LOCK_ON);
                    }
                    mConnection.deleteSurroundingText(1, 0);
                } else {
                    return true;
                }

                //一进入输入状态时光标就处于选择模式的处理分为三部分
                // 第一部分，当输入空格的时候的处理
                // 第二部分，是输入backspace键时的处理
                // 这里是第三部分，其他输入时候的处理
                // 基本的思路是把当前处于选择模式下的字符保存下来，在undo的时候用
                if (mStart != mEnd) {
                    undoAutotext.update(mStart, mStart + 1, mConnection.getSelectedText(0).toString(), String.valueOf(c), Autotext.SELECT_DEL);
                }
                mConnection.commitText(String.valueOf(c), 1);
            } return true;
        }
        //return super.onKeyDown(keyCode, event);
    }

    // /////////////////////////////////////////////////////////////////////////
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        state = HandleMetaKey.handleKeyUp(state, keyCode);
        mMetaState = HandleMetaKey.getMetaState(state);
        bbKeyboard.set_status_line_view(mMetaState, methodName);
        return super.onKeyUp(keyCode, event);
    }

    // ///////////////////////////////////////////////////////////////////////
    @Override
    public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd, int candidatesStart, int candidatesEnd) {
        currentCursorStart = newSelStart;
        currentCursorEnd = newSelEnd;
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd);
    }

    //处理软键盘输入事件
    @Override
    public void onClick(View v) {
        int metaState = bb_keyboard_getmetastate(v);
        int code = Integer.parseInt((String) v.getTag());
        KeyEvent clickEvent = new KeyEvent(System.currentTimeMillis(), System.currentTimeMillis(), KeyEvent.ACTION_DOWN, code, 0, metaState);
        this.onKeyDown(code, clickEvent);
        clickEvent = new KeyEvent(System.currentTimeMillis(), System.currentTimeMillis(), KeyEvent.ACTION_UP, code, 0, metaState);
        this.onKeyUp(code, clickEvent);
    }

    //处理软健盘输入长按事件
    @Override
    public boolean onLongClick(View v) {
        int metaState = bb_keyboard_getmetastate(v);
        int code = Integer.parseInt((String) v.getTag());
        KeyEvent clickEvent = new KeyEvent(System.currentTimeMillis(), System.currentTimeMillis(), KeyEvent.ACTION_DOWN, code, 0, metaState);
        this.onKeyDown(code, clickEvent);
        clickEvent = new KeyEvent(System.currentTimeMillis(), System.currentTimeMillis(), KeyEvent.ACTION_DOWN, code, 1, metaState);
        this.onKeyDown(code, clickEvent);
        clickEvent = new KeyEvent(System.currentTimeMillis(), System.currentTimeMillis(), KeyEvent.ACTION_UP, code, 0, metaState);
        this.onKeyUp(code, clickEvent);
        return true;//如果设置为false，刚在释放按纽的时候，还会再调用一次onClick()
    }

    private int bb_keyboard_getmetastate(View v) {//用于在软键盘输入时获取 功能键 的状态
        int metaState = 0;
        int tag = Integer.parseInt((String) v.getTag());
        if (tag == 57) metaState = KeyEvent.META_ALT_LEFT_ON;
        if (tag == 59) metaState = KeyEvent.META_SHIFT_LEFT_ON;
        if (tag == 63) metaState = KeyEvent.META_SYM_ON;
        if (tag == 60) metaState = KeyEvent.META_SHIFT_RIGHT_ON;
        return metaState;
    }

}
