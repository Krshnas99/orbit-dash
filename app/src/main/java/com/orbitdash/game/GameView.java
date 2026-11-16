package com.orbitdash.game;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import java.util.Random;

/** Orbit Dash: tap to reverse direction on the ring, collect gold, dodge red sparks. */
public class GameView extends View {
    private static final int BG = 0xFF0B1020, RING = 0xFF2A3566, CYAN = 0xFF4DE8FF,
            GOLD = 0xFFFFD54A, RED = 0xFFFF4D6D, WHITE = 0xFFFFFFFF, PINK = 0xFFB86BFF;
    private static final int READY = 0, PLAY = 1, OVER = 2;
    private static final int MAXP = 24, MAXQ = 140, TRAIL = 18, STARS = 50;
    private static final float PI = (float) Math.PI;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random rnd = new Random();
    private final SharedPreferences prefs;

    private float w, h, cx, cy, ringR, u; // u = 1% of screen width
    private int state = READY, score, best, dir = 1;
    private boolean newBest;
    private float angle = -PI / 2, gemAngle, clock, spawnTimer, overTime, shake, gemPop;
    private long last;

    // projectiles (sparks fired from the core): angle, radius, speed
    private final float[] pa = new float[MAXP], pr = new float[MAXP], pv = new float[MAXP];
    private final boolean[] pOn = new boolean[MAXP];
    // particles
    private final float[] qx = new float[MAXQ], qy = new float[MAXQ], qvx = new float[MAXQ],
            qvy = new float[MAXQ], ql = new float[MAXQ], qm = new float[MAXQ];
    private final int[] qc = new int[MAXQ];
    // player trail
    private final float[] tx = new float[TRAIL], ty = new float[TRAIL];
    private int tHead;
    // background stars (fractions of the screen)
    private final float[] sx = new float[STARS], sy = new float[STARS], sp = new float[STARS];

    public GameView(Context c) {
        super(c);
        prefs = c.getSharedPreferences("orbitdash", Context.MODE_PRIVATE);
        best = prefs.getInt("best", 0);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        text.setTextAlign(Paint.Align.CENTER);
        text.setFakeBoldText(true);
        for (int i = 0; i < STARS; i++) {
            sx[i] = rnd.nextFloat();
            sy[i] = rnd.nextFloat();
            sp[i] = rnd.nextFloat() * 6f;
        }
        setKeepScreenOn(true);
    }

    @Override
    protected void onSizeChanged(int nw, int nh, int ow, int oh) {
        w = nw;
        h = nh;
        u = w / 100f;
        cx = w / 2f;
        cy = h * 0.56f;
        ringR = Math.min(w * 0.38f, h * 0.30f);
        resetTrail();
        newGem();
    }

    private static int alpha(int color, float a) {
        if (a < 0) a = 0;
        if (a > 1) a = 1;
        return (color & 0x00FFFFFF) | ((int) (a * 255) << 24);
    }

    private float speed() { return Math.min(1.9f + score * 0.05f, 3.8f); }
    private float px() { return cx + (float) Math.cos(angle) * ringR; }
    private float py() { return cy + (float) Math.sin(angle) * ringR; }

    private void resetTrail() {
        for (int i = 0; i < TRAIL; i++) { tx[i] = px(); ty[i] = py(); }
    }

    private void newGem() {
        gemAngle = angle + PI / 2 + rnd.nextFloat() * PI; // 90..270 degrees away
        gemPop = 0;
    }

    private void start() {
        score = 0;
        newBest = false;
        dir = 1;
        angle = -PI / 2;
        spawnTimer = 1.6f;
        for (int i = 0; i < MAXP; i++) pOn[i] = false;
        resetTrail();
        newGem();
        state = PLAY;
    }

    private void burst(float x, float y, int color, int n, float power) {
        for (int i = 0; i < MAXQ && n > 0; i++) {
            if (ql[i] > 0) continue;
            float a = rnd.nextFloat() * 2 * PI, s = (0.3f + rnd.nextFloat()) * power * u;
            qx[i] = x; qy[i] = y;
            qvx[i] = (float) Math.cos(a) * s;
            qvy[i] = (float) Math.sin(a) * s;
            qm[i] = ql[i] = 0.4f + rnd.nextFloat() * 0.5f;
            qc[i] = color;
            n--;
        }
    }

    private void fire() {
        float travel = Math.max(0.75f, 1.15f - score * 0.01f); // seconds from core to ring
        float lead = speed() * travel * dir;
        if (rnd.nextFloat() < 0.3f) lead = -lead;           // sometimes punish a reversal
        for (int i = 0; i < MAXP; i++) {
            if (pOn[i]) continue;
            pOn[i] = true;
            pa[i] = angle + lead + (rnd.nextFloat() - 0.5f) * 0.5f;
            pr[i] = ringR * 0.12f;
            pv[i] = ringR * 0.88f / travel;
            return;
        }
    }

    private void update(float dt) {
        clock += dt;
        gemPop = Math.min(1f, gemPop + dt * 4f);
        if (shake > 0) shake = Math.max(0, shake - dt * 2.5f);

        if (state != OVER) {
            angle += dir * (state == PLAY ? speed() : 1.2f) * dt;
            tHead = (tHead + 1) % TRAIL;
            tx[tHead] = px();
            ty[tHead] = py();
        } else {
            overTime += dt;
        }

        if (state == PLAY) {
            spawnTimer -= dt;
            if (spawnTimer <= 0) {
                fire();
                spawnTimer = Math.max(0.5f, 1.3f - score * 0.025f);
            }
            float gx = cx + (float) Math.cos(gemAngle) * ringR, gy = cy + (float) Math.sin(gemAngle) * ringR;
            if (dist(px(), py(), gx, gy) < 7.5f * u) {
                score++;
                burst(gx, gy, GOLD, 14, 45);
                performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                newGem();
            }
        }

        for (int i = 0; i < MAXP; i++) {
            if (!pOn[i]) continue;
            pr[i] += pv[i] * dt;
            if (pr[i] > Math.max(w, h)) { pOn[i] = false; continue; }
            if (state == PLAY) {
                float x = cx + (float) Math.cos(pa[i]) * pr[i], y = cy + (float) Math.sin(pa[i]) * pr[i];
                if (dist(px(), py(), x, y) < 4.6f * u) { die(); }
            }
        }

        for (int i = 0; i < MAXQ; i++) {
            if (ql[i] <= 0) continue;
            ql[i] -= dt;
            qx[i] += qvx[i] * dt;
            qy[i] += qvy[i] * dt;
            qvx[i] *= 0.96f;
            qvy[i] *= 0.96f;
        }
    }

    private static float dist(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2, dy = y1 - y2;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private void die() {
        state = OVER;
        overTime = 0;
        shake = 1f;
        burst(px(), py(), CYAN, 40, 70);
        burst(px(), py(), WHITE, 12, 40);
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        if (score > best) {
            best = score;
            newBest = true;
            prefs.edit().putInt("best", best).apply();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            if (state == PLAY) {
                dir = -dir;
                burst(px(), py(), CYAN, 5, 22);
            } else if (state == READY || overTime > 0.6f) {
                start();
            }
        }
        return true;
    }

    private void glow(Canvas c, float x, float y, float r, int color) {
        fill.setColor(alpha(color, 0.10f));
        c.drawCircle(x, y, r * 2.6f, fill);
        fill.setColor(alpha(color, 0.22f));
        c.drawCircle(x, y, r * 1.7f, fill);
        fill.setColor(color);
        c.drawCircle(x, y, r, fill);
    }

    @Override
    protected void onDraw(Canvas c) {
        long now = System.nanoTime();
        float dt = last == 0 ? 0 : (now - last) / 1e9f;
        last = now;
        if (dt > 0.033f) dt = 0.033f; // also absorbs pauses when the app is in the background
        if (w > 0) update(dt);

        c.drawColor(BG);
        if (w == 0) { postInvalidateOnAnimation(); return; }

        c.save();
        if (shake > 0) {
            c.translate((rnd.nextFloat() - 0.5f) * shake * 5 * u, (rnd.nextFloat() - 0.5f) * shake * 5 * u);
        }

        // stars
        for (int i = 0; i < STARS; i++) {
            fill.setColor(alpha(WHITE, 0.15f + 0.25f * (float) Math.abs(Math.sin(clock * 0.8f + sp[i]))));
            c.drawCircle(sx[i] * w, sy[i] * h, (0.2f + (i % 3) * 0.12f) * u, fill);
        }

        // ring
        stroke.setColor(alpha(RING, 0.35f));
        stroke.setStrokeWidth(4.5f * u);
        c.drawCircle(cx, cy, ringR, stroke);
        stroke.setColor(RING);
        stroke.setStrokeWidth(0.9f * u);
        c.drawCircle(cx, cy, ringR, stroke);

        // core
        float pulse = 1f + 0.12f * (float) Math.sin(clock * 5f);
        if (state == PLAY) pulse += 0.5f * Math.max(0, 0.25f - spawnTimer); // swell before firing
        glow(c, cx, cy, ringR * 0.10f * pulse, state == PLAY ? RED : PINK);

        // gem
        float gx = cx + (float) Math.cos(gemAngle) * ringR, gy = cy + (float) Math.sin(gemAngle) * ringR;
        glow(c, gx, gy, (2.4f + 0.4f * (float) Math.sin(clock * 6f)) * u * gemPop, GOLD);

        // sparks
        for (int i = 0; i < MAXP; i++) {
            if (!pOn[i]) continue;
            float ca = (float) Math.cos(pa[i]), sa = (float) Math.sin(pa[i]);
            float x = cx + ca * pr[i], y = cy + sa * pr[i];
            float tail = Math.min(pr[i], 9 * u);
            stroke.setColor(alpha(RED, 0.45f));
            stroke.setStrokeWidth(1.6f * u);
            c.drawLine(x, y, x - ca * tail, y - sa * tail, stroke);
            glow(c, x, y, 2.0f * u, RED);
        }

        // player + trail
        if (state != OVER) {
            for (int i = 1; i < TRAIL; i++) {
                int k = (tHead - i + TRAIL * 2) % TRAIL;
                float f = 1f - i / (float) TRAIL;
                fill.setColor(alpha(CYAN, 0.35f * f));
                c.drawCircle(tx[k], ty[k], 3.0f * u * f, fill);
            }
            glow(c, px(), py(), 3.0f * u, CYAN);
            fill.setColor(WHITE);
            c.drawCircle(px(), py(), 1.3f * u, fill);
        }

        // particles
        for (int i = 0; i < MAXQ; i++) {
            if (ql[i] <= 0) continue;
            float f = ql[i] / qm[i];
            fill.setColor(alpha(qc[i], f));
            c.drawCircle(qx[i], qy[i], 1.2f * u * f + 0.3f * u, fill);
        }
        c.restore();

        drawHud(c);
        postInvalidateOnAnimation();
    }

    private void label(Canvas c, String s, float y, float size, int color) {
        text.setTextSize(size * u);
        text.setColor(color);
        c.drawText(s, cx, y, text);
    }

    private void drawHud(Canvas c) {
        float top = cy - ringR; // space above the ring
        float blink = 0.55f + 0.45f * (float) Math.sin(clock * 4f);
        if (state == READY) {
            label(c, "ORBIT DASH", top * 0.45f, 13, CYAN);
            label(c, "Tap anywhere to reverse direction", top * 0.45f + 8 * u, 4.4f, alpha(WHITE, 0.8f));
            label(c, "Collect gold  •  Dodge red", top * 0.45f + 15 * u, 4.4f, alpha(WHITE, 0.8f));
            label(c, "TAP TO START", cy + ringR + 16 * u, 6, alpha(GOLD, blink));
            if (best > 0) label(c, "BEST " + best, cy + ringR + 25 * u, 4.4f, alpha(WHITE, 0.6f));
        } else if (state == PLAY) {
            label(c, String.valueOf(score), top * 0.55f, 18, WHITE);
            label(c, "BEST " + best, top * 0.55f + 7 * u, 4, alpha(WHITE, 0.55f));
        } else {
            label(c, "GAME OVER", top * 0.40f, 11, RED);
            label(c, String.valueOf(score), top * 0.40f + 20 * u, 18, WHITE);
            label(c, newBest ? "NEW BEST!" : "BEST " + best, top * 0.40f + 28 * u, 4.6f,
                    newBest ? GOLD : alpha(WHITE, 0.6f));
            if (overTime > 0.6f) label(c, "TAP TO RETRY", cy + ringR + 16 * u, 6, alpha(GOLD, blink));
        }
    }
}
