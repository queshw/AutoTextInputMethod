package cn.queshw.autotextsetting;

import java.util.List;

import cn.queshw.autotextinputmethod.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class AutotextAdapter extends ArrayAdapter<AutotextItem> {
	int resource;
	Context context;

	public AutotextAdapter(Context context, int resourceId,
			List<AutotextItem> objects) {
		super(context, resourceId, objects);
		this.resource = resourceId;
		this.context = context;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		AutotextItem item = getItem(position);
		ViewHolder holder = new ViewHolder();
		if(convertView == null){
			convertView = LayoutInflater.from(context).inflate(resource, null);
			holder.inputTextView = (TextView) convertView.findViewById(R.id.input_textview_autotextitem);
			holder.autotextTextView  = (TextView) convertView.findViewById(R.id.autotext_textview_autotextitem);
			convertView.setTag(holder);
		}else{
			holder = (ViewHolder) convertView.getTag();
		}
		
		//holder.inputTextView.setText(String.valueOf(item.getId()) + "." +item.getInput());
		holder.inputTextView.setText(item.getInput());
		holder.autotextTextView.setText(item.getAutotext());
		return convertView;
	}
	
	private class ViewHolder{
		TextView inputTextView;
		TextView autotextTextView;		
	}


}
