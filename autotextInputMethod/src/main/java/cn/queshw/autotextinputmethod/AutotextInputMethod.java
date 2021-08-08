package cn.queshw.autotextinputmethod;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.inputmethodservice.InputMethodService;
import android.text.InputType;
import android.text.TextUtils;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import cn.queshw.autotextsetting.DBOperations;
import cn.queshw.autotextsetting.MethodItem;

@SuppressLint("SimpleDateFormat")
public class AutotextInputMethod extends InputMethodService implements View.OnClickListener,View.OnLongClickListener{
    private final int FROMEND = 1;
    private final int FROMSTART = 0;

    private InputConnection mConnection;
    private EditorInfo mEditInfo;
    private int currentCursorStart = -1;// 当前光标开始位置，用于从onUpdateSelection中获最新光标位置
    private int currentCursorEnd = -1;// 当前光标结束位置，用于从onUpdateSelection中获最新光标位置
    private int offsetBefore;// 相对当前光标
    private int offsetAfter;// 相对当前光标

    private int mMetaState = 0;
    private long state = 0; // for metakeylistener

    private String inputString;// 替换前的文本
    private StringBuilder autotextString;// 替换后的文本
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
    //private CurrentStatusIcon curStatusIcon;// 当前的状态图标
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
    private EmojiBoard emojiBoard;
    private View emojiKeyboard;
    private boolean NEXT = true;
    private boolean PRE = false;

    // 用于软健盘
    private View bb_keyboard;//整个软键盘，包括按键和状态样栏
    private ImageView metakey_staus;//状态栏上的功能键状态显示图标
    private TextView inputMethodName;//状态栏上的当前使用的输入词库名称
    private ImageView setting;//状态栏上的设置按纽，直接进入词库设置界面
    boolean hasHardKeyboard = false;//手机是否有物理键盘
    BBkeyboardMap BBsoftKeyboardMap;//用于对应软键盘
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
        emojiKeyboard = this.getLayoutInflater().inflate(R.layout.emoji_keyboard_layout, null);
        this.setCandidatesView(emojiKeyboard);
        emojiBoard = new EmojiBoard(emojiKeyboard);

        //初始化软键盘和字符映射表
        BBsoftKeyboardMap = new BBkeyboardMap();
        bb_keyboard = this.getLayoutInflater().inflate(R.layout.bb_keyboard, null);

        //设置软键盘上按键的监听器
        BbKeyBoard.initBbKeyboard(bb_keyboard, this, this);

        //设置软键盘的状态栏
        metakey_staus = (ImageView) bb_keyboard.findViewById(R.id.ImageView_status);
        inputMethodName = (TextView) bb_keyboard.findViewById(R.id.textView_inputmethod);
        setting = (ImageView) bb_keyboard.findViewById(R.id.ImageView_setting);
        setting.setOnClickListener(new View.OnClickListener() {//设置按键监听器
            @Override
            public void onClick(View v) {
                Intent intent = getPackageManager().getLaunchIntentForPackage("cn.queshw.autotextinputmethod");
                if (intent != null) {
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
            }
        });
        this.setInputView(bb_keyboard);

    }

    public void onConfigurationChanged(Configuration newConfig) {
        View bb_keyboard_only = (LinearLayout) bb_keyboard.findViewById(R.id.bb_keyboard_keyonly);
        if(newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_YES){//没有物理键盘
            bb_keyboard_only.setVisibility(View.VISIBLE);
            hasHardKeyboard = false;
        }else{//有物理健盘
            bb_keyboard_only.setVisibility(View.GONE);
            hasHardKeyboard = true;
        }
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
        curOper = new CursorOperator(mConnection);
        state = 0L;
        //curStatusIcon = CurrentStatusIcon.NONE;

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
            Toast.makeText(this, this.getString(R.string.msg6), Toast.LENGTH_SHORT).show();
            inputMethodName.setText(R.string.msg7);
            return;
        }

        for (int i = 0; i < methodItemList.size(); i++) {
            MethodItem item = methodItemList.get(i);
            if (item.getIsDefault() == MethodItem.DEFAULT)
                selectedMethodPostion = i;
        }
        defaultMethodId = methodItemList.get(selectedMethodPostion).getId();
        inputMethodName.setText(methodItemList.get(selectedMethodPostion).getName());
        //Toast.makeText(this, methodItemList.get(selectedMethodPostion).getName(), TOASTDURATION).show();
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
        super.onEvaluateInputViewShown();
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see android.inputmethodservice.InputMethodService#onFinishInput()
     */
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
            inputString = "";// 替换前的文本
            autotextString = new StringBuilder();// 替换后的文本

            if (mStart != mEnd) {// 处于选择模式
                mConnection.commitText("", 1);
                mConnection.setSelection(mStart, mStart);
                mEnd = mStart;
            }

            // 判断是否是刚反向替换完
            if (autotext.getStat() == Autotext.REVERSE_AFTER) {//刚反向替换完
                // 取光标前相应长度的字符，或者取到头
                String rawInput = mConnection.getTextBeforeCursor(autotext.getInput().length(), 0).toString();
                //如果光标处于上次替换的结尾处，并且光标前的字符与上次输入的编码一样，那么就不替换，直接输出一个空格
                if (mEnd == autotext.getEnd() && rawInput.equals(autotext.getInput())) {
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
                if (c == ConstantList.SUBSTITUTION_SEPERRATOR || c == '\n') {// 如果找到了替换分隔符或者行首
                    break;
                }
            }
            offsetBefore--;
            candidateInput = candidateInput.subSequence(candidateInput.length() - offsetBefore, candidateInput.length());

            String rawAutotext = dboper.searchAutotext("autotext" + defaultMethodId, candidateInput.toString());// 在库中查找替换项
            if (rawAutotext == null) {// 如果没有找到替换项，替换失败
                mConnection.commitText(" ", 1);
                //autotext.beforeString = candidateInput.toString();
                //把失败的字串保存起来，后面%w宏命令要用到
                autotext.update(mEnd - candidateInput.length(), mEnd, candidateInput.toString(), "", Autotext.FAIL);
                return true;
            } else {// 如果找到了替换项
                // 开始扫描替换项
                int macroBnumber = 0;// 用于记录%B宏命令的个数
                for (int i = 0; i < rawAutotext.length(); i++) {
                    c = rawAutotext.charAt(i);
                    // 如果扫描到宏命令的字符%
                    if (c == ConstantList.MACRO_MACROCHARACTER) {
                        c = (i + 1 < rawAutotext.length()) ? rawAutotext.charAt(i + 1) : ConstantList.MACRO_MACROCHARACTER;// '%'不能是最后一个字符，否则就当作是普通字符
                        switch (c) {
                            case ConstantList.MACRO_DELETEBACK:// 在前面删除一个字符
                                if (autotextString.length() != 0) {
                                    autotextString.deleteCharAt(autotextString.length() - 1);
                                } else {
                                    offsetBefore++;
                                }
                                i++;
                                break;
                            case ConstantList.MACRO_DELETEWORD:// 删除刚替换的单词，或者错误的输入
                                if(autotext.getStat() == Autotext.AFTER){//如果再上一次替换成功了
                                    //则删除上一次成功替换的内容
                                    //这是第二个循环分支，在下一行计算前，offsetBefore已经在上一个循环中加1了，因为前面有%b
                                    offsetBefore += autotext.getEnd() - autotext.getStart();
                                    //如果当前光标位置减去offsetBefore等于0，这说明要删除到头上，那么在用于触发正向替换的空格也不需要了
                                    if(mEnd - offsetBefore == 0) macroBnumber++;
                                }else if(autotext.getStat() == Autotext.FAIL){//如果再上一次替换失败
                                    offsetBefore += autotext.getEnd() - autotext.getStart();
                                    macroBnumber++;
                                }
                                i++;
                                break;
                            case ConstantList.MACRO_DELETEFORWARD:// 在后面删除一个字符
                                macroBnumber++;
                                i++;
                                break;
                            case ConstantList.MACRO_DATE:// date
                                String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date(System.currentTimeMillis()));
                                autotextString.append(date);
                                i++;
                                break;
                            case ConstantList.MACRO_LONGDATE:// date
                                String datetime = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(System.currentTimeMillis()));
                                autotextString.append(datetime);
                                i++;
                                break;
                            case ConstantList.MACRO_TIME:// time
                                String time = new SimpleDateFormat("HH:mm").format(new Date(System.currentTimeMillis()));
                                autotextString.append(time);
                                i++;
                                break;
                            default:
                                autotextString.append(ConstantList.MACRO_MACROCHARACTER);
                                //mAfterSubString.append(c);
                        }
                    } else {// 是普通字符
                        autotextString.append(c);
                    }
                }

                // 接下来是替换过程
                // 首先，根据前面的扫描结果，获得要被替换的文本
                offsetAfter = macroBnumber <= 1 ? 0 : macroBnumber - 1;// 因为后面还有一个刚输入的空格需要考虑到
                offsetAfter = mConnection.getTextAfterCursor(offsetAfter, 0).length();
                offsetBefore = mConnection.getTextBeforeCursor(offsetBefore, 0).length();
                inputString = mConnection.getTextBeforeCursor(offsetBefore, 0).toString()
                        + mConnection.getTextAfterCursor(offsetAfter, 0).toString();

                // 处理以换行符作为分隔的情况，相当于是换行符就是最开头
                final int length = inputString.length();
                // Log.d("Here", "before = |" + this.mBeforeSubString + "|");
                for (int j = 0; j < length; ++j) {
                    if (inputString.charAt(length - 1 - j) == '\n') {
                        inputString = inputString.substring(length - j);
                        offsetBefore = inputString.length();
                        break;
                    }
                }

                // 第二，记录替换块的信息，不能放到后面记录，因为mAfterSubString会变，可能需要加上空格
                /*autotext.start = mStart - offsetBefore;
                autotext.end = autotext.start + mAfterSubString.length();
                autotext.beforeString = mBeforeSubString;
                autotext.afterString = mAfterSubString.toString();*/
                autotext.update(mStart - offsetBefore, mStart - offsetBefore + autotextString.length(), inputString, autotextString.toString(), Autotext.AFTER);

                // 第三，构建用于替换的文本，看是否要加上空格
                autotextString = macroBnumber == 0 ? autotextString.append(ConstantList.SUBSTITUTION_SEPERRATOR) : autotextString;

                // 第四，准备好了之后，最后才替换，同时强制更新光标位置

                mConnection.deleteSurroundingText(offsetBefore, offsetAfter);
                mConnection.commitText(autotextString, 1);
                mConnection.setSelection(autotext.getStart() + autotextString.length(), autotext.getStart() + autotextString.length());

                return true;
            }
            // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        } else if (keyCode == ConstantList.SUBSTITUTION_TRIGGER_REVERSE) {// 触发反向替换的字符，为"backspace"键
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
            // 3、autotext对象的状态为 Autotext.AFTER ，即替换成功状态

            // 下面是反向替换过程
            //先找出光标前的相应字符
            String rawInput = mConnection.getTextBeforeCursor(autotext.getAutotext().length(), 0).toString();
            if(!TextUtils.isEmpty(autotext.getAutotext()) &&
                    !TextUtils.isEmpty(autotext.getInput()) &&
                    mEnd == autotext.getEnd() &&
                    rawInput.equals(autotext.getAutotext()) &&
                    (autotext.getStat() == Autotext.AFTER))
            {
                // 开始反向替换
                mConnection.deleteSurroundingText(autotext.getEnd() - autotext.getStart(), 0);
                mConnection.commitText(autotext.getInput(), 1);

                // 记录替换块信息的变化，并强制更新光标位置
                autotext.update(mEnd - autotext.getAutotext().length(), mEnd - autotext.getAutotext().length() + autotext.getInput().length(),
                        autotext.getInput(), autotext.getAutotext(), Autotext.REVERSE_AFTER);
                mConnection.setSelection(autotext.getEnd(), autotext.getEnd());
                return true;
            }else{// 删除光标前一个字符
                mUndoSubString = mConnection.getTextBeforeCursor(1, 0).toString();
                mConnection.deleteSurroundingText(1, 0);
                return true;
            }

            /*// 如果 条件1:替换块不能为空  不满足
            if (TextUtils.isEmpty(autotext.getAfterString()) || TextUtils.isEmpty(autotext.getBeforeString())) {
                mUndoSubString = mConnection.getTextBeforeCursor(1, 0).toString();
                mConnection.deleteSurroundingText(1, 0);
                return true;
            }

            // 如果 条件2 当前光标前的字符与替换块的完全相同，并且光标的位置与替换块的结束标记相同 不满足
            String rawInput = mConnection.getTextBeforeCursor(autotext.getAfterString().length(), 0).toString();
            if (mEnd != autotext.getEnd() || !rawInput.equals(autotext.getAfterString())) {
                mUndoSubString = mConnection.getTextBeforeCursor(1, 0).toString();
                mConnection.deleteSurroundingText(1, 0);
                return true;
            }*/
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
            pos = Math.max(pos, 0);
            mConnection.setSelection(pos, pos);
            return true;
        } else if (isCtrlPressed && keyCode == ConstantList.EDIT_FORWARD) {
            // 往右移
            isSelectModel = false;
            int totalLength = mEnd + curOper.getAfterLength();
            int pos = mStart != mEnd ? mEnd : mEnd + 1;
            pos = Math.min(pos, totalLength);
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
            mConnection.setSelection(0, 0);
            return true;
        } else if (isCtrlPressed && keyCode == ConstantList.EDIT_TOEND) {
            // 移到尾
            isSelectModel = false;
            int totalLength = mEnd + curOper.getAfterLength();
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

                Toast.makeText(this, this.getString(R.string.copyed), Toast.LENGTH_SHORT).show();
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
            //c-enter 切换输入词库
			selectedMethodPostion = selectedMethodPostion + 1 < methodItemList.size() ? selectedMethodPostion + 1 : 0;
			defaultMethodId = methodItemList.get(selectedMethodPostion).getId();
			dboper.addOrUpdateMethodItem(methodItemList.get(selectedMethodPostion).getName(), MethodItem.DEFAULT, defaultMethodId);
            inputMethodName.setText(methodItemList.get(selectedMethodPostion).getName());
			return true;
        }
        // ////////其他快捷键结束//////////////////
        else if (isSymPressed) {//用于表情键盘的往前翻页
            // Log.d("Here", "sym pressed there");

            isSelectModel = false;
            if (keyCode == KeyEvent.KEYCODE_0) {// 往前翻页
                this.symBoardTurn(stickerStartPosition, PRE);
                state |= KeyEvent.META_SYM_ON | HandleMetaKey.META_SYM_RELEASED;
                return true;
            }

            String temp = emojiBoard.getSticker(keyCode);
            if (!temp.equals("NONE")) {
                mConnection.commitText(temp, 1);
            }
            return true;
        } else if (keyCode == ConstantList.SUBSTITUTION_ENTER || keyCode == ConstantList.SUBSTITUTION_NUMPAD_ENTER) {// 如果输入回车健
            isSelectModel = false;
            //如果是多行的输入框，刚回车表示换行
            if((mEditInfo.inputType & InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0){
                mConnection.commitText("\n",1);
            }else{//如果是单行的输入框，回车表示执行输入框定义的动作，比如go search等
                mConnection.performEditorAction(mEditInfo.imeOptions & EditorInfo.IME_MASK_ACTION);
            }
            return true;
        } else {
            KeyCharacterMap kcm = event.getKeyCharacterMap();

            if (kcm.isPrintingKey(keyCode)) {
                if (isCtrlPressed)// 说明正处于快捷命令模式中
                    return true;

                isSelectModel = false;
                char c;
                if (event.getRepeatCount() == 0) {// 短按
                    if(hasHardKeyboard == false) c = BBsoftKeyboardMap.getCharactor(keyCode, isAltPressed,isCapPressed);
                    else c = (char) kcm.get(keyCode, mMetaState);
                } else if (event.getRepeatCount() == 1) {// 长按大写
                    if(hasHardKeyboard == false) c = BBsoftKeyboardMap.getCharactor(keyCode, isAltPressed,true);
                    else c = (char) kcm.get(keyCode, KeyEvent.META_CAPS_LOCK_ON);
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
        state = HandleMetaKey.handleKeyUp(state, keyCode, event);

        if (isSymPressed && (state & HandleMetaKey.META_SYM_TURNPAGE) != 0 && (state & HandleMetaKey.META_SYM_RELEASED) != 0) {// 说明表情小键盘要翻页
            // 翻页
            this.symBoardTurn(stickerStartPosition, NEXT);
        }
        mMetaState = HandleMetaKey.getMetaState(state);
        handleStatusIcon(mMetaState);
        return super.onKeyUp(keyCode, event);
    }

    // ///////////////////////////////////////////////////////////////////////
    @Override
    public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd, int candidatesStart, int candidatesEnd) {
        //Log.d("Here", "newSelS = " + String.valueOf(newSelStart) + " | newSelE = " + String.valueOf(newSelEnd));
        //Log.d("Here", "oldSelS = " + String.valueOf(oldSelStart) + " | oldSelE = " + String.valueOf(oldSelEnd));
        currentCursorStart = newSelStart;
        currentCursorEnd = newSelEnd;

        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd);
    }

    // /////////////////////////////////////////////
    private void setMetaKeyStatus(int mMetaState) {
        if ((mMetaState & KeyEvent.META_ALT_LEFT_ON) != 0) {// 按了alt键
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
            stickerStartPosition -= 2 * emojiBoard.getStickerNumbersPerPage();
            if (stickerStartPosition < 0)
                stickerStartPosition = 0;
        }

        emojiBoard.setStickerKeyboard(stickerStartPosition);
        stickerStartPosition += emojiBoard.getStickerNumbersPerPage();
        if (stickerStartPosition >= emojiBoard.getEmojiNumbers())
            stickerStartPosition = 0;

        // 清空
        state &= ~HandleMetaKey.META_SYM_TURNPAGE;
    }

    // /////////////////////////////////////////////
    private void handleStatusIcon(int mMetaState) {
        // Log.d("Here", "handleStatusIcon = " +
        // String.valueOf(Integer.toBinaryString(mMetaState)));
        //CurrentStatusIcon icon;
        if ((mMetaState & KeyEvent.META_SYM_ON) != 0) {// sym on
            this.setCandidatesViewShown(true);// 显示表情小键盘
        } else {
            this.setCandidatesViewShown(false);// 关闭表情小键盘
            stickerStartPosition = 0;

            if ((mMetaState & HandleMetaKey.META_CAP_LOCKED) != 0) {// cap locked
                metakey_staus.setImageResource(R.drawable.status_cap_lock);
            } else if ((mMetaState & KeyEvent.META_SHIFT_RIGHT_ON) != 0) {// cap on
                metakey_staus.setImageResource(R.drawable.status_cap);


            } else if ((mMetaState & HandleMetaKey.META_ALT_LOCKED) != 0) {// alt locked
                metakey_staus.setImageResource(R.drawable.status_alt_lock);
            } else if ((mMetaState & KeyEvent.META_ALT_LEFT_ON) != 0) {// alt on
                metakey_staus.setImageResource(R.drawable.status_alt);

            } else if ((mMetaState & HandleMetaKey.META_SYM_LOCKED) != 0) {// sym locked
                metakey_staus.setImageResource(R.drawable.status_sym_lock);
            } else if ((mMetaState & HandleMetaKey.META_NEWSIM_LOCKED) != 0) {// ctrl locked
                metakey_staus.setImageResource(R.drawable.status_ctrl_lock);


            } else if ((mMetaState & KeyEvent.META_SHIFT_LEFT_ON) != 0) {// ctrl on
                metakey_staus.setImageResource(R.drawable.status_ctrl);


            } else {
                metakey_staus.setImageResource(R.drawable.status_normal);
            }
        }
    }

    //处理软键盘输入事件
    @Override
    public void onClick(View v) {
        int metaState = bb_keyboard_getmetastate(v);
        int code = Integer.parseInt( (String) v.getTag());
        KeyEvent clickEvent = new KeyEvent(System.currentTimeMillis(), System.currentTimeMillis(), KeyEvent.ACTION_DOWN, code, 0, metaState);
        this.onKeyDown(code, clickEvent);
        clickEvent = new KeyEvent(System.currentTimeMillis(), System.currentTimeMillis(), KeyEvent.ACTION_UP, code, 0, metaState);
        this.onKeyUp(code, clickEvent);
    }

    //处理软健盘输入长按事件
    @Override
    public boolean onLongClick(View v) {
        int metaState = bb_keyboard_getmetastate(v);
        int code = Integer.parseInt( (String) v.getTag());
        KeyEvent clickEvent = new KeyEvent(System.currentTimeMillis(), System.currentTimeMillis(), KeyEvent.ACTION_DOWN, code, 0, metaState);
        this.onKeyDown(code, clickEvent);
        clickEvent = new KeyEvent(System.currentTimeMillis(), System.currentTimeMillis(), KeyEvent.ACTION_DOWN, code, 1, metaState);
        this.onKeyDown(code, clickEvent);
        clickEvent = new KeyEvent(System.currentTimeMillis(), System.currentTimeMillis(), KeyEvent.ACTION_UP, code, 0, metaState);
        this.onKeyUp(code, clickEvent);
        return true;//如果设置为false，刚在释放按纽的时候，还会再调用一次onClick()
    }

    private int bb_keyboard_getmetastate(View v){//用于在软键盘输入时获取 功能键 的状态
        int metaState = 0;
        int tag = Integer.parseInt( (String) v.getTag());
        if( tag == 57) metaState = KeyEvent.META_ALT_LEFT_ON;
        if( tag == 59) metaState = KeyEvent.META_SHIFT_LEFT_ON;
        if( tag == 63) metaState = KeyEvent.META_SYM_ON;
        if( tag == 60) metaState = KeyEvent.META_SHIFT_RIGHT_ON;
        return metaState;
    }
}
