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
    private EditText event,date,issuer,gratitude;
    private TextView csvStatus,selectedLabel,saveLocation;
    private Uri saveTree;
    private Uri csv;
    private BroadcastReceiver updateReceiver;
    private long updateDownloadId=-1;
    private Template selected;
    private LinearLayout root, previewRow;
    private int ink=Color.rgb(31,43,62), muted=Color.rgb(111,122,139), accent=Color.rgb(72,91,176);

    private String language;
    private String tr(String source){
        if(source==null)return null;
        String[] ru={
            "Создавайте документы,\\nкоторые запоминаются","Персональные сертификаты и дипломы — сразу для всего списка участников.",
            "ШАГ 1 · ДИЗАЙН","Выберите оформление","Открыть все шаблоны","Свой дизайн",
            "Добавьте фоновый макет JPG, PNG или PDF","＋ Добавить",
            "В галерее — готовые оформления для дипломов, грамот, конкурсов и beauty-обучения.",
            "ШАГ 2 · ДАННЫЕ","Загрузить список участников · CSV","Файл не выбран",
            "Название мероприятия или курса","Дата выдачи","Организация / подпись","НАЗВАНИЕ","ДАТА","ОРГАНИЗАТОР",
            "ШАГ 3 · РЕЗУЛЬТАТ","ПАПКА СОХРАНЕНИЯ","Выбрать папку на телефоне",
            "Для каждого участника будет создан отдельный PDF. Все файлы также соберём в один ZIP-архив.",
            "Создать сертификаты  →","Ваши макеты и документы остаются на устройстве.",
            "Разработано SoloDefStudio","Проверить обновления","Выберите дизайн сертификата","Закрыть",
            "Выбранный дизайн: ","✓ Выбран","Нажмите для выбора","Королевский рубин","Королевский сапфир",
            "Императорский герб","Розовый сад","Изумрудный венок","Мраморные колонны","Сапфировая награда",
            "Чёрный арт-деко","Пудровые цветы","Старинный свиток","Изумрудная корона","Жемчужная классика",
            "Фиолетовая флора","Золотая классика","Бордовый бархат","Синий медальон",
            "Сертификаты","Дипломы","Грамоты","Мои шаблоны","Имя и фамилия","Название курса","Организатор / подпись",
            "Настройка полей шаблона","Перетаскивайте поля пальцем. Их положение будет сохранено и применено ко всем PDF.",
            "＋ Добавить поле","Удалить последнее поле","Какое поле добавить?","Номер сертификата","Сохранить","Отмена",
            "Позиции полей сохранены","По умолчанию: папка приложения Certificates во внутреннем хранилище Android",
            "Выбрана папка телефона. PDF и ZIP будут сохраняться туда.","Папка сохранения выбрана",
            "настоящим подтверждается, что","успешно принял(а) участие в мероприятии","Дата: ","Организатор","Подпись","Текст благодарности","ТЕКСТ БЛАГОДАРНОСТИ","Настроить расположение полей","СЕРТИФИКАТ","ДИПЛОМ","ПОЧЁТНАЯ ГРАМОТА",
            "Настройка расположения полей","Изменить поля","Дата"
        };
        String[] de={
            "Erstellen Sie Dokumente,\\ndie in Erinnerung bleiben","Personalisierte Zertifikate und Diplome – für Ihre gesamte Teilnehmerliste.",
            "SCHRITT 1 · DESIGN","Design auswählen","Alle Vorlagen anzeigen","Eigenes Design",
            "Hintergrund als JPG, PNG oder PDF hinzufügen","＋ Hinzufügen",
            "Fertige Designs für Diplome, Urkunden, Wettbewerbe und Schulungen.",
            "SCHRITT 2 · DATEN","Teilnehmerliste laden · CSV","Keine Datei ausgewählt",
            "Name der Veranstaltung oder des Kurses","Ausstellungsdatum","Organisation / Unterschrift","TITEL","DATUM","ORGANISATOR",
            "SCHRITT 3 · ERGEBNIS","SPEICHERORDNER","Ordner auf dem Smartphone wählen",
            "Für jeden Teilnehmer wird eine eigene PDF erstellt. Alle Dateien werden auch als ZIP gespeichert.",
            "Zertifikate erstellen  →","Ihre Vorlagen und Dokumente bleiben auf Ihrem Gerät.",
            "Entwickelt von SoloDefStudio","Nach Updates suchen","Zertifikatsdesign auswählen","Schließen",
            "Ausgewähltes Design: ","✓ Ausgewählt","Zum Auswählen tippen","Königlicher Rubin","Königlicher Saphir",
            "Kaiserliches Wappen","Rosengarten","Smaragdkranz","Marmorsäulen","Saphir-Auszeichnung",
            "Schwarzes Art déco","Puderrosa Blumen","Antike Schriftrolle","Smaragdkrone","Perlenklassik",
            "Violette Blüten","Goldene Klassik","Bordeaux-Samt","Blaues Medaillon",
            "Zertifikate","Diplome","Urkunden","Eigene Vorlagen","Vor- und Nachname","Kursname","Organisation / Unterschrift",
            "Vorlagenfelder bearbeiten","Ziehen Sie die Felder mit dem Finger. Die Position wird für alle PDFs gespeichert.",
            "＋ Feld hinzufügen","Letztes Feld löschen","Welches Feld hinzufügen?","Zertifikatsnummer","Speichern","Abbrechen",
            "Feldpositionen gespeichert","Standard: App-Ordner Certificates im internen Android-Speicher",
            "Smartphone-Ordner ausgewählt. PDF und ZIP werden dort gespeichert.","Speicherordner ausgewählt",
            "Hiermit wird bestätigt, dass","erfolgreich an der Veranstaltung teilgenommen hat","Datum: ","Organisation","Unterschrift","Dankestext","DANKES TEXT","Feldpositionen anpassen","ZERTIFIKAT","DIPLOM","EHRENURKUNDE",
            "Feldeinstellungen","Felder bearbeiten","Datum"
        };
        String[] en={
            "Create documents\\nthat make an impression","Personalized certificates and diplomas for your entire participant list.",
            "STEP 1 · DESIGN","Choose a design","Browse all templates","Custom design",
            "Add a JPG, PNG or PDF background","＋ Add",
            "Ready-made designs for diplomas, certificates, contests and training.",
            "STEP 2 · DETAILS","Load participant list · CSV","No file selected",
            "Event or course name","Issue date","Organization / signature","TITLE","DATE","ORGANIZER",
            "STEP 3 · OUTPUT","SAVE FOLDER","Choose a folder on your phone",
            "A separate PDF will be created for each participant. All files will also be packed into a ZIP archive.",
            "Create certificates  →","Your templates and documents stay on your device.",
            "Developed by SoloDefStudio","Check for updates","Choose certificate design","Close",
            "Selected design: ","✓ Selected","Tap to select","Royal Ruby","Royal Sapphire",
            "Imperial Crest","Rose Garden","Emerald Laurel","Marble Columns","Sapphire Award",
            "Black Art Deco","Powder Flowers","Vintage Scroll","Emerald Crown","Ivory Pearl",
            "Violet Flourish","Classic Gold","Burgundy Velvet","Navy Medallion",
            "Certificates","Diplomas","Awards","My templates","Full name","Course title","Organization / signature",
            "Edit template fields","Drag fields with your finger. Their positions will be saved and used in every PDF.",
            "＋ Add field","Remove last field","Which field would you like to add?","Certificate number","Save","Cancel",
            "Field positions saved","Default: Certificates app folder in Android internal storage",
            "Phone folder selected. PDFs and ZIP files will be saved there.","Save folder selected",
            "This is to certify that","has successfully participated in the event","Date: ","Organizer","Signature","Thank-you message","THANK-YOU MESSAGE","Adjust field positions","CERTIFICATE","DIPLOMA","AWARD CERTIFICATE",
            "Field position settings","Edit fields","Date"
        };
        String[] target=language.equals("de")?de:language.equals("en")?en:ru;
        for(int i=0;i<ru.length;i++)if(ru[i].equals(source))return target[i];
        return source;
    }
    private void chooseLanguage(){
        String[] labels={"Русский","Deutsch","English"};
        String[] codes={"ru","de","en"};
        int checked=language.equals("de")?1:language.equals("en")?2:0;
        new AlertDialog.Builder(this).setTitle("Language / Язык").setSingleChoiceItems(labels,checked,(d,w)->{
            String oldEvent=event==null?"":event.getText().toString();
            String oldDate=date==null?"":date.getText().toString();
            String oldIssuer=issuer==null?"":issuer.getText().toString();
            String oldGratitude=gratitude==null?"":gratitude.getText().toString();
            language=codes[w];
            getPreferences(MODE_PRIVATE).edit().putString("app_language",language).apply();
            d.dismiss();build();
            event.setText(oldEvent);date.setText(oldDate);issuer.setText(oldIssuer);gratitude.setText(oldGratitude);
        }).setNegativeButton("Отмена",null).show();
    }


    static class TemplateField {
        String key,label; float x,y,size;
        TemplateField(String k,String l,float px,float py,float sz){key=k;label=l;x=px;y=py;size=sz;}
    }
    static class Template {
        String name,category; int color,accent,style; Uri uri; String assetName;
        ArrayList<TemplateField> fields=new ArrayList<>();
        Template(String n,String c,int col,int a,int st){name=n;category=c;color=col;accent=a;style=st;}
    }

    private void addArtworkTemplate(String name,String category,String asset){
        Template t=new Template(name,category,Color.rgb(35,29,22),Color.rgb(213,174,91),0);t.assetName=asset;
        t.fields.add(new TemplateField("person","Имя и фамилия",.50f,.47f,34));
        t.fields.add(new TemplateField("gratitude","Текст благодарности",.50f,.60f,18));
        t.fields.add(new TemplateField("title","Название курса",.50f,.70f,22));
        t.fields.add(new TemplateField("date","Дата",.30f,.83f,16));
        t.fields.add(new TemplateField("org","Организатор / подпись",.74f,.90f,14));
        restoreTemplateFields(t);templates.add(t);
    }

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        language=getPreferences(MODE_PRIVATE).getString("app_language", Locale.getDefault().getLanguage().equals("de")?"de":Locale.getDefault().getLanguage().equals("en")?"en":"ru");
        getWindow().setStatusBarColor(Color.rgb(246,247,251));
        getWindow().setNavigationBarColor(Color.rgb(246,247,251));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        // Премиальная коллекция: каждый макет — отдельный JPEG-фон из assets/templates.
        addArtworkTemplate("Королевский рубин","Сертификаты","01_royal_ruby.jpg");
        addArtworkTemplate("Королевский сапфир","Дипломы","02_royal_sapphire.jpg");
        addArtworkTemplate("Императорский герб","Грамоты","03_imperial_eagle.jpg");
        addArtworkTemplate("Розовый сад","Сертификаты","04_rose_garden.jpg");
        addArtworkTemplate("Изумрудный венок","Дипломы","05_emerald_laurel.jpg");
        addArtworkTemplate("Мраморные колонны","Дипломы","06_marble_columns.jpg");
        addArtworkTemplate("Сапфировая награда","Грамоты","07_blue_laurel.jpg");
        addArtworkTemplate("Чёрный арт-деко","Сертификаты","08_black_artdeco.jpg");
        addArtworkTemplate("Пудровые цветы","Сертификаты","09_pink_floral.jpg");
        addArtworkTemplate("Старинный свиток","Грамоты","10_vintage_scroll.jpg");
        addArtworkTemplate("Изумрудная корона","Дипломы","11_green_crown.jpg");
        addArtworkTemplate("Жемчужная классика","Сертификаты","12_ivory_pearl.jpg");
        addArtworkTemplate("Фиолетовая флора","Сертификаты","13_violet_flourish.jpg");
        addArtworkTemplate("Золотая классика","Дипломы","14_classic_gold.jpg");
        addArtworkTemplate("Бордовый бархат","Грамоты","15_burgundy_velvet.jpg");
        addArtworkTemplate("Синий медальон","Сертификаты","16_navy_medallion.jpg");
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
        Button languageBtn=button("🌐 "+(language.equals("de")?"Deutsch":language.equals("en")?"English":"Русский")+"  ▾",Color.rgb(235,238,250)); languageBtn.setTextColor(accent); root.addView(languageBtn,params(-1,dp(42),12)); languageBtn.setOnClickListener(v->chooseLanguage());
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
        Button editLayout=button("Настроить расположение полей",Color.rgb(235,238,250));editLayout.setTextColor(accent);hero.addView(editLayout);editLayout.setOnClickListener(v->{ensureEditableFields(selected);showFieldEditor(selected);});

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
        event=field("Название мероприятия или курса"); date=field("Дата выдачи"); issuer=field("Организация / подпись"); gratitude=field("Текст благодарности");
        addInput(data,"НАЗВАНИЕ",event); addInput(data,"ДАТА",date); addInput(data,"ОРГАНИЗАТОР",issuer); addInput(data,"ТЕКСТ БЛАГОДАРНОСТИ",gratitude);

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
            Bitmap original=null;float pageH=595f;
            try{
                if(template.uri!=null)original=loadTemplate(template.uri);
                else if(template.assetName!=null)try(InputStream in=getAssets().open("templates/"+template.assetName)){original=BitmapFactory.decodeStream(in);}
                if(original!=null&&original.getWidth()>0)pageH=842f*original.getHeight()/original.getWidth();
            }catch(Exception ignored){}
            float scale=Math.min(getWidth()/842f,getHeight()/pageH);
            float dx=(getWidth()-842f*scale)/2f,dy=(getHeight()-pageH*scale)/2f;
            canvas.save();canvas.translate(dx,dy);canvas.scale(scale,scale);
            if(original!=null){canvas.drawBitmap(original,null,new RectF(0,0,842,pageH),paint);original.recycle();}
            drawCustomFields(canvas,paint,template,"ИМЯ УЧАСТНИКА","Название мероприятия","12.12.2026","Организатор","Благодарим за участие!",1,842,pageH);
            canvas.restore();
        }
    }
    // Текст — независимый слой поверх готового художественного JPEG-фона.
    private void drawSampleText(Canvas c,Paint p,Template t,float pageH){drawCustomFields(c,p,t,"ИМЯ УЧАСТНИКА","Название мероприятия","12.12.2026","Организатор","Благодарим за участие!",1,842,pageH);}
    private void drawArtworkText(Canvas c,Paint p,Template t,String person,String title,String d,String org,int num,float pageH){
        float k=pageH/595f;
        p.setTextAlign(Paint.Align.CENTER);p.setColor(Color.rgb(112,79,34));
        p.setTypeface(Typeface.create("serif",Typeface.BOLD));p.setTextSize(17*k);
        c.drawText(t.category.equals("Дипломы")?"ДИПЛОМ":t.category.equals("Грамоты")?"ПОЧЁТНАЯ ГРАМОТА":"СЕРТИФИКАТ",421*k,132*k,p);
        p.setColor(Color.rgb(48,43,37));p.setTypeface(Typeface.create("serif",Typeface.NORMAL));p.setTextSize(19*k);
        c.drawText(tr("настоящим подтверждается, что"),421*k,205*k,p);
        p.setTypeface(Typeface.create("serif",Typeface.BOLD));p.setTextSize(30*k);
        drawFit(c,person,421*k,285*k,690*k,p,30*k,18*k);
        p.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));p.setTextSize(16*k);
        c.drawText(tr("успешно принял(а) участие в мероприятии"),421*k,350*k,p);
        p.setTypeface(Typeface.create("serif",Typeface.BOLD));p.setTextSize(22*k);p.setColor(Color.rgb(112,79,34));
        drawFit(c,title,421*k,405*k,690*k,p,22*k,15*k);
        p.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));p.setTextSize(13*k);p.setColor(Color.rgb(75,69,60));
        c.drawText(tr("Дата: ")+d,250*k,480*k,p);c.drawText("№ "+String.format(Locale.US,"%05d",num),590*k,480*k,p);
        p.setStrokeWidth(1*k);c.drawLine(90*k,515*k,300*k,515*k,p);c.drawLine(540*k,515*k,750*k,515*k,p);p.setTextSize(11*k);
        c.drawText(org.isEmpty()?tr("Организатор"):org,195*k,535*k,p);c.drawText(tr("Подпись"),645*k,535*k,p);
    }
    private void showTemplates(){
        ScrollView scroll=new ScrollView(this); LinearLayout list=new LinearLayout(this); list.setOrientation(1); list.setPadding(dp(10),dp(8),dp(10),dp(8)); scroll.addView(list);
        for(int i=0;i<templates.size();i+=2){
            LinearLayout row=new LinearLayout(this); row.setOrientation(0); list.addView(row,params(-1,-2,8));
            for(int j=i;j<Math.min(i+2,templates.size());j++){
                final Template t=templates.get(j);
                LinearLayout item=new LinearLayout(this); item.setOrientation(1); item.setPadding(dp(7),dp(7),dp(7),dp(7));
                item.setBackground(drawable(Color.rgb(25,22,18),13,t==selected?Color.rgb(218,174,83):Color.rgb(83,70,48),t==selected?3:1));
                LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(0,-2,1); ip.setMargins(dp(4),0,dp(4),0); row.addView(item,ip);
                FrameLayout preview=new FrameLayout(this); preview.setBackground(drawable(t.color,8,Color.TRANSPARENT,0)); item.addView(preview,new LinearLayout.LayoutParams(-1,dp(105)));
                drawPreviewArtwork(preview,t);
                TextView name=txt(tr(t.name),13,Color.rgb(246,229,190),true); name.setPadding(0,dp(8),0,dp(2)); item.addView(name);
                TextView category=txt(tr(t.category),10,Color.rgb(190,174,143),false); item.addView(category);
                TextView state=txt(tr(t==selected?"✓ Выбран":"Нажмите для выбора"),10,t==selected?Color.rgb(232,190,93):Color.rgb(176,163,140),true); state.setPadding(0,dp(5),0,0); item.addView(state);
                item.setOnClickListener(v->{selected=t;updateSelected();});
            }
        }
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(tr("Выберите дизайн сертификата")).setView(scroll).setNegativeButton(tr("Закрыть"),null).create();
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
    private void ensureEditableFields(Template t){
        if(t.fields.isEmpty()){
            t.fields.add(new TemplateField("person","Имя и фамилия",.50f,.45f,34));
            t.fields.add(new TemplateField("gratitude","Текст благодарности",.50f,.59f,18));
            t.fields.add(new TemplateField("title","Название курса",.50f,.70f,22));
            t.fields.add(new TemplateField("date","Дата",.30f,.83f,16));
            t.fields.add(new TemplateField("org","Организатор / подпись",.74f,.90f,14));
        }
    }
    private void updateSelected(){
        if(selectedLabel!=null)selectedLabel.setText(tr("Выбранный дизайн: ")+tr(selected.name)+"  ·  "+tr(selected.category));
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
            t.fields.add(new TemplateField("person","Имя и фамилия",.50f,.43f,34));
            t.fields.add(new TemplateField("gratitude","Текст благодарности",.50f,.58f,18));
            t.fields.add(new TemplateField("title","Название курса",.50f,.69f,22));
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
        String title=event.getText().toString().trim(),d=date.getText().toString().trim(),org=issuer.getText().toString().trim(),thanks=gratitude.getText().toString().trim();
        if(title.isEmpty()||d.isEmpty()){toast("Заполните название и дату");return;}
        try{
            if(saveTree!=null){
                saveCertificatesToTree(title,d,org,thanks);
                new AlertDialog.Builder(this).setTitle("Готово!").setMessage("Создано документов: "+people.size()+"\\n\\nPDF и ZIP сохранены в выбранную папку, в новой папке с датой и временем.").setPositiveButton("Понятно",null).show();
            }else{
                File dir=new File(getExternalFilesDir(null),"Certificates");if(!dir.exists())dir.mkdirs();File zip=new File(dir,"certificates_"+System.currentTimeMillis()+".zip");
                try(ZipOutputStream zos=new ZipOutputStream(new FileOutputStream(zip))){
                    for(int n=0;n<people.size();n++){String fn=String.format(Locale.US,"%03d_%s.pdf",n+1,safe(people.get(n)));byte[] bytes=pdf(people.get(n),title,d,org,thanks,n+1);
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
            Intent install=new Intent(Intent.ACTION_INSTALL_PACKAGE);
            install.setDataAndType(apk,"application/vnd.android.package-archive");
            install.putExtra(Intent.EXTRA_RETURN_RESULT,true);
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
    private Uri saveCertificatesToTree(String title,String d,String org,String thanks)throws Exception{
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
                byte[] bytes=pdf(people.get(n),title,d,org,thanks,n+1);
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

    private byte[] pdf(String person,String title,String d,String org,String thanks,int num)throws Exception{
        Bitmap bg=null;int pageW=842,pageH=595;
        if(selected.uri!=null)bg=loadTemplate(selected.uri);
        else if(selected.assetName!=null)try(InputStream in=getAssets().open("templates/"+selected.assetName)){bg=BitmapFactory.decodeStream(in);}
        if(bg!=null&&bg.getWidth()>0&&bg.getHeight()>0)pageH=Math.max(1,Math.round(pageW*(float)bg.getHeight()/bg.getWidth()));
        PdfDocument doc=new PdfDocument();PdfDocument.Page page=doc.startPage(new PdfDocument.PageInfo.Builder(pageW,pageH,1).create());Canvas c=page.getCanvas();Paint p=new Paint(3);
        if(bg!=null){c.drawBitmap(bg,null,new Rect(0,0,pageW,pageH),p);bg.recycle();}
        drawCustomFields(c,p,selected,person,title,d,org,thanks,num,pageW,pageH);
        doc.finishPage(page);ByteArrayOutputStream out=new ByteArrayOutputStream();doc.writeTo(out);doc.close();return out.toByteArray();
    }

    private String fieldValue(TemplateField f,String person,String title,String d,String org,String thanks,int num){
        switch(f.key){case "person":return person;case "title":return title;case "date":return d;case "org":return org;case "gratitude":return thanks;case "number":return String.format(Locale.US,"%05d",num);default:return f.label;}
    }
    private void drawCustomFields(Canvas c,Paint p,Template t,String person,String title,String d,String org,String thanks,int num,float pageW,float pageH){
        float fontScale=pageH/595f;
        p.setTextAlign(Paint.Align.CENTER);p.setColor(Color.rgb(112,79,34));p.setTypeface(Typeface.create("serif",Typeface.BOLD));p.setTextSize(17*fontScale);
        c.drawText(t.category.equals("Дипломы")?tr("ДИПЛОМ"):t.category.equals("Грамоты")?tr("ПОЧЁТНАЯ ГРАМОТА"):tr("СЕРТИФИКАТ"),pageW*.5f,pageH*.22f,p);
        for(TemplateField f:t.fields){
            String value=fieldValue(f,person,title,d,org,thanks,num);
            p.setColor(Color.rgb(35,42,52));p.setTextAlign(Paint.Align.CENTER);
            p.setTypeface(Typeface.create("serif",f.key.equals("person")?Typeface.BOLD:Typeface.NORMAL));
            float size=f.size*fontScale;p.setTextSize(size);
            drawFit(c,value,pageW*f.x,pageH*f.y,pageW*.84f,p,size,Math.max(8,size*.55f));
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
    private String templatePrefsKey(Template t){return t.uri!=null?"custom_fields_"+t.uri.toString():"builtin_fields_"+t.assetName;}
    private void restoreTemplateFields(Template t){
        String raw=getPreferences(MODE_PRIVATE).getString(templatePrefsKey(t),null);if(raw==null)return;
        try{JSONArray a=new JSONArray(raw);t.fields.clear();for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);t.fields.add(new TemplateField(o.optString("key"),o.optString("label"),(float)o.optDouble("x",.5),(float)o.optDouble("y",.5),(float)o.optDouble("size",18)));}}catch(Exception ignored){}
    }
    private void saveCustomTemplate(Template t){
        try{
            JSONArray a=new JSONArray();
            for(TemplateField f:t.fields){JSONObject o=new JSONObject();o.put("key",f.key);o.put("label",f.label);o.put("x",f.x);o.put("y",f.y);o.put("size",f.size);a.put(o);}
            getPreferences(MODE_PRIVATE).edit().putString(templatePrefsKey(t),a.toString()).apply();
        }catch(Exception ignored){}
    }
    private void showFieldEditor(Template t){
        ensureEditableFields(t);
        LinearLayout panel=new LinearLayout(this);panel.setOrientation(1);panel.setPadding(dp(12),dp(8),dp(12),dp(8));
        TextView help=txt("Перетаскивайте поля пальцем. Их положение будет сохранено и применено ко всем PDF.",12,muted,false);panel.addView(help);
        FrameLayout canvas=new FrameLayout(this);canvas.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,dp(245));cp.topMargin=dp(10);panel.addView(canvas,cp);
        try{Bitmap bg=t.uri!=null?loadTemplate(t.uri):BitmapFactory.decodeStream(getAssets().open("templates/"+t.assetName));if(bg!=null){final int bw=bg.getWidth(),bh=bg.getHeight();ImageView image=new ImageView(this);image.setImageBitmap(bg);image.setScaleType(ImageView.ScaleType.FIT_XY);canvas.addView(image,new FrameLayout.LayoutParams(-1,-1));canvas.post(()->{int w=canvas.getWidth();ViewGroup.LayoutParams size=canvas.getLayoutParams();size.height=Math.max(dp(120),Math.round(w*(float)bh/Math.max(1,bw)));canvas.setLayoutParams(size);});}}catch(Exception ignored){}
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
            String[] names={"Имя и фамилия","Название курса","Дата","Организатор / подпись","Текст благодарности","Номер сертификата"};
            String[] keys={"person","title","date","org","gratitude","number"};
            new AlertDialog.Builder(this).setTitle(tr("Какое поле добавить?")).setItems(new String[]{tr(names[0]),tr(names[1]),tr(names[2]),tr(names[3]),tr(names[4]),tr(names[5])},(d,w)->{
                t.fields.add(new TemplateField(keys[w],names[w],.5f,.5f,w==0?34:18));canvas.post(render);
            }).show();
        });
        Button remove=button("Удалить последнее поле",Color.rgb(247,238,238));remove.setTextColor(Color.rgb(160,65,65));panel.addView(remove);
        remove.setOnClickListener(v->{if(!t.fields.isEmpty()){t.fields.remove(t.fields.size()-1);canvas.post(render);}});
        new AlertDialog.Builder(this).setTitle(tr("Настройка расположения полей")).setView(panel)
            .setPositiveButton(tr("Сохранить"),(d,w)->{saveCustomTemplate(t);toast(tr("Позиции полей сохранены"));})
            .setNegativeButton(tr("Отмена"),null).setNeutralButton(tr("Изменить поля"),(d,w)->showFieldEditor(t)).show();
    }

    private Bitmap loadTemplate(Uri uri)throws Exception{
        String type=getContentResolver().getType(uri);
        if(type!=null&&type.equals("application/pdf")){
            try(ParcelFileDescriptor fd=getContentResolver().openFileDescriptor(uri,"r");PdfRenderer renderer=new PdfRenderer(fd)){if(renderer.getPageCount()==0)return null;PdfRenderer.Page pg=renderer.openPage(0);int w=1684,h=Math.max(1,Math.round(w*(float)pg.getHeight()/Math.max(1,pg.getWidth())));Bitmap b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);b.eraseColor(Color.WHITE);pg.render(b,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);pg.close();return b;}
        }
        try(InputStream in=getContentResolver().openInputStream(uri)){return BitmapFactory.decodeStream(in);}
    }
    private void drawFit(Canvas c,String s,float x,float y,float width,Paint p,float size,float min){p.setTextSize(size);while(p.measureText(s)>width&&size>min){size--;p.setTextSize(size);}c.drawText(s,x,y,p);}
    private String safe(String s){return s.replaceAll("[\\\\/:*?\"<>|]","_").trim().replaceAll("\\s+","_");}
    private LinearLayout card(){LinearLayout l=new LinearLayout(this);l.setBackground(drawable(Color.WHITE,18,Color.rgb(232,235,241),1));return l;}
    private TextView txt(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(tr(s));t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(null,1);return t;}
    private TextView section(String s){TextView t=txt(s,11,accent,true);t.setLetterSpacing(.12f);return t;}
    private EditText field(String hint){EditText e=new EditText(this);e.setSingleLine(true);e.setTextSize(14);e.setHint(tr(hint));e.setPadding(dp(12),0,dp(12),0);e.setBackground(drawable(Color.rgb(247,248,251),10,Color.rgb(232,235,241),1));return e;}
    private void addInput(LinearLayout box,String label,EditText e){TextView l=txt(label,10,muted,true);l.setPadding(0,dp(8),0,dp(5));box.addView(l);box.addView(e,params(-1,dp(48),0));}
    private Button button(String s,int color){Button b=new Button(this);b.setText(tr(s));b.setTextSize(13);b.setAllCaps(false);b.setTypeface(null,1);b.setBackground(drawable(color,12,Color.TRANSPARENT,0));return b;}
    private GradientDrawable drawable(int color,int radius,int stroke,int sw){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));if(sw>0)d.setStroke(dp(sw),stroke);return d;}
    private LinearLayout.LayoutParams params(int w,int h,int top){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h);if(top>0)p.topMargin=dp(top);return p;}
    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
}