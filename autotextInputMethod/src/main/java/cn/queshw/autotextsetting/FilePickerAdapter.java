package cn.queshw.autotextsetting;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.List;

import cn.queshw.autotextinputmethod.R;

public class FilePickerAdapter extends ArrayAdapter<File> {
	private int resource;
	private Context context;

	public FilePickerAdapter(Context context, int resourceId,
			List<File> objects) {
		super(context, resourceId, objects);
		resource = resourceId;
		this.context = context;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		File file = getItem(position);
		Holder holder = new Holder();
		if(convertView == null){
			convertView = LayoutInflater.from(context).inflate(resource, null);
			holder.typeImageView = (ImageView) convertView.findViewById(R.id.type_imageView_filelist);
			holder.nameTextView = (TextView) convertView.findViewById(R.id.name_textView_filelist);
			convertView.setTag(holder);
		}else{
			holder = (Holder) convertView.getTag();
		}
		//如果是一个目录，那么就设置不同的图片，并且在名字后加"/"
		if(file.isDirectory()){
			holder.typeImageView.setImageResource(R.drawable.dir);
			holder.nameTextView.setText(file.getName() + "/");
		}else{
			holder.typeImageView.setImageResource(R.drawable.file);
			holder.nameTextView.setText(file.getName());
		}
		return convertView;
	}
	
	class Holder{
		ImageView typeImageView;
		TextView nameTextView;
	}

}
