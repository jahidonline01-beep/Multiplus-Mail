package com.jahid.multiplusmail;
import android.content.*;import org.json.*;import java.util.*;
public class MailStorage{static String P="mail_store",K="accounts";
public static ArrayList<MailAccount> get(Context c){ArrayList<MailAccount> l=new ArrayList<>();try{JSONArray a=new JSONArray(c.getSharedPreferences(P,0).getString(K,"[]"));for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);l.add(new MailAccount(o.getString("id"),o.getString("name"),o.getString("provider"),o.getString("url"),o.optInt("slot",1)));}}catch(Exception e){}return l;}
public static int nextSlot(Context c){boolean[] used=new boolean[51];for(MailAccount m:get(c)){if(m.slot>=1&&m.slot<=50)used[m.slot]=true;}for(int i=1;i<=50;i++)if(!used[i])return i;return -1;}
public static void add(Context c,MailAccount m){ArrayList<MailAccount> l=get(c);boolean ex=false;for(int i=0;i<l.size();i++){if(l.get(i).id.equals(m.id)){l.set(i,m);ex=true;}}if(!ex)l.add(m);save(c,l);}
public static void rename(Context c,String id,String name){ArrayList<MailAccount> l=get(c);for(MailAccount m:l)if(m.id.equals(id))m.name=name;save(c,l);}
public static void del(Context c,String id){ArrayList<MailAccount> n=new ArrayList<>();for(MailAccount m:get(c))if(!m.id.equals(id))n.add(m);save(c,n);}
public static void clearAll(Context c){save(c,new ArrayList<MailAccount>());}
public static void move(Context c,String id,int dir){ArrayList<MailAccount> l=get(c);int idx=-1;for(int i=0;i<l.size();i++)if(l.get(i).id.equals(id))idx=i;if(idx<0)return;int ni=idx+dir;if(ni<0||ni>=l.size())return;Collections.swap(l,idx,ni);save(c,l);}
public static void save(Context c,ArrayList<MailAccount> l){JSONArray a=new JSONArray();try{for(MailAccount m:l){JSONObject o=new JSONObject();o.put("id",m.id);o.put("name",m.name);o.put("provider",m.provider);o.put("url",m.url);o.put("slot",m.slot);a.put(o);}}catch(Exception e){}c.getSharedPreferences(P,0).edit().putString(K,a.toString()).apply();}}