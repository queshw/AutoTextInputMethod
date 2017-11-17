package cn.queshw.autotextinputmethod;

import java.util.HashMap;

public class GenAutotext {
	public final char CHINESE_QUOTATION_LEFT = '【';
    public final char CHINESE_QUOTATION_RIGHT = '】';
    
    private HashMap<String, String> result = new HashMap<String, String>();
    
	public GenAutotext() {
		// TODO Auto-generated constructor stub
	}
	
	//把一个字串转化为autotext的条目
	HashMap<String, String> gen(String line){
		result.clear();
		line = line.trim();
        if (line.isEmpty()) {//如果传入了一个空字串
            return null;
        }
        
        //去掉空的项
        line = this.escape(line);
        final String[] tempitem = line.split("[,]");
        int realItemNumber = 0;
        for (int i = 0; i < tempitem.length; ++i) {
            tempitem[i] = tempitem[i].trim();
            if (!tempitem[i].isEmpty()) {
                ++realItemNumber;
            }
        }
        
        //把数组重新排列
        final String[] item = new String[realItemNumber];
        realItemNumber = 0;
        for (int j = 0; j < tempitem.length; ++j) {
            tempitem[j] = tempitem[j].trim();
            if (!tempitem[j].isEmpty()) {
                item[realItemNumber] = tempitem[j];
                ++realItemNumber;
            }
        }
        if(realItemNumber <= 1) return null;//如果不是一个完整的autotext条目
        
        //计算替换项有几页
        final int candiWordNumber = item.length - 1;
        int candiPageNumber;      
        if (candiWordNumber % 9 > 0) {
            candiPageNumber = candiWordNumber / 9 + 1;
        }
        else {
            candiPageNumber = candiWordNumber / 9;
        }
        
        //构造按页分的数组，在每一个项前标上数字
        final String[] pages = new String[candiPageNumber + 1];
        pages[0] = item[0];//第一项为编码
        int itemIndex = 1;
        for (int k = 1; k <= candiPageNumber; ++k) {
            final StringBuilder candiPage = new StringBuilder();
            for (int i = 0; i < 9 && itemIndex <= candiWordNumber; ++itemIndex, ++i) {
                if (itemIndex == candiWordNumber && i == 0) {
                    candiPage.append(item[itemIndex]);//如果只有一项
                }
                else {
                    candiPage.append(String.valueOf(String.valueOf(i + 1)) + item[itemIndex]);//在每个项目前加上数字
                }
            }
            pages[k] = candiPage.toString();
        }//到此为止，已经有一个按页构造好的数组，如果有多个替换项，每个项之前已经标好数字
        
        //接下来，准备生成autotext条目
        if (candiPageNumber > 1) {//如果超过一页，则在开头后结尾加上中文的中括号
            pages[1] = String.valueOf(CHINESE_QUOTATION_LEFT) + pages[1];
            pages[pages.length - 1] = pages[pages.length - 1] + CHINESE_QUOTATION_RIGHT;
        }
        
        if (candiPageNumber == 1) {//如果只有一页
            final String[] nextitem = pages[1].split("[1-9]");//把替换项用数字分开
            if (nextitem.length == 1) {//说明只有一个替换项
            	result.put(this.recover(pages[0]), pages[1]);
            }
            else {//如果有多个替换项
            	result.put(this.recover(pages[0]), pages[1]);
            	result.put(this.recover(pages[1]) + "a", "%b");
                this.writeEntries(pages[1]);
            }
        }
        else {//如果不止一页
            for (int k = 0; k < pages.length; ++k) {
            	//处理多页的往后翻
            	if (k == pages.length - 1) result.put(recover(pages[k]), recover(pages[1]) + "%B");    //如果为最后一页，则翻回第一页   
            	else result.put(recover(pages[k]), recover(pages[k + 1]) + "%B");//否则往后翻
                
            	//接下来处理往前翻
            	if(k == 1) result.put(recover(pages[1]) + "0", recover(pages[1]) + "%B");
            	else if(k > 1) result.put(pages[k] + "0", recover(pages[k - 1]) + "%B");
            	
            	//接下来处理删除
            	if(k != 0) result.put(recover(pages[k]) + "a", "%b");//page[0]是编码
            	
            	//接下来处理各页中的替换项
            	this.writeEntries(pages[k]);   	
            }
        }
		return result;
	}
	
	
	private void writeEntries(final String s) {
		// TODO Auto-generated method stub
        final String[] item = s.split("[1-9]");
            for (int i = 1; i < item.length; ++i) {//之所以从1开始，是因为第一项要么为空，要么为中括号【
            	if (item[i].charAt(item[i].length() - 1) == CHINESE_QUOTATION_RIGHT) {//如果最后一个字符是右中括号，说明是这是多页中的最后一页
            			result.put(this.recover(s) + getchar(i), "%b" + item[i].substring(0, item[i].length() - 1));
                 }else{//如果是多页中的其他页
                    	result.put(this.recover(s) + getchar(i), "%b" + item[i]);
                 }
            }
	}

	//多选字时，与数字对应的字母是什么
    String getchar(final int i) {
        String c = "";
        switch (i) {
            case 0: {
                c = "0";
                break;
            }
          case 1: {
                c = "";
                break;
            }
            case 2: {
                c = "e";
                break;
            }
            case 3: {
                c = "r";
                break;
            }
            case 4: {
                c = "s";
                break;
            }
            case 5: {
                c = "d";
                break;
            }
            case 6: {
                c = "f";
                break;
            }
            case 7: {
                c = "z";
                break;
            }
            case 8: {
                c = "x";
                break;
            }
            case 9: {
                c = "c";
                break;
            }
        }
        return c;
    }
	//把转义字符恢复成原状
	private String recover(final String str) {
        final StringBuilder s = new StringBuilder();
        final String[] item = str.split("#");
        for (int i = 0; i < item.length; ++i) {
            if (item[i].equals("NUMBER_ZERO")) {
                s.append("0");
            }
            else if (item[i].equals("NUMBER_ONE")) {
                s.append("1");
            }
            else if (item[i].equals("NUMBER_TWO")) {
                s.append("2");
            }
            else if (item[i].equals("NUMBER_THREE")) {
                s.append("3");
            }
            else if (item[i].equals("NUMBER_FOUR")) {
                s.append("4");
            }
            else if (item[i].equals("NUMBER_FIVE")) {
                s.append("5");
            }
            else if (item[i].equals("NUMBER_SIX")) {
                s.append("6");
            }
            else if (item[i].equals("NUMBER_SEVEN")) {
                s.append("7");
            }
            else if (item[i].equals("NUMBER_EIGHT")) {
                s.append("8");
            }
            else if (item[i].equals("NUMBER_NINE")) {
                s.append("9");
            }
            else if (item[i].equals("CHINESE_QUOTATION_LEFT")) {
                s.append(CHINESE_QUOTATION_LEFT);
            }
            else if (item[i].equals("CHINESE_QUOTATION_RIGHT")) {
                s.append(CHINESE_QUOTATION_RIGHT);
            }
            else if (item[i].equals("SINGLE_QUOTATION")) {
                s.append("#SINGLE_QUOTATION#");
            }
            else if (item[i].equals("SHARP")) {
                s.append("#SHARP#");
            }
            else if (item[i].equals("COMMA")) {
                s.append("#COMMA#");
            }
            else {
                s.append(item[i]);
            }
        }
        return s.toString();
    }
    
	//把相关字符换成转义字符
    private String escape(final String str) {
        final StringBuilder s = new StringBuilder();
        for (int i = 0; i < str.length(); ++i) {
            final char c = str.charAt(i);
            if (c == '0') {
                s.append("#NUMBER_ZERO#");
            }
            else if (c == '1') {
                s.append("#NUMBER_ONE#");
            }
            else if (c == '2') {
                s.append("#NUMBER_TWO#");
            }
            else if (c == '3') {
                s.append("#NUMBER_THREE#");
            }
            else if (c == '4') {
                s.append("#NUMBER_FOUR#");
            }
            else if (c == '5') {
                s.append("#NUMBER_FIVE#");
            }
            else if (c == '6') {
                s.append("#NUMBER_SIX#");
            }
            else if (c == '7') {
                s.append("#NUMBER_SEVEN#");
            }
            else if (c == '8') {
                s.append("#NUMBER_EIGHT#");
            }
            else if (c == '9') {
                s.append("#NUMBER_NINE#");
            }
            else if (c == CHINESE_QUOTATION_LEFT) {
                s.append("#CHINESE_QUOTATION_LEFT#");
            }
            else if (c == CHINESE_QUOTATION_RIGHT) {
                s.append("#CHINESE_QUOTATION_RIGHT#");
            }
            else if (c == '\'') {
                s.append("#SINGLE_QUOTATION#");
            }
            else {
                s.append(c);
            }
        }
        return s.toString();
    }

}
