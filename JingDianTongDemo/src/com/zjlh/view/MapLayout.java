package com.zjlh.view;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.text.Html;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsoluteLayout;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.zjlh.jingdiantongdemo.PoiDetailActivity;
import com.zjlh.jingdiantongdemo.R;

public class MapLayout extends FrameLayout implements View.OnClickListener {
	private MyMapView mMapView;
	private AbsoluteLayout mPoisLayout;
	private RelativeLayout controlsLayout;
	public CompassView compassView;
	private View poiDialogView;
	private PoiPoint currSelectPoi;
	private static final String ZOOM_UP_BUTTON_TAG = "ZOOM_UP_BUTTON_TAG";
    private static final String ZOOM_IN_BUTTON_TAG = "ZOOM_IN_BUTTON_TAG";
	private boolean poisAdded;
	private boolean piecesLoaded = false;
	private List<View> poiViews = new ArrayList<View>();
	private String[][][] mapPiecesFileName;
	private int mapWidth = 4733, mapHeight = 3183;
	public static final float MAX_SCALE = 1f;// 最大缩放比例
    public static final float MIN_SCALE = 0.25f;
	private View.OnClickListener zoomBtnsClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			if (piecesLoaded) {
				if (v.getTag().equals(ZOOM_UP_BUTTON_TAG)) {
					zoomUp();
				} else if (v.getTag().equals(ZOOM_IN_BUTTON_TAG)) {
					zoomIn();
				}
			}
		}
	};
	
	private static final String MAP_CONFIG_FILE_PATH = "map_data_1" + File.separator + "content.xml";
	private String readTextFromAssets(String path) {
		StringBuilder body = new StringBuilder();
		try {
			InputStream is = getContext().getAssets().open(path);
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			String nextLine;
			while ((nextLine = reader.readLine()) != null) {
				body.append(nextLine);
				body.append("\n");
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return body.toString();
	}
	
	private PoiPoint parsePoiPoint(String xmlString) {
		PoiPoint poi = new PoiPoint();
		String pointHead = xmlString.substring(xmlString.indexOf("<point"), xmlString.indexOf("\">"));
		poi.bmpX = Integer.parseInt(pointHead.replaceAll("(.+x=\")|(\"\\D+y.+)", ""));
		poi.bmpY = Integer.parseInt(pointHead.replaceAll(".+y=\"", ""));
		poi.id = xmlString.substring(xmlString.indexOf("<id>") + 4, xmlString.indexOf("</id>"));
		poi.latitude = Double.parseDouble(xmlString.substring(xmlString.indexOf("<latitude>") + 10, xmlString.indexOf("</latitude>")));
		poi.longitude = Double.parseDouble(xmlString.substring(xmlString.indexOf("<longitude>") + 11, xmlString.indexOf("</longitude>")));
		poi.title = xmlString.substring(xmlString.indexOf("<title>") + 7, xmlString.indexOf("</title>"));
		poi.htmlPath = xmlString.substring(xmlString.indexOf("<url>") + 5, xmlString.indexOf("</url>"));
		poi.thumbUrl = xmlString.substring(xmlString.indexOf("thumbs=") + 8, xmlString.indexOf(".jpg") + 4);
		return poi;
	}
	
	private List<PoiPoint> parsePoiPointsFromXml(String xmlContent) {
		List<PoiPoint> poiList = new ArrayList<PoiPoint>();
		int parseStart = xmlContent.indexOf("<point", xmlContent.indexOf("<points>") + 5);
		int pointStart;
		while ((pointStart = xmlContent.indexOf("<point", parseStart)) != -1) {
			poiList.add(parsePoiPoint(xmlContent.substring(pointStart,
					xmlContent.indexOf("</point", parseStart))));
			parseStart = xmlContent.indexOf("</point", parseStart) + 5;
		}
		return poiList;
	}
	
	private void parseMapPiecesFileName() {
		try {
			String piecesFiles[] = getContext().getAssets().list("map_data_1" + File.separator + "tiles");
			List<String> piecesFileList = Arrays.asList(piecesFiles);
			int pieceKinds = 0;
			for (pieceKinds = 0; ; pieceKinds++) {
				if (!piecesFileList.contains("map_" + pieceKinds + "_0_0@2x.jpg")) {
					break;
				}
			}
			mapPiecesFileName = new String[pieceKinds][][];
			
			int rows = 0;
			for (int i = 0; i < mapPiecesFileName.length; i++) {
				for (rows = 0; ; rows++) {
					if (!piecesFileList.contains("map_" + i + "_"+ rows + "_0@2x.jpg")) {
						break;
					}
				}
				mapPiecesFileName[i] = new String[rows][];
			}
			
			int columns = 0;
			for (int i = 0; i < mapPiecesFileName.length; i++) {
				for (int j = 0; j < mapPiecesFileName[i].length; j++) {
					for (columns = 0; ; columns++) {
						if (!piecesFileList.contains("map_" + i + "_"+ j + "_" + columns + "@2x.jpg")) {
							break;
						}
					}
					mapPiecesFileName[i][j] = new String[columns];
				}
			}
			
			for (int i = 0; i < mapPiecesFileName.length; i++) {
				for (int j = 0; j < mapPiecesFileName[i].length; j++) {
					for (int k = 0; k < mapPiecesFileName[i][j].length; k++) {
						mapPiecesFileName[i][j][k] = "map_data_1" + File.separator
								+ "tiles" + File.separator
								+ "map_" + i + "_"+ j + "_" + k + "@2x.jpg";
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public Bitmap readBitmapFromAssets(String path) {
		Bitmap bmp = null;
		try {
			InputStream is = getContext().getAssets().open(path);
			bmp = BitmapFactory.decodeStream(is);
			is.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return bmp;
	}

	public void setScale(float scale) {
		if (mMapView.currScale != scale) {
			mMapView.currScale = scale;
			mMapView.mMatrix.setScale(mMapView.currScale, mMapView.currScale);
			mMapView.mMatrix.postTranslate(-(mMapView.currScale*mapWidth - getWidth()-2*mMapView.initMatrixX)/2,
					-(mMapView.currScale*mapHeight-getHeight()-2*mMapView.initMatrixY)/2);
			mMapView.invalidate();
			onMapChanged();
		}
	}
	
	private void zoomUp() {
		if (mMapView.currScale != MAX_SCALE) {
			int grade = (int) ((mMapView.currScale - MIN_SCALE) / MyMapView.GRADE_SCALE);
			grade++;
			mMapView.currScale = MIN_SCALE + MyMapView.GRADE_SCALE * grade;
			mMapView.mMatrix.setScale(mMapView.currScale, mMapView.currScale);
			mMapView.mMatrix.postTranslate(-(mMapView.currScale*mapWidth - getWidth()-2*mMapView.initMatrixX)/2,
					-(mMapView.currScale*mapHeight-getHeight()-2*mMapView.initMatrixY)/2);
			mMapView.invalidate();
			onMapChanged();
		}
	}
	
	private void zoomIn() {
		if (mMapView.currScale != MIN_SCALE) {
			int grade = (int) ((mMapView.currScale - MIN_SCALE) / MyMapView.GRADE_SCALE);
			if (grade > 0) {
				grade--;
			}
			mMapView.currScale = MIN_SCALE + MyMapView.GRADE_SCALE * grade;
			mMapView.mMatrix.setScale(mMapView.currScale, mMapView.currScale);
			mMapView.mMatrix.postTranslate(-(mMapView.currScale*mapWidth - getWidth()-2*mMapView.initMatrixX)/2,
					-(mMapView.currScale*mapHeight-getHeight()-2*mMapView.initMatrixY)/2);
			mMapView.insureCanSee();
			mMapView.invalidate();
			onMapChanged();
		}
	}
	
	public MapLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public MapLayout(Context context) {
		super(context);
		init();
	}
	
	private void init() {
		parseMapPiecesFileName();
		
		mMapView = new MyMapView(getContext());
		addView(mMapView);
		
		mPoisLayout = new AbsoluteLayout(getContext());
		addView(mPoisLayout);
		
		List<PoiPoint> poiList = parsePoiPointsFromXml(readTextFromAssets(MAP_CONFIG_FILE_PATH));
		addPoiViews(poiList);
		
		controlsLayout = new RelativeLayout(getContext());
		addView(controlsLayout);
		
		addCompass();
		addZoomControls();
	}
	
	private void addZoomControls() {
		LinearLayout zoomLayout = new LinearLayout(getContext());
		Button zoomUpBtn = new Button(getContext());
		zoomUpBtn.setTag(ZOOM_UP_BUTTON_TAG);
		zoomUpBtn.setBackgroundResource(R.drawable.xml_zoom_up_btn);
		Button zoomInBtn = new Button(getContext());
		zoomInBtn.setTag(ZOOM_IN_BUTTON_TAG);
		zoomInBtn.setBackgroundResource(R.drawable.xml_zoom_in_btn);
		LinearLayout.LayoutParams btnsLp = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		btnsLp.leftMargin = 15;
		btnsLp.rightMargin = 15;
		zoomLayout.addView(zoomUpBtn, btnsLp);
		zoomLayout.addView(zoomInBtn, btnsLp);
		zoomUpBtn.setOnClickListener(zoomBtnsClickListener);
		zoomInBtn.setOnClickListener(zoomBtnsClickListener);
		
		RelativeLayout.LayoutParams zoomLp = new RelativeLayout.LayoutParams(
    	    			RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		zoomLp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		zoomLp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		zoomLp.topMargin = 25;
		zoomLp.rightMargin = 25;
		controlsLayout.addView(zoomLayout, zoomLp);
	}
	
	private void addCompass() {
		compassView = new CompassView(getContext());
		RelativeLayout.LayoutParams compassLp = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);
		compassLp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		compassLp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		compassLp.leftMargin = 25;
		compassLp.topMargin = 25;
		controlsLayout.addView(compassView, compassLp);
	}
	
	public void addPoiViews(List<PoiPoint> poiList) {
		for (int i = 0; i < poiList.size(); i++) {
			PoiPoint point = poiList.get(i);
			View poiView = LayoutInflater.from(getContext()).inflate(R.layout.poi_layout, null);
			poiView.setTag(point);
			((TextView)poiView.findViewById(R.id.tvNumber)).setText(String.valueOf(i+1));
			poiView.setOnClickListener(this);
			poiViews.add(poiView);
			int x = point.bmpX, y = point.bmpY;
			float xy[] = {x, y};
			mMapView.mMatrix.mapPoints(xy);
			xy[0] = xy[0] - mMapView.initMatrixX - poiView.getWidth()/3;
			xy[1] = xy[1] - mMapView.initMatrixY - poiView.getHeight();
			AbsoluteLayout.LayoutParams lp = new AbsoluteLayout.LayoutParams(
					AbsoluteLayout.LayoutParams.WRAP_CONTENT,
					AbsoluteLayout.LayoutParams.WRAP_CONTENT, (int)xy[0], (int)xy[1]);
			mPoisLayout.addView(poiView, lp);
		}
		poisAdded = true;
		
		poiDialogView = LayoutInflater.from(getContext()).inflate(R.layout.poi_dialog, null);
		AbsoluteLayout.LayoutParams lp = new AbsoluteLayout.LayoutParams(
				AbsoluteLayout.LayoutParams.WRAP_CONTENT,
				AbsoluteLayout.LayoutParams.WRAP_CONTENT, 0, 0);
		mPoisLayout.addView(poiDialogView, lp);
		poiDialogView.setVisibility(View.INVISIBLE);
		poiDialogView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (poiDialogView.getVisibility() == View.VISIBLE) {
					Intent intent = new Intent(getContext(), PoiDetailActivity.class);
					intent.putExtra("poiPoint", currSelectPoi);
					getContext().startActivity(intent);
				}
			}
		});
	}
	
	@Override
	public void onClick(View v) {
		currSelectPoi = (PoiPoint) v.getTag();
		((TextView)poiDialogView.findViewById(R.id.tvTitle)).setText(currSelectPoi.title);
		TextView tvDetail = (TextView) poiDialogView.findViewById(R.id.tvDetail);
		String html = Html.fromHtml(readTextFromAssets("map_data_1/" + currSelectPoi.htmlPath)).toString();
		tvDetail.setText(html);
		int x = currSelectPoi.bmpX, y = currSelectPoi.bmpY;
		float xy[] = {x, y};
		mMapView.mMatrix.mapPoints(xy);
		xy[0] = xy[0] - mMapView.initMatrixX - poiDialogView.getWidth()/2;
		xy[1] = xy[1] - mMapView.initMatrixY - poiDialogView.getHeight();
		AbsoluteLayout.LayoutParams lp = new AbsoluteLayout.LayoutParams(
				AbsoluteLayout.LayoutParams.WRAP_CONTENT,
				AbsoluteLayout.LayoutParams.WRAP_CONTENT, (int)xy[0], (int)xy[1]);
		poiDialogView.setLayoutParams(lp);
		poiDialogView.setVisibility(View.VISIBLE);
		
		boolean changed = false;
		if (xy[0] < 0) {
			mMapView.mMatrix.postTranslate(-xy[0], 0);
			changed = true;
		} else if (xy[0] > getWidth() - poiDialogView.getWidth()) {
			mMapView.mMatrix.postTranslate(getWidth() - poiDialogView.getWidth() - xy[0], 0);
			changed = true;
		}
		
		if (xy[1] < 0) {
			mMapView.mMatrix.postTranslate(0, -xy[1]);
			changed = true;
		} else if (xy[1] > getHeight() - poiDialogView.getHeight()) {
			mMapView.mMatrix.postTranslate(0, getHeight() - poiDialogView.getHeight() - xy[1]);
			changed = true;
		}
		if (changed) {
			mMapView.invalidate();
			onMapChanged();
		}
	}
	
	public void onMapChanged() {
		if (poisAdded) {
			for (int i = 0; i < poiViews.size(); i++) {
				View poiView = poiViews.get(i);
				AbsoluteLayout.LayoutParams lp = (AbsoluteLayout.LayoutParams) poiView.getLayoutParams();
				PoiPoint p = (PoiPoint) poiView.getTag();
				float xy[] = {p.bmpX, p.bmpY};
				mMapView.mMatrix.mapPoints(xy);
				lp.x = (int) (xy[0] - mMapView.initMatrixX - poiView.getWidth()/3);
				lp.y = (int) (xy[1] - mMapView.initMatrixY - poiView.getHeight());
				poiView.setLayoutParams(lp);
			}
		}
		
		if (poiDialogView.getVisibility() == View.VISIBLE) {
			AbsoluteLayout.LayoutParams lp = (AbsoluteLayout.LayoutParams) poiDialogView.getLayoutParams();
			float xy[] = {currSelectPoi.bmpX, currSelectPoi.bmpY};
			mMapView.mMatrix.mapPoints(xy);
			lp.x = (int) (xy[0] - mMapView.initMatrixX - poiDialogView.getWidth()/2);
			lp.y = (int) (xy[1] - mMapView.initMatrixY - poiDialogView.getHeight());
			poiDialogView.setLayoutParams(lp);
		}
	}
	
	private class MyMapView extends View {
	    private static final int GRADE_NUM = 8;
	    private static final float GRADE_SCALE = (MAX_SCALE - MIN_SCALE) / GRADE_NUM;
		private static final int NONE = 0;// 初始状态
	    private static final int DRAG = 1;// 拖动
	    private static final int ZOOM = 2;// 缩放
	    private int mode = NONE; // Remember some things for zooming
	    private PointF downPoint = new PointF(); //单点触控时的按下点
	    private PointF pointerDownMidPoint = new PointF(); //两点触控时按下的两点的中间点
	    private PointF pointerMoveMidPoint = new PointF(); //两点触控时移动过程中两点的中间点
	    private float pointerDownDistance; //两点触控时按下的两点间的距离
	    private float pointerDownAngle; //两点触控时由按下的两点确定的角度
	    private float currRotateAngle;
	    private float savedRotateAngle;
	    private float currScale = MAX_SCALE;
	    private float savedScale;
		private Matrix mMatrix = new Matrix();
		private Matrix savedMatrix = new Matrix();
		private Paint bitmapPaint = new Paint();
		private Bitmap bmpMapContainer = Bitmap.createBitmap(mapWidth, mapHeight, Bitmap.Config.RGB_565);
		private float initMatrixX, initMatrixY;
		public MyMapView(Context context) {
			super(context);
		}
		
		
		@Override
		protected void onDraw(Canvas canvas) {
			super.onDraw(canvas);
			if (!piecesLoaded) {
				piecesLoaded = true;
				Canvas mapCanvas = new Canvas(bmpMapContainer);
				int currWidth = 0, currHeight = 0;
				Bitmap bmp = null;
//				for (int i = 0; i < mapPiecesFileName.length; i++) {
				for (int i = 0; i < 1; i++) {
					for (int j = 0; j < mapPiecesFileName[i].length; j++) {
						currWidth = 0;
						for (int k = 0; k < mapPiecesFileName[i][j].length; k++) {
							bmp = readBitmapFromAssets(mapPiecesFileName[i][j][k]);
							mapCanvas.drawBitmap(bmp, currWidth, currHeight, bitmapPaint);
							currWidth += bmp.getWidth();
						}
						currHeight += bmp.getHeight();
					}
				}
				
				mMatrix = canvas.getMatrix();
				mMatrix.getValues(currMatrixArray);
				initMatrixX = currMatrixArray[2];
				initMatrixY = currMatrixArray[5];
			}
			
			if (piecesLoaded) {
				canvas.setMatrix(mMatrix);
				canvas.drawBitmap(bmpMapContainer, 0, 0, bitmapPaint);
			}
		}
		
		private long downTime;
		private static final long CLICK_TIME_GAP = 100;
		@Override
		public boolean onTouchEvent(MotionEvent event) {
			int action = event.getAction() & MotionEvent.ACTION_MASK;
	        switch (action) {
	        // 主点按下
	        case MotionEvent.ACTION_DOWN:
	            savedMatrix.set(mMatrix);
	            // 设置初始点位置
	            downPoint.set(event.getX(), event.getY());
	            mode = DRAG;
	            downTime = System.currentTimeMillis();
	            break;
	        case MotionEvent.ACTION_POINTER_DOWN:
	            pointerDownDistance = spacing(event);
	            // 如果连续两点距离大于10，则判定为多点模式
	            if (pointerDownDistance > 10f) {
	            	pointerDownAngle = getAngle(event);
	            	savedRotateAngle = currRotateAngle;
	            	savedScale = currScale;
	                savedMatrix.set(mMatrix);
	                midPoint(pointerDownMidPoint, event);
	                mode = ZOOM;
	            }
	            break;
	        case MotionEvent.ACTION_UP:
	        case MotionEvent.ACTION_POINTER_UP:
//	        	if (mode == ZOOM) {
//	        		insureScaleRange();
//	        	}
//	        	insureCanSee();
	            mode = NONE;
	            if (System.currentTimeMillis() - downTime <= CLICK_TIME_GAP) {
	            	poiDialogView.setVisibility(View.INVISIBLE);
	            }
	            break;
	        case MotionEvent.ACTION_MOVE:
	            if (mode == DRAG) {
	            	mMatrix.set(savedMatrix);
	            	mMatrix.postTranslate(event.getX() - downPoint.x, event.getY() - downPoint.y);
	            	
	            	insureCanSee();
	            	
	                invalidate();
	                onMapChanged();
	            } else if (mode == ZOOM) {
	            	 float newDist = spacing(event);
	                 // A'B'中点
	                 midPoint(pointerMoveMidPoint, event);
	                 mMatrix.set(savedMatrix);
	                 // AB移动到A''B'
	                 float xMove = pointerMoveMidPoint.x - pointerDownMidPoint.x;
	                 float yMove = pointerMoveMidPoint.y - pointerDownMidPoint.y;
	                 mMatrix.postTranslate(xMove, yMove);
	                 // A''B'缩放到A'''B'
	                 float scale = newDist / pointerDownDistance;
	                 currScale = savedScale * scale;
	                 mMatrix.postScale(scale, scale,
	                		 pointerMoveMidPoint.x, pointerMoveMidPoint.y);
	                 float rotateAngle = getAngle(event) - pointerDownAngle;
	                 currRotateAngle = (savedRotateAngle + rotateAngle) % 360;
//	            	 mMatrix.postRotate(rotateAngle,
//	            			 pointerMoveMidPoint.x, pointerMoveMidPoint.y);
	                 insureScaleRange();
	                 insureCanSee();
	                 invalidate();
	                 onMapChanged();
	            }
	            break;
	        }
	        return true;
	    }
		
		private float getAngle(MotionEvent Event) {
	        double DeltalX=Event.getX(0)-Event.getX(1);
	        double DeltalY=Event.getY(0)-Event.getY(1);
	        return (float) (360 - Math.atan2(DeltalX, DeltalY) * 180 / Math.PI);
	    }
		
		/** * 两点的距离 * Determine the space between the first two fingers */
	    private float spacing(MotionEvent event) {
	        float x = event.getX(0) - event.getX(1);
	        float y = event.getY(0) - event.getY(1);
	        return FloatMath.sqrt(x * x + y * y);
	    }
	    
	    /** * 两点的中点 * Calculate the mid point of the first two fingers * */
	    private void midPoint(PointF point, MotionEvent event) {
	        float x = event.getX(0) + event.getX(1);
	        float y = event.getY(0) + event.getY(1);
	        point.set(x / 2, y / 2);
	    }
	    
	    private void insureScaleRange() {
	    	if (currScale > MAX_SCALE) {
				mMatrix.postScale(MAX_SCALE/currScale, MAX_SCALE/currScale,
	              		 pointerMoveMidPoint.x, pointerMoveMidPoint.y);
				currScale = MAX_SCALE;
//				invalidate();
			} else if (currScale < MIN_SCALE) {
				mMatrix.postScale(MIN_SCALE/currScale, MIN_SCALE/currScale,
	             		 pointerMoveMidPoint.x, pointerMoveMidPoint.y);
				currScale = MIN_SCALE;
//				invalidate();
			}
	    }
	    
	    private float currMatrixArray[] = new float[9];
	    private void insureCanSee() {
	    	mMatrix.getValues(currMatrixArray);
	    	float currMapBmpWidth = currMatrixArray[0] * mapWidth;
	    	float currMapBmpHeight = currMatrixArray[0] * mapHeight;
	    	if (currMapBmpWidth > getWidth()) {
		    	if (currMatrixArray[2] > initMatrixX) {
		    		mMatrix.postTranslate(-currMatrixArray[2] + initMatrixX, 0);
		    	} else {
		    		float maxMatrixWidth = mapWidth * currMatrixArray[0] - getWidth() - initMatrixX;
			    	if (currMatrixArray[2] < -maxMatrixWidth) {
			    		mMatrix.postTranslate(-currMatrixArray[2] - maxMatrixWidth, 0);
			    	}
		    	}
	    	} else {
	    		if (currMatrixArray[2] < initMatrixX) {
	    			mMatrix.postTranslate(-currMatrixArray[2] + initMatrixX, 0);
		    	} else if (currMatrixArray[2] > getWidth() - currMapBmpWidth + initMatrixX) {
		    		mMatrix.postTranslate(-currMatrixArray[2] + getWidth() - currMapBmpWidth + initMatrixX, 0);
		    	}
	    	}
	    	
	    	if (currMapBmpHeight > getHeight()) {
		    	if (currMatrixArray[5] > initMatrixY) {
		    		mMatrix.postTranslate(0, -currMatrixArray[5] + initMatrixY);
		    	} else {
		    		float maxMatrixHeight = mapHeight * currMatrixArray[0] - getHeight() - initMatrixY;
			    	if (currMatrixArray[5] < -maxMatrixHeight) {
			    		mMatrix.postTranslate(0, -currMatrixArray[5] - maxMatrixHeight);
			    	}
		    	}
	    	} else {
	    		if (currMatrixArray[5] < initMatrixY) {
		    		mMatrix.postTranslate(0, -currMatrixArray[5] + initMatrixY);
		    	} else if (currMatrixArray[5] > getHeight() - currMapBmpHeight + initMatrixY) {
		    		mMatrix.postTranslate(0, -currMatrixArray[5] + getHeight() - currMapBmpHeight + initMatrixY);
		    	}
	    	}
	    }
	}
	
	public static class PoiPoint implements Serializable {
		private static final long serialVersionUID = 1L;
		public String id;
		public int bmpX, bmpY;
		public double latitude, longitude;
		public String title;
		public String thumbUrl;
		public String htmlPath;
	}
}
