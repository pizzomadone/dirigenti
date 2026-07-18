package it.dirigenti.quiz;

import android.app.*;import android.os.*;import android.graphics.Color;import android.view.*;import android.widget.*;import org.json.*;import java.io.*;import java.nio.charset.StandardCharsets;import java.util.*;

public class MainActivity extends Activity{
 static class Q{String id,rif,area,question;String[] answers;int correct;}
 ArrayList<Q> all=new ArrayList<>(), deck=new ArrayList<>(); int index=0, ok=0, ko=0; LinearLayout root,answersBox; TextView title,score,question,progress; Spinner area;
 public void onCreate(Bundle b){super.onCreate(b); load(); build(); start("Tutte");}
 void build(){ ScrollView sv=new ScrollView(this); root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(28,28,28,28); sv.addView(root); setContentView(sv);
  title=txt("Dirigenti Quiz",24,true); root.addView(title); root.addView(txt("Banca dati preselettiva caricata dal PDF. La risposta corretta nel documento è sempre la prima; qui le opzioni sono mischiate.",14,false));
  area=new Spinner(this); ArrayList<String> items=new ArrayList<>(); items.add("Tutte"); TreeSet<String> set=new TreeSet<>(); for(Q q:all)set.add(q.area); for(String s:set)items.add("Area "+s); area.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items)); root.addView(area);
  area.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){start(pos==0?"Tutte":items.get(pos).replace("Area ",""));} public void onNothingSelected(android.widget.AdapterView<?> p){}});
  score=txt("",18,true); progress=txt("",14,false); question=txt("",18,false); answersBox=new LinearLayout(this); answersBox.setOrientation(LinearLayout.VERTICAL); root.addView(score); root.addView(progress); root.addView(question); root.addView(answersBox); Button reset=new Button(this); reset.setText("Azzera punteggio"); reset.setOnClickListener(v->{ok=ko=0; show();}); root.addView(reset); }
 TextView txt(String s,int sp,boolean bold){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setPadding(0,10,0,10); if(bold)v.setTypeface(android.graphics.Typeface.DEFAULT_BOLD); return v;}
 void start(String a){ deck.clear(); for(Q q:all) if(a.equals("Tutte")||q.area.equals(a)) deck.add(q); Collections.shuffle(deck); index=0; ok=ko=0; if(question!=null)show(); }
 void show(){ answersBox.removeAllViews(); if(deck.isEmpty()){question.setText("Nessuna domanda disponibile.");return;} Q q=deck.get(index%deck.size()); score.setText("Punteggio: "+ok+" corrette, "+ko+" errate - preparazione "+(ok+ko==0?"n/d":(ok*100/(ok+ko))+"%")); progress.setText("Domanda "+(index+1)+"/"+deck.size()+" • RIF. "+q.rif+" • Area "+q.area); question.setText(q.question);
  ArrayList<Integer> ord=new ArrayList<>(); for(int i=0;i<4;i++)ord.add(i); Collections.shuffle(ord); for(int oi:ord){Button b=new Button(this); b.setAllCaps(false); b.setText(q.answers[oi]); b.setOnClickListener(v->{ if(oi==q.correct){ok++; flash("Corretto",true);} else {ko++; flash("Errato. Corretta: "+q.answers[q.correct],false);} index++; show();}); answersBox.addView(b);} }
 void flash(String s,boolean good){ Toast t=Toast.makeText(this,s,Toast.LENGTH_LONG); t.show(); }
 void load(){ try{InputStream is=getAssets().open("questions.json"); ByteArrayOutputStream bo=new ByteArrayOutputStream(); byte[] buf=new byte[8192]; for(int n;(n=is.read(buf))>0;)bo.write(buf,0,n); JSONArray arr=new JSONArray(bo.toString(StandardCharsets.UTF_8.name())); for(int i=0;i<arr.length();i++){JSONObject o=arr.getJSONObject(i);Q q=new Q();q.id=o.getString("id");q.rif=o.getString("rif");q.area=o.getString("area");q.question=o.getString("question");JSONArray aa=o.getJSONArray("answers");q.answers=new String[]{aa.getString(0),aa.getString(1),aa.getString(2),aa.getString(3)};q.correct=o.getInt("correct");all.add(q);} }catch(Exception e){throw new RuntimeException(e);} }
}
