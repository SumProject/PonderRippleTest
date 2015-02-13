package halfmoonstudios.com.ripplegl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import java.util.Random;
import android.os.Handler;

/**
 * Created by jerom_000 on 11/02/2015.
 */
public class WaterOverlay extends SurfaceView implements SurfaceHolder.Callback {

    private Swell ripple;
    private WaterThread thread;
    Bitmap rockpool;
    private Bitmap pic = BitmapFactory.decodeResource(getResources(), R.drawable.bgnoripples);
    Paint paint = new Paint();
    private int width = 400;
    private int height = 400;
    private short riprad = 3;
    private boolean reverse;
    private short[] ripplemap, lastMap;
    private int[] td, rd;


    public WaterOverlay(Context context) {
        super(context);
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);

        //gets height and width of screen
        int width = this.getResources().getDisplayMetrics().widthPixels;
        int height = this.getResources().getDisplayMetrics().heightPixels;

        ripple = new JavaSwell();

        initialiseGlobals();

        //Sets thread
        getHolder().addCallback(this);
        setFocusable(true);
    }

    public void initialiseGlobals() {
        int size = width * (height + 2) * 2;
        ripplemap = new short[size];
        lastMap = new short[size];
        Bitmap texture = createBackground(width, height, pic); //creates Mutable pic
        rockpool = texture;
        td = new int[width * height];
        texture.getPixels(td, 0, width, 0, 0, width, height);
        rd = new int[width * height];
    }

    public void randomise() {
        final Random random = new Random();
        final Handler handler = new Handler();
        final Runnable disturbWater = new Runnable() {
            @Override
            public void run() {
                disturb(random.nextInt(width), random.nextInt(height));
                handler.postDelayed(this,7000);
            }
        };
        handler.post(disturbWater);
    }

    private static Bitmap createBackground(int width, int height, Bitmap bm) {
        bm = Bitmap.createScaledBitmap(bm,width,height,false);
        Canvas c = new Canvas(bm);
        c.save();
        c.restore();
        return bm;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = w;
        height = h;
        initialiseGlobals();
    }

    public void onDraw(Canvas canvas){
        //do drawing stuff here
        super.onDraw(canvas);
        newframe();
        canvas.drawBitmap(rockpool, 0, 0, null);

        //canvas.drawBitmap(rockpool,0,0,paint);
    }

    private void disturb(int dx, int dy) {
        ripple.disturb(dx, dy, width, height, riprad, ripplemap, reverse);
    }

    private void newframe() {
        System.arraycopy(td, 0, rd, 0, width * height);
        reverse = !reverse;
        ripple.transformRipples(height, width, ripplemap, lastMap, td, rd, reverse);
        rockpool.setPixels(rd, 0, width, 0, 0, width, height);
    }

    @Override
    public synchronized boolean onTouchEvent(MotionEvent event) {
        disturb((int)event.getX(), (int)event.getY());
        return true;
    }

    @Override
    public void surfaceChanged(SurfaceHolder surface, int first, int second, int third){

    }

    @Override
    public void surfaceCreated(SurfaceHolder surface) {
        thread = new WaterThread(getHolder(),this); //starts Thread that calls onDraw()
        thread.setRunning(true);
        thread.start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surface) {
        boolean retry = true;
        thread.setRunning(false); //stops Thread
        while (retry) {
            try {
                thread.join(); //removes from Memory
                retry = false;
            } catch (InterruptedException e) {}
        }
    }

    class WaterThread extends Thread {
        private SurfaceHolder surfaceHolder;
        private WaterOverlay waterOverlay;
        private boolean running = false;

        public WaterThread(SurfaceHolder surfaceHolder, WaterOverlay waterOverlay) {
            this.surfaceHolder = surfaceHolder;
            this.waterOverlay = waterOverlay;
        }

        public void setRunning(boolean run) {
            running = run;
        }

        public SurfaceHolder getSurfaceHolder() {
            return surfaceHolder;
        }

        @Override
        public void run() {
            Canvas canvas;

            while (running) {
                canvas = null;

                try {
                    canvas = surfaceHolder.lockCanvas(null);
                    synchronized (surfaceHolder) {
                        waterOverlay.onDraw(canvas);
                        //postInvalidate();
                    }
                }
                finally {
                    if (canvas != null) {
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    }
                }
            }
        }
    }
}

