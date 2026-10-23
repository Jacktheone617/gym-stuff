const fs = require('node:fs');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const {randomUUID} = require('node:crypto');
const elements = new Map();
function element(key) {
  if (!elements.has(key)) elements.set(key, {
    dataset: {}, classList: {add(){}, remove(){}, toggle(){}},
    handlers: {}, addEventListener(name, action){this.handlers[name]=action;},
    setAttribute(){}, showModal(){}, close(){}, focus(){}, select(){}, click(){},
    value:'', innerHTML:''
  });
  return elements.get(key);
}
const stored = new Map();
const context = vm.createContext({
  crypto:{randomUUID}, document:{querySelector:element,querySelectorAll:()=>[],createElement:()=>element('link')},
  localStorage:{getItem:key=>stored.get(key)||null,setItem:(key,value)=>stored.set(key,value)},
  setTimeout:()=>1,clearTimeout(){},confirm:()=>true,console,assert,Blob,URL
});
vm.runInContext(fs.readFileSync('preview/plan.js','utf8'),context);
vm.runInContext(fs.readFileSync('preview/app.js','utf8'),context);
vm.runInContext(`
const click = (action, extra={}) => $('#content').handlers.click({target:{closest:()=>({dataset:{action,...extra}})}});
assert.equal(data.routines.length,6);
assert.equal(data.routines.map(r=>r.name).join(','),'Push A,Pull A,Legs A,Push B,Pull B,Legs B');
assert.equal(data.routines.filter(r=>r.exercises.some(e=>e.name==='Abdominal Crunch machine')).map(r=>r.day).join(','),'Tuesday,Thursday,Saturday');
assert.equal(installPlan(data),false);
assert.equal(data.routines.length,6);
for(const routine of data.routines){
 click('start',{id:routine.id});
 routine.exercises.forEach((exercise,i)=>{
  assert.equal(data.draft.exercises[i].sets.length,exercise.targetSets);
  assert.equal(data.draft.exercises[i].targetReps,exercise.targetReps);
  assert.ok(data.draft.exercises[i].sets.every(s=>s.kg===''&&s.reps===''));
 });
 click('discard');
}
const legacy={version:1,routines:['Push','Pull','Legs'].map(name=>({id:uid(),name,exercises:[]})),history:[]};
assert.equal(installPlan(legacy),true);assert.equal(legacy.routines.length,6);
const custom={version:1,routines:[{id:'custom',name:'My workout',exercises:[{id:'custom-e',name:'Curl'}]}],history:[{routine:'custom'}],draft:{name:'Keep me'}};
const before=JSON.stringify(custom);
installPlan(custom);assert.equal(custom.routines.length,7);
assert.equal(custom.draft.name,'Keep me');assert.equal(custom.history.length,1);
assert.equal(custom.routines[0].exercises[0].name,'Curl');
assert.equal(installPlan(custom),false);assert.equal(custom.routines.length,7);
data={version:1,routines:[{id:uid(),name:'Custom',exercises:[]},{id:uid(),name:'Other',exercises:[]}],history:[]};
assert.equal(data.history.length,0);
const push=data.routines[0];
click('start',{id:push.id});
assert.equal(tab,'Routines');
assert.equal(data.draft,undefined);
push.exercises.push({id:uid(),name:'Bench <press>'});
tab='Train'; click('start',{id:push.id});
assert.equal(data.draft.exercises[0].sets.length,3);
assert.ok($('#content').innerHTML.includes('Bench &lt;press&gt;'));
click('finish'); assert.equal(data.history.length,0);
const set=data.draft.exercises[0].sets[0];
set.kg='60'; set.reps=''; click('finish'); assert.equal(data.history.length,0);
set.reps='8.5'; click('finish'); assert.equal(data.history.length,0);
set.reps='8'; click('finish');
assert.equal(data.history.length,1);
assert.equal(data.history[0].exercises[0].sets.length,1);
assert.equal(data.draft,undefined);
assert.equal(JSON.parse(localStorage.getItem(KEY)).history.length,1);
const session=data.history[0];session.time=Date.now()-10000;
assert.equal(previous(push.id,Date.now(),false),session);
assert.equal(previous(data.routines[1].id,Date.now(),false),undefined);
const anchor=new Date(2026,9,23,17).getTime();
session.time=new Date(2026,9,12,0).getTime();
assert.equal(previous(push.id,anchor,true),session);
session.time=new Date(2026,9,19,0).getTime();
assert.equal(previous(push.id,anchor,true),undefined);
assert.throws(()=>validate({version:1,routines:[],history:[{}]}));
assert.equal(validate(JSON.parse(JSON.stringify(data))).history.length,1);
click('start',{id:push.id});
$('#content').handlers.input({target:{dataset:{e:'0',s:'0',field:'kg'},value:'62.5'}});
assert.equal(JSON.parse(localStorage.getItem(KEY)).draft.exercises[0].sets[0].kg,'62.5');
click('discard');assert.equal(data.draft,undefined);
console.log('Preview checks passed: routines, escaping, set validation, saving, comparison, restore validation, draft persistence.');
`,context);
