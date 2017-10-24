package cn.queshw.autotextsetting;

import java.util.ArrayList;

import cn.queshw.autotextinputmethod.ConstantList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.text.TextUtils;

public class DBOperations {
	public static final int NOOFFSET = -1;// 如果是提取全部数据
	public static final int NOLIMIt = -1;// 如果是提取全部数据

	private DBHelper helper;
	private SQLiteDatabase db;

	// ////////////////////////////////////////////////////
	// 构造函数
	public DBOperations(Context context) {
		// TODO Auto-generated constructor stub
		helper = new DBHelper(context, "methods.db", null, 1);
		db = helper.getWritableDatabase();
	}

	// /////////////////////////////////////////////////////
	// 提取methods的数据，构造成一个ArrayList<MethodItem>返回
	public ArrayList<MethodItem> loadMethodsData() {
		ArrayList<MethodItem> itemList = new ArrayList<MethodItem>();
		Cursor cursor;
		cursor = db.rawQuery("select * from methods order by id", null);
		while (cursor.moveToNext()) {
			MethodItem item = constructMethodItem(cursor);
			itemList.add(item);
		}
		cursor.close();
		return itemList;
	}

	// 根据id提取记录，构造MethodItem
	public MethodItem getMethodItem(int id) {
		Cursor cursor = db.rawQuery("select * from methods where id = ?",
				new String[] { String.valueOf(id) });
		cursor.moveToNext();
		MethodItem item = constructMethodItem(cursor);
		cursor.close();
		return item;
	}

	// 根据一条记录构造一个对象
	private MethodItem constructMethodItem(Cursor cursor) {
		MethodItem item = new MethodItem();
		item.setId(cursor.getInt(cursor.getColumnIndex("id")));
		item.setName(ConstantList.recover(cursor.getString(cursor.getColumnIndex("name"))));
		// Log.d("Here", "name=" +
		// cursor.getString(cursor.getColumnIndex("name")));
		item.setIsDefault(cursor.getInt(cursor.getColumnIndex("isDefault")));
		return item;
	}

	// ////////////////////////////////////////////////////////

	// ////////////////////////////////////////////////////////
	// 添加一条记录，返回id号
	public int addOrSaveMethodItem(String name, int isDefault, int id) {
		name = ConstantList.escape(name);
		//Log.d("Here", "name=" + name);
		ContentValues values = new ContentValues();
		values.put("name", name);
		values.put("isDefault", isDefault);

		if (!TextUtils.isEmpty(name)) {// 如果不为空
			Cursor cursor = db.rawQuery(
					"select isDefault from methods where id = ?",
					new String[] { String.valueOf(id) });

			if (cursor.getCount() == 0) {// 如果原来没有此条记录，则新增记录
				if (isDefault == MethodItem.DEFAULT) {
					db.execSQL("update methods set isDefault = ?",
							new String[] { String
									.valueOf(MethodItem.NOTDEFAULT) });
				}
				id = (int) db.insert("methods", null, values);

				// 创建与新增输入法的相连的词库表
				String tableName = "autotext" + String.valueOf(id);
				String sql = "create table " + tableName
						+ "(id integer primary key autoincrement,"
						+ "input text not null," + "autotext text not null)";
				//Log.d("Here", sql);
				db.execSQL(sql);
			} else {// 如果原来已经有这条记录，则修改记录		
				if (isDefault == MethodItem.DEFAULT) {//先把原有默认输入法的设置清空
					db.execSQL("update methods set isDefault = ?",
							new String[] { String
									.valueOf(MethodItem.NOTDEFAULT) });
				}
				db.update("methods", values, "id = ?", new String[] { String.valueOf(id) });
			}
			cursor.close();	
			return id;
		}
		return -1;
	}

	// 删除一条记录
	public void deleteMethodItem(String table, int id) {// ??
		// TODO Auto-generated method stub
		Cursor cursor = db.rawQuery(
				"select isDefault from methods where id = ?",
				new String[] { String.valueOf(id) });
		cursor.moveToNext();
		// int isDefault = cursor.getInt(cursor.getColumnIndex("isDefault"));//
		// 看看要删除的记录是否是默认的输入法

		db.delete("methods", "id=?", new String[] { String.valueOf(id) });
		String tableName = "autotext" + String.valueOf(id);
		String sql = "drop table if exists " + tableName;
		// Log.d("Here", sql);
		db.execSQL(sql);

		// if(isDefault == MethodItem.DEFAULT){//如果已经把默认输入法删掉了，那就把第一条记录的
		// cursor = db.rawQuery("select min(id) from methods", null);
		// cursor.moveToNext();
		// id = cursor.getInt(0);
		// db.execSQL("update methods set isDefault=? where id =?", new
		// String[]{String.valueOf(MethodItem.DEFAULT), String.valueOf(id)});
		// }
		cursor.close();

	}

	// //////////////////////////////////////////////////////////////////
	// 操作autotext的系列表
	// 根据相关参数提取记录
	public ArrayList<AutotextItem> searchAutotextItems(String table,
			String searchText, int limit, int offset) {
		//searchText.toLowerCase();
		searchText = ConstantList.escape(searchText);
		ArrayList<AutotextItem> data = new ArrayList<AutotextItem>();
		String sql = "select * from " + table + " where input like '"
				+ searchText + "%' order by input limit " + String.valueOf(limit)
				+ " offset " + String.valueOf(offset);
		// Log.d("Here", sql);
		Cursor cursor = db.rawQuery(sql, null);
		while (cursor.moveToNext()) {
			AutotextItem item;
			item = constructAutotextItem(cursor);
			data.add(item);
		}
		cursor.close();
		return data;
	}

	// 根据id提取单条记录
	public AutotextItem getAutotextItem(String table, int id) {
		AutotextItem item;
		String sql = "select * from " + table + " where id = "
				+ String.valueOf(id);
		// Log.d("Here", sql);
		Cursor cursor = db.rawQuery(sql, null);
		cursor.moveToNext();
		item = constructAutotextItem(cursor);
		cursor.close();
		return item;
	}

	// 构造autotextitem
	private AutotextItem constructAutotextItem(Cursor cursor) {
		// TODO Auto-generated method stub
		AutotextItem item = new AutotextItem();
		item.setId(cursor.getInt(cursor.getColumnIndex("id")));
		item.setInput(ConstantList.recover(cursor.getString(cursor.getColumnIndex("input"))));
		item.setAutotext(ConstantList.recover(cursor.getString(cursor.getColumnIndex("autotext"))));
		return item;
	}

	// 添加或者修改单条数据
	public void addOrSaveAutotextItem(String table, String input,
			String autotext, int id) {
		input = ConstantList.escape(input);
		autotext = ConstantList.escape(autotext);
		
		if (!TextUtils.isEmpty(input) && !TextUtils.isEmpty(autotext)) {// 如果有一个为空，则什么都不干
			// 先判断相应id号的记录是否存在，以此来确定是新增记录还是修改记录
			String sql = "select id from " + table + " where id = "
					+ String.valueOf(id);
			Cursor cursor = db.rawQuery(sql, null);

			if (cursor.getCount() == 0) {// 说明为新增记录
				sql = "insert into " + table + " values(null, '" + input
						+ "', '" + autotext + "')";
			} else {// 说明为修改原有记录
				sql = "update " + table + " set input = '" + input
						+ "', autotext='" + autotext + "' where id = "
						+ String.valueOf(id);
			}
			//Log.d("Here", sql);
			db.execSQL(sql);
			cursor.close();
		}
	}

	// 用于快速批量添加数据
	// public void importData(String table, ArrayList<String[]> data) {
	// String sql;
	// db.beginTransaction();
	// for(String[] item : data){
	// sql = "insert into " + table + " values(null, '" + item[0] + "', '"
	// + item[1] + "')";
	// db.execSQL(sql);
	// }
	// db.setTransactionSuccessful();
	// db.endTransaction();
	// }
	// 此版本效率大为提高
	public void importData(String table, ArrayList<String[]> data) {
		String sql = "insert into " + table + " values(null, ?, ?)";
		SQLiteStatement statement = db.compileStatement(sql);
		db.beginTransaction();
		for (String[] item : data) {			
			statement.bindString(1, item[0]);
			statement.bindString(2, item[1]);
			statement.executeInsert();
		}
		db.setTransactionSuccessful();
		db.endTransaction();
	}

	// 删除一条数据
	public void deleteAutotextItem(String table, int id) {
		// TODO Auto-generated method stub
		String sql = "delete from " + table + " where id = "
				+ String.valueOf(id);
		db.execSQL(sql);
	}

	//////////////////////////////////////////////////////////////////////////////
	//用于输入法中的查询与替换
	public String searchAutotext(String table,
			String input) {
		//input.toLowerCase();
		input = ConstantList.escape(input);
		String result;
		String sql = "select autotext from " + table + " where input = '" + input + "' order by id limit 1";
		//Log.d("Here", sql + "|");
		Cursor cursor = db.rawQuery(sql, null);
		if(cursor.getCount() == 0){//如果没有找到，则返回一个空的SpannableStringBuilder对象
			result = null;
		}else{//如果找到了，那么就返回对应的autotext的SpannableStringBuilder对象
			cursor.moveToNext();
			result = ConstantList.recover(cursor.getString(cursor.getColumnIndex("autotext")));
		}		
		cursor.close();
		return result;
	}
	
	///////////////////////////////////////////////////////////////////////////////
	//用于取得最长的input列的长度
	public int getMaxInputLength(int methodId){
		String sql = "select max(length(input)) from autotext" + String.valueOf(methodId);
		Cursor cursor = db.rawQuery(sql, null);
		cursor.moveToNext();
		int max = cursor.getInt(0);
		cursor.close();
		return max;
	}

}
