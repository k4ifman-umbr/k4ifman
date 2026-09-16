package app.gardenpilot;
import android.accessibilityservice.*;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.*;
import android.hardware.HardwareBuffer;
import android.os.*;
import android.view.*;
import android.view.accessibility.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;

public final class PilotService extends AccessibilityService {
 public static final String GAME="com.playrix.gardenscapes";public static volatile PilotService instance;
 private final Handler main=new Handler(Looper.getMainLooper());private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private WindowManager wm;private LinearLayout panel;private TextView status;private WindowManager.LayoutParams panelParams;
 private ArrowView arrow;private EditorOverlay editor;private Profile profile;
 private boolean compact=false,capturing=false,closed=false;private int mode=0,stable=0,moves=0,misses=0,scanCount=0;
 private long epoch=0,previous=Long.MIN_VALUE,lastAction=Long.MIN_VALUE,lastGoodActionTime=0,startedAt=0;private Rect lastBounds;
 // 0 idle, 1 hint, 2 single move, 3 auto, 4 calibration.
 @Override protected void onServiceConnected(){instance=this;wm=(WindowManager)getSystemService(WINDOW_SERVICE);profile=Profile.load(this);showPanel();}
 public void reload(){pause("Настройки сохранены. Проверьте поле.");profile=Profile.load(this);}
 private WindowManager.LayoutParams overlay(int width,int height,boolean touchable){
  int flags=WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN|WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;if(!touchable)flags|=WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
  WindowManager.LayoutParams p=new WindowManager.LayoutParams(width,height,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,flags,PixelFormat.TRANSLUCENT);p.gravity=Gravity.TOP|Gravity.LEFT;p.layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;return p;
 }
 public void showPanel(){if(closed||panel!=null)return;
  panel=Ui.column(this);panel.setPadding(Ui.dp(this,8),Ui.dp(this,8),Ui.dp(this,8),Ui.dp(this,8));panel.setBackground(Ui.bg(Ui.BG,16,this));panel.setElevation(Ui.dp(this,8));
  TextView title=Ui.text(this,compact?"GP  ↕":"GardenPilot  ↕",compact?15:19,Ui.MINT);title.setPadding(0,Ui.dp(this,7),0,Ui.dp(this,7));Ui.add(panel,title);
  status=Ui.text(this,mode==3?"Авто":"Пауза · настройте поле",compact?10:12,Ui.MUTED);status.setMaxLines(compact?3:4);Ui.add(panel,status);
  if(compact){Ui.add(panel,Ui.button(this,"▶",Ui.MINT,()->begin(3)));Ui.add(panel,Ui.button(this,"Ⅱ",Ui.GOLD,()->pause("Пауза")));Ui.add(panel,Ui.button(this,"■ Стоп",Ui.RED,this::stopSession));Ui.add(panel,Ui.button(this,"≡",Ui.MUTED,this::togglePanel));}
  else{row(Ui.button(this,"Авто",Ui.MINT,()->begin(3)),Ui.button(this,"Шаг",Ui.MINT,()->begin(2)));row(Ui.button(this,"Подсказка",Ui.GOLD,()->begin(1)),Ui.button(this,"Поле",Ui.TEXT,()->begin(4)));row(Ui.button(this,"Пауза",Ui.GOLD,()->pause("Пауза")),Ui.button(this,"Стоп",Ui.RED,this::stopSession));Ui.add(panel,Ui.button(this,"Свернуть",Ui.MUTED,this::togglePanel));}
  if(panelParams==null){panelParams=overlay(Ui.dp(this,compact?84:220),-2,true);panelParams.x=Ui.dp(this,8);panelParams.y=Ui.dp(this,12);}panelParams.width=Ui.dp(this,compact?84:220);
  title.setOnTouchListener(new View.OnTouchListener(){float x,y;int px,py;public boolean onTouch(View v,MotionEvent e){
   if(e.getAction()==MotionEvent.ACTION_DOWN){x=e.getRawX();y=e.getRawY();px=panelParams.x;py=panelParams.y;return true;}
   if(e.getAction()==MotionEvent.ACTION_MOVE){Rect screen=wm.getCurrentWindowMetrics().getBounds();panelParams.x=Math.max(0,Math.min(screen.width()-panel.getWidth(),px+Math.round(e.getRawX()-x)));panelParams.y=Math.max(0,Math.min(screen.height()-panel.getHeight(),py+Math.round(e.getRawY()-y)));wm.updateViewLayout(panel,panelParams);}return true;
  }});wm.addView(panel,panelParams);if(arrow==null){arrow=new ArrowView();wm.addView(arrow,overlay(-1,-1,false));}
 }
 private void row(View a,View b){LinearLayout l=new LinearLayout(this);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.setMargins(2,0,2,0);l.addView(a,p);l.addView(b,new LinearLayout.LayoutParams(p));Ui.add(panel,l);}
 private void togglePanel(){if(panel==null)return;wm.removeView(panel);panel=null;compact=!compact;showPanel();}
 private void message(String s){if(status!=null)status.setText(s);}
 public void pause(String why){epoch++;mode=0;stable=0;main.removeCallbacksAndMessages(null);if(arrow!=null)arrow.clear();message(why);}
 private void stopSession(){pause("Остановлено");if(editor!=null){EditorOverlay e=editor;editor=null;wm.removeView(e);e.release();}if(panel!=null){wm.removeView(panel);panel=null;status=null;}if(arrow!=null){wm.removeView(arrow);arrow=null;}}
 private void begin(int requested){if(editor!=null)return;pause("Подготовка…");profile=Profile.load(this);
  if(requested!=4&&!profile.verified){message("Сначала нажмите «Поле» и подтвердите сетку.");return;}
  mode=requested;moves=0;misses=0;scanCount=0;previous=Long.MIN_VALUE;lastAction=Long.MIN_VALUE;lastBounds=null;startedAt=SystemClock.uptimeMillis();message(requested==4?"Снимок для настройки…":"Ожидаю неподвижное поле…");schedule(epoch,50);
 }
 private void schedule(long token,long delay){main.postDelayed(()->scan(token),delay);}
 private static final class Target{final int id;final Rect bounds;Target(int i,Rect b){id=i;bounds=b;}}
 private Target target(){for(AccessibilityWindowInfo w:getWindows()){
  if(w.getType()!=AccessibilityWindowInfo.TYPE_APPLICATION||(!w.isActive()&&!w.isFocused()))continue;
  AccessibilityNodeInfo root=w.getRoot();if(root==null)continue;CharSequence pkg=root.getPackageName();boolean ok=pkg!=null&&GAME.contentEquals(pkg);root.recycle();
  if(ok){Rect bounds=new Rect();w.getBoundsInScreen(bounds);if(bounds.width()>0&&bounds.height()>0)return new Target(w.getId(),bounds);}
 }return null;}
 private void scan(long token){if(token!=epoch||mode==0||closed)return;
  if(SystemClock.uptimeMillis()-Math.max(startedAt,lastGoodActionTime)>60000){pause("Долго нет хода. Проверьте поле.");return;}
  if(capturing){schedule(token,500);return;}Target t=target();if(t==null){pause("Откройте уровень Gardenscapes и нажмите запуск.");return;}
  if(lastBounds!=null&&!lastBounds.equals(t.bounds)){pause("Размер окна изменился. Проверьте «Поле».");return;}lastBounds=new Rect(t.bounds);capturing=true;Profile snapshot=profile;
  try{takeScreenshotOfWindow(t.id,getMainExecutor(),new TakeScreenshotCallback(){
   @Override public void onSuccess(ScreenshotResult result){capturing=false;HardwareBuffer buffer=result.getHardwareBuffer();Bitmap hardware=null,copy=null;
    try{if(token!=epoch||mode==0||closed)return;hardware=Bitmap.wrapHardwareBuffer(buffer,result.getColorSpace());if(hardware==null)throw new IllegalStateException("bitmap");copy=hardware.copy(Bitmap.Config.ARGB_8888,false);}
    catch(Exception e){pause("Не удалось прочитать снимок.");}finally{if(hardware!=null)hardware.recycle();buffer.close();}
    if(copy==null)return;final Bitmap image=copy;
    try{worker.execute(()->{try{Vision.Result resultBoard=Vision.read(image,snapshot);List<Engine.Move> ranked=Engine.rank(resultBoard.board,snapshot.goal);main.post(()->analyzed(token,t,image,resultBoard,ranked));}catch(Exception e){image.recycle();main.post(()->{if(token==epoch)pause("Ошибка распознавания. Проверьте «Поле».");});}});}catch(RejectedExecutionException e){image.recycle();}
   }
   @Override public void onFailure(int error){capturing=false;if(token!=epoch)return;if(error==ERROR_TAKE_SCREENSHOT_INTERVAL_TIME_SHORT&&misses++<3){schedule(token,700);return;}pause("Снимок недоступен ("+error+"). Откройте игру заново.");}
  });}catch(Exception e){capturing=false;pause("Доступ к экрану недоступен. Переподключите службу.");}
 }
 private void analyzed(long token,Target t,Bitmap image,Vision.Result result,List<Engine.Move> ranked){
  if(token!=epoch||mode==0||closed){image.recycle();return;}
  if(mode==4){mode=0;if(panel!=null)panel.setVisibility(View.GONE);editor=new EditorOverlay(this,image,t.bounds,profile,new EditorOverlay.Listener(){
   public void done(Profile p){profile=p;p.verified=true;p.save(PilotService.this);closeEditor("Сетка сохранена. Начните с подсказки.");}
   public void cancel(){closeEditor("Настройка отменена.");}public void stop(){stopSession();}
  });wm.addView(editor,overlay(-1,-1,true));return;}
  image.recycle();scanCount++;Target now=target();if(now==null||now.id!=t.id||!now.bounds.equals(t.bounds)){pause("Игра потеряла фокус. Запустите снова.");return;}
  if(!result.usable()){stable=0;previous=Long.MIN_VALUE;message("Не распознано: "+result.unknown+"/"+result.active+". Проверка…");if(++misses>=4)pause("Поле не распознано. Откройте «Поле» и добавьте образцы.");else schedule(token,650);return;}
  misses=0;long fingerprint=result.board.fingerprint();if(fingerprint==previous)stable++;else{stable=1;previous=fingerprint;}
  if(stable<3){message("Ожидаю окончания анимации…");schedule(token,450);return;}
  if(fingerprint==lastAction){if(SystemClock.uptimeMillis()-lastGoodActionTime>6500){pause("Поле не изменилось после хода. Проверьте сетку.");return;}schedule(token,650);return;}
  if(ranked.isEmpty()){if(scanCount>16){pause("Подходящих ходов нет. Проверьте поле.");return;}message("Ожидаю новые фишки…");schedule(token,650);return;}
  Engine.Move move=ranked.get(0);if(result.confidence[move.from]<.74||result.confidence[move.to]<.74){pause("Недостаточно уверенности в фишках хода.");return;}
  if(arrow!=null)arrow.setMove(move,t.bounds,profile);message(move.describe()+"\n"+(mode==3?"Авто · "+moves+"/"+profile.limit:mode==2?"Один ход":"Подсказка"));
  if(mode==1){mode=0;return;}main.postDelayed(()->executeMove(token,t,result.board,move),500);
 }
 private void executeMove(long token,Target t,Engine.Board board,Engine.Move move){
  if(token!=epoch||mode==0||closed)return;Target now=target();if(now==null||now.id!=t.id||!now.bounds.equals(t.bounds)){pause("Игра потеряла фокус.");return;}
  float x=t.bounds.left+profile.x(move.from)*t.bounds.width(),y=t.bounds.top+profile.y(move.from)*t.bounds.height();float x2=t.bounds.left+profile.x(move.to)*t.bounds.width(),y2=t.bounds.top+profile.y(move.to)*t.bounds.height();
  if(!t.bounds.contains((int)x,(int)y)||!t.bounds.contains((int)x2,(int)y2)){pause("Ход вне окна игры.");return;}
  if(panel!=null){int[] loc=new int[2];panel.getLocationOnScreen(loc);Rect hit=new Rect(loc[0],loc[1],loc[0]+panel.getWidth(),loc[1]+panel.getHeight());Rect pathBounds=new Rect((int)Math.min(x,x2)-4,(int)Math.min(y,y2)-4,(int)Math.max(x,x2)+5,(int)Math.max(y,y2)+5);if(Rect.intersects(hit,pathBounds)){pause("Панель закрывает ход. Перетащите её за заголовок.");return;}}
  Path path=new Path();path.moveTo(x,y);if(!move.tap)path.lineTo(x2,y2);GestureDescription gesture=new GestureDescription.Builder().addStroke(new GestureDescription.StrokeDescription(path,0,move.tap?65:150)).build();lastAction=board.fingerprint();lastGoodActionTime=SystemClock.uptimeMillis();
  boolean accepted=dispatchGesture(gesture,new GestureResultCallback(){
   @Override public void onCompleted(GestureDescription g){if(token!=epoch)return;moves++;stable=0;scanCount=0;if(mode==2){pause("Один ход выполнен.");return;}if(moves>=profile.limit){pause("Лимит "+moves+" ходов достигнут.");return;}if(arrow!=null)arrow.clear();message("Ход "+moves+" · ожидаю поле…");schedule(token,1350);}
   @Override public void onCancelled(GestureDescription g){if(token==epoch)pause("Жест отменён. Можно запустить снова.");}
  },main);if(!accepted)pause("Android не принял жест.");
 }
 private void closeEditor(String text){if(editor!=null){EditorOverlay e=editor;editor=null;wm.removeView(e);e.release();}if(panel!=null)panel.setVisibility(View.VISIBLE);pause(text);}
 @Override public void onAccessibilityEvent(AccessibilityEvent event){if(mode==0||mode==4)return;if(event.getEventType()==AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED&&event.getPackageName()!=null&&!GAME.contentEquals(event.getPackageName())&&!getPackageName().contentEquals(event.getPackageName()))pause("Открыто другое окно. Бот на паузе.");}
 @Override protected boolean onKeyEvent(KeyEvent event){if(event.getKeyCode()==KeyEvent.KEYCODE_VOLUME_DOWN&&event.getAction()==KeyEvent.ACTION_DOWN)pause("Пауза кнопкой громкости");return false;}
 @Override public void onConfigurationChanged(Configuration c){super.onConfigurationChanged(c);pause("Ориентация изменилась. Проверьте поле.");if(editor!=null)closeEditor("Проверьте поле в новой ориентации.");if(profile!=null){profile.verified=false;profile.save(this);}if(panelParams!=null){panelParams.x=Ui.dp(this,8);panelParams.y=Ui.dp(this,12);if(panel!=null)wm.updateViewLayout(panel,panelParams);}}
 @Override public void onInterrupt(){pause("Управление прервано.");}
 @Override public void onDestroy(){closed=true;stopSession();instance=null;worker.shutdown();super.onDestroy();}
 private final class ArrowView extends View {
  private final Paint paint=new Paint(3);private float x,y,xx,yy;private boolean show=false,tap;
  ArrowView(){super(PilotService.this);}void clear(){show=false;invalidate();}
  void setMove(Engine.Move m,Rect r,Profile p){x=r.left+p.x(m.from)*r.width();y=r.top+p.y(m.from)*r.height();xx=r.left+p.x(m.to)*r.width();yy=r.top+p.y(m.to)*r.height();tap=m.tap;show=true;invalidate();}
  @Override protected void onDraw(Canvas c){if(!show)return;paint.setColor(Ui.MINT);paint.setStrokeWidth(Ui.dp(PilotService.this,3));paint.setStyle(Paint.Style.STROKE);c.drawCircle(x,y,Ui.dp(PilotService.this,13),paint);if(tap){c.drawCircle(x,y,Ui.dp(PilotService.this,21),paint);return;}c.drawLine(x,y,xx,yy,paint);double a=Math.atan2(yy-y,xx-x);float len=Ui.dp(PilotService.this,11);c.drawLine(xx,yy,xx-len*(float)Math.cos(a-.6),yy-len*(float)Math.sin(a-.6),paint);c.drawLine(xx,yy,xx-len*(float)Math.cos(a+.6),yy-len*(float)Math.sin(a+.6),paint);}
 }
}
