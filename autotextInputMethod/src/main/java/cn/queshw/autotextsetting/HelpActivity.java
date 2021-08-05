package cn.queshw.autotextsetting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import com.google.android.material.tabs.TabLayout;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import cn.queshw.autotextinputmethod.R;

public class HelpActivity extends Activity {

	// TextView helpTextView;
	TabLayout mTabLayout;
	ViewPager mViewPager;
	ArrayList<ScrollView> tabViewList = new ArrayList<ScrollView>();
	String[] mTitle;
	String[] mFile;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.layout_helpactivity);

		mTabLayout = (TabLayout) findViewById(R.id.tl);
		mViewPager = (ViewPager) findViewById(R.id.viewpager);
		mTitle = new String[]{"版本","问与答","背景知识"};//标签页的标题
		mFile = new String[]{"help_tab1.txt","help_tab2.txt","help_tab3.txt"};//标签页内容的文件名
		//读取tab 的内容
		for(int i = 0; i < mTitle.length; i++){
			View temp = this.getLayoutInflater().inflate(R.layout.helptab_layout, null);
			ScrollView tempSv = (ScrollView) temp.findViewById(R.id.helptab_scrollview);
			TextView tempTv = (TextView) temp.findViewById(R.id.helptab_textview);
			tabViewList.add(tempSv);
			try {			
				InputStreamReader is = new InputStreamReader(this.getAssets().open(mFile[i]));
				char [] buffer = new char[1024];
				StringBuilder text = new StringBuilder();
				BufferedReader br = new BufferedReader(is);
				while(true){
					int tempFs = br.read(buffer);
					if(tempFs == -1) break;
					String s = new String(buffer, 0, tempFs);
					text.append(s);
				}
				tempTv.setText(text);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
		}
		
		PagerAdapter mAdapter = new PagerAdapter() {
	        @Override
	        public CharSequence getPageTitle(int position) {
	            return mTitle[position];
	        }

	        @Override
	        public int getCount() {
	            return mTitle.length;
	        }

	        @Override
	        public Object instantiateItem(View container, int position) {
	            ((ViewPager) container).addView(tabViewList.get(position));
	            return tabViewList.get(position);
	        }

	        @Override
	        public void destroyItem(ViewGroup container, int position, Object object) {
	            ((ViewPager) container).removeView((View) object);
	        }

	        @Override
	        public boolean isViewFromObject(View view, Object object) {
	            return view == object;
	        }

	    };
	    
		mViewPager.setAdapter(mAdapter);
        mTabLayout.setupWithViewPager(mViewPager);
	}
}
