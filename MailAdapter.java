package com.jahid.multiplusmail;
import android.view.*;import android.widget.*;import androidx.recyclerview.widget.RecyclerView;import java.util.*;

public class MailAdapter extends RecyclerView.Adapter<MailAdapter.VH>{
public interface Listener{void open(MailAccount a);void menu(MailAccount a);}
ArrayList<MailAccount> list;Listener l;
public MailAdapter(ArrayList<MailAccount> list,Listener l){this.list=list;this.l=l;setHasStableIds(true);}
public ArrayList<MailAccount> getList(){return list;}
public long getItemId(int p){return list.get(p).id.hashCode();}
public VH onCreateViewHolder(ViewGroup p,int v){return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_mail,p,false));}
public void onBindViewHolder(VH h,int i){
    MailAccount a=list.get(i);
    h.serial.setText(label(a.provider)+" "+serialFor(i,a.provider));
    h.name.setText(cleanName(a.name,a.provider));
    h.provider.setText(label(a.provider));
    h.icon.setImageResource(icon(a.provider));
    h.nameIcon.setImageResource(R.drawable.ic_name_mail);
    h.itemView.setOnClickListener(v->l.open(a));
    h.itemView.setOnLongClickListener(v->{l.menu(a);return true;});
}
int serialFor(int pos,String provider){int n=0;for(int i=0;i<=pos&&i<list.size();i++){if(provider.equals(list.get(i).provider))n++;}return n==0?1:n;}
String cleanName(String n,String p){
    String label=label(p);
    if(n==null||n.trim().isEmpty())return p.equals("gmail")?"Gmail":label;
    n=n.trim();
    if(n.matches("(?i)"+label+"\\s+\\d+"))return p.equals("gmail")?"Gmail":label;
    if(n.matches("(?i)Gmail\\s+\\d+"))return "Gmail";
    return n;
}
public int getItemCount(){return list.size();}
String label(String p){if("gmail".equals(p))return"Google";if("outlook".equals(p))return"Outlook";if("yahoo".equals(p))return"Yahoo";if("exchange".equals(p))return"Exchange";return"Other";}
int icon(String p){if("gmail".equals(p))return R.drawable.ic_google;if("outlook".equals(p))return R.drawable.ic_outlook;if("yahoo".equals(p))return R.drawable.ic_yahoo;if("exchange".equals(p))return R.drawable.ic_exchange;return R.drawable.ic_other;}
static class VH extends RecyclerView.ViewHolder{TextView serial,name,provider;ImageView icon,nameIcon;VH(View v){super(v);serial=v.findViewById(R.id.txtSerial);name=v.findViewById(R.id.txtName);provider=v.findViewById(R.id.txtProvider);icon=v.findViewById(R.id.imgProvider);nameIcon=v.findViewById(R.id.imgNameIcon);}}
}
