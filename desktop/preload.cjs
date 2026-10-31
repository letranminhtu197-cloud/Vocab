const { contextBridge, ipcRenderer } = require('electron');
contextBridge.exposeInMainWorld('aurestarDesktop', {
  getState: () => ipcRenderer.invoke('state:get'),
  saveState: (state) => ipcRenderer.invoke('state:save', state),
  completeTask: (payload) => ipcRenderer.invoke('task:complete', payload),
  toggleFocus: () => ipcRenderer.invoke('focus:toggle'),
  closePomodoroWidget: () => ipcRenderer.invoke('pomodoro:widget-close'),
  onPomodoroState: (fn) => { const handler=(_e,s)=>fn(s); ipcRenderer.on('pomodoro:state',handler); return () => ipcRenderer.removeListener('pomodoro:state',handler); },
  resetFocus: (minutes) => ipcRenderer.invoke('focus:reset', minutes),
  finishFocus: (minutes) => ipcRenderer.invoke('focus:finish', minutes),
  exportData: () => ipcRenderer.invoke('data:export'),
  importData: () => ipcRenderer.invoke('data:import'),
  backupNow: () => ipcRenderer.invoke('backup:now'),
  testNotification: () => ipcRenderer.invoke('notification:test'),
  openExternal: (url) => ipcRenderer.invoke('app:open-external', url),
  getVersion: () => ipcRenderer.invoke('app:version'),
  onState: (fn) => { const handler=(_e,state)=>fn(state); ipcRenderer.on('aurestar:state',handler); return () => ipcRenderer.removeListener('aurestar:state',handler); },
  onQuickAdd: (fn) => { const handler=()=>fn(); ipcRenderer.on('aurestar:quick-add',handler); return () => ipcRenderer.removeListener('aurestar:quick-add',handler); }
});
