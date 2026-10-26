package com.ecofiberbd.app;

import android.app.*;
import android.os.Bundle;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    final int GREEN = Color.rgb(22,125,82), DARK = Color.rgb(16,92,61), BG = Color.rgb(245,248,246);
    DB db; LinearLayout root, content; boolean english = false;

    public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(DARK); getWindow().setNavigationBarColor(DARK);
        db = new DB(this); showHome();
    }

    TextView text(String s, int size, int color) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        t.setPadding(0,5,0,5); return t;
    }
    LinearLayout column() { LinearLayout l = new LinearLayout(this); l.setOrientation(1); return l; }
    LinearLayout row() { LinearLayout l = new LinearLayout(this); l.setOrientation(0); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    Button button(String label, Runnable action) {
        Button b = new Button(this); b.setText(label); b.setAllCaps(false);
        b.setOnClickListener(v -> action.run()); return b;
    }
    String money(double n) { return "৳ " + String.format(Locale.US, "%,.2f", n); }
    double balance(String name) {
        Cursor c = db.getReadableDatabase().rawQuery("SELECT balance FROM accounts WHERE name=?", new String[]{name});
        double n=0; if(c.moveToFirst()) n=c.getDouble(0); c.close(); return n;
    }
    List<String> accounts() {
        List<String> a=new ArrayList<>(); Cursor c=db.getReadableDatabase().rawQuery("SELECT name FROM accounts ORDER BY id",null);
        while(c.moveToNext()) a.add(c.getString(0)); c.close(); return a;
    }
    void shell(String title) {
        root=column(); root.setBackgroundColor(BG);
        LinearLayout header=column(); header.setPadding(18,16,18,14); header.setBackgroundColor(DARK);
        header.addView(text("ECO FIBER BD",23,Color.WHITE)); header.addView(text(title,14,Color.WHITE));
        root.addView(header);
        ScrollView sc=new ScrollView(this); content=column(); content.setPadding(16,12,16,24); sc.addView(content);
        root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout nav=row(); nav.setBackgroundColor(Color.WHITE);
        nav.addView(button("হোম",()->showHome()),new LinearLayout.LayoutParams(0,-2,1));
        nav.addView(button("লেনদেন",()->showTransactions()),new LinearLayout.LayoutParams(0,-2,1));
        nav.addView(button("নোট",()->showNotes()),new LinearLayout.LayoutParams(0,-2,1));
        nav.addView(button("ব্যাংক +",()->addAccount()),new LinearLayout.LayoutParams(0,-2,1));
        root.addView(nav); setContentView(root);
    }
    void card(String title, String value) {
        LinearLayout c=column(); c.setPadding(14,10,14,10); c.setBackgroundColor(Color.WHITE);
        TextView a=text(title,14,DARK), b=text(value,22,GREEN); c.addView(a); c.addView(b);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.setMargins(0,5,0,7); content.addView(c,p);
    }
    void showHome() {
        shell("ড্যাশবোর্ড");
        double banks=0; for(String a:accounts()) if(!isNonBank(a)) banks+=balance(a);
        double total=0; for(String a:accounts()) total+=balance(a);
        card("সর্বমোট হিসাব",money(total));
        card("সব ব্যাংকে মোট",money(banks));
        card("হ্যান্ড ক্যাশ",money(balance("হ্যান্ড ক্যাশ")));
        card("ভল্ট",money(balance("ভল্ট")));
        card("মাস্টার ওয়ালেট",money(balance("মাস্টার ওয়ালেট")));
        card("ছেঁড়া-ফাটা নোট",money(db.tornTotal()));
        content.addView(button("＋ টাকা যোগ / জমা",()->transactionDialog(false)));
        content.addView(button("－ টাকা উত্তোলন / খরচ",()->transactionDialog(true)));
        content.addView(button("⇄ এক হিসাব থেকে অন্য হিসাবে ট্রান্সফার",()->transferDialog()));
        content.addView(text("ব্যাংক ব্যালেন্স",18,DARK));
        for(String a:accounts()) if(!isNonBank(a)) card(a,money(balance(a)));
        content.addView(text("নোট: এই সংস্করণে হিসাব ফোনে অফলাইনে সংরক্ষিত হয়। অনলাইন ব্যাকআপ এখনো সেটআপ করা নেই।",12,Color.DKGRAY));
    }
    boolean isNonBank(String s) { return s.equals("হ্যান্ড ক্যাশ")||s.equals("ভল্ট")||s.equals("মাস্টার ওয়ালেট"); }

    void transactionDialog(boolean withdraw) {
        List<String> ac=accounts(); String[] names=ac.toArray(new String[0]);
        LinearLayout box=column(); box.setPadding(20,8,20,0);
        Spinner sp=new Spinner(this); sp.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,names)); box.addView(text("হিসাব নির্বাচন করুন",14,DARK)); box.addView(sp);
        EditText amount=new EditText(this); amount.setHint("টাকার পরিমাণ"); amount.setInputType(8194); box.addView(amount);
        EditText note=new EditText(this); note.setHint("বিবরণ / নোট (ঐচ্ছিক)"); box.addView(note);
        new AlertDialog.Builder(this).setTitle(withdraw?"টাকা উত্তোলন":"টাকা যোগ / জমা").setView(box)
            .setNegativeButton("বাতিল",null).setPositiveButton("সংরক্ষণ",(d,w)->{
                try {
                    double v=Double.parseDouble(amount.getText().toString().trim()); if(v<=0) throw new Exception();
                    String name=sp.getSelectedItem().toString(); if(withdraw && balance(name)<v) { msg("পর্যাপ্ত ব্যালেন্স নেই"); return; }
                    db.change(name,withdraw?-v:v,withdraw?"উত্তোলন":"জমা",note.getText().toString());
                    showHome(); msg("হিসাব সংরক্ষণ হয়েছে");
                } catch(Exception e) { msg("সঠিক টাকার পরিমাণ লিখুন"); }
            }).show();
    }
    void transferDialog() {
        List<String> ac=accounts(); if(ac.size()<2)return;
        LinearLayout box=column(); box.setPadding(18,5,18,0);
        Spinner from=new Spinner(this), to=new Spinner(this);
        ArrayAdapter<String> ad=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,ac);
        from.setAdapter(ad); to.setAdapter(ad);
        box.addView(text("যেখান থেকে",14,DARK)); box.addView(from);
        box.addView(text("যেখানে",14,DARK)); box.addView(to);
        EditText amount=new EditText(this); amount.setHint("টাকার পরিমাণ"); amount.setInputType(8194); box.addView(amount);
        EditText note=new EditText(this); note.setHint("বিবরণ (ঐচ্ছিক)"); box.addView(note);
        new AlertDialog.Builder(this).setTitle("ট্রান্সফার").setView(box).setNegativeButton("বাতিল",null).setPositiveButton("সংরক্ষণ",(d,w)->{
            try {
                String f=from.getSelectedItem().toString(), t=to.getSelectedItem().toString();
                double v=Double.parseDouble(amount.getText().toString().trim());
                if(f.equals(t)||v<=0) throw new Exception();
                if(balance(f)<v){msg("পর্যাপ্ত ব্যালেন্স নেই");return;}
                db.transfer(f,t,v,note.getText().toString()); showHome(); msg("ট্রান্সফার সংরক্ষিত হয়েছে");
            } catch(Exception e){msg("তথ্যগুলো সঠিকভাবে দিন");}
        }).show();
    }
    void addAccount() {
        EditText input=new EditText(this); input.setHint("নতুন ব্যাংক / হিসাবের নাম");
        new AlertDialog.Builder(this).setTitle("নতুন ব্যাংক যোগ").setView(input).setNegativeButton("বাতিল",null).setPositiveButton("যোগ করুন",(d,w)->{
            String n=input.getText().toString().trim(); if(n.isEmpty()){msg("নাম লিখুন");return;}
            if(db.hasAccount(n)){msg("এই নামটি আগে থেকেই আছে");return;}
            db.addAccount(n); showHome(); msg("নতুন হিসাব যোগ হয়েছে");
        }).show();
    }
    void showTransactions() {
        shell("লেনদেনের ইতিহাস");
        Cursor c=db.getReadableDatabase().rawQuery("SELECT date,account,kind,amount,note FROM tx ORDER BY id DESC LIMIT 200",null);
        if(c.getCount()==0) content.addView(text("এখনো কোনো লেনদেন নেই।",16,DARK));
        while(c.moveToNext()) {
            String s=c.getString(0)+"\n"+c.getString(1)+" • "+c.getString(2)+" • "+money(c.getDouble(3))+
                (c.getString(4)==null||c.getString(4).isEmpty()?"":"\n"+c.getString(4));
            card("লেনদেন",s);
        } c.close();
    }
    void showNotes() {
        shell("ছেঁড়া-ফাটা নোট");
        content.addView(text("নোটের মূল্যমান ও সংখ্যা লিখুন। মোট মূল্য স্বয়ংক্রিয়ভাবে হিসাব হবে।",14,DARK));
        int[] vals={1000,500,200,100,50,20,10,5,2,1};
        for(int v:vals) {
            LinearLayout r=row(); r.addView(text("৳ "+v,16,DARK),new LinearLayout.LayoutParams(0,-2,1));
            EditText count=new EditText(this); count.setHint("সংখ্যা"); count.setInputType(2);
            count.setText(String.valueOf(db.tornCount(v))); r.addView(count,new LinearLayout.LayoutParams(0,-2,1));
            r.addView(button("সংরক্ষণ",()->{
                try { int n=Integer.parseInt(count.getText().toString().trim()); if(n<0)throw new Exception();
                    db.setTorn(v,n); showNotes(); } catch(Exception e){msg("০ বা তার বেশি সংখ্যা দিন");}
            }));
            content.addView(r);
        }
        card("মোট ছেঁড়া-ফাটা নোট",money(db.tornTotal()));
        content.addView(text("সতর্কতা: এই নোটের হিসাব আলাদা ট্র্যাক করা হয়; এটি মূল ব্যালেন্সে স্বয়ংক্রিয়ভাবে যোগ করা হয় না।",12,Color.DKGRAY));
    }
    void msg(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}

    class DB extends SQLiteOpenHelper {
        DB(android.content.Context c){super(c,"ecofiberbd.db",null,1);}
        public void onCreate(SQLiteDatabase d) {
            d.execSQL("CREATE TABLE accounts(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT UNIQUE,balance REAL NOT NULL DEFAULT 0)");
            d.execSQL("CREATE TABLE tx(id INTEGER PRIMARY KEY AUTOINCREMENT,date TEXT,account TEXT,kind TEXT,amount REAL,note TEXT)");
            d.execSQL("CREATE TABLE torn(denom INTEGER PRIMARY KEY,count INTEGER NOT NULL)");
            String[] defaults={"সোনালী ব্যাংক","পূবালী ব্যাংক","সিটি ব্যাংক","অগ্রণী ব্যাংক","হ্যান্ড ক্যাশ","ভল্ট","মাস্টার ওয়ালেট"};
            for(String s:defaults) addAccountTo(d,s);
            int[] vals={1000,500,200,100,50,20,10,5,2,1}; for(int v:vals)d.execSQL("INSERT INTO torn VALUES("+v+",0)");
        }
        public void onUpgrade(SQLiteDatabase d,int a,int b){}
        void addAccountTo(SQLiteDatabase d,String n){android.content.ContentValues v=new android.content.ContentValues();v.put("name",n);v.put("balance",0);d.insert("accounts",null,v);}
        void addAccount(String n){addAccountTo(getWritableDatabase(),n);}
        boolean hasAccount(String n){Cursor c=getReadableDatabase().rawQuery("SELECT id FROM accounts WHERE lower(name)=lower(?)",new String[]{n});boolean b=c.moveToFirst();c.close();return b;}
        void change(String n,double delta,String kind,String note){
            SQLiteDatabase d=getWritableDatabase(); d.beginTransaction();
            try {
                android.content.ContentValues v=new android.content.ContentValues(); v.put("balance",balance(n)+delta);
                d.update("accounts",v,"name=?",new String[]{n});
                android.content.ContentValues t=new android.content.ContentValues();
                t.put("date",new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.getDefault()).format(new Date()));
                t.put("account",n);t.put("kind",kind);t.put("amount",delta);t.put("note",note);d.insert("tx",null,t);
                d.setTransactionSuccessful();
            } finally {d.endTransaction();}
        }
        void transfer(String f,String t,double v,String note){
            SQLiteDatabase d=getWritableDatabase();d.beginTransaction();
            try {
                android.content.ContentValues a=new android.content.ContentValues();a.put("balance",balance(f)-v);d.update("accounts",a,"name=?",new String[]{f});
                android.content.ContentValues b=new android.content.ContentValues();b.put("balance",balance(t)+v);d.update("accounts",b,"name=?",new String[]{t});
                String date=new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.getDefault()).format(new Date());
                insertTx(d,date,f,"ট্রান্সফার আউট",-v,note+" → "+t);insertTx(d,date,t,"ট্রান্সফার ইন",v,note+" ← "+f);
                d.setTransactionSuccessful();
            } finally {d.endTransaction();}
        }
        void insertTx(SQLiteDatabase d,String date,String account,String kind,double amount,String note){
            android.content.ContentValues v=new android.content.ContentValues();v.put("date",date);v.put("account",account);v.put("kind",kind);v.put("amount",amount);v.put("note",note);d.insert("tx",null,v);
        }
        int tornCount(int den){Cursor c=getReadableDatabase().rawQuery("SELECT count FROM torn WHERE denom=?",new String[]{String.valueOf(den)});int n=0;if(c.moveToFirst())n=c.getInt(0);c.close();return n;}
        void setTorn(int den,int count){android.content.ContentValues v=new android.content.ContentValues();v.put("denom",den);v.put("count",count);getWritableDatabase().insertWithOnConflict("torn",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
        double tornTotal(){Cursor c=getReadableDatabase().rawQuery("SELECT SUM(denom*count) FROM torn",null);double n=0;if(c.moveToFirst())n=c.getDouble(0);c.close();return n;}
    }
}
