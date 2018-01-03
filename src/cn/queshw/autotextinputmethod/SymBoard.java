package cn.queshw.autotextinputmethod;

import java.util.ArrayList;
import java.util.HashMap;

import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

public class SymBoard {
	View candidateView;
	ArrayList<TextView> keyboardTextViewList;
	HashMap<Integer, String> stickerStringMap;
	int[] keys;
	int emojiNumbers;
	int stickerNumbersPerPage;

	public SymBoard(View candidateView) {
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
		keys = new int[] { KeyEvent.KEYCODE_Q, KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_E, KeyEvent.KEYCODE_R, KeyEvent.KEYCODE_T, KeyEvent.KEYCODE_Y,
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
			// 表情
			0x1F641, 0x1F642, 0x1F600, 0x1F601, 0x1F602, 0x1F605, 0x1F607, 0x1F608, 0x1F47F, 0x1F60A, 0x1F60B, 0x1F60C, 0x1F60D, 0x1F60E, 0x1F61A, 0x1F61B,
			0x1F61C, 0x1F621, 0x1F624, 0x1F628, 0x1F62A, 0x1F62C, 0x1F62D, 0x1F630, 0x1F631, 0x1F634, 0x1F637, 0x1F911, 0x1F914,
			0x1F915, 0x1F917, 0x1F44C, 0x1F44D, 0x1F44E, 0x1F44F, 0x1F595,
			0x1F4AA,
			0x1f64f,
			0x1F48B,
			0x26A1,
			0x1F494,
			0x1F498,
			0x1F4A3,
			0x1F4A4,
			0x1F3F3,			
			0x1F4AF,
			0x1F4B0,
			0x1F6AC,
			0x1F385,0x1F56F,

			// 动物
			0x1F43C, 0x1F400, 0x1F401, 0x1F402, 0x1F403, 0x1F404, 0x1F405, 0x1F406, 0x1F407, 0x1F408, 0x1F409, 0x1F40A, 0x1F40B, 0x1F40C, 0x1F40D,
			0x1F40E, 0x1F40F, 0x1F410, 0x1F411, 0x1F412, 0x1F413, 0x1F414, 0x1F415, 0x1F416, 0x1F417, 0x1F418, 0x1F419, 0x1F41A, 0x1F41B, 0x1F41C,
			0x1F41D, 0x1F41E, 0x1F41F, 0x1F420, 0x1F421, 0x1F422, 0x1F423, 0x1F424, 0x1F425, 0x1F426, 0x1F427, 0x1F428, 0x1F429, 0x1F42A, 0x1F42B,
			0x1F42C, 0x1F42D, 0x1F42E, 0x1F42F, 0x1F430, 0x1F431, 0x1F432, 0x1F433, 0x1F434, 0x1F435, 0x1F436, 0x1F437, 0x1F438, 0x1F439, 0x1F43A,
			0x1F43B, 0x1F980, 0x1F981, 0x1F982, 0x1F983, 0x1F984, 0x1F985, 0x1F986, 0x1F987,
			0x1F988,
			0x1F989,
			0x1F98A,
			0x1F98B,
			0x1F98C,
			0x1F98D,
			0x1F98E,
			0x1F98F,
			0x1F990,
			0x1F991,

			// 食物
			0x1F32D, 0x1F32E, 0x1F32F, 0x1F330, 0x1F331, 0x1F332, 0x1F333, 0x1F334, 0x1F335, 0x1F336, 0x1F337, 0x1F338, 0x1F339, 0x1F33A, 0x1F33B,
			0x1F33C, 0x1F33D, 0x1F33E, 0x1F33F, 0x1F340, 0x1F341, 0x1F342, 0x1F343, 0x1F344, 0x1F345, 0x1F346, 0x1F347, 0x1F348, 0x1F349, 0x1F34A,
			0x1F34B, 0x1F34C, 0x1F34D, 0x1F34E, 0x1F34F, 0x1F350, 0x1F351, 0x1F352, 0x1F353, 0x1F354, 0x1F355, 0x1F356, 0x1F357, 0x1F358, 0x1F359,
			0x1F35A, 0x1F35B, 0x1F35C, 0x1F35D, 0x1F35E, 0x1F35F, 0x1F360, 0x1F361, 0x1F362, 0x1F363, 0x1F364, 0x1F365, 0x1F366, 0x1F367, 0x1F368,
			0x1F369, 0x1F36A, 0x1F36B, 0x1F36C, 0x1F36D, 0x1F36E, 0x1F36F, 0x1F370, 0x1F371, 0x1F372, 0x1F373, 0x1F374, 0x1F375, 0x1F376, 0x1F377,
			0x1F378, 0x1F379, 0x1F37A, 0x1F37B, 0x1F37C, 0x1F37D, 0x1F37E, 0x1F37F, 0x1F380, 0x1F381, 0x1F382, 0x1F383, 0x1F384, 0x1F385, 0x1F386,
			0x1F387, 0x1F388, 0x1F389, 0x1F38A, 0x1F950, 0x1F951, 0x1F952, 0x1F953, 0x1F954, 0x1F955, 0x1F956, 0x1F957, 0x1F958, 0x1F959, 0x1F95A,
			0x1F95B, 0x1F95C,
			0x1F95D,
			0x1F95E,

			// 交通工具
			0x1F680, 0x1F681, 0x1F682, 0x1F683, 0x1F684, 0x1F685, 0x1F686, 0x1F687, 0x1F688, 0x1F689, 0x1F68A, 0x1F68B, 0x1F68C, 0x1F68D, 0x1F68E,
			0x1F68F, 0x1F690, 0x1F691, 0x1F692, 0x1F693, 0x1F694, 0x1F695, 0x1F696, 0x1F697, 0x1F698, 0x1F699, 0x1F69A, 0x1F69B, 0x1F69C, 0x1F69D,
			0x1F69E, 0x1F69F, 0x1F6A0, 0x1F6A1, 0x1F6A2, 0x1F6A3, 0x1F6A4, 0x1F6E5, 0x1F6E9, 0x1F6EB, 0x1F6EC, 0x1F6F0, 0x1F6F3, 0x1F6F4, 0x1F6F5,
			0x1F6F6 };
}
