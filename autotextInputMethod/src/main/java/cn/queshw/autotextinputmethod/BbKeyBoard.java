package cn.queshw.autotextinputmethod;

import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class BbKeyBoard {
    Context con;
    // 用于软健盘
    private View bb_keyboard;//整个软键盘，包括按键和状态栏
    private View keyonly_view;//键盘的按键部分，不包括状态栏
    private View status_line_view;//键盘的状态栏部V分
    private boolean isAltOn = false;
    private boolean isCtrlOn = false;
    private boolean isCapOn = false;
    private boolean isSymOn = false;

    //状态栏的几个键
    private ImageView imageView_cap_status;
    private ImageView imageView_alt_status;
    private ImageView imageView_ctrl_status;
    private ImageView imageView_sym_status;
    private TextView textView_inputMethodName;//状态栏上的当前使用的输入词库名称
    private ImageView imageView_setting;//状态栏上的设置按纽，直接进入词库设置界面
    BBkeyboardMap BBsoftKeyboardMap;//用于对应软键盘

    BbKeyBoard() {
    }

    BbKeyBoard(Context con, View.OnClickListener clickListener, View.OnLongClickListener longClickListener) {
        this.con = con;
        //初始化软键盘和字符映射表
        BBsoftKeyboardMap = new BBkeyboardMap();
        bb_keyboard = View.inflate(con, R.layout.bb_keyboard, null);
        keyonly_view = bb_keyboard.findViewById(R.id.bb_keyboard_keyonly);

        status_line_view = bb_keyboard.findViewById(R.id.status_line_view);
        imageView_cap_status = bb_keyboard.findViewById(R.id.cap_status);
        imageView_alt_status = bb_keyboard.findViewById(R.id.alt_status);
        imageView_ctrl_status = bb_keyboard.findViewById(R.id.ctrl_status);
        imageView_sym_status = bb_keyboard.findViewById(R.id.sym_status);
        textView_inputMethodName = bb_keyboard.findViewById(R.id.textView_inputmethodname);
        imageView_setting = bb_keyboard.findViewById(R.id.imageView_setting);

        imageView_setting.setOnClickListener(new View.OnClickListener() {//设置按键监听器
            @Override
            public void onClick(View v) {
                Intent intent = con.getPackageManager().getLaunchIntentForPackage("cn.queshw.autotextinputmethod");
                if (intent != null) {
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    con.startActivity(intent);
                }
            }
        });

        setKeysListener(clickListener, longClickListener);
    }

    //设置键盘按键的监听器
    private void setKeysListener(View.OnClickListener clickListener, View.OnLongClickListener longClickListener) {
        ImageView key;
        int[] buttonKeyboard = new int[]{//能输入字符的按键和功能键
                R.id.button_q, R.id.button_w, R.id.button_e, R.id.button_r, R.id.button_t, R.id.button_y, R.id.button_u, R.id.button_o, R.id.button_i, R.id.button_p,
                R.id.button_a, R.id.button_s, R.id.button_d, R.id.button_f, R.id.button_g, R.id.button_h, R.id.button_j, R.id.button_k, R.id.button_l, R.id.button_delete,
                R.id.button_alt, R.id.button_z, R.id.button_x, R.id.button_c, R.id.button_v, R.id.button_b, R.id.button_n, R.id.button_m, R.id.button_dolla, R.id.button_enter,
                R.id.button_ctrl, R.id.button_0, R.id.button_space, R.id.button_emoji, R.id.button_cap
        };
        for (int i = 0; i < buttonKeyboard.length; i++) {
            key = bb_keyboard.findViewById(buttonKeyboard[i]);
            key.setOnClickListener(clickListener);
            key.setOnLongClickListener(longClickListener);
        }
    }

    //返回键盘的view
    public View getbbKeyboardView() {
        return bb_keyboard;
    }

    //根据keycode 和 功能键状态获得字符
    char getCharactor(int keyCode){
        return BBsoftKeyboardMap.getCharactor(keyCode, isAltOn, isCapOn);
    }

    //键盘按键部分是否可见，有物理键盘的时候就不显示按键部分
    public void set_keyonly_view_visibile(boolean hasHardKeyboard) {
        if (hasHardKeyboard) keyonly_view.setVisibility(View.GONE);
        else keyonly_view.setVisibility(View.VISIBLE);
    }

    //设置状态栏
    public void set_status_line_view(int metastate, String methodName) {
        clear_ImageViews_status();
        textView_inputMethodName.setText(methodName);
        isAltOn = false;
        isCtrlOn = false;
        isCapOn = false;
        isSymOn = false;

        //设置cap
        if ((metastate & HandleMetaKey.META_CAP_LOCKED) != 0) {// cap locked
            set_Imageviews_status(imageView_cap_status, R.drawable.status_cap_lock, R.color.myyellow, R.string.cap_mode);
            isCapOn = true;
        } else if ((metastate & KeyEvent.META_SHIFT_RIGHT_ON) != 0) {// cap on
            set_Imageviews_status(imageView_cap_status, R.drawable.status_cap, R.color.myyellow, R.string.cap_mode);
            isCapOn = true;
        }

        //设置alt
        if ((metastate & HandleMetaKey.META_ALT_LOCKED) != 0) {// alt locked
            set_Imageviews_status(imageView_alt_status, R.drawable.status_alt_lock, R.color.lightgreen, R.string.alt_mode);
            isAltOn = true;
        } else if ((metastate & KeyEvent.META_ALT_LEFT_ON) != 0) {// alt on
            set_Imageviews_status(imageView_alt_status, R.drawable.status_alt, R.color.lightgreen, R.string.alt_mode);
            isAltOn = true;
        }

        //设置sym
        if ((metastate & HandleMetaKey.META_SYM_ON) != 0) {// sym locked
            set_Imageviews_status(imageView_sym_status, R.drawable.status_sym_lock, R.color.lightGray, methodName);
            isSymOn = true;
        } else if ((metastate & HandleMetaKey.META_SYM_LOCKED) != 0) {// sym locked
            set_Imageviews_status(imageView_sym_status, R.drawable.status_sym, R.color.lightGray, methodName);
            isSymOn = true;
        }

        //设置ctrl
        if ((metastate & HandleMetaKey.META_CTRL_LOCKED) != 0) {// ctrl locked
            set_Imageviews_status(imageView_ctrl_status, R.drawable.status_ctrl_lock, R.color.lightred, R.string.ctrl_mode);
            isCapOn = true;
        } else if ((metastate & KeyEvent.META_SHIFT_LEFT_ON) != 0) {// ctrl on
            set_Imageviews_status(imageView_ctrl_status, R.drawable.status_ctrl, R.color.lightred, R.string.ctrl_mode);
            isCapOn = true;
        }
    }
    private void clear_ImageViews_status(){
        imageView_cap_status.setVisibility(View.INVISIBLE);
        imageView_cap_status.setVisibility(View.INVISIBLE);
        imageView_cap_status.setVisibility(View.INVISIBLE);
        imageView_cap_status.setVisibility(View.INVISIBLE);
    }
    private void set_Imageviews_status(ImageView button, int image, int color, int name){
        button.setImageResource(image);
        status_line_view.setBackgroundColor(con.getResources().getColor(color, null));
        textView_inputMethodName.setText(name);
        button.setVisibility(View.VISIBLE);
    }
    private void set_Imageviews_status(ImageView button, int image, int color, String name){
        button.setImageResource(image);
        status_line_view.setBackgroundColor(con.getResources().getColor(color, null));
        textView_inputMethodName.setText(name);
        button.setVisibility(View.VISIBLE);;
    }

    //设置状态栏，专为选择模式准备的
    public void set_status_line_view_for_selectmode(boolean isSelectMode, String methodName){
        if(isSelectMode){
            status_line_view.setBackgroundColor(con.getResources().getColor(R.color.lightblue, null));
            textView_inputMethodName.setText(R.string.select_mode);
        }
        else{
            status_line_view.setBackgroundColor(con.getResources().getColor(R.color.lightGray, null));
            textView_inputMethodName.setText(methodName);
        }
    }

    //设置输入法的名称
    public void setInputMethodName(String name){
        textView_inputMethodName.setText(name);
    }

    //几个状态的获取函数
    public boolean isAltOn(){
        return isAltOn;
    }
    public boolean isCtrlOn(){
        return isCtrlOn;
    }
    public boolean isCapOn(){
        return isCapOn;
    }
    public boolean isSymOn(){
        return isSymOn;
    }


}