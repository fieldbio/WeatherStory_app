package com.segre.weatherstories;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import android.text.InputType;
import org.json.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
 final ExecutorService worker=Executors.newSingleThreadExecutor();
 final Handler ui=new Handler(Looper.getMainLooper());
 final ArrayList<Story> stories=new ArrayList<>();
 TextView heading,status; ImageView image; Button previous,next; android.content.SharedPreferences prefs;
 int page=0,generation=0; String office="",zip=""; boolean busy=false;
 static final String UA="WeatherStoriesAndroid/0.1 (personal Weather Story viewer)";
 static class Story { String url,title; Bitmap bitmap; Story(String u,String t,Bitmap b){url=u;title=t;bitmap=b;} }
 @Override public void onCreate(Bundle b){super.onCreate(b);
  prefs=getSharedPreferences("settings",MODE_PRIVATE);
  getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
  LinearLayout layout=new LinearLayout(this);layout.setOrientation(1);layout.setBackgroundColor(Color.rgb(12,18,26));
  LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);
  heading=new TextView(this);heading.setTextColor(Color.WHITE);heading.setTextSize(16);heading.setPadding(12,0,8,0);
  bar.addView(heading,new LinearLayout.LayoutParams(0,48,1));
  Button refresh=new Button(this);refresh.setText("Refresh");refresh.setOnClickListener(v->load());bar.addView(refresh);
  Button settings=new Button(this);settings.setText("ZIP");settings.setOnClickListener(v->askZip());bar.addView(settings);
  layout.addView(bar);
  image=new ImageView(this);image.setScaleType(ImageView.ScaleType.FIT_CENTER);
  GestureDetector gestures=new GestureDetector(this,new GestureDetector.SimpleOnGestureListener(){
   @Override public boolean onDown(android.view.MotionEvent e){return true;}
   @Override public boolean onFling(android.view.MotionEvent a,android.view.MotionEvent b,float vx,float vy){if(a!=null&&Math.abs(b.getX()-a.getX())>60&&Math.abs(vx)>Math.abs(vy)){move(b.getX()<a.getX()?1:-1);return true;}return false;}
   @Override public boolean onDoubleTap(android.view.MotionEvent e){zoom();return true;}
  });
  image.setOnTouchListener((v,e)->gestures.onTouchEvent(e));
  layout.addView(image,new LinearLayout.LayoutParams(-1,0,1));
  LinearLayout bottom=new LinearLayout(this);bottom.setGravity(Gravity.CENTER_VERTICAL);
  previous=new Button(this);previous.setText("Previous");previous.setOnClickListener(v->move(-1));bottom.addView(previous);
  status=new TextView(this);status.setTextColor(Color.LTGRAY);status.setGravity(Gravity.CENTER);status.setTextSize(12);bottom.addView(status,new LinearLayout.LayoutParams(0,-2,1));
  next=new Button(this);next.setText("Next");next.setOnClickListener(v->move(1));bottom.addView(next);
  layout.addView(bottom);setContentView(layout);zip=prefs.getString("zip","");
  if(zip.isEmpty())askZip();else load();
 }
 void askZip(){EditText entry=new EditText(this);entry.setInputType(InputType.TYPE_CLASS_NUMBER);entry.setSingleLine();entry.setText(zip);entry.setHint("Five-digit US ZIP");
  AlertDialog d=new AlertDialog.Builder(this).setTitle("Weather Story ZIP code").setView(entry).setNegativeButton("Cancel",null).setPositiveButton("Save",null).create();
  d.setOnShowListener(v->d.getButton(-1).setOnClickListener(w->{String z=entry.getText().toString().trim();if(!z.matches("[0-9]{5}")){entry.setError("Enter exactly five digits");return;}zip=z;prefs.edit().putString("zip",zip).apply();d.dismiss();load();}));d.show();
 }
 byte[] download(String address)throws Exception{
  URL url=new URL(address);if(!url.getProtocol().equals("https"))throw new IOException("Only HTTPS sources are allowed");
  HttpURLConnection c=(HttpURLConnection)url.openConnection();c.setRequestProperty("User-Agent",UA);c.setConnectTimeout(15000);c.setReadTimeout(20000);
  try{int code=c.getResponseCode();if(code!=200)throw new IOException("HTTP "+code+" from "+url.getHost());try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buf=new byte[16384];int n;while((n=in.read(buf))!=-1){out.write(buf,0,n);if(out.size()>20000000)throw new IOException("Response too large");}return out.toByteArray();}}finally{c.disconnect();}
 }
 JSONObject json(String url)throws Exception{return new JSONObject(new String(download(url),java.nio.charset.StandardCharsets.UTF_8));}
 void load(){
  if(zip.isEmpty())return;
  final int request=++generation;final String requestedZip=zip;busy=true;heading.setText("ZIP "+zip+" — loading");status.setText("Finding office and current stories…");
  worker.execute(()->{try{
   String of=prefs.getString("office_"+requestedZip,"");
   if(of.isEmpty()){
    JSONObject place=json("https://api.zippopotam.us/us/"+requestedZip).getJSONArray("places").getJSONObject(0);
    JSONObject properties=json("https://api.weather.gov/points/"+place.getString("latitude")+","+place.getString("longitude")).getJSONObject("properties");
    of=properties.getString("gridId").toLowerCase(Locale.US);
    if(!of.matches("[a-z]{3}"))throw new IOException("Unexpected NWS office");
    prefs.edit().putString("office_"+requestedZip,of).apply();
   }
   String source="https://www.weather.gov/"+of+"/weatherstory";
   Document doc=Jsoup.parse(new String(download(source),java.nio.charset.StandardCharsets.UTF_8),source);
   LinkedHashMap<String,String> candidates=new LinkedHashMap<>();
   for(Element e:doc.select("img")){
    String u=e.absUrl("src");if(u.isEmpty())u=e.absUrl("data-src");
    String low=u.toLowerCase(Locale.US);
    boolean story=low.contains("/images/"+of+"/")||low.contains("/wxstory/")||low.contains("weatherstory")||low.contains("wxstory");
    boolean excluded=low.matches(".*(logo|icon|banner|facebook|twitter|youtube|header|footer|small).*");
    if(story&&!excluded&&u.startsWith("https://"))candidates.put(u,e.attr("alt"));
   }
   ArrayList<Story> found=new ArrayList<>();
   for(Map.Entry<String,String> c:candidates.entrySet()){
    try{byte[] bytes=download(c.getKey());Bitmap b=BitmapFactory.decodeByteArray(bytes,0,bytes.length);if(b!=null&&b.getWidth()>=400&&b.getHeight()>=200)found.add(new Story(c.getKey(),c.getValue(),b));}catch(Exception ignored){}
    if(found.size()>=20)break;
   }
   if(found.isEmpty())throw new IOException("No supported story images found. This office may use a different page format.");
   String resolved=of;String checked=java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT,java.text.DateFormat.SHORT).format(new Date());
   ui.post(()->{if(request!=generation)return;busy=false;office=resolved;stories.clear();stories.addAll(found);page=0;heading.setText("NWS "+office.toUpperCase(Locale.US)+" • ZIP "+requestedZip);show();status.append("\nChecked "+checked);cache(found,requestedZip,resolved,checked);});
  }catch(Exception e){ui.post(()->{if(request!=generation)return;busy=false;heading.setText("ZIP "+requestedZip);if(!restore(requestedZip)&&!stories.isEmpty()){stories.clear();image.setImageDrawable(null);}status.setText("Refresh failed: "+e.getMessage()+"\n"+(stories.isEmpty()?"Tap Refresh to retry or change ZIP.":"CACHED — may be outdated; not live."));});}});
 }
 void cache(ArrayList<Story> found,String z,String o,String checked){
  worker.execute(()->{try{File folder=new File(getFilesDir(),"stories");folder.mkdirs();JSONArray a=new JSONArray();int i=0;
   for(Story s:found){String name="panel"+(i++)+".png";try(FileOutputStream out=new FileOutputStream(new File(folder,name))){s.bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}a.put(new JSONObject().put("file",name).put("title",s.title));}
   JSONObject meta=new JSONObject().put("zip",z).put("office",o).put("checked",checked).put("stories",a);
   try(FileOutputStream out=new FileOutputStream(new File(folder,"cache.json"))){out.write(meta.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}
  }catch(Exception ignored){}});
 }
 boolean restore(String z){try{File folder=new File(getFilesDir(),"stories");JSONObject m=new JSONObject(new String(java.nio.file.Files.readAllBytes(new File(folder,"cache.json").toPath()),java.nio.charset.StandardCharsets.UTF_8));if(!m.getString("zip").equals(z))return false;
  ArrayList<Story> old=new ArrayList<>();JSONArray a=m.getJSONArray("stories");for(int i=0;i<a.length();i++){JSONObject s=a.getJSONObject(i);Bitmap b=BitmapFactory.decodeFile(new File(folder,s.getString("file")).getPath());if(b!=null)old.add(new Story("",s.getString("title"),b));}if(old.isEmpty())return false;stories.clear();stories.addAll(old);page=0;office=m.getString("office");heading.setText("NWS "+office.toUpperCase(Locale.US)+" • ZIP "+z+" • CACHED "+m.getString("checked"));show();return true;
 }catch(Exception e){return false;}}
 void move(int delta){if(stories.isEmpty())return;page=Math.max(0,Math.min(stories.size()-1,page+delta));show();}
 void show(){if(stories.isEmpty())return;Story s=stories.get(page);image.setImageBitmap(s.bitmap);previous.setEnabled(page>0);next.setEnabled(page<stories.size()-1);status.setText((page+1)+" / "+stories.size()+"  •  National Weather Service"+(s.title.isEmpty()?"":"\n"+s.title));}
 void zoom(){if(stories.isEmpty())return;android.webkit.WebView w=new android.webkit.WebView(this);w.getSettings().setBuiltInZoomControls(true);w.getSettings().setDisplayZoomControls(false);w.getSettings().setUseWideViewPort(true);w.getSettings().setLoadWithOverviewMode(true);ByteArrayOutputStream out=new ByteArrayOutputStream();stories.get(page).bitmap.compress(Bitmap.CompressFormat.PNG,100,out);String data=android.util.Base64.encodeToString(out.toByteArray(),android.util.Base64.NO_WRAP);w.loadDataWithBaseURL(null,"<meta name='viewport' content='width=device-width,initial-scale=1'><body style='margin:0;background:#000'><img style='width:100%' src='data:image/png;base64,"+data+"'></body>","text/html","UTF-8",null);new AlertDialog.Builder(this).setView(w).setPositiveButton("Close",(d,v)->w.destroy()).show();}
 @Override protected void onDestroy(){generation++;worker.shutdownNow();super.onDestroy();}
}
