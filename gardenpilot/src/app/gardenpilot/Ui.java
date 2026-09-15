package app.gardenpilot;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
public final class Ui {
 public static final int BG=Color.rgb(16,26,35),CARD=Color.rgb(28,43,55),TEXT=Color.rgb(237,247,247),MUTED=Color.rgb(166,187,194),MINT=Color.rgb(101,226,183),RED=Color.rgb(244,112,126),GOLD=Color.rgb(251,203,115);
 public static int dp(Context c,float v){return Math.round(v*c.getResources().getDisplayMetrics().density);}
 public static GradientDrawable bg(int color,int radius,Context c){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(c,radius));return d;}
 public static TextView text(Context c,String s,int size,int color){TextView t=new TextView(c);t.setText(s);t.setTextSize(size);t.setTextColor(color);return t;}
 public static Button button(Context c,String s,int color,Runnable action){Button b=new Button(c);b.setText(s);b.setAllCaps(false);b.setTextSize(13);b.setTextColor(color);b.setBackground(bg(CARD,12,c));b.setMinHeight(dp(c,46));b.setMinimumHeight(dp(c,46));b.setPadding(dp(c,8),dp(c,4),dp(c,8),dp(c,4));b.setOnClickListener(v->action.run());return b;}
 public static void add(LinearLayout p,View v){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(p.getContext(),8);p.addView(v,lp);}
 public static LinearLayout column(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.VERTICAL);return l;}
 public static int tileColor(int t){switch(t){case 1:return 0xFFFF7770;case 2:return MINT;case 3:return GOLD;case 4:return 0xFFBE95FF;case 5:return 0xFF65CAFF;case 6:return 0xFFFF9BD6;case 10:case 11:case 12:case 13:case 14:return 0xFFFFFFFF;case 20:case 21:return 0xFFDC9B5F;case 22:case 23:return 0xFFFFAC42;default:return MUTED;}}
}
