package com.segre.weatherstories;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.view.*;
import android.widget.ImageView;

public class ZoomImageView extends ImageView {
 public interface PageListener { void turn(int direction); }
 private PageListener pages;
 private final Matrix transform=new Matrix();
 private final ScaleGestureDetector pinch;
 private final GestureDetector gestures;
 private float zoom=1f,base=1f,lastX,lastY;
 private boolean multi=false;
 private int pointer=-1;
 public ZoomImageView(Context context){super(context);setScaleType(ScaleType.MATRIX);
  setContentDescription("Weather Story. Pinch to zoom, drag to pan. Double-tap to zoom or reset. Swipe left or right at normal zoom to change story.");
  pinch=new ScaleGestureDetector(context,new ScaleGestureDetector.SimpleOnScaleGestureListener(){
   @Override public boolean onScaleBegin(ScaleGestureDetector d){multi=true;return true;}
   @Override public boolean onScale(ScaleGestureDetector d){if(getDrawable()==null)return false;float next=Math.max(1f,Math.min(6f,zoom*d.getScaleFactor()));float factor=next/zoom;zoom=next;transform.postScale(factor,factor,d.getFocusX(),d.getFocusY());constrain();return true;}
  });
  gestures=new GestureDetector(context,new GestureDetector.SimpleOnGestureListener(){
   @Override public boolean onDown(MotionEvent e){return true;}
   @Override public boolean onDoubleTap(MotionEvent e){if(getDrawable()==null)return true;if(zoom>1.01f)resetZoom();else{zoom=2.5f;transform.postScale(zoom,zoom,e.getX(),e.getY());constrain();}return true;}
   @Override public boolean onSingleTapConfirmed(MotionEvent e){performClick();return true;}
   @Override public boolean onFling(MotionEvent a,MotionEvent b,float vx,float vy){if(!multi&&zoom<=1.01f&&a!=null&&Math.abs(b.getX()-a.getX())>40*getResources().getDisplayMetrics().density&&Math.abs(vx)>Math.abs(vy)){if(pages!=null)pages.turn(b.getX()<a.getX()?1:-1);return true;}return false;}
  });
 }
 public void setPageListener(PageListener listener){pages=listener;}
 @Override public void setImageBitmap(Bitmap bitmap){super.setImageBitmap(bitmap);resetZoom();}
 @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);resetZoom();}
 public void resetZoom(){if(transform==null)return;zoom=1f;transform.reset();if(getDrawable()!=null&&getWidth()>0&&getHeight()>0){float w=getDrawable().getIntrinsicWidth(),h=getDrawable().getIntrinsicHeight();if(w>0&&h>0){base=Math.min(getWidth()/w,getHeight()/h);transform.setScale(base,base);transform.postTranslate((getWidth()-w*base)/2,(getHeight()-h*base)/2);}}setImageMatrix(transform);}
 private void constrain(){if(getDrawable()==null)return;RectF bounds=new RectF(0,0,getDrawable().getIntrinsicWidth(),getDrawable().getIntrinsicHeight());transform.mapRect(bounds);float dx=0,dy=0;if(bounds.width()<=getWidth())dx=(getWidth()-bounds.width())/2-bounds.left;else if(bounds.left>0)dx=-bounds.left;else if(bounds.right<getWidth())dx=getWidth()-bounds.right;if(bounds.height()<=getHeight())dy=(getHeight()-bounds.height())/2-bounds.top;else if(bounds.top>0)dy=-bounds.top;else if(bounds.bottom<getHeight())dy=getHeight()-bounds.bottom;transform.postTranslate(dx,dy);setImageMatrix(transform);}
 @Override public boolean onTouchEvent(MotionEvent e){int action=e.getActionMasked();if(action==MotionEvent.ACTION_DOWN){multi=false;pointer=e.getPointerId(0);lastX=e.getX();lastY=e.getY();}if(e.getPointerCount()>1)multi=true;
  pinch.onTouchEvent(e);gestures.onTouchEvent(e);
  if(action==MotionEvent.ACTION_MOVE){int index=e.findPointerIndex(pointer);if(index>=0){float x=e.getX(index),y=e.getY(index);if(zoom>1.01f&&!pinch.isInProgress()&&e.getPointerCount()==1){transform.postTranslate(x-lastX,y-lastY);constrain();}lastX=x;lastY=y;}}
  if(action==MotionEvent.ACTION_POINTER_UP){int lifted=e.getActionIndex();if(e.getPointerId(lifted)==pointer){int remaining=lifted==0?1:0;pointer=e.getPointerId(remaining);lastX=e.getX(remaining);lastY=e.getY(remaining);}}
  if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL){pointer=-1;if(zoom<1.01f)resetZoom();}
  return true;
 }
 @Override public boolean performClick(){super.performClick();return true;}
}
