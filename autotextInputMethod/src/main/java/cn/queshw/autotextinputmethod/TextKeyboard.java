package cn.queshw.autotextinputmethod;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class TextKeyboard implements BbKeyBoard{
    int myMetakeyStatus = 0;//用于记录键盘功能键的状态

    BBkeyboardMap kMap;//用于对应软键盘
    View textKeyboard;//键盘的View实例
    ArrayList<RelativeLayout> wholeKeys;//按键的view控件，是指整个按键
    ArrayList<TextView> centerViews, upViews, downViews;
    int [] wholeKeysId, centerGroupIds, upGroupIds, downGroupIds;
    String[] centerGroupText, upGroupText, downGroupText;


    public TextKeyboard(Context context) {
        wholeKeysId = new int[]{
                R.id.q_key_textview,R.id.w_key_textview,R.id.e_key_textview, R.id.r_key_textview, R.id.t_key_textview, 
                R.id.y_key_textview,R.id.u_key_textview, R.id.i_key_textview, R.id.o_key_textview, R.id.p_key_textview, 
                R.id.a_key_textview, R.id.s_key_textview, R.id.d_key_textview, R.id.f_key_textview, R.id.g_key_textview, 
                R.id.h_key_textview, R.id.j_key_textview, R.id.k_key_textview, R.id.l_key_textview, R.id.backspace_key_textview, 
                R.id.alt_key_textview, R.id.z_key_textview, R.id.x_key_textview, R.id.c_key_textview, R.id.v_key_textview, 
                R.id.b_key_textview, R.id.n_key_textview, R.id.m_key_textview, R.id.dolla_key_textview, R.id.enter_key_textview, 
                R.id.tabmidleft_key_textview, R.id.ctrl_key_textview, R.id.zero_key_textview, R.id.space_key_textview, 
                R.id.sym_key_textview, R.id.shift_key_textview, R.id.tabmidright_key_textview, R.id.tabbottomleft_key_textview, 
                R.id.tabunderctrl_key_textview, R.id.tabunderzero_key_textview, R.id.tabundersym_key_textview, R.id.tabundershift_key_textview, 
                R.id.tabbottomright_key_textview};
        centerGroupIds = new int[]{
                R.id.q_text_textview, R.id.w_text_textview, R.id.e_text_textview, R.id.r_text_textview, R.id.t_text_textview ,
                R.id.y_text_textview , R.id.u_text_textview , R.id.i_text_textview , R.id.o_text_textview , R.id.p_text_textview ,
                R.id.a_text_textview , R.id.s_text_textview , R.id.d_text_textview , R.id.f_text_textview , R.id.g_text_textview ,
                R.id.h_text_textview , R.id.j_text_textview , R.id.k_text_textview , R.id.l_text_textview , R.id.backspace_text_textview ,
                R.id.alt_text_textview , R.id.z_text_textview , R.id.x_text_textview , R.id.c_text_textview , R.id.v_text_textview ,
                R.id.b_text_textview , R.id.n_text_textview , R.id.m_text_textview , R.id.dolla_text_textview , R.id.enter_text_textview ,
                R.id.tabmidleft_text_textview , R.id.ctrl_text_textview , R.id.zero_text_textview , R.id.space_text_textview , R.id.sym_text_textview ,
                R.id.shift_text_textview , R.id.tabmidright_text_textview , R.id.tabbottomleft_text_textview , R.id.tabunderctrl_text_textview ,
                R.id.tabunderzero_text_textview , R.id.tabundersym_text_textview , R.id.tabundershift_text_textview , R.id.tabbottomright_text_textview };
        upGroupIds = new int[]{
                R.id.q_up_textview, R.id.w_up_textview, R.id.e_up_textview, R.id.r_up_textview, R.id.t_up_textview,
                R.id.y_up_textview, R.id.u_up_textview, R.id.i_up_textview, R.id.o_up_textview, R.id.p_up_textview,
                R.id.a_up_textview, R.id.s_up_textview, R.id.d_up_textview, R.id.f_up_textview, R.id.g_up_textview,
                R.id.h_up_textview, R.id.j_up_textview, R.id.k_up_textview, R.id.l_up_textview, R.id.backspace_up_textview,
                R.id.alt_up_textview, R.id.z_up_textview, R.id.x_up_textview, R.id.c_up_textview, R.id.v_up_textview,
                R.id.b_up_textview, R.id.n_up_textview, R.id.m_up_textview, R.id.dolla_up_textview, R.id.enter_up_textview,
                R.id.tabmidleft_up_textview, R.id.ctrl_up_textview, R.id.zero_up_textview, R.id.space_up_textview,
                R.id.sym_up_textview, R.id.shift_up_textview, R.id.tabmidright_up_textview, R.id.tabbottomleft_up_textview,
                R.id.tabunderctrl_up_textview, R.id.tabunderzero_up_textview, R.id.tabundersym_up_textview, R.id.tabundershift_up_textview,
                R.id.tabbottomright_up_textview};
        downGroupIds = new int[]{
                R.id.q_down_textview, R.id.w_down_textview, R.id.e_down_textview, R.id.r_down_textview, R.id.t_down_textview,
                R.id.y_down_textview, R.id.u_down_textview, R.id.i_down_textview, R.id.o_down_textview, R.id.p_down_textview,
                R.id.a_down_textview, R.id.s_down_textview, R.id.d_down_textview, R.id.f_down_textview, R.id.g_down_textview,
                R.id.h_down_textview, R.id.j_down_textview, R.id.k_down_textview, R.id.l_down_textview, R.id.backspace_down_textview,
                R.id.alt_down_textview, R.id.z_down_textview, R.id.x_down_textview, R.id.c_down_textview, R.id.v_down_textview,
                R.id.b_down_textview, R.id.n_down_textview, R.id.m_down_textview, R.id.dolla_down_textview, R.id.enter_down_textview,
                R.id.tabmidleft_down_textview, R.id.ctrl_down_textview, R.id.zero_down_textview, R.id.space_down_textview,
                R.id.sym_down_textview, R.id.shift_down_textview, R.id.tabmidright_down_textview, R.id.tabbottomleft_down_textview,
                R.id.tabunderctrl_down_textview, R.id.tabunderzero_down_textview, R.id.tabundersym_down_textview, R.id.tabundershift_down_textview,
                R.id.tabbottomright_down_textview};
        
        centerGroupText = new String[]{
                "Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P",
                "A", "S", "D", "F", "G", "H", "J", "K", "L", "←", "" +
                "alt", "Z", "X", "C", "V", "B", "N", "M", "$", "⏎",
                "", "ctrl", "0", "space", "☺", "⇪aA", "",
                "", "", "", "", "", ""};
        upGroupText = new String[]{
                "#", "1", "2", "3", "(", ")", "_", "-", "+", "@",
                "*", "4", "5", "6", "/", ":", ";", "'", "\"", "←",
                "alt", "7", "8", "9", "?", "!", ",",".", "~", "⏎",
                "","ctrl", "0", "space", "☺", "⇪aA","",
                "", "", "", "", "", ""};
        downGroupText = new String[]{
                "《", "》", "`", "^", "（", "）", "——", "……", "、", "·",
                "<", ">", "“", "‘", "\\", "：", "；", "’", "”", "←",
                "alt", "[", "]", "&", "？", "！", "，", "。", "%", "⏎",
                "", "ctrl", "0", "space", "☺", "⇪aA", "",
                "", "", "", "", "", ""};
    

        textKeyboard = View.inflate(context, R.layout.textkeyboard_layout, null);
        kMap = new BBkeyboardMap();//初始化软键盘和字符映射表
        wholeKeys = new ArrayList<>();//按键的view控件，是指整个按键
        centerViews = new ArrayList<>();
        upViews = new ArrayList<>();
        downViews = new ArrayList<>();

        //构造按键和textview的hashmap
        for(int i : wholeKeysId){
            wholeKeys.add(textKeyboard.findViewById(i));
        }
        constructViewsArrayList();

        initKeyboardText();
    }

    //找出对应id的view控件，放到ArrayList里
    private void constructViewsArrayList(){
        for(int i : centerGroupIds) centerViews.add(textKeyboard.findViewById(i));
        for(int i : upGroupIds) upViews.add(textKeyboard.findViewById(i));
        for(int i : downGroupIds) downViews.add(textKeyboard.findViewById(i));
    }

    //初始化键盘的设定
    private void initKeyboardText(){
        //初始化各控件上的文本
        for(int i = 0; i < centerGroupIds.length; i++) {
            centerViews.get(i).setText(centerGroupText[i]);
            centerViews.get(i).setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
            upViews.get(i).setText(upGroupText[i]);
            upViews.get(i).setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
            clearGroupText(downViews);
        }
    }

    //清空某组的文本
    private void clearGroupText(ArrayList<TextView> whickGroup){
        for(TextView v : whickGroup) v.setText("");
    }

    //设置某组控件显示的文本
    private void setGroupText(ArrayList<TextView> whickGroup, String[] whickText){
        for(int i = 0; i < whickGroup.size(); i++){
            whickGroup.get(i).setText(whickText[i]);
            whickGroup.get(i).setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        }
    }

    @Override
    public void setKeysListener(View.OnClickListener clickListener, View.OnLongClickListener longClickListener) {
        for(RelativeLayout re : wholeKeys){
            re.setOnClickListener(clickListener);
            re.setOnLongClickListener(longClickListener);
        }
    }

    @Override
    public char getCharactor(int keyCode, boolean isAltOn, boolean isCapOn) {
        return kMap.getCharactor(keyCode, isAltOn, isCapOn);
    }

    @Override
    public void updateKeyboard(int metakeyStatus) {
        /*if(myMetakeyStatus != metakeyStatus){
            myMetakeyStatus = metakeyStatus;
            if((metakeyStatus & BbKeyBoardView.ALT_ON) != 0){
                clearGroupText(downViews);
                setGroupText(centerViews, upGroupText);
                setGroupText(upViews, centerGroupText);
            }else{
                initKeyboardText();
            }
        }*/
    }

    public View getBbKeyboard(){
        return textKeyboard;
    }
}
