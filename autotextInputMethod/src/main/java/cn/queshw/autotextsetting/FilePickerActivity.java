package cn.queshw.autotextsetting;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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

import java.io.File;
import java.util.ArrayList;

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
	ArrayList<File> fileList = new ArrayList<File>();//某目录下所有文件和文件夹的数据
	ArrayList<File> showfileList = new ArrayList<File>();//真实用于显示的文件或文件夹的数据，在连输入边搜索的情况下，不符合条件的就不显示
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
		// 
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.layout_filepickeractivity);

		Intent intent = this.getIntent();
		relativeRoot = intent.getStringExtra("relativeRoot");
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
				showfileList);
		fileListAdapter.setNotifyOnChange(true);
		fileListView.setAdapter(fileListAdapter);

		// 设置下拉列表的适配器
		spinnerAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, pathList);
		spinnerAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerAdapter.setNotifyOnChange(true);
		pathSpinner.setAdapter(spinnerAdapter);

		// 调用数据构造函数
		updateData(relativeRoot, purpose);

		// 设置下拉列表的点击监听器
		pathSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int position, long arg3) {
				if(position != 0)	updateData(pathList.get(position), purpose);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// 

			}
		});

		// 设置“向上”按纽的点击监听器
		parentButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// 
				if (new File(relativeRoot).getParent() != null)
					updateData(new File(relativeRoot).getParent(), purpose);
			}
		});

		// 设置文件列表的点击事件监听器
		fileListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int pos,
					long arg3) {
				// 

				if (showfileList.get(pos).isDirectory()) {// 如果点击了一个目录，则打开进入
					updateData(showfileList.get(pos).getPath(), purpose);
					resultEditText.setText("");
				} else {// 如果点击了一个文件，则获取文件名
					resultEditText.setText(showfileList.get(pos).getName());
				}
			}
		});

		// 设置文件各输入框的 边输入边搜索监听器
		resultEditText.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {

			}

			@Override
			public void afterTextChanged(Editable s) {
				showfileList.clear();
				for(int i= 0; i < fileList.size(); i++){
					if(fileList.get(i).getName().toLowerCase().contains(s.toString().toLowerCase()))
						showfileList.add(fileList.get(i));
				}
				fileListAdapter.notifyDataSetChanged();
			}
		});

		// 设置“否”按纽的监听器
		noButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// 
				FilePickerActivity.this.setResult(RESULT_CANCELED);
				finish();
			}
		});

		// 设置“是”按纽的监听器
		yesButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// 

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

		// 先把原有数据清空
		fileList.clear();
		showfileList.clear();
		pathList.clear();

		// 如果目录列不出东西，则直接返回
		File nowPathFile = new File(path);
		File[] temp = nowPathFile.listFiles();
		if(temp != null){
			// 更新文件列表的数据
			for (File tempFile : temp) {
				fileList.add(tempFile);
				showfileList.add(tempFile);
			}

			// 更新下拉列表的数据
			while (nowPathFile != null) {
				pathList.add(nowPathFile.getPath());
				nowPathFile = nowPathFile.getParentFile();
			}
		}else{
			pathList.add(path);
		}

		// 更新下拉列表和文件列表
		fileListAdapter.notifyDataSetChanged();
		spinnerAdapter.notifyDataSetChanged();
		pathSpinner.setSelection(0);
		this.relativeRoot = path;
	}

}
