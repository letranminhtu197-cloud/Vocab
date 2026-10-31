const $=id=>document.getElementById(id);
let expression=1,phrase=0;
const phrases=["One step at a time.","You're doing well—keep going.","Take a breath, then continue.","Small progress still counts.","Welcome back! Let's focus."];
function change(){expression=expression%30+1;phrase=(phrase+1)%phrases.length;$('line').textContent=phrases[phrase];window.aurestarDesktop.getItsukiArtUrl(expression).then(url=>{$('itsukiArt').src=url}).catch(()=>{});}
window.aurestarDesktop.getItsukiArtUrl(1).then(url=>{$('itsukiArt').src=url}).catch(()=>{});
$('itsukiClick').addEventListener('click',change);
window.aurestarDesktop.onItsukiEvent(payload=>{const e=payload?.event;const f=payload?.focus||{};if(e==='focus-start')$('line').textContent='Focus time—one task at a time.';else if(e==='break-start')$('line').textContent='Time for a real break.';else if(e==='focus-tick'&&f.active&&!f.paused&&f.phase==='focus')$('line').textContent='Keep your attention on the next small step.';});
document.addEventListener('keydown',e=>{if(e.key==='Escape')window.aurestarDesktop.hideItsuki();});
