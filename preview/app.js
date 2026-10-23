'use strict';
const KEY='gym-stuff-preview-v1';
const uid=()=>crypto.randomUUID();
const fresh=()=>({version:1,routines:[],history:[]});
function installPlan(value){
 if(value.pplPlanVersion===1)return false;
 const untouched=value.routines.length===3&&value.routines.every((r,i)=>r.name===['Push','Pull','Legs'][i]&&r.exercises.length===0)&&value.history.length===0&&!value.draft;
 if(untouched)value.routines=[];
 for(const routine of PPL_PLAN){if(!value.routines.some(r=>r.id===routine.id))value.routines.push(JSON.parse(JSON.stringify(routine)));}
 value.pplPlanVersion=1;return true;
}
const target=e=>e.targetReps?`${e.targetSets||3} sets × ${e.targetReps} reps`:'';
let data;let tab='Train';let week=false;let nameAction;
try{data=JSON.parse(localStorage.getItem(KEY))||fresh();}catch{data=null;}
const $=s=>document.querySelector(s);
const escape=s=>String(s).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const date=t=>new Date(t).toLocaleString('en-GB',{weekday:'short',day:'numeric',month:'short',hour:'2-digit',minute:'2-digit'});
function monday(t){const d=new Date(t);d.setHours(0,0,0,0);d.setDate(d.getDate()-(d.getDay()+6)%7);return +d;}
function previous(routine,time,weekly){const end=monday(time);const start=new Date(end);start.setDate(start.getDate()-7);return data.history.filter(s=>s.routine===routine&&s.time<time&&(!weekly||(s.time>=+start&&s.time<end))).sort((a,b)=>b.time-a.time)[0];}
function toast(message){$('#toast').textContent=message;$('#toast').classList.add('visible');clearTimeout(toast.timer);toast.timer=setTimeout(()=>$('#toast').classList.remove('visible'),4500);}
function save(){try{localStorage.setItem(KEY,JSON.stringify(data));return true;}catch{toast('Could not save. Keep this tab open and export a backup.');return false;}}
const btn=(label,action,cls='',extra='')=>`<button class="${cls}" data-action="${action}" ${extra}>${label}</button>`;
function summary(e){return e?.sets.length?e.sets.map((s,i)=>`${i+1}. &nbsp; ${escape(s.kg)} kg × ${escape(s.reps)} reps`).join('<br>'):'No matching exercise recorded yet.';}
function render(){
 document.querySelectorAll('[data-tab]').forEach(b=>{const active=b.dataset.tab===tab;b.classList.toggle('active',active);b.setAttribute('aria-current',active?'page':'false');});
 if(!data){$('#content').innerHTML='<h1>Could not read your saved data</h1><p>Your data has been kept. Reload the page to try again.</p>';return;}
 $('#content').innerHTML=tab==='Train'?(data.draft?workout():train()):tab==='Routines'?routines():history();
}
function train(){const count=data.history.filter(s=>s.time>=monday(Date.now())).length;return `<div class="eyebrow">YOUR TRAINING NOTEBOOK</div><h1>Ready when you are.</h1><p>Pick a routine. Get a little stronger.</p><div class="stat"><strong>${count}</strong><span>workouts this week<br><span class="muted">Monday – Sunday</span></span></div><p class="muted">Monday–Saturday: Push / Pull / Legs A, then B.<br>Abs: Tuesday, Thursday, Saturday · Sunday: rest.</p><h3>Choose today's routine</h3>`+data.routines.map(r=>{const p=previous(r.id,Date.now(),false);return `<section class="card"><div class="card-head"><div>${r.day?`<div class="eyebrow">${escape(r.day)}</div>`:''}<h2>${escape(r.name)}</h2></div><span class="badge">${r.exercises.length} exercises</span></div><p>${p?'Last trained: '+date(p.time):'A fresh start. Make it yours.'}</p>${btn('Start '+escape(r.name)+' &nbsp; →','start','wide primary',`data-id="${r.id}"`)}</section>`;}).join('');}
function routines(){return `<div class="eyebrow">BUILT AROUND YOU</div><h1>Your routines.</h1><p>Add the exercises you actually do. Changes apply to your next workout.</p>`+data.routines.map(r=>`<section class="card"><div class="card-head"><div>${r.day?`<div class="eyebrow">${escape(r.day)}</div>`:''}<h2>${escape(r.name)}</h2></div>${btn('Rename','rename-routine','small',`data-id="${r.id}"`)}</div>${r.exercises.length?r.exercises.map(e=>`<div class="exercise"><span>${escape(e.name)}${target(e)?`<br><small class="muted">${escape(target(e))}</small>`:''}</span>${btn('Edit','edit-exercise','small',`data-id="${r.id}" data-exercise="${e.id}"`)}</div>`).join(''):'<p class="empty">Your exercises go here.</p>'}${btn('+ Add exercise','add-exercise','wide',`data-id="${r.id}"`)}</section>`).join('')+btn('+ Add routine','add-routine','wide');}
function workout(){const d=data.draft;const p=previous(d.routine,d.time,week);return `<div class="eyebrow">WORKOUT IN PROGRESS</div><h1>${escape(d.name)}</h1><p>Enter kg and reps for each set you complete.<br>Your draft saves as you type.</p><label class="toggle"><input type="checkbox" id="week" ${week?'checked':''}> Compare with previous week</label><p class="muted">${p?(week?'Latest matching workout last week: ':'Last workout: ')+date(p.time):week?'No matching routine last week.':'Your first session of this routine.'}</p>`+d.exercises.map((e,i)=>`<section class="card"><h2>${escape(e.name)}</h2>${target(e)?`<p class="muted">Target: ${escape(target(e))}</p>`:''}<div class="previous"><div class="eyebrow">PREVIOUS</div>${summary(p?.exercises.find(x=>x.id===e.id))}</div><div class="set-head"><span>SET</span><span>WEIGHT / KG</span><span>REPS</span><span></span></div>${e.sets.map((s,j)=>`<div class="set-row"><span>${j+1}</span><input type="number" inputmode="decimal" min="0" step="any" placeholder="kg" aria-label="${escape(e.name)} set ${j+1} kilograms" data-e="${i}" data-s="${j}" data-field="kg" value="${escape(s.kg)}"><input type="number" inputmode="numeric" min="1" step="1" placeholder="reps" aria-label="${escape(e.name)} set ${j+1} reps" data-e="${i}" data-s="${j}" data-field="reps" value="${escape(s.reps)}">${btn('−','remove-set','',`data-e="${i}" data-s="${j}" aria-label="Remove set ${j+1}"`)}</div>`).join('')}${btn('+ Add set','add-set','wide',`data-e="${i}"`)}</section>`).join('')+btn('Finish & save workout','finish','wide primary')+btn('Discard workout','discard','wide danger');}
function history(){let group;return `<div class="eyebrow">SHOWING UP ADDS UP</div><h1>Your progress.</h1><p>Your workouts, saved in this browser. Export a backup to keep a copy.</p><div class="actions">${btn('Export backup','export')}${btn('Restore backup','restore')}</div>`+(data.history.length?[...data.history].sort((a,b)=>b.time-a.time).map(s=>{const m=monday(s.time);const heading=m!==group?`<h3>Week of ${new Date(m).toLocaleDateString('en-GB',{day:'numeric',month:'short',year:'numeric'})}</h3>`:'';group=m;return `${heading}<section class="card"><h2>${escape(s.name)}</h2><span class="muted">${date(s.time)}</span>${s.exercises.map(e=>`<div class="history-exercise"><strong>${escape(e.name)}</strong><div class="set-list">${summary(e)}</div></div>`).join('')}${btn('Delete workout','delete','wide danger',`data-index="${data.history.indexOf(s)}"`)}</section>`;}).join(''):'<div class="empty"><h3>Your story starts with one workout.</h3><p>Finished workouts will appear here.</p></div>');}
function ask(title,initial,callback){$('#dialog-title').textContent=title;$('#name').value=initial;nameAction=callback;$('#name-dialog').showModal();$('#name').focus();$('#name').select();}
$('#name-form').addEventListener('submit',event=>{event.preventDefault();const name=$('#name').value.trim();if(!name)return;nameAction(name);save();$('#name-dialog').close();render();});
$('#cancel').onclick=()=>$('#name-dialog').close();
document.querySelectorAll('[data-tab]').forEach(b=>b.onclick=()=>{tab=b.dataset.tab;render();});
$('#brand').onclick=e=>{e.preventDefault();tab='Train';render();};
$('#content').addEventListener('input',event=>{const t=event.target;if(t.dataset.field){data.draft.exercises[+t.dataset.e].sets[+t.dataset.s][t.dataset.field]=t.value;save();}});
$('#content').addEventListener('change',event=>{if(event.target.id==='week'){week=event.target.checked;render();}});
$('#content').addEventListener('click',event=>{
 const b=event.target.closest('[data-action]');if(!b||!data)return;const a=b.dataset.action;const r=data.routines.find(r=>r.id===b.dataset.id);
 if(a==='add-routine')return ask('New routine','',name=>data.routines.push({id:uid(),name,exercises:[]}));
 if(a==='rename-routine')return ask('Rename routine',r.name,name=>r.name=name);
 if(a==='add-exercise')return ask('New exercise','',name=>r.exercises.push({id:uid(),name}));
 if(a==='edit-exercise'){const e=r.exercises.find(e=>e.id===b.dataset.exercise);return ask('Rename exercise',e.name,name=>e.name=name);}
 if(a==='start'){
  if(!r.exercises.length){tab='Routines';render();toast('Add your exercises to '+r.name+' first.');return;}
  data.draft={routine:r.id,name:r.name,time:Date.now(),exercises:r.exercises.map(e=>({...e,sets:Array.from({length:e.targetSets||3},()=>({kg:'',reps:''}))}))};
 }
 if(a==='add-set')data.draft.exercises[+b.dataset.e].sets.push({kg:'',reps:''});
 if(a==='remove-set')data.draft.exercises[+b.dataset.e].sets.splice(+b.dataset.s,1);
 if(a==='discard'){if(!confirm('Discard this unfinished workout?'))return;delete data.draft;}
 if(a==='finish'){
  const completed=JSON.parse(JSON.stringify(data.draft));
  for(const e of completed.exercises){e.sets=e.sets.filter(s=>s.kg!==''||s.reps!=='');for(const s of e.sets){if(s.kg===''||s.reps===''||!Number.isFinite(+s.kg)||+s.kg<0||!Number.isSafeInteger(+s.reps)||+s.reps<1){toast('Complete every set: kg must be 0 or more, reps a whole number above 0.');return;}}}
  completed.exercises=completed.exercises.filter(e=>e.sets.length);
  if(!completed.exercises.length){toast('Enter at least one set. Use 0 kg for bodyweight.');return;}
  const draft=data.draft;data.history.push(completed);delete data.draft;
  if(!save()){data.history.pop();data.draft=draft;return;}tab='History';toast('Workout saved. Nice work.');
 }
 if(a==='delete'){if(!confirm('Delete this workout? This cannot be undone.'))return;data.history.splice(+b.dataset.index,1);}
 if(a==='export'){const blob=new Blob([JSON.stringify(data,null,2)],{type:'application/json'});const url=URL.createObjectURL(blob);const link=document.createElement('a');link.href=url;link.download='gym-stuff-'+new Date().toISOString().slice(0,10)+'.json';link.click();setTimeout(()=>URL.revokeObjectURL(url),1000);return;}
 if(a==='restore'){if(confirm('Replace this browser’s routines, history and draft with a backup? Export your current data first.'))$('#restore-file').click();return;}
 save();render();
});
function validate(v){
 if(!v||v.version!==1||!Array.isArray(v.routines)||!Array.isArray(v.history))throw Error();
 const str=(o,k)=>{if(typeof o[k]!=='string')throw Error();};
 for(const r of v.routines){str(r,'id');str(r,'name');if(!Array.isArray(r.exercises))throw Error();for(const e of r.exercises){str(e,'id');str(e,'name');if(e.targetSets!==undefined&&(!Number.isInteger(e.targetSets)||e.targetSets<1||e.targetSets>20))throw Error();if(e.targetReps!==undefined)str(e,'targetReps');}}
 for(const s of [...v.history,...(v.draft?[v.draft]:[])]){str(s,'routine');str(s,'name');if(!Number.isFinite(s.time)||s.time<0||s.time>Date.now()+86400000||!Array.isArray(s.exercises))throw Error();for(const e of s.exercises){str(e,'id');str(e,'name');if(!Array.isArray(e.sets))throw Error();for(const set of e.sets){str(set,'kg');str(set,'reps');}}}
 return v;
}
$('#restore-file').onchange=async event=>{const file=event.target.files[0];if(!file)return;try{if(file.size>10000000)throw Error();const restored=validate(JSON.parse(await file.text()));installPlan(restored);const old=data;data=restored;if(!save()){data=old;return;}render();toast('Backup restored.');}catch{toast('Choose a valid Gym Stuff JSON backup.');}finally{event.target.value='';}};
if(data){try{validate(data);if(installPlan(data))save();}catch{data=null;}}render();
