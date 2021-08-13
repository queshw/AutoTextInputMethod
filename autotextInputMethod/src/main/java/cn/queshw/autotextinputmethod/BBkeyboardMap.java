package cn.queshw.autotextinputmethod;

import android.view.KeyEvent;

import java.util.ArrayList;

//用于代替软键盘的输入，代替KeyCharactorMap
public class BBkeyboardMap {
    ArrayList<Integer> keycode;
    ArrayList<Character> altKeys;
    ArrayList<Character> capKeys;
    ArrayList<Character> normalKeys;
    BBkeyboardMap(){
        keycode = new ArrayList<>();
        altKeys = new ArrayList<>();
        capKeys = new ArrayList<>();
        normalKeys = new ArrayList<>();
        
        keycode.add(KeyEvent.KEYCODE_A);
        keycode.add(KeyEvent.KEYCODE_B);
        keycode.add(KeyEvent.KEYCODE_C);
        keycode.add(KeyEvent.KEYCODE_D);
        keycode.add(KeyEvent.KEYCODE_E);
        keycode.add(KeyEvent.KEYCODE_F);
        keycode.add(KeyEvent.KEYCODE_G);

        keycode.add(KeyEvent.KEYCODE_H);
        keycode.add(KeyEvent.KEYCODE_I);
        keycode.add(KeyEvent.KEYCODE_J);
        keycode.add(KeyEvent.KEYCODE_K);
        keycode.add(KeyEvent.KEYCODE_L);
        keycode.add(KeyEvent.KEYCODE_M);
        keycode.add(KeyEvent.KEYCODE_N);

        keycode.add(KeyEvent.KEYCODE_O);
        keycode.add(KeyEvent.KEYCODE_P);
        keycode.add(KeyEvent.KEYCODE_Q);
        keycode.add(KeyEvent.KEYCODE_R);
        keycode.add(KeyEvent.KEYCODE_S);
        keycode.add(KeyEvent.KEYCODE_T);
        
        keycode.add(KeyEvent.KEYCODE_U);
        keycode.add(KeyEvent.KEYCODE_V);
        keycode.add(KeyEvent.KEYCODE_W);
        keycode.add(KeyEvent.KEYCODE_X);
        keycode.add(KeyEvent.KEYCODE_Y);
        keycode.add(KeyEvent.KEYCODE_Z);
        
        keycode.add(KeyEvent.KEYCODE_0);
        keycode.add(KeyEvent.KEYCODE_LEFT_BRACKET);//自定义的 $  这个符号的值

        normalKeys.add('a');
        normalKeys.add('b');
        normalKeys.add('c');
        normalKeys.add('d');
        normalKeys.add('e');
        normalKeys.add('f');
        normalKeys.add('g');

        normalKeys.add('h');
        normalKeys.add('i');
        normalKeys.add('j');
        normalKeys.add('k');
        normalKeys.add('l');
        normalKeys.add('m');
        normalKeys.add('n');

        normalKeys.add('o');
        normalKeys.add('p');
        normalKeys.add('q');
        normalKeys.add('r');
        normalKeys.add('s');
        normalKeys.add('t');

        normalKeys.add('u');
        normalKeys.add('v');
        normalKeys.add('w');
        normalKeys.add('x');
        normalKeys.add('y');
        normalKeys.add('z');

        normalKeys.add('0');
        normalKeys.add('$');

        capKeys.add('A');
        capKeys.add('B');
        capKeys.add('C');
        capKeys.add('D');
        capKeys.add('E');
        capKeys.add('F');
        capKeys.add('G');

        capKeys.add('H');
        capKeys.add('I');
        capKeys.add('J');
        capKeys.add('K');
        capKeys.add('L');
        capKeys.add('M');
        capKeys.add('N');

        capKeys.add('O');
        capKeys.add('P');
        capKeys.add('Q');
        capKeys.add('R');
        capKeys.add('S');
        capKeys.add('T');

        capKeys.add('U');
        capKeys.add('V');
        capKeys.add('W');
        capKeys.add('X');
        capKeys.add('Y');
        capKeys.add('Z');

        capKeys.add('0');
        capKeys.add('$');



        altKeys.add('*');
        altKeys.add('!');
        altKeys.add('9');
        altKeys.add('5');
        altKeys.add('2');
        altKeys.add('6');
        altKeys.add('/');

        altKeys.add(':');
        altKeys.add('-');
        altKeys.add(';');
        altKeys.add('\'');
        altKeys.add('"');
        altKeys.add('.');
        altKeys.add(',');
        
        altKeys.add('+');
        altKeys.add('@');
        altKeys.add('#');
        altKeys.add('3');
        altKeys.add('4');
        altKeys.add('(');
        
        altKeys.add('_');
        altKeys.add('?');
        altKeys.add('1');
        altKeys.add('8');
        altKeys.add(')');
        altKeys.add('7');
        
        altKeys.add('0');
        altKeys.add('%');

    }
    //根据按键 和 功能键的状态 来获取字符
    char getCharactor(int code, boolean isAltPressed, boolean isCapPressed){
        char c = '\0';
        if(isAltPressed){
            c = altKeys.get(keycode.indexOf(code));
        }else if(isCapPressed){
            c = capKeys.get(keycode.indexOf(code));
        }else{
            c = normalKeys.get(keycode.indexOf(code));
        }
        return c;
    }
}
