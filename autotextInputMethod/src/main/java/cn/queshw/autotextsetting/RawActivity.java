package cn.queshw.autotextsetting;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;

import cn.queshw.autotextinputmethod.ConstantList;
import cn.queshw.autotextinputmethod.R;

public class RawActivity extends Activity {
	public static final int LIMIT = 50;// 一次提取5０条记录
	private final int LOADED = -1;// 下一个50条的数据已经加载
	private final int NOTLOAD = -2;// 下一个50条的数据没有加载
	private final int IMPORTED = -3;// 导入完成
	private final int EXPORTED = -4;// 导出完成

	private Handler handler;// 用于数据异步加载
	private int loadtag = NOTLOAD;// 下一个50条数据是否已经加载的标记，默认为没有加载

	private int offset = 0;// sql操作的初始偏移量
	private int methodId;// 传过来的method的id

	private ArrayList<RawItem> listdata;// 用于产生listview的数据
	private RawAdapter adapter;// listview 用的适配器
	private ListView rawListview;// listview用于放置autotext条目的
	private int totalItems = 0;// 列表的数据中总共有几条数据，用于异步数据加载

	private EditText searchEditText;// EditText
	private String searchText = "";// 输入框的内容是什么

	private ImageView deleteIcon;// 删除的图标
	private float downX;// ontouch事件的x
	private float upX;// ontouch事件的x
	private int position;// 用于设置删除图标的监听器时用的
	private Animation animation;// 删除动画
	private View view;// 要删除的视图，这里是一个list条目

	private DBOperations dboper;// 用于数据库操作
	private String table;// 要操作的表的名称

	// alertdialog中视图的元素，分别用于接收input和autotext数据
	private EditText codeEditText;
	private EditText candidateEditText;
	private final int ADD = -1;// 代替AutotextItem的id，用于表示是新增的条目

	// 用于导入导出数据
	File resultFile;// 用于导入与导出数据
	int lines = 0;// 已经导入导出数据有多少条
	TextView statusTextView;// 用于显示导入导出状态

	// 给别人用的启动本activity的方法
	public static void actionStart(Context context, int methodId) {
		Intent intent = new Intent(context, RawActivity.class);
		intent.putExtra("methodId", methodId);
		context.startActivity(intent);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.layout_rawactivity);

		// 从Intent中获得methodId，并构建要操作的表的名称
		Intent intent = getIntent();
		methodId = intent.getIntExtra("methodId", -1);
		//table = "autotext" + String.valueOf(methodId);
		table = "raw" + String.valueOf(methodId);
		animation = AnimationUtils.loadAnimation(this, R.anim.push_out);

		// 从表中提取记录，为listview做准备，一次提取３０条记录，然后显示出来
		dboper = new DBOperations(this);
		listdata = dboper.searchRawItems(table, "", LIMIT, offset);
		adapter = new RawAdapter(this, R.layout.rawitem, listdata);
		rawListview = (ListView) findViewById(R.id.raws_listview_layout_rawactivity);
		rawListview.setAdapter(adapter);
		rawListview.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1,
					int position, long arg3) {
				// TODO Auto-generated method stub
				addOrEdit(listdata.get(position).getId());
			}

		});
		// 为listview设置触摸监听器，向左划出现删除图标，点击后删除相应的条目
		rawListview.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				// 看看点击的是哪个条目
				position = rawListview.pointToPosition((int) event.getX(),
						(int) event.getY());
				if (position == AdapterView.INVALID_POSITION) {// 如果是无效位置
					if (deleteIcon != null) {// 如果已经显示了删除图标
						deleteIcon.setVisibility(View.GONE);
					}
					return false;
				}
				// 如果是有效的位置，那么就获得相应的条目的View，并过一步获得删除图标，用于后面设置点击监听器
				view = rawListview.getChildAt(position
						- rawListview.getFirstVisiblePosition());
				ImageView tempDeleteIcon = (ImageView) view
						.findViewById(R.id.delete_imageview_rawitem);
				// 处理触摸事件
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					downX = event.getX();
					break;
				case MotionEvent.ACTION_UP:
					upX = event.getX();
					if (deleteIcon != null) {// 如果delete图标已经显示，则隐藏它，同时不传送touch事件
						deleteIcon.setVisibility(View.GONE);
						deleteIcon = null;
						return true;
					} else {// 如果删除图标没有显示
						if (Math.abs(downX - upX) > 80) {// 如果delete图标没有显示，并且如果向左划动距离大于35dp，则显示delete图标
							tempDeleteIcon.setVisibility(View.VISIBLE);
							deleteIcon = tempDeleteIcon;
							deleteIcon
									.setOnClickListener(new View.OnClickListener() {
										@Override
										public void onClick(View arg0) {
											// TODO Auto-generated method stub
											view.startAnimation(animation);// 设置动画
											animation
													.setAnimationListener(new AnimationListener() {
														@Override
														public void onAnimationEnd(
																Animation animation) {
															// TODO
															// Auto-generated
															// method stub
															dboper.deleteRawItem(
																	methodId,
																	listdata.get(
																			position));
															refreshListView();
															deleteIcon
																	.setVisibility(View.GONE);
															deleteIcon = null;
														}

														@Override
														public void onAnimationRepeat(
																Animation animation) {
															// TODO
															// Auto-generated
															// method stub
														}

														@Override
														public void onAnimationStart(
																Animation animation) {
															// TODO
															// Auto-generated
															// method stub
														}
													});

										}
									});
							return true;
						}
					}
					return false;
				}
				return false;
			}
		});

		// 设置列表的滚动监听器，用于异步加载数据
		handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				// TODO Auto-generated method stub
				switch (msg.what) {
				case LOADED:// 如果数据已经加载完了
					adapter.notifyDataSetChanged();
					// Toast.makeText(AutotextActivity.this,
					// AutotextActivity.this.getString(R.string.loaded),
					// Toast.LENGTH_SHORT).show();
					break;
				case IMPORTED:
					refreshListView();
					Toast.makeText(RawActivity.this,
							RawActivity.this.getString(R.string.imported),
							Toast.LENGTH_LONG).show();
					statusTextView.setVisibility(View.GONE);
					break;
				case EXPORTED:
					Toast.makeText(RawActivity.this,
							RawActivity.this.getString(R.string.exported),
							Toast.LENGTH_LONG).show();
					statusTextView.setVisibility(View.GONE);
					break;
				case 1:
					statusTextView.setVisibility(View.VISIBLE);
				default:
					statusTextView.setText(String.valueOf(msg.what));
					break;
				}
				// 这时，数据列表中的总条目如果已经变化了，改变数据是否已经加载的标记，用于下一次加载
				if (totalItems != listdata.size()) {
					loadtag = NOTLOAD;
				}
			}
		};
		rawListview.setOnScrollListener(new OnScrollListener() {
			@Override
			public void onScroll(AbsListView listview, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
				// TODO Auto-generated method stub

				if (listview.getLastVisiblePosition() == totalItemCount - LIMIT
						/ 2
						&& loadtag == NOTLOAD) {
					loadtag = LOADED;
					totalItems = totalItemCount;// 把局部变量传到activity的变量中

					// 开始异步加载数据
					Thread loaddata = new Thread(new Runnable() {
						@Override
						public synchronized void run() {
							// TODO Auto-generated method stub
							ArrayList<RawItem> data = dboper
									.searchRawItems(table, searchText,
											LIMIT, totalItems);
							for (RawItem item : data) {
								listdata.add(item);
							}

							// 数据加载完后，向主线程发送消息
							Message msg = new Message();
							msg.what = LOADED;
							handler.sendMessage(msg);
							// Log.d("Here", "Data loaded");
						}
					});
					loaddata.start();
				}
			}

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				// TODO Auto-generated method stub
			}

		});

		// 给输入框设置按键监听器
		searchEditText = (EditText) findViewById(R.id.search_edittext_layout_rawactivity);		
		searchEditText.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				// TODO Auto-generated method stub
				// 加载数据，刷新列表内容
				offset = 0;
				loadtag = NOTLOAD;
				searchText = s.toString();
				refreshListView();
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				// TODO Auto-generated method stub
			}
		});

		// 获得导入导出状态显示的TextView
		statusTextView = (TextView) findViewById(R.id.status_textview_layout_rawactivity);		
	}

	// ////////////////////////////////////////
	// 刷新列表函数
	private void refreshListView() {
		listdata.clear();
		for (RawItem item : dboper.searchRawItems(table, searchText,
				LIMIT, offset)) {
			listdata.add(item);
		}
		adapter.notifyDataSetChanged();
		rawListview.setSelection(0);
	}

	// ///////////////////////////////////////////
	// 菜单处理
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		this.getMenuInflater().inflate(R.menu.menu_rawactivity, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		switch (item.getItemId()) {
		case R.id.add_menu_rawactivity:
			addOrEdit(ADD);
			break;
		case R.id.import_menu_rawactivity:
			startFilePickerActivity(
					String.valueOf(Environment.getExternalStorageDirectory()),
					FilePickerActivity.IMPORT);
			break;
		case R.id.export_menu_rawactivity:
			startFilePickerActivity(
					String.valueOf(Environment.getExternalStorageDirectory()),
					FilePickerActivity.EXPORT);
			break;
		}
		return true;
	}

	// 打开FilePickerActivity的函数，用于菜单项
	private void startFilePickerActivity(String relativeRoot, int purpose) {// 与FilePickerActivity对应，需要两个参数
		Intent intent = new Intent(this, FilePickerActivity.class);
		intent.putExtra("relativeRoot", relativeRoot);
		intent.putExtra("purpose", purpose);
		this.startActivityForResult(intent, purpose);
	}

	// FilePickerActivity返回数据后的处理函数
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		if (resultCode == Activity.RESULT_OK) {
			String result = data.getStringExtra("result");
			// Log.d("Here", "RequestCode=" + String.valueOf(requestCode)
			// + " | resultCode=" + String.valueOf(resultCode)
			// + " | result=" + result);
			resultFile = new File(result);
			if (requestCode == FilePickerActivity.IMPORT) {// 导入操作
				new Thread(new Runnable() {
					@Override
					public void run() {
						// TODO Auto-generated method stub

						FileReader fr;
						ArrayList<String[]> data = new ArrayList<String[]>();
						try {
							fr = new FileReader(resultFile);
							BufferedReader br = new BufferedReader(fr);
							String line = "";
							
							int twolevel = 0;//用于标识二级替换的项
							boolean isTwoLevel = false;//用于标识当前是否处于二级替换块内
							
							while ((line = br.readLine()) != null) {
								line = line.trim();
								
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
								// dboper.importAutotext(table, item[0],
								// item[1]);
								// 通知显示导入进度								
								Message msg = new Message();
								msg.what = ++lines;
								if(lines % 500 == 0) handler.sendMessage(msg);
							}
							br.close();
							dboper.importData(methodId, data);

							// 完成后，通知程序已经完成了
							Message msg = new Message();
							msg.what = IMPORTED;
							handler.sendMessage(msg);

						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}).start();
				//importAutotexts(result);
			} else {// 导出操作
				new Thread(new Runnable() {
					@Override
					public void run() {
						// TODO Auto-generated method stub

						FileWriter fw;
						try {
							fw = new FileWriter(resultFile, false);
							BufferedWriter bw = new BufferedWriter(fw);
							ArrayList<RawItem> tempData = dboper.exportData(methodId);
							int preTwolevel = 0;
							int nextTwolevel = 0;
							RawItem item;
							for (int i = 0; i < tempData.size(); i++) {
								preTwolevel = 0;
								nextTwolevel = 0;
								if(i != 0) preTwolevel = tempData.get(i - 1).getTwolevel();
								if(i != tempData.size() - 1) nextTwolevel = tempData.get(i + 1).getTwolevel();
								item = tempData.get(i);
								
								
								if(item.getTwolevel() < 0 && item.getTwolevel() != preTwolevel) bw.write("[twolevel]\n");
								
								bw.write(ConstantList.commaPercentEscape(item.getCode()) + ","
										+ item.getCandidate() + "\n");
								
								if(item.getTwolevel() < 0 && item.getTwolevel() != nextTwolevel) bw.write("[/twolevel]\n");
								// 通知显示导出进度
								Message msg = new Message();
								msg.what = ++lines;
								if(lines % 500 == 0) handler.sendMessage(msg);
							}
							bw.close();

							// 完成后，通知程序已经完成了
							Message msg = new Message();
							msg.what = EXPORTED;
							handler.sendMessage(msg);

						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}).start();
				//exportAutotexts(result);
			}			
		} else {
			Log.d("Here", "set result cancel!");
		}
	}

	// addOrSave函数，用于处理菜单事件，增加或者修改raw条目
	@SuppressLint("InflateParams")
	private void addOrEdit(final int rawItemId) {
		// 获取对话框要用的view，然后取得view中元素
		View view = this.getLayoutInflater().inflate(
				R.layout.add_or_edit_rawitem, null);
		codeEditText = (EditText) view
				.findViewById(R.id.code_add_or_edit_raw);
		candidateEditText = (EditText) view
				.findViewById(R.id.candidate_add_or_edit_raw);
		if (rawItemId != ADD) {// 如果传入的id号不是ADD，表示这是要修改条目，不是新增的
			RawItem item = dboper.getRawItem(table, rawItemId);
			codeEditText.setText(item.getCode());
			candidateEditText.setText(item.getCandidate());
		}

		// 构建一个AlertDialog，用于用户修改或输入数据
		AlertDialog dialog = new AlertDialog.Builder(this)
				.setTitle(R.string.autotextitem)
				.setView(view)
				.setCancelable(true)
				.setPositiveButton(R.string.save, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						dboper.addOrSaveRawItem(methodId, codeEditText
								.getText().toString(), candidateEditText
								.getText().toString(), rawItemId);
						refreshListView();
					}
				}).setNeutralButton(R.string.comma, null)
				.setNegativeButton(R.string.cancel, null).show();

		// 然后手动获得这个altertdialog的中性按纽，给它设置view.onClickListener，这样的话一按按纽就不会默认关闭对话框了
		dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						candidateEditText.setText(candidateEditText.getText() + "%c %p");
						candidateEditText.setSelection(candidateEditText
								.getText().length());
					}
				});
	}

}
