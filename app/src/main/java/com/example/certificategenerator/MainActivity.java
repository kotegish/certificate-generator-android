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
    private LinearLayout root, previewRow;
    private int ink=Color.rgb(31,43,62), muted=Color.rgb(111,122,139), accent=Color.rgb(72,91,176);

    static class TemplateField {
        String key,label; float x,y,size;
        TemplateField(String k,String l,float px,float py,float sz){key=k;label=l;x=px;y=py;size=sz;}
    }
    static class Template {
        String name,category; int color,accent,style; Uri uri;
        ArrayList<TemplateField> fields=new ArrayList<>();
        Template(String n,String c,int col,int a,int st){name=n;category=c;color=col;accent=a;style=st;}
    }

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(246,247,251));
        getWindow().setNavigationBarColor(Color.rgb(246,247,251));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        // Только художественная коллекция: 14 авторских оформлений.
        templates.add(new Template("Барокко · Золото","Дипломы",Color.rgb(250,244,226),Color.rgb(151,105,45),21));
        templates.add(new Template("Королевская печать","Сертификаты",Color.rgb(246,239,219),Color.rgb(126,82,31),22));
        templates.add(new Template("Флорентийская вязь","Дипломы",Color.rgb(239,246,237),Color.rgb(48,111,77),23));
        templates.add(new Template("Лавровая симфония","Грамоты",Color.rgb(243,246,235),Color.rgb(99,119,62),24));
        templates.add(new Template("Ажурное золото","Сертификаты",Color.rgb(251,246,235),Color.rgb(172,127,56),25));
        templates.add(new Template("Пудровый шёлк","Сертификаты",Color.rgb(252,240,242),Color.rgb(174,112,131),26));
        templates.add(new Template("Золотая классика","Дипломы",Color.rgb(250,247,239),Color.rgb(174,133,61),27));
        templates.add(new Template("Винтажный герб","Грамоты",Color.rgb(248,243,231),Color.rgb(130,91,43),28));
        templates.add(new Template("Изумрудный сад","Сертификаты",Color.rgb(237,245,239),Color.rgb(43,112,82),29));
        templates.add(new Template("Сапфировый атлас","Дипломы",Color.rgb(239,244,251),Color.rgb(48,83,145),30));
        templates.add(new Template("Лавровая награда","Грамоты",Color.rgb(245,247,237),Color.rgb(105,126,69),31));
        templates.add(new Template("Розовое золото","Сертификаты",Color.rgb(252,243,241),Color.rgb(181,119,112),32));
        templates.add(new Template("Арт-деко · Вечер","Дипломы",Color.rgb(30,37,53),Color.rgb(222,184,104),33));
        templates.add(new Template("Перламутровый минимализм","Сертификаты",Color.rgb(250,250,248),Color.rgb(77,91,111),34));
        restoreCustomTemplates();
        selected=templates.get(0);
        String savedTree=getPreferences(MODE_PRIVATE).getString("save_tree_uri",null);
        if(savedTree!=null)try{saveTree=Uri.parse(savedTree);}catch(Exception ignored){}
        updateDownloadId=getPreferences(MODE_PRIVATE).getLong("update_download_id",-1);
        build();
        registerUpdateReceiver();
        new Handler(Looper.getMainLooper()).postDelayed(this::checkForUpdatesAutomatically, 1500);
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
        previewRow=new LinearLayout(this); previewRow.setOrientation(0); previewRow.setPadding(0,dp(12),0,dp(8)); hero.addView(previewRow);
        refreshPreviewRow();
        Button gallery=button("Открыть все шаблоны",accent); hero.addView(gallery); gallery.setOnClickListener(v->showTemplates());

        LinearLayout custom=card(); custom.setOrientation(0); custom.setGravity(Gravity.CENTER_VERTICAL); custom.setPadding(dp(16),dp(13),dp(16),dp(13));
        root.addView(custom,params(-1,-2,12));
        LinearLayout customText=new LinearLayout(this); customText.setOrientation(1); custom.addView(customText,new LinearLayout.LayoutParams(0,-2,1));
        customText.addView(txt("Свой дизайн",16,ink,true)); customText.addView(txt("Добавьте фоновый макет JPG, PNG или PDF",12,muted,false));
        Button importBtn=button("＋ Добавить",Color.rgb(235,238,250)); importBtn.setTextColor(accent); custom.addView(importBtn); importBtn.setOnClickListener(v->pickTemplate());

        TextView galleryHint=txt("В галерее — готовые оформления для дипломов, грамот, конкурсов и beauty-обучения.",12,muted,false); galleryHint.setPadding(0,dp(8),0,0); root.addView(galleryHint,params(-1,-2,4));

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
        frame.addView(new CertificateArtwork(this,t),new FrameLayout.LayoutParams(-1,-1));
    }
    // Галерея и PDF используют одну функцию отрисовки, поэтому превью совпадает с результатом.
    private class CertificateArtwork extends View {
        private final Template template; private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
        CertificateArtwork(Context context,Template t){super(context);template=t;}
        @Override protected void onDraw(Canvas canvas){
            super.onDraw(canvas);
            float scale=Math.min(getWidth()/842f,getHeight()/595f);
            float dx=(getWidth()-842f*scale)/2f,dy=(getHeight()-595f*scale)/2f;
            canvas.save();canvas.translate(dx,dy);canvas.scale(scale,scale);
            if(template.uri!=null){
                try{Bitmap original=loadTemplate(template.uri);if(original!=null){canvas.drawBitmap(original,null,new Rect(0,0,842,595),paint);original.recycle();}}
                catch(Exception ignored){}
                drawCustomFields(canvas,paint,template,"ИМЯ УЧАСТНИКА","Название мероприятия","12.12.2026","Организатор",1);
            }else drawCertificate(canvas,paint,template,"ИМЯ УЧАСТНИКА","Название мероприятия","12.12.2026","Организатор",1,true);
            canvas.restore();
        }
    }
    private void drawCertificate(Canvas c,Paint p,Template t,String person,String title,String d,String org,int num,boolean preview){
        drawDesign(c,p,t);
        p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.create("serif",Typeface.BOLD));p.setColor(t.accent);p.setTextSize(17);
        c.drawText(t.category.equals("Дипломы")?"ДИПЛОМ":t.category.equals("Грамоты")?"ПОЧЁТНАЯ ГРАМОТА":"СЕРТИФИКАТ",421,132,p);
        p.setColor(ink);p.setTypeface(Typeface.create("serif",Typeface.NORMAL));p.setTextSize(21);c.drawText("настоящим подтверждается, что",421,205,p);
        p.setTypeface(Typeface.create("serif",Typeface.BOLD));p.setTextSize(preview?29:34);drawFit(c,person,421,285,690,p,preview?29:34,18);
        p.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));p.setTextSize(17);c.drawText("успешно принял(а) участие в мероприятии",421,350,p);
        p.setTypeface(Typeface.create("serif",Typeface.BOLD));p.setTextSize(23);p.setColor(t.accent);drawFit(c,title,421,405,690,p,23,15);
        p.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));p.setTextSize(14);p.setColor(muted);
        c.drawText("Дата: "+d,250,480,p);c.drawText("№ "+String.format(Locale.US,"%05d",num),590,480,p);
        p.setStrokeWidth(1);c.drawLine(90,515,300,515,p);c.drawLine(540,515,750,515,p);p.setTextSize(12);
        c.drawText(org.isEmpty()?"Организатор":org,195,535,p);c.drawText("Подпись",645,535,p);
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
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Выберите дизайн сертификата").setView(scroll).setNegativeButton("Закрыть",null).create();
        dialog.show();
        for(int i=0;i<list.getChildCount();i++){
            LinearLayout row=(LinearLayout)list.getChildAt(i);
            for(int j=0;j<row.getChildCount();j++){
                View item=row.getChildAt(j);
                final int index=i*2+j;
                item.setOnClickListener(v->{selected=templates.get(index);updateSelected();dialog.dismiss();});
            }
        }
    }
    private void refreshPreviewRow(){
        if(previewRow==null||templates.isEmpty())return;
        previewRow.removeAllViews();
        int index=templates.indexOf(selected);if(index<0)index=0;
        for(int n=0;n<3;n++)addPreview(previewRow,templates.get((index+n)%templates.size()));
    }
    private void updateSelected(){
        if(selectedLabel!=null)selectedLabel.setText("✓ Выбранный дизайн: "+selected.name+"  ·  "+selected.category);
        refreshPreviewRow();
    }
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
            String n="Мой шаблон "+(templates.size()-3);
            Template t=new Template(n,"Мои шаблоны",Color.rgb(246,245,241),Color.rgb(104,91,142),0); t.uri=u;
            t.fields.add(new TemplateField("person","Имя и фамилия",.50f,.48f,34));
            t.fields.add(new TemplateField("title","Название курса",.50f,.64f,22));
            t.fields.add(new TemplateField("date","Дата",.30f,.82f,16));
            t.fields.add(new TemplateField("org","Организатор / подпись",.74f,.90f,14));
            templates.add(t); selected=t; updateSelected(); saveCustomTemplate(t); saveCustomTemplateList(); showFieldEditor(t);
        }
        if(req==PICK_FOLDER){
            saveTree=u;
            getPreferences(MODE_PRIVATE).edit().putString("save_tree_uri",u.toString()).apply();
            updateSaveLocation();
            toast("Папка сохранения выбрана");
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
    private void checkForUpdatesAutomatically(){
        long last=getPreferences(MODE_PRIVATE).getLong("last_auto_update_check",0);
        if(System.currentTimeMillis()-last<6L*60L*60L*1000L)return;
        getPreferences(MODE_PRIVATE).edit().putLong("last_auto_update_check",System.currentTimeMillis()).apply();
        checkForUpdates(null);
    }
    private void checkForUpdates(Button button){
        if(button!=null){button.setEnabled(false); button.setText("Проверяем...");}
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
                if(button!=null){button.setEnabled(true);button.setText("Проверить обновления");}
                if(problem!=null){new AlertDialog.Builder(this).setTitle("Проверка обновлений").setMessage(problem+"\\n\\nТекущая версия: "+currentVersion()).setPositiveButton("Понятно",null).show();return;}
                if(apk==null||apk.isEmpty()){
                    new AlertDialog.Builder(this).setTitle("Новая версия: "+v).setMessage("Релиз опубликован, но APK-файл к нему не прикреплён. Откройте страницу релиза позже.").setPositiveButton("Открыть релиз",(d,w)->openUpdateUrl(page)).setNegativeButton("Закрыть",null).show();return;
                }
                if(normalizeVersion(v).equals(normalizeVersion(currentVersion()))){
                    if(button!=null)new AlertDialog.Builder(this).setTitle("Обновления не найдены").setMessage("Установлена последняя версия: "+currentVersion()).setPositiveButton("Понятно",null).show();
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
        Bitmap bg=null;int pageW=842,pageH=595;
        if(selected.uri!=null){bg=loadTemplate(selected.uri);if(bg!=null&&bg.getWidth()>0&&bg.getHeight()>0)pageH=Math.max(1,Math.round(pageW*(float)bg.getHeight()/bg.getWidth()));}
        PdfDocument doc=new PdfDocument();PdfDocument.Page page=doc.startPage(new PdfDocument.PageInfo.Builder(pageW,pageH,1).create());Canvas c=page.getCanvas();Paint p=new Paint(3);
        if(selected.uri!=null){
            if(bg!=null){c.drawBitmap(bg,null,new Rect(0,0,pageW,pageH),p);bg.recycle();}else drawDesign(c,p,selected);
            c.save();c.scale(pageW/842f,pageH/595f);drawCustomFields(c,p,selected,person,title,d,org,num);c.restore();
        }else drawCertificate(c,p,selected,person,title,d,org,num,false);
        doc.finishPage(page);ByteArrayOutputStream out=new ByteArrayOutputStream();doc.writeTo(out);doc.close();return out.toByteArray();
    }

    private String fieldValue(TemplateField f,String person,String title,String d,String org,int num){
        switch(f.key){case "person":return person;case "title":return title;case "date":return d;case "org":return org;case "number":return String.format(Locale.US,"%05d",num);default:return f.label;}
    }
    private void drawCustomFields(Canvas c,Paint p,Template t,String person,String title,String d,String org,int num){
        for(TemplateField f:t.fields){
            String value=fieldValue(f,person,title,d,org,num);
            p.setColor(Color.rgb(35,42,52));p.setTextAlign(Paint.Align.CENTER);
            p.setTypeface(Typeface.create("serif",f.key.equals("person")?Typeface.BOLD:Typeface.NORMAL));
            p.setTextSize(f.size);
            drawFit(c,value,842*f.x,595*f.y,700,p,f.size,Math.max(10,f.size*.55f));
        }
    }
    private void saveCustomTemplateList(){
        java.util.HashSet<String> uris=new java.util.HashSet<>();
        for(Template t:templates)if(t.uri!=null)uris.add(t.uri.toString());
        getPreferences(MODE_PRIVATE).edit().putStringSet("custom_template_uris",uris).apply();
    }
    private void restoreCustomTemplates(){
        java.util.Set<String> uris=getPreferences(MODE_PRIVATE).getStringSet("custom_template_uris",null);
        if(uris==null)return;
        for(String value:uris){
            try{
                Uri uri=Uri.parse(value);
                Template t=new Template("Мой шаблон "+(templates.size()-2),"Мои шаблоны",Color.rgb(246,245,241),Color.rgb(104,91,142),0);t.uri=uri;
                String raw=getPreferences(MODE_PRIVATE).getString("custom_fields_"+value,null);
                if(raw!=null){JSONArray a=new JSONArray(raw);for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);t.fields.add(new TemplateField(o.optString("key"),o.optString("label"),(float)o.optDouble("x",.5),(float)o.optDouble("y",.5),(float)o.optDouble("size",18)));}}
                templates.add(t);
            }catch(Exception ignored){}
        }
    }
    private void saveCustomTemplate(Template t){
        try{
            JSONArray a=new JSONArray();
            for(TemplateField f:t.fields){JSONObject o=new JSONObject();o.put("key",f.key);o.put("label",f.label);o.put("x",f.x);o.put("y",f.y);o.put("size",f.size);a.put(o);}
            getPreferences(MODE_PRIVATE).edit().putString("custom_fields_"+t.uri.toString(),a.toString()).apply();
        }catch(Exception ignored){}
    }
    private void showFieldEditor(Template t){
        LinearLayout panel=new LinearLayout(this);panel.setOrientation(1);panel.setPadding(dp(12),dp(8),dp(12),dp(8));
        TextView help=txt("Перетаскивайте поля пальцем. Их положение будет сохранено и применено ко всем PDF.",12,muted,false);panel.addView(help);
        FrameLayout canvas=new FrameLayout(this);canvas.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,dp(245));cp.topMargin=dp(10);panel.addView(canvas,cp);
        try{Bitmap bg=loadTemplate(t.uri);if(bg!=null){final int bw=bg.getWidth(),bh=bg.getHeight();ImageView image=new ImageView(this);image.setImageBitmap(bg);image.setScaleType(ImageView.ScaleType.FIT_XY);canvas.addView(image,new FrameLayout.LayoutParams(-1,-1));canvas.post(()->{int w=canvas.getWidth();ViewGroup.LayoutParams size=canvas.getLayoutParams();size.height=Math.max(dp(120),Math.round(w*(float)bh/Math.max(1,bw)));canvas.setLayoutParams(size);});}}catch(Exception ignored){}
        ArrayList<TextView> labels=new ArrayList<>();
        Runnable render=()->{
            for(TextView v:labels)canvas.removeView(v);labels.clear();
            for(TemplateField f:t.fields){
                TextView v=txt(f.label,Math.max(10,Math.round(f.size*.42f)),Color.rgb(28,39,61),true);
                v.setBackground(drawable(Color.argb(65,90,120,210),5,accent,1));v.setPadding(dp(5),dp(3),dp(5),dp(3));v.setGravity(Gravity.CENTER);
                v.measure(View.MeasureSpec.makeMeasureSpec(canvas.getWidth(),View.MeasureSpec.AT_MOST),View.MeasureSpec.makeMeasureSpec(canvas.getHeight(),View.MeasureSpec.AT_MOST));
                int vw=v.getMeasuredWidth(),vh=v.getMeasuredHeight();
                FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(-2,-2);lp.leftMargin=Math.max(0,Math.min(canvas.getWidth()-vw,Math.round(canvas.getWidth()*f.x-vw/2f)));lp.topMargin=Math.max(0,Math.min(canvas.getHeight()-vh,Math.round(canvas.getHeight()*f.y-vh/2f)));
                canvas.addView(v,lp);labels.add(v);
                v.setOnTouchListener((view,event)->{
                    if(event.getAction()==android.view.MotionEvent.ACTION_DOWN){view.setTag(new float[]{event.getRawX(),event.getRawY(),view.getX(),view.getY()});return true;}
                    if(event.getAction()==android.view.MotionEvent.ACTION_MOVE){float[] q=(float[])view.getTag();float nx=Math.max(0,Math.min(canvas.getWidth()-view.getWidth(),q[2]+event.getRawX()-q[0]));float ny=Math.max(0,Math.min(canvas.getHeight()-view.getHeight(),q[3]+event.getRawY()-q[1]));view.setX(nx);view.setY(ny);f.x=(nx+view.getWidth()/2f)/Math.max(1,canvas.getWidth());f.y=(ny+view.getHeight()/2f)/Math.max(1,canvas.getHeight());return true;}
                    return event.getAction()==android.view.MotionEvent.ACTION_UP;
                });
            }
        };
        canvas.post(render);
        Button add=button("＋ Добавить поле",Color.rgb(235,238,250));add.setTextColor(accent);panel.addView(add);
        add.setOnClickListener(v->{
            String[] names={"Имя и фамилия","Название курса","Дата","Организатор / подпись","Номер сертификата"};
            String[] keys={"person","title","date","org","number"};
            new AlertDialog.Builder(this).setTitle("Какое поле добавить?").setItems(names,(d,w)->{
                t.fields.add(new TemplateField(keys[w],names[w],.5f,.5f,w==0?34:18));canvas.post(render);
            }).show();
        });
        Button remove=button("Удалить последнее поле",Color.rgb(247,238,238));remove.setTextColor(Color.rgb(160,65,65));panel.addView(remove);
        remove.setOnClickListener(v->{if(!t.fields.isEmpty()){t.fields.remove(t.fields.size()-1);canvas.post(render);}});
        new AlertDialog.Builder(this).setTitle("Настройка полей шаблона").setView(panel)
            .setPositiveButton("Сохранить",(d,w)->{saveCustomTemplate(t);toast("Позиции полей сохранены");})
            .setNegativeButton("Отмена",null).setNeutralButton("Изменить поля",(d,w)->showFieldEditor(t)).show();
    }

    private Bitmap loadTemplate(Uri uri)throws Exception{
        String type=getContentResolver().getType(uri);
        if(type!=null&&type.equals("application/pdf")){
            try(ParcelFileDescriptor fd=getContentResolver().openFileDescriptor(uri,"r");PdfRenderer renderer=new PdfRenderer(fd)){if(renderer.getPageCount()==0)return null;PdfRenderer.Page pg=renderer.openPage(0);Bitmap b=Bitmap.createBitmap(1684,1190,Bitmap.Config.ARGB_8888);b.eraseColor(Color.WHITE);pg.render(b,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);pg.close();return b;}
        }
        try(InputStream in=getContentResolver().openInputStream(uri)){return BitmapFactory.decodeStream(in);}
    }
    private void drawDesign(Canvas c,Paint p,Template t){
        final int gold=t.accent;
        c.drawColor(t.color);
        p.setStyle(Paint.Style.FILL);
        p.setColor(t.color);
        // Тонкая фактура бумаги: мягкая двойная рамка и внутренний кант.
        p.setStyle(Paint.Style.STROKE);p.setColor(gold);p.setStrokeWidth(5f);
        c.drawRect(22,22,820,573,p);
        p.setStrokeWidth(1.7f);c.drawRect(34,34,808,561,p);
        p.setStrokeWidth(.8f);c.drawRect(43,43,799,552,p);
        drawBeadedBorder(c,p,gold);
        int style=t.style;
        if(style==33){ // Арт-деко: ступенчатая геометрия и веерные лучи
            p.setStrokeWidth(3.2f);c.drawRect(70,70,772,525,p);c.drawRect(82,82,760,513,p);
            for(int i=0;i<9;i++){float x=421-i*24; c.drawLine(x,84,421,178,p);c.drawLine(421,178,842-x,84,p);}
            for(int i=0;i<7;i++){float y=470+i*13;c.drawLine(160,y,682,y,p);}
            drawMedallion(c,p,421,112,48,gold);
            drawDecoCorners(c,p,gold);
        }else if(style==28||style==22){ // Герб и королевская печать
            drawLaurel(c,p,421,295,270,195,gold,18);
            drawMedallion(c,p,421,105,54,gold);
            p.setStrokeWidth(5);c.drawOval(300,445,542,548,p);p.setStrokeWidth(1.6f);c.drawOval(311,456,531,537,p);
            drawCornerFlourish(c,p,gold,68,68,1f);drawCornerFlourish(c,p,gold,774,68,-1f);
            drawCornerFlourish(c,p,gold,68,527,1f);drawCornerFlourish(c,p,gold,774,527,-1f);
        }else if(style==29||style==31){ // Ботанический / лавровый
            drawLaurel(c,p,421,300,305,215,gold,22);
            drawCornerFlourish(c,p,gold,55,55,1.05f);drawCornerFlourish(c,p,gold,787,55,-1.05f);
            drawCornerFlourish(c,p,gold,55,540,1.05f);drawCornerFlourish(c,p,gold,787,540,-1.05f);
            drawMedallion(c,p,421,92,34,gold);
        }else if(style==30||style==17){ // Сапфир / атлас с драпировкой
            p.setStyle(Paint.Style.FILL);
            Path drape=new Path();drape.moveTo(35,35);drape.cubicTo(175,165,260,40,421,130);drape.cubicTo(580,40,670,165,807,35);drape.lineTo(807,112);drape.cubicTo(650,225,560,100,421,190);drape.cubicTo(275,100,180,225,35,112);drape.close();
            p.setColor(Color.rgb(19,39,76));c.drawPath(drape,p);
            p.setStyle(Paint.Style.STROKE);p.setColor(gold);p.setStrokeWidth(5);c.drawPath(drape,p);
            drawMedallion(c,p,421,142,48,gold);
            drawLaurel(c,p,421,430,220,100,gold,14);
        }else if(style==27||style==34){ // Классическая архитектурная рамка
            p.setStrokeWidth(7);c.drawLine(82,70,82,525,p);c.drawLine(760,70,760,525,p);
            p.setStrokeWidth(2);c.drawLine(98,70,98,525,p);c.drawLine(744,70,744,525,p);
            for(int y=115;y<500;y+=42){p.setStrokeWidth(2);c.drawLine(67,y,98,y,p);c.drawLine(744,y,775,y,p);}
            drawMedallion(c,p,421,90,34,gold);
            drawCornerFlourish(c,p,gold,65,65,.8f);drawCornerFlourish(c,p,gold,777,65,-.8f);
            drawCornerFlourish(c,p,gold,65,530,.8f);drawCornerFlourish(c,p,gold,777,530,-.8f);
        }else if(style==32||style==26){ // Розовое золото / романтика
            drawFlower(c,p,92,105,40,gold);drawFlower(c,p,750,105,40,gold);
            drawFlower(c,p,92,490,40,gold);drawFlower(c,p,750,490,40,gold);
            drawCornerFlourish(c,p,gold,52,52,.9f);drawCornerFlourish(c,p,gold,790,52,-.9f);
            drawCornerFlourish(c,p,gold,52,543,.9f);drawCornerFlourish(c,p,gold,790,543,-.9f);
            drawMedallion(c,p,421,90,28,gold);
        }else{ // Барокко: насыщенные зеркальные вензеля
            drawCornerFlourish(c,p,gold,58,58,1.1f);drawCornerFlourish(c,p,gold,784,58,-1.1f);
            drawCornerFlourish(c,p,gold,58,537,1.1f);drawCornerFlourish(c,p,gold,784,537,-1.1f);
            Path center=new Path();center.moveTo(120,297);center.cubicTo(220,235,265,340,340,297);center.cubicTo(370,275,390,275,421,297);center.cubicTo(452,275,472,275,502,297);center.cubicTo(577,340,622,235,722,297);
            p.setStrokeWidth(2.2f);c.drawPath(center,p);
            drawMedallion(c,p,421,82,31,gold);drawMedallion(c,p,421,513,24,gold);
            if(style==21||style==25||style==23){drawLaurel(c,p,421,300,260,170,gold,16);}
        }
        // Малые разделители и ювелирные точки на длинных сторонах.
        p.setStyle(Paint.Style.STROKE);p.setColor(gold);p.setStrokeWidth(1.1f);
        for(int x=145;x<=697;x+=46){c.drawCircle(x,47,3.4f,p);c.drawCircle(x,548,3.4f,p);}
        p.setStyle(Paint.Style.FILL);p.setColor(gold);p.setTextAlign(Paint.Align.CENTER);
        p.setTypeface(Typeface.create("serif",Typeface.BOLD));p.setTextSize(20);
        if(style!=33)c.drawText("✦",421,64,p);
        p.setStyle(Paint.Style.FILL);
    }
    private void drawBeadedBorder(Canvas c,Paint p,int color){
        p.setStyle(Paint.Style.STROKE);p.setColor(color);p.setStrokeWidth(1f);
        for(int x=60;x<=782;x+=18){c.drawCircle(x,27,2.1f,p);c.drawCircle(x,568,2.1f,p);}
        for(int y=65;y<=530;y+=18){c.drawCircle(27,y,2.1f,p);c.drawCircle(815,y,2.1f,p);}
    }
    private void drawCornerFlourish(Canvas c,Paint p,int color,float x,float y,float mirror){
        p.setStyle(Paint.Style.STROKE);p.setColor(color);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeWidth(2.6f);
        c.save();c.translate(x,y);c.scale(mirror,1);
        Path a=new Path();a.moveTo(0,100);a.cubicTo(-12,54,14,20,52,18);a.cubicTo(88,16,93,52,69,60);a.cubicTo(48,67,37,44,52,36);
        a.cubicTo(90,18,112,48,95,82);a.cubicTo(80,111,45,113,35,92);a.cubicTo(27,72,45,63,56,76);
        a.moveTo(0,100);a.cubicTo(30,135,70,135,95,105);a.cubicTo(120,76,146,78,165,98);
        a.moveTo(18,18);a.cubicTo(42,-4,72,-3,91,15);a.cubicTo(106,30,128,28,143,12);
        c.drawPath(a,p);c.drawCircle(0,100,7,p);c.drawCircle(95,82,5,p);c.restore();
    }
    private void drawMedallion(Canvas c,Paint p,float x,float y,float r,int color){
        p.setStyle(Paint.Style.STROKE);p.setColor(color);p.setStrokeWidth(3);c.drawCircle(x,y,r,p);
        p.setStrokeWidth(1.2f);c.drawCircle(x,y,r-7,p);c.drawCircle(x,y,r-12,p);
        for(int i=0;i<12;i++){double a=i*Math.PI/6;float px=(float)(x+Math.cos(a)*(r+5)),py=(float)(y+Math.sin(a)*(r+5));c.drawCircle(px,py,3,p);}
        p.setStyle(Paint.Style.FILL);p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.create("serif",Typeface.BOLD));p.setTextSize(r*.85f);p.setColor(color);c.drawText("✦",x,y+r*.28f,p);
    }
    private void drawLaurel(Canvas c,Paint p,float cx,float cy,float rx,float ry,int color,int count){
        p.setStyle(Paint.Style.STROKE);p.setColor(color);p.setStrokeWidth(2.2f);
        for(int side=0;side<2;side++){
            c.save();if(side==1)c.scale(-1,1,cx,cy);
            Path stem=new Path();stem.moveTo(cx,cy+ry*.72f);stem.cubicTo(cx-rx*.35f,cy+ry*.25f,cx-rx*.65f,cy-ry*.35f,cx-rx,cy-ry*.72f);c.drawPath(stem,p);
            for(int i=0;i<count;i++){float q=i/(float)Math.max(1,count-1);float lx=cx-rx*q,ly=cy+ry*.65f-ry*1.35f*q;Path leaf=new Path();leaf.moveTo(lx,ly);leaf.cubicTo(lx-rx*.10f,ly-ry*.18f,lx-rx*.22f,ly-ry*.17f,lx-rx*.24f,ly);leaf.cubicTo(lx-rx*.16f,ly+ry*.10f,lx-rx*.05f,ly+ry*.08f,lx,ly);c.drawPath(leaf,p);}
            c.restore();
        }
    }
    private void drawFlower(Canvas c,Paint p,float x,float y,float r,int color){
        p.setStyle(Paint.Style.STROKE);p.setColor(color);p.setStrokeWidth(1.8f);
        for(int i=0;i<8;i++){c.save();c.rotate(i*45,x,y);c.drawOval(x-r*.22f,y-r,x+r*.22f,y-r*.18f,p);c.restore();}
        p.setStyle(Paint.Style.FILL);c.drawCircle(x,y,r*.16f,p);
    }
    private void drawDecoCorners(Canvas c,Paint p,int color){
        p.setStyle(Paint.Style.STROKE);p.setColor(color);p.setStrokeWidth(3);
        for(int x:new int[]{90,752})for(int y:new int[]{90,505}){c.drawRect(x-25,y-25,x+25,y+25,p);c.drawLine(x-25,y,x,y-25,p);c.drawLine(x,y-25,x+25,y,p);c.drawLine(x+25,y,x,y+25,p);c.drawLine(x,y+25,x-25,y,p);}
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