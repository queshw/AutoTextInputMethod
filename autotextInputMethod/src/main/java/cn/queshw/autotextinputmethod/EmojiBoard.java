package cn.queshw.autotextinputmethod;

import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

public class EmojiBoard {
    View candidateView;
    ArrayList<TextView> keyboardTextViewList;
    HashMap<Integer, String> stickerStringMap;
    int[] keys;
    int emojiNumbers;
    int stickerNumbersPerPage;

    public EmojiBoard(View candidateView) {
        this.candidateView = candidateView;
        emojiNumbers = EMOJI_LIST.length;
        keyboardTextViewList = new ArrayList<TextView>();
        keyboardTextViewList.add((TextView) candidateView.findViewById(R.id.q_sticker_textview));
        keyboardTextViewList.add((TextView) candidateView.findViewById(R.id.w_sticker_textview));
        keyboardTextViewList.add((TextView) candidateView.findViewById(R.id.e_sticker_textview));
        keyboardTextViewList.add((TextView) candidateView.findViewById(R.id.r_sticker_textview));
        keyboardTextViewList.add((TextView) candidateView.findViewById(R.id.t_sticker_textview));
        keyboardTextViewList.add((TextView) candidateView.findViewById(R.id.y_sticker_textview));
        keyboardTextViewList.add((TextView) candidateView.findViewById(R.id.u_sticker_textview));
        keyboardTextViewList.add((TextView) candidateView.findViewById(R.id.i_sticker_textview));
        keyboardTextViewList.add((TextView) candidateView.findViewById(R.id.o_sticker_textview));
        keyboardTextViewList.add((TextView) candidateView.findViewById(R.id.p_sticker_textview));
        keyboardTextViewList.add((TextView) candidateView.findViewById(R.id.a_sticker_textview));
        keyboardTextViewList.add((TextView) candidateView.findViewById(R.id.s_sticker_textview));
        keyboardTextViewList.add((TextView) candidateView.findViewById(R.id.d_sticker_textview));
        keyboardTextViewList.add((TextView) candidateView.findViewById(R.id.f_sticker_textview));
        keyboardTextViewList.add((TextView) candidateView.findViewById(R.id.g_sticker_textview));
        keyboardTextViewList.add((TextView) candidateView.findViewById(R.id.h_sticker_textview));
        keyboardTextViewList.add((TextView) candidateView.findViewById(R.id.j_sticker_textview));
        keyboardTextViewList.add((TextView) candidateView.findViewById(R.id.k_sticker_textview));
        keyboardTextViewList.add((TextView) candidateView.findViewById(R.id.l_sticker_textview));
        keyboardTextViewList.add((TextView) candidateView.findViewById(R.id.z_sticker_textview));
        keyboardTextViewList.add((TextView) candidateView.findViewById(R.id.x_sticker_textview));
        keyboardTextViewList.add((TextView) candidateView.findViewById(R.id.c_sticker_textview));
        keyboardTextViewList.add((TextView) candidateView.findViewById(R.id.v_sticker_textview));
        keyboardTextViewList.add((TextView) candidateView.findViewById(R.id.b_sticker_textview));
        keyboardTextViewList.add((TextView) candidateView.findViewById(R.id.n_sticker_textview));
        keyboardTextViewList.add((TextView) candidateView.findViewById(R.id.m_sticker_textview));
        // keyboardTextViewList.add((TextView)
        // candidateView.findViewById(R.id.zero_sticker_textview));
        stickerNumbersPerPage = keyboardTextViewList.size();
        stickerStringMap = new HashMap<Integer, String>();
        keys = new int[]{KeyEvent.KEYCODE_Q, KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_E, KeyEvent.KEYCODE_R, KeyEvent.KEYCODE_T, KeyEvent.KEYCODE_Y,
                KeyEvent.KEYCODE_U, KeyEvent.KEYCODE_I, KeyEvent.KEYCODE_O, KeyEvent.KEYCODE_P, KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_S,
                KeyEvent.KEYCODE_D, KeyEvent.KEYCODE_F, KeyEvent.KEYCODE_G, KeyEvent.KEYCODE_H, KeyEvent.KEYCODE_J, KeyEvent.KEYCODE_K,
                KeyEvent.KEYCODE_L, KeyEvent.KEYCODE_Z, KeyEvent.KEYCODE_X, KeyEvent.KEYCODE_C, KeyEvent.KEYCODE_V, KeyEvent.KEYCODE_B,
                KeyEvent.KEYCODE_N, KeyEvent.KEYCODE_M
                // ,KeyEvent.KEYCODE_0
        };
    }

    // 更新键盘上的表情
    public void setStickerKeyboard(int stickerStartPosition) {
        int emojiCode;
        for (int i = stickerStartPosition; i < stickerStartPosition + stickerNumbersPerPage; i++) {
            if (i >= emojiNumbers) {
                keyboardTextViewList.get(i % stickerNumbersPerPage).setEnabled(false);
            } else {
                emojiCode = EMOJI_LIST[i];
                String temp = new String(Character.toChars(emojiCode));
                keyboardTextViewList.get(i % stickerNumbersPerPage).setText(temp);
                keyboardTextViewList.get(i % stickerNumbersPerPage).setEnabled(true);
                stickerStringMap.put(keys[i % stickerNumbersPerPage], temp);
            }
        }
    }

    // 返回总共有多少个表情
    public int getEmojiNumbers() {
        return emojiNumbers;
    }

    // 返回总一页有多少个表情
    public int getStickerNumbersPerPage() {
        return stickerNumbersPerPage;
    }

    // 返回当前表情键盘的某个键对应的表情字符
    public String getSticker(int keyCode) {
        String result = "NONE";
        if (stickerStringMap.containsKey(keyCode))
            return stickerStringMap.get(keyCode);
        return result;
    }

    public static int[] EMOJI_LIST = {
            /////////////////////表情////////////////////////////
            ///////////////////手势////////////////////
            0x270C, //胜利
            0x1F44C,//OK
            0x1F44D,//赞
            0x1F4AA,//加油
            0x1F91D, //握手
            0x1F44F,//鼓掌
            0x1F44E,//踩
            0x1F595, //竖中指
            0x1F64F,//拜托，合十
            // 双叹号 ！！  惊叹号？！
            0x203C, 0x2049,
            0x1F4AB,//眼冒金星
            //高兴
            0x263A, 0x1F600, 0x1F601, 0x1F602, 0x1F603,
            0x1F4AF,//100分
            0x1F4B0, //钱袋

            //不好意思，得意
            0x1F604, 0x1F605, 0x1F606, 0x1F607, 0x1F608, 0x1F609, 0x1F60A, 0x1F60B,
            0x1F60C, 0x1F60D, 0x1F60E, 0x1F60F,
            0x1F613, 0x1F614, 0x1F615,
            0x1F616, 0x1F910,

            //调皮
            0x1F617, 0x1F618, 0x1F619, 0x1F61A, 0x1F61B, 0x1F61C, 0x1F61D, 0x1F633, 0x1F917,
            0x1F911,//见钱眼开
            0x1F913,

            //嘲笑
            0x1F923, 0x1F610, 0x1F611, 0x1F612,

            //思考
            0x1F914,

            //不高兴，生气
            0x2639,
            0x1F61E, 0x1F61F,
            0x1F620, 0x1F621, 0x1F622, 0x1F623, 0x1F624, 0x1F915,
            0x1F47F, //恶魔气
            0x1F4A3,//炸弹
            0x1F4A5,//炸
            0x1F4A2,//青筋

            //吃惊
            0x1F625, 0x1F626, 0x1F627, 0x1F628, 0x1F629,
            0x1F62A, 0x1F62B, 0x1F62C, 0x1F62D, 0x1F62E, 0x1F62F, 0x1F630, 0x1F631, 0x1F632, 0x1F922,

            //晕
            0x1F635, 0x1F636, 0x1F637, 0x1F924, 0x1F925,
            0x1F912, //生病
            0x1F927,
            0x1F4A4,//困zzZZZ
            0x1F634,//睡觉


            0x1F926, //后悔
            0x1F937, //无奈
            0x1F56F, //蜡烛


            //危险标志，三角叹号、放射性等
            0x26A0, 0x2620, 0x2622, 0x2623, 0x26D4,


    };
}
