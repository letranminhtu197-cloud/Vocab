const $=id=>document.getElementById(id);
let displayedImage='';
const statusLabels={focus:'Focus',break:'Break',pause:'Break',stop:'Session stopped',care:'Take care',warning:'Back to focus',done:'Completed',encouragement:'Study companion'};
async function sendItsukiEvent(event='periodic',message=''){
  try{return await chrome.runtime.sendMessage({type:'itsukiExpressionAction',event,message:String(message||'').slice(0,240)});}
  catch(error){console.error('Itsuki update failed',error);return null;}
}
async function refreshItsuki(record){
  try{
    if(!record){
      const stored=await chrome.storage.local.get('itsukiExpressionState');
      record=stored?.itsukiExpressionState;
    }
    if(!record||typeof record!=='object')return false;
    if(typeof record.message==='string'&&record.message.trim())$('itsukiLine').textContent=record.message;
    $('itsukiStatus').textContent=statusLabels[record.kind]||'Study companion';
    const match=String(record.imageSrc||'').match(/assets\/itsuki\/(\d+)\.(?:jpg|png)$/i);
    if(match){
      const imageKey=String(record.imageId||match[1]);
      if(displayedImage!==imageKey){
        displayedImage=imageKey;
        const url=await window.aurestarDesktop.getItsukiArtUrl(Number(match[1]));
        $('itsukiArt').src=url;
      }
    }else if(!$('itsukiArt').src){
      $('itsukiArt').src=await window.aurestarDesktop.getItsukiArtUrl(1);
    }
    return true;
  }catch(error){console.error('Could not display Itsuki state',error);return false;}
}
chrome.storage.onChanged.addListener((changes,area)=>{
  if(area==='local'&&changes.itsukiExpressionState?.newValue)refreshItsuki(changes.itsukiExpressionState.newValue);
});
async function boot(){
  const shown=await refreshItsuki();
  if(!shown){
    $('itsukiArt').src=await window.aurestarDesktop.getItsukiArtUrl(1);
    await sendItsukiEvent('periodic');
    await refreshItsuki();
  }
}
function bindDrag(target){
  if(!target)return;
  let pointer=null,lastX=0,lastY=0,moved=false;
  target.addEventListener('pointerdown',event=>{
    if(event.button!==0||event.target.closest('button'))return;
    pointer=event.pointerId;lastX=event.screenX;lastY=event.screenY;moved=false;
    target.setPointerCapture?.(pointer);
  });
  target.addEventListener('pointermove',event=>{
    if(pointer!==event.pointerId)return;
    const dx=event.screenX-lastX,dy=event.screenY-lastY;
    if(Math.abs(dx)+Math.abs(dy)>1){moved=true;window.aurestarDesktop.dragItsukiBy(dx,dy);}
    lastX=event.screenX;lastY=event.screenY;
  });
  target.addEventListener('pointerup',event=>{
    if(pointer!==event.pointerId)return;
    pointer=null;
    if(target=== $('itsukiPet')&&!moved)sendItsukiEvent('periodic');
  });
  target.addEventListener('pointercancel',()=>{pointer=null;});
}
bindDrag($('itsukiPet'));
bindDrag($('dragHandle'));
$('hideItsuki').addEventListener('click',()=>window.aurestarDesktop.hideItsuki());
$('openWorkspace').addEventListener('click',()=>window.aurestarDesktop.openWorkspace());
boot();
