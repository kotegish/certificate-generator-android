package com.example.certificategenerator;

import android.app.Activity;
import android.content.Intent;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class MainActivity extends Activity {
    private static final int PICK = 41;
    private final ArrayList<String> people = new ArrayList<>();
    private EditText event, date, issuer;
    private TextView status;
    private Uri csv;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(1); root.setPadding(28,32,28,24);
        root.setBackgroundColor(Color.rgb(246,248,251));
        TextView heading = new TextView(this);
        heading.setText("ГЕНЕРАТОР СЕРТИФИКАТОВ"); heading.setTextSize(23);
        heading.setTextColor(Color.rgb(16,36,58)); heading.setTypeface(null,1);
        root.addView(heading);
        TextView sub = new TextView(this);
        sub.setText("Массовое создание персональных PDF"); sub.setTextSize(14);
        sub.setTextColor(Color.DKGRAY); root.addView(sub);
        LinearLayout card = new LinearLayout(this); card.setOrientation(1);
        card.setPadding(20,20,20,20); card.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1,-2); cp.topMargin=24;
        root.addView(card,cp);
        Button choose = new Button(this); choose.setText("Выбрать CSV-файл");
        card.addView(choose); choose.setOnClickListener(v -> chooseCsv());
        status = new TextView(this); status.setText("CSV: файл не выбран");
        status.setTextSize(13); card.addView(status);
        event = field("Название мероприятия"); date = field("Дата (например, 10.11.2026)");
        issuer = field("Организация / подпись");
        addField(card,"МЕРОПРИЯТИЕ",event); addField(card,"ДАТА",date); addField(card,"ОРГАНИЗАТОР",issuer);
        Button make = new Button(this); make.setText("СОЗДАТЬ СЕРТИФИКАТЫ");
        card.addView(make); make.setOnClickListener(v -> generate());
        TextView hint = new TextView(this);
        hint.setText("Выберите CSV с колонкой «ФИО» или «name». PDF-файлы и ZIP сохраняются в папку Documents/Certificates.");
        hint.setTextSize(12); hint.setTextColor(Color.GRAY); hint.setPadding(0,20,0,0);
        root.addView(hint);
        ScrollView scroll = new ScrollView(this); scroll.addView(root); setContentView(scroll);
    }
    private EditText field(String hint) {
        EditText e=new EditText(this); e.setSingleLine(true); e.setHint(hint); return e;
    }
    private void addField(LinearLayout box,String label,EditText e) {
        TextView t=new TextView(this); t.setText(label); t.setTextSize(11); t.setTextColor(Color.GRAY);
        box.addView(t); box.addView(e);
    }
    private void chooseCsv() {
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("text/*"); startActivityForResult(i,PICK);
    }
    @Override protected void onActivityResult(int req,int result,Intent data) {
        super.onActivityResult(req,result,data);
        if(req==PICK && result==RESULT_OK && data!=null) {
            csv=data.getData();
            try {
                getContentResolver().takePersistableUriPermission(csv,Intent.FLAG_GRANT_READ_URI_PERMISSION);
                people.clear(); people.addAll(readNames(csv));
                status.setText("Загружено участников: "+people.size());
            } catch(Exception e) { status.setText("Ошибка CSV: "+e.getMessage()); }
        }
    }
    private List<String> readNames(Uri uri) throws Exception {
        ArrayList<String> out=new ArrayList<>();
        BufferedReader br=new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri),StandardCharsets.UTF_8));
        String line; boolean first=true; int col=0;
        while((line=br.readLine())!=null) {
            if(line.trim().isEmpty()) continue;
            List<String> cells=parse(line);
            if(first) {
                first=false;
                for(int n=0;n<cells.size();n++) {
                    String h=cells.get(n).trim().toLowerCase(Locale.ROOT).replace("\uFEFF","");
                    if(h.equals("фио")||h.equals("name")||h.equals("full_name")||h.equals("participant")) { col=n; break; }
                }
                if(cells.get(col).trim().equalsIgnoreCase("фио")||cells.get(col).trim().equalsIgnoreCase("name")||cells.get(col).trim().equalsIgnoreCase("full_name")||cells.get(col).trim().equalsIgnoreCase("participant")) continue;
            }
            if(col<cells.size()&&!cells.get(col).trim().isEmpty()) out.add(cells.get(col).trim());
        }
        br.close(); return out;
    }
    private List<String> parse(String s) {
        ArrayList<String> a=new ArrayList<>(); StringBuilder b=new StringBuilder(); boolean q=false;
        for(int i=0;i<s.length();i++){ char c=s.charAt(i);
            if(c=='"'){ if(q&&i+1<s.length()&&s.charAt(i+1)=='"'){b.append('"');i++;}else q=!q; }
            else if((c==','||c==';'||c=='\t')&&!q){a.add(b.toString());b.setLength(0);} else b.append(c);
        } a.add(b.toString()); return a;
    }
    private void generate() {
        if(people.isEmpty()){status.setText("Сначала выберите CSV-файл со списком участников.");return;}
        String title=event.getText().toString().trim(), d=date.getText().toString().trim(), org=issuer.getText().toString().trim();
        if(title.isEmpty()||d.isEmpty()){status.setText("Заполните название мероприятия и дату.");return;}
        try {
            File dir=new File(getExternalFilesDir(null),"Certificates"); dir.mkdirs();
            File zip=new File(dir,"certificates_"+System.currentTimeMillis()+".zip");
            try(ZipOutputStream zos=new ZipOutputStream(new FileOutputStream(zip))) {
                for(int n=0;n<people.size();n++){
                    String filename=String.format(Locale.US,"%03d_%s.pdf",n+1,safe(people.get(n)));
                    byte[] pdf=pdf(people.get(n),title,d,org,n+1);
                    try(FileOutputStream fos=new FileOutputStream(new File(dir,filename))){fos.write(pdf);}
                    zos.putNextEntry(new ZipEntry(filename)); zos.write(pdf); zos.closeEntry();
                }
            }
            status.setText("Готово: "+people.size()+" PDF\nZIP: "+zip.getAbsolutePath());
        } catch(Exception e){status.setText("Ошибка генерации: "+e.getMessage());}
    }
    private String safe(String s){return s.replaceAll("[\\\\/:*?\"<>|]","_").trim().replaceAll("\\s+","_");}
    private byte[] pdf(String person,String title,String d,String org,int num) throws Exception {
        PdfDocument doc=new PdfDocument(); PdfDocument.Page page=doc.startPage(new PdfDocument.PageInfo.Builder(842,595,1).create());
        Canvas c=page.getCanvas(); Paint p=new Paint(3);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(3); p.setColor(Color.rgb(16,36,58)); c.drawRect(24,24,818,571,p);
        p.setStrokeWidth(1); p.setColor(Color.rgb(31,78,121)); c.drawRect(34,34,808,561,p);
        p.setStyle(Paint.Style.FILL); p.setTextAlign(Paint.Align.CENTER);
        p.setTypeface(Typeface.create("serif",Typeface.BOLD)); p.setColor(Color.rgb(160,125,55)); p.setTextSize(17);
        c.drawText("СЕРТИФИКАТ",421,130,p);
        p.setColor(Color.DKGRAY); p.setTypeface(Typeface.create("serif",Typeface.NORMAL)); p.setTextSize(25);
        c.drawText("настоящим подтверждается, что",421,205,p);
        p.setTypeface(Typeface.create("serif",Typeface.BOLD)); p.setTextSize(32);
        drawFit(c,person,421,285,690,p,32,18);
        p.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL)); p.setTextSize(18); p.setColor(Color.DKGRAY);
        c.drawText("успешно принял(а) участие в мероприятии",421,350,p);
        p.setTypeface(Typeface.create("serif",Typeface.BOLD)); p.setTextSize(24); p.setColor(Color.rgb(31,78,121));
        drawFit(c,title,421,405,690,p,24,15);
        p.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL)); p.setTextSize(14); p.setColor(Color.DKGRAY);
        c.drawText("Дата: "+d,250,480,p); c.drawText("№ "+String.format(Locale.US,"%05d",num),590,480,p);
        p.setStrokeWidth(1); c.drawLine(90,515,300,515,p); c.drawLine(540,515,750,515,p);
        p.setTextSize(12); c.drawText(org.isEmpty()?"Организатор":org,195,535,p); c.drawText("Подпись",645,535,p);
        doc.finishPage(page); ByteArrayOutputStream out=new ByteArrayOutputStream(); doc.writeTo(out); doc.close(); return out.toByteArray();
    }
    private void drawFit(Canvas c,String s,float x,float y,float width,Paint p,float size,float min) {
        p.setTextSize(size); while(p.measureText(s)>width&&size>min){size--;p.setTextSize(size);} c.drawText(s,x,y,p);
    }
}