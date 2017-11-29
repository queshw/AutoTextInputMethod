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
				//Log.d("Here", "methodid = " + String.valueOf(cursor.getInt(0)));
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
				cursor = db.rawQuery("select * from " + autotextTableName + " order by id", null);
				HashMap<String, String> autotextMap = new HashMap<String, String>();
				while (cursor.moveToNext()) {// 把autotext表中的条目都放到一个hashmap中
					autotextMap.put(cursor.getString(1), cursor.getString(2));
				}
				ArrayList<String[]> rawList = new ArrayList<String[]>();
				String key, value;
				for (Entry<String, String> autotext : autotextMap.entrySet()) {
					key = autotext.getKey();
					value = autotext.getValue();
					if(value.length()<2){
						rawList.add(new String[] { key, value });
						continue;
					}
					if (!autotextMap.containsValue(key + "%B") && !autotextMap.containsValue(key.subSequence(0, key.length() - 1) + "%B")) {
						// 如果key或者key减去最后一个字母可以是别的条目的替换项，则说明当前条目为别的条目产生的重码的条目。如果找不到说明就是重码选择的第一个条目
						if (value.subSequence(0, 2).equals("%b")) {// 如果头上两位是%b
							value = value.substring(2);
						} else if (value.subSequence(value.length() - 2, value.length()).equals("%B")) {// 如果最后两位是%B
							value = (String) value.subSequence(0, value.length() - 2);
							value = getCandidate(value, value, autotextMap);
							if (value.subSequence(0, 1).equals(","))
								value = value.substring(1);
						}
						// Log.d("Here", key + "," + value);
						rawList.add(new String[] { key, value });// 到此已经生成一个list，内容为所有的raw数据
					}
				}
				// 3、升级autotext表的结构
				db.execSQL("drop table " + autotextTableName);
				sql = "create table " + autotextTableName + "(id integer primary key autoincrement," 
						+ "input text not null,"
						+ "autotext text not null," 
						+ "rawid integer default 0)";
				db.execSQL(sql);
				// 4、接下来是把所有raw的数据写入raw表中
				sql = "insert into " + rawTableName + " values(null, ?, ?, null)";
				SQLiteStatement statement = db.compileStatement(sql);
				db.beginTransaction();
				for (String[] raw : rawList) {
					statement.bindString(1, raw[0]);
					statement.bindString(2, raw[1]);
					statement.executeInsert();
				}
				db.setTransactionSuccessful();
				db.endTransaction();
				//5、根据raw表，生成autotext数据
				GenAutotext ga = new GenAutotext();
				HashMap<String, String> autotext = new HashMap<String, String>();
				cursor = db.rawQuery("select id,code,candidate from " + rawTableName, null);
				while (cursor.moveToNext()) {					
//					autotext = ga.gen(cursor.getString(1) + "," + cursor.getString(2));
//					for(Entry<String,String> entry : autotext.entrySet()){
//						sql = "insert into " + autotextTableName + " values(null,'" + entry.getKey() + "','" + entry.getValue()  + "',"+ cursor.getInt(0) +")";
//						//db.execSQL("insert into ? values(?,?,?,?)", new String[]{autotextTableName, null, entry.getKey(), entry.getValue(), String.valueOf(cursor.getInt(0))});
//						db.execSQL(sql);
//					}
					autotext.clear();
				}
				cursor.close();
			}
		default:
		}
	}

	private String getCandidate(CharSequence s, CharSequence start, HashMap<String, String> m) {
		// TODO Auto-generated method stub
		String candidate = "";
		// Log.d("Here", "s = " + s);
		// Log.d("Here", "m.get(s) = " + m.get(s));
		// Log.d("Here", "m.containsKey(s) = " +
		// String.valueOf(m.containsKey(s)));
		if ((m.containsKey(s) && m.get(s).subSequence(0, 2).equals("%b")))
			candidate = candidate + "," + m.get(s).substring(2);
		if ((m.containsKey(s + "w") && m.get(s + "w").subSequence(0, 2).equals("%b")))
			candidate = candidate + "," + m.get(s + "w").substring(2);
		if ((m.containsKey(s + "e") && m.get(s + "e").subSequence(0, 2).equals("%b")))
			candidate = candidate + "," + m.get(s + "e").substring(2);
		if ((m.containsKey(s + "r") && m.get(s + "r").subSequence(0, 2).equals("%b")))
			candidate = candidate + "," + m.get(s + "r").substring(2);
		if ((m.containsKey(s + "s") && m.get(s + "s").subSequence(0, 2).equals("%b")))
			candidate = candidate + "," + m.get(s + "s").substring(2);
		if ((m.containsKey(s + "d") && m.get(s + "d").subSequence(0, 2).equals("%b")))
			candidate = candidate + "," + m.get(s + "d").substring(2);
		if ((m.containsKey(s + "f") && m.get(s + "f").subSequence(0, 2).equals("%b")))
			candidate = candidate + "," + m.get(s + "f").substring(2);
		if ((m.containsKey(s + "z") && m.get(s + "z").subSequence(0, 2).equals("%b")))
			candidate = candidate + "," + m.get(s + "z").substring(2);
		if ((m.containsKey(s + "x") && m.get(s + "x").subSequence(0, 2).equals("%b")))
			candidate = candidate + "," + m.get(s + "x").substring(2);
		if ((m.containsKey(s + "c") && m.get(s + "c").subSequence(0, 2).equals("%b")))
			candidate = candidate + "," + m.get(s + "c").substring(2);

		if (m.containsKey(s) && m.get(s).subSequence(m.get(s).length() - 2, m.get(s).length()).equals("%B")
				&& !m.get(s).subSequence(0, m.get(s).length() - 2).equals(start)) {
			candidate = candidate + this.getCandidate(m.get(s).subSequence(0, m.get(s).length() - 2), start, m);
		}
		return candidate;
	}
}
