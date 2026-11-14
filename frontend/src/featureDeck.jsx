import React,{useMemo,useState} from 'react';

const Icon=({children})=><span className="mini-icon" aria-hidden="true">{children}</span>;

export function FeatureDeck({name,companion,onAsk,onPlan,onNotice}){
  const [meds,setMeds]=useState([{id:1,name:'Vitamin D3',time:'08:00',taken:false},{id:2,name:'Daily supplement',time:'19:00',taken:false}]);
  const [mood,setMood]=useState(null);
  const [expanded,setExpanded]=useState(null);
  const [reminder,setReminder]=useState(false);
  const taken=meds.filter(m=>m.taken).length;
  const greeting=useMemo(()=>mood===null?'How are you feeling today?':mood<2?'I’m here with you. Let’s keep today gentle.':mood<4?'Thanks for checking in. Small steps count.':'Lovely. Let’s keep that feeling going.',[mood]);
  const toggle=(id)=>setMeds(items=>items.map(item=>item.id===id?{...item,taken:!item.taken}:item));
  return <>
    <section className="daily-progress" aria-label="Today’s progress">
      <div className="progress-ring" style={{'--progress':`${taken/meds.length*360}deg`}}><span>{taken}/{meds.length}</span></div>
      <div><span className="eyebrow">YOUR GENTLE DAILY PLAN</span><h2>{taken===meds.length?'All set for today.':'A few small things, together.'}</h2><p>{taken===meds.length?'You’ve done what you needed to do.':'Your agent will keep the rest simple.'}</p></div>
      <button className="mini-action" onClick={()=>setExpanded(expanded==='meds'?null:'meds')} aria-expanded={expanded==='meds'}>{expanded==='meds'?'Close':'See plan'}</button>
    </section>
    {expanded==='meds'&&<section className="feature-panel slide-in"><div className="feature-panel-head"><div><span className="eyebrow">TODAY’S ROUTINE</span><h2>Medications, your way</h2></div><span className="soft-badge">Prototype</span></div>{meds.map(med=><div className="med-row" key={med.id}><span className={`med-orb ${med.taken?'taken':''}`}>{med.taken?'✓':'○'}</span><div><strong>{med.name}</strong><small>{med.time} · daily</small></div><button className={`check-button ${med.taken?'done':''}`} aria-pressed={med.taken} onClick={()=>{toggle(med.id);onNotice(med.taken?`${med.name} marked as not taken.`:`${med.name} marked as taken.`)}}>{med.taken?'Taken':'Mark taken'}</button></div>)}<button className="agent-nudge" onClick={()=>onAsk('Can you help me remember my medicines?')}><Icon>✦</Icon><span>Want reminders that fit your day?<small>Your agent can suggest a gentle routine.</small></span><b>›</b></button></section>}
    <section className="checkin-card">
      <div className="checkin-top"><div><span className="eyebrow">A MOMENT FOR YOU</span><h2>{greeting}</h2></div><span className="pulse-face">{mood===null?'☼':['☹','◔','☺','✦'][mood]}</span></div>
      <div className="mood-scale" role="group" aria-label="How are you feeling today?">{['A bit low','Not bad','Good','Great'].map((label,index)=><button className={mood===index?'active':''} onClick={()=>{setMood(index);onNotice('Your private check-in was saved for this session.')}} key={label}><span>{['☁','◔','☼','✦'][index]}</span>{label}</button>)}</div>
      {mood!==null&&<button className="small-help" onClick={()=>onAsk(mood<2?'I could use a gentle check-in today.':'I am feeling good today.')}>Tell my agent more <b>→</b></button>}
    </section>
    <section className="quick-actions"><div className="section-title"><h2>What can I help with?</h2></div><div className="quick-grid">
      <button onClick={()=>onAsk('Can you help with my transport costs?')}><Icon>⌂</Icon><strong>Getting there</strong><small>Travel support</small></button>
      <button onClick={()=>onAsk('What is on my care plan?')}><Icon>♡</Icon><strong>My next step</strong><small>Care plan</small></button>
      <button onClick={()=>{setReminder(!reminder);onNotice(reminder?'Sample reminder paused.':'Sample gentle reminder set for 18:30.')}}><Icon>{reminder?'✓':'◷'}</Icon><strong>{reminder?'Reminder set':'Remind me'}</strong><small>{reminder?'18:30 today':'When it suits me'}</small></button>
      <button onClick={()=>onAsk(`I want to talk about ${companion}.`)}><Icon>⌁</Icon><strong>My life</strong><small>{companion}</small></button>
    </div></section>
    <button className="care-coach" onClick={onPlan}><span className="coach-orbit">✦</span><span><strong>Care coach says: you’re not alone.</strong><small>We can take one practical step together.</small></span><b>Open plan →</b></button>
  </>;
}
