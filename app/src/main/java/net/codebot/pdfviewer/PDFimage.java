package net.codebot.pdfviewer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.*;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.graphics.Paint;

import java.util.ArrayList;
import java.util.Stack;

@SuppressLint("AppCompatCustomView")
public class PDFimage extends ImageView {

    final String LOGNAME = "pdf_image";
    // drawing path
    mPath path = null;
    ArrayList<mPath> paths = new ArrayList<>();

    Stack<ArrayList<mPath>> undos = new Stack<>();
    Stack<ArrayList<mPath>> redos = new Stack<>();
    // image to display
    Bitmap bitmap;
    Paint mpaint;
    int mode=0;
    double start_dis;
    float max_zoom = 15;
    float min_zoom = 0.8f;
    Matrix currentm = new Matrix();
    Matrix lastm = new Matrix();
    PointF midpoint = new PointF();
    double start_x;
    double start_y;
    boolean if_first = true;

    // constructor
    public PDFimage(Context context) {
        super(context);
        mpaint = new Paint();
        midpoint.set(this.getX() + this.getWidth()/2,this.getY() + this.getHeight()/2);
        currentm.setScale(1,1,midpoint.x,midpoint.y);
    }


    // capture touch events (down/move/up) to create a path
    // and use that to create a stroke that we can draw
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(mode==0||mode==1||mode==2) { //draw
            switch (event.getAction()&MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    if(event.getPointerCount()==1) {
                        if(mode==2){
                            Log.d(LOGNAME, "eraser down");
                            Matrix m2 = new Matrix();
                            currentm.invert(m2);
                            float[] before2 = {event.getX(), event.getY()};
                            float[] after2 = {event.getX(), event.getY()};
                            m2.mapPoints(after2, before2);
                            for(mPath p : paths){
                                if(ifhit(after2[0],after2[1],p)){
                                    Log.d(LOGNAME, "hit");
                                    redos.clear();
                                    if(if_first){
                                        if(undos.size()>=10) undos.remove(0);
                                        undos.push(clone(paths));
                                        if_first=false;
                                    }
                                    paths.remove(p);
                                    break;
                                }
                            }
                        } else {
                            Log.d(LOGNAME, "Action down");
                            path = new mPath(mode);
                            path.mpath.reset();
                            Matrix m = new Matrix();
                            currentm.invert(m);
                            float[] before = {event.getX(), event.getY()};
                            float[] after = {event.getX(), event.getY()};
                            m.mapPoints(after, before);
                            path.mpath.moveTo(after[0], after[1]);
                            if(undos.size()>=10) undos.remove(0);
                            undos.push(clone(paths));
                            paths.add(path);
                            redos.clear();
                        }
                    }
                    invalidate();
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    if(event.getPointerCount()==2) {
                        float x_dis = Math.abs(event.getX(0) - event.getX(1));
                        float y_dis = Math.abs(event.getY(0) - event.getY(1));
                        start_dis = Math.sqrt(x_dis * x_dis + y_dis * y_dis);
                        midpoint.set((event.getX(0) + event.getX(1)) / 2, ((event.getY(0) + event.getY(1)) / 2));
                        lastm.set(currentm);
                        Log.d(LOGNAME, "Zoom");
                    }
                    invalidate();
                    break;
                case MotionEvent.ACTION_MOVE:
                    if(event.getPointerCount()==1) {
                        if(mode==2){
                            Matrix m2 = new Matrix();
                            currentm.invert(m2);
                            float[] before2 = {event.getX(), event.getY()};
                            float[] after2 = {event.getX(), event.getY()};
                            m2.mapPoints(after2, before2);
                            for(mPath p : paths){
                                Log.d(LOGNAME, "eraser move");
                                if(ifhit(after2[0],after2[1],p)){
                                    Log.d(LOGNAME, "hit");
                                    redos.clear();
                                    if(if_first){
                                        if(undos.size()>=10) undos.remove(0);
                                        undos.push(clone(paths));
                                        if_first=false;
                                    }
                                    paths.remove(p);
                                    break;
                                }
                            }
                        } else {
                            Log.d(LOGNAME, "Action move");
                            Matrix m2 = new Matrix();
                            currentm.invert(m2);
                            float[] before2 = {event.getX(), event.getY()};
                            float[] after2 = {event.getX(), event.getY()};
                            m2.mapPoints(after2, before2);
                            path.mpath.lineTo(after2[0], after2[1]);
                        }
                    } else if(event.getPointerCount()==2) {
                        float newscale;
                        currentm.set(lastm);
                        float x_dis_n = Math.abs(event.getX(0) - event.getX(1));
                        float y_dis_n = Math.abs(event.getY(0) - event.getY(1));
                        double current_dis = Math.sqrt(x_dis_n*x_dis_n + y_dis_n*y_dis_n);
                        float[] matrixv= new float[9];
                        currentm.getValues(matrixv);
                        float currentscale = matrixv[Matrix.MSCALE_X];
                        newscale = (float) (current_dis/start_dis);
                        if((newscale*currentscale)>max_zoom) newscale=(max_zoom/currentscale);
                        if((newscale*currentscale)<min_zoom) newscale=(min_zoom/currentscale);
                        currentm.postScale(newscale , newscale , midpoint.x, midpoint.y);
                    }
                    invalidate();
                    break;
                case MotionEvent.ACTION_UP:
                    if(event.getPointerCount()==1) {
                        if(mode==2){
                            Log.d(LOGNAME, "Eraser up");
                            if_first=true;
                        } else {
                            Log.d(LOGNAME, "Action up");
                        }
                    } else if(event.getPointerCount()==2) {
                    Log.d(LOGNAME, "Zoom up");
                    }
                    invalidate();
                    break;
            }
        } else if (mode==3){ //pan
            switch (event.getAction()& MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    if(event.getPointerCount()==1) {
                        Log.d(LOGNAME, "Pan Start");
                        start_x = event.getX();
                        start_y = event.getY();
                    }
                    invalidate();
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    if(event.getPointerCount()==2) {
                        float x_dis = Math.abs(event.getX(0) - event.getX(1));
                        float y_dis = Math.abs(event.getY(0) - event.getY(1));
                        start_dis = Math.sqrt(x_dis * x_dis + y_dis * y_dis);
                        midpoint.set((event.getX(0) + event.getX(1)) / 2, ((event.getY(0) + event.getY(1)) / 2));
                        lastm.set(currentm);
                        Log.d(LOGNAME, "Zoom");
                    }
                    invalidate();
                    break;
                case MotionEvent.ACTION_MOVE:
                    if(event.getPointerCount()==2) {
                        float newscale;
                        currentm.set(lastm);
                        float x_dis_n = Math.abs(event.getX(0) - event.getX(1));
                        float y_dis_n = Math.abs(event.getY(0) - event.getY(1));
                        double current_dis = Math.sqrt(x_dis_n * x_dis_n + y_dis_n * y_dis_n);
                        float[] matrixv = new float[9];
                        currentm.getValues(matrixv);
                        float currentscale = matrixv[Matrix.MSCALE_X];
                        newscale = (float) (current_dis / start_dis);
                        if ((newscale * currentscale) > max_zoom) newscale = (max_zoom / currentscale);
                        if ((newscale * currentscale) < min_zoom) newscale = (min_zoom / currentscale);
                        currentm.postScale(newscale, newscale, midpoint.x, midpoint.y);
                    } else if(event.getPointerCount()==1) {
                        Log.d(LOGNAME, "Pan Move");
                        double end_x=event.getX();
                        double end_y=event.getY();
                        currentm.postTranslate((float) (end_x-start_x),(float)(end_y-start_y));
                        start_x=end_x;
                        start_y=end_y;
                    }
                    invalidate();
                    break;
                case MotionEvent.ACTION_UP:
                    if(event.getPointerCount()==2) {
                        Log.d(LOGNAME, "Zoom up");
                    } else if(event.getPointerCount()==1) {
                        Log.d(LOGNAME, "Pan up");
                    }
                    invalidate();
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                    invalidate();
                    break;
            }
        }
        return true;
    }

    public boolean ifhit(float x, float y, mPath path){
       for(int i = 0; i < path.mpath.actions.size()-1;i++) {
           float x1 = path.mpath.actions.get(i).getX();
           float y1 = path.mpath.actions.get(i).getY();
           float x2 = path.mpath.actions.get(i+1).getX();
           float y2 = path.mpath.actions.get(i+1).getY();
           //find distance from point to line
           double a = x - x1;
           double b = y - y1;
           double c = x2 - x1 ;
           double d = y2  - y1 ;

           double e = a * c + b * d;
           double f = c * c + d * d;
           double param = e / f;
           double xx, yy;
           if (param < 0) {
               xx = x1 ;
               yy = y1 ;
           } else if (param > 1) {
               xx = x2 ;
               yy = y2 ;
           } else {
               xx = x1  + param * c;
               yy = y1  + param * d;
           }
           double dx = x - xx;
           double dy = y - yy;
           double distance = Math.sqrt(dx * dx + dy * dy);
           if(path.mode==0 &&distance<=5)return true;
           if(path.mode==1 &&distance<=15)return true;
       }
        return false;
    }

    public ArrayList<mPath> clone(ArrayList<mPath> paths){
        ArrayList<mPath> newp = new ArrayList<>();
        for(mPath p:paths){
            newp.add(p);
        }
        return newp;
    }
    // set image as background
    public void setImage(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public void undo(){
        if(this.undos.size()>=1){
            this.redos.push(clone(paths));
            paths = clone(this.undos.pop());
        }
    }

    public void redo(){
        if(this.redos.size()>=1){
            this.undos.push(clone(paths));
            paths = clone(this.redos.pop());
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // draw background
        if (bitmap != null) {

            this.setImageBitmap(bitmap);
        }

        canvas.setMatrix(currentm);
        // draw lines over it
        for (mPath path : paths) {
            if(path.mode==0){
                this.mpaint.setStyle(Paint.Style.STROKE);
                this.mpaint.setColor(Color.parseColor("#1649B2"));
                this.mpaint.setStrokeWidth(4);

            } else if (path.mode==1){
                this.mpaint.setStyle(Paint.Style.STROKE);
                this.mpaint.setColor(Color.YELLOW);
                this.mpaint.setAlpha(100);
                this.mpaint.setStrokeWidth(30);
            }
            canvas.drawPath(path.mpath, mpaint);
        }
        super.onDraw(canvas);
    }
}
