const fs = require('node:fs');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const path = require('node:path');
function journey(){
 const elements={'.shell':{},'#content':{},'.nav':{},'#details':{}};
 const context={document:{body:{classList:{add(){}}},querySelector:s=>elements[s]},window:{scrollTo(){}}};
 vm.createContext(context);
 for(const file of ['app.js','pitch.js'])vm.runInContext(fs.readFileSync(path.join(__dirname,'../preview',file),'utf8'),context);
 return {elements,run:code=>vm.runInContext(code,context),html:()=>elements['.shell'].innerHTML};
}
const j=journey();
assert(j.html().includes('Good morning, Eero.'));
for(const page of ['today','agent','plan','context']){j.run(`openPitch('${page}')`);assert(j.html().includes('aria-current="page"'));assert(!j.html().includes('undefined'))}
j.run('coordinate()');assert.equal(j.run('planStatus'),'draft','Cannot coordinate without clinic permission');
j.run("consent('clinic',true);coordinate()");assert.equal(j.run('messages.length'),3);assert(!j.run("messages.some(m=>m.agent.includes('Insurance')||m.agent.includes('Transport'))"),'Optional recipients must be omitted');
j.run('coordinate()');assert.equal(j.run('messages.length'),3,'Repeat must not duplicate handoffs');
j.run("consent('insurer',true)");assert.equal(j.run('grants.insurer'),false,'Permissions lock after review');
j.run("revise();consent('insurer',true);consent('transport',true);coordinate()");assert.equal(j.run('messages.length'),5);assert(j.run("messages.some(m=>m.body.includes('No approval'))"));
j.run('revise()');assert.equal(j.run('messages.length'),0);
j.run("answer('Can you help with transport costs?')");assert(j.run("conversation.at(-1).text.includes('Eligibility') || conversation.at(-1).text.includes('eligibility')"));
j.run("answer('Is back pain cancer?')");assert(j.run("conversation.at(-1).text.includes('cannot diagnose')"));
j.run("answer('<img src=x onerror=alert(1)>')");assert(!j.html().includes('<img src=x'));assert(j.html().includes('&lt;img'));
const updated={name:'Aino',city:'Tampere',vision:'Wheelchair access',transport:'Need an accessible ride',routine:'Gardening after lunch',companion:'Milo'};
for(const [key,value]of Object.entries(updated))j.elements['#life-'+key]={value};
j.run('saveSituation({preventDefault(){}})');assert.equal(j.run('life.city'),'Tampere');assert.equal(j.run('planStatus'),'draft');assert(!j.run('Object.values(grants).some(Boolean)'),'Context edit must clear approvals');assert.equal(j.run('conversation.length'),1);
j.run("answer('Where is the centre?')");assert(j.run("conversation.at(-1).text.includes('Tampere')"));
j.run("answer('What do you remember?')");assert(j.run("conversation.at(-1).text.includes('Gardening')"));
j.elements['#life-name'].value='';j.run('saveSituation({preventDefault(){}})');assert.equal(j.run('life.name'),'Aino','Blank edit must not replace context');
j.run('resetJourney()');assert.equal(j.run('life.name'),'Eero');assert(!j.run('Object.values(grants).some(Boolean)'));assert.equal(j.run('messages.length'),0);
console.log('PASS: pitch navigation, consent gating, recipient isolation, duplicate prevention, context edits, permission reset, scripted answers, escaping, reset');
