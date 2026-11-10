package com.example.certificategenerator;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.graphics.pdf.PdfDocument;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

public class MainActivity extends Activity {
    private static final int PICK_CSV=41, PICK_TEMPLATE=42;
    private final ArrayList<String> people=new ArrayList<>();
    private final ArrayList<Template> templates=new ArrayList<>();
    private EditText event,date,issuer;
    private TextView csvStatus,selectedLabel;
    private Uri csv;
    private Template selected;
    private LinearLayout root;
    private int ink=Color.rgb(31,43,62), muted=Color.rgb(111,122,139), accent=Color.rgb(72,91,176);

    static class Template {
        String name,category; int color,accent; Uri uri;
        Template(String n,String c,int col,int a){name=n;category=c;color=col;accent=a;}
    }

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(246,247,251));
        getWindow().setNavigationBarColor(Color.rgb(246,247,251));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        templates.add(new Template("Золотая классика","Сертификаты",Color.rgb(250,246,235),Color.rgb(166,126,54)));
        templates.add(new Template("Академический","Дипломы",Color.rgb(241,245,252),Color.rgb(43,77,133)));
        templates.add(new Template("Современный","Сертификаты",Color.rgb(239,248,246),Color.rgb(40,125,111)));
        templates.add(new Template("Минимализм","Грамоты",Color.rgb(248,247,245),Color.rgb(72,78,91)));
        selected=templates.get(0);
        build();
    }

    private void build(){
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true);
        root=new LinearLayout(this); root.setOrientation(1); root.setPadding(dp(20),dp(22),dp(20),dp(32));
        root.setBackgroundColor(Color.rgb(246,247,251)); scroll.addView(root); setContentView(scroll);

        TextView brand=txt("CERTIFICATE STUDIO",11,accent,true); brand.setLetterSpacing(.16f); root.addView(brand);
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
        TextView note=txt("Для каждого участника будет создан отдельный PDF. Все файлы также соберём в один ZIP-архив.",13,muted,false); root.addView(note);
        Button make=button("Создать сертификаты  →",Color.rgb(39,53,83)); make.setTextColor(Color.WHITE); root.addView(make,params(-1,dp(54),14)); make.setOnClickListener(v->generate());
        TextView footer=txt("Ваши макеты и документы остаются на устройстве.",11,muted,false); footer.setGravity(Gravity.CENTER); footer.setPadding(0,dp(18),0,0); root.addView(footer);
        updateSelected();
    }

    private void addPreview(LinearLayout row,Template t){
        LinearLayout wrap=new LinearLayout(this); wrap.setOrientation(1); wrap.setGravity(Gravity.CENTER); 
        LinearLayout.LayoutParams wp=new LinearLayout.LayoutParams(0,-2,1); wp.setMargins(dp(3),0,dp(3),0); row.addView(wrap,wp);
        FrameLayout frame=new FrameLayout(this); frame.setBackground(drawable(t.color,12,Color.rgb(226,229,236),1)); frame.setPadding(dp(5),dp(5),dp(5),dp(5));
        LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(-1,dp(67)); wrap.addView(frame,fp);
        View border=new View(this); GradientDrawable gd=drawable(t.color,3,t.accent,2); border.setBackground(gd);
        FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(-1,-1); bp.setMargins(dp(4),dp(4),dp(4),dp(4)); frame.addView(border,bp);
        TextView stamp=txt("✦",17,t.accent,true); stamp.setGravity(Gravity.CENTER); frame.addView(stamp,new FrameLayout.LayoutParams(-1,-1));
        TextView name=txt(t.name,10,ink,true); name.setGravity(Gravity.CENTER); name.setPadding(0,dp(5),0,0); wrap.addView(name);
        wrap.setOnClickListener(v->{selected=t;updateSelected();});
    }
    private void showTemplates(){
        String[] names=new String[templates.size()]; for(int i=0;i<templates.size();i++) names[i]=templates.get(i).name+"  ·  "+templates.get(i).category;
        new AlertDialog.Builder(this).setTitle("Галерея шаблонов").setItems(names,(d,w)->{selected=templates.get(w);updateSelected();}).setNegativeButton("Закрыть",null).show();
    }
    private void updateSelected(){if(selectedLabel!=null) selectedLabel.setText("Выбран: "+selected.name+"  ·  "+selected.category);}
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
        try{getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}
        if(req==PICK_CSV){csv=u;try{people.clear();people.addAll(readNames(u));csvStatus.setText("✓ Загружено участников: "+people.size());}catch(Exception e){csvStatus.setText("Не удалось прочитать CSV: "+e.getMessage());}}
        if(req==PICK_TEMPLATE){
            String mime=getContentResolver().getType(u); String n="Мой шаблон "+(templates.size()-3);
            Template t=new Template(n,"Мои шаблоны",Color.rgb(246,245,241),Color.rgb(104,91,142)); t.uri=u; templates.add(t); selected=t; updateSelected();
            new AlertDialog.Builder(this).setTitle("Шаблон добавлен").setMessage("Макет добавлен в галерею. Он будет использоваться как фон сертификата.").setPositiveButton("Отлично",null).show();
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
            File dir=new File(getExternalFilesDir(null),"Certificates");if(!dir.exists())dir.mkdirs();File zip=new File(dir,"certificates_"+System.currentTimeMillis()+".zip");
            try(ZipOutputStream zos=new ZipOutputStream(new FileOutputStream(zip))){
                for(int n=0;n<people.size();n++){String fn=String.format(Locale.US,"%03d_%s.pdf",n+1,safe(people.get(n)));byte[] bytes=pdf(people.get(n),title,d,org,n+1);
                    try(FileOutputStream f=new FileOutputStream(new File(dir,fn))){f.write(bytes);}zos.putNextEntry(new ZipEntry(fn));zos.write(bytes);zos.closeEntry();}
            }
            new AlertDialog.Builder(this).setTitle("Готово!").setMessage("Создано документов: "+people.size()+"\n\nПапка: "+dir.getAbsolutePath()+"\nZIP: "+zip.getName()).setPositiveButton("Понятно",null).show();
        }catch(Exception e){toast("Ошибка: "+e.getMessage());}
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