const $=id=>document.getElementById(id);
const lines={
 startup:"Hey! I'm here. Let's take one small step at a time.",
 "focus-start":"You've started. Stay with this one task—I'll keep you company.",
 "break-start":"Focus time is complete. Take a real break and come back refreshed.",
 "focus-stop":"No rush. When you're ready, we can begin another focused session.",
 "focus-tick":"You're doing well. Keep your attention on the next small step.",
 dismiss:"All right. I'll be right here when you need me."
};
let lastEvent="",lineHidden=false;
function say(text){$('line').textContent=text;$('line').hidden=false;lineHidden=false;}
window.aurestarDesktop.onItsukiEvent(payload=>{
 const event=payload?.event||"";
 if(event==="dismiss"){say(lines.dismiss);return;}
 if(event==="focus-tick"){
   const f=payload?.focus||{};
   if(f.active&&!f.paused&&f.phase==="focus"&&lastEvent!=="focus-start"){lastEvent="focus-start";say(lines["focus-start"]);}
   return;
 }
 if(lines[event]){lastEvent=event;say(lines[event]);}
});
$('hideLine').onclick=()=>{$('line').hidden=true;lineHidden=true;};
$('hidePet').onclick=()=>window.aurestarDesktop.hideItsuki();
$('dismiss').onclick=()=>say("Got it. Keep going at your own pace.");
$('showLine').onclick=()=>say(lines[lastEvent]||lines.startup);
$('hidePet').onclick=()=>window.aurestarDesktop.hideItsuki();
$('showLine').onclick=()=>say(lines[lastEvent]||lines.startup);
document.addEventListener('keydown',e=>{if(e.key==="Escape")window.aurestarDesktop.hideItsuki();});
