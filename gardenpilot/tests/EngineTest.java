package app.gardenpilot;
import java.util.*;
public final class EngineTest {
 static int checks=0;
 static void check(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
 static Engine.Board board(String... rows){int[] a=new int[rows.length*rows[0].length()];int k=0;for(String row:rows){if(row.length()!=rows[0].length())throw new IllegalArgumentException(row);for(char c:row.toCharArray()){int t;switch(c){case 'r':t=1;break;case 'g':t=2;break;case 'y':t=3;break;case 'p':t=4;break;case 's':t=5;break;case 'v':t=6;break;case 'f':t=10;break;case 'o':t=11;break;case 'd':t=12;break;case 't':t=13;break;case '*':t=14;break;case 'B':t=20;break;case 'b':t=21;break;case 'j':t=22;break;case 'h':t=23;break;case '#':t=24;break;case '.':t=-1;break;case '-':t=0;break;default:t=-2;}a[k++]=t;}}return new Engine.Board(rows[0].length(),rows.length,a);}
 public static void main(String[] args){
  Engine.Board b=board("rgr","yry","gyg");int[] original=b.cells.clone();
  Engine.Move m=Engine.evaluate(b,1,4,false,0);check(m!=null&&m.pieces==3&&m.created==0,"ordinary triple");check(Arrays.equals(original,b.cells),"evaluation preserves board");
  check(Engine.evaluate(b,0,4,false,0)==null,"no diagonal swap");check(Engine.evaluate(b,2,3,false,0)==null,"no row wrapping");check(Engine.evaluate(b,0,0,true,0)==null,"no ordinary tap");
  check(Engine.evaluate(board("rgr","y?y","gyg"),1,4,false,0)==null,"unknown cannot move");
  check(Engine.evaluate(board("rgr","yBy","gyg"),1,4,false,0)==null,"box cannot move");
  check(Engine.matches(board("r.r","gyg","yry")).isEmpty(),"hole breaks match");check(Engine.rank(board("rrr","ygg","gyp"),0).isEmpty(),"unsettled matches wait");
  m=Engine.evaluate(board("rrgr","yprg","gypy"),2,6,false,0);check(m!=null&&m.pieces==4&&m.created==1,"four creates power-up");
  int[] cross=new int[25];Arrays.fill(cross,Engine.BLOCK);for(int i:new int[]{2,7,11,13,17})cross[i]=Engine.RED;cross[12]=Engine.GREEN;
  m=Engine.evaluate(new Engine.Board(5,5,cross),12,17,false,0);check(m!=null&&m.pieces==5&&m.created==1,"T shape merges overlapping matches");
  Engine.Board honey=board("rgr","jry","gyg");m=Engine.evaluate(honey,1,4,false,1);check(m!=null&&m.obstacles==1,"adjacent jar damaged");
  check(m.score>Engine.evaluate(honey,1,4,false,0).score,"goal rewards honey");
  check(Engine.evaluate(b,1,4,false,3).score>Engine.evaluate(b,1,4,false,0).score,"colour goal affects score");
  Engine.Board rainbow=board("*ry","gry","ygr");m=Engine.evaluate(rainbow,0,1,false,0);check(m!=null&&m.pieces==3,"rainbow clears colour");
  m=Engine.evaluate(board("**j","gBy","ygr"),0,1,false,1);check(m!=null&&m.obstacles==2,"double rainbow hits obstacle layer");
  check(Engine.evaluate(board("*fy","gry","ygr"),0,1,false,0)==null,"unsupported rainbow-explosive not guessed");
  int[] chain=new int[25];Arrays.fill(chain,Engine.BLOCK);chain[12]=Engine.FIRE;chain[13]=Engine.BOMB;chain[14]=Engine.JAR;
  m=Engine.evaluate(new Engine.Board(5,5,chain),12,12,true,1);check(m!=null&&m.explosions==2&&m.obstacles==1,"power-up chain");
  Engine.Board sample=board("...rgpgr...","rbBypryyrBj","jbBrygyypBj","jbbppyggyBj","jbb#...rpBj","jbB#rrypgBj","jb-rprpggBj","jjggrgyrrBj","...gfyyr...");
  check(Engine.matches(sample).isEmpty(),"sample is settled");List<Engine.Move> sampleMoves=Engine.rank(sample,1);check(!sampleMoves.isEmpty(),"sample has playable moves");
  System.out.println("Sample: "+sampleMoves.get(0).describe()+"; from="+sampleMoves.get(0).from+" to="+sampleMoves.get(0).to);
  Random random=new Random(4123);int evaluated=0;
  for(int run=0;run<1200;run++){
   int[] a=new int[64];for(int i=0;i<a.length;i++){int x=random.nextInt(14);a[i]=x<6?x+1:x==6?Engine.VOID:x==7?Engine.UNKNOWN:x==8?Engine.BOX:x==9?Engine.JAR:x==10?Engine.FIRE:x==11?Engine.RAINBOW:Engine.BLOCK;}
   Engine.Board q=new Engine.Board(8,8,a);long fingerprint=q.fingerprint();List<Engine.Move> moves=Engine.rank(q,1);double last=Double.POSITIVE_INFINITY;
   for(Engine.Move move:moves){evaluated++;check(move.score<=last,"ranked scores descend");last=move.score;check(move.from>=0&&move.to>=0&&move.from<64&&move.to<64,"indices valid");check(Engine.movable(a[move.from])&&Engine.movable(a[move.to]),"action avoids obstacles/unknowns");check(move.tap?move.from==move.to&&Engine.explosive(a[move.from]):q.adjacent(move.from,move.to),"valid gesture geometry");}
   check(Arrays.equals(a,q.cells)&&q.fingerprint()==fingerprint,"ranking preserves state");
  }
  check(evaluated>1000,"random boards exercise moves");System.out.println("PASS: "+checks+" assertions; "+evaluated+" randomized candidate moves.");
 }
}
