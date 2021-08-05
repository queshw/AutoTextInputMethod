package cn.queshw.autotextsetting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBHelper extends SQLiteOpenHelper {
	private final String method_sql = "CREATE TABLE methods(" + "id INTEGER PRIMARY KEY AUTOINCREMENT," + "name TEXT NOT NULL,"
			+ "isDefault INTEGER NOT NULL)";
	private SQLiteDatabase mdb;

	public DBHelper(Context context, String name, CursorFactory factory, int version) {
		super(context, name, factory, version);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// TODO Auto-generated method stub
		mdb = db;
		mdb.execSQL(method_sql);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		switch (oldVersion) {
		case 1:// 从autotext中产生源文件
				// 第一步先取出methods表中的id号
			ArrayList<Integer> methodIdList = new ArrayList<Integer>();
			Cursor cursor = db.rawQuery("select id from methods order by id", null);
			while (cursor.moveToNext()) {
				methodIdList.add(cursor.getInt(0));
				// Log.d("Here", "methodid = " +
				// String.valueOf(cursor.getInt(0)));
			}
			// 第二步，根据method的id号，建立raw系列的表，并且从autotext表中生成raw表的条目
			for (int i : methodIdList) {
				String rawTableName = "raw" + String.valueOf(i);
				String autotextTableName = "autotext" + String.valueOf(i);
				// 1、创建raw表
				String sql = "create table " + rawTableName + "(" + "id integer primary key autoincrement," + "code text not null,"
						+ "candidate text not null," + "twolevel int default 0)";
				// Log.d("Here", "create raw sql = " + sql);
				db.execSQL(sql);

				// 2，从autotext表中生成raw表的条目
				ArrayList<String[]> rawList = new ArrayList<String[]>();// 最后生成的raw条目表
				ArrayList<String> inputList = new ArrayList<String>();// 头上两位为%b的条目
				ArrayList<String> autotextList = new ArrayList<String>();// 头上两位为%b的条目
				ArrayList<String> inputList2 = new ArrayList<String>();// 最后两位为%B的条目
				ArrayList<String> autotextList2 = new ArrayList<String>();// 最后两位为%B的条目

				// 使用过的条目删除后的列表
				ArrayList<String> inputList3 = new ArrayList<String>();// 头上两位为%b的条目
				ArrayList<String> autotextList3 = new ArrayList<String>();// 头上两位为%b的条目

				cursor = db.rawQuery("select * from " + autotextTableName + " order by id", null);
				String code, candidate;
				while (cursor.moveToNext()) {// 把autotext表中的条目都放到一个List中
					code = cursor.getString(1);
					candidate = cursor.getString(2);
					if (candidate.length() < 2) {
						rawList.add(new String[] { code, candidate });
					} else if (candidate.subSequence(0, 2).equals("%b")) {// 如果最后两位为%b
						inputList.add(code);
						autotextList.add(candidate);
					} else if (candidate.substring(candidate.length() - 2).equals("%B")) {// 如果最后两位为%B
						inputList2.add(code);
						autotextList2.add(candidate);
					} else {
						rawList.add(new String[] { code, candidate });
					}
				}

				for (int h = 0; h < inputList.size(); h++) {// 头上均为%b的列表中的循环
					code = inputList.get(h);
					candidate = autotextList.get(h);
					if (!autotextList2.contains(code + "%B") && !autotextList2.contains(code.subSequence(0, code.length() - 1) + "%B")) {
						// 如果code或者code减去最后一个字母可以是别的条目的替换项，则说明当前条目为别的条目产生的重码的条目。如果找不到说明就是单码的条目
						// if (candidate.subSequence(0, 2).equals("%b")) {//
						// 如果头上两位是%b
						// candidate = candidate.substring(2);
						// } else if (candidate.subSequence(candidate.length() -
						// 2, candidate.length()).equals("%B")) {// 如果最后两位是%B
						// candidate = (String) candidate.subSequence(0,
						// candidate.length() - 2);
						// candidate = getCandidate(candidate, candidate,
						// inputList, autotextList);
						// if (candidate.subSequence(0, 1).equals(","))
						// candidate = candidate.substring(1);
						// }
						// Log.d("Here", key + "," + value);
						candidate = candidate.substring(2);
						rawList.add(new String[] { code, candidate });
					}
					inputList3.add(code);
					autotextList3.add(candidate);
				}

				for (int h = 0; h < inputList2.size(); h++) {// 尾巴上均为%B的列表中的循环
					code = inputList2.get(h);
					candidate = autotextList2.get(h);
					if (!autotextList2.contains(code + "%B") && !autotextList2.contains(code.subSequence(0, code.length() - 1) + "%B")) {
						// 如果code或者code减去最后一个字母可以是别的条目的替换项，则说明当前条目为别的条目产生的重码的条目。如果找不到说明就是多码的第一个条目
						candidate = (String) candidate.subSequence(0, candidate.length() - 2);
						candidate = getCandidate(candidate, candidate, inputList3, autotextList3, inputList2, autotextList2);
						if (candidate.subSequence(0, 1).equals(","))
							candidate = candidate.substring(1);
						rawList.add(new String[] { code, candidate });// 到此已经生成一个list，内容为所有的raw数据
					}
				}

				// 3、升级autotext表的结构
				db.execSQL("drop table " + autotextTableName);
				sql = "create table " + autotextTableName + "(id integer primary key autoincrement," + "input text not null,"
						+ "autotext text not null," + "rawid integer default 0)";
				db.execSQL(sql);
				// 4、接下来是把所有raw的数据写入raw表中
				sql = "insert into " + rawTableName + " values(null, ?, ?, ?)";
				SQLiteStatement statement = db.compileStatement(sql);
				db.beginTransaction();
				for (String[] raw : rawList) {
					statement.bindString(1, raw[0]);
					statement.bindString(2, raw[1]);
					statement.bindLong(3, 0);
					statement.executeInsert();
				}
				db.setTransactionSuccessful();
				db.endTransaction();
				// 5、根据raw表，生成autotext数据

				GenAutotext ga = new GenAutotext();
				// 接下来处理生成autotext条目的事情
				// 1、把raw表中的数据都取出来，生成要插入的autotext的数组
				ArrayList<String> input = new ArrayList<String>();
				ArrayList<String> autotext = new ArrayList<String>();
				ArrayList<Integer> rawid = new ArrayList<Integer>();

				ArrayList<String> tempInput = new ArrayList<String>();
				ArrayList<String> tempAutotext = new ArrayList<String>();
				cursor = db.rawQuery("select * from " + rawTableName + " order by id", null);
				while (cursor.moveToNext()) {
					tempInput.clear();
					tempAutotext.clear();
					ga.gen(cursor.getString(cursor.getColumnIndex("code")) + "," + cursor.getString(cursor.getColumnIndex("candidate")));
					tempInput = ga.getInputList();
					tempAutotext = ga.getAutotextList();
					// 处理二级替换项的问题
					if (cursor.getInt(cursor.getColumnIndex("twolevel")) < 0) {// 如果当前行为二级替换项目
						// 查看这一组二级替换中，谁是第一行
						Cursor tempCursor = db.rawQuery("select min(id) from " + rawTableName + " where twolevel=?",
								new String[] { String.valueOf(cursor.getInt(cursor.getColumnIndex("twolevel"))) });
						tempCursor.moveToNext();

						if (cursor.getInt(cursor.getColumnIndex("id")) == tempCursor.getInt(0)) {// 当前为此组二级替换的第一行
							for (int i1 = 0; i1 < tempInput.size(); i1++) {
								input.add(tempInput.get(i1));
								autotext.add(tempAutotext.get(i1));
								rawid.add(cursor.getInt(cursor.getColumnIndex("twolevel")));
							}
						} else {// 当前为此组二级替换的其他行
							int j = -1;
							for (int i1 = 0; i1 < autotext.size(); i1++) {
								if (rawid.get(i1) == cursor.getInt(cursor.getColumnIndex("twolevel"))
										&& autotext.get(i1).equals("%b" + tempInput.get(0))) {
									j = i1;
									break;
								}
							}
							if (j == -1) {// 如果没有找到
								input.add(tempInput.get(0));
								autotext.add(tempAutotext.get(0));
								rawid.add(cursor.getInt(cursor.getColumnIndex("twolevel")));
							} else {// 如果找到了
								autotext.set(j, tempAutotext.get(0));
							}
							for (int i1 = 1; i1 < tempInput.size(); i1++) {
								input.add(tempInput.get(i1));
								autotext.add(tempAutotext.get(i1));
								rawid.add(cursor.getInt(cursor.getColumnIndex("twolevel")));
							}
						}
						tempCursor.close();
					} else {// 此行不是二级替换项目
						for (int i1 = 0; i1 < tempInput.size(); i1++) {
							input.add(tempInput.get(i1));
							autotext.add(tempAutotext.get(i1));
							rawid.add(cursor.getInt(cursor.getColumnIndex("id")));
						}
					}
				}
				cursor.close();// 至此已经生成用于导入autotext表的数组

				// 2、批量导入autotext表中
				sql = "insert into " + autotextTableName + " values(null, ?, ?, ?)";
				statement = db.compileStatement(sql);

				db.beginTransaction();
				for (int i1 = 0; i1 < input.size(); i1++) {
					statement.bindString(1, input.get(i1));
					statement.bindString(2, autotext.get(i1));
					statement.bindLong(3, rawid.get(i1));
					statement.executeInsert();
				}
				db.setTransactionSuccessful();
				db.endTransaction();

			}
		default:
		}
	}

	private String getCandidate(CharSequence s, CharSequence start, ArrayList<String> inputList, ArrayList<String> autotextList,
			ArrayList<String> inputList2, ArrayList<String> autotextList2) {
		// TODO Auto-generated method stub
		String candidate = "";
		int i = inputList.indexOf(s);
		if (i != -1) {
			candidate = candidate + "," + autotextList.get(i).substring(2);
			//inputList.remove(i);
		}
		i = inputList.indexOf(s + "w");
		if (i != -1) {
			candidate = candidate + "," + autotextList.get(i).substring(2);
			//inputList.remove(i);
		}
		i = inputList.indexOf(s + "e");
		if (i != -1) {
			candidate = candidate + "," + autotextList.get(i).substring(2);
			//inputList.remove(i);
		}
		i = inputList.indexOf(s + "r");
		if (i != -1) {
			candidate = candidate + "," + autotextList.get(i).substring(2);
			//inputList.remove(i);
		}
		i = inputList.indexOf(s + "s");
		if (i != -1) {
			candidate = candidate + "," + autotextList.get(i).substring(2);
			//inputList.remove(i);
		}
		i = inputList.indexOf(s + "d");
		if (i != -1) {
			candidate = candidate + "," + autotextList.get(i).substring(2);
			//inputList.remove(i);
		}
		i = inputList.indexOf(s + "f");
		if (i != -1) {
			candidate = candidate + "," + autotextList.get(i).substring(2);
			//inputList.remove(i);
		}
		i = inputList.indexOf(s + "z");
		if (i != -1) {
			candidate = candidate + "," + autotextList.get(i).substring(2);
			//inputList.remove(i);
		}
		i = inputList.indexOf(s + "x");
		if (i != -1) {
			candidate = candidate + "," + autotextList.get(i).substring(2);
			//inputList.remove(i);
		}
		i = inputList.indexOf(s + "c");
		if (i != -1) {
			candidate = candidate + "," + autotextList.get(i).substring(2);
			//inputList.remove(i);
		}

		if (inputList2.contains(s)) {
			i = inputList2.indexOf(s);// 第几个元素
			String au = autotextList2.get(i);
			int len = autotextList2.get(i).length();
			if (!au.subSequence(0, len - 2).equals(start)) {
				candidate = candidate + this.getCandidate(au.subSequence(0, len - 2), start, inputList, autotextList, inputList2, autotextList2);
			}
		}
		return candidate;
	}
}
