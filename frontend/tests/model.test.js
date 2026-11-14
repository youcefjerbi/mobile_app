import {test} from 'node:test';
import assert from 'node:assert/strict';
import {initialContext,initialPermissions,agentReply,coordinationSteps,sampleMail,sampleEvents} from '../src/model.js';
test('coordination requires clinic permission and omits unselected recipients',()=>{
 assert.deepEqual(coordinationSteps(initialPermissions,initialContext),[]);
 const steps=coordinationSteps({...initialPermissions,clinic:true},initialContext);
 assert.deepEqual(steps.map(s=>s.title),['Your personal agent','Clinic agent','Back to your day']);
 assert(!steps.some(s=>/insurance|transport agent/i.test(s.title)));
});
test('each selected recipient receives only its stated context',()=>{
 const steps=coordinationSteps({clinic:true,insurance:true,transport:true},initialContext);
 assert.equal(steps.length,5);
 const insurer=steps.find(s=>s.title==='Insurance agent').detail;
 assert(insurer.includes('transport barrier'));
 assert(!insurer.includes(initialContext.companion));
 const driver=steps.find(s=>s.title==='Transport agent').detail;
 assert(driver.includes('No home address'));assert(driver.includes('unconfirmed'));
 assert(steps.at(-1).detail.includes('No real booking'));
});
test('replies use edited context and do not infer a cancer diagnosis',()=>{
 const context={...initialContext,city:'Tampere',routine:'Lunch with friends at 12:00',transport:'Need step-free access'};
 assert(agentReply('Where is the centre?',context).includes('Tampere'));
 assert(agentReply('What do you remember?',context).includes('lunch with friends'));
 assert(agentReply('Can you help with transport?',context).includes('Need step-free access'));
 assert(agentReply('Does my back pain mean cancer?',context).includes('can’t diagnose'));
 assert(agentReply('Book the appointment',context).includes('still need to confirm'));
});
test('email and calendar fixtures are explicit samples with no real account operations',()=>{
 assert.equal(sampleMail.length,3);assert.equal(sampleEvents.length,3);
 assert(sampleMail.every(m=>m.body.includes('fictional')||m.body.includes('Fictional')||m.body.includes('Sample')));
 assert(sampleEvents.every(e=>/sample|draft/i.test(e.status)));
});
