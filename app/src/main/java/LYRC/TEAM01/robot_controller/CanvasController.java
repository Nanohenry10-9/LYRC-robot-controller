package LYRC.TEAM01.robot_controller;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

public class CanvasController extends View {
    class Tuple {
        float x, y;
        Tuple (float ix, float iy) {
            x = ix;
            y = iy;
        }
        public String toString() {
            return "(" + x + ", " + y + ")";
        }
    }

    class Button {
        private int x1, y1, x2, y2;
        private String label;
        private int touchId = -1;
        private boolean pressed = false;
        public Button(int x1, int y1, int x2, int y2, String l) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.label = l;
        }
        public void draw(Canvas canvas) {
            paint.setStyle(Paint.Style.FILL);
            if (pressed) {
                paint.setARGB(255, 15, 15, 15);
            } else {
                paint.setARGB(255, 63, 63, 63);
            }
            canvas.drawRoundRect(new RectF(this.x1, this.y1, this.x2, this.y2), 20, 20, paint);
            paint.setARGB(255, 255, 255, 255);
            paint.setTextSize(48);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(label, (x1 + x2) / 2, (y1 + y2) / 2 + (int)(48 * 0.4), paint);
        }
        public void update(MotionEvent e) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN:
                    boolean cPressed = this.pressed(new Tuple(e.getX(e.getActionIndex()), e.getY(e.getActionIndex())));
                    if (cPressed && !pressed) {
                        touchId = e.getPointerId(e.getActionIndex());
                        pressed = true;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_OUTSIDE:
                    if (touchId == e.getPointerId(e.getActionIndex())) {
                        touchId = -1;
                        pressed = false;
                    }
                    break;
            }
        }
        private boolean pressed(Tuple t) {
            return t.x > this.x1 && t.x < this.x2 && t.y > this.y1 && t.y < this.y2;
        }
        public boolean isPressed() {
            return pressed;
        }
    }

    class Updater extends Thread {
        public boolean running = true;
        private long lastTick;
        public double fps;
        private View view;

        public Updater(View v, double s) {
            view = v;
            fps = s;
        }

        @Override
        public void run() {
            while (running) {
                if (System.currentTimeMillis() - lastTick >= (1000.0 / fps)) {
                    lastTick = System.currentTimeMillis();
                    if (drawCanvas != null) {
                        view.invalidate();
                    }
                }
            }
        }
    }

    private int width;
    private int height;
    private Paint paint;
    private Canvas drawCanvas = null;
    private Updater updater;

    private int slider1Pointer = -1;
    private Tuple slider1PointerLoc;
    private int slider2Pointer = -1;
    private Tuple slider2PointerLoc;

    private double slider1 = 0, slider1t = 0;
    private double slider2 = 0, slider2t = 0;

    private static BLEUtils BLE;

    private ArrayList<Button> buttons;

    public static void setBLE(BLEUtils b) {
        BLE = b;
    }

    private static ArrayList<String> console;

    public static void log(String s) {
        console.add(s);
        if (console.size() > 10) {
            console.remove(0);
        }
    }

    public CanvasController(Context context, AttributeSet as) {
        super(context, as);
        paint = new Paint();
        updater = new Updater(this, 60.0);
        updater.start();
        buttons = new ArrayList<Button>();
        console = new ArrayList<String>();
    }

    private void addButtons() {
        buttons.clear();
        buttons.add(new Button((int)Math.round(width * 0.2), (int)Math.round(height * 0.32), (int)Math.round(width * 0.48), (int)Math.round(height * 0.52), "Open gripper"));
        buttons.add(new Button((int)Math.round(width * 0.52), (int)Math.round(height * 0.32), (int)Math.round(width * 0.8), (int)Math.round(height * 0.52), "Close gripper"));
        buttons.add(new Button((int)Math.round(width * 0.2), (int)Math.round(height * 0.54), (int)Math.round(width * 0.48), (int)Math.round(height * 0.74), "Raise arm"));
        buttons.add(new Button((int)Math.round(width * 0.52), (int)Math.round(height * 0.54), (int)Math.round(width * 0.8), (int)Math.round(height * 0.74), "Lower arm"));
        buttons.add(new Button((int)Math.round(width * 0.2), (int)Math.round(height * 0.76), (int)Math.round(width * 0.48), (int)Math.round(height * 0.96), "Check RFID"));
        buttons.add(new Button((int)Math.round(width * 0.52), (int)Math.round(height * 0.76), (int)Math.round(width * 0.8), (int)Math.round(height * 0.96), "Check base"));
    }

    @Override
    protected void onMeasure(int w, int h) {
        width = View.MeasureSpec.getSize(w);
        height = View.MeasureSpec.getSize(h);
        setMeasuredDimension(width, height);
        addButtons();
    }

    @Override
    protected void onSizeChanged(int w, int h, int a, int b) {
        setMeasuredDimension(w, h);
        addButtons();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawCanvas = canvas;
        paint.setStyle(Paint.Style.STROKE);
        paint.setARGB(255, 0, 0, 0);
        paint.setStyle(Paint.Style.FILL);
        paint.setARGB(255, 0, 0, 255);
        canvas.drawRect(0, 0, width, height, paint);
        paint.setARGB(255, 128, 128, 128);
        canvas.drawRoundRect(new RectF((float)(width * 0.05), (float)(height * 0.05), (float)(width * 0.15), (float)(height * 0.95)), 20, 20, paint);
        canvas.drawRoundRect(new RectF((float)(width * 0.85), (float)(height * 0.05), (float)(width * 0.95), (float)(height * 0.95)), 20, 20, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setARGB(255, 63, 63, 63);
        canvas.drawRoundRect(new RectF((float)(width * 0.2), (float)(-20), (float)(width * 0.8), (float)(height * 0.3)), 20, 20, paint);
        paint.setARGB(255, 255, 255, 255);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTextSize(28);
        for (int i = console.size() - 1; i >= 0; i--) {
            canvas.drawText(console.get(i), (float)(width * 0.2 + 50), (float)(height * 0.3 - 50 - (console.size() - i - 1) * 40 + (28 * 0.4)), paint);
        }
        paint.setARGB(255, 63, 63, 63);
        float slider1Mapped = (float)map(slider1, -100, 100, height * 0.05 + 100, height * 0.95 - 100);
        canvas.drawRoundRect(new RectF((float)(width * 0.05), slider1Mapped - 100, (float)(width * 0.15), slider1Mapped + 100), 20, 20, paint);
        float slider2Mapped = (float)map(slider2, -100, 100, height * 0.05 + 100, height * 0.95 - 100);
        canvas.drawRoundRect(new RectF((float)(width * 0.85), slider2Mapped - 100, (float)(width * 0.95), slider2Mapped + 100), 20, 20, paint);
        for (int i = 0; i < buttons.size(); i++) {
            buttons.get(i).draw(canvas);
        }

        if (slider1Pointer != -1) {
            slider1t = constrain(map(slider1PointerLoc.y, height * 0.05 + 100, height * 0.95 - 100, -100, 100), -100, 100);
        } else {
            slider1t = 0;
        }
        if (slider2Pointer != -1) {
            slider2t = constrain(map(slider2PointerLoc.y, height * 0.05 + 100, height * 0.95 - 100, -100, 100), -100, 100);
        } else {
            slider2t = 0;
        }
        slider1 += (slider1t - slider1) * 0.2;
        slider2 += (slider2t - slider2) * 0.2;

        byte d[] = {
                (byte)101,
                (byte)(buttons.get(0).isPressed()? 1:0),
                (byte)(buttons.get(1).isPressed()? 1:0),
                (byte)(buttons.get(2).isPressed()? 1:0),
                (byte)(buttons.get(3).isPressed()? 1:0),
                (byte)(buttons.get(4).isPressed()? 1:0),
                (byte)(buttons.get(5).isPressed()? 1:0),
                (byte)Math.round(-slider1),
                (byte)Math.round(-slider2)
        };
        BLE.sendData(d);
    }

    double map(double v, double o1, double o2, double n1, double n2) {
        return n1 + ((v - o1) / (o2 - o1)) * (n2 - n1);
    }

    double constrain(double v, double l, double u) {
        if (v > u) {
            return u;
        }
        if (v < l) {
            return l;
        }
        return v;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        for (int i = 0; i < buttons.size(); i++) {
            buttons.get(i).update(e);
        }
        int index = e.getActionIndex();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                if (e.getX(index) > width * 0.05 && e.getY(index) > height * 0.05 &&
                        e.getX(index) < width * 0.15 && e.getY(index) < height * 0.95 &&
                        slider1Pointer == -1) {
                    slider1Pointer = e.getPointerId(index);
                    slider1PointerLoc = new Tuple(e.getX(index), e.getY(index));
                } else if (e.getX(index) > width * 0.85 && e.getY(index) > height * 0.05 &&
                        e.getX(index) < width * 0.95 && e.getY(index) < height * 0.95 &&
                        slider2Pointer == -1) {
                    slider2Pointer = e.getPointerId(index);
                    slider2PointerLoc = new Tuple(e.getX(index), e.getY(index));
                }
                break;
            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < e.getPointerCount(); i++) {
                    if (e.getPointerId(i) == slider1Pointer) {
                        slider1PointerLoc = new Tuple(e.getX(i), e.getY(i));
                    } else if (e.getPointerId(i) == slider2Pointer) {
                        slider2PointerLoc = new Tuple(e.getX(i), e.getY(i));
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_OUTSIDE:
                if (e.getPointerId(index) == slider1Pointer) {
                    slider1Pointer = -1;
                } else if (e.getPointerId(index) == slider2Pointer) {
                    slider2Pointer = -1;
                }
                break;
        }
        return true;
    }
}