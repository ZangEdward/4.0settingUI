package com.icssettings.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Android 4.0 (ICS) "About phone" easter egg. Tapping
 * "Android version" seven times from the About panel launches this full-screen
 * view: the classic green Android robot with the "Ice Cream Sandwich" wordmark.
 * Tapping (or flinging) the screen releases little Android creatures that
 * bounce around — mirroring the ICS platypus/bean behaviour.
 *
 * <p>This implementation uses a real drawable bitmap asset
 * ({@code android_robot.png}) generated to the official Android robot geometry,
 * instead of runtime Canvas shape drawing, so the mascot is crisp and clean.
 */
public class EasterEggActivity extends Activity {

    private EggView mView;
    private final Handler mTick = new Handler();
    private final Runnable mLoop = new Runnable() {
        @Override
        public void run() {
            if (mView != null && mView.hasCreatures()) {
                mView.invalidate();
                mTick.postDelayed(this, 16);
            }
        }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        mView = new EggView(this);
        setContentView(mView);
        if (getActionBar() != null) {
            getActionBar().setTitle(getString(R.string.egg_title));
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
        Toast.makeText(this, R.string.egg_toast, Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mTick.removeCallbacks(mLoop);
        mTick.postDelayed(mLoop, 16);
    }

    @Override
    protected void onPause() {
        mTick.removeCallbacks(mLoop);
        super.onPause();
    }

    // ----------------------------------------------------------------- view
    private static class EggView extends View {
        private final Bitmap mRobot;
        private final int mRobotW;
        private final int mRobotH;
        private final Paint mText = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mDarkText = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final List<Creature> mCreatures = new ArrayList<>();
        private final Random mRand = new Random();
        private float mDownX, mDownY, mDownT;

        EggView(Context c) {
            super(c);
            Bitmap src = BitmapFactory.decodeResource(c.getResources(), R.drawable.android_robot);
            if (src == null) {
                src = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
            }
            mRobot = src;
            mRobotW = mRobot.getWidth();
            mRobotH = mRobot.getHeight();
            mText.setColor(Color.parseColor("#4a6b1f"));
            mText.setTextAlign(Paint.Align.CENTER);
            mText.setTextSize(34);
            mDarkText.setColor(Color.parseColor("#777777"));
            mDarkText.setTextAlign(Paint.Align.CENTER);
            mDarkText.setTextSize(20);
        }

        boolean hasCreatures() {
            return !mCreatures.isEmpty();
        }

        private void spawn(float x, float y, float vx, float vy) {
            Creature cr = new Creature();
            cr.x = x;
            cr.y = y;
            cr.vx = vx;
            cr.vy = vy;
            cr.r = 24 + mRand.nextFloat() * 22;
            cr.rot = mRand.nextFloat() * 360;
            mCreatures.add(cr);
            if (mCreatures.size() > 60) mCreatures.remove(0);
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mDownX = e.getX();
                    mDownY = e.getY();
                    mDownT = e.getEventTime();
                    break;
                case MotionEvent.ACTION_UP: {
                    float dt = Math.max(1, e.getEventTime() - mDownT);
                    float vx = (e.getX() - mDownX) / dt * 16f;
                    float vy = (e.getY() - mDownY) / dt * 16f;
                    float dist = (float) Math.hypot(e.getX() - mDownX, e.getY() - mDownY);
                    if (dist < 8) {
                        double a = mRand.nextDouble() * Math.PI * 2;
                        vx = (float) Math.cos(a) * (2 + mRand.nextFloat() * 4);
                        vy = (float) Math.sin(a) * (2 + mRand.nextFloat() * 4);
                    }
                    spawn(e.getX(), e.getY(), vx, vy);
                    break;
                }
            }
            return true;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int w = getWidth(), h = getHeight();
            // Ice-cream-sandwich cream background
            canvas.drawColor(Color.parseColor("#fdf6e3"));

            // Title wordmark
            mText.setTextSize(Math.min(46, w / 12f));
            canvas.drawText(getContext().getString(R.string.egg_title), w / 2f, h * 0.16f, mText);
            mText.setTextSize(Math.min(28, w / 20f));
            canvas.drawText(getContext().getString(R.string.egg_subtitle), w / 2f, h * 0.16f + 44, mText);

            // Big Android robot in the center
            float bigSize = Math.min(w, h) * 0.42f;
            drawRobot(canvas, w / 2f, h * 0.52f, bigSize);

            // Hint
            mDarkText.setTextSize(Math.min(20, w / 28f));
            canvas.drawText(getContext().getString(R.string.egg_hint), w / 2f, h * 0.9f, mDarkText);

            // Update + draw creatures
            float pad = mRobotW * 0.15f;
            for (int i = mCreatures.size() - 1; i >= 0; i--) {
                Creature cr = mCreatures.get(i);
                cr.x += cr.vx;
                cr.y += cr.vy;
                if (cr.x < pad) { cr.x = pad; cr.vx = Math.abs(cr.vx); }
                if (cr.x > w - pad) { cr.x = w - pad; cr.vx = -Math.abs(cr.vx); }
                if (cr.y < pad) { cr.y = pad; cr.vy = Math.abs(cr.vy); }
                if (cr.y > h - pad) { cr.y = h - pad; cr.vy = -Math.abs(cr.vy); }
                drawRobot(canvas, cr.x, cr.y, cr.r * 2);
            }
        }

        private void drawRobot(Canvas c, float cx, float cy, float targetHeight) {
            float scale = targetHeight / mRobotH;
            float dw = mRobotW * scale;
            float dh = mRobotH * scale;
            c.drawBitmap(mRobot, cx - dw / 2f, cy - dh / 2f, null);
        }
    }

    private static class Creature {
        float x, y, vx, vy, r, rot;
    }
}
