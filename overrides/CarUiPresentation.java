package com.autolife;

import android.app.Presentation;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * AutoLife home rendered as a Presentation on the private car VirtualDisplay.
 * This avoids Activity launch restrictions on Xiaomi/HyperOS secondary displays.
 */
public final class CarUiPresentation extends Presentation {
    private final CarDisplayController controller;
    private TextView clockView;
    private TextView statusView;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long huDownTime;

    private final Runnable clockTick = new Runnable() {
        @Override public void run() {
            if (clockView != null) {
                clockView.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));
            }
            handler.postDelayed(this, 20_000L);
        }
    };

    public CarUiPresentation(Context context, Display display, CarDisplayController controller) {
        super(context, display);
        this.controller = controller;
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window window = getWindow();
        if (window != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            window.setStatusBarColor(Color.BLACK);
            window.setNavigationBarColor(Color.BLACK);
        }
        buildUi();
        handler.post(clockTick);
    }

    @Override
    public void dismiss() {
        handler.removeCallbacks(clockTick);
        super.dismiss();
    }

    public void setStatus(String text) {
        if (statusView != null) statusView.setText(text == null ? "" : text);
    }

    /** Injects a CarLife touch directly into this Presentation window. */
    public boolean dispatchHuTouch(int action, float x, float y) {
        Window window = getWindow();
        if (window == null || window.getDecorView() == null || !isShowing()) return false;

        final int motionAction;
        long now = SystemClock.uptimeMillis();
        switch (action) {
            case 0 -> {
                motionAction = MotionEvent.ACTION_DOWN;
                huDownTime = now;
            }
            case 1 -> motionAction = MotionEvent.ACTION_UP;
            case 2 -> motionAction = MotionEvent.ACTION_MOVE;
            case 3 -> motionAction = MotionEvent.ACTION_CANCEL;
            default -> { return false; }
        }
        if (huDownTime == 0L) huDownTime = now;
        MotionEvent event = MotionEvent.obtain(huDownTime, now, motionAction, x, y, 0);
        boolean handled = window.getDecorView().dispatchTouchEvent(event);
        event.recycle();
        if (motionAction == MotionEvent.ACTION_UP || motionAction == MotionEvent.ACTION_CANCEL) huDownTime = 0L;
        return handled;
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(getContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(38), dp(24), dp(38), dp(24));
        root.setBackgroundColor(Color.rgb(13, 16, 21));

        LinearLayout header = new LinearLayout(getContext());
        header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout brand = new LinearLayout(getContext());
        brand.setOrientation(LinearLayout.VERTICAL);
        brand.addView(label("AutoLife", 34, Color.WHITE, true));
        brand.addView(label("Независимый автомобильный дисплей", 14, Color.rgb(147, 158, 174), false));
        header.addView(brand, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        clockView = label("--:--", 31, Color.WHITE, true);
        clockView.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        header.addView(clockView, new LinearLayout.LayoutParams(dp(160), LinearLayout.LayoutParams.WRAP_CONTENT));
        root.addView(header);

        LinearLayout hero = new LinearLayout(getContext());
        hero.setOrientation(LinearLayout.HORIZONTAL);
        hero.setGravity(Gravity.CENTER_VERTICAL);
        hero.setPadding(dp(22), dp(12), dp(22), dp(12));
        hero.setBackground(rounded(Color.rgb(25, 31, 41), 22));
        TextView independent = label("●  Телефон независим", 16, Color.rgb(181, 214, 255), true);
        hero.addView(independent, new LinearLayout.LayoutParams(0, dp(50), 1f));
        statusView = label("Car UI • Presentation", 14, Color.rgb(160, 170, 186), false);
        statusView.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        hero.addView(statusView, new LinearLayout.LayoutParams(dp(380), dp(50)));
        LinearLayout.LayoutParams heroLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        heroLp.topMargin = dp(18);
        root.addView(hero, heroLp);

        LinearLayout cards = new LinearLayout(getContext());
        cards.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams cardsLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        cardsLp.topMargin = dp(18);
        root.addView(cards, cardsLp);

        View map = appCard("ru.dublgis.dgismobile", "2ГИС", "Навигация", "Открыть");
        map.setOnClickListener(v -> openApp("ru.dublgis.dgismobile", "2ГИС"));
        cards.addView(map, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f));
        cards.addView(space(dp(18)), new LinearLayout.LayoutParams(dp(18), 1));
        View music = appCard("ru.yandex.music", "Яндекс Музыка", "Музыка", "Открыть");
        music.setOnClickListener(v -> openApp("ru.yandex.music", "Яндекс Музыка"));
        cards.addView(music, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f));

        LinearLayout footer = new LinearLayout(getContext());
        footer.setGravity(Gravity.CENTER_VERTICAL);
        TextView hint = label("Car UI рисуется внутри AutoLife и не зависит от экрана телефона", 14,
                Color.rgb(150, 160, 174), false);
        footer.addView(hint, new LinearLayout.LayoutParams(0, dp(52), 1f));
        TextView badge = label("AutoLife 0.6.1", 14, Color.rgb(183, 191, 204), true);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(rounded(Color.rgb(35, 41, 51), 16));
        footer.addView(badge, new LinearLayout.LayoutParams(dp(170), dp(46)));
        LinearLayout.LayoutParams footerLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        footerLp.topMargin = dp(14);
        root.addView(footer, footerLp);

        setContentView(root);
    }

    private void openApp(String pkg, String title) {
        boolean ok = controller.launchPackageOnCar(pkg);
        setStatus(ok ? title + " запускается…" : title + ": HyperOS запретил secondary display");
    }

    private View appCard(String packageName, String title, String type, String actionText) {
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(30), dp(24), dp(30), dp(24));
        card.setBackground(rounded(Color.rgb(31, 36, 45), 26));
        card.setClickable(true);
        card.setFocusable(true);

        ImageView icon = new ImageView(getContext());
        try {
            Drawable d = getContext().getPackageManager().getApplicationIcon(packageName);
            icon.setImageDrawable(d);
        } catch (Exception e) {
            icon.setImageResource(android.R.drawable.sym_def_app_icon);
        }
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        card.addView(icon, new LinearLayout.LayoutParams(dp(76), dp(76)));

        TextView typeView = label(type.toUpperCase(Locale.getDefault()), 12,
                Color.rgb(124, 174, 255), true);
        LinearLayout.LayoutParams typeLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        typeLp.topMargin = dp(16);
        card.addView(typeView, typeLp);

        TextView titleView = label(title, 29, Color.WHITE, true);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        titleLp.topMargin = dp(3);
        card.addView(titleView, titleLp);

        TextView action = label(actionText + "  ›", 15, Color.rgb(177, 186, 199), false);
        LinearLayout.LayoutParams actionLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        actionLp.topMargin = dp(8);
        card.addView(action, actionLp);
        return card;
    }

    private TextView label(String text, float size, int color, boolean bold) {
        TextView view = new TextView(getContext());
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private View space(int width) {
        Space s = new Space(getContext());
        s.setMinimumWidth(width);
        return s;
    }

    private GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radiusDp));
        return d;
    }

    private int dp(int value) {
        return Math.round(value * getContext().getResources().getDisplayMetrics().density);
    }
}
