package com.example.caykho;

import android.content.*;import android.database.*;import android.database.sqlite.*;import org.json.*;import java.io.*;import java.util.*;

public class Db extends SQLiteOpenHelper {
    static final String DB="caykho.db"; static final int VER=1;
    public Db(Context c){super(c,DB,null,VER);}
    public void onCreate(SQLiteDatabase d){
        d.execSQL("CREATE TABLE plants(id INTEGER PRIMARY KEY, stt INTEGER, name TEXT, size TEXT, plant_price REAL, active INTEGER DEFAULT 1)");
        d.execSQL("CREATE TABLE employees(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT UNIQUE)");
        d.execSQL("CREATE TABLE rates(size TEXT PRIMARY KEY, rate REAL)");
        d.execSQL("CREATE TABLE daily(date TEXT, plant_id INTEGER, employee_id INTEGER, qty INTEGER, PRIMARY KEY(date,plant_id,employee_id))");
        d.execSQL("CREATE TABLE receipts(date TEXT, plant_id INTEGER, qty INTEGER, PRIMARY KEY(date,plant_id))");
        d.execSQL("CREATE TABLE opening(date TEXT, plant_id INTEGER, qty INTEGER, PRIMARY KEY(date,plant_id))");
        seed(d);
    }
    void seed(SQLiteDatabase d){
        try{InputStream in=getClass().getClassLoader().getResourceAsStream("catalog_seed.json"); if(in==null) return; Scanner s=new Scanner(in,"UTF-8").useDelimiter("\\A"); String txt=s.hasNext()?s.next():""; JSONObject root=new JSONObject(txt);
            JSONArray em=root.getJSONArray("employees"); for(int i=0;i<em.length();i++){ContentValues v=new ContentValues();v.put("name",em.getString(i));d.insert("employees",null,v);}
            JSONObject rr=root.getJSONObject("rates"); Iterator<String> keys=rr.keys(); while(keys.hasNext()){String k=keys.next();ContentValues v=new ContentValues();v.put("size",k);v.put("rate",rr.getDouble(k));d.insert("rates",null,v);}
            JSONArray a=root.getJSONArray("items"); for(int i=0;i<a.length();i++){JSONObject x=a.getJSONObject(i);ContentValues v=new ContentValues();v.put("id",i+1);v.put("stt",x.getInt("stt"));v.put("name",x.getString("name"));v.put("size",x.optString("size","")); if(x.isNull("plantPrice"))v.putNull("plant_price");else v.put("plant_price",x.getDouble("plantPrice"));d.insert("plants",null,v);}
        }catch(Exception e){e.printStackTrace();}
    }
    public void onUpgrade(SQLiteDatabase d,int a,int b){}
    public List<String> employees(){List<String> x=new ArrayList<>();Cursor c=getReadableDatabase().rawQuery("SELECT name FROM employees ORDER BY id",null);while(c.moveToNext())x.add(c.getString(0));c.close();return x;}
    public Cursor employeeRows(){return getReadableDatabase().rawQuery("SELECT id,name FROM employees ORDER BY id",null);}
    public int employeeId(String n){Cursor c=getReadableDatabase().rawQuery("SELECT id FROM employees WHERE name=?",new String[]{n});int id=c.moveToFirst()?c.getInt(0):-1;c.close();return id;}
    public boolean addEmployee(String name){
        name=name==null?"":name.trim(); if(name.length()==0)return false;
        try{ContentValues v=new ContentValues();v.put("name",name);return getWritableDatabase().insertOrThrow("employees",null,v)!=-1;}catch(Exception e){return false;}
    }
    public boolean renameEmployee(int id,String name){
        name=name==null?"":name.trim(); if(name.length()==0)return false;
        try{ContentValues v=new ContentValues();v.put("name",name);return getWritableDatabase().update("employees",v,"id=?",new String[]{String.valueOf(id)})>0;}catch(Exception e){return false;}
    }
    public Cursor plants(){return getReadableDatabase().rawQuery("SELECT id,stt,name,size,plant_price FROM plants WHERE active=1 ORDER BY stt",null);}
    public double rate(String size){Cursor c=getReadableDatabase().rawQuery("SELECT rate FROM rates WHERE size=?",new String[]{size});double r=c.moveToFirst()?c.getDouble(0):0;c.close();return r;}
    public int qty(String date,int plant,int emp){Cursor c=getReadableDatabase().rawQuery("SELECT qty FROM daily WHERE date=? AND plant_id=? AND employee_id=?",new String[]{date,""+plant,""+emp});int q=c.moveToFirst()?c.getInt(0):0;c.close();return q;}
    public void setQty(String date,int plant,int emp,int q){SQLiteDatabase d=getWritableDatabase();ContentValues v=new ContentValues();v.put("date",date);v.put("plant_id",plant);v.put("employee_id",emp);v.put("qty",Math.max(0,q));d.insertWithOnConflict("daily",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
    public int packed(String date,int plant){Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE(SUM(qty),0) FROM daily WHERE date=? AND plant_id=?",new String[]{date,""+plant});int q=c.moveToFirst()?c.getInt(0):0;c.close();return q;}
    public int receipt(String date,int plant){Cursor c=getReadableDatabase().rawQuery("SELECT qty FROM receipts WHERE date=? AND plant_id=?",new String[]{date,""+plant});int q=c.moveToFirst()?c.getInt(0):0;c.close();return q;}
    public void setReceipt(String date,int plant,int q){SQLiteDatabase d=getWritableDatabase();ContentValues v=new ContentValues();v.put("date",date);v.put("plant_id",plant);v.put("qty",Math.max(0,q));d.insertWithOnConflict("receipts",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
    public int opening(String date,int plant){Cursor c=getReadableDatabase().rawQuery("SELECT qty FROM opening WHERE date=? AND plant_id=?",new String[]{date,""+plant});int q=c.moveToFirst()?c.getInt(0):0;c.close();return q;}
    public void setOpening(String date,int plant,int q){SQLiteDatabase d=getWritableDatabase();ContentValues v=new ContentValues();v.put("date",date);v.put("plant_id",plant);v.put("qty",Math.max(0,q));d.insertWithOnConflict("opening",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
    public String previousDate(String date){Cursor c=getReadableDatabase().rawQuery("SELECT MAX(date) FROM (SELECT date FROM daily UNION SELECT date FROM receipts UNION SELECT date FROM opening) WHERE date<?",new String[]{date});String s=c.moveToFirst()?c.getString(0):null;c.close();return s;}
    public int start(String date,int plant){int own=opening(date,plant); String prev=previousDate(date); if(prev!=null)return ending(prev,plant); return own;}
    public int ending(String date,int plant){return start(date,plant)+receipt(date,plant)-packed(date,plant);}
    public double employeePay(String date,int emp){Cursor c=getReadableDatabase().rawQuery("SELECT p.size,d.qty FROM daily d JOIN plants p ON p.id=d.plant_id WHERE d.date=? AND d.employee_id=?",new String[]{date,""+emp});double sum=0;while(c.moveToNext())sum+=c.getInt(1)*rate(c.getString(0));c.close();return sum;}
}
