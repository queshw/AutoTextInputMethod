package cn.queshw.autotextsetting;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import cn.queshw.autotextinputmethod.R;

public class ImportDefaultActivity extends Activity {
	private TextView filenameTextview;
	private TextView statusTextview;
	private ProgressBar importProgressBar;
	private Handler handler;

	private DBOperations dboper;// 用于数据库操作

	String[] list;// 用于存放assets/dicts/目录下需要导入的文件的列表
	String[] fileName;//用于存放assets/dicts/目录下文件对应的中文输入法名称
	AssetManager assetManager;
	int lines = 0;

	static void startAction(Context context) {
		Intent intent = new Intent(context, ImportDefaultActivity.class);
		context.startActivity(intent);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// 
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.importdefault);

		dboper = new DBOperations(this);

		filenameTextview = (TextView) findViewById(R.id.filename_textview_layout_importdefault);
		importProgressBar = (ProgressBar) findViewById(R.id.import_progressbar_layout_importdefault);
		statusTextview = (TextView) findViewById(R.id.status_textview_layout_importdefault);

		assetManager = ImportDefaultActivity.this.getAssets();
		try {
			list = assetManager.list("dicts");
			fileName = new String[list.length];
			for (int i = 0; i < list.length; i++) {
				if(list[i].equals("01wubi_pinyin")){
					fileName[i] = this.getString(R.string.wubi_pinyin);
				}else if(list[i].equals("02yinwen")){
					fileName[i] = this.getString(R.string.yinwen);
				}
				else if(list[i].equals("03code_search")){
					fileName[i] = this.getString(R.string.code_search);
				}
				else{
					fileName[i] = list[i];
				}
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		handler = new Handler() {			
			public void handleMessage(Message msg) {
				// 				
				if (msg.what != -1) {
					filenameTextview.setText(fileName[msg.what]);
					lines = msg.arg1;
					//Log.d("Here", list[msg.what] + "|" + String.valueOf(lines));
				} else {
					//Log.d("Here", "|" + String.valueOf(msg.arg2));
					importProgressBar.setProgress(msg.arg2 * 100 / lines );
					statusTextview.setText(String.valueOf(msg.arg2) + "/" + String.valueOf(lines));
				}
			}
		};

		new Thread(new Runnable() {
			@Override
			public void run() {
				// 
				try {
					for (int i = 0; i < list.length; i++) {
						// 取得文件的行数
						InputStream is = assetManager.open("dicts/" + list[i]);
						BufferedReader br = new BufferedReader(new InputStreamReader(is));
						br.mark(1000000);
						int lines = 0;
						while (br.readLine() != null) {
							lines++;
						}
						//Log.d("Here", list[i] + "|" + String.valueOf(lines));

						// 通知主进程更新导入状态
						Message msg = Message.obtain();
						msg.what = i;
						msg.arg1 = lines;
						handler.sendMessage(msg);

						// 开始导入，并显示进度
						int count = 0;
						String line;
						br.reset();
						//写入methods条目
						int id = dboper.addOrUpdateMethodItem(fileName[i], MethodItem.NOTDEFAULT, MethodsListActivity.ADD);
						//接下来构造用于导入raw表的数组
						ArrayList<String[]> data = new ArrayList<String[]>();
						int twolevel = 0;//用于标识二级替换的项
						boolean isTwoLevel = false;//用于标识当前是否处于二级替换块内
						while ((line = br.readLine()) != null) {
							line = line.trim();
							count++;
							
							if(TextUtils.isEmpty(line)) continue;//如果为空行则跳过
							if(line.equals("[twolevel]")){
								//二级替换块开始
								isTwoLevel = true;
								twolevel--;
								continue;
							}
							else if(line.equals("[/twolevel]")){
								//二级替换块结束
								isTwoLevel = false;
								continue;
							}
							
							String[] item = new String[3];
							item[0] = line.substring(0, line.indexOf(','));
							item[1] = line.substring(line.indexOf(',') + 1);
							if(isTwoLevel == true) item[2] = String.valueOf(twolevel);
							else item[2] = String.valueOf(0);
							if(TextUtils.isEmpty(item[0]) || TextUtils.isEmpty(item[1])) continue;//如果有一个空的，则是非常的替换项，跳过
							
							data.add(item);	
							if(count % 500 == 0) {
								Message msg2 = Message.obtain();
								msg2.what = -1;
								msg2.arg2 = count;
								handler.sendMessage(msg2);
							}
						}
						dboper.importData(id, data);
						br.close();
					}
					ImportDefaultActivity.this.finish();
				} catch (Exception e) {
					e.printStackTrace();
				}
				
			}
		}).start();

	}
}
