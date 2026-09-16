package app.gardenpilot;
import android.content.Context;
import android.graphics.*;
import android.view.*;
import android.widget.*;

public final class EditorOverlay extends FrameLayout {
 public interface Listener{void done(Profile p);void cancel();void stop();}
 private Bitmap image;private final Rect game;private final Profile profile;private final Listener listener;private final Grid grid;
 private Vision.Result recognized;private TextView info;private int cornerMode=0,selected=-100;
 private final int[] types={-100,Engine.VOID,Engine.EMPTY,Engine.RED,Engine.GREEN,Engine.YELLOW,Engine.PURPLE,Engine.BLUE,Engine.PINK,Engine.FIRE,Engine.BOMB,Engine.DYNAMITE,Engine.TNT,Engine.RAINBOW,Engine.BOX,Engine.BOX2,Engine.JAR,Engine.HONEY,Engine.BLOCK};
 public EditorOverlay(Context c,Bitmap b,Rect rect,Profile p,Listener l){super(c);image=b;game=new Rect(rect);profile=p;listener=l;setBackgroundColor(Ui.BG);
  grid=new Grid(c);addView(grid,new FrameLayout.LayoutParams(-1,-1));LinearLayout panel=Ui.column(c);panel.setBackground(Ui.bg(Ui.BG,14,c));panel.setPadding(Ui.dp(c,8),Ui.dp(c,5),Ui.dp(c,8),Ui.dp(c,5));
  FrameLayout.LayoutParams panelParams=new FrameLayout.LayoutParams(Ui.dp(c,216),-1,Gravity.TOP|Gravity.LEFT);addView(panel,panelParams);
  TextView title=Ui.text(c,"Настройка поля  ↔",17,Ui.MINT);Ui.add(panel,title);
  title.setOnTouchListener(new View.OnTouchListener(){float start;int origin;public boolean onTouch(View v,MotionEvent e){if(e.getAction()==MotionEvent.ACTION_DOWN){start=e.getRawX();origin=panelParams.leftMargin;return true;}if(e.getAction()==MotionEvent.ACTION_MOVE){panelParams.leftMargin=Math.max(0,Math.min(getWidth()-panel.getWidth(),origin+(int)(e.getRawX()-start)));panel.setLayoutParams(panelParams);}return true;}});
  LinearLayout safety=new LinearLayout(c);safety.addView(Ui.button(c,"Отмена",Ui.MUTED,l::cancel),new LinearLayout.LayoutParams(0,-2,1));safety.addView(Ui.button(c,"Стоп",Ui.RED,l::stop),new LinearLayout.LayoutParams(0,-2,1));Ui.add(panel,safety);
  ScrollView scroll=new ScrollView(c);LinearLayout body=Ui.column(c);scroll.addView(body);panel.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));info=Ui.text(c,"",12,Ui.GOLD);Ui.add(body,info);
  Ui.add(body,Ui.text(c,"1. Совместите центры сетки. Размер "+p.cols+" × "+p.rows+".",13,Ui.TEXT));
  Ui.add(body,Ui.button(c,"Левый верх сетки",Ui.MINT,()->{cornerMode=1;info.setText("Коснитесь центра крайней верхней левой позиции сетки, включая пустой угол.");}));
  Ui.add(body,Ui.button(c,"Правый низ сетки",Ui.MINT,()->{cornerMode=2;info.setText("Коснитесь центра крайней нижней правой позиции сетки.");}));
  Ui.add(body,Ui.text(c,"2. Выберите элемент и коснитесь его образца. Подписи показывают результат распознавания.",13,Ui.TEXT));
  Spinner spinner=new Spinner(c);String[] names=new String[types.length];for(int i=0;i<types.length;i++)names[i]=types[i]==-100?"Проверить клетку":Engine.name(types[i]);
  ArrayAdapter<String> adapter=new ArrayAdapter<String>(c,android.R.layout.simple_spinner_dropdown_item,names){
   @Override public View getView(int pos,View reused,ViewGroup parent){TextView v=Ui.text(c,getItem(pos),13,Ui.TEXT);v.setPadding(5,18,5,18);return v;}
   @Override public View getDropDownView(int pos,View reused,ViewGroup parent){TextView v=Ui.text(c,getItem(pos),14,Ui.TEXT);v.setBackgroundColor(Ui.CARD);v.setPadding(15,18,15,18);return v;}
  };spinner.setAdapter(adapter);spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> a){}public void onItemSelected(AdapterView<?> a,View v,int pos,long id){selected=types[pos];cornerMode=0;}});Ui.add(body,spinner);
  Ui.add(body,Ui.text(c,"«Нет клетки» исключает позицию. Другой тип включает её обратно. Для каждого вида бонуса добавьте образец. При ошибках добавьте 2–3 образца одного типа.",12,Ui.MUTED));
  Ui.add(body,Ui.text(c,"Проверьте все подписи. Неизвестное — «?». Перетащите панель за заголовок, если она закрывает клетку.",12,Ui.MUTED));
  Ui.add(panel,Ui.button(c,"Сетка и подписи верны ✓",Ui.MINT,()->{if(!valid()){info.setText("Неверные границы. Задайте углы заново.");return;}listener.done(profile);}));refresh();
 }
 private boolean valid(){return profile.left>=0&&profile.top>=0&&profile.right<=1&&profile.bottom<=1&&profile.right-profile.left>.05&&profile.bottom-profile.top>.05;}
 public void release(){if(image!=null){image.recycle();image=null;}}
 private void refresh(){if(image==null||!valid())return;recognized=Vision.read(image,profile);info.setText("Неизвестно: "+recognized.unknown+"/"+recognized.active+" · образцов "+profile.samples.size());grid.invalidate();}
 private final class Grid extends View {
  private final Paint paint=new Paint(3);Grid(Context c){super(c);}
  @Override protected void onDraw(Canvas c){if(image==null)return;c.drawBitmap(image,null,game,paint);if(!valid()||recognized==null)return;
   float dx=(profile.right-profile.left)*game.width()/(profile.cols-1),dy=(profile.bottom-profile.top)*game.height()/(profile.rows-1);paint.setTextSize(Math.min(Ui.dp(getContext(),12),dx*.30f));paint.setTextAlign(Paint.Align.CENTER);
   for(int i=0;i<profile.mask.length;i++){
    float x=game.left+profile.x(i)*game.width(),y=game.top+profile.y(i)*game.height();int t=recognized.board.cells[i];paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.5f);paint.setColor(profile.mask[i]?0x8865E2B7:0x55FFFFFF);c.drawRect(x-dx*.5f,y-dy*.5f,x+dx*.5f,y+dy*.5f,paint);
    paint.setStyle(Paint.Style.FILL);paint.setColor(0xDD101A23);c.drawRoundRect(x-dx*.29f,y+dy*.12f,x+dx*.29f,y+dy*.43f,4,4,paint);paint.setColor(t==Engine.UNKNOWN?Ui.RED:Ui.tileColor(t));c.drawText(Engine.code(t),x,y+dy*.36f,paint);
   }
  }
  @Override public boolean onTouchEvent(MotionEvent e){
   if(e.getAction()!=MotionEvent.ACTION_UP)return true;if(image==null||!game.contains((int)e.getX(),(int)e.getY()))return true;
   float nx=(e.getX()-game.left)/game.width(),ny=(e.getY()-game.top)/game.height();
   if(cornerMode==1){if(nx>=profile.right-.05||ny>=profile.bottom-.05){info.setText("Левый верх должен быть выше и левее правого низа.");return true;}profile.left=nx;profile.top=ny;profile.verified=false;cornerMode=0;refresh();return true;}
   if(cornerMode==2){if(nx<=profile.left+.05||ny<=profile.top+.05){info.setText("Правый низ должен быть ниже и правее левого верха.");return true;}profile.right=nx;profile.bottom=ny;profile.verified=false;cornerMode=0;refresh();return true;}
   if(!valid())return true;float gx=(nx-profile.left)/(profile.right-profile.left)*(profile.cols-1),gy=(ny-profile.top)/(profile.bottom-profile.top)*(profile.rows-1);int col=Math.round(gx),row=Math.round(gy);if(col<0||col>=profile.cols||row<0||row>=profile.rows)return true;int i=row*profile.cols+col;
   if(selected==-100){info.setText("Строка "+(row+1)+", столбец "+(col+1)+"\n"+Engine.name(recognized.board.cells[i]));return true;}
   if(selected==Engine.VOID)profile.mask[i]=false;else{profile.mask[i]=true;profile.add(selected,Vision.feature(image,profile,i));}profile.verified=false;refresh();return true;
  }
 }
}
