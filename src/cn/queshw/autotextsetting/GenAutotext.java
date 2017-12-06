package cn.queshw.autotextsetting;

import java.util.ArrayList;
import java.util.HashMap;

import android.os.Bundle;
import android.util.Log;

public class GenAutotext {
	public final char CHINESE_QUOTATION_LEFT = '【';
	public final char CHINESE_QUOTATION_RIGHT = '】';

	// private HashMap<String, String> result = new HashMap<String, String>();
	private ArrayList<String> input = new ArrayList<String>();// 用于存放autotext条目的input
	private ArrayList<String> autotext = new ArrayList<String>();// 用于存放autotext条目的autotext

	
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
		line = this.escape(line);
		final String[] tempitem = line.split("[,]");
		int realItemNumber = 0;
		for (int i = 0; i < tempitem.length; ++i) {
			tempitem[i] = tempitem[i].trim();
			if (!tempitem[i].isEmpty()) {
				++realItemNumber;
			}
		}

		// 把数组重新排列
		final String[] item = new String[realItemNumber];
		realItemNumber = 0;
		for (int j = 0; j < tempitem.length; ++j) {
			tempitem[j] = tempitem[j].trim();
			if (!tempitem[j].isEmpty()) {
				item[realItemNumber] = tempitem[j];
				++realItemNumber;
			}
		}
		if (realItemNumber <= 1)
			return;// 如果不是一个完整的autotext条目

		// 计算替换项有几页
		final int candiWordNumber = item.length - 1;
		int candiPageNumber;
		if (candiWordNumber % 9 > 0) {
			candiPageNumber = candiWordNumber / 9 + 1;
		} else {
			candiPageNumber = candiWordNumber / 9;
		}

		// 构造按页分的数组，在每一个项前标上数字
		final String[] pages = new String[candiPageNumber + 1];
		pages[0] = item[0];// 第一项为编码
		int itemIndex = 1;
		for (int k = 1; k <= candiPageNumber; ++k) {
			final StringBuilder candiPage = new StringBuilder();
			for (int i = 0; i < 9 && itemIndex <= candiWordNumber; ++itemIndex, ++i) {
				if (itemIndex == candiWordNumber && i == 0) {
					candiPage.append(item[itemIndex]);// 如果只有一项
				} else {
					candiPage.append(getNumber(i + 1) + item[itemIndex]);// 在每个项目前加上数字
				}
			}
			pages[k] = candiPage.toString();
		}// 到此为止，已经有一个按页构造好的数组，如果有多个替换项，每个项之前已经标好数字

		// 接下来，准备生成autotext条目
		if (candiPageNumber > 1) {// 如果超过一页，则在开头后结尾加上中文的中括号
			pages[1] = String.valueOf(CHINESE_QUOTATION_LEFT) + pages[1];
			pages[pages.length - 1] = pages[pages.length - 1] + CHINESE_QUOTATION_RIGHT;
		}

		if (candiPageNumber == 1) {// 如果只有一页
			final String[] nextitem = pages[1].split("[➊➋➌➍➎➏➐➑➒]");// 把替换项用数字分开
			if (nextitem.length == 1) {// 说明只有一个替换项
				input.add(recover(pages[0]));
				autotext.add("%b" + recover(pages[1]));
			} else {// 如果有多个替换项
				input.add(recover(pages[0]));
				autotext.add(recover(pages[1]) + "%B");

				input.add(recover(pages[1]) + "a");
				autotext.add("%b");

				this.writeEntries(pages[1], true);
			}
		} else {// 如果不止一页
			for (int k = 0; k < pages.length; ++k) {
				// 处理多页的往后翻
				if (k == pages.length - 1) {
					input.add(recover(pages[k]));
					autotext.add(recover(pages[1]) + "%B");// 如果为最后一页，则翻回第一页
				} else {
					input.add(recover(pages[k]));
					autotext.add(recover(pages[k + 1]) + "%B");// 否则往后翻
				}
				// 接下来处理往前翻
				if (k == 1) {
					input.add(recover(pages[1]) + "0");
					autotext.add(recover(pages[1]) + "%B");
				} else if (k > 1) {
					input.add(pages[k] + "0");
					autotext.add(recover(pages[k - 1]) + "%B");
				}
				// 接下来处理删除
				if (k != 0) {
					input.add(recover(pages[k]) + "a");
					autotext.add("%b");// page[0]是编码
					// 接下来处理各页中的替换项
					this.writeEntries(pages[k], false);
				}
			}
		}
	}

	private String getNumber(int i) {
		// TODO Auto-generated method stub
		String c = "";
		switch (i) {
		case 1: {
			c = "➊";
			break;
		}
		case 2: {
			c = "➋";
			break;
		}
		case 3: {
			c = "➌";
			break;
		}
		case 4: {
			c = "➍";
			break;
		}
		case 5: {
			c = "➎";
			break;
		}
		case 6: {
			c = "➏";
			break;
		}
		case 7: {
			c = "➐";
			break;
		}
		case 8: {
			c = "➑";
			break;
		}
		case 9: {
			c = "➒";
			break;
		}
		}
		return c;
	}

	private void writeEntries(final String s, boolean singlePage) {
		// TODO Auto-generated method stub
		final String[] item = s.split("[➊➋➌➍➎➏➐➑➒]");
		if (item.length == 1 && !singlePage) {
			input.add(recover(s) + getchar(1));
			autotext.add("%b" + recover(item[0].substring(0, item[0].length() - 1)));
			return;
		}
		for (int i = 1; i < item.length; ++i) {// 第一项可能为空，也可能中括号【
			// 如果是单页
			// if(item[i].isEmpty() ||
			// item[i].equals(String.valueOf(CHINESE_QUOTATION_LEFT)))
			// continue;// 第一项可能为空，也可能中括号【。如果本页只有一项的时候，则是多页中最后一页只有一项
			if (singlePage && i == 1) {
				input.add(recover(s));
				autotext.add("%b" + recover(item[i]));
			} else if (singlePage) {
				input.add(recover(s) + getchar(i));
				autotext.add("%b" + recover(item[i]));
			}
			// 如果是多页
			if (!singlePage && item[i].substring(item[i].length() - 1).equals(String.valueOf(CHINESE_QUOTATION_RIGHT))) { // 如果最后一个字符是右中括号，说明是这是多页中的最后一页
				input.add(recover(s) + getchar(i));
				autotext.add("%b" + recover(item[i].substring(0, item[i].length() - 1)));
			} else if (!singlePage) {
				// 如果是多页中的其他页
				input.add(recover(s) + getchar(i));
				autotext.add("%b" + recover(item[i]));
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
		case 9: {
			c = "c";
			break;
		}
		}
		return c;
	}

	// 把相关字符换成转义字符
	private String escape(final String str) {
		final StringBuilder s = new StringBuilder();
		for (int i = 0; i < str.length(); ++i) {
			final char c = str.charAt(i);
			if (c == '➊') {
				s.append("#NUMBER_ONE#");
			} else if (c == '➋') {
				s.append("#NUMBER_TWO#");
			} else if (c == '➌') {
				s.append("#NUMBER_THREE#");
			} else if (c == '➍') {
				s.append("#NUMBER_FOUR#");
			} else if (c == '➎') {
				s.append("#NUMBER_FIVE#");
			} else if (c == '➏') {
				s.append("#NUMBER_SIX#");
			} else if (c == '➐') {
				s.append("#NUMBER_SEVEN#");
			} else if (c == '➑') {
				s.append("#NUMBER_EIGHT#");
			} else if (c == '➒') {
				s.append("#NUMBER_NINE#");
			} else if (c == CHINESE_QUOTATION_LEFT) {
				s.append("#CHINESE_QUOTATION_LEFT#");
			} else if (c == CHINESE_QUOTATION_RIGHT) {
				s.append("#CHINESE_QUOTATION_RIGHT#");
			} else if (c == '\'') {
				s.append("#SINGLE_QUOTATION#");
			} else {
				s.append(c);
			}
		}
		return s.toString();
	}

	// 把转义字符恢复成原状
	private String recover(final String str) {
		final StringBuilder s = new StringBuilder();
		final String[] item = str.split("#");
		for (int i = 0; i < item.length; ++i) {
			if (item[i].equals("NUMBER_ONE")) {
				s.append("➊");
			} else if (item[i].equals("NUMBER_TWO")) {
				s.append("➋");
			} else if (item[i].equals("NUMBER_THREE")) {
				s.append("➌");
			} else if (item[i].equals("NUMBER_FOUR")) {
				s.append("➍");
			} else if (item[i].equals("NUMBER_FIVE")) {
				s.append("➎");
			} else if (item[i].equals("NUMBER_SIX")) {
				s.append("➏");
			} else if (item[i].equals("NUMBER_SEVEN")) {
				s.append("➐");
			} else if (item[i].equals("NUMBER_EIGHT")) {
				s.append("➑");
			} else if (item[i].equals("NUMBER_NINE")) {
				s.append("➒");
			} else if (item[i].equals("CHINESE_QUOTATION_LEFT")) {
				s.append(CHINESE_QUOTATION_LEFT);
			} else if (item[i].equals("CHINESE_QUOTATION_RIGHT")) {
				s.append(CHINESE_QUOTATION_RIGHT);
			} else if (item[i].equals("SINGLE_QUOTATION")) {
				s.append("#SINGLE_QUOTATION#");
			} else if (item[i].toUpperCase().equals("SHARP")) {
				s.append("#SHARP#");
			} else if (item[i].toUpperCase().equals("COMMA")) {
				s.append("#COMMA#");
			} else {
				s.append(item[i]);
			}
		}
		return s.toString();
	}

}
