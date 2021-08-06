package cn.queshw.autotextinputmethod;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class BbKeyBoard {

    
    BbKeyBoard(){};
    
    static public void  initBbKeyboard(View bb_keyboard, int keyWidth, View.OnClickListener clickListener, View.OnLongClickListener longClickListener){
        ImageView key ;
        TextView mothldName = bb_keyboard.findViewById(R.id.textView_inputmethod);
        mothldName.setHeight(keyWidth);

        int[] buttonNames = new int[]{
                R.id.button_topleft, R.id.ImageView_status, R.id.ImageView_setting, R.id.button_topright,
                R.id.button_q, R.id.button_w, R.id.button_e, R.id.button_r, R.id.button_t, R.id.button_y, R.id.button_u, R.id.button_o, R.id.button_i, R.id.button_p,
                R.id.button_a, R.id.button_s, R.id.button_d, R.id.button_f, R.id.button_g, R.id.button_h, R.id.button_j, R.id.button_k, R.id.button_l, R.id.button_delete,
                R.id.button_alt, R.id.button_z, R.id.button_x, R.id.button_c, R.id.button_v, R.id.button_b, R.id.button_n, R.id.button_m, R.id.button_dolla, R.id.button_enter,
                R.id.button_midleft, R.id.button_ctrl, R.id.button_0, R.id.button_space, R.id.button_emoji, R.id.button_cap, R.id.button_midright,
                R.id.button_bottom
        };
        for(int i = 0; i < buttonNames.length; i++){
            key = bb_keyboard.findViewById(buttonNames[i]);
            key.setOnClickListener(clickListener);
            key.setOnLongClickListener(longClickListener);
            key.setMaxHeight(keyWidth);
        }
    }
}