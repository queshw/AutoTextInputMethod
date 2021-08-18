package cn.queshw.autotextinputmethod;

import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class BbKeyBoardView {
    public static int ALT_ON = 1;
    public static int CAP_ON = 2;
    public static int SYM_ON = 4;
    public static int CTRL_ON = 8;
    private int myMetakeyStatus = 0;//标记键盘功能键的状态。在metastate中已经有对应的标记，再设立这个是为了方便
    //在metastate中， 一个键的两种状态on 和 locked状态都是这个键启用的状态，设立myMetakeyStatus，就相当于把这两种状态归并管理了
    private int myMetastate = -1;//初始状态不能设置为0，要不最初始传过来的就是0，那么一开始就不会更新了

    Context con;
    // 用于软健盘
    private View bb_keyboard_view;//整个软键盘，包括按键和状态栏
    private View status_line_view;//键盘的状态栏部分
    private LinearLayout keyonly_view;//包含键盘的线性布局类
    private BbKeyBoard bbKeyBoard = null;//键盘

    //状态栏的几个键
    private ImageView imageView_cap_status;
    private ImageView imageView_alt_status;
    private ImageView imageView_ctrl_status;
    private ImageView imageView_sym_status;
    private TextView textView_inputMethodName;//状态栏上的当前使用的输入词库名称
    private ImageView imageView_setting;//状态栏上的设置按纽，直接进入词库设置界面

    BbKeyBoardView(Context con, BbKeyBoard bb, View.OnClickListener clickListener, View.OnLongClickListener longClickListener) {
        this.con = con;

        bb_keyboard_view = View.inflate(con, R.layout.bbkeyboardview_layout, null);
        keyonly_view = bb_keyboard_view.findViewById(R.id.bb_keyboard_keyonly);

        status_line_view = bb_keyboard_view.findViewById(R.id.status_line_view);
        imageView_cap_status = bb_keyboard_view.findViewById(R.id.cap_status);
        imageView_alt_status = bb_keyboard_view.findViewById(R.id.alt_status);
        imageView_ctrl_status = bb_keyboard_view.findViewById(R.id.ctrl_status);
        imageView_sym_status = bb_keyboard_view.findViewById(R.id.sym_status);
        textView_inputMethodName = bb_keyboard_view.findViewById(R.id.textView_inputmethodname);
        imageView_setting = bb_keyboard_view.findViewById(R.id.imageView_setting);

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

        setKeyboard(bb);
        setKeysListener(clickListener, longClickListener);
    }
    //设置这个BbkeyboardView 使用哪个键盘
    public void setKeyboard(BbKeyBoard bbKeyBoard){
        this.bbKeyBoard = bbKeyBoard;
        keyonly_view.removeView(bbKeyBoard.getBbKeyboard());
        keyonly_view.addView(bbKeyBoard.getBbKeyboard());
    }
    //设置键盘按键的监听器
    private void setKeysListener(View.OnClickListener clickListener, View.OnLongClickListener longClickListener) {
        bbKeyBoard.setKeysListener(clickListener, longClickListener);
    }

    //返回键盘的view
    public View getbbKeyboardView() {
        return bb_keyboard_view;
    }

    //根据keycode 和 功能键状态获得字符
    public char getCharactor(int keyCode, boolean isAlton, boolean isCapOn){
        return bbKeyBoard.getCharactor(keyCode, isAlton, isCapOn);
    }

    //键盘按键部分是否可见，有物理键盘的时候就不显示按键部分
    public void set_keyonly_view_visibile(boolean hasHardKeyboard) {
        if (hasHardKeyboard) keyonly_view.setVisibility(View.GONE);
        else keyonly_view.setVisibility(View.VISIBLE);
    }

    //设置状态栏
    public void set_status_line_view(int metastate, String methodName) {
        //如果这次传过来的metastate与上一次没有变化，则什么都不需要改变
        if(myMetastate == metastate) return;
        else myMetastate = metastate;

        //如果这一次的metastate有变化，则进行下面的处理，更新状态栏和键盘的样式，也更新功能键标记
        clear_ImageViews_status();
        myMetakeyStatus = 0;
        textView_inputMethodName.setText(methodName);

        //设置cap
        if ((metastate & HandleMetaKey.META_CAP_LOCKED) != 0) {// cap locked
            set_Imageviews_status(imageView_cap_status, R.drawable.status_cap_lock, R.color.myyellow, R.string.cap_mode);
            myMetakeyStatus |= CAP_ON;
        } else if ((metastate & KeyEvent.META_SHIFT_RIGHT_ON) != 0) {// cap on
            set_Imageviews_status(imageView_cap_status, R.drawable.status_cap, R.color.myyellow, R.string.cap_mode);
            myMetakeyStatus |= CAP_ON;
        }

        //设置alt
        if ((metastate & HandleMetaKey.META_ALT_LOCKED) != 0) {// alt locked
            set_Imageviews_status(imageView_alt_status, R.drawable.status_alt_lock, R.color.lightgreen, R.string.alt_mode);
            myMetakeyStatus |= ALT_ON;
        } else if ((metastate & KeyEvent.META_ALT_LEFT_ON) != 0) {// alt on
            set_Imageviews_status(imageView_alt_status, R.drawable.status_alt, R.color.lightgreen, R.string.alt_mode);
            myMetakeyStatus |= ALT_ON;
        }

        //设置sym
        if ((metastate & HandleMetaKey.META_SYM_ON) != 0) {// sym locked
            set_Imageviews_status(imageView_sym_status, R.drawable.status_sym_lock, R.color.lightGray, methodName);
            myMetakeyStatus |= SYM_ON;
        } else if ((metastate & HandleMetaKey.META_SYM_LOCKED) != 0) {// sym locked
            set_Imageviews_status(imageView_sym_status, R.drawable.status_sym, R.color.lightGray, methodName);
            myMetakeyStatus |= SYM_ON;
        }

        //设置ctrl
        if ((metastate & HandleMetaKey.META_CTRL_LOCKED) != 0) {// ctrl locked
            set_Imageviews_status(imageView_ctrl_status, R.drawable.status_ctrl_lock, R.color.lightred, R.string.ctrl_mode);
            myMetakeyStatus |= CTRL_ON;
        } else if ((metastate & KeyEvent.META_SHIFT_LEFT_ON) != 0) {// ctrl on
            set_Imageviews_status(imageView_ctrl_status, R.drawable.status_ctrl, R.color.lightred, R.string.ctrl_mode);
            myMetakeyStatus |= CTRL_ON;
        }
        bbKeyBoard.updateKeyboard(myMetakeyStatus);
    }
    private void clear_ImageViews_status(){
        if(imageView_cap_status.getVisibility() != View.INVISIBLE) imageView_cap_status.setVisibility(View.INVISIBLE);
        if(imageView_ctrl_status.getVisibility() != View.INVISIBLE) imageView_ctrl_status.setVisibility(View.INVISIBLE);
        if(imageView_sym_status.getVisibility() != View.INVISIBLE) imageView_sym_status.setVisibility(View.INVISIBLE);
        if(imageView_alt_status.getVisibility() != View.INVISIBLE) imageView_alt_status.setVisibility(View.INVISIBLE);
        status_line_view.setBackgroundColor(con.getResources().getColor(R.color.lightGray, null));
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
        button.setVisibility(View.VISIBLE);
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
        if((myMetakeyStatus & ALT_ON) !=0 ) return true;
        else return false;
    }
    public boolean isCtrlOn(){
        if((myMetakeyStatus & CTRL_ON) !=0 ) return true;
        else return false;
    }
    public boolean isCapOn(){
        if((myMetakeyStatus & CAP_ON) !=0 ) return true;
        else return false;
    }
    public boolean isSymOn(){
        if((myMetakeyStatus & SYM_ON) !=0 ) return true;
        else return false;
    }
}