package cn.queshw.autotextsetting;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import cn.queshw.autotextinputmethod.R;

public class FilePickerActivity extends Activity {
	// 这个活动需要两个参数，一是要浏览的目录，二是要选取的是目录还是文件
	public static final int EXPORT = 0;
	public static final int IMPORT = 1;
	private String relativeRoot = "/";
	private int purpose = IMPORT;
	private String result;// 启动这个活动应该使用startActivityForResult方法，这是结果

	// 布局中的元素
	ListView fileListView;
	ArrayList<File> fileList = new ArrayList<File>();
	FilePickerAdapter fileListAdapter;
	EditText resultEditText;
	Button yesButton;
	Button noButton;
	Button parentButton;
	Spinner pathSpinner;
	ArrayList<String> pathList = new ArrayList<String>();
	ArrayAdapter<String> spinnerAdapter;

	// 要用到的其他变量

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.layout_filepickeractivity);

		// Log.d("Here", "getDataDirectory()=" +
		// Environment.getDataDirectory().toString());
		// Log.d("Here", "getDownloadCacheDirectory()=" +
		// Environment.getDownloadCacheDirectory().toString());
		// Log.d("Here", "getExternalStorageDirectory()=" +
		// Environment.getExternalStorageDirectory().toString());
		// Log.d("Here", "getExternalStoragePublicDirectory()=" +
		// Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString());
		// Log.d("Here", "getExternalStorageState()=" +
		// Environment.getExternalStorageState());
		// Log.d("Here", "getRootDirectory()=" +
		// Environment.getRootDirectory().toString());
		// Log.d("Here", "isExternalStorageRemovable()=" +
		// Environment.isExternalStorageRemovable());
		// 获取传过来的两个参数
		Intent intent = this.getIntent();
		relativeRoot = intent.getStringExtra("relativeRoot");
		// if(!new File(relativeRoot).exists()) relativeRoot = "/";
		purpose = intent.getIntExtra("purpose", IMPORT);
		// 获取布局中的元素
		fileListView = (ListView) findViewById(R.id.file_listView_layout_filepickeractivity);
		resultEditText = (EditText) findViewById(R.id.result_editText_layout_filepickeractivity);
		yesButton = (Button) findViewById(R.id.yes_button_layout_filepickeractivity);
		noButton = (Button) findViewById(R.id.no_button_layout_filepickeractivity);
		parentButton = (Button) findViewById(R.id.parent_button_layout_filepickeractivity);
		pathSpinner = (Spinner) findViewById(R.id.path_spinner_layout_filepickeractivity);

		// 设置文件列表的适配器
		fileListAdapter = new FilePickerAdapter(this, R.layout.filelist,
				fileList);
		fileListAdapter.setNotifyOnChange(false);
		fileListView.setAdapter(fileListAdapter);

		// 设置下拉列表的适配器
		spinnerAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, pathList);
		spinnerAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerAdapter.setNotifyOnChange(false);
		pathSpinner.setAdapter(spinnerAdapter);

		// 调用数据构造函数
		updateData(relativeRoot, purpose);

		// 设置下拉列表的点击监听器
		pathSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int position, long arg3) {
				// TODO Auto-generated method stub
				// Log.d("Here", "position=" + String.valueOf(position));
				updateData(pathList.get(position), purpose);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub

			}
		});

		// 设置“向上”按纽的点击监听器
		parentButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (new File(relativeRoot).getParent() != null)
					updateData(new File(relativeRoot).getParent(), purpose);
			}
		});

		// 设置文件列表的点击事件监听器
		fileListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int pos,
					long arg3) {
				// TODO Auto-generated method stub

				if (fileList.get(pos).isDirectory()) {// 如果点击了一个目录，则打开进入
					updateData(fileList.get(pos).getPath(), purpose);
					// resultEditText.setText("");
				} else {// 如果点击了一个文件，则获取文件名
					resultEditText.setText(fileList.get(pos).getName());
				}
			}
		});

		// 设置“否”按纽的监听器
		noButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				FilePickerActivity.this.setResult(RESULT_CANCELED);
				finish();
			}
		});

		// 设置“是”按纽的监听器
		yesButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub

				if (relativeRoot.equals("/"))
					result = relativeRoot + resultEditText.getText().toString();
				else
					result = relativeRoot + "/"
							+ resultEditText.getText().toString();
				// Log.d("Here", result);
				// 检查结果的有效性
				// 如果purpose是FILE，则结果字串所代表的不能是一个目录，而且一定要存在，同时可读
				// 如果purpost是DIR，则结果字串所代表的不能是一个目录，而且不能存在（如果存在则会被覆盖，同时可写
				if (new File(result).isDirectory()) {
					Toast.makeText(FilePickerActivity.this,
							FilePickerActivity.this.getString(R.string.msg1),
							Toast.LENGTH_LONG).show();
					return;
				} else if (purpose == IMPORT) {
					if (!new File(result).exists()) {
						Toast.makeText(
								FilePickerActivity.this,
								FilePickerActivity.this
										.getString(R.string.msg5),
								Toast.LENGTH_LONG).show();
						return;
					} else if (!new File(result).canRead()) {
						Toast.makeText(
								FilePickerActivity.this,
								FilePickerActivity.this
										.getString(R.string.msg3),
								Toast.LENGTH_LONG).show();
						return;
					}
				} else {
					if (new File(result).exists()) {
						if (!new File(result).canWrite()) {
							Toast.makeText(
									FilePickerActivity.this,
									FilePickerActivity.this
											.getString(R.string.msg4),
									Toast.LENGTH_LONG).show();
							return;
						}
						new AlertDialog.Builder(FilePickerActivity.this)
								.setTitle(R.string.msg2)
								.setNegativeButton(R.string.no, null)
								.setPositiveButton(R.string.yes,
										new Dialog.OnClickListener() {
											@Override
											public void onClick(
													DialogInterface dialog,
													int which) {
												// TODO Auto-generated method
												// stub
												Intent intent = new Intent();
												intent.putExtra("result",
														result);
												FilePickerActivity.this
														.setResult(RESULT_OK,
																intent);
												finish();
											}
										}).show();

						return;
					}else{
						if (!new File(result).getParentFile().canWrite()) {
							Toast.makeText(
									FilePickerActivity.this,
									FilePickerActivity.this
											.getString(R.string.msg4),
									Toast.LENGTH_LONG).show();
							return;
						}
					}
				}

				Intent intent = new Intent();
				intent.putExtra("result", result);
				FilePickerActivity.this.setResult(RESULT_OK, intent);
				finish();
			}
		});

	}

	// 构造数据的函数，从而更新下拉路径列表和文件列表
	private void updateData(String path, int purpose) {
		// Log.d("Here", "path = " + path);
		// Log.d("Here", "purpose = " + String.valueOf(purpose));

		// 先把原有数据清空
		fileList.clear();
		pathList.clear();

		// 如果传过来的path不存在，或者是一个文件名，则用root路径代替
		File root = new File(path);
		if (!root.exists())
			path = "/";

		// 更新文件列表的数据
		for (File tempFile : root.listFiles()) {
			fileList.add(tempFile);
		}

		// 更新下拉列表的数据
		while (root != null) {
			pathList.add(root.getPath());
			root = root.getParentFile();
		}

		// 更新下拉列表和文件列表
		fileListAdapter.notifyDataSetChanged();
		spinnerAdapter.notifyDataSetChanged();
		pathSpinner.setSelection(0);
		relativeRoot = path;
	}

}
