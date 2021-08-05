package cn.queshw.autotextinputmethod;

import android.view.KeyEvent;
//这个类用于实现键盘上的功能键的功能，主要思路是用一个长整型来记录这些功能键的状态，比如是否被按下，是否锁定等等。而在实际键盘产生字符的过程中，使用的是一个整型的状态标记
//也就是说长整型的后四个字节放的就是正常的状态标记，前四个字节就是我们用来实现额外功能的或者记录之前按键状态的地方
//在一个程序中，应该先定义一个长整型来记录键盘功能键的状态，然后在按键的过程中用本类中的方法来调整功能键的标记状态。然后在程序中再使用这些标记来实现对应的功能。
//这个类中跟踪四个功能键的状态，即alt、左shift、右shift、sym。它们变化的过程是这样的
//1、首先是clear的状态，如果按下后不放，变为pressed状态
//3、松开键，如果之前是pressed状态则变成了released状态，如果之前是used状态则清空，对应的键的标记位是on状态。
//4、如果此时再按该功能键，如果之前是released状态，则变成locked状态。如果之前是locked状态，再按该键，则清空状态
//按一个其他的非功能键，如果之前是pressed则变成used状态。如果之前是released，那就清空
//pressed和used状态都表示一直按着功能键没有松开呢。used表示的是在一直按着功能键的过程中，按了其他的非功能键。
//sym键有特殊性，它没有locked状态，而且需要一个标记位来表示表情小键盘要翻页

public class HandleMetaKey {

    public static final int META_CAP_ON = KeyEvent.META_SHIFT_RIGHT_ON;
    public static final int META_ALT_ON = KeyEvent.META_ALT_LEFT_ON;
    public static final int META_SYM_ON = KeyEvent.META_SYM_ON;
    public static final int META_NEWSIM_ON = KeyEvent.META_SHIFT_LEFT_ON;// 自定义的ctrl键，也就是左shift键

    public static final int META_CAP_LOCKED = 0x100;
    public static final int META_ALT_LOCKED = 0x200;
    public static final int META_SYM_LOCKED = 0x400;
    public static final int META_NEWSIM_LOCKED = 0x800;

    private static final long META_CAP_USED = 1L << 32;
    private static final long META_ALT_USED = 1L << 33;
    private static final long META_SYM_USED = 1L << 34;
    private static final long META_NEWSIM_USED = 1L << 35;

    private static final long META_CAP_LOCK_RELEASED = 1L << 36;
    private static final long META_ALT_LOCK_RELEASED = 1L << 37;
    private static final long META_SYM_LOCK_RELEASED = 1L << 38;
    private static final long META_NEWSIM_LOCK_RELEASED = 1L << 39;

    private static final long META_CAP_PRESSED = 1L << 40;
    private static final long META_ALT_PRESSED = 1L << 41;
    private static final long META_SYM_PRESSED = 1L << 42;
    private static final long META_NEWSIM_PRESSED = 1L << 43;

    private static final long META_CAP_RELEASED = 1L << 48;
    private static final long META_ALT_RELEASED = 1L << 49;
    public static final long META_SYM_RELEASED = 1L << 50;
    private static final long META_NEWSIM_RELEASED = 1L << 51;

    public static final long META_SYM_TURNPAGE = 1L << 52;// 用于标记表情小键盘要翻页，在翻页后清空


    private static final long META_ALL = META_CAP_ON | META_CAP_LOCKED | META_CAP_USED | META_CAP_PRESSED | META_CAP_RELEASED |
            META_ALT_ON | META_ALT_LOCKED | META_ALT_USED | META_ALT_PRESSED | META_ALT_RELEASED |
            META_SYM_ON | META_SYM_LOCKED | META_SYM_USED | META_SYM_PRESSED | META_SYM_RELEASED |
            META_NEWSIM_ON | META_NEWSIM_LOCKED | META_NEWSIM_USED | META_NEWSIM_PRESSED | META_NEWSIM_RELEASED |
            META_SYM_TURNPAGE | META_CAP_LOCK_RELEASED | META_ALT_LOCK_RELEASED | META_SYM_LOCK_RELEASED | META_NEWSIM_LOCK_RELEASED;

    private static final long META_CAP_MASK = META_ALL;
    private static final long META_ALT_MASK = META_ALL;
    private static final long META_SYM_MASK = META_ALL;
    private static final long META_NEWSIM_MASK = META_ALL;

//	private static final int CLEAR_RETURN_VALUE = 0;
//	private static final int PRESSED_RETURN_VALUE = 1;
//	private static final int LOCKED_RETURN_VALUE = 2;

    // 用于从state中获得metastate，这个metastate与当前的键盘事件中带的metastate不同，它们的综合效果者是最终的metastate。应该这样用：
    // mMetaState = event.getMetaState() | listener.getMetaState()
    // 从long state中获得功能键的状态，只检测四个键哦
    public static final int getMetaState(long state) {
        int result = 0;

        if ((state & (META_CAP_LOCKED | META_CAP_LOCK_RELEASED)) != 0) {
            result |= META_CAP_LOCKED | META_CAP_ON;
        } else if ((state & META_CAP_ON) != 0) {
            result |= META_CAP_ON;
        }

        if ((state & (META_ALT_LOCKED | META_ALT_LOCK_RELEASED)) != 0) {
            result |= META_ALT_LOCKED | META_ALT_ON;
        } else if ((state & META_ALT_ON) != 0) {
            result |= META_ALT_ON;
        }

        if ((state & (META_SYM_LOCKED | META_SYM_LOCK_RELEASED)) != 0) {
            result |= META_SYM_LOCKED | META_SYM_ON;
        } else if ((state & META_SYM_ON) != 0) {
            result |= META_SYM_ON;
        }

        if ((state & (META_NEWSIM_LOCKED | META_NEWSIM_LOCK_RELEASED)) != 0) {
            result |= META_NEWSIM_LOCKED | META_NEWSIM_ON;
        } else if ((state & META_NEWSIM_ON) != 0) {
            result |= META_NEWSIM_ON;
        }

        if ((state & META_SYM_TURNPAGE) != 0) {//用于获取表情键盘翻页的状态
            result |= META_SYM_TURNPAGE;
        }

        return result;
    }

//	// 用于判断在state中，某个键的状态。是锁定状态，还是已经按下的状态，还是已经清空了
//	// 注意：只能检测四种键CAP ALT SYM NEWSIM
//	public static final int getMetaState(long state, int meta) {
//		switch (meta) {
//		case META_CAP_ON:
//			if ((state & META_CAP_LOCKED) != 0)
//				return LOCKED_RETURN_VALUE;
//			if ((state & META_CAP_ON) != 0)
//				return PRESSED_RETURN_VALUE;
//			return CLEAR_RETURN_VALUE;
//
//		case META_ALT_ON:
//			if ((state & META_ALT_LOCKED) != 0)
//				return LOCKED_RETURN_VALUE;
//			if ((state & META_ALT_ON) != 0)
//				return PRESSED_RETURN_VALUE;
//			return CLEAR_RETURN_VALUE;
//
//		case META_SYM_ON:
//			if ((state & META_SYM_LOCKED) != 0)
//				return LOCKED_RETURN_VALUE;
//			if ((state & META_SYM_ON) != 0)
//				return PRESSED_RETURN_VALUE;
//			return CLEAR_RETURN_VALUE;
//
//		case META_NEWSIM_ON:
//			if ((state & META_NEWSIM_LOCKED) != 0)
//				return LOCKED_RETURN_VALUE;
//			if ((state & META_NEWSIM_ON) != 0)
//				return PRESSED_RETURN_VALUE;
//			return CLEAR_RETURN_VALUE;
//
//		default:
//			return CLEAR_RETURN_VALUE;
//		}
//	}

    // 这个是一个主要的函数，用于处理当CAP ALT SYM NEWSIM被按下后，state状态的调整
    public static long handleKeyDown(long state, int keyCode, KeyEvent event) {
        // 如果当前的键盘事件是右shift被按下
        if (keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT) {
            return press(state, META_CAP_ON, META_CAP_MASK, META_CAP_LOCKED, META_CAP_PRESSED, META_CAP_RELEASED, META_CAP_LOCK_RELEASED, META_CAP_USED);
        }
        // 如果当前的键盘事件是alt被按下
        if (keyCode == KeyEvent.KEYCODE_ALT_LEFT || keyCode == KeyEvent.KEYCODE_ALT_RIGHT || keyCode == KeyEvent.KEYCODE_NUM) {
            return press(state, META_ALT_ON, META_ALT_MASK, META_ALT_LOCKED, META_ALT_PRESSED, META_ALT_RELEASED, META_ALT_LOCK_RELEASED, META_ALT_USED);
        }
        // 如果当前的键盘事件是sym被按下
        if (keyCode == KeyEvent.KEYCODE_SYM) {
            return press(state, META_SYM_ON, META_SYM_MASK, META_SYM_LOCKED, META_SYM_PRESSED, META_SYM_RELEASED, META_SYM_LOCK_RELEASED, META_SYM_USED);
        }
        // 如果当前的键盘事件是左shift被按下，当作ctrl
        if (keyCode == KeyEvent.KEYCODE_SHIFT_LEFT) {
            return press(state, META_NEWSIM_ON, META_NEWSIM_MASK, META_NEWSIM_LOCKED, META_NEWSIM_PRESSED, META_NEWSIM_RELEASED, META_NEWSIM_LOCK_RELEASED, META_NEWSIM_USED);
        }

        return state;
    }

    // 真正调整功能键被按下后的状态的是这个函数
    // state 当前功能键的状态
    // what 当前被按下的功能键是哪个
    // mask locked pressed released used 这些都是功能键对应的状态的时候的标记位
    // 在pressed 和used 状态的时候，同时对应的on状态肯定是已经标记的
    private static long press(long state, int what, long mask, long locked, long pressed, long released, long lock_released, long used) {
        if ((state & pressed) != 0) {
            // 表示之前已经标记这个功能键被按下了，也就是说现在只是重复事件，也就是操作者按着这个功能键不放。
            // 不需要操作，仍然是标记这个功能键被按下了，即pressed
            // repeat before use
            //Log.d("Here", "pressed Repeating……");
        } else if ((state & released) != 0) {
            // 如果之前的标记此功能键被按下之后又释放了，现在又按下，说明操作者是按一下之后又按了一下，那就是锁定了
            // 同时清空其他功能键的状态
            if (what == META_SYM_ON) {//sym键没有锁定状态，只标记要表情键盘要翻页
                //state = (state & ~mask) | what | pressed | META_SYM_TURNPAGE;
                state = (state & ~mask) | what | released;
            } else {
                state = (state & ~mask) | what | locked;
                //Log.d("Here", "released to locked!");
                //Log.d("Here", "released to locked! = " + String.valueOf(state));
            }
        } else if ((state & used) != 0) {
            // 如果之前标记功能键已经使用过了，但是操作者还是按着功能键
            // repeat after use
            //Log.d("Here", " Repeating after used……!");
        } else if ((state & locked) != 0) {
            // 如果之前的标记显示此功能键已经锁定，那么再按此功能键仍然是锁定状态
            //Log.d("Here", "locked repeating……!");
        } else if ((state & lock_released) != 0) {
            // 如果之前的标记显示此功能键已经锁定，并且已松开，那么再按此功能键就是解锁了
            //Log.d("Here", " lock_released to clear!");
            state &= ~mask;
        } else {
            // 如果是其他情况，那就是标记功能键被按下了，同时把what也存储起来，往下传
            //Log.d("Here", " clear to pressed!");
            state &= ~mask;
            state |= what | pressed;
            if (what == META_SYM_ON) {
                //Log.d("Here", "turn on META_SYM_TURNPAGE");
                state |= META_SYM_TURNPAGE;
            }
        }
        return state;
    }

    // 这也是一个主要函数，用于处理当CAP ALT SYM NEWSIM释放后，state状态的调整
    public static long handleKeyUp(long state, int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT) {
            return release(state, META_CAP_ON, META_CAP_MASK, META_CAP_PRESSED, META_CAP_RELEASED, META_CAP_LOCKED, META_CAP_LOCK_RELEASED, META_CAP_USED, event);
        }

        if (keyCode == KeyEvent.KEYCODE_ALT_LEFT || keyCode == KeyEvent.KEYCODE_ALT_RIGHT || keyCode == KeyEvent.KEYCODE_NUM) {
            return release(state, META_ALT_ON, META_ALT_MASK, META_ALT_PRESSED, META_ALT_RELEASED, META_ALT_LOCKED, META_ALT_LOCK_RELEASED, META_ALT_USED, event);
        }

        if (keyCode == KeyEvent.KEYCODE_SYM) {
            return release(state, META_SYM_ON, META_SYM_MASK, META_SYM_PRESSED, META_SYM_RELEASED, META_SYM_LOCKED, META_SYM_LOCK_RELEASED, META_SYM_USED, event);
        }

        if (keyCode == KeyEvent.KEYCODE_SHIFT_LEFT) {
            return release(state, META_NEWSIM_ON, META_NEWSIM_MASK, META_NEWSIM_PRESSED, META_NEWSIM_RELEASED, META_NEWSIM_LOCKED, META_NEWSIM_LOCK_RELEASED, META_NEWSIM_USED, event);
        }

        return state;
    }

    private static long release(long state, int what, long mask, long pressed, long released, long locked, long lock_released, long used, KeyEvent event) {
        //Log.d("Here", "before release state = " + String.valueOf(state));
        if ((state & used) != 0) {
            // 如果之前标记显示此功能键已经使用过了，现在释放则清空状态
            //Log.d("Here", " used to clear!");
            state &= ~mask;
        } else if ((state & pressed) != 0) {
            // 如果之前标记被按下的状态，现在释放则表示是第一次按下些功能键，则标记为释放，同时把what传下去
            //Log.d("Here", " pressed to released!");
            state &= ~mask;
            state |= what | released;
        } else if ((state & locked) != 0) {
            // 如果之前标记被按下的状态，现在释放则表示是第一次按下些功能键，则标记为释放，同时把what传下去
            //Log.d("Here", " locked to lock_released!");
            state &= ~mask;
            state |= what | lock_released;
        } else if ((state & released) != 0) {
            if (what == META_SYM_ON) {
                //Log.d("Here", "turn on META_SYM_TURNPAGE");
                state |= META_SYM_TURNPAGE;
            }
        } else {
            state &= ~mask;
            //Log.d("Here", " default to clear!");
        }
        return state;
    }

    // 用于在非功能键键盘事件处理之后state状态的调整.它只应该在CAP ALT SYM NEWSIM之外的事件被处理后，才调用这个函数
    public static long adjustMetaAfterKeypress(long state) {
        if ((state & META_CAP_PRESSED) != 0) {
            // 表示之前功能键已经被按下了，并且没有释放，说明操作者一直按着功能键，那么就标记此功能键已经使用，同时还按着
            state = (state & ~META_CAP_MASK) | META_CAP_ON | META_CAP_USED;
            // Log.d("Here", "CAP pressed to used!");
        } else if ((state & META_CAP_RELEASED) != 0) {
            // 表示之前功能键已经按了然后又被释放，现在有了其他按键的事件，说明此功能键的状态已经要清空了。
            state &= ~META_CAP_MASK;
            // Log.d("Here", "CAP released to clear!");
        }

        if ((state & META_ALT_PRESSED) != 0) {
            state = (state & ~META_ALT_MASK) | META_ALT_ON | META_ALT_USED;
            // Log.d("Here", "ALT pressed to used!");
        } else if ((state & META_ALT_RELEASED) != 0) {
            state &= ~META_ALT_MASK;
            // Log.d("Here", "ALT released to clear!");
        }

        if ((state & META_SYM_PRESSED) != 0) {
            state = (state & ~META_SYM_MASK) | META_SYM_ON | META_SYM_USED;
            // Log.d("Here", "SYM pressed to used!");
        } else if ((state & META_SYM_RELEASED) != 0) {
            // Log.d("Here", "SYM released to clear!");
            state &= ~META_SYM_MASK;
        }

        if ((state & META_NEWSIM_PRESSED) != 0) {
            state = (state & ~META_NEWSIM_MASK) | META_NEWSIM_ON | META_NEWSIM_USED;
            // Log.d("Here", "NEWSIM pressed to used!");
        } else if ((state & META_NEWSIM_RELEASED) != 0) {
            state &= ~META_NEWSIM_MASK;
            // Log.d("Here", "NEWSIM released to clear!");
        }
        return state;
    }

    // 清除state中的某些键的状态值
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
