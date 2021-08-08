package cn.queshw.autotextsetting;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.text.TextUtils;

import java.util.ArrayList;

import cn.queshw.autotextinputmethod.ConstantList;

public class DBOperations {
	public static final int NOOFFSET = -1;// 如果是提取全部数据
	public static final int NOLIMIt = -1;// 如果是提取全部数据

	private DBHelper helper;
	private SQLiteDatabase db;
	private GenAutotext ga = new GenAutotext();

	// ////////////////////////////////////////////////////
	// 构造函数
	public DBOperations(Context context) {
		helper = new DBHelper(context, "methods.db", null, 2);
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
		Cursor cursor = db.rawQuery("select * from methods where id = ?", new String[] { String.valueOf(id) });
		cursor.moveToNext();
		MethodItem item = constructMethodItem(cursor);
		cursor.close();
		return item;
	}

	// 根据一条记录构造一个对象
	private MethodItem constructMethodItem(Cursor cursor) {
		MethodItem item = new MethodItem();
		item.setId(cursor.getInt(cursor.getColumnIndex("id")));
		item.setName(cursor.getString(cursor.getColumnIndex("name")));
		item.setIsDefault(cursor.getInt(cursor.getColumnIndex("isDefault")));
		return item;
	}

	// ////////////////////////////////////////////////////////

	// ////////////////////////////////////////////////////////
	// 添加一条记录，返回id号
	public int addOrUpdateMethodItem(String name, int isDefault, int id) {
		name = ConstantList.insertUpdateEscape(name);
		ContentValues values = new ContentValues();
		values.put("name", name);
		values.put("isDefault", isDefault);

		if (!TextUtils.isEmpty(name)) {// 如果不为空
			Cursor cursor = db.rawQuery("select isDefault from methods where id = ?", new String[] { String.valueOf(id) });

			if (cursor.getCount() == 0) {// 如果原来没有此条记录，则新增记录
				if (isDefault == MethodItem.DEFAULT) {
					db.execSQL("update methods set isDefault = ?", new String[] { String.valueOf(MethodItem.NOTDEFAULT) });
				}
				id = (int) db.insert("methods", null, values);

				// 创建与新增输入法的相连的词库表
				String rawTableName = "raw" + String.valueOf(id);
				String autotextTableName = "autotext" + String.valueOf(id);
				// 1、创建raw表
				String sql = "create table " + rawTableName + "(" + "id integer primary key autoincrement," + "code text not null,"
						+ "candidate text not null," + "twolevel int default 0)";
				db.execSQL(sql);

				// 2 ，创建autotext表的结构
				sql = "create table " + autotextTableName + "(id integer primary key autoincrement," + "input text not null,"
						+ "autotext text not null," + "rawid integer default 0)";
				db.execSQL(sql);
			} else {// 如果原来已经有这条记录，则修改记录
				if (isDefault == MethodItem.DEFAULT) {// 先把原有默认输入法的设置清空
					db.execSQL("update methods set isDefault = ?", new String[] { String.valueOf(MethodItem.NOTDEFAULT) });
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
		Cursor cursor = db.rawQuery("select isDefault from methods where id = ?", new String[] { String.valueOf(id) });
		cursor.moveToNext();
		// int isDefault = cursor.getInt(cursor.getColumnIndex("isDefault"));//
		// 看看要删除的记录是否是默认的输入法

		db.delete("methods", "id=?", new String[] { String.valueOf(id) });
		String rawTableName = "raw" + String.valueOf(id);
		String autotextTableName = "autotext" + String.valueOf(id);

		// 接下来删除对应的raw表，和autotext表
		String sql = "drop table if exists " + rawTableName;
		db.execSQL(sql);

		sql = "drop table if exists " + autotextTableName;
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
	// 操作raw的系列表
	// 根据相关参数提取记录
	public ArrayList<RawItem> searchRawItems(String table, String searchText, int limit, int offset) {
		// searchText.toLowerCase();
		searchText = ConstantList.searchEscape(searchText);
		ArrayList<RawItem> data = new ArrayList<RawItem>();
		// String sql = "select * from " + table + " where input like '" +
		// searchText + "%' order by input limit " + String.valueOf(limit) +
		// " offset "
		// + String.valueOf(offset);
		String sql;
		if (searchText.equals("twolevel"))
			sql = "select * from " + table + " where twolevel < 0 order by twolevel,code";
		else
			sql = "select * from " + table + " where code like '" + searchText + "%' escape '/' order by code limit " + String.valueOf(limit) + " offset "
					+ String.valueOf(offset);
		Cursor cursor = db.rawQuery(sql, null);
		while (cursor.moveToNext()) {
			RawItem item;
			item = constructRawItem(cursor);
			data.add(item);
		}
		cursor.close();
		return data;
	}

	// 根据id提取单条记录
	public RawItem getRawItem(String table, int id) {
		RawItem item;
		String sql = "select * from " + table + " where id = " + String.valueOf(id);
		Cursor cursor = db.rawQuery(sql, null);
		cursor.moveToNext();
		item = constructRawItem(cursor);
		cursor.close();
		return item;
	}

	// 构造rawitem
	private RawItem constructRawItem(Cursor cursor) {
		RawItem item = new RawItem();
		item.setId(cursor.getInt(cursor.getColumnIndex("id")));
		item.setCode(cursor.getString(cursor.getColumnIndex("code")));
		item.setCandidate(cursor.getString(cursor.getColumnIndex("candidate")));
		item.setTwolevel(cursor.getInt(cursor.getColumnIndex("twolevel")));
		return item;
	}

	// 添加或者修改单条数据
	public void addOrSaveRawItem(int methodId, String code, String candidate, int id) {
		String rawTableName = "raw" + String.valueOf(methodId);

		code = ConstantList.insertUpdateEscape(code);
		candidate = ConstantList.insertUpdateEscape(candidate);

		if (!TextUtils.isEmpty(code) && !TextUtils.isEmpty(candidate)) {// 如果有一个为空，则什么都不干
			// 先判断相应id号的记录是否存在，以此来确定是新增记录还是修改记录
			String sql = "select id from " + rawTableName + " where id = " + String.valueOf(id);
			Cursor cursor = db.rawQuery(sql, null);

			if (cursor.getCount() == 0) {// 说明为新增记录
				ContentValues cv = new ContentValues();
				cv.put("code", code);
				cv.put("candidate", candidate);
				cv.put("twolevel", 0);
				//int tempId = (int) db.insert(rawTableName, null, cv);
				sql = "insert into " + rawTableName + " values(null, '" + code + "', '" + candidate + "', 0)";

				db.execSQL(sql);
				Cursor cursorForRowid = db.rawQuery("select last_insert_rowid() from " + rawTableName,null);
				int tempId;
				cursorForRowid.moveToFirst();
				tempId= cursorForRowid.getInt(0);
				cursorForRowid.close();
				// 接下来应该更新对应的autotext表中的记录
				regenAutotext(methodId, tempId);
			} else {// 说明为修改原有记录
				sql = "update " + rawTableName + " set code = '" + code + "', candidate='" + candidate + "' where id = " + String.valueOf(id);
				db.execSQL(sql);
				RawItem item = getRawItem(rawTableName, id);
				// 接下来应该更新对应的autotext表中的记录
				if (item.getTwolevel() < 0) {// 是二级替换项目
					regenAutotext(methodId, item.getTwolevel());
				} else {// 不是二级替换项目
					regenAutotext(methodId, item.getId());
				}
			}

			cursor.close();
		}
	}

	private void regenAutotext(int methodId, int id) {
		// 注意如果是二级替换项目，id应该传入raw表中的twolevel号，如果不是则直接入raw表中的id号即可
		String rawTableName = "raw" + String.valueOf(methodId);
		String autotextTableName = "autotext" + String.valueOf(methodId);
		String sql;

		// 先删除现有的对应的项目
		sql = "delete from " + autotextTableName + " where rawid=" + String.valueOf(id);
		db.execSQL(sql);

		// 把raw表中对应的数据取出来，生成要插入的autotext的数组
		ArrayList<String> input = new ArrayList<String>();
		ArrayList<String> autotext = new ArrayList<String>();
		ArrayList<Integer> rawid = new ArrayList<Integer>();

		ArrayList<String> tempInput = new ArrayList<String>();
		ArrayList<String> tempAutotext = new ArrayList<String>();
		if (id < 0) {// 需要更新的为二级替换项
			sql = "select * from " + rawTableName + " where twolevel = " + String.valueOf(id);
		} else {// 需要更新的不是二级替换项
			sql = "select * from " + rawTableName + " where id = " + String.valueOf(id);
		}
		Cursor cursor = db.rawQuery(sql, null);
		while (cursor.moveToNext()) {
			tempInput.clear();
			tempAutotext.clear();
			ga.gen(ConstantList.commaPercentEscape(cursor.getString(cursor.getColumnIndex("code"))) + "," + cursor.getString(cursor.getColumnIndex("candidate")));
			tempInput = ga.getInputList();
			tempAutotext = ga.getAutotextList();
			// 处理二级替换项的问题
			if (cursor.getInt(cursor.getColumnIndex("twolevel")) < 0) {// 如果当前行为二级替换项目
				// 查看这一组二级替换中，谁是第一行
				Cursor tempCursor = db.rawQuery("select min(id) from " + rawTableName + " where twolevel=?",
						new String[] { String.valueOf(cursor.getInt(cursor.getColumnIndex("twolevel"))) });
				tempCursor.moveToNext();

				if (cursor.getInt(cursor.getColumnIndex("id")) == tempCursor.getInt(0)) {// 当前为此组二级替换的第一行
					for (int i = 0; i < tempInput.size(); i++) {
						input.add(tempInput.get(i));
						autotext.add(tempAutotext.get(i));
						rawid.add(cursor.getInt(cursor.getColumnIndex("twolevel")));
					}
				} else {// 当前为此组二级替换的其他行
					int j = autotext.lastIndexOf("%b" + tempInput.get(0));
					if (j == -1) {// 如果没有找到
						input.add(tempInput.get(0));
						autotext.add(tempAutotext.get(0));
						rawid.add(cursor.getInt(cursor.getColumnIndex("twolevel")));
					} else {// 如果找到了
						autotext.set(j, tempAutotext.get(0));
					}
					for (int i = 1; i < tempInput.size(); i++) {
						input.add(tempInput.get(i));
						autotext.add(tempAutotext.get(i));
						rawid.add(cursor.getInt(cursor.getColumnIndex("twolevel")));
					}
				}
				tempCursor.close();
			} else {// 此行不是二级替换项目
				for (int i = 0; i < tempInput.size(); i++) {
					input.add(tempInput.get(i));
					autotext.add(tempAutotext.get(i));
					rawid.add(cursor.getInt(cursor.getColumnIndex("id")));
				}
			}
		}
		cursor.close();// 至此已经生成用于导入autotext表的数组

		// 2、批量导入autotext表中
		sql = "insert into " + autotextTableName + " values(null, ?, ?, ?)";
		SQLiteStatement statement = db.compileStatement(sql);

		db.beginTransaction();
		for (int i = 0; i < input.size(); i++) {
			statement.bindString(1, ConstantList.commaPercentRecover(input.get(i)));
			statement.bindString(2, ConstantList.commaPercentRecover(autotext.get(i)));
			statement.bindLong(3, rawid.get(i));
			statement.executeInsert();
		}
		db.setTransactionSuccessful();
		db.endTransaction();
	}

	// 用于批量导入数据
	// 此版本效率大提升
	public void importData(int methodId, ArrayList<String[]> data) {
		String rawTableName = "raw" + String.valueOf(methodId);
		String autotextTableName = "autotext" + String.valueOf(methodId);
		// 查询raw表中，最小的twolevel值，用于后面计算接下来要用的twolevel值
		int preTwolevel = 0;
		Cursor cursor = db.rawQuery("select min(twolevel) from " + rawTableName, null);
		if (cursor.getCount() != 0) {
			cursor.moveToNext();
			preTwolevel = cursor.getInt(0);
			cursor.close();
		}


		String sql = "insert into " + rawTableName + " values(null, ?, ?, ?)";
		SQLiteStatement statement = db.compileStatement(sql);

		db.beginTransaction();
		for (String[] item : data) {
			statement.bindString(1, ConstantList.commaPercentRecover(item[0]));
			statement.bindString(2, item[1]);
			if (Integer.parseInt(item[2]) < 0)
				statement.bindLong(3, Integer.parseInt(item[2]) + preTwolevel);
			else
				statement.bindLong(3, 0);
			statement.executeInsert();
		}
		db.setTransactionSuccessful();
		db.endTransaction();

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
			ga.gen(ConstantList.commaPercentEscape(cursor.getString(cursor.getColumnIndex("code"))) + "," + cursor.getString(cursor.getColumnIndex("candidate")));
			tempInput = ga.getInputList();
			tempAutotext = ga.getAutotextList();
			// 处理二级替换项的问题
			if (cursor.getInt(cursor.getColumnIndex("twolevel")) < 0) {// 如果当前行为二级替换项目
				// 查看这一组二级替换中，谁是第一行
				Cursor tempCursor = db.rawQuery("select min(id) from " + rawTableName + " where twolevel=?",
						new String[] { String.valueOf(cursor.getInt(cursor.getColumnIndex("twolevel"))) });
				tempCursor.moveToNext();

				if (cursor.getInt(cursor.getColumnIndex("id")) == tempCursor.getInt(0)) {// 当前为此组二级替换的第一行
					for (int i = 0; i < tempInput.size(); i++) {
						input.add(tempInput.get(i));
						autotext.add(tempAutotext.get(i));
						rawid.add(cursor.getInt(cursor.getColumnIndex("twolevel")));
					}
				} else {// 当前为此组二级替换的其他行
					int j = -1;
					for (int i = 0; i < autotext.size(); i++) {
						if (rawid.get(i) == cursor.getInt(cursor.getColumnIndex("twolevel")) && autotext.get(i).equals("%b" + tempInput.get(0))) {
							j = i;
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
					for (int i = 1; i < tempInput.size(); i++) {
						input.add(tempInput.get(i));
						autotext.add(tempAutotext.get(i));
						rawid.add(cursor.getInt(cursor.getColumnIndex("twolevel")));
					}
				}
				tempCursor.close();
			} else {// 此行不是二级替换项目
				for (int i = 0; i < tempInput.size(); i++) {
					input.add(tempInput.get(i));
					autotext.add(tempAutotext.get(i));
					rawid.add(cursor.getInt(cursor.getColumnIndex("id")));
				}
			}
		}
		cursor.close();// 至此已经生成用于导入autotext表的数组

		// 2、批量导入autotext表中
		sql = "insert into " + autotextTableName + " values(null, ?, ?, ?)";
		statement = db.compileStatement(sql);

		db.beginTransaction();
		for (int i = 0; i < input.size(); i++) {
			statement.bindString(1, ConstantList.commaPercentRecover(input.get(i)));
			statement.bindString(2, ConstantList.commaPercentRecover(autotext.get(i)));
			statement.bindLong(3, rawid.get(i));
			statement.executeInsert();
		}
		db.setTransactionSuccessful();
		db.endTransaction();
	}

	public ArrayList<RawItem> exportData(int methodId) {
		String rawTableName = "raw" + String.valueOf(methodId);
		ArrayList<RawItem> data = new ArrayList<RawItem>();
		// 先读出不是二级替换的条目
		String sql = "select * from " + rawTableName + " where twolevel=0 order by id";
		Cursor cursor = db.rawQuery(sql, null);
		while (cursor.moveToNext()) {
			RawItem item;
			item = constructRawItem(cursor);
			data.add(item);
		}
		cursor.close();

		// 再读出二级替换的条目
		sql = "select * from " + rawTableName + " where twolevel<0 order by id,twolevel desc";
		cursor = db.rawQuery(sql, null);
		while (cursor.moveToNext()) {
			RawItem item;
			item = constructRawItem(cursor);
			data.add(item);
		}
		cursor.close();

		return data;
	}

	// 删除一条数据
	public void deleteRawItem(int methodId, RawItem item) {
		String rawTable = "raw" + String.valueOf(methodId);
		String sql = "delete from " + rawTable + " where id = " + String.valueOf(item.getId());
		db.execSQL(sql);
		// 接下来应该更新autotext表中的对应数据
		if (item.getTwolevel() < 0) {// 是二级替换项目
			regenAutotext(methodId, item.getTwolevel());
		} else {// 不是二级替换项目
			regenAutotext(methodId, item.getId());
		}
	}



	// ////////////////////////////////////////////////////////////////////////////
	// 用于输入法中的查询与替换
	public String searchAutotext(String table, String input) {
		// input.toLowerCase();
		input = ConstantList.searchEscape(input);
		String result;
		db.execSQL("PRAGMA case_sensitive_like=ON");//设置成大小写敏感
		String sql = "select autotext from " + table + " where input like '" + input + "' escape '/' order by id limit 1";
		Cursor cursor = db.rawQuery(sql, null);
		if (cursor.getCount() == 0) {// 如果没有找到，则返回一个空的SpannableStringBuilder对象
			result = null;
		} else {// 如果找到了，那么就返回对应的autotext的SpannableStringBuilder对象
			cursor.moveToNext();
			result = cursor.getString(cursor.getColumnIndex("autotext"));
		}
		cursor.close();
		return result;
	}

	// /////////////////////////////////////////////////////////////////////////////
	// 用于取得最长的input列的长度
	public int getMaxInputLength(int methodId) {
		String sql = "select max(length(input)) from autotext" + String.valueOf(methodId);
		Cursor cursor = db.rawQuery(sql, null);
		cursor.moveToNext();
		int max = cursor.getInt(0);
		cursor.close();
		return max;
	}

}
