package com.icssettings.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Replica of the Android 4.0 (ICS) "About phone" easter egg. Tapping
 * "Android version" seven times from the About panel launches this full-screen
 * view: the classic green Android robot with the "Ice Cream Sandwich" wordmark.
 * Tapping (or flinging) the screen releases little Android creatures that
 * bounce around — mirroring the ICS platypus/bean behaviour.
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
            getActionBar().setTitle("Ice Cream Sandwich");
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
        Toast.makeText(this, "Tap to release little Androids", Toast.LENGTH_LONG).show();
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
        private final Paint mRobot = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mDark = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mText = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final List<Creature> mCreatures = new ArrayList<>();
        private final Random mRand = new Random();
        private float mDownX, mDownY, mLastX, mLastY, mDownT;

        EggView(Context c) {
            super(c);
            mRobot.setColor(Color.parseColor("#a4c639"));
            mDark.setColor(Color.parseColor("#333333"));
            mText.setColor(Color.parseColor("#4a6b1f"));
            mText.setTextAlign(Paint.Align.CENTER);
            mText.setTextSize(34);
        }

        boolean hasCreatures() {
            return !mCreatures.isEmpty();
        }

        private void spawn(float x, float y, float vx, float vy) {
            Creature cr = new Creature();
            cr.x = x; cr.y = y; cr.vx = vx; cr.vy = vy;
            cr.r = 18 + mRand.nextFloat() * 14;
            cr.rot = mRand.nextFloat() * 360;
            mCreatures.add(cr);
            if (mCreatures.size() > 60) mCreatures.remove(0);
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mDownX = mLastX = e.getX();
                    mDownY = mLastY = e.getY();
                    mDownT = e.getEventTime();
                    break;
                case MotionEvent.ACTION_UP: {
                    float dt = Math.max(1, e.getEventTime() - mDownT);
                    float vx = (e.getX() - mDownX) / dt * 16f;
                    float vy = (e.getY() - mDownY) / dt * 16f;
                    float dist = (float) Math.hypot(e.getX() - mDownX, e.getY() - mDownY);
                    if (dist < 8) {
                        // a plain tap -> random fling
                        double a = mRand.nextDouble() * Math.PI * 2;
                        vx = (float) Math.cos(a) * (2 + mRand.nextFloat() * 4);
                        vy = (float) Math.sin(a) * (2 + mRand.nextFloat() * 4);
                    }
                    spawn(e.getX(), e.getY(), vx, vy);
                    break;
                }
                default:
                    mLastX = e.getX();
                    mLastY = e.getY();
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
            canvas.drawText("Ice Cream Sandwich", w / 2f, h * 0.16f, mText);
            mText.setTextSize(Math.min(28, w / 20f));
            canvas.drawText("Android 4.0", w / 2f, h * 0.16f + 40, mText);

            // Big Android robot in the center
            drawAndroid(canvas, w / 2f, h * 0.5f, Math.min(w, h) * 0.28f);

            // Hint
            mText.setTextSize(Math.min(20, w / 28f));
            canvas.drawText("Tap the screen to release little Androids", w / 2f, h * 0.9f, mText);

            // Update + draw creatures
            float pad = 24;
            for (int i = mCreatures.size() - 1; i >= 0; i--) {
                Creature cr = mCreatures.get(i);
                cr.x += cr.vx;
                cr.y += cr.vy;
                if (cr.x < pad) { cr.x = pad; cr.vx = Math.abs(cr.vx); }
                if (cr.x > w - pad) { cr.x = w - pad; cr.vx = -Math.abs(cr.vx); }
                if (cr.y < pad) { cr.y = pad; cr.vy = Math.abs(cr.vy); }
                if (cr.y > h - pad) { cr.y = h - pad; cr.vy = -Math.abs(cr.vy); }
                drawCreature(canvas, cr);
            }
        }

        private void drawAndroid(Canvas c, float cx, float cy, float s) {
            // head
            float hw = s * 0.9f, hh = s * 0.7f;
            RectF head = new RectF(cx - hw / 2, cy - hh * 1.25f, cx + hw / 2, cy - hh * 0.25f);
            c.drawRoundRect(head, s * 0.18f, s * 0.18f, mRobot);
            // eyes
            float ey = cy - hh * 0.85f;
            c.drawCircle(cx - hw * 0.22f, ey, s * 0.1f, mDark);
            c.drawCircle(cx + hw * 0.22f, ey, s * 0.1f, mDark);
            // antennae
            c.drawLine(cx - hw * 0.28f, head.top + s * 0.05f, cx - hw * 0.42f, head.top - s * 0.3f, mRobot);
            c.drawLine(cx + hw * 0.28f, head.top + s * 0.05f, cx + hw * 0.42f, head.top - s * 0.3f, mRobot);
            c.drawCircle(cx - hw * 0.42f, head.top - s * 0.3f, s * 0.07f, mDark);
            c.drawCircle(cx + hw * 0.42f, head.top - s * 0.3f, s * 0.07f, mDark);
            // body
            RectF body = new RectF(cx - hw * 0.6f, cy - hh * 0.2f, cx + hw * 0.6f, cy + hh * 0.7f);
            c.drawRoundRect(body, s * 0.2f, s * 0.2f, mRobot);
            // arms
            c.drawRoundRect(new RectF(cx - hw * 0.85f, cy - hh * 0.05f, cx - hw * 0.6f, cy + hh * 0.5f),
                    s * 0.12f, s * 0.12f, mRobot);
            c.drawRoundRect(new RectF(cx + hw * 0.6f, cy - hh * 0.05f, cx + hw * 0.85f, cy + hh * 0.5f),
                    s * 0.12f, s * 0.12f, mRobot);
            // legs
            c.drawRoundRect(new RectF(cx - hw * 0.4f, cy + hh * 0.7f, cx - hw * 0.15f, cy + hh * 1.15f),
                    s * 0.1f, s * 0.1f, mRobot);
            c.drawRoundRect(new RectF(cx + hw * 0.15f, cy + hh * 0.7f, cx + hw * 0.4f, cy + hh * 1.15f),
                    s * 0.1f, s * 0.1f, mRobot);
        }

        private void drawCreature(Canvas c, Creature cr) {
            float s = cr.r;
            c.save();
            c.translate(cr.x, cr.y);
            RectF head = new RectF(-s * 0.5f, -s * 0.5f, s * 0.5f, s * 0.2f);
            c.drawRoundRect(head, s * 0.15f, s * 0.15f, mRobot);
            c.drawCircle(-s * 0.18f, -s * 0.1f, s * 0.08f, mDark);
            c.drawCircle(s * 0.18f, -s * 0.1f, s * 0.08f, mDark);
            c.drawLine(-s * 0.25f, -s * 0.5f, -s * 0.4f, -s * 0.8f, mRobot);
            c.drawLine(s * 0.25f, -s * 0.5f, s * 0.4f, -s * 0.8f, mRobot);
            c.drawCircle(-s * 0.4f, -s * 0.8f, s * 0.06f, mDark);
            c.drawCircle(s * 0.4f, -s * 0.8f, s * 0.06f, mDark);
            c.restore();
        }
    }

    private static class Creature {
        float x, y, vx, vy, r, rot;
    }
}
