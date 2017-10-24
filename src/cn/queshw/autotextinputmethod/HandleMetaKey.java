package cn.queshw.autotextinputmethod;

import android.view.KeyCharacterMap;
import android.view.KeyEvent;

public class HandleMetaKey {	
	
    public static final int META_CAP_ON = KeyEvent.META_SHIFT_RIGHT_ON;
    public static final int META_ALT_ON = KeyEvent.META_ALT_ON;
    public static final int META_SYM_ON = KeyEvent.META_SYM_ON;
    public static final int META_NEWSIM_ON = KeyEvent.META_SHIFT_LEFT_ON;//自定义的ctrl键，也就是左shift键

    public static final int META_CAP_LOCKED = 0x100;
    public static final int META_ALT_LOCKED = 0x200;
    public static final int META_SYM_LOCKED = 0x400;
    public static final int META_NEWSIM_LOCKED = 0x800;
    
    private static final long META_CAP_USED = 1L << 32;
    private static final long META_ALT_USED = 1L << 33;
    private static final long META_SYM_USED = 1L << 34;
    private static final long META_NEWSIM_USED = 1L << 35;
    
    private static final long META_CAP_PRESSED = 1L << 40;
    private static final long META_ALT_PRESSED = 1L << 41;
    private static final long META_SYM_PRESSED = 1L << 42;
    private static final long META_NEWSIM_PRESSED = 1L << 43;
    
    private static final long META_CAP_RELEASED = 1L << 48;
    private static final long META_ALT_RELEASED = 1L << 49;
    private static final long META_SYM_RELEASED = 1L << 50;
    private static final long META_NEWSIM_RELEASED = 1L << 51;

//    private static final long META_CAP_MASK = META_CAP_ON
//            | META_CAP_LOCKED | META_CAP_USED
//            | META_CAP_PRESSED | META_CAP_RELEASED;
//    private static final long META_ALT_MASK = META_ALT_ON
//            | META_ALT_LOCKED | META_ALT_USED
//            | META_ALT_PRESSED | META_ALT_RELEASED;
//    private static final long META_SYM_MASK = META_SYM_ON
//            | META_SYM_LOCKED | META_SYM_USED
//            | META_SYM_PRESSED | META_SYM_RELEASED;
//    private static final long META_NEWSIM_MASK = META_NEWSIM_ON
//            | META_NEWSIM_LOCKED | META_NEWSIM_USED
//            | META_NEWSIM_PRESSED | META_NEWSIM_RELEASED;
    
    private static final long META_ALL =  META_CAP_ON
            | META_CAP_LOCKED | META_CAP_USED
            | META_CAP_PRESSED | META_CAP_RELEASED 
            | META_ALT_ON
            | META_ALT_LOCKED | META_ALT_USED
            | META_ALT_PRESSED | META_ALT_RELEASED 
            | META_NEWSIM_ON
            | META_NEWSIM_LOCKED | META_NEWSIM_USED
            | META_NEWSIM_PRESSED | META_NEWSIM_RELEASED;
    
	private static final long META_CAP_MASK = META_ALL;
	private static final long META_ALT_MASK = META_ALL;
	private static final long META_SYM_MASK = META_ALL;
	private static final long META_NEWSIM_MASK = META_ALL;
    
    private static final int CLEAR_RETURN_VALUE = 0;
    private static final int PRESSED_RETURN_VALUE = 1;
    private static final int LOCKED_RETURN_VALUE = 2;
    
    //用于从state中获得metastate，这个metastate与当前的键盘事件中带的metastate不同，它们的综合效果者是最终的metastate。应该这样用：
    //mMetaState = event.getMetaState() | listener.getMetaState()
    public static final int getMetaState(long state) {
        int result = 0;

        if ((state & META_CAP_LOCKED) != 0) {
            result |= META_CAP_LOCKED | META_CAP_ON;
        } else if ((state & META_CAP_ON) != 0) {
            result |= META_CAP_ON;
        }

        if ((state & META_ALT_LOCKED) != 0) {
            result |= META_ALT_LOCKED | META_ALT_ON;
        } else if ((state & META_ALT_ON) != 0) {
            result |= META_ALT_ON;
        }

        if ((state & META_SYM_LOCKED) != 0) {
            result |= META_SYM_LOCKED | META_SYM_ON;
        } else if ((state & META_SYM_ON) != 0) {
            result |= META_SYM_ON;
        }
        
        if ((state & META_NEWSIM_LOCKED) != 0) {
            result |= META_NEWSIM_LOCKED | META_NEWSIM_ON;
        } else if ((state & META_NEWSIM_ON) != 0) {
            result |= META_NEWSIM_ON;
        }

        return result;
    }

    //用于判断在state中，某个键的状态。
    //注意：只能检测四种键CAP ALT SYM NEWSIM
    public static final int getMetaState(long state, int meta) {
        switch (meta) {
            case META_CAP_ON:
                if ((state & META_CAP_LOCKED) != 0) return LOCKED_RETURN_VALUE;
                if ((state & META_CAP_ON) != 0) return PRESSED_RETURN_VALUE;
                return CLEAR_RETURN_VALUE;

            case META_ALT_ON:
                if ((state & META_ALT_LOCKED) != 0) return LOCKED_RETURN_VALUE;
                if ((state & META_ALT_ON) != 0) return PRESSED_RETURN_VALUE;
                return CLEAR_RETURN_VALUE;

            case META_SYM_ON:
                if ((state & META_SYM_LOCKED) != 0) return LOCKED_RETURN_VALUE;
                if ((state & META_SYM_ON) != 0) return PRESSED_RETURN_VALUE;
                return CLEAR_RETURN_VALUE;
                
            case META_NEWSIM_ON:
                if ((state & META_NEWSIM_LOCKED) != 0) return LOCKED_RETURN_VALUE;
                if ((state & META_NEWSIM_ON) != 0) return PRESSED_RETURN_VALUE;
                return CLEAR_RETURN_VALUE;

            default:
                return CLEAR_RETURN_VALUE;
        }
    }

    //用于在键盘事件处理之后state状态的调整.它只应该在CAP ALT SYM NEWSIM之外的事件被处理后，才调用这个函数
    public static long adjustMetaAfterKeypress(long state) {
        if ((state & META_CAP_PRESSED) != 0) {
            state = (state & ~META_CAP_MASK) | META_CAP_ON | META_CAP_USED;
            //Log.d("Here", "CAP pressed to used!");
        } else if ((state & META_CAP_RELEASED) != 0) {
            state &= ~META_CAP_MASK;
           // Log.d("Here", "CAP released to clear!");
        }

        if ((state & META_ALT_PRESSED) != 0) {
            state = (state & ~META_ALT_MASK) | META_ALT_ON | META_ALT_USED;
           // Log.d("Here", "ALT pressed to used!");
        } else if ((state & META_ALT_RELEASED) != 0) {
            state &= ~META_ALT_MASK;
            //Log.d("Here", "ALT released to clear!");
        }

        if ((state & META_SYM_PRESSED) != 0) {
            state = (state & ~META_SYM_MASK) | META_SYM_ON | META_SYM_USED;
            //Log.d("Here", "SYM pressed to used!");
        } else if ((state & META_SYM_RELEASED) != 0) {
        	//Log.d("Here", "SYM released to clear!");
            state &= ~META_SYM_MASK;
        }
        
        if ((state & META_NEWSIM_PRESSED) != 0) {
            state = (state & ~META_NEWSIM_MASK) | META_NEWSIM_ON | META_NEWSIM_USED;
            //Log.d("Here", "NEWSIM pressed to used!");
        } else if ((state & META_NEWSIM_RELEASED) != 0) {
            state &= ~META_NEWSIM_MASK;
            //Log.d("Here", "NEWSIM released to clear!");
        }
        return state;
    }

    //这个是一个主要的函数，用于处理当CAP ALT SYM NEWSIM被按下后，state状态的调整 
    public static long handleKeyDown(long state, int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT) {
            return press(state, META_CAP_ON, META_CAP_MASK,
                    META_CAP_LOCKED, META_CAP_PRESSED, META_CAP_RELEASED, META_CAP_USED);
        }

        if (keyCode == KeyEvent.KEYCODE_ALT_LEFT || keyCode == KeyEvent.KEYCODE_ALT_RIGHT
                || keyCode == KeyEvent.KEYCODE_NUM) {
            return press(state, META_ALT_ON, META_ALT_MASK,
                    META_ALT_LOCKED, META_ALT_PRESSED, META_ALT_RELEASED, META_ALT_USED);
        }

        if (keyCode == KeyEvent.KEYCODE_SYM) {
            return press(state, META_SYM_ON, META_SYM_MASK,
                    META_SYM_LOCKED, META_SYM_PRESSED, META_SYM_RELEASED, META_SYM_USED);
        }
        
        
        if (keyCode == KeyEvent.KEYCODE_SHIFT_LEFT) {        	
            return press(state, META_NEWSIM_ON, META_NEWSIM_MASK,
                    META_NEWSIM_LOCKED, META_NEWSIM_PRESSED, META_NEWSIM_RELEASED, META_NEWSIM_USED);
        }
        
        return state;
    }

    private static long press(long state, int what, long mask,
            long locked, long pressed, long released, long used) {
        if ((state & pressed) != 0) {
            // repeat before use
        	//Log.d("Here", " Repeating……");
        } else if ((state & released) != 0) {
            state = (state &~ mask) | what | locked;
            //Log.d("Here", " released to locked!");
        } else if ((state & used) != 0) {
            // repeat after use
        	//Log.d("Here", " Repeating after used……!");
        } else if ((state & locked) != 0) {
        	//Log.d("Here", " locked to clear!");
            state &= ~mask;
        } else {     
        	//Log.d("Here", " clear to pressed!");
            state |= what | pressed;
        }
        return state;
    }

    //这也是一个主要函数，用于处理当CAP ALT SYM NEWSIM释放后，state状态的调整 
    public static long handleKeyUp(long state, int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT) {
            return release(state, META_CAP_ON, META_CAP_MASK,
                    META_CAP_PRESSED, META_CAP_RELEASED, META_CAP_USED, event);
        }

        if (keyCode == KeyEvent.KEYCODE_ALT_LEFT || keyCode == KeyEvent.KEYCODE_ALT_RIGHT
                || keyCode == KeyEvent.KEYCODE_NUM) {
            return release(state, META_ALT_ON, META_ALT_MASK,
                    META_ALT_PRESSED, META_ALT_RELEASED, META_ALT_USED, event);
        }

        if (keyCode == KeyEvent.KEYCODE_SYM) {
            return release(state, META_SYM_ON, META_SYM_MASK,
                    META_SYM_PRESSED, META_SYM_RELEASED, META_SYM_USED, event);
        }
        
        if (keyCode == KeyEvent.KEYCODE_SHIFT_LEFT) {
            return release(state, META_NEWSIM_ON, META_NEWSIM_MASK,
                    META_NEWSIM_PRESSED, META_NEWSIM_RELEASED, META_NEWSIM_USED, event);
        }
        
        return state;
    }

    private static long release(long state, int what, long mask,
            long pressed, long released, long used, KeyEvent event) {
        switch (event.getKeyCharacterMap().getModifierBehavior()) {
            case KeyCharacterMap.MODIFIER_BEHAVIOR_CHORDED_OR_TOGGLED:
                if ((state & used) != 0) {
                	//Log.d("Here"," used to clear!");
                    state &= ~mask;
                } else if ((state & pressed) != 0) {  
                	//Log.d("Here", " pressed to released!");
                	state &= ~mask;
                    state |= what | released;
                }
                break;

            default:
                state &= ~mask;
                break;
        }
        return state;
    }

    //清除state中的某些键的状态值
    public static long clearMetaKeyState(long state, int which) {
        if ((which & META_CAP_ON) != 0 && (state & META_CAP_LOCKED) != 0) {
            state &= ~META_CAP_MASK;
        }
        if ((which & META_ALT_ON) != 0 && (state & META_ALT_LOCKED) != 0) {
            state &= ~META_ALT_MASK;
        }
        if ((which & META_SYM_ON) != 0 && (state & META_SYM_LOCKED) != 0) {
            state &= ~META_SYM_MASK;
        }
        if ((which & META_NEWSIM_ON) != 0 && (state & META_NEWSIM_LOCKED) != 0) {
            state &= ~META_NEWSIM_MASK;
        }
        return state;
    }
}
