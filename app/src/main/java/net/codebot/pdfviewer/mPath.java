package net.codebot.pdfviewer;

import android.graphics.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;

public class mPath implements Serializable{
    sPath mpath;
    int mode;
    mPath(int s){
        mpath=new sPath();
        mode=s;
    };

    //reference https://stackoverflow.com/questions/4919740/how-to-serialize-an-object-of-android-graphics-path/8127953#8127953

    public static class sPath extends Path implements Serializable {
        private static final long serialVersionUID = 1L;
        ArrayList<Action> actions = new ArrayList<>();
        @Override
        public void moveTo(float x, float y) {
            actions.add(new Move(x, y));
            super.moveTo(x, y);
        }
        @Override
        public void lineTo(float x, float y){
            actions.add(new Line(x, y));
            super.lineTo(x, y);
        }

        void updatePath(){
            super.reset();
            for(Action a : actions){
                if(a.type()==1){
                    super.moveTo(a.getX(), a.getY());
                } else if(a.type()==2){
                    super.lineTo(a.getX(), a.getY());
                }
            }
        }

        public interface Action extends Serializable{
            int type();
            float getX();
            float getY();
        }

        public class Move implements Action{
            private static final long serialVersionUID = 2L;
            private float my_x,my_y;
            public Move(float x, float y){
                this.my_x = x;
                this.my_y = y;
            }

            @Override
            public int type() { return 1; }

            @Override
            public float getX() { return my_x; }

            @Override
            public float getY() { return my_y; }
        }
        public class Line implements Action{
            private static final long serialVersionUID = 3L;

            private float my_x,my_y;

            public Line(float x, float y){
                this.my_x = x;
                this.my_y = y;
            }

            @Override
            public int type() { return 2; }

            @Override
            public float getX() { return my_x; }

            @Override
            public float getY() { return my_y; }
        }
    }
}
