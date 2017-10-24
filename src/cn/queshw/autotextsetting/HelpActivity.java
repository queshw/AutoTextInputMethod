package cn.queshw.autotextsetting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import cn.queshw.autotextinputmethod.R;

public class HelpActivity extends Activity {
	private TextView helpTextView;

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.layout_helpactivity);
		helpTextView = (TextView) this.findViewById(R.id.help_textview_layout_helpactvity);		
		try {			
			InputStreamReader is = new InputStreamReader(this.getAssets().open("help.txt"));
			char [] buffer = new char[1024];
			StringBuilder text = new StringBuilder();
			BufferedReader br = new BufferedReader(is);
			while(true){
				int temp = br.read(buffer);
				if(temp == -1) break;
				String s = new String(buffer, 0, temp);
				text.append(s);
			}
			helpTextView.setText(text);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
}
