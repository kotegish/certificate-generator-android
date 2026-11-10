package com.example.certificategenerator;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.graphics.pdf.PdfDocument;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private static final int PICK_CSV=41, PICK_TEMPLATE=42, PICK_FOLDER=43;
    private final ArrayList<String> people=new ArrayList<>();
    private final ArrayList<Template> templates=new ArrayList<>();
    private EditText event,date,issuer;
    private TextView csvStatus,selectedLabel,saveLocation;
    private Uri saveTree;
    private Uri csv;
    private BroadcastReceiver updateReceiver;
    private long updateDownloadId=-1;
    private Template selected;
    private LinearLayout root;
    private int ink=Color.rgb(31,43,62), muted=Color.rgb(111,122,139), accent=Color.rgb(72,91,176);

    static class Template {
        String name,category; int color,accent,style; Uri uri;
        Template(String n,String c,int col,int a,int st){name=n;category=c;color=col;accent=a;style=st;}
    }

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(246,247,251));
        getWindow().setNavigationBarColor(Color.rgb(246,247,251));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        templates.add(new Template("Золотая классика","Сертификаты",Color.rgb(250,246,235),Color.rgb(166,126,54),0));
        templates.add(new Template("Королевский орнамент","Дипломы",Color.rgb(247,240,222),Color.rgb(137,93,35),1));
        templates.add(new Template("Изумрудная вязь","Сертификаты",Color.rgb(235,246,239),Color.rgb(35,112,83),2));
        templates.add(new Template("Синий гильош","Дипломы",Color.rgb(235,241,251),Color.rgb(43,77,145),3));
        templates.add(new Template("Лавровый венок","Грамоты",Color.rgb(244,246,235),Color.rgb(104,125,65),4));
        templates.add(new Template("Розовое золото","Сертификаты",Color.rgb(250,239,237),Color.rgb(177,105,103),5));
        templates.add(new Template("Арт-деко","Дипломы",Color.rgb(31,39,57),Color.rgb(218,177,94),6));
        templates.add(new Template("Ночная звезда","Сертификаты",Color.rgb(31,39,66),Color.rgb(224,195,119),7));
        templates.add(new Template("Пергамент","Грамоты",Color.rgb(246,235,211),Color.rgb(130,91,48),8));
        templates.add(new Template("Сакура","Сертификаты",Color.rgb(252,239,244),Color.rgb(190,111,143),9));
        templates.add(new Template("Геометрия","Дипломы",Color.rgb(239,242,247),Color.rgb(75,91,119),10));
        templates.add(new Template("Праздничный узор","Грамоты",Color.rgb(250,243,229),Color.rgb(167,72,61),11));
        selected=templates.get(0);
        String savedTree=getPreferences(MODE_PRIVATE).getString("save_tree_uri",null);
        if(savedTree!=null)try{saveTree=Uri.parse(savedTree);}catch(Exception ignored){}
        updateDownloadId=getPreferences(MODE_PRIVATE).getLong("update_download_id",-1);
        build();
        registerUpdateReceiver();
    }

    private void build(){
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true);
        root=new LinearLayout(this); root.setOrientation(1); root.setPadding(dp(20),dp(22),dp(20),dp(32));
        root.setBackgroundColor(Color.rgb(246,247,251)); scroll.addView(root); setContentView(scroll);

        TextView brand=txt("SOLODEFSTUDIO",12,accent,true); brand.setLetterSpacing(.18f); root.addView(brand);
        TextView brandSub=txt("CERTIFICATE GENERATOR",9,muted,true); brandSub.setLetterSpacing(.14f); brandSub.setPadding(0,dp(3),0,0); root.addView(brandSub);
        TextView title=txt("Создавайте документы,\nкоторые запоминаются",27,ink,true); title.setPadding(0,dp(12),0,dp(5)); root.addView(title);
        TextView intro=txt("Персональные сертификаты и дипломы — сразу для всего списка участников.",14,muted,false); root.addView(intro);

        LinearLayout hero=card(); hero.setOrientation(1); hero.setPadding(dp(17),dp(16),dp(17),dp(16));
        LinearLayout.LayoutParams hp=params(-1,-2,18); root.addView(hero,hp);
        TextView step=txt("ШАГ 1 · ДИЗАЙН",11,accent,true); hero.addView(step);
        TextView chooseTitle=txt("Выберите оформление",19,ink,true); chooseTitle.setPadding(0,dp(6),0,dp(10)); hero.addView(chooseTitle);
        selectedLabel=txt("",13,muted,true); hero.addView(selectedLabel);
        LinearLayout previewRow=new LinearLayout(this); previewRow.setOrientation(0); previewRow.setPadding(0,dp(12),0,dp(8)); hero.addView(previewRow);
        addPreview(previewRow,templates.get(0)); addPreview(previewRow,templates.get(1)); addPreview(previewRow,templates.get(2));
        Button gallery=button("Открыть все шаблоны",accent); hero.addView(gallery); gallery.setOnClickListener(v->showTemplates());

        LinearLayout custom=card(); custom.setOrientation(0); custom.setGravity(Gravity.CENTER_VERTICAL); custom.setPadding(dp(16),dp(13),dp(16),dp(13));
        root.addView(custom,params(-1,-2,12));
        LinearLayout customText=new LinearLayout(this); customText.setOrientation(1); custom.addView(customText,new LinearLayout.LayoutParams(0,-2,1));
        customText.addView(txt("Свой дизайн",16,ink,true)); customText.addView(txt("Добавьте фоновый макет JPG, PNG или PDF",12,muted,false));
        Button importBtn=button("＋ Добавить",Color.rgb(235,238,250)); importBtn.setTextColor(accent); custom.addView(importBtn); importBtn.setOnClickListener(v->pickTemplate());

        TextView s2=section("ШАГ 2 · ДАННЫЕ"); root.addView(s2,params(-1,-2,24));
        LinearLayout data=card(); data.setOrientation(1); data.setPadding(dp(16),dp(14),dp(16),dp(16)); root.addView(data,params(-1,-2,10));
        Button csvBtn=button("Загрузить список участников · CSV",Color.rgb(235,238,250)); csvBtn.setTextColor(accent); data.addView(csvBtn); csvBtn.setOnClickListener(v->chooseCsv());
        csvStatus=txt("Файл не выбран",12,muted,false); csvStatus.setPadding(0,dp(7),0,dp(12)); data.addView(csvStatus);
        event=field("Название мероприятия или курса"); date=field("Дата выдачи"); issuer=field("Организация / подпись");
        addInput(data,"НАЗВАНИЕ",event); addInput(data,"ДАТА",date); addInput(data,"ОРГАНИЗАТОР",issuer);

        TextView s3=section("ШАГ 3 · РЕЗУЛЬТАТ"); root.addView(s3,params(-1,-2,22));
        LinearLayout destination=card(); destination.setOrientation(1); destination.setPadding(dp(15),dp(13),dp(15),dp(13)); root.addView(destination,params(-1,-2,12));
        TextView destinationTitle=txt("ПАПКА СОХРАНЕНИЯ",10,muted,true); destination.addView(destinationTitle);
        saveLocation=txt("",12,ink,false); saveLocation.setPadding(0,dp(6),0,dp(8)); destination.addView(saveLocation);
        Button folderBtn=button("Выбрать папку на телефоне",Color.rgb(235,238,250)); folderBtn.setTextColor(accent); destination.addView(folderBtn); folderBtn.setOnClickListener(v->chooseSaveFolder());
        updateSaveLocation();
        TextView note=txt("Для каждого участника будет создан отдельный PDF. Все файлы также соберём в один ZIP-архив.",13,muted,false); root.addView(note,params(-1,-2,12));
        Button make=button("Создать сертификаты  →",Color.rgb(39,53,83)); make.setTextColor(Color.WHITE); root.addView(make,params(-1,dp(54),14)); make.setOnClickListener(v->generate());
        TextView footer=txt("Ваши макеты и документы остаются на устройстве.",11,muted,false); footer.setGravity(Gravity.CENTER); footer.setPadding(0,dp(18),0,dp(5)); root.addView(footer);
        TextView studio=txt("Разработано SoloDefStudio",10,accent,true); studio.setGravity(Gravity.CENTER); studio.setPadding(0,dp(3),0,dp(8)); root.addView(studio);
        Button updateBtn=button("Проверить обновления",Color.rgb(235,238,250)); updateBtn.setTextColor(accent); root.addView(updateBtn,params(-1,dp(48),6)); updateBtn.setOnClickListener(v->checkForUpdates(updateBtn));
        TextView version=txt("Версия "+currentVersion(),10,muted,false); version.setGravity(Gravity.CENTER); version.setPadding(0,dp(5),0,0); root.addView(version);
        updateSelected();
    }

    private void addPreview(LinearLayout row,Template t){
        LinearLayout wrap=new LinearLayout(this); wrap.setOrientation(1); wrap.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams wp=new LinearLayout.LayoutParams(0,-2,1); wp.setMargins(dp(3),0,dp(3),0); row.addView(wrap,wp);
        FrameLayout frame=new FrameLayout(this); frame.setBackground(drawable(t.color,9,t==selected?accent:Color.rgb(226,229,236),t==selected?3:1));
        LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(-1,dp(72)); wrap.addView(frame,fp);
        drawPreviewArtwork(frame,t);
        TextView name=txt(t.name,10,ink,true); name.setGravity(Gravity.CENTER); name.setPadding(0,dp(5),0,0); wrap.addView(name);
        wrap.setOnClickListener(v->{selected=t;updateSelected();showTemplates();});
    }
    private void drawPreviewArtwork(FrameLayout frame,Template t){
        View pattern=new PreviewPattern(this,t); frame.addView(pattern,new FrameLayout.LayoutParams(-1,-1));
        View border=new View(this); border.setBackground(drawable(Color.TRANSPARENT,4,t.accent,2));
        FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(-1,-1); bp.setMargins(dp(5),dp(5),dp(5),dp(5)); frame.addView(border,bp);
        LinearLayout art=new LinearLayout(this); art.setOrientation(1); art.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams ap=new FrameLayout.LayoutParams(-1,-1); ap.setMargins(dp(9),dp(7),dp(9),dp(7)); frame.addView(art,ap);
        TextView star=txt("✦",13,t.accent,true); star.setGravity(Gravity.CENTER); art.addView(star);
        TextView heading=txt(t.category.equals("Дипломы")?"ДИПЛОМ":t.category.equals("Грамоты")?"ГРАМОТА":"СЕРТИФИКАТ",7,t.accent,true); heading.setGravity(Gravity.CENTER); art.addView(heading);
        TextView line=txt("ИМЯ УЧАСТНИКА",7,ink,true); line.setGravity(Gravity.CENTER); line.setPadding(0,dp(3),0,0); art.addView(line);
        View rule=new View(this); rule.setBackgroundColor(t.accent); LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(dp(48),dp(1)); rp.topMargin=dp(3); art.addView(rule,rp);
    }
    private void showTemplates(){
        ScrollView scroll=new ScrollView(this); LinearLayout list=new LinearLayout(this); list.setOrientation(1); list.setPadding(dp(10),dp(8),dp(10),dp(8)); scroll.addView(list);
        for(int i=0;i<templates.size();i+=2){
            LinearLayout row=new LinearLayout(this); row.setOrientation(0); list.addView(row,params(-1,-2,8));
            for(int j=i;j<Math.min(i+2,templates.size());j++){
                final Template t=templates.get(j);
                LinearLayout item=new LinearLayout(this); item.setOrientation(1); item.setPadding(dp(7),dp(7),dp(7),dp(7));
                item.setBackground(drawable(Color.WHITE,13,t==selected?accent:Color.rgb(232,235,241),t==selected?3:1));
                LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(0,-2,1); ip.setMargins(dp(4),0,dp(4),0); row.addView(item,ip);
                FrameLayout preview=new FrameLayout(this); preview.setBackground(drawable(t.color,8,Color.TRANSPARENT,0)); item.addView(preview,new LinearLayout.LayoutParams(-1,dp(105)));
                drawPreviewArtwork(preview,t);
                TextView name=txt(t.name,13,ink,true); name.setPadding(0,dp(8),0,dp(2)); item.addView(name);
                TextView category=txt(t.category,10,muted,false); item.addView(category);
                TextView state=txt(t==selected?"✓ Выбран":"Нажмите для выбора",10,t==selected?accent:muted,true); state.setPadding(0,dp(5),0,0); item.addView(state);
                item.setOnClickListener(v->{selected=t;updateSelected();});
            }
        }
        new AlertDialog.Builder(this).setTitle("Выберите дизайн сертификата").setView(scroll).setPositiveButton("Готово",null).show();
    }
    private void updateSelected(){if(selectedLabel!=null) selectedLabel.setText("✓ Выбранный дизайн: "+selected.name+"  ·  "+selected.category);}
    private void pickTemplate(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"image/jpeg","image/png","application/pdf"}); startActivityForResult(i,PICK_TEMPLATE);
    }
    private void chooseCsv(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("text/*"); startActivityForResult(i,PICK_CSV);
    }
    @Override protected void onActivityResult(int req,int res,Intent data){
        super.onActivityResult(req,res,data); if(res!=RESULT_OK||data==null)return;
        Uri u=data.getData(); if(u==null)return;
        try{
            int grantFlags=data.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            getContentResolver().takePersistableUriPermission(u,grantFlags);
        }catch(Exception ignored){}
        if(req==PICK_CSV){csv=u;try{people.clear();people.addAll(readNames(u));csvStatus.setText("✓ Загружено участников: "+people.size());}catch(Exception e){csvStatus.setText("Не удалось прочитать CSV: "+e.getMessage());}}
        if(req==PICK_TEMPLATE){
            String mime=getContentResolver().getType(u); String n="Мой шаблон "+(templates.size()-3);
            Template t=new Template(n,"Мои шаблоны",Color.rgb(246,245,241),Color.rgb(104,91,142),0); t.uri=u; templates.add(t); selected=t; updateSelected();
            new AlertDialog.Builder(this).setTitle("Шаблон добавлен").setMessage("Макет добавлен в галерею. Он будет использоваться как фон сертификата.").setPositiveButton("Отлично",null).show();
        }
        if(req==PICK_FOLDER){
            saveTree=u;
            getPreferences(MODE_PRIVATE).edit().putString("save_tree_uri",u.toString()).apply();
            updateSaveLocation();
            toast("Папка сохранения выбрана");
        }
    }

    private class PreviewPattern extends View {
        private final Template t; private final Paint paint=new Paint(3);
        PreviewPattern(Context context,Template template){super(context);t=template;}
        @Override protected void onDraw(Canvas canvas){
            super.onDraw(canvas);float w=getWidth(),h=getHeight();paint.setColor(t.accent);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.2f);
            canvas.drawRoundRect(5,5,w-5,h-5,7,7,paint);
            if(t.style%4==0){for(int y=15;y<h;y+=24){canvas.drawCircle(13,y,5,paint);canvas.drawCircle(w-13,y,5,paint);}canvas.drawOval(w*.22f,h*.12f,w*.78f,h*.9f,paint);}
            else if(t.style%4==1){for(int x=15;x<w;x+=22){canvas.drawCircle(x,12,3,paint);canvas.drawCircle(x,h-12,3,paint);}}
            else if(t.style%4==2){for(int k=0;k<8;k++){double a=k*Math.PI/4;canvas.drawCircle((float)(w/2+Math.cos(a)*w*.36),(float)(h/2+Math.sin(a)*h*.34),5,paint);}}
            else{for(int x=12;x<w-12;x+=25){canvas.drawLine(x,8,x+12,20,paint);canvas.drawLine(x+12,20,x+24,8,paint);}}
            paint.setStyle(Paint.Style.FILL);
        }
    }

    private List<String> readNames(Uri uri)throws Exception{
        ArrayList<String> out=new ArrayList<>(); BufferedReader br=new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri),StandardCharsets.UTF_8));
        String line;boolean first=true;int col=0;while((line=br.readLine())!=null){if(line.trim().isEmpty())continue;List<String> cells=parse(line);
            if(first){first=false;for(int n=0;n<cells.size();n++){String h=cells.get(n).trim().toLowerCase(Locale.ROOT).replace("\uFEFF","");if(h.equals("фио")||h.equals("name")||h.equals("full_name")||h.equals("participant")){col=n;break;}}
                String h=cells.get(col).trim().toLowerCase(Locale.ROOT);if(h.equals("фио")||h.equals("name")||h.equals("full_name")||h.equals("participant"))continue;}
            if(col<cells.size()&&!cells.get(col).trim().isEmpty())out.add(cells.get(col).trim());}br.close();return out;
    }
    private List<String> parse(String s){ArrayList<String>a=new ArrayList<>();StringBuilder b=new StringBuilder();boolean q=false;for(int i=0;i<s.length();i++){char c=s.charAt(i);if(c=='"'){if(q&&i+1<s.length()&&s.charAt(i+1)=='"'){b.append('"');i++;}else q=!q;}else if((c==','||c==';'||c=='\t')&&!q){a.add(b.toString());b.setLength(0);}else b.append(c);}a.add(b.toString());return a;}

    private void generate(){
        if(people.isEmpty()){toast("Сначала загрузите CSV со списком участников");return;}
        String title=event.getText().toString().trim(),d=date.getText().toString().trim(),org=issuer.getText().toString().trim();
        if(title.isEmpty()||d.isEmpty()){toast("Заполните название и дату");return;}
        try{
            if(saveTree!=null){
                saveCertificatesToTree(title,d,org);
                new AlertDialog.Builder(this).setTitle("Готово!").setMessage("Создано документов: "+people.size()+"\\n\\nPDF и ZIP сохранены в выбранную папку, в новой папке с датой и временем.").setPositiveButton("Понятно",null).show();
            }else{
                File dir=new File(getExternalFilesDir(null),"Certificates");if(!dir.exists())dir.mkdirs();File zip=new File(dir,"certificates_"+System.currentTimeMillis()+".zip");
                try(ZipOutputStream zos=new ZipOutputStream(new FileOutputStream(zip))){
                    for(int n=0;n<people.size();n++){String fn=String.format(Locale.US,"%03d_%s.pdf",n+1,safe(people.get(n)));byte[] bytes=pdf(people.get(n),title,d,org,n+1);
                        try(FileOutputStream f=new FileOutputStream(new File(dir,fn))){f.write(bytes);}zos.putNextEntry(new ZipEntry(fn));zos.write(bytes);zos.closeEntry();}
                }
                new AlertDialog.Builder(this).setTitle("Готово!").setMessage("Создано документов: "+people.size()+"\\n\\nПапка: "+dir.getAbsolutePath()+"\\nZIP: "+zip.getName()).setPositiveButton("Понятно",null).show();
            }
        }catch(Exception e){toast("Ошибка сохранения: "+e.getMessage());}
    }
    private String currentVersion(){
        try{return getPackageManager().getPackageInfo(getPackageName(),0).versionName;}catch(Exception e){return "неизвестна";}
    }
    private void checkForUpdates(Button button){
        button.setEnabled(false); button.setText("Проверяем...");
        new Thread(()->{
            String latest=null,download=null,releasePage=null,error=null;
            HttpURLConnection connection=null;
            try{
                URL url=new URL("https://api.github.com/repos/kotegish/certificate-generator-android/releases/latest");
                connection=(HttpURLConnection)url.openConnection();
                connection.setRequestMethod("GET"); connection.setConnectTimeout(10000); connection.setReadTimeout(10000);
                connection.setRequestProperty("Accept","application/vnd.github+json");
                connection.setRequestProperty("User-Agent","CertificateGenerator-Android");
                int code=connection.getResponseCode();
                if(code==404)throw new IOException("Пока нет опубликованных релизов.");
                if(code!=200)throw new IOException("Сервер вернул ошибку "+code);
                StringBuilder body=new StringBuilder();
                try(BufferedReader reader=new BufferedReader(new InputStreamReader(connection.getInputStream(),StandardCharsets.UTF_8))){
                    String line;while((line=reader.readLine())!=null)body.append(line);
                }
                JSONObject release=new JSONObject(body.toString());
                latest=release.optString("tag_name","");
                releasePage=release.optString("html_url","");
                JSONArray assets=release.optJSONArray("assets");
                if(assets!=null)for(int i=0;i<assets.length();i++){
                    JSONObject asset=assets.getJSONObject(i);
                    String name=asset.optString("name","");
                    if(name.toLowerCase(Locale.ROOT).endsWith(".apk")){download=asset.optString("browser_download_url","");break;}
                }
                if(latest.isEmpty())throw new IOException("У релиза не указана версия.");
            }catch(Exception e){error=e.getMessage()==null?"Не удалось проверить обновления":e.getMessage();}
            finally{if(connection!=null)connection.disconnect();}
            final String v=latest,apk=download,page=releasePage,problem=error;
            new Handler(Looper.getMainLooper()).post(()->{
                button.setEnabled(true);button.setText("Проверить обновления");
                if(problem!=null){new AlertDialog.Builder(this).setTitle("Проверка обновлений").setMessage(problem+"\\n\\nТекущая версия: "+currentVersion()).setPositiveButton("Понятно",null).show();return;}
                if(apk==null||apk.isEmpty()){
                    new AlertDialog.Builder(this).setTitle("Новая версия: "+v).setMessage("Релиз опубликован, но APK-файл к нему не прикреплён. Откройте страницу релиза позже.").setPositiveButton("Открыть релиз",(d,w)->openUpdateUrl(page)).setNegativeButton("Закрыть",null).show();return;
                }
                if(normalizeVersion(v).equals(normalizeVersion(currentVersion()))){
                    new AlertDialog.Builder(this).setTitle("Обновления не найдены").setMessage("Установлена последняя версия: "+currentVersion()).setPositiveButton("Понятно",null).show();
                }else{
                    new AlertDialog.Builder(this).setTitle("Доступно обновление").setMessage("Новая версия: "+v+"\\nУстановлена: "+currentVersion()+"\\n\\nСкачать APK?").setPositiveButton("Скачать",(d,w)->downloadUpdate(apk)).setNegativeButton("Позже",null).show();
                }
            });
        }).start();
    }
    private void registerUpdateReceiver(){
        updateReceiver=new BroadcastReceiver(){
            @Override public void onReceive(Context context,Intent intent){
                if(DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())){
                    long id=intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID,-1);
                    if(id==getPreferences(MODE_PRIVATE).getLong("update_download_id",-2))processUpdateDownload(id);
                }
            }
        };
        IntentFilter filter=new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        if(android.os.Build.VERSION.SDK_INT>=33)registerReceiver(updateReceiver,filter,Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(updateReceiver,filter);
    }
    private void processUpdateDownload(long id){
        DownloadManager manager=(DownloadManager)getSystemService(DOWNLOAD_SERVICE);
        DownloadManager.Query query=new DownloadManager.Query().setFilterById(id);
        try(Cursor cursor=manager.query(query)){
            if(cursor==null||!cursor.moveToFirst())return;
            int status=cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
            if(status==DownloadManager.STATUS_SUCCESSFUL){
                Uri apk=manager.getUriForDownloadedFile(id);
                if(apk!=null){
                    getPreferences(MODE_PRIVATE).edit().putString("pending_update_uri",apk.toString()).remove("update_download_id").apply();
                    updateDownloadId=-1;
                    installPendingUpdate();
                }else toast("Не удалось получить загруженный APK");
            }else if(status==DownloadManager.STATUS_FAILED){
                int reason=cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON));
                getPreferences(MODE_PRIVATE).edit().remove("update_download_id").apply();
                updateDownloadId=-1;
                toast("Загрузка обновления не удалась. Код: "+reason);
            }
        }catch(Exception e){toast("Ошибка загрузки обновления: "+e.getMessage());}
    }
    @Override protected void onDestroy(){
        if(updateReceiver!=null)try{unregisterReceiver(updateReceiver);}catch(Exception ignored){}
        super.onDestroy();
    }
    @Override protected void onResume(){
        super.onResume();
        long pendingId=getPreferences(MODE_PRIVATE).getLong("update_download_id",-1);
        if(pendingId!=-1)processUpdateDownload(pendingId);
        String pending=getPreferences(MODE_PRIVATE).getString("pending_update_uri",null);
        if(pending!=null)installPendingUpdate();
    }
    private void downloadUpdate(String url){
        try{
            DownloadManager manager=(DownloadManager)getSystemService(DOWNLOAD_SERVICE);
            DownloadManager.Request request=new DownloadManager.Request(Uri.parse(url));
            request.setTitle("Обновление SoloDefStudio");
            request.setDescription("Загрузка новой версии приложения");
            request.setMimeType("application/vnd.android.package-archive");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalFilesDir(this,android.os.Environment.DIRECTORY_DOWNLOADS,"SoloDefStudio-update.apk");
            updateDownloadId=manager.enqueue(request);
            getPreferences(MODE_PRIVATE).edit().putLong("update_download_id",updateDownloadId).apply();
            toast("Загрузка обновления началась. Уведомление покажет её завершение.");
        }catch(Exception e){toast("Не удалось начать загрузку: "+e.getMessage());}
    }
    private void installPendingUpdate(){
        String value=getPreferences(MODE_PRIVATE).getString("pending_update_uri",null);
        if(value==null)return;
        try{
            if(android.os.Build.VERSION.SDK_INT>=26&&!getPackageManager().canRequestPackageInstalls()){
                new AlertDialog.Builder(this).setTitle("Разрешите установку обновления")
                    .setMessage("Android просит разрешить установку обновлений для этого приложения. Включите разрешение и вернитесь сюда.")
                    .setPositiveButton("Открыть настройки",(d,w)->{
                        Intent settings=new Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+getPackageName()));
                        startActivity(settings);
                    }).setNegativeButton("Позже",null).show();
                return;
            }
            Uri apk=Uri.parse(value);
            Intent install=new Intent(Intent.ACTION_VIEW);
            install.setDataAndType(apk,"application/vnd.android.package-archive");
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(install);
            getPreferences(MODE_PRIVATE).edit().remove("pending_update_uri").apply();
        }catch(Exception e){toast("Не удалось открыть установщик: "+e.getMessage());}
    }
    private String normalizeVersion(String version){return version==null?"":version.trim().toLowerCase(Locale.ROOT).replaceFirst("^v","");}
    private void openUpdateUrl(String url){
        try{Intent intent=new Intent(Intent.ACTION_VIEW,Uri.parse(url));startActivity(intent);}
        catch(Exception e){toast("Не удалось открыть ссылку обновления");}
    }
    private void chooseSaveFolder(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i,PICK_FOLDER);
    }
    private void updateSaveLocation(){
        if(saveLocation==null)return;
        if(saveTree==null)saveLocation.setText("По умолчанию: папка приложения Certificates во внутреннем хранилище Android");
        else saveLocation.setText("Выбрана папка телефона. PDF и ZIP будут сохраняться туда.");
    }
    private Uri saveCertificatesToTree(String title,String d,String org)throws Exception{
        String folderName="Certificates_"+new java.text.SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(new Date());
        String rootId=DocumentsContract.getTreeDocumentId(saveTree);
        Uri rootDoc=DocumentsContract.buildDocumentUriUsingTree(saveTree,rootId);
        Uri runFolder=DocumentsContract.createDocument(getContentResolver(),rootDoc,DocumentsContract.Document.MIME_TYPE_DIR,folderName);
        if(runFolder==null)throw new IOException("Не удалось создать папку в выбранном месте");
        Uri zipUri=DocumentsContract.createDocument(getContentResolver(),runFolder,"application/zip","certificates.zip");
        if(zipUri==null)throw new IOException("Не удалось создать ZIP-архив");
        OutputStream raw=getContentResolver().openOutputStream(zipUri);
        if(raw==null)throw new IOException("Не удалось открыть ZIP для записи");
        try(ZipOutputStream zos=new ZipOutputStream(raw)){
            for(int n=0;n<people.size();n++){
                String fn=String.format(Locale.US,"%03d_%s.pdf",n+1,safe(people.get(n)));
                byte[] bytes=pdf(people.get(n),title,d,org,n+1);
                Uri pdfUri=DocumentsContract.createDocument(getContentResolver(),runFolder,"application/pdf",fn);
                if(pdfUri==null)throw new IOException("Не удалось создать файл "+fn);
                OutputStream out=getContentResolver().openOutputStream(pdfUri);
                if(out==null)throw new IOException("Не удалось записать "+fn);
                try(OutputStream fileOut=out){fileOut.write(bytes);}
                zos.putNextEntry(new ZipEntry(fn));zos.write(bytes);zos.closeEntry();
            }
        }
        return runFolder;
    }

    private byte[] pdf(String person,String title,String d,String org,int num)throws Exception{
        PdfDocument doc=new PdfDocument();PdfDocument.Page page=doc.startPage(new PdfDocument.PageInfo.Builder(842,595,1).create());Canvas c=page.getCanvas();Paint p=new Paint(3);
        if(selected.uri!=null){Bitmap bg=loadTemplate(selected.uri);if(bg!=null){c.drawBitmap(bg,null,new Rect(0,0,842,595),p);bg.recycle();}else drawDesign(c,p,selected);}
        else drawDesign(c,p,selected);
        if(selected.uri!=null){
            p.setColor(Color.rgb(35,42,52));p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.create("serif",Typeface.BOLD));p.setTextSize(25);
            c.drawText("СЕРТИФИКАТ",421,145,p);p.setTypeface(Typeface.create("serif",Typeface.NORMAL));p.setTextSize(19);c.drawText("настоящим подтверждается, что",421,218,p);
            p.setTypeface(Typeface.create("serif",Typeface.BOLD));p.setTextSize(34);drawFit(c,person,421,300,690,p,34,18);
            p.setTypeface(Typeface.create("sans-serif",0));p.setTextSize(17);c.drawText("успешно принял(а) участие",421,355,p);p.setTypeface(Typeface.create("serif",Typeface.BOLD));p.setTextSize(23);drawFit(c,title,421,405,690,p,23,14);
            p.setTypeface(Typeface.create("sans-serif",0));p.setTextSize(14);c.drawText(d+"     ·     № "+String.format(Locale.US,"%05d",num),421,485,p);c.drawText(org,421,530,p);
        }else{
            p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.create("serif",Typeface.BOLD));p.setColor(selected.accent);p.setTextSize(17);c.drawText(selected.category.equals("Дипломы")?"ДИПЛОМ":selected.category.equals("Грамоты")?"ПОЧЁТНАЯ ГРАМОТА":"СЕРТИФИКАТ",421,132,p);
            p.setColor(ink);p.setTypeface(Typeface.create("serif",0));p.setTextSize(23);c.drawText("настоящим подтверждается, что",421,205,p);
            p.setTypeface(Typeface.create("serif",Typeface.BOLD));p.setTextSize(34);drawFit(c,person,421,285,690,p,34,18);
            p.setTypeface(Typeface.create("sans-serif",0));p.setTextSize(18);c.drawText("успешно принял(а) участие в мероприятии",421,350,p);
            p.setTypeface(Typeface.create("serif",Typeface.BOLD));p.setTextSize(25);p.setColor(selected.accent);drawFit(c,title,421,405,690,p,25,15);
            p.setTypeface(Typeface.create("sans-serif",0));p.setTextSize(14);p.setColor(muted);c.drawText("Дата: "+d,250,480,p);c.drawText("№ "+String.format(Locale.US,"%05d",num),590,480,p);
            p.setStrokeWidth(1);c.drawLine(90,515,300,515,p);c.drawLine(540,515,750,515,p);p.setTextSize(12);c.drawText(org.isEmpty()?"Организатор":org,195,535,p);c.drawText("Подпись",645,535,p);
        }
        doc.finishPage(page);ByteArrayOutputStream out=new ByteArrayOutputStream();doc.writeTo(out);doc.close();return out.toByteArray();
    }
    private Bitmap loadTemplate(Uri uri)throws Exception{
        String type=getContentResolver().getType(uri);
        if(type!=null&&type.equals("application/pdf")){
            try(ParcelFileDescriptor fd=getContentResolver().openFileDescriptor(uri,"r");PdfRenderer renderer=new PdfRenderer(fd)){if(renderer.getPageCount()==0)return null;PdfRenderer.Page pg=renderer.openPage(0);Bitmap b=Bitmap.createBitmap(1684,1190,Bitmap.Config.ARGB_8888);b.eraseColor(Color.WHITE);pg.render(b,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);pg.close();return b;}
        }
        try(InputStream in=getContentResolver().openInputStream(uri)){return BitmapFactory.decodeStream(in);}
    }
    private void drawDesign(Canvas c,Paint p,Template t){
        p.setColor(t.color);c.drawColor(t.color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(4);p.setColor(t.accent);c.drawRect(24,24,818,571,p);p.setStrokeWidth(1);c.drawRect(36,36,806,559,p);
        p.setStyle(Paint.Style.FILL);p.setTextAlign(Paint.Align.CENTER);p.setColor(t.accent);p.setTextSize(30);c.drawText("✦",421,75,p);
        p.setStrokeWidth(1.5f);p.setStyle(Paint.Style.STROKE);
        if(t.style%4==0){
            for(int side:new int[]{0,1}){float x=side==0?72:770;for(int k=0;k<5;k++){c.drawCircle(x,110+k*88,18,p);c.drawCircle(x,110+k*88,11,p);}}
            c.drawOval(170,54,672,541,p);
        }else if(t.style%4==1){
            for(int x=90;x<=752;x+=44){c.drawCircle(x,58,7,p);c.drawCircle(x,537,7,p);}
            for(int y=105;y<=490;y+=48){c.drawCircle(57,y,7,p);c.drawCircle(785,y,7,p);}
            c.drawRoundRect(52,52,790,543,24,24,p);
        }else if(t.style%4==2){
            for(int k=0;k<12;k++){double a=k*Math.PI/6;float x=(float)(421+Math.cos(a)*290),y=(float)(297+Math.sin(a)*205);c.drawCircle(x,y,22,p);c.drawCircle(x,y,12,p);}
            c.drawCircle(421,297,145,p);c.drawCircle(421,297,153,p);
        }else{
            for(int k=0;k<9;k++){float inset=48+k*8;c.drawRoundRect(inset,inset*.62f,842-inset,595-inset*.62f,18,18,p);}
            for(int x=105;x<760;x+=55){c.drawLine(x,44,x+24,68,p);c.drawLine(x+24,68,x+48,44,p);}
        }
        p.setStyle(Paint.Style.FILL);
    }
    private void drawFit(Canvas c,String s,float x,float y,float width,Paint p,float size,float min){p.setTextSize(size);while(p.measureText(s)>width&&size>min){size--;p.setTextSize(size);}c.drawText(s,x,y,p);}
    private String safe(String s){return s.replaceAll("[\\\\/:*?\"<>|]","_").trim().replaceAll("\\s+","_");}
    private LinearLayout card(){LinearLayout l=new LinearLayout(this);l.setBackground(drawable(Color.WHITE,18,Color.rgb(232,235,241),1));return l;}
    private TextView txt(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(null,1);return t;}
    private TextView section(String s){TextView t=txt(s,11,accent,true);t.setLetterSpacing(.12f);return t;}
    private EditText field(String hint){EditText e=new EditText(this);e.setSingleLine(true);e.setTextSize(14);e.setHint(hint);e.setPadding(dp(12),0,dp(12),0);e.setBackground(drawable(Color.rgb(247,248,251),10,Color.rgb(232,235,241),1));return e;}
    private void addInput(LinearLayout box,String label,EditText e){TextView l=txt(label,10,muted,true);l.setPadding(0,dp(8),0,dp(5));box.addView(l);box.addView(e,params(-1,dp(48),0));}
    private Button button(String s,int color){Button b=new Button(this);b.setText(s);b.setTextSize(13);b.setAllCaps(false);b.setTypeface(null,1);b.setBackground(drawable(color,12,Color.TRANSPARENT,0));return b;}
    private GradientDrawable drawable(int color,int radius,int stroke,int sw){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));if(sw>0)d.setStroke(dp(sw),stroke);return d;}
    private LinearLayout.LayoutParams params(int w,int h,int top){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h);if(top>0)p.topMargin=dp(top);return p;}
    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
}