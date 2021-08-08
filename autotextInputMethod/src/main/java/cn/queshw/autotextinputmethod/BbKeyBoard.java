package cn.queshw.autotextinputmethod;

import android.view.View;
import android.widget.ImageView;

public class BbKeyBoard {

    
    BbKeyBoard(){};
    
    static public void  initBbKeyboard(View bb_keyboard, View.OnClickListener clickListener, View.OnLongClickListener longClickListener){
        ImageView key ;

        int[] buttonKeyboard = new int[]{//能输入字符的按键和功能键
                R.id.button_q, R.id.button_w, R.id.button_e, R.id.button_r, R.id.button_t, R.id.button_y, R.id.button_u, R.id.button_o, R.id.button_i, R.id.button_p,
                R.id.button_a, R.id.button_s, R.id.button_d, R.id.button_f, R.id.button_g, R.id.button_h, R.id.button_j, R.id.button_k, R.id.button_l, R.id.button_delete,
                R.id.button_alt, R.id.button_z, R.id.button_x, R.id.button_c, R.id.button_v, R.id.button_b, R.id.button_n, R.id.button_m, R.id.button_dolla, R.id.button_enter,
                R.id.button_ctrl, R.id.button_0, R.id.button_space, R.id.button_emoji, R.id.button_cap
        };

        for(int i = 0; i < buttonKeyboard.length; i++){
            key = bb_keyboard.findViewById(buttonKeyboard[i]);
            key.setOnClickListener(clickListener);
            key.setOnLongClickListener(longClickListener);
        }
    }
}