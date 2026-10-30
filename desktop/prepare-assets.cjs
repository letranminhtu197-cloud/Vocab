const fs=require('node:fs');const path=require('node:path');
const source=path.resolve(__dirname,'..','legacy-extension','Aurestar');
for(const name of ['assets','icons']){const from=path.join(source,name),to=path.join(__dirname,name);if(!fs.existsSync(from)){console.warn('Legacy asset folder not found:',from);continue;}fs.rmSync(to,{recursive:true,force:true});fs.cpSync(from,to,{recursive:true});}
for(const name of ['logo-star.svg','icon.svg']){const from=path.join(source,name),to=path.join(__dirname,name);if(fs.existsSync(from))fs.copyFileSync(from,to);}
console.log('Prepared Aurestar legacy icons and media assets for desktop packaging.');
