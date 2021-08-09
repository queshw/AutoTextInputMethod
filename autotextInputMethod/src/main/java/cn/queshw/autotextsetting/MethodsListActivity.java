package cn.queshw.autotextsetting;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Switch;

import java.util.ArrayList;

import cn.queshw.autotextinputmethod.R;

public class MethodsListActivity extends Activity {
	public final static int ADD = -1;// 新装记录
	private ListView methodsListview;
	private ArrayList<MethodItem> methodsItemList;
	private MethodsAdapter adapter;
	private DBOperations dboper;

	// 为左划出的菜单设置的变量
	private RelativeLayout slideMenu;
	private ImageView editImageView;
	private ImageView deleteImageView;
	private ImageView isdefaultImageView;
	private int position;// 点击的是listview中的哪个图条目
	private Animation animation;
	private View view;// 要使用动画的视图，这里是一条list条目
	float downX;
	float upX;

	// for AlertDialog view
	String name;
	int isDefault = MethodItem.NOTDEFAULT;
	EditText ed;
	Switch sw;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.methods_list);

		dboper = new DBOperations(this);
		animation = AnimationUtils.loadAnimation(this, R.anim.push_out);// 创建动画对象
		methodsListview = (ListView) findViewById(R.id.methods_listview_activity_main);

		methodsItemList = dboper.loadMethodsData();
		this.registerForContextMenu(methodsListview);
		adapter = new MethodsAdapter(this, R.layout.methoditem, methodsItemList);
		methodsListview.setAdapter(adapter);
		methodsListview.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int position, long arg3) {
				// 
				RawActivity.actionStart(MethodsListActivity.this, methodsItemList.get(position).getId());
				// Log.d("Here", "Position=" + String.valueOf(position) +
				// " clicked");
			}
		});

		// 设置listview的触摸事件
		methodsListview.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// 
				// 确定点到哪条了
				position = methodsListview.pointToPosition((int) event.getX(), (int) event.getY());
				if (position == AdapterView.INVALID_POSITION) {// 如果点击的是无效位置
					if (slideMenu != null) {// 并且左划菜单已经显示
						slideMenu.setVisibility(View.GONE);
						slideMenu = null;
					}
					return false;
				}
				// 如果是有效位置
				view = methodsListview.getChildAt(position - methodsListview.getFirstVisiblePosition());// 根据位置获得要操作的视图
				RelativeLayout tempslideMenu = (RelativeLayout) view.findViewById(R.id.slidemenu_linearlayout_methoditem);
				isdefaultImageView = (ImageView) view.findViewById(R.id.isdefalut_imageview_methoditem);// 根据ID获得相应的左划菜单上的项目，为以后设置点击监听器作准备
				editImageView = (ImageView) view.findViewById(R.id.edit_imageview_methoditem);
				deleteImageView = (ImageView) view.findViewById(R.id.delete_imageview_methoditem);

				// 接下来处理触摸事件
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					downX = event.getX();
					break;
				case MotionEvent.ACTION_UP:
					upX = event.getX();
					// 如果左划菜单已经显示，那么就隐藏，同时消耗掉事件
					if (slideMenu != null) {
						slideMenu.setVisibility(View.GONE);
						slideMenu = null;
						return true;
					} else {// 如果左划菜单还没有显示
						if (Math.abs(downX - upX) > 35) {// 如果向左划动距离大于35dp，则显示左划菜单
							tempslideMenu.setVisibility(View.VISIBLE);
							slideMenu = tempslideMenu;
							// 接下来设置各个菜单项的点击监听事件
							// “设为默认”的监听器
							isdefaultImageView.setOnClickListener(new View.OnClickListener() {
								@Override
								public void onClick(View v) {
									// 
									dboper.addOrUpdateMethodItem(methodsItemList.get(position).getName(), MethodItem.DEFAULT,
											methodsItemList.get(position).getId());
									slideMenu.setVisibility(View.GONE);
									slideMenu = null;
									refresh();
								}
							});
							// “修改”的监听器
							editImageView.setOnClickListener(new View.OnClickListener() {
								@Override
								public void onClick(View v) {
									// 
									addOrEdit(methodsItemList.get(position).getId());
									slideMenu.setVisibility(View.GONE);
									slideMenu = null;
									refresh();
								}
							});
							// “删除”的监听器
							deleteImageView.setOnClickListener(new View.OnClickListener() {
								@Override
								public void onClick(View v) {
									// 
									view.startAnimation(animation);
									animation.setAnimationListener(new AnimationListener() {
										@Override
										public void onAnimationEnd(Animation animation) {
											// Auto-generated
											// method stub
											dboper.deleteMethodItem("methods", methodsItemList.get(position).getId());
											refresh();
											slideMenu.setVisibility(View.GONE);
											slideMenu = null;
										}

										@Override
										public void onAnimationRepeat(Animation animation) {
											// Auto-generated
											// method stub
										}

										@Override
										public void onAnimationStart(Animation animation) {
											// Auto-generated
											// method stub

										}
									});
								}
							});
							return true;
						}
					}
				}
				return false;
			}

		});

	}

	@Override
	protected void onResume() {
		// 
		super.onResume();
		refresh();
	}

	// 用于刷新列表
	private void refresh() {
		// 
		// Log.d("Here", "refresh");
		methodsItemList.clear();
		for (MethodItem item : dboper.loadMethodsData()) {
			methodsItemList.add(item);
		}
		adapter.notifyDataSetChanged();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_methodsactivity, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// 
		switch (item.getItemId()) {
		case R.id.add_menu_methodactivity:
			addOrEdit(ADD);
			break;
		case R.id.loaddefault_menu_methodactivity:
			loadDefault();
			break;
		case R.id.help_menu_methodactivity:
			help();
			break;
		}
		return true;
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		// 
		switch (keyCode) {
		case KeyEvent.KEYCODE_C:
			addOrEdit(ADD);
			break;
		case KeyEvent.KEYCODE_L:
			loadDefault();
			break;
		case KeyEvent.KEYCODE_H:
			help();
			break;
		default:
			return super.onKeyUp(keyCode, event);
		}
		return true;
	}

	// 菜单项方法
	//help菜单项
	private void help() {
		Intent intent = new Intent(this, HelpActivity.class);
		startActivity(intent);
	}
	
	//导入自带词库的菜单项
	private void loadDefault() {
		try {
			final AssetManager assetManager = this.getAssets();
			final String[] list = assetManager.list("dicts");
			StringBuilder s = new StringBuilder();
			for (int i = 0; i < list.length; i++) {
				if(list[i].equals("01wubi_pinyin")){
					s.append("1、" + this.getString(R.string.wubi_pinyin) + "\n");
				}else if(list[i].equals("02yinwen")){
					s.append("2、" + this.getString(R.string.yinwen) + "\n");
				}
				else if(list[i].equals("03code_search")){
					s.append("3、" + this.getString(R.string.code_search) + "\n");
				}else{
					s.append(list[i] + "\n");
				}				
			}

			AlertDialog dialog = new AlertDialog.Builder(this).setTitle(getString(R.string.importdefault)).setMessage(s)
					.setNegativeButton(R.string.no, null).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							ImportDefaultActivity.startAction(MethodsListActivity.this);
						}
					}).show();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	

	
	//添加或修改词库的菜单项
	private void addOrEdit(final int id) {
		View view = this.getLayoutInflater().inflate(R.layout.add_or_edit_method, null);
		ed = (EditText) view.findViewById(R.id.name_add_or_edit_method);
		sw = (Switch) view.findViewById(R.id.isdefault_add_or_edit_method);

		if (id != ADD) {
			MethodItem item = dboper.getMethodItem(id);
			ed.setText(item.getName());
			isDefault = item.getIsDefault();
			if (item.getIsDefault() == MethodItem.DEFAULT) {
				sw.setChecked(true);
			} else {
				sw.setChecked(false);
			}
		}

		sw.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				// 
				isDefault = isChecked ? MethodItem.DEFAULT : MethodItem.NOTDEFAULT;
			}
		});

		AlertDialog dialog = new AlertDialog.Builder(this).setTitle(getString(R.string.title)).setView(view).setCancelable(true)
				.setOnCancelListener(new OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						// 
						isDefault = MethodItem.NOTDEFAULT;
						refresh();
						// Log.d("Here", "cancel isDefault=" +
						// String.valueOf(isDefault));
					}
				})

				.setNegativeButton(R.string.cancel, new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// 
						dialog.dismiss();
						isDefault = MethodItem.NOTDEFAULT;
						refresh();
						// Log.d("Here", "Dismiss isDefault=" +
						// String.valueOf(isDefault));
					}
				})

				.setPositiveButton(R.string.save, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// 
						name = ed.getText().toString();
						dboper.addOrUpdateMethodItem(name, isDefault, id);
						isDefault = MethodItem.NOTDEFAULT;
						refresh();
					}
				}).create();
		dialog.show();
	}
}
