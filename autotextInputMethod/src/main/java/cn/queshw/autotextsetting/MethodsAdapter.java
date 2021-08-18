package cn.queshw.autotextsetting;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import cn.queshw.autotextinputmethod.R;

public class MethodsAdapter extends ArrayAdapter<MethodItem> {
	private final int resource;
	private final Context context;

	public MethodsAdapter(Context context, int resourceId,
			List<MethodItem> objects) {
		super(context, resourceId, objects);
		resource = resourceId;
		this.context = context;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.ArrayAdapter#getView(int, android.view.View,
	 * android.view.ViewGroup)
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// Log.d("Here", "getView position=" + String.valueOf(position));
		MethodItem item = getItem(position);
		Holder holder = new Holder();
		if (convertView == null) {
			convertView = LayoutInflater.from(context).inflate(resource, null);
			holder.default_imageview = convertView
					.findViewById(R.id.default_imageview_methoditem);
			holder.name_textview = convertView
					.findViewById(R.id.name_textview_methoditem);
			holder.linearlayout = convertView.findViewById(R.id.linearlayout_methoditem);
			convertView.setTag(holder);
		} else {
			holder = (Holder) convertView.getTag();
		}
		
		//设置内容
		if(item.getIsDefault() == MethodItem.DEFAULT){
			holder.default_imageview.setVisibility(View.VISIBLE);
		}else{
			holder.default_imageview.setVisibility(View.INVISIBLE);
		}
		holder.name_textview.setText(item.getName());		

		return convertView;
	}

	class Holder {
		LinearLayout linearlayout;
		ImageView default_imageview;
		TextView name_textview;
	}

}
