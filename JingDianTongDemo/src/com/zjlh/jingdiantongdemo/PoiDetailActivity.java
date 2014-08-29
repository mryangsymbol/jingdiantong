package com.zjlh.jingdiantongdemo;

import com.zjlh.view.MapLayout;
import com.zjlh.view.MapLayout.PoiPoint;
import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.widget.TextView;

public class PoiDetailActivity extends Activity {
	private TextView tvTitle;
	private WebView wvDetail;
	private MapLayout.PoiPoint poiPoint;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_poi_detail);
		
		tvTitle = (TextView) findViewById(R.id.tvTitle);
		wvDetail = (WebView) findViewById(R.id.wvDetail);
		
		poiPoint = (PoiPoint) getIntent().getSerializableExtra("poiPoint");
		tvTitle.setText(poiPoint.title);
		wvDetail.loadUrl("file:///android_asset/map_data_1/" + poiPoint.htmlPath);
	}

}
