package app.gardenpilot;
import android.graphics.Bitmap;
import android.graphics.Color;

/** On-device visual classifier; power-ups are taught from real game screenshots. */
public final class Vision {
 public static final int SIDE=12,DIM=SIDE*SIDE*3;
 public static final class Result{
  public final Engine.Board board;public final float[] confidence;public final int unknown,active,colors;
  public Result(Engine.Board b,float[] p,int u,int a,int c){board=b;confidence=p;unknown=u;active=a;colors=c;}
  public boolean usable(){return active>=12&&colors>=8&&unknown<=Math.max(2,active/5);}
 }
 public static float[] feature(Bitmap image,Profile p,int cell){
  float cx=p.x(cell)*image.getWidth(),cy=p.y(cell)*image.getHeight();
  float w=(p.right-p.left)*image.getWidth()/(p.cols-1)*.82f,h=(p.bottom-p.top)*image.getHeight()/(p.rows-1)*.82f;
  float[] f=new float[DIM];int k=0;
  for(int y=0;y<SIDE;y++)for(int x=0;x<SIDE;x++){
   int ix=Math.max(0,Math.min(image.getWidth()-1,Math.round(cx+((x+.5f)/SIDE-.5f)*w)));
   int iy=Math.max(0,Math.min(image.getHeight()-1,Math.round(cy+((y+.5f)/SIDE-.5f)*h)));
   int rgb=image.getPixel(ix,iy);f[k++]=Color.red(rgb)/255f;f[k++]=Color.green(rgb)/255f;f[k++]=Color.blue(rgb)/255f;
  }return f;
 }
 public static Result read(Bitmap image,Profile p){
  int n=p.rows*p.cols,u=0,a=0,c=0;int[] cells=new int[n];float[] conf=new float[n];
  for(int i=0;i<n;i++){if(!p.mask[i]){cells[i]=Engine.VOID;conf[i]=1;continue;}a++;Guess g=guess(feature(image,p,i),p);cells[i]=g.type;conf[i]=g.p;if(g.type==Engine.UNKNOWN)u++;if(Engine.color(g.type))c++;}
  return new Result(new Engine.Board(p.cols,p.rows,cells),conf,u,a,c);
 }
 private static final class Guess{final int type;final float p;Guess(int t,float p){type=t;this.p=p;}}
 private static Guess guess(float[] f,Profile profile){
  double best=10,other=10;int type=Engine.UNKNOWN;
  for(Profile.Sample s:profile.samples){double d=0;for(int k=0;k<DIM;k++){double v=f[k]-s.feature[k];d+=v*v;}d/=DIM;
   if(d<best){if(s.type!=type)other=best;best=d;type=s.type;}else if(s.type!=type&&d<other)other=d;
  }
  if(best<.022&&other-best>.0025)return new Guess(type,(float)Math.max(.75,1-Math.sqrt(best)*1.5));
  float[] hsv=new float[3];int[] colors=new int[7];int total=0,brown=0,dark=0,white=0,topGold=0,bottomPurple=0,muted=0;
  for(int y=0;y<SIDE;y++)for(int x=0;x<SIDE;x++){
   int k=(y*SIDE+x)*3;Color.RGBToHSV((int)(f[k]*255),(int)(f[k+1]*255),(int)(f[k+2]*255),hsv);
   float h=hsv[0],s=hsv[1],v=hsv[2];
   if(y<4&&h>=20&&h<65&&s>.6&&v>.6)topGold++;if(y>=7&&(h>280||h<12)&&s>.2&&v<.8)bottomPurple++;
   if(h>15&&h<42&&s>.55&&v<.88)brown++;if(s<.38&&v>.4)muted++;if(v<.25)dark++;if(s<.20&&v>.83)white++;
   if(x<2||x>9||y<2||y>9)continue;total++;if(s<.48||v<.36)continue;int t=0;
   if(h<15||h>348)t=Engine.RED;else if(h>=70&&h<=165)t=Engine.GREEN;else if(h>=38&&h<=70)t=Engine.YELLOW;
   else if(h>=245&&h<=290)t=Engine.PURPLE;else if(h>=180&&h<=238)t=Engine.BLUE;else if(h>290&&h<=348)t=Engine.PINK;
   if(t>0)colors[t]++;
  }
  if(topGold>20&&bottomPurple>12)return new Guess(Engine.JAR,.80f);
  if(white>25&&colors[Engine.PINK]>8)return new Guess(Engine.UNKNOWN,0);
  if(brown>75&&colors[Engine.YELLOW]<28)return new Guess(Engine.BOX,.78f);
  int winner=1;for(int t=2;t<=6;t++)if(colors[t]>colors[winner])winner=t;
  if(colors[winner]>=total*.67&&dark<15){if(winner==Engine.BLUE||winner==Engine.PINK)return new Guess(Engine.UNKNOWN,0);return new Guess(winner,.78f);}
  if(muted>130)return new Guess(Engine.EMPTY,.75f);return new Guess(Engine.UNKNOWN,0);
 }
}
