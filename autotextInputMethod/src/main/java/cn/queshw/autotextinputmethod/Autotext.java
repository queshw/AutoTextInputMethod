package cn.queshw.autotextinputmethod;

//用于管理单词替换过程中的几个变量
final class Autotext {
    public final static int CLEAR = 0;//所有替换开始前，或者连input都没有找到，比如当前字串 在数据库最长input的范围内 没有找到空格或换行符
    public final static int AFTER = 1;//替换成功后
    public final static int REVERSE_AFTER = 2;//反向替换成功后
    public final static int FAIL = 3;//替换失败
    public final static int DEL = 4;//普通的使用backspace删除，或者用快捷键删除，比如全删，删除一行等
    public final static int SELECT_DEL = 5;//start 和 end 的光标不一致，再输入字符后，把原字符删除了
    public final static int UNDO = 6;//使用了undo功能后。就是把autotext对象中的input字符按位置放回去

    private int start;//刚替换成功的字串的开始位置
    private int end;//刚替换成功的字串的结束位置
    private String input;//被替换的字串，包含空格
    private String autotext;//用于替换的字串，也包含空格
    private int stat;//状态码

    public Autotext() {
        clear();
    }

    public void clear() {
        start = -1;
        end = -1;
        input = "";
        autotext = "";
        stat = CLEAR;
    }

    //更新状态
    public void update(int start, int end,String be, String af, int stat) {
        this.start = start;
        this.end = end;
        input = be;
        autotext = af;
        this.stat = stat;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public String getInput() {
        return input;
    }

    public String getAutotext() {
        return autotext;
    }

    public int getStat() {
        return stat;
    }
}
