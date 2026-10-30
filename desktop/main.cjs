const { app, BrowserWindow, Tray, Menu, Notification, ipcMain, dialog, shell, nativeImage, globalShortcut } = require('electron');
const fs = require('node:fs');
const path = require('node:path');

const APP_NAME = 'Aurestar';
let win = null, tray = null, reminderWindow = null, reminderTimer = null, focusTickTimer = null, focusSaveCounter = 0;
let state = null;
const today = () => { const d = new Date(); return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`; };
const dataDir = () => app.getPath('userData');
const dataFile = () => path.join(dataDir(), 'aurestar-data.json');
const backupDir = () => path.join(dataDir(), 'backups');
const defaultState = () => ({
  schemaVersion: 1, createdAt: Date.now(),
  settings: { notifications:true, dailyReminder:'08:00', taskReminders:true, autoBackup:true, backupTime:'23:30', focusPreset:25, focusShortBreak:5, focusLongBreak:15, focusLongBreakEvery:4, blockedWebsites:['tiktok.com','facebook.com','youtube.com'], blockDuringFocus:true, startWithWindows:false, minimizeToTray:true, theme:'dark', language:'en' },
  profile:{totalFocusMinutes:0,level:1,xp:0,streak:0,lastActiveDate:''},
  tasks:[], habits:[], lists:[{id:'inbox',name:'Inbox',color:'red',createdAt:Date.now()}],
  focusSessions:[], workoutSessions:[], completions:[], remindersSent:{}, focus:{active:false,paused:false,phase:'focus',remaining:1500,total:1500,startedAt:0,taskId:null,completedCycles:0},
  motivationQuotes:['Start now. Not later.','Well begun is half done.','Great things are done by a series of small things brought together.','You do not have to see the whole staircase; just take the first step.']
});
function readState(){
  try { const s=JSON.parse(fs.readFileSync(dataFile(),'utf8')); return { ...defaultState(), ...s, settings:{...defaultState().settings,...s.settings}, profile:{...defaultState().profile,...s.profile}, focus:{...defaultState().focus,...s.focus} }; }
  catch { return defaultState(); }
}
function saveState(){ fs.mkdirSync(dataDir(),{recursive:true}); fs.writeFileSync(dataFile(),JSON.stringify(state,null,2),'utf8'); }
function notify(title,body){
  if (!state?.settings?.notifications || !Notification.isSupported()) return;
  new Notification({title,body,silent:false}).show();
}
function showReminderLine(text){
  try {
    if(reminderWindow && !reminderWindow.isDestroyed()) reminderWindow.close();
    reminderWindow=new BrowserWindow({width:980,height:260,frame:false,transparent:false,alwaysOnTop:true,skipTaskbar:true,resizable:false,show:false,backgroundColor:'#211217',webPreferences:{contextIsolation:true,nodeIntegration:false,sandbox:true}});
    const safe=String(text||'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
    const html='<!doctype html><html><head><meta charset="utf-8"><meta http-equiv="Content-Security-Policy" content="default-src \'none\'; style-src \'unsafe-inline\'"><style>*{box-sizing:border-box}html,body{margin:0;width:100%;height:100%;background:#211217;color:#f6e9ec;font:700 30px/1.45 Inter,Segoe UI,system-ui,sans-serif}body{display:grid;place-items:center;padding:34px;text-align:center}main{max-width:900px}body:before{content:"★";position:absolute;top:20px;color:#f6c555;font-size:16px;opacity:.8}</style></head><body><main>'+safe+'</main></body></html>';
    reminderWindow.loadURL('data:text/html;charset=utf-8,'+encodeURIComponent(html));
    reminderWindow.once('ready-to-show',()=>reminderWindow?.showInactive());
    const current=reminderWindow;setTimeout(()=>{if(current&&!current.isDestroyed())current.close();},10000);
    reminderWindow.on('closed',()=>{if(reminderWindow===current)reminderWindow=null;});
  } catch(e){console.warn('Reminder window failed',e);notify(APP_NAME,text);}
}
function sendReminder(type, text){
  const key = `${today()}:${type}`;
  if(state.remindersSent[key]) return;
  state.remindersSent[key]=Date.now(); saveState(); showReminderLine(text);
}
function checkSchedule(){
  if(!state) return;
  const now=new Date(), hm=`${String(now.getHours()).padStart(2,'0')}:${String(now.getMinutes()).padStart(2,'0')}`;
  const stamp=`${today()}:${hm}`;
  if(hm==='05:30') sendReminder('wake-0530','Every morning is a new beginning.');
  if(hm==='06:00') sendReminder('wake-0600','The secret of getting ahead is getting started.');
  if(hm==='20:45') sendReminder('wind-down','Rest is part of the work. Begin winding down.');
  if(hm==='21:00') sendReminder('sleep','A well-spent day brings happy sleep.');
  if(hm>='06:00' && hm<'20:45' && !state.focus.active){const quotes=state.motivationQuotes||['Start now. Not later.','Well begun is half done.','The secret of getting ahead is getting started.'];sendReminder('study-'+hm,quotes[Math.floor(Math.random()*quotes.length)]);}
  if(hm===state.settings.dailyReminder) sendReminder('daily','Your study plan is ready. Choose one clear task and take the first step.');
  if(state.settings.taskReminders){
    for(const t of state.tasks||[]) if(!t.archived && t.dueDate===today() && t.dueTime===hm && !(t.completedDates||[]).includes(today())) sendReminder('task-'+t.id,`Task reminder: ${t.title}`);
  }
  if(state.settings.autoBackup && hm===state.settings.backupTime && !state.remindersSent[today()+':backup']){
    try{fs.mkdirSync(backupDir(),{recursive:true});fs.copyFileSync(dataFile(),path.join(backupDir(),`aurestar-${today()}.json`));state.remindersSent[today()+':backup']=Date.now();saveState();}catch(e){console.error('Auto backup failed',e);}
  }
}
function tickFocus(){
  if(!state?.focus?.active || state.focus.paused) return;
  state.focus.remaining=Math.max(0,Number(state.focus.remaining||0)-1);
  focusSaveCounter++;
  if(state.focus.remaining===0){
    if(state.focus.phase==='break'){
      const next=(Number(state.settings.focusPreset)||25)*60;
      state.focus={...state.focus,active:false,paused:false,phase:'focus',remaining:next,total:next,startedAt:0};
      showReminderLine('Break complete. Start your next focus session.');
    } else {
      const minutes=Math.max(1,Math.round((Number(state.focus.total)||1500)/60));
      state.focusSessions.push({id:crypto.randomUUID(),date:today(),minutes,createdAt:Date.now()});
      state.profile.totalFocusMinutes=(state.profile.totalFocusMinutes||0)+minutes;
      state.profile.xp=(state.profile.xp||0)+minutes;
      state.completions.push({kind:'focus',date:today(),minutes,at:Date.now()});
      const cycles=(state.focus.completedCycles||0)+1;
      const every=Math.max(1,Number(state.settings.focusLongBreakEvery)||4);
      const breakMinutes=cycles%every===0?(Number(state.settings.focusLongBreak)||15):(Number(state.settings.focusShortBreak)||5);
      state.focus={...state.focus,active:false,paused:false,phase:'break',remaining:breakMinutes*60,total:breakMinutes*60,startedAt:0,completedCycles:cycles};
      showReminderLine('Take a breath. Drink some water. Begin again.');
    }
    focusSaveCounter=0;
  }
  if(focusSaveCounter>=10 || state.focus.remaining===0){saveState();focusSaveCounter=0;}
  win?.webContents.send('aurestar:state',state);
}
function createWindow(){
  win=new BrowserWindow({width:1440,height:920,minWidth:1024,minHeight:700,show:false,backgroundColor:'#211217',title:APP_NAME,autoHideMenuBar:true,webPreferences:{preload:path.join(__dirname,'preload.cjs'),contextIsolation:true,nodeIntegration:false,sandbox:true}});
  win.loadFile(path.join(__dirname,'index.html'));
  win.once('ready-to-show',()=>win.show());
  win.on('close',(e)=>{if(state?.settings?.minimizeToTray && tray && !app.isQuitting){e.preventDefault();win.hide();}});
  win.on('closed',()=>{win=null;});
}
function createTray(){
  const iconPath=path.join(__dirname,'icons','icon16.png');
  let icon=fs.existsSync(iconPath)?nativeImage.createFromPath(iconPath):nativeImage.createEmpty();
  if(process.platform==='win32' && !icon.isEmpty()) icon=icon.resize({width:16,height:16});
  tray=new Tray(icon);tray.setToolTip(APP_NAME);
  tray.setContextMenu(Menu.buildFromTemplate([
    {label:'Open Aurestar',click:()=>{if(!win)createWindow();else win.show();}},
    {label:'Start / pause focus',accelerator:'Ctrl+Shift+L',click:()=>toggleFocus()},
    {label:'Quick add task',accelerator:'Ctrl+Shift+K',click:()=>{if(!win)createWindow();win?.show();win?.webContents.send('aurestar:quick-add');}},
    {type:'separator'},{label:'Quit',click:()=>{app.isQuitting=true;app.quit();}}
  ]));
  tray.on('double-click',()=>{if(!win)createWindow();else win.show();});
}
function toggleFocus(){
  if(!state.focus.active){
    state.focus.active=true;state.focus.paused=false;
    if(state.focus.phase==='break'){
      state.focus.total=Math.max(1,Number(state.focus.remaining)||Number(state.settings.focusShortBreak||5)*60);
      state.focus.remaining=state.focus.total;
    } else {
      state.focus.phase='focus';state.focus.total=(Number(state.settings.focusPreset)||25)*60;state.focus.remaining=state.focus.total;
    }
    state.focus.startedAt=Date.now();
  } else {state.focus.paused=!state.focus.paused;}
  saveState();win?.webContents.send('aurestar:state',state);
}
ipcMain.handle('state:get',()=>state);
ipcMain.handle('state:save',(_e,next)=>{if(!next||typeof next!=='object')throw new Error('Invalid state');const previousStart=Boolean(state?.settings?.startWithWindows);state={...state,...next};saveState();if(process.platform==='win32'&&Boolean(state.settings?.startWithWindows)!==previousStart)app.setLoginItemSettings({openAtLogin:Boolean(state.settings.startWithWindows)});return state;});
ipcMain.handle('task:complete',(_e,{id,date=today()})=>{
  const t=state.tasks.find(x=>String(x.id)===String(id));if(!t)return {ok:false};
  t.completedDates=Array.isArray(t.completedDates)?t.completedDates:[];
  const done=t.completedDates.includes(date);
  if(done)t.completedDates=t.completedDates.filter(d=>d!==date);else{t.completedDates.push(date);state.completions.push({id:String(id),kind:'task',date,at:Date.now()});state.profile.xp=(state.profile.xp||0)+10;}
  saveState();return {ok:true,done:!done,state};
});
ipcMain.handle('focus:toggle',()=>{toggleFocus();return state.focus;});
ipcMain.handle('focus:reset',(_e,minutes)=>{state.focus={...state.focus,active:false,paused:false,phase:'focus',remaining:Math.max(1,Number(minutes)||25)*60,total:Math.max(1,Number(minutes)||25)*60,startedAt:0};saveState();return state.focus;});
ipcMain.handle('focus:finish',(_e,minutes)=>{const n=Math.max(1,Number(minutes)||25);state.focusSessions.push({id:crypto.randomUUID(),date:today(),minutes:n,createdAt:Date.now()});state.profile.totalFocusMinutes=(state.profile.totalFocusMinutes||0)+n;state.profile.xp=(state.profile.xp||0)+n;state.focus={...state.focus,active:false,paused:false,phase:'focus',remaining:(Number(state.settings.focusPreset)||25)*60,total:(Number(state.settings.focusPreset)||25)*60,startedAt:0,completedCycles:(state.focus.completedCycles||0)+1};state.completions.push({kind:'focus',date:today(),minutes:n,at:Date.now()});saveState();showReminderLine('Take a breath. Drink some water. Begin again.');return state;});
ipcMain.handle('data:export',async()=>{const r=await dialog.showSaveDialog(win,{title:'Export Aurestar backup',defaultPath:`Aurestar-${today()}.json`,filters:[{name:'JSON backup',extensions:['json']}]});if(r.canceled||!r.filePath)return {canceled:true};fs.writeFileSync(r.filePath,JSON.stringify(state,null,2),'utf8');return {ok:true,filePath:r.filePath};});
ipcMain.handle('data:import',async()=>{const r=await dialog.showOpenDialog(win,{title:'Restore Aurestar backup',properties:['openFile'],filters:[{name:'JSON backup',extensions:['json']}]});if(r.canceled||!r.filePaths[0])return {canceled:true};const next=JSON.parse(fs.readFileSync(r.filePaths[0],'utf8'));if(!next||typeof next!=='object'||!Array.isArray(next.tasks))throw new Error('This file is not a valid Aurestar data backup.');state={...defaultState(),...next,settings:{...defaultState().settings,...next.settings}};saveState();win?.webContents.send('aurestar:state',state);return {ok:true,state};});
ipcMain.handle('backup:now',()=>{fs.mkdirSync(backupDir(),{recursive:true});const p=path.join(backupDir(),`aurestar-${today()}-${Date.now()}.json`);fs.copyFileSync(dataFile(),p);return {ok:true,path:p};});
ipcMain.handle('notification:test',()=>{notify(APP_NAME,'Notifications are working.');return {ok:true};});
ipcMain.handle('app:open-external',async(_e,url)=>{if(typeof url!=='string'||!/^https?:\/\//i.test(url))return false;await shell.openExternal(url);return true;});
ipcMain.handle('app:version',()=>app.getVersion());
app.whenReady().then(()=>{
  state=readState();saveState();createWindow();createTray();
  reminderTimer=setInterval(checkSchedule,15000);focusTickTimer=setInterval(tickFocus,1000);checkSchedule();
  globalShortcut.register('CommandOrControl+Shift+L',toggleFocus);
  globalShortcut.register('CommandOrControl+Shift+K',()=>{if(!win)createWindow();win?.show();win?.webContents.send('aurestar:quick-add');});
  if(process.platform==='win32' && state.settings.startWithWindows){app.setLoginItemSettings({openAtLogin:true});}
  app.on('activate',()=>{if(!win)createWindow();else win.show();});
});
app.on('before-quit',()=>{app.isQuitting=true;if(reminderTimer)clearInterval(reminderTimer);if(focusTickTimer)clearInterval(focusTickTimer);globalShortcut.unregisterAll();saveState();});
app.on('window-all-closed',()=>{if(process.platform!=='darwin' && !state?.settings?.minimizeToTray)app.quit();});
