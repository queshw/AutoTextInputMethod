package cn.queshw.autotextsetting;

import java.util.List;

import cn.queshw.autotextinputmethod.R;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class RawAdapter extends ArrayAdapter<RawItem> {
	int resource;
	Context context;

	public RawAdapter(Context context, int resourceId, List<RawItem> objects) {
		super(context, resourceId, objects);
		this.resource = resourceId;
		this.context = context;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		RawItem item = getItem(position);
		ViewHolder holder = new ViewHolder();
		if (convertView == null) {
			convertView = LayoutInflater.from(context).inflate(resource, null);
			holder.linear = (LinearLayout) convertView.findViewById(R.id.rawitem_linear);
			holder.codeTextView = (TextView) convertView.findViewById(R.id.code_textview_rawitem);
			holder.candidateTextView = (TextView) convertView.findViewById(R.id.candidate_textview_rawitem);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		// 设置背景颜色
		if (item.getTwolevel() < 0) {
			holder.linear.setBackgroundColor(Color.LTGRAY);
			holder.candidateTextView.setText(item.getCandidate());
			holder.codeTextView.setText("(" + String.valueOf(item.getTwolevel()) + ")" + item.getCode());
		} else {
			holder.linear.setBackgroundColor(Color.TRANSPARENT);
			holder.candidateTextView.setText(item.getCandidate());
			holder.codeTextView.setText(item.getCode());
		}
		// holder.inputTextView.setText(String.valueOf(item.getId()) + "."
		// +item.getInput());
		
		return convertView;
	}

	private class ViewHolder {
		LinearLayout linear;
		TextView codeTextView;
		TextView candidateTextView;
	}

}
