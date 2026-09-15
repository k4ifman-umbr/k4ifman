package app.gardenpilot;
import android.app.*;
import android.content.*;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.*;
import android.widget.*;

public final class MainActivity extends Activity {
 private TextView status;private EditText cols,rows,limit;private Spinner goal;
 @Override public void onCreate(Bundle saved){super.onCreate(saved);
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);LinearLayout content=Ui.column(this);int pad=Ui.dp(this,22);content.setPadding(pad,pad,pad,pad);scroll.addView(content);setContentView(scroll);
  scroll.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets i=insets.getInsets(WindowInsets.Type.systemBars());scroll.setPadding(i.left,i.top,i.right,i.bottom);return insets;});
  Ui.add(content,Ui.text(this,"GardenPilot",32,Ui.MINT));Ui.add(content,Ui.text(this,"Помощник для Gardenscapes · 0.1 alpha",14,Ui.MUTED));
  Ui.add(content,Ui.text(this,"Проверяй распознавание. Запускай, ставь на паузу и останавливай бота прямо поверх игры.",17,Ui.TEXT));
  status=Ui.text(this,"",15,Ui.GOLD);Ui.add(content,status);
  Ui.add(content,Ui.button(this,"1. Разрешить управление игрой",Ui.MINT,()->new AlertDialog.Builder(this).setTitle("Доступ к экрану и жестам")
   .setMessage("GardenPilot получает снимки окна Gardenscapes и выполняет ходы по вашей команде. Обработка идёт на телефоне. В настройках откройте «Установленные приложения» и включите GardenPilot. Для APK может потребоваться «Разрешить ограниченные настройки» в информации о приложении.")
   .setPositiveButton("Открыть настройки",(d,w)->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))).setNegativeButton("Отмена",null).show()));
  Ui.add(content,Ui.button(this,"2. Показать плавающую панель",Ui.MINT,()->{if(PilotService.instance==null){toast("Сначала включите GardenPilot в специальных возможностях");return;}PilotService.instance.showPanel();}));
  Ui.add(content,Ui.button(this,"3. Открыть Gardenscapes",Ui.MINT,()->{Intent launch=getPackageManager().getLaunchIntentForPackage(PilotService.GAME);if(launch==null){toast("Откройте Gardenscapes вручную");return;}startActivity(launch);}));
  Ui.add(content,Ui.text(this,"Настройка поля",23,Ui.TEXT));Ui.add(content,Ui.text(this,"Размер сетки включает отсутствующие угловые клетки. Для примера: 11 столбцов и 9 строк.",14,Ui.MUTED));
  Profile p=Profile.load(this);cols=number(content,"Столбцы (3–16)",p.cols);rows=number(content,"Строки (3–16)",p.rows);limit=number(content,"Лимит ходов за запуск (1–200)",p.limit);
  Ui.add(content,Ui.text(this,"Приоритет цели",14,Ui.MUTED));goal=new Spinner(this);
  String[] goals={"Очистка поля","Мёд и горшочки","Ящики","Красные фишки","Зелёные фишки","Жёлтые фишки","Виноград","Синие фишки","Розовые фишки"};
  goal.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,goals));goal.setSelection(p.goal);Ui.add(content,goal);
  Ui.add(content,Ui.button(this,"Сохранить настройки",Ui.MINT,()->{try{
   int c=Integer.parseInt(cols.getText().toString()),r=Integer.parseInt(rows.getText().toString()),n=Integer.parseInt(limit.getText().toString());if(c<3||c>16||r<3||r>16||n<1||n>200)throw new IllegalArgumentException();
   Profile q=Profile.load(this);if(c!=q.cols||r!=q.rows)q.resize(c,r);q.goal=goal.getSelectedItemPosition();q.limit=n;q.save(this);
   if(PilotService.instance!=null)PilotService.instance.reload();toast("Сохранено. В игре нажмите «Поле» и проверьте сетку.");
  }catch(Exception e){toast("Сетка: от 3 до 16. Лимит: от 1 до 200.");}}));
  Ui.add(content,Ui.text(this,"Первый запуск",23,Ui.TEXT));
  Ui.add(content,Ui.text(this,"Откройте уровень → «Поле» на панели. Совместите сетку с клетками. Выберите тип элемента и коснитесь его образца; отсутствующие позиции отметьте «Нет клетки». Проверьте подписи и нажмите «Сетка верна».\n\n«Подсказка» только рисует ход. «Шаг» выполняет один ход. «Авто» запускает серию до лимита. «Пауза» и «Стоп» всегда доступны; уменьшение громкости тоже ставит бота на паузу. Перетаскивайте панель за заголовок.\n\nПосле смены уровня или ориентации снова проверьте поле. Цель выбирается здесь вручную. Бот не использует расходуемые инструменты справа и не покупает ходы.",15,Ui.MUTED));
  Ui.add(content,Ui.text(this,"Экспериментальная версия",22,Ui.GOLD));
  Ui.add(content,Ui.text(this,"Распознавание основано на цветах и ваших образцах. Бонусы нужно обучить на экране. Ходы оцениваются по текущему полю, без предсказания случайных фишек. Радиусы сложных взрывов, слои препятствий, цепи, порталы и новые элементы могут оцениваться неполно. Прохождение не гарантируется. Сначала проверьте «Подсказку» и «Шаг».",14,Ui.MUTED));
 }
 private EditText number(LinearLayout p,String label,int value){Ui.add(p,Ui.text(this,label,14,Ui.MUTED));EditText e=new EditText(this);e.setTextColor(Ui.TEXT);e.setInputType(InputType.TYPE_CLASS_NUMBER);e.setText(Integer.toString(value));e.setSingleLine(true);Ui.add(p,e);return e;}
 private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
 @Override protected void onResume(){super.onResume();if(status!=null)status.setText(PilotService.instance!=null?"● Управление подключено":"○ Требуется разрешение");}
}
