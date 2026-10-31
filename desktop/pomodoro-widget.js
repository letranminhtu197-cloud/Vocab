let latestState=null,refreshTimer=null,busy=false;
const $=id=>document.getElementById(id);
const send=(type,payload={})=>chrome.runtime.sendMessage({type,...payload});
function formatSeconds(value){const n=Math.max(0,Math.ceil(Number(value)||0));return String(Math.floor(n/60)).padStart(2,'0')+':'+String(n%60).padStart(2,'0');}
function render(response){
  if(!response?.ok||!response.result?.focus)return;
  latestState=response.state||latestState;
  const f=response.result.focus,s=latestState?.settings||{};
  const isBreak=f.phase==='break',active=Boolean(f.active),paused=Boolean(f.paused);
  const seconds=isBreak?f.remaining:(f.countdown===false?f.elapsed:f.remaining);
  $('time').textContent=formatSeconds(seconds);
  $('phase').textContent=(isBreak?'BREAK':f.phase==='idle'?'FOCUS':'FOCUS')+' · '+(active?'RUNNING':paused?'PAUSED':'READY');
  $('detail').textContent=isBreak
    ? (f.breakKind==='long'?'Long break':'Short break')+' · '+Number(f.pomodorosCompleted||0)+' focus cycles completed'
    : (f.countdown===false?'Count-up':'Countdown')+' · '+(Number(f.mode)||Number(s.focusPreset)||25)+' min focus';
  $('toggle').textContent=active?'Pause':paused?'Resume':isBreak?'Start break':'Start focus';
  $('break').disabled=active||paused||isBreak;
  $('skip').hidden=!isBreak;
  $('stop').disabled=!(active||paused);
  $('reset').disabled=!(active||paused||isBreak||f.phase==='idle');
}
async function refresh(){
  if(busy)return;
  try{const response=await send('getFocusState');render(response);}
  catch(error){$('detail').textContent='Could not connect to focus service';console.error(error);}
}
async function act(type,payload={}){
  if(busy)return;busy=true;
  try{
    const response=await send(type,payload);
    if(!response?.ok){$('detail').textContent=response?.error||'Action failed';}
    else render(response);
  }catch(error){$('detail').textContent=String(error?.message||error);}
  finally{busy=false;await refresh();}
}
$('toggle').addEventListener('click',()=>{
  const f=latestState?.focusEngine||{};
  act(f.active?'focusPause':'focusStart',{mode:latestState?.settings?.focusPreset||25});
});
$('break').addEventListener('click',()=>act('focusStartBreak',{kind:'short'}));
$('skip').addEventListener('click',()=>act('focusSkipBreak'));
$('reset').addEventListener('click',()=>act('focusReset',{mode:latestState?.settings?.focusPreset||25}));
$('stop').addEventListener('click',()=>act('focusStop'));
$('close').addEventListener('click',()=>window.aurestarDesktop.closePomodoroWidget());
chrome.storage.onChanged.addListener((changes,area)=>{if(area==='local'&&changes.focusEngine)refresh();});
refresh();
refreshTimer=setInterval(refresh,1000);
window.addEventListener('beforeunload',()=>{if(refreshTimer)clearInterval(refreshTimer);});
