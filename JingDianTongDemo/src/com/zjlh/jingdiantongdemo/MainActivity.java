package com.zjlh.jingdiantongdemo;

import com.zjlh.view.MapLayout;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;

public class MainActivity extends Activity {
	private MapLayout mMyMapLayout;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mMyMapLayout = new MapLayout(this);
		setContentView(mMyMapLayout);
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				mMyMapLayout.setScale(MapLayout.MIN_SCALE);
			}
		}, 1000);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mMyMapLayout.compassView.enable();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		mMyMapLayout.compassView.disable();
	}
}
