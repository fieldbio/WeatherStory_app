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
 android.content.SharedPreferences prefs;
 ZoomImageView image; TextView info,attribution;
 String zip="",city="",office="",checked="",notice=""; int page=0,generation=0; boolean cached=false;
 static final String UA="WeatherStoriesAndroid/0.4 (personal Weather Story viewer)";
 static class Story {String title; Bitmap bitmap; Story(String t,Bitmap b){title=t;bitmap=b;}}
 int dp(int x){return (int)(getResources().getDisplayMetrics().density*x+0.5f);}
 @Override public void onCreate(Bundle state){super.onCreate(state);
  prefs=getSharedPreferences("settings",MODE_PRIVATE);zip=prefs.getString("zip","");
  LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setBackgroundColor(Color.BLACK);
  image=new ZoomImageView(this);image.setPageListener(this::move);root.addView(image,new LinearLayout.LayoutParams(-1,0,1));
  LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setBackgroundColor(Color.rgb(14,25,38));bar.setPadding(dp(8),0,dp(2),0);
  info=new TextView(this);info.setTextColor(Color.rgb(223,234,245));info.setTextSize(12);info.setSingleLine(true);info.setEllipsize(android.text.TextUtils.TruncateAt.END);info.setGravity(Gravity.CENTER_VERTICAL);info.setOnClickListener(v->details());bar.addView(info,new LinearLayout.LayoutParams(0,-1,1));
  attribution=new TextView(this);attribution.setTextColor(Color.rgb(160,182,202));attribution.setTextSize(11);attribution.setSingleLine(true);attribution.setGravity(Gravity.CENTER_VERTICAL);attribution.setPadding(dp(8),0,dp(4),0);attribution.setOnClickListener(v->details());bar.addView(attribution,new LinearLayout.LayoutParams(-2,-1));
  bar.addView(button("ZIP","Change ZIP code",v->askZip()),new LinearLayout.LayoutParams(dp(48),-1));
  Button refresh=button("↻","Refresh weather stories",v->load());refresh.setTextSize(24);bar.addView(refresh,new LinearLayout.LayoutParams(dp(44),-1));
  root.addView(bar,new LinearLayout.LayoutParams(-1,dp(36)));setContentView(root);immersive();
  if(zip.isEmpty()){updateInfo();askZip();}else{city=prefs.getString("city_"+zip,"");office=prefs.getString("office_"+zip,"");load();}
 }
 Button button(String text,String description,View.OnClickListener action){Button b=new Button(this);b.setText(text);b.setTextSize(12);b.setTextColor(Color.rgb(137,216,246));b.setBackgroundColor(Color.TRANSPARENT);b.setPadding(0,0,0,0);b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(0);b.setMinimumHeight(0);b.setContentDescription(description);b.setOnClickListener(action);return b;}
 void immersive(){getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);}
 @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);if(focus)immersive();}
 void updateInfo(){info.setText(zip.isEmpty()?"Weather Stories":city.isEmpty()?"ZIP "+zip:city+"  "+zip);String label="NWS"+(stories.isEmpty()?"":" · "+(page+1)+"/"+stories.size())+(cached?" · CACHED":"");if(!notice.isEmpty())label+=" · "+notice;attribution.setText(label);attribution.setContentDescription("National Weather Service. "+label+". Tap for details and source.");info.setContentDescription(info.getText()+". Tap for full details.");}
 void askZip(){EditText input=new EditText(this);input.setInputType(InputType.TYPE_CLASS_NUMBER);input.setSingleLine();input.setText(zip);input.setHint("Five-digit US ZIP");AlertDialog d=new AlertDialog.Builder(this).setTitle("Weather Story ZIP").setView(input).setNegativeButton("Cancel",null).setPositiveButton("Save",null).create();d.setOnShowListener(v->d.getButton(-1).setOnClickListener(w->{String z=input.getText().toString().trim();if(!z.matches("[0-9]{5}")){input.setError("Enter five digits");return;}if(!zip.equals(z)){stories.clear();image.setImageBitmap(null);page=0;cached=false;checked="";}zip=z;city=prefs.getString("city_"+zip,"");office=prefs.getString("office_"+zip,"");prefs.edit().putString("zip",zip).apply();d.dismiss();load();}));d.show();}
 byte[] download(String address)throws Exception{URL u=new URL(address);if(!u.getProtocol().equals("https"))throw new IOException("HTTPS required");HttpURLConnection c=(HttpURLConnection)u.openConnection();c.setRequestProperty("User-Agent",UA);c.setConnectTimeout(15000);c.setReadTimeout(20000);try{int code=c.getResponseCode();if(code!=200)throw new IOException("HTTP "+code+" from "+u.getHost());try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buf=new byte[16384];int n;while((n=in.read(buf))!=-1){out.write(buf,0,n);if(out.size()>20000000)throw new IOException("Response too large");}return out.toByteArray();}}finally{c.disconnect();}}
 JSONObject json(String u)throws Exception{return new JSONObject(new String(download(u),java.nio.charset.StandardCharsets.UTF_8));}
 void load(){if(zip.isEmpty()){askZip();return;}final String z=zip;final int request=++generation;notice="Loading";updateInfo();worker.execute(()->{try{
  String of=prefs.getString("office_"+z,"");String label=prefs.getString("city_"+z,"");
  if(of.isEmpty()||label.isEmpty()){try{JSONObject place=json("https://api.zippopotam.us/us/"+z).getJSONArray("places").getJSONObject(0);label=place.getString("place name")+", "+place.getString("state abbreviation");if(of.isEmpty())of=json("https://api.weather.gov/points/"+place.getString("latitude")+","+place.getString("longitude")).getJSONObject("properties").getString("gridId").toLowerCase(Locale.US);if(!of.matches("[a-z]{3}"))throw new IOException("Unexpected office");prefs.edit().putString("office_"+z,of).putString("city_"+z,label).apply();}catch(Exception e){if(of.isEmpty())throw e;}}
  String source="https://www.weather.gov/"+of+"/weatherstory";Document doc=Jsoup.parse(new String(download(source),java.nio.charset.StandardCharsets.UTF_8),source);LinkedHashMap<String,String> candidates=new LinkedHashMap<>();
  for(Element e:doc.select("img")){String u=e.absUrl("src");if(u.isEmpty())u=e.absUrl("data-src");String low=u.toLowerCase(Locale.US);boolean story=low.contains("/images/"+of+"/")||low.contains("/wxstory/")||low.contains("weatherstory")||low.contains("wxstory");boolean excluded=low.matches(".*(logo|icon|banner|facebook|twitter|youtube|header|footer|small).*");if(story&&!excluded&&u.startsWith("https://"))candidates.put(u,e.attr("alt"));}
  ArrayList<Story> found=new ArrayList<>();for(Map.Entry<String,String> c:candidates.entrySet()){try{byte[] bytes=download(c.getKey());Bitmap b=BitmapFactory.decodeByteArray(bytes,0,bytes.length);if(b!=null&&b.getWidth()>=400&&b.getHeight()>=200)found.add(new Story(c.getValue(),b));}catch(Exception ignored){}if(found.size()>=20)break;}
  if(found.isEmpty())throw new IOException("No supported story images found for this office.");String stamp=java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT,java.text.DateFormat.SHORT).format(new Date());cache(found,z,of,label,stamp);final String resolved=of,location=label;ui.post(()->{if(request!=generation)return;stories.clear();stories.addAll(found);office=resolved;city=location;page=0;cached=false;checked=stamp;notice="";show();});
 }catch(Exception e){ui.post(()->{if(request!=generation)return;boolean recovered=restore(z);if(!recovered&&!stories.isEmpty())cached=true;notice="Error";updateInfo();new AlertDialog.Builder(this).setTitle("Could not refresh").setMessage(e.getMessage()+(stories.isEmpty()?"":"\n\nShowing older images; these may be outdated.")).setPositiveButton("OK",null).show();});}});}
 void cache(ArrayList<Story> data,String z,String of,String label,String stamp){try{File folder=new File(getFilesDir(),"stories");folder.mkdirs();JSONArray a=new JSONArray();int i=0;for(Story s:data){String name="panel"+(i++)+".png";try(FileOutputStream out=new FileOutputStream(new File(folder,name))){s.bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}a.put(new JSONObject().put("file",name).put("title",s.title));}JSONObject m=new JSONObject().put("zip",z).put("office",of).put("city",label).put("checked",stamp).put("stories",a);try(FileOutputStream out=new FileOutputStream(new File(folder,"cache.json"))){out.write(m.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}}catch(Exception ignored){}}
 boolean restore(String z){try{File folder=new File(getFilesDir(),"stories");JSONObject m=new JSONObject(new String(java.nio.file.Files.readAllBytes(new File(folder,"cache.json").toPath()),java.nio.charset.StandardCharsets.UTF_8));if(!m.getString("zip").equals(z))return false;ArrayList<Story> old=new ArrayList<>();JSONArray a=m.getJSONArray("stories");for(int i=0;i<a.length();i++){JSONObject s=a.getJSONObject(i);Bitmap b=BitmapFactory.decodeFile(new File(folder,s.getString("file")).getPath());if(b!=null)old.add(new Story(s.optString("title"),b));}if(old.isEmpty())return false;stories.clear();stories.addAll(old);page=0;office=m.getString("office");city=m.optString("city",prefs.getString("city_"+z,""));checked=m.optString("checked","");cached=true;show();return true;}catch(Exception e){return false;}}
 void move(int d){if(stories.isEmpty())return;int target=Math.max(0,Math.min(stories.size()-1,page+d));if(target!=page){page=target;show();}}
 void show(){if(!stories.isEmpty())image.setImageBitmap(stories.get(page).bitmap);updateInfo();}
 void details(){String title=stories.isEmpty()?"":stories.get(page).title;String text=(city.isEmpty()?"":city+"\n")+"ZIP: "+zip+"\nNational Weather Service"+(office.isEmpty()?"":" • "+office.toUpperCase(Locale.US))+"\n"+(cached?"CACHED — may be outdated\n":"")+(checked.isEmpty()?"":"Last checked: "+checked+" (not publication time)\n")+"\n"+title+"\n\nPinch to zoom (up to 6x); drag to pan while zoomed. Double-tap to zoom or reset. Swipe left/right at normal zoom for stories.\nWeather Stories v0.4";AlertDialog.Builder d=new AlertDialog.Builder(this).setTitle("Weather Story details").setMessage(text).setPositiveButton("Close",null);if(!office.isEmpty())d.setNeutralButton("NWS source",(v,w)->{try{startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://www.weather.gov/"+office+"/weatherstory")));}catch(Exception e){Toast.makeText(this,"No browser available",Toast.LENGTH_SHORT).show();}});d.show();}
 @Override protected void onDestroy(){generation++;worker.shutdownNow();super.onDestroy();}
}
