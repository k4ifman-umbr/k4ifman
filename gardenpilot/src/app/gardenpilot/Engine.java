package app.gardenpilot;
import java.util.*;

/** One-ply evaluator. Does not invent random future drops or unsupported mechanics. */
public final class Engine {
 public static final int VOID=-1,UNKNOWN=-2,EMPTY=0,RED=1,GREEN=2,YELLOW=3,PURPLE=4,BLUE=5,PINK=6;
 public static final int FIRE=10,BOMB=11,DYNAMITE=12,TNT=13,RAINBOW=14,BOX=20,BOX2=21,JAR=22,HONEY=23,BLOCK=24;
 public static final int GOAL_CLEAR=0,GOAL_HONEY=1,GOAL_BOX=2;
 public static boolean color(int v){return v>=1&&v<=6;}
 public static boolean explosive(int v){return v>=FIRE&&v<=TNT;}
 public static boolean movable(int v){return color(v)||explosive(v)||v==RAINBOW;}
 public static boolean obstacle(int v){return v==BOX||v==BOX2||v==JAR||v==HONEY;}
 public static String name(int v){switch(v){
  case VOID:return "Нет клетки";case EMPTY:return "Пустая клетка";case RED:return "Красное яблоко";
  case GREEN:return "Зелёный лист";case YELLOW:return "Жёлтая груша";case PURPLE:return "Виноград";
  case BLUE:return "Синяя фишка";case PINK:return "Розовая фишка";case FIRE:return "Петарда";
  case BOMB:return "Бомба";case DYNAMITE:return "Динамит";case TNT:return "TNT";case RAINBOW:return "Радужный заряд";
  case BOX:return "Ящик";case BOX2:return "Укреплённый ящик";case JAR:return "Горшочек мёда";
  case HONEY:return "Разлитый мёд";case BLOCK:return "Неподвижная преграда";default:return "Не распознано";}}
 public static String code(int v){switch(v){
  case RED:return "Я";case GREEN:return "Л";case YELLOW:return "Г";case PURPLE:return "В";case BLUE:return "С";case PINK:return "Р";
  case FIRE:return "Пт";case BOMB:return "Б";case DYNAMITE:return "Д";case TNT:return "T";case RAINBOW:return "Рд";
  case BOX:return "Ящ";case BOX2:return "Я2";case JAR:return "Мд";case HONEY:return "М";case VOID:return "×";
  case EMPTY:return "·";case BLOCK:return "■";default:return "?";}}
 public static final class Board {
  public final int cols,rows;public final int[] cells;
  public Board(int c,int r,int[] cells){if(c<3||r<3||c>16||r>16||cells.length!=c*r)throw new IllegalArgumentException("grid");cols=c;rows=r;this.cells=cells.clone();}
  public boolean adjacent(int a,int b){return a>=0&&b>=0&&a<cells.length&&b<cells.length&&Math.abs(a/cols-b/cols)+Math.abs(a%cols-b%cols)==1;}
  public int[] neighbors(int p){int[] t=new int[4];int n=0;if(p%cols>0)t[n++]=p-1;if(p%cols+1<cols)t[n++]=p+1;if(p>=cols)t[n++]=p-cols;if(p+cols<cells.length)t[n++]=p+cols;return Arrays.copyOf(t,n);}
  public long fingerprint(){long h=1469598103934665603L;for(int v:cells)h=(h^(v+3))*1099511628211L;return h;}
 }
 public static final class Move {
  public final int from,to,pieces,obstacles,created,explosions;public final boolean tap;public final double score;
  public Move(int a,int b,boolean tap,double s,int p,int o,int c,int e){from=a;to=b;this.tap=tap;score=s;pieces=p;obstacles=o;created=c;explosions=e;}
  public String describe(){return(tap?"Взрыв":"Обмен")+" · фишки "+pieces+" · препятствия "+obstacles+(created>0?" · новый бонус":"");}
 }
 public static BitSet matches(Board b){
  BitSet out=new BitSet(b.cells.length);
  for(int r=0;r<b.rows;r++)for(int c=0;c<b.cols;){int a=r*b.cols+c,v=b.cells[a],n=c+1;while(n<b.cols&&b.cells[r*b.cols+n]==v)n++;if(color(v)&&n-c>=3)out.set(a,r*b.cols+n);c=n;}
  for(int c=0;c<b.cols;c++)for(int r=0;r<b.rows;){int a=r*b.cols+c,v=b.cells[a],n=r+1;while(n<b.rows&&b.cells[n*b.cols+c]==v)n++;if(color(v)&&n-r>=3)for(int k=r;k<n;k++)out.set(k*b.cols+c);r=n;}
  return out;
 }
 public static List<Move> rank(Board b,int goal){
  ArrayList<Move> out=new ArrayList<>();if(!matches(b).isEmpty())return out;
  for(int a=0;a<b.cells.length;a++){
   if(explosive(b.cells[a]))out.add(evaluate(b,a,a,true,goal));
   if(!movable(b.cells[a]))continue;
   for(int z:b.neighbors(a))if(z>a&&movable(b.cells[z])){Move m=evaluate(b,a,z,false,goal);if(m!=null)out.add(m);}
  }
  Collections.sort(out,(a,z)->{int n=Double.compare(z.score,a.score);if(n!=0)return n;return a.from!=z.from?Integer.compare(a.from,z.from):Integer.compare(a.to,z.to);});return out;
 }
 public static Move evaluate(Board source,int a,int z,boolean tap,int goal){
  if(a<0||z<0||a>=source.cells.length||z>=source.cells.length)return null;
  if(tap&&(!explosive(source.cells[a])||a!=z))return null;
  if(!tap&&(!source.adjacent(a,z)||!movable(source.cells[a])||!movable(source.cells[z])))return null;
  Board b=new Board(source.cols,source.rows,source.cells);
  BitSet cleared=new BitSet(b.cells.length),blast=new BitSet(b.cells.length),matched=new BitSet(b.cells.length);
  ArrayDeque<Integer> queue=new ArrayDeque<>();int made=0;double bonus=0;
  if(tap)queue.add(a);
  else if(b.cells[a]==RAINBOW||b.cells[z]==RAINBOW){
   int other=b.cells[a]==RAINBOW?b.cells[z]:b.cells[a];
   if(other==RAINBOW){for(int i=0;i<b.cells.length;i++)if(b.cells[i]!=VOID&&b.cells[i]!=BLOCK&&b.cells[i]!=UNKNOWN)blast.set(i);bonus+=12;}
   else if(color(other)){for(int i=0;i<b.cells.length;i++)if(b.cells[i]==other)cleared.set(i);cleared.set(a);cleared.set(z);}
   else return null;
  }else{
   if(!color(b.cells[a])||!color(b.cells[z])||b.cells[a]==b.cells[z])return null;
   int t=b.cells[a];b.cells[a]=b.cells[z];b.cells[z]=t;matched=matches(b);
   if(!matched.get(a)&&!matched.get(z))return null;cleared.or(matched);
   BitSet seen=new BitSet(b.cells.length);
   for(int i=matched.nextSetBit(0);i>=0;i=matched.nextSetBit(i+1)){
    if(seen.get(i))continue;ArrayDeque<Integer> component=new ArrayDeque<>();component.add(i);seen.set(i);int size=0;
    while(!component.isEmpty()){int p=component.remove();size++;for(int n:b.neighbors(p))if(matched.get(n)&&!seen.get(n)&&b.cells[n]==b.cells[p]){seen.set(n);component.add(n);}}
    if(size>=4){made++;bonus+=size==4?7:size==5?12:size==6?17:23;}
   }
  }
  BitSet fired=new BitSet(b.cells.length);boolean again=true;
  while(again){again=false;
   for(int i=blast.nextSetBit(0);i>=0;i=blast.nextSetBit(i+1))if(explosive(b.cells[i])&&!fired.get(i))queue.add(i);
   while(!queue.isEmpty()){
    int p=queue.remove();if(fired.get(p))continue;fired.set(p);again=true;int radius=b.cells[p]-FIRE+1;
    // Conservative diamond. Exact blast footprints and combination boosts need device validation.
    for(int r=Math.max(0,p/b.cols-radius);r<=Math.min(b.rows-1,p/b.cols+radius);r++)for(int c=Math.max(0,p%b.cols-radius);c<=Math.min(b.cols-1,p%b.cols+radius);c++)if(Math.abs(r-p/b.cols)+Math.abs(c-p%b.cols)<=radius)blast.set(r*b.cols+c);
   }
  }
  cleared.or(blast);BitSet damaged=new BitSet(b.cells.length);
  for(int p=cleared.nextSetBit(0);p>=0;p=cleared.nextSetBit(p+1)){
   if(obstacle(b.cells[p]))damaged.set(p);
   if(color(b.cells[p])||explosive(b.cells[p])||b.cells[p]==RAINBOW)for(int n:b.neighbors(p))if(obstacle(b.cells[n]))damaged.set(n);
  }
  int pieces=0;double score=bonus+fired.cardinality()*1.5;
  for(int p=cleared.nextSetBit(0);p>=0;p=cleared.nextSetBit(p+1))if(color(b.cells[p])){pieces++;score+=1+(goal>=3&&b.cells[p]==goal-2?3:0);}
  for(int p=damaged.nextSetBit(0);p>=0;p=damaged.nextSetBit(p+1)){
   int v=b.cells[p];if(v==JAR)score+=goal==GOAL_HONEY?20:8;else if(v==HONEY)score+=goal==GOAL_HONEY?17:9;else score+=goal==GOAL_BOX?14:6;
   if((v==BOX||v==BOX2)&&goal==GOAL_HONEY)for(int n:b.neighbors(p))if(b.cells[n]==JAR||b.cells[n]==HONEY)score+=5;
  }
  if(tap&&damaged.isEmpty())score-=2;
  return new Move(a,z,tap,score,pieces,damaged.cardinality(),made,fired.cardinality());
 }
}
