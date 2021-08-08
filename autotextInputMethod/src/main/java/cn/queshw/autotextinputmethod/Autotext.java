package cn.queshw.autotextinputmethod;

//用于管理单词替换过程中的几个变量
final class Autotext {
    public final static int CLEAR = 0;//所有替换开始前
    public final static int AFTER = 1;//替换成功后
    public final static int REVERSE_AFTER = 2;//反向替换成功后
    public final static int FAIL = -1;//替换失败

    private int start;//刚替换成功的字串的开始位置
    private int end;//刚替换成功的字串的结束位置
    private String input;//用户的输入
    private String autotext;//输入法的输出，替换的字串
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
