package com.gymstuff.app;

import android.app.*;
import android.os.Bundle;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.*;
import android.view.*;
import android.widget.*;
import android.util.AtomicFile;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** A local-only workout notebook. No account or internet permission. */
public class MainActivity extends Activity {
    JSONObject data;
    LinearLayout page;
    String tab = "Train";
    boolean lastWeek = false;
    boolean storageFailed = false;
    final int ink = Color.rgb(28, 47, 38), green = Color.rgb(36, 100, 72);
    final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("EEE d MMM, HH:mm");

    JSONObject obj(Object... values) {
        JSONObject result = new JSONObject();
        for (int i=0; i<values.length; i+=2) put(result, (String)values[i], values[i+1]);
        return result;
    }
    void put(JSONObject o, String key, Object value) {
        try { o.put(key, value); } catch (JSONException e) { throw new IllegalArgumentException(e); }
    }
    JSONArray array(JSONObject o, String key) { return o.optJSONArray(key); }
    JSONObject at(JSONArray a, int i) { return a.optJSONObject(i); }
    String id() { return UUID.randomUUID().toString(); }
    JSONObject fresh() {
        return obj("version", 1, "routines", new JSONArray(), "history", new JSONArray());
    }
    boolean installPlan(JSONObject value) throws IOException, JSONException {
        if(value.optInt("pplPlanVersion")==1)return false;
        JSONArray plan;
        try(InputStream in=getAssets().open("ppl-plan.json"); ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            byte[] bytes=new byte[4096];int n;
            while((n=in.read(bytes))!=-1)out.write(bytes,0,n);
            plan=new JSONArray(new String(out.toByteArray(),StandardCharsets.UTF_8));
        }
        JSONArray routines=array(value,"routines");
        boolean untouched=routines.length()==3&&array(value,"history").length()==0&&!value.has("draft");
        String[] names={"Push","Pull","Legs"};
        for(int i=0;untouched&&i<3;i++)untouched=at(routines,i).optString("name").equals(names[i])&&array(at(routines,i),"exercises").length()==0;
        if(untouched){routines=new JSONArray();put(value,"routines",routines);}
        for(int i=0;i<plan.length();i++) {
            JSONObject routine=at(plan,i);boolean exists=false;
            for(int j=0;j<routines.length();j++)if(at(routines,j).optString("id").equals(routine.optString("id")))exists=true;
            if(!exists)routines.put(routine);
        }
        put(value,"pplPlanVersion",1);return true;
    }
    String target(JSONObject exercise) {
        return exercise.optString("targetReps").isEmpty()?"":exercise.optInt("targetSets",3)+" sets × "+exercise.optString("targetReps")+" reps";
    }
    AtomicFile file() { return new AtomicFile(new File(getFilesDir(), "gym-stuff.json")); }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        try { data = new JSONObject(new String(file().readFully(), StandardCharsets.UTF_8)); }
        catch (FileNotFoundException e) { data = fresh(); }
        catch (Exception e) {
            new AlertDialog.Builder(this).setTitle("Could not read your workouts")
                .setMessage("Your saved file has been kept. Close the app and try again; no data will be overwritten.")
                .setPositiveButton("Close", (d,w)->finish()).setCancelable(false).show();
            return;
        }
        try { if(installPlan(data))save(); }
        catch(Exception e){toast("Could not load the routine plan.");}
        render();
    }
    boolean save() {
        FileOutputStream stream = null;
        try {
            stream = file().startWrite();
            stream.write(data.toString().getBytes(StandardCharsets.UTF_8));
            file().finishWrite(stream); storageFailed = false; return true;
        } catch (Exception e) {
            if (stream != null) file().failWrite(stream);
            if (!storageFailed) new AlertDialog.Builder(this).setTitle("Changes could not be saved")
                .setMessage("Keep the app open and free some phone storage, then try again. You can also export a backup from History.")
                .setPositiveButton("OK", null).show();
            storageFailed = true; return false;
        }
    }
    int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    TextView text(LinearLayout parent, String value, int size, boolean bold) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(ink);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        t.setPadding(0,dp(6),0,dp(6)); parent.addView(t); return t;
    }
    LinearLayout column(LinearLayout parent) {
        LinearLayout c = new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL);
        if (parent != null) parent.addView(c); return c;
    }
    LinearLayout card() {
        LinearLayout c = column(page); c.setPadding(dp(16),dp(12),dp(16),dp(12));
        GradientDrawable bg = new GradientDrawable(); bg.setColor(Color.WHITE); bg.setCornerRadius(dp(18));
        c.setBackground(bg);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,-2); p.bottomMargin=dp(14); c.setLayoutParams(p);
        return c;
    }
    Button button(LinearLayout parent, String title, Runnable action) {
        Button b = new Button(this); b.setText(title); b.setAllCaps(false); b.setTextColor(green);
        b.setMinHeight(dp(48)); parent.addView(b); b.setOnClickListener(v->action.run()); return b;
    }
    String stamp(long ms) { return Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(dateFormat); }
    long monday(long ms) {
        return WorkoutDates.monday(ms, ZoneId.systemDefault());
    }
    void render() {
        LinearLayout root = column(null); root.setBackgroundColor(Color.rgb(245,246,241));
        root.setOnApplyWindowInsetsListener((v, insets)-> {
            v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
            return insets;
        });
        setContentView(root); root.requestApplyInsets();
        LinearLayout header = column(root); header.setPadding(dp(20),dp(12),dp(20),0);
        text(header,"GYM STUFF",14,true);
        LinearLayout nav = new LinearLayout(this); header.addView(nav);
        for (String name : new String[]{"Train","Routines","History"}) {
            Button b = button(nav,name,()->{tab=name;render();});
            b.setLayoutParams(new LinearLayout.LayoutParams(0,dp(52),1));
            if (tab.equals(name)) b.setTypeface(null,Typeface.BOLD);
        }
        ScrollView scroll = new ScrollView(this); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        page=column(null); page.setPadding(dp(20),dp(12),dp(20),dp(24)); scroll.addView(page);
        if (tab.equals("Train")) train(); else if (tab.equals("Routines")) routines(); else history();
    }
    void train() {
        JSONObject draft=data.optJSONObject("draft");
        if (draft!=null) { workout(draft); return; }
        text(page,"Ready when you are.",28,true);
        long start=monday(System.currentTimeMillis()); int count=0;
        JSONArray history=array(data,"history");
        for(int i=0;i<history.length();i++) if(at(history,i).optLong("time")>=start) count++;
        text(page,count+" workouts this week · Monday–Sunday",16,false);
        text(page,"Monday–Saturday: Push / Pull / Legs A, then B. Abs: Tuesday, Thursday, Saturday. Sunday: rest.",14,false);
        text(page,"Choose today's routine",18,true);
        JSONArray routines=array(data,"routines");
        for(int i=0;i<routines.length();i++) {
            JSONObject r=at(routines,i); LinearLayout c=card();
            if(r.has("day"))text(c,r.optString("day"),14,true);
            text(c,r.optString("name"),23,true);
            text(c,array(r,"exercises").length()+" exercises",15,false);
            JSONObject prev=previous(r.optString("id"),System.currentTimeMillis(),false);
            text(c,prev==null?"No workouts yet":"Last trained: "+stamp(prev.optLong("time")),14,false);
            button(c,"Start "+r.optString("name"),()->start(r));
        }
    }
    JSONObject previous(String routine, long before, boolean week) {
        JSONObject best=null; JSONArray h=array(data,"history");
        for(int i=0;i<h.length();i++) {
            JSONObject s=at(h,i); long t=s.optLong("time");
            if(!s.optString("routine").equals(routine)) continue;
            if(!WorkoutDates.eligible(t,before,week,ZoneId.systemDefault())) continue;
            if(best==null||t>best.optLong("time")) best=s;
        } return best;
    }
    void start(JSONObject routine) {
        JSONArray exercises=array(routine,"exercises");
        if(exercises.length()==0) { tab="Routines";render();toast("Add your exercises to "+routine.optString("name")+" first.");return; }
        JSONArray entries=new JSONArray();
        for(int i=0;i<exercises.length();i++) {
            JSONObject e=at(exercises,i); JSONArray sets=new JSONArray();
            int count=Math.max(1,Math.min(20,e.optInt("targetSets",3)));
            for(int j=0;j<count;j++) sets.put(obj("kg","","reps",""));
            entries.put(obj("id",e.optString("id"),"name",e.optString("name"),"targetSets",count,"targetReps",e.optString("targetReps"),"sets",sets));
        }
        put(data,"draft",obj("routine",routine.optString("id"),"name",routine.optString("name"),"time",System.currentTimeMillis(),"exercises",entries));
        save();render();
    }
    JSONObject match(JSONObject session,String exercise) {
        if(session==null) return null;
        JSONArray es=array(session,"exercises");
        for(int i=0;i<es.length();i++) if(at(es,i).optString("id").equals(exercise))return at(es,i);
        return null;
    }
    String setSummary(JSONObject e) {
        if(e==null)return "No matching exercise recorded.";
        JSONArray sets=array(e,"sets"); ArrayList<String> rows=new ArrayList<>();
        for(int i=0;i<sets.length();i++) rows.add((i+1)+".  "+at(sets,i).optString("kg")+" kg × "+at(sets,i).optString("reps"));
        return String.join("\n",rows);
    }
    void workout(JSONObject draft) {
        text(page,draft.optString("name"),28,true);
        text(page,"Enter kg and reps for each set you complete. Your draft saves as you type.",15,false);
        Switch toggle=new Switch(this);toggle.setText("Compare with previous week");toggle.setChecked(lastWeek);toggle.setMinHeight(dp(48));page.addView(toggle);
        toggle.setOnCheckedChangeListener((b,on)->{lastWeek=on;render();});
        JSONObject prev=previous(draft.optString("routine"),draft.optLong("time"),lastWeek);
        text(page,prev==null?(lastWeek?"No matching routine last week.":"Your first session of this routine."):
            (lastWeek?"Latest matching workout last week: ":"Last workout: ")+stamp(prev.optLong("time")),14,false);
        JSONArray entries=array(draft,"exercises");
        for(int i=0;i<entries.length();i++) {
            JSONObject e=at(entries,i);LinearLayout c=card(); text(c,e.optString("name"),21,true);
            if(!target(e).isEmpty())text(c,"Target: "+target(e),14,false);
            text(c,"PREVIOUS",12,true);text(c,setSummary(match(prev,e.optString("id"))),15,false);
            JSONArray sets=array(e,"sets");
            for(int j=0;j<sets.length();j++) {
                final int index=j; JSONObject set=at(sets,j);
                LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);c.addView(row);
                text(row,""+(j+1),16,true).setWidth(dp(26));
                input(row,set,"kg","kg",false);input(row,set,"reps","reps",true);
                Button remove=button(row,"−",()->{sets.remove(index);save();render();});
                remove.setContentDescription("Remove set "+(j+1)+" of "+e.optString("name"));
                remove.setLayoutParams(new LinearLayout.LayoutParams(dp(52),dp(52)));
            }
            button(c,"+ Add set",()->{sets.put(obj("kg","","reps",""));save();render();});
        }
        button(page,"Finish & save workout",()->finishWorkout(draft));
        button(page,"Discard workout",()->confirm("Discard this workout?","This removes only the unfinished session.",()->{data.remove("draft");save();render();}));
    }
    void input(LinearLayout row,JSONObject set,String key,String hint,boolean integer) {
        EditText input=new EditText(this);input.setSingleLine();input.setTextSize(18);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | (integer?0:android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL));
        input.setHint(hint);input.setContentDescription(hint.equals("kg")?"Weight in kilograms":"Repetitions");
        input.setText(set.optString(key));row.addView(input,new LinearLayout.LayoutParams(0,dp(56),1));
        input.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int a,int c,int f){} public void onTextChanged(CharSequence s,int a,int b,int c){}
            public void afterTextChanged(Editable value){put(set,key,value.toString());save();}
        });
    }
    void finishWorkout(JSONObject draft) {
        try {
            JSONObject completed=new JSONObject(draft.toString());JSONArray exercises=array(completed,"exercises");int total=0;
            for(int i=exercises.length()-1;i>=0;i--) {
                JSONArray sets=array(at(exercises,i),"sets");
                for(int j=sets.length()-1;j>=0;j--) {
                    JSONObject s=at(sets,j);String kg=s.optString("kg").trim(),reps=s.optString("reps").trim();
                    if(kg.isEmpty()&&reps.isEmpty()){sets.remove(j);continue;}
                    double weight=Double.parseDouble(kg.replace(',','.'));int n=Integer.parseInt(reps);
                    if(!Double.isFinite(weight)||weight<0||n<1)throw new IllegalArgumentException();
                    put(s,"kg",kg.replace(',','.'));put(s,"reps",reps);total++;
                }
                if(sets.length()==0)exercises.remove(i);
            }
            if(total==0){toast("Enter at least one set. Use 0 kg for bodyweight.");return;}
            JSONArray history=array(data,"history");history.put(completed);data.remove("draft");
            if(!save()){history.remove(history.length()-1);put(data,"draft",draft);return;}
            tab="History";render();toast("Workout saved.");
        } catch(Exception e){toast("Check each set: weight must be 0 or more, and reps a whole number above 0.");}
    }
    void routines() {
        text(page,"Your routines",28,true);text(page,"Add your own exercises. Changes apply to your next workout.",16,false);
        JSONArray rs=array(data,"routines");
        for(int i=0;i<rs.length();i++) {
            JSONObject r=at(rs,i);LinearLayout c=card();text(c,r.optString("name"),22,true);
            if(r.has("day"))text(c,r.optString("day"),14,false);
            JSONArray es=array(r,"exercises");
            if(es.length()==0)text(c,"No exercises yet.",15,false);
            for(int j=0;j<es.length();j++) {
                final int index=j;JSONObject e=at(es,j);
                if(!target(e).isEmpty())text(c,target(e),14,false);
                button(c,e.optString("name")+"  ·  Edit",()->new AlertDialog.Builder(this).setTitle(e.optString("name"))
                    .setItems(new String[]{"Rename","Remove from routine"},(d,w)->{
                        if(w==0)ask("Exercise name",e.optString("name"),name->{put(e,"name",name);save();render();});
                        else confirm("Remove exercise?","Existing workouts stay in your history.",()->{es.remove(index);save();render();});
                    }).show());
            }
            button(c,"+ Add exercise",()->ask("Exercise name","",name->{es.put(obj("id",id(),"name",name));save();render();}));
            button(c,"Rename routine",()->ask("Routine name",r.optString("name"),name->{put(r,"name",name);save();render();}));
        }
        button(page,"+ Add routine",()->ask("Routine name","",name->{rs.put(obj("id",id(),"name",name,"exercises",new JSONArray()));save();render();}));
    }
    void history() {
        text(page,"Your progress",28,true);
        text(page,"Saved on this phone. Export a backup before changing phones or uninstalling.",15,false);
        button(page,"Export backup",()->{
            Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE);
            intent.putExtra(Intent.EXTRA_TITLE,"gym-stuff-"+LocalDate.now()+".json");startActivityForResult(intent,1);
        });
        button(page,"Restore backup",()->confirm("Restore a backup?","This replaces your current routines, history and unfinished workout. Export your current data first.",()->{
            startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE),2);
        }));
        JSONArray h=array(data,"history");
        if(h.length()==0)text(page,"Your finished workouts will appear here.",18,true);
        long week=-1;
        for(int i=h.length()-1;i>=0;i--) {
            final int index=i;JSONObject s=at(h,i);long m=monday(s.optLong("time"));
            if(m!=week){week=m;text(page,"Week of "+Instant.ofEpochMilli(m).atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ofPattern("d MMM yyyy")),18,true);}
            LinearLayout c=card();text(c,s.optString("name"),23,true);text(c,stamp(s.optLong("time")),15,false);
            JSONArray es=array(s,"exercises");
            for(int j=0;j<es.length();j++){JSONObject e=at(es,j);text(c,e.optString("name"),17,true);text(c,setSummary(e),16,false);}
            button(c,"Delete workout",()->confirm("Delete this workout?","This cannot be undone.",()->{h.remove(index);save();render();}));
        }
    }
    interface NameAction { void run(String name); }
    void ask(String title,String initial,NameAction action) {
        EditText field=new EditText(this);field.setSingleLine();field.setText(initial);field.setSelectAllOnFocus(true);
        LinearLayout wrap=column(null);wrap.setPadding(dp(24),0,dp(24),0);wrap.addView(field);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(title).setView(wrap).setNegativeButton("Cancel",null).setPositiveButton("Save",null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String value=field.getText().toString().trim();if(value.isEmpty()){field.setError("Enter a name");return;}
            action.run(value);dialog.dismiss();
        }));dialog.show();field.requestFocus();dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }
    void confirm(String title,String message,Runnable yes){new AlertDialog.Builder(this).setTitle(title).setMessage(message).setNegativeButton("Cancel",null).setPositiveButton("Continue",(d,w)->yes.run()).show();}
    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    @Override protected void onActivityResult(int request,int result,Intent intent) {
        super.onActivityResult(request,result,intent);if(result!=RESULT_OK||intent==null||intent.getData()==null)return;
        try {
            if(request==1) {
                try(OutputStream out=getContentResolver().openOutputStream(intent.getData(),"wt")){out.write(data.toString(2).getBytes(StandardCharsets.UTF_8));}
                toast("Backup exported.");
            } else if(request==2) {
                byte[] bytes;
                try(InputStream in=getContentResolver().openInputStream(intent.getData());ByteArrayOutputStream out=new ByteArrayOutputStream()) {
                    byte[] buffer=new byte[8192];int size;while((size=in.read(buffer))!=-1){out.write(buffer,0,size);if(out.size()>10_000_000)throw new IOException("Too large");}bytes=out.toByteArray();
                }
                JSONObject restored=new JSONObject(new String(bytes,StandardCharsets.UTF_8));validate(restored);installPlan(restored);
                JSONObject old=data;data=restored;if(!save()){data=old;return;}render();toast("Backup restored.");
            }
        }catch(Exception e){toast("Could not "+(request==1?"export backup. Try another location.":"restore: choose a valid Gym Stuff backup."));}
    }
    void validate(JSONObject root) throws JSONException {
        if(root.getInt("version")!=1)throw new JSONException("Version");
        JSONArray routines=root.getJSONArray("routines");
        for(int i=0;i<routines.length();i++) {
            JSONObject r=routines.getJSONObject(i);r.getString("id");r.getString("name");
            JSONArray es=r.getJSONArray("exercises");for(int j=0;j<es.length();j++){es.getJSONObject(j).getString("id");es.getJSONObject(j).getString("name");}
        }
        JSONArray sessions=new JSONArray(root.getJSONArray("history").toString());
        if(root.has("draft"))sessions.put(root.getJSONObject("draft"));
        for(int i=0;i<sessions.length();i++) {
            JSONObject s=sessions.getJSONObject(i);s.getString("routine");s.getString("name");
            if(s.getLong("time")<0||s.getLong("time")>System.currentTimeMillis()+86400000L)throw new JSONException("Date");
            JSONArray es=s.getJSONArray("exercises");
            for(int j=0;j<es.length();j++) {
                JSONObject e=es.getJSONObject(j);e.getString("id");e.getString("name");JSONArray sets=e.getJSONArray("sets");
                for(int k=0;k<sets.length();k++){sets.getJSONObject(k).getString("kg");sets.getJSONObject(k).getString("reps");}
            }
        }
    }
    @Override public void onBackPressed(){if(!tab.equals("Train")){tab="Train";render();}else super.onBackPressed();}
}
