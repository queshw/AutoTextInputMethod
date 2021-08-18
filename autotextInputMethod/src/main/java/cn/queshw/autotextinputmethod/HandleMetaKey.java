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
//sym键有特殊性，它被本app设计为emoji小键盘的功能键，一被按下就直接标记为locked状态，一直到非小键盘的前后翻页键被按下为止（往后翻是sym 往前翻是0）

public class HandleMetaKey {

    public static final int META_CAP_ON = KeyEvent.META_SHIFT_RIGHT_ON;//1L << 7
    public static final int META_ALT_ON = KeyEvent.META_ALT_LEFT_ON;//1L << 4
    public static final int META_SYM_ON = KeyEvent.META_SYM_ON;//1L << 2
    public static final int META_CTRL_ON = KeyEvent.META_SHIFT_LEFT_ON;// 1L << 6 自定义的ctrl键，也就是左shift键

    public static final int META_CAP_LOCKED = 0x100;// 1L << 8
    public static final int META_ALT_LOCKED = 0x200;// 1L << 9
    public static final int META_SYM_LOCKED = 0x400;// 1L << 10
    public static final int META_CTRL_LOCKED = 0x800;// 1L << 11

    private static final long META_CAP_USED = 1L << 32;
    private static final long META_ALT_USED = 1L << 33;
    private static final long META_SYM_USED = 1L << 34;
    private static final long META_CTRL_USED = 1L << 35;

    private static final long META_CAP_LOCK_RELEASED = 1L << 36;
    private static final long META_ALT_LOCK_RELEASED = 1L << 37;
    private static final long META_SYM_LOCK_RELEASED = 1L << 38;
    private static final long META_CTRL_LOCK_RELEASED = 1L << 39;

    private static final long META_CAP_PRESSED = 1L << 40;
    private static final long META_ALT_PRESSED = 1L << 41;
    private static final long META_SYM_PRESSED = 1L << 42;
    private static final long META_CTRL_PRESSED = 1L << 43;

    private static final long META_CAP_RELEASED = 1L << 48;
    private static final long META_ALT_RELEASED = 1L << 49;
    private static final long META_SYM_RELEASED = 1L << 50;
    private static final long META_CTRL_RELEASED = 1L << 51;

    public static final long META_CAP_ALL = META_CAP_ON | META_CAP_PRESSED | META_CAP_RELEASED | META_CAP_LOCKED |  META_CAP_LOCK_RELEASED | META_CAP_USED;
    public static final long META_ALT_ALL = META_ALT_ON | META_ALT_PRESSED | META_ALT_RELEASED | META_ALT_LOCKED |  META_ALT_LOCK_RELEASED | META_ALT_USED;
    public static final long META_SYM_ALL = META_SYM_ON | META_SYM_PRESSED | META_SYM_RELEASED | META_SYM_LOCKED |  META_SYM_LOCK_RELEASED | META_SYM_USED;
    public static final long META_CTRL_ALL = META_CTRL_ON | META_CTRL_PRESSED | META_CTRL_RELEASED | META_CTRL_LOCKED |  META_CTRL_LOCK_RELEASED | META_CTRL_USED;


    private static final long META_ALL = META_ALT_ALL | META_CTRL_ALL | META_SYM_ALL | META_CAP_ALL;

    private static final long META_CAP_MASK = META_ALL;
    private static final long META_ALT_MASK = META_ALL;
    private static final long META_SYM_MASK = META_ALL;
    private static final long META_CTRL_MASK = META_ALL;


    // 用于从state中获得metastate，这个metastate与当前的键盘事件中带的metastate不同，它们的综合效果者是最终的metastate。应该这样用：
    // mMetaState = event.getMetaState() | listener.getMetaState()
    // 从long state中获得功能键的状态，只检测四个键哦
    public static final int getMetaState(long state) {
        int result = 0;

        if ((state & (META_CAP_LOCKED | META_CAP_LOCK_RELEASED)) != 0) {
            result |= META_CAP_LOCKED | META_CAP_ON;
        } else if ((state & (META_CAP_ON | META_CAP_PRESSED | META_CAP_RELEASED)) != 0) {
            result |= META_CAP_ON;
        }

        if ((state & (META_ALT_LOCKED | META_ALT_LOCK_RELEASED)) != 0) {
            result |= META_ALT_LOCKED | META_ALT_ON;
        } else if ((state & (META_ALT_ON | META_ALT_PRESSED | META_ALT_RELEASED)) != 0) {
            result |= META_ALT_ON;
        }

        if ((state & (META_SYM_LOCKED | META_SYM_LOCK_RELEASED)) != 0) {
            result |= META_SYM_LOCKED | META_SYM_ON;
        } else if ((state & (META_SYM_ON | META_SYM_PRESSED | META_SYM_RELEASED)) != 0) {
            result |= META_SYM_ON;
        }

        if ((state & (META_CTRL_LOCKED | META_CTRL_LOCK_RELEASED)) != 0) {
            result |= META_CTRL_LOCKED | META_CTRL_ON;
        } else if ((state & (META_CTRL_ON | META_CTRL_PRESSED | META_CTRL_RELEASED)) != 0) {
            result |= META_CTRL_ON;
        }

        return result;
    }

    // 这个是一个主要的函数，用于处理当CAP ALT SYM NEWSIM被按下后，state状态的调整
    // 如果按下的不是一个功能键，那么就用adjustMetaAfterKeypress函数来调整功能键秀状态
    public static long handleKeyDown(long state, int keyCode) {
        //首先清空除了keycode外的其他功能键的状态，但是也不是全部清除，pressed和locked是属于一直按着才会设置的，可以保留
        // released 和 lock released 两种状态表明，其他功能键 已经被按下，并且释放了，现在又按了不同的功能键，其他功能键的状态的这两个状态就要清空
        // 如果当前的键盘事件是右shift被按下
        if (keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT) {
            state = state & (META_CAP_ALL |
                    META_SYM_PRESSED | META_SYM_LOCKED | META_ALT_PRESSED | META_ALT_LOCKED | META_CTRL_PRESSED | META_CTRL_LOCKED);

            return press(state, META_CAP_ON, META_CAP_MASK, META_CAP_LOCKED, META_CAP_PRESSED, META_CAP_RELEASED, META_CAP_LOCK_RELEASED, META_CAP_USED);
        }
        // 如果当前的键盘事件是alt被按下
        else if (keyCode == KeyEvent.KEYCODE_ALT_LEFT || keyCode == KeyEvent.KEYCODE_ALT_RIGHT || keyCode == KeyEvent.KEYCODE_NUM) {
            state = state & (META_ALT_ALL |
                    META_SYM_PRESSED | META_SYM_LOCKED | META_CAP_PRESSED | META_CAP_LOCKED | META_CTRL_PRESSED | META_CTRL_LOCKED);

            return press(state, META_ALT_ON, META_ALT_MASK, META_ALT_LOCKED, META_ALT_PRESSED, META_ALT_RELEASED, META_ALT_LOCK_RELEASED, META_ALT_USED);
        }
        // 如果当前的键盘事件是sym被按下
        else if (keyCode == KeyEvent.KEYCODE_SYM) {
            state = state & (META_SYM_ALL |
                    META_ALT_PRESSED | META_ALT_LOCKED | META_CAP_PRESSED | META_CAP_LOCKED | META_CTRL_PRESSED | META_CTRL_LOCKED);
            return press(state, META_SYM_ON, META_SYM_MASK, META_SYM_LOCKED, META_SYM_PRESSED, META_SYM_RELEASED, META_SYM_LOCK_RELEASED, META_SYM_USED);
        }
        // 如果当前的键盘事件是左shift被按下，当作ctrl
        else if (keyCode == KeyEvent.KEYCODE_SHIFT_LEFT) {
            state = state & (META_CTRL_ALL |
                    META_ALT_PRESSED | META_ALT_LOCKED | META_CAP_PRESSED | META_CAP_LOCKED | META_SYM_PRESSED | META_SYM_LOCKED);
            return press(state, META_CTRL_ON, META_CTRL_MASK, META_CTRL_LOCKED, META_CTRL_PRESSED, META_CTRL_RELEASED, META_CTRL_LOCK_RELEASED, META_CTRL_USED);
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
        } else if ((state & released) != 0) {
            // 如果之前的标记此功能键被按下之后又释放了，现在又按下，说明操作者是按一下之后又按了一下，那就是锁定了
            // 同时清空其他功能键的状态
            state = (state & ~mask) | what | locked;
        } else if ((state & used) != 0) {
            // 如果之前标记功能键已经使用过了，但是操作者还是按着功能键
        } else if ((state & locked) != 0) {
            // 如果之前的标记显示此功能键已经锁定，那么再按此功能键仍然是锁定状态
        } else if ((state & lock_released) != 0) {
            // 如果之前的标记显示此功能键已经锁定，并且已松开，那么再按此功能键就是解锁了
            state &= ~mask;
        } else {
            // 如果是其他情况，那就是标记功能键被按下了，同时把what也存储起来，往下传
            state &= ~mask;
            if (what == META_SYM_ON) {
                state |= what | META_SYM_LOCKED;//sym一按下就变成locked状态
            }else{
                state |= what | pressed;
            }
        }
        return state;
    }

    // 这也是一个主要函数，用于处理当CAP ALT SYM NEWSIM弹起后，state状态的调整
    // 当是非功能键弹起的时候，由于功能键的状态已经在按下的时候调整过，这里就不作变动了
    public static long handleKeyUp(long state, int keyCode) {
        if (keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT) {
            return release(state, META_CAP_ON, META_CAP_MASK, META_CAP_PRESSED, META_CAP_RELEASED, META_CAP_LOCKED, META_CAP_LOCK_RELEASED, META_CAP_USED);
        }

        else if (keyCode == KeyEvent.KEYCODE_ALT_LEFT || keyCode == KeyEvent.KEYCODE_ALT_RIGHT || keyCode == KeyEvent.KEYCODE_NUM) {
            return release(state, META_ALT_ON, META_ALT_MASK, META_ALT_PRESSED, META_ALT_RELEASED, META_ALT_LOCKED, META_ALT_LOCK_RELEASED, META_ALT_USED);
        }

        else if (keyCode == KeyEvent.KEYCODE_SYM) {
            return release(state, META_SYM_ON, META_SYM_MASK, META_SYM_PRESSED, META_SYM_RELEASED, META_SYM_LOCKED, META_SYM_LOCK_RELEASED, META_SYM_USED);
        }

        else if (keyCode == KeyEvent.KEYCODE_SHIFT_LEFT) {
            return release(state, META_CTRL_ON, META_CTRL_MASK, META_CTRL_PRESSED, META_CTRL_RELEASED, META_CTRL_LOCKED, META_CTRL_LOCK_RELEASED, META_CTRL_USED);
        }

        //如果是其他普通键被释放，则调用adjustMetaAfterKeypress来调整功能键状态码
        else {
            state = adjustMetaAfterKeypress(state);
        }
        return state;
    }

    private static long release(long state, int what, long mask, long pressed, long released, long locked, long lock_released, long used) {
        //Log.d("Here", "before release state = " + String.valueOf(state));
        if ((state & used) != 0) {
            // 如果之前标记显示此功能键已经使用过了，现在释放则清空状态
            state &= ~mask;
        } else if ((state & pressed) != 0) {
            // 如果之前标记被按下的状态，现在释放则表示是第一次按下些功能键，则标记为释放，同时把what传下去
            state &= ~mask;
            state |= what | released;
        } else if ((state & locked) != 0) {
            // 如果之前标记被按下的状态，现在释放则表示是第一次按下些功能键，则标记为释放，同时把what传下去
            // 对于sym键来说，locked状态后，一直不变，除非按了其他的非功能键，在自定义的小表情键盘中去改变状态
            state &= ~mask;
            if(what == META_SYM_ON){
                state |= what | locked;
            }
            else{
                state |= what | lock_released;
            }
        } else if ((state & released) != 0) {
            //表示功能键被按下后，已经轮松开了，不可能再松一次，所以这个地方是不会被运行的
        } else {
            state &= ~mask;
        }
        return state;
    }

    // 用于在非功能键键盘事件处理之后state状态的调整.它只应该在CAP ALT SYM NEWSIM之外的事件被处理后，才调用这个函数
    public static long adjustMetaAfterKeypress(long state) {
        if ((state & META_CAP_PRESSED) != 0) {
            // 表示之前功能键已经被按下了，并且没有释放，说明操作者一直按着功能键，那么就标记此功能键已经使用，同时还按着
            state = (state & ~META_CAP_MASK) | META_CAP_ON | META_CAP_USED;
        } else if ((state & META_CAP_RELEASED) != 0) {
            // 表示之前功能键已经按了然后又被释放，现在有了其他按键的事件，说明此功能键的状态已经要清空了。
            state &= ~META_CAP_MASK;
        }

        if ((state & META_ALT_PRESSED) != 0) {
            state = (state & ~META_ALT_MASK) | META_ALT_ON | META_ALT_USED;
        } else if ((state & META_ALT_RELEASED) != 0) {
            state &= ~META_ALT_MASK;
        }

        if ((state & META_SYM_PRESSED) != 0) {
            state = (state & ~META_SYM_MASK) | META_SYM_ON | META_SYM_USED;
        } else if ((state & META_SYM_RELEASED) != 0) {
            state &= ~META_SYM_MASK;
        }

        if ((state & META_CTRL_PRESSED) != 0) {
            state = (state & ~META_CTRL_MASK) | META_CTRL_ON | META_CTRL_USED;
        } else if ((state & META_CTRL_RELEASED) != 0) {
            state &= ~META_CTRL_MASK;
        }
        return state;
    }

}
