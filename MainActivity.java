package com.example.caykho;

import android.app.*;import android.os.*;import android.graphics.Color;import android.content.*;import android.view.*;import android.widget.*;import java.text.*;import java.util.*;import android.database.*;

public class MainActivity extends Activity{
 Db db; LinearLayout root; TextView title; String date;
 int pad=12; int blue=Color.rgb(21,101,192);
 public void onCreate(Bundle b){super.onCreate(b);db=new Db(this);date=today();home();}
 String today(){return new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date());}
 TextView tv(String s,int sp){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setPadding(pad,pad,pad,pad);return t;}
 Button btn(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);return b;}
 void base(String t){root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(10,10,10,10);root.setBackgroundColor(Color.rgb(245,247,250));title=tv(t,22);title.setTextColor(blue);root.addView(title);setContentView(root);}
 void home(){base("QUẢN LÝ KHO CÂY");
   TextView info=tv("Ngày đang xem: "+date+"\nDữ liệu lưu ngay trên điện thoại.",16);root.addView(info);
   Button a=btn("📦 Nhập cây đã đóng gói");a.setOnClickListener(v->packing());root.addView(a);
   Button b=btn("📊 Tồn kho theo ngày");b.setOnClickListener(v->inventory());root.addView(b);
   Button c=btn("🌱 Danh mục cây & đơn giá");c.setOnClickListener(v->catalog());root.addView(c);
   Button d=btn("💰 Tổng tiền công nhân viên");d.setOnClickListener(v->payroll());root.addView(d);
   Button e=btn("👥 Quản lý nhân viên");e.setOnClickListener(v->employeeManager());root.addView(e);
   Button f=btn("📅 Chọn ngày");f.setOnClickListener(v->pickDate(()->home()));root.addView(f);
   root.addView(tv("\nLuồng dữ liệu: Danh mục cây → Đóng gói → Tồn kho → Tiền công.\nKhi sửa tên/kích thước/đơn giá trong Danh mục, toàn bộ tính toán sẽ dùng giá trị mới.\n\nNhân viên được quản lý theo ID cố định: đổi tên sẽ cập nhật tên ở tất cả các màn hình mà không làm mất dữ liệu đóng gói cũ.",14));
 }
 void employeeManager(){
   base("QUẢN LÝ NHÂN VIÊN");
   root.addView(tv("Thêm mới hoặc đổi tên nhân viên. Dữ liệu đóng gói cũ vẫn giữ nguyên vì mỗi nhân viên có ID riêng.",14));
   ScrollView sc=new ScrollView(this);
   LinearLayout list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL);
   Cursor c=db.employeeRows();
   while(c.moveToNext()){
     int id=c.getInt(0); String oldName=c.getString(1);
     LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL);
     TextView idv=tv("#"+id,14); row.addView(idv,new LinearLayout.LayoutParams(55,Wrap()));
     EditText name=new EditText(this); name.setSingleLine(true); name.setText(oldName); row.addView(name,new LinearLayout.LayoutParams(0,Wrap(),1));
     Button save=btn("Lưu");
     save.setOnClickListener(v->{String newName=name.getText().toString().trim(); if(newName.length()==0){Toast.makeText(this,"Tên nhân viên không được trống",Toast.LENGTH_SHORT).show();return;} if(newName.equals(oldName)){Toast.makeText(this,"Không có thay đổi",Toast.LENGTH_SHORT).show();return;} if(db.renameEmployee(id,newName)){Toast.makeText(this,"Đã đổi tên: "+oldName+" → "+newName,Toast.LENGTH_SHORT).show();}else Toast.makeText(this,"Tên đã tồn tại hoặc không thể đổi",Toast.LENGTH_SHORT).show();});
     row.addView(save); list.addView(row);
   }
   c.close(); sc.addView(list); root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
   LinearLayout add=new LinearLayout(this); add.setOrientation(LinearLayout.HORIZONTAL);
   EditText newName=new EditText(this); newName.setSingleLine(true); newName.setHint("Tên nhân viên mới"); add.addView(newName,new LinearLayout.LayoutParams(0,Wrap(),1));
   Button addBtn=btn("＋ Thêm"); addBtn.setOnClickListener(v->{String n=newName.getText().toString().trim(); if(n.length()==0){Toast.makeText(this,"Vui lòng nhập tên",Toast.LENGTH_SHORT).show();return;} if(db.addEmployee(n)){Toast.makeText(this,"Đã thêm "+n,Toast.LENGTH_SHORT).show();employeeManager();}else Toast.makeText(this,"Tên đã tồn tại hoặc không thể thêm",Toast.LENGTH_SHORT).show();}); add.addView(addBtn); root.addView(add);
   Button back=btn("← Trang chính");back.setOnClickListener(v->home());root.addView(back);
 }
 interface Done{void run();}
 void pickDate(Done done){Calendar c=Calendar.getInstance(); try{c.setTime(new SimpleDateFormat("yyyy-MM-dd",Locale.US).parse(date));}catch(Exception e){} DatePickerDialog p=new DatePickerDialog(this,(v,y,m,d)->{date=String.format(Locale.US,"%04d-%02d-%02d",y,m+1,d);done.run();},c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH));p.show();}
 void packing(){base("NHẬP ĐÓNG GÓI — "+date); LinearLayout top=new LinearLayout(this);top.setOrientation(LinearLayout.HORIZONTAL);Spinner emp=new Spinner(this);ArrayAdapter<String> ea=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,db.employees());emp.setAdapter(ea);top.addView(emp,new LinearLayout.LayoutParams(0,Wrap(),1));Button ch=btn("Đổi ngày");ch.setOnClickListener(v->pickDate(()->packing()));top.addView(ch);root.addView(top);
   ScrollView sc=new ScrollView(this);LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);Cursor c=db.plants();int eid=db.employeeId(ea.getItem(0));
   while(c.moveToNext()){int id=c.getInt(0);String name=c.getString(2),size=c.getString(3);LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);TextView n=tv(c.getInt(1)+". "+name+" — "+size,14);row.addView(n,new LinearLayout.LayoutParams(0,Wrap(),1));EditText q=new EditText(this);q.setInputType(2);q.setGravity(Gravity.CENTER);q.setText(""+db.qty(date,id,eid));row.addView(q,new LinearLayout.LayoutParams(90,Wrap()));row.setTag(id);q.setOnFocusChangeListener((v,f)->{if(!f){try{db.setQty(date,id,eid,Integer.parseInt(q.getText().toString()));}catch(Exception ex){q.setText("0");db.setQty(date,id,eid,0);}}});list.addView(row);}
   c.close();sc.addView(list);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));Button save=btn("LƯU TẤT CẢ");save.setOnClickListener(v->{for(int i=0;i<list.getChildCount();i++){View r=list.getChildAt(i);EditText q=(EditText)((LinearLayout)r).getChildAt(1);int id=(int)r.getTag();int x=0;try{x=Integer.parseInt(q.getText().toString());}catch(Exception ex){}db.setQty(date,id,db.employeeId(emp.getSelectedItem().toString()),x);}Toast.makeText(this,"Đã lưu ngày "+date,Toast.LENGTH_SHORT).show();});root.addView(save);Button back=btn("← Trang chính");back.setOnClickListener(v->home());root.addView(back);
   emp.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){int selected=db.employeeId(emp.getSelectedItem().toString());for(int i=0;i<list.getChildCount();i++){LinearLayout row=(LinearLayout)list.getChildAt(i);EditText q=(EditText)row.getChildAt(1);int pid=(int)row.getTag();q.setText(""+db.qty(date,pid,selected));}}});
 }
 int Wrap(){return LinearLayout.LayoutParams.WRAP_CONTENT;}
 void inventory(){base("TỒN KHO — "+date);Button ch=btn("📅 Đổi ngày");ch.setOnClickListener(v->pickDate(()->inventory()));root.addView(ch);root.addView(tv("Đầu ngày + Nhập − Đã đóng gói = Tồn cuối",14));
   ScrollView sc=new ScrollView(this);LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);Cursor c=db.plants();
   while(c.moveToNext()){int id=c.getInt(0);String name=c.getString(2),size=c.getString(3);int start=db.start(date,id),in=db.receipt(date,id),pack=db.packed(date,id),end=start+in-pack;TextView r=tv(c.getInt(1)+". "+name+" | "+size+"\nĐầu: "+start+"   Nhập: "+in+"   Đóng: "+pack+"   Tồn: "+end,14);if(end<0)r.setTextColor(Color.RED);list.addView(r);}
   c.close();sc.addView(list);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));Button back=btn("← Trang chính");back.setOnClickListener(v->home());root.addView(back);}
 void catalog(){base("DANH MỤC CÂY");root.addView(tv("Sửa tại đây; các phép tính khác dùng dữ liệu mới.",14));ScrollView sc=new ScrollView(this);LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);Cursor c=db.plants();
   while(c.moveToNext()){int id=c.getInt(0);LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.VERTICAL);EditText n=new EditText(this);n.setText(c.getString(2));EditText s=new EditText(this);s.setText(c.getString(3));EditText p=new EditText(this);p.setInputType(2|8192);p.setText(c.isNull(4)?"":String.valueOf(c.getDouble(4)));Button save=btn("Lưu #"+c.getInt(1));save.setOnClickListener(v->{android.content.ContentValues cv=new android.content.ContentValues();cv.put("name",n.getText().toString());cv.put("size",s.getText().toString());try{cv.put("plant_price",Double.parseDouble(p.getText().toString()));}catch(Exception ex){cv.putNull("plant_price");}db.getWritableDatabase().update("plants",cv,"id=?",new String[]{""+id});Toast.makeText(this,"Đã lưu",Toast.LENGTH_SHORT).show();});row.addView(tv("Cây #"+c.getInt(1),14));row.addView(n);row.addView(s);row.addView(p);row.addView(save);list.addView(row);}
   c.close();sc.addView(list);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));Button back=btn("← Trang chính");back.setOnClickListener(v->home());root.addView(back);}
 void payroll(){base("TIỀN CÔNG — "+date);Button ch=btn("📅 Đổi ngày");ch.setOnClickListener(v->pickDate(()->payroll()));root.addView(ch);for(String n:db.employees()){int id=db.employeeId(n);double pay=db.employeePay(date,id);TextView t=tv(n+": "+String.format(Locale.US,"%,.0f",pay)+" đ",17);root.addView(t);}Button back=btn("← Trang chính");back.setOnClickListener(v->home());root.addView(back);}
}
