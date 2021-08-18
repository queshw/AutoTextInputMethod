package cn.queshw.autotextinputmethod;

import android.view.View;

public interface BbKeyBoard{

    void setKeysListener(View.OnClickListener clickListener, View.OnLongClickListener longClickListener);
    char getCharactor(int keyCode, boolean isAltOn, boolean isCapOn);
    void updateKeyboard(int metakeyStatus);
    View getBbKeyboard();
}
