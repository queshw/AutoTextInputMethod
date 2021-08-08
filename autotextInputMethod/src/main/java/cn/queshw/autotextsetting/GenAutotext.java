package cn.queshw.autotextsetting;

import static java.lang.String.valueOf;

import java.util.ArrayList;

public class GenAutotext {
	public final char CHINESE_QUOTATION_LEFT = '[';
	public final char CHINESE_QUOTATION_RIGHT = ']';

	// private HashMap<String, String> result = new HashMap<String, String>();
	private ArrayList<String> input = new ArrayList<String>();// 用于存放autotext条目的input
	private ArrayList<String> autotext = new ArrayList<String>();// 用于存放autotext条目的autotext
    private int numcodesPrepage = 6;//每页最多几个编码，最大为9，最小为2
	
	public GenAutotext() {
		// TODO Auto-generated constructor stub
	}
	
	public ArrayList<String> getInputList(){
		return input;
	}
	
	public ArrayList<String> getAutotextList(){
		return autotext;
	}

	// 把一个字串转化为autotext的条目
	public void gen(String line) {
		input.clear();
		autotext.clear();
		line = line.trim();
		if (line.isEmpty()) {// 如果传入了一个空字串
			return;
		}

		// 去掉空的项
		//line = this.escape(line);
		final String[] tempitem = line.split("[,]");
		int realItemNumber = 0;
		for (int i = 0; i < tempitem.length; ++i) {
			tempitem[i] = tempitem[i].trim();
			if (!tempitem[i].isEmpty()) {
				++realItemNumber;
			}
		}

		// 把数组重新排列，把空的项压缩掉
		final String[] items = new String[realItemNumber];
		realItemNumber = 0;
		for (int j = 0; j < tempitem.length; ++j) {
			tempitem[j] = tempitem[j].trim();
			if (!tempitem[j].isEmpty()) {
				items[realItemNumber] = tempitem[j];
				++realItemNumber;
			}
		}
		if (realItemNumber <= 1)
			return;// 如果不是一个完整的autotext条目

		// 计算替换项有几页
		final int candiWordNumber = items.length - 1;
		int candiPageNumber;
		if (candiWordNumber % numcodesPrepage > 0) {
			candiPageNumber = candiWordNumber / numcodesPrepage + 1;
		} else {
			candiPageNumber = candiWordNumber / numcodesPrepage;
		}

		// 构造按页分的数组，在每一个项前标上数字
		final String[] pages = new String[candiPageNumber + 1];
		pages[0] = items[0];// 第一项为编码
		int itemIndex = 1;
		for (int k = 1; k <= candiPageNumber; ++k) {
			final StringBuilder candiPage = new StringBuilder();
			for (int i = 0; i < numcodesPrepage && itemIndex <= candiWordNumber; ++itemIndex, ++i) {
				if (itemIndex == candiWordNumber && i == 0) {
					candiPage.append(items[itemIndex]);// 如果只有一项
				} else {
					candiPage.append(getNumber(i + 1) + items[itemIndex]);// 在每个项目前加上数字
				}
			}
			pages[k] = candiPage.toString();
		}// 到此为止，已经有一个按页构造好的数组，如果有多个替换项，每个项之前已经标好数字

		// 接下来，准备生成autotext条目
		if (candiPageNumber > 1) {// 如果超过一页，则在开头后结尾加上中文的中括号
			pages[1] = valueOf(CHINESE_QUOTATION_LEFT) + pages[1];
			pages[pages.length - 1] = pages[pages.length - 1] + CHINESE_QUOTATION_RIGHT;
		}

		if (candiPageNumber == 1) {// 如果只有一页
			if (items.length == 2) {// 说明只有一个替换项
				input.add(pages[0]);
				autotext.add("%b" + pages[1]);
			} else {// 如果有多个替换项
				input.add(pages[0]);
				autotext.add(pages[1] + "%B");

				input.add(pages[1] + "a");
				autotext.add("%b");

				this.writeEntries(1, true, pages, items);
			}
		} else {// 如果不止一页
			for (int k = 0; k < pages.length; ++k) {
				// 处理多页的往后翻
				if (k == pages.length - 1) {
					input.add(pages[k]);
					autotext.add(pages[1] + "%B");// 如果为最后一页，则翻回第一页
				} else {
					input.add(pages[k]);
					autotext.add(pages[k + 1] + "%B");// 否则往后翻
				}

				// 接下来处理往前翻
				if (k == 1) {
					input.add(pages[1] + "0");
					autotext.add(pages[1] + "%B");
				} else if (k > 1) {
					input.add(pages[k] + "0");
					autotext.add(pages[k - 1] + "%B");
				}
				// 接下来处理删除
				if (k != 0) {
					input.add(pages[k] + "a");
					autotext.add("%b");// page[0]是编码
					// 接下来处理各页中的替换项
					this.writeEntries(k, false, pages, items);
				}
			}
		}
	}

	private String getNumber(int i) {
		// TODO Auto-generated method stub
		String c = "";
		switch (i) {
		case 1: {
			c = "1";
			break;
		}
		case 2: {
			c = "2";
			break;
		}
		case 3: {
			c = "3";
			break;
		}
		case 4: {
			c = "4";
			break;
		}
		case 5: {
			c = "5";
			break;
		}
		case 6: {
			c = "6";
			break;
		}
		case 7: {
			c = "7";
			break;
		}
		case 8: {
			c = "8";
			break;
		}
		case 9: {
			c = "9";
			break;
		}
		}
		return c;
	}
    //用于构建autotext的数组
	private void writeEntries(int pageNumber, boolean singlePage, String[] pages, String[] items) {
		for (int i = (pageNumber - 1) * numcodesPrepage + 1; i < pageNumber * numcodesPrepage + 1 && i < items.length; ++i) {
			// 如果是单页
			if(singlePage){
				if (i == (pageNumber - 1) * numcodesPrepage + 1) {//如果是第一项，相当于是默认，不需要再输入数字来选择
					input.add(pages[pageNumber]);
					autotext.add("%b" + items[i]);
				} else{
					input.add(pages[pageNumber] + getchar(i));
					autotext.add("%b" + items[i]);
				}
			}else{// 如果是多页
				input.add(pages[pageNumber] + getchar(i%numcodesPrepage));
				autotext.add("%b" + items[i]);
			}
		}
	}

	// 多选字时，与数字对应的字母是什么
	private String getchar(final int i) {
		String c = "";
		switch (i) {
		case 1: {
			c = "w";
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
		case 0: {//由于模计算的原因，所以为0的时候，表示第9个
			c = "c";
			break;
		}
		}
		return c;
	}
}
