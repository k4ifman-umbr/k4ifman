package app.gardenpilot;
import android.content.Context;
import org.json.*;
import java.util.*;

public final class Profile {
 public int cols=11,rows=9,goal=Engine.GOAL_HONEY,limit=30;
 public float left=430f/1536,top=65f/709,right=1154f/1536,bottom=644f/709;
 public boolean[] mask=new boolean[99];public boolean verified=false;
 public final ArrayList<Sample> samples=new ArrayList<>();
 public static final class Sample{public final int type;public final float[] feature;public Sample(int t,float[] f){type=t;feature=f;}}
 public Profile(){Arrays.fill(mask,true);for(int r:new int[]{0,8})for(int c=0;c<11;c++)if(c<3||c>7)mask[r*11+c]=false;}
 public void resize(int c,int r){cols=c;rows=r;mask=new boolean[c*r];Arrays.fill(mask,true);verified=false;}
 public float x(int i){return left+(right-left)*(i%cols)/(cols-1);}
 public float y(int i){return top+(bottom-top)*(i/cols)/(rows-1);}
 public void add(int type,float[] f){int n=0;for(int i=samples.size()-1;i>=0;i--)if(samples.get(i).type==type&&++n>=5)samples.remove(i);samples.add(new Sample(type,f));verified=false;}
 public void save(Context c){try{
  JSONObject j=new JSONObject();j.put("cols",cols);j.put("rows",rows);j.put("goal",goal);j.put("limit",limit);
  j.put("left",left);j.put("right",right);j.put("top",top);j.put("bottom",bottom);j.put("verified",verified);
  JSONArray m=new JSONArray();for(boolean v:mask)m.put(v);j.put("mask",m);JSONArray a=new JSONArray();
  for(Sample s:samples){JSONObject o=new JSONObject();o.put("t",s.type);JSONArray d=new JSONArray();for(float v:s.feature)d.put(v);o.put("f",d);a.put(o);}j.put("samples",a);
  c.getSharedPreferences("profile",0).edit().putString("json",j.toString()).apply();
 }catch(JSONException e){throw new IllegalStateException(e);}}
 public static Profile load(Context c){Profile p=new Profile();try{
  String text=c.getSharedPreferences("profile",0).getString("json","");if(text.isEmpty())return p;JSONObject j=new JSONObject(text);
  int cols=j.getInt("cols"),rows=j.getInt("rows");if(cols<3||rows<3||cols>16||rows>16)return p;p.resize(cols,rows);
  p.goal=Math.max(0,Math.min(8,j.optInt("goal",1)));p.limit=Math.max(1,Math.min(200,j.optInt("limit",30)));
  p.left=(float)j.getDouble("left");p.right=(float)j.getDouble("right");p.top=(float)j.getDouble("top");p.bottom=(float)j.getDouble("bottom");
  if(p.left<0||p.top<0||p.right>1||p.bottom>1||p.right-p.left<.05||p.bottom-p.top<.05)return new Profile();
  JSONArray m=j.getJSONArray("mask");if(m.length()!=cols*rows)return new Profile();for(int i=0;i<m.length();i++)p.mask[i]=m.getBoolean(i);p.verified=j.optBoolean("verified",false);
  JSONArray a=j.optJSONArray("samples");if(a!=null)for(int i=0;i<Math.min(a.length(),120);i++){
   JSONObject o=a.getJSONObject(i);JSONArray v=o.getJSONArray("f");if(v.length()!=Vision.DIM)continue;
   float[] feat=new float[v.length()];for(int k=0;k<feat.length;k++)feat[k]=(float)v.getDouble(k);p.samples.add(new Sample(o.getInt("t"),feat));
  }
 }catch(Exception ignored){return new Profile();}return p;}
}
