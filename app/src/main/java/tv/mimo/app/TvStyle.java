package tv.mimo.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;

public final class TvStyle {
    public static final int BG=Color.rgb(9,21,22), PANEL=Color.rgb(18,38,39), TEAL=Color.rgb(19,57,57),
        INK=Color.rgb(237,241,231), MUTED=Color.rgb(166,187,177), GREEN=Color.rgb(184,217,119), GOLD=Color.rgb(217,174,81);
    public static int dp(Context c,float value){return Math.round(value*c.getResources().getDisplayMetrics().density);}
    public static GradientDrawable shape(int color,int radius,int stroke){
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(radius);if(stroke!=0)d.setStroke(3,stroke);return d;
    }
    public static TextView text(Context c,String text,int sp,int color){TextView t=new TextView(c);t.setText(text);t.setTextSize(sp);t.setTextColor(color);t.setFontFeatureSettings("kern");return t;}
    public static TextView button(Context c,String label,Runnable action){
        TextView t=text(c,label,15,INK);t.setGravity(Gravity.CENTER_VERTICAL);t.setTypeface(null,Typeface.BOLD);
        t.setPadding(dp(c,16),dp(c,10),dp(c,16),dp(c,10));t.setMinHeight(dp(c,44));focus(t,PANEL);t.setOnClickListener(v->action.run());return t;
    }
    public static void focus(View v,int base){
        v.setFocusable(true);v.setBackground(shape(base,dp(v.getContext(),9),0));
        v.setOnFocusChangeListener((view,focused)->{
            view.setBackground(shape(focused?TEAL:base,dp(view.getContext(),9),focused?GREEN:0));
            view.setElevation(focused?dp(view.getContext(),5):0);
        });
    }
    public static LinearLayout column(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.VERTICAL);return l;}
    public static LinearLayout row(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    public static void immersive(Activity a){a.getWindow().getDecorView().setSystemUiVisibility(
        View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);}
}
