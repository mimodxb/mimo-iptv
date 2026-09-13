package tv.mimo.app.mobile;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;

public final class MobileStyle {
    public static final int BG = Color.parseColor("#091516");
    public static final int PANEL = Color.parseColor("#122627");
    public static final int TEAL = Color.parseColor("#133939");
    public static final int INK = Color.parseColor("#EDF1E7");
    public static final int MUTED = Color.parseColor("#A6BBB1");
    public static final int GREEN = Color.parseColor("#B8D977");
    public static final int GOLD = Color.parseColor("#D9AE51");

    public static int dp(Context c, float value) {
        return Math.round(value * c.getResources().getDisplayMetrics().density);
    }

    public static GradientDrawable shape(int color, int radius, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        if (stroke != 0) d.setStroke(3, stroke);
        return d;
    }

    public static TextView text(Context c, String text, int sp, int color) {
        TextView t = new TextView(c);
        t.setText(text);
        t.setTextSize(sp);
        t.setTextColor(color);
        return t;
    }

    public static TextView button(Context c, String label, Runnable action) {
        TextView t = text(c, label, 15, INK);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setTypeface(null, Typeface.BOLD);
        t.setPadding(dp(c, 16), dp(c, 10), dp(c, 16), dp(c, 10));
        t.setMinHeight(dp(c, 44));
        t.setBackground(shape(PANEL, dp(c, 9), 0));
        t.setOnClickListener(v -> action.run());
        return t;
    }

    public static void focus(View v, int base) {
        v.setFocusable(true);
        v.setBackground(shape(base, dp(v.getContext(), 9), 0));
        v.setOnFocusChangeListener((view, focused) -> {
            view.setBackground(shape(focused ? TEAL : base, dp(view.getContext(), 9), focused ? GREEN : 0));
            view.setElevation(focused ? dp(view.getContext(), 5) : 0);
        });
    }

    public static LinearLayout column(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    public static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    public static ImageView icon(Context c, int resId, int sizeDp) {
        ImageView iv = new ImageView(c);
        iv.setImageResource(resId);
        int size = dp(c, sizeDp);
        iv.setLayoutParams(new ViewGroup.LayoutParams(size, size));
        iv.setColorFilter(INK);
        return iv;
    }
}