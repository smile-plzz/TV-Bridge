'use strict';
const $=s=>document.querySelector(s);
let token=sessionStorage.getItem('tvbridge-token')||'',activeUpload=null,paused=false,working=false,fileCache=[],lastEvent='';
function readLocal(key,fallback){try{return JSON.parse(localStorage.getItem(key))||fallback;}catch(e){return fallback;}}
let pending=readLocal('tvbridge-pending',{}),history=readLocal('tvbridge-history',[]);
function persist(){try{localStorage.setItem('tvbridge-pending',JSON.stringify(pending));localStorage.setItem('tvbridge-history',JSON.stringify(history.slice(0,20)));}catch(e){}renderPending();renderHistory();}
function notify(message,error=false){$('#notice').textContent=message;$('#notice').classList.toggle('error',error);}
function showConnected(yes){$('#pair').hidden=yes;$('#connected').hidden=!yes;$('#badge').textContent=yes?'Paired with TV':'Local connection';}
async function api(path,options={}){
  const headers={...options.headers};if(token)headers.Authorization='Bearer '+token;
  const controller=new AbortController(),timeout=setTimeout(()=>controller.abort(),path.endsWith('/finish')?180000:20000);
  let response;try{response=await fetch(path,{...options,headers,signal:controller.signal});}catch(e){throw new Error('Cannot reach the TV. Check the bridge is running and both devices share the same network.');}finally{clearTimeout(timeout);}
  if(response.status===401){token='';sessionStorage.removeItem('tvbridge-token');showConnected(false);}
  if(!response.ok){let d;try{d=await response.json();}catch(e){}throw new Error(d?.message||'Request failed. Try again.');}return response;
}
const json=(path,data,method='POST')=>api(path,{method,headers:{'Content-Type':'application/json'},body:JSON.stringify(data)});
const size=n=>n<1024?n+' B':n<1048576?(n/1024).toFixed(1)+' KB':n<1073741824?(n/1048576).toFixed(1)+' MB':(n/1073741824).toFixed(2)+' GB';
function busy(button,work){button.disabled=true;return work().catch(e=>notify(e.message,true)).finally(()=>button.disabled=false);}
$('#pairForm').onsubmit=e=>{e.preventDefault();busy($('#connect'),async()=>{
  const data=await(await json('/api/pair',{code:$('#code').value,name:$('#phoneName').value||'My phone'})).json();notify('Open TV Bridge on your TV and approve this phone.');
  for(let i=0;i<35;i++){await new Promise(r=>setTimeout(r,1500));const d=await(await json('/api/pair/status',{request:data.request})).json();if(d.token){token=d.token;sessionStorage.setItem('tvbridge-token',token);showConnected(true);notify('Connected. You can keep using other TV apps during transfers.');await refresh();await poll();return;}}throw new Error('Pairing timed out. Request a new connection.');
});};
document.querySelectorAll('[data-tab]').forEach(b=>b.onclick=()=>{document.querySelectorAll('.tab').forEach(t=>t.hidden=t.id!==b.dataset.tab);document.querySelectorAll('[data-tab]').forEach(n=>n.setAttribute('aria-pressed',String(n===b)));});
async function refresh(){const data=await(await api('/api/files')).json();fileCache=data.files;$('#space').textContent=size(data.free)+' free on TV';renderFiles();}
function renderFiles(){
  const query=$('#search').value.toLocaleLowerCase(),kind=$('#kind').value,sort=$('#sort').value;
  const files=fileCache.filter(f=>(kind==='all'||f.kind===kind)&&(f.displayName||f.name).toLocaleLowerCase().includes(query)).sort((a,b)=>sort==='name'?(a.displayName||a.name).localeCompare(b.displayName||b.name):sort==='largest'?b.size-a.size:b.modified-a.modified);
  const list=$('#fileList');list.replaceChildren();
  if(!files.length){const li=document.createElement('li');li.textContent=fileCache.length?'No files match your filters.':'Nothing here yet. Send your first file above.';list.append(li);}
  for(const f of files){const li=document.createElement('li'),name=document.createElement('div'),meta=document.createElement('div'),actions=document.createElement('div');name.className='filename';name.textContent=f.displayName||f.name;meta.className='muted';meta.textContent=size(f.size)+' · '+new Date(f.modified).toLocaleString();actions.className='file-actions';
    for(const [label,work]of[['Open on TV',async()=>{await json('/api/open',{name:f.name});notify('Confirm the file on your TV.');}],['Download',async()=>{const d=await(await json('/api/download',{name:f.name})).json();const a=document.createElement('a');a.href=d.url;a.download=f.displayName||f.name;document.body.append(a);a.click();a.remove();notify('Download handed to your browser. Check Downloads or Files.');}],['Delete',async()=>{if(confirm('Delete '+(f.displayName||f.name)+' from the TV?')){await json('/api/file',{name:f.name},'DELETE');await refresh();notify('File deleted.');}}]]){const button=document.createElement('button');button.className='secondary';button.textContent=label;button.onclick=()=>busy(button,work);actions.append(button);}
    li.append(name,meta,actions);list.append(li);
  }
}
$('#search').oninput=renderFiles;$('#kind').onchange=renderFiles;$('#sort').onchange=renderFiles;$('#refresh').onclick=()=>busy($('#refresh'),refresh);
function renderPending(){const box=$('#paused');box.replaceChildren();for(const [key,p]of Object.entries(pending)){const row=document.createElement('div'),label=document.createElement('p');label.textContent='Paused: '+p.name+' · Choose the same file to resume.';const discard=document.createElement('button');discard.textContent='Discard';discard.className='secondary';discard.disabled=working;discard.onclick=()=>busy(discard,async()=>{try{await json('/api/upload/discard',{id:p.id});}catch(e){notify('Removed from this phone. Any unreachable TV partial will expire after 24 hours.',true);}delete pending[key];persist();});row.append(label,discard);box.append(row);}}
function renderHistory(){const list=$('#history');list.replaceChildren();if(!history.length){const li=document.createElement('li');li.textContent='Completed uploads will appear here.';list.append(li);}for(const item of history.slice(0,20)){const li=document.createElement('li');li.textContent=item.name+' · '+size(item.size)+' · '+new Date(item.time).toLocaleString();if(item.sha256){const detail=document.createElement('details'),summary=document.createElement('summary'),value=document.createElement('p');summary.textContent='TV file SHA-256';value.textContent=item.sha256;detail.append(summary,value);li.append(detail);}list.append(li);}}
async function identity(file){let hash=2166136261;for(const blob of[file.slice(0,32768),file.slice(Math.max(0,file.size-32768))])for(const b of new Uint8Array(await blob.arrayBuffer()))hash=Math.imul(hash^b,16777619)>>>0;return JSON.stringify([file.name,file.size,hash.toString(16)]);}
function chunk(id,offset,blob,file,index,count,start){return new Promise((resolve,reject)=>{
  const xhr=new XMLHttpRequest();activeUpload=xhr;xhr.open('POST','/api/upload/chunk');xhr.setRequestHeader('Authorization','Bearer '+token);xhr.setRequestHeader('X-Upload-Id',id);xhr.setRequestHeader('X-Upload-Offset',String(offset));xhr.timeout=120000;
  xhr.upload.onprogress=e=>{const loaded=offset+(e.lengthComputable?e.loaded:0),pc=Math.round(loaded/file.size*100),rate=Math.max(0,loaded-start.offset)/Math.max(1,(Date.now()-start.time)/1000);$('#progress').value=pc;$('#progressText').textContent=index+'/'+count+' · '+file.name+' · '+pc+'% · '+size(rate)+'/s';};
  xhr.onload=()=>{activeUpload=null;let d={};try{d=JSON.parse(xhr.responseText);}catch(e){}if(xhr.status===200)resolve(d);else reject(new Error(d.message||'Chunk failed. Choose the same file to resume.'));};
  xhr.onerror=()=>{activeUpload=null;reject(new Error('Connection interrupted. Choose the same file to resume.'));};xhr.ontimeout=xhr.onerror;xhr.onabort=()=>{activeUpload=null;reject(new Error('Upload paused. Choose the same file to resume.'));};xhr.send(blob);
});}
async function upload(file,index,count){
  if(!file.size||file.size>1073741824)throw new Error('Choose a nonempty file up to 1 GiB.');
  const signature=await identity(file);let p=pending[signature],status;
  if(p)status=await(await json('/api/upload/status',{id:p.id})).json();else{status=await(await json('/api/upload/start',{name:file.name,size:file.size,signature})).json();p={id:status.id,name:file.name};pending[signature]=p;persist();}
  if(status.signature!==signature||status.size!==file.size)throw new Error('This is a different file. Discard the paused transfer first.');
  const start={offset:status.offset,time:Date.now()};
  while(status.offset<file.size){if(paused)throw new Error('Upload paused. Choose the same file to resume.');status=await chunk(p.id,status.offset,file.slice(status.offset,Math.min(file.size,status.offset+status.chunkSize)),file,index,count,start);}
  if(paused)throw new Error('Upload paused. Choose the same file to finish it.');
  $('#progressText').textContent='Saving and verifying '+file.name+' on TV…';
  const done=await(await json('/api/upload/finish',{id:p.id})).json();history.unshift({name:file.name,size:file.size,time:Date.now(),sha256:done.sha256});delete pending[signature];persist();
  try{await json('/api/upload/ack',{id:p.id});}catch(e){}
}
$('#upload').onchange=async()=>{
  const files=[...$('#upload').files];if(!files.length)return;paused=false;working=true;$('#upload').disabled=true;$('#transfer').hidden=false;renderPending();
  try{for(let i=0;i<files.length;i++){if(paused)break;await upload(files[i],i+1,files.length);}notify('Files are on your TV. Choose Open on TV below to view one.');await refresh();}
  catch(e){notify(e.message,true);}finally{working=false;$('#upload').disabled=false;$('#upload').value='';$('#transfer').hidden=true;renderPending();}
};
$('#cancel').onclick=()=>{paused=true;if(activeUpload)activeUpload.abort();};
$('#sendForm').onsubmit=e=>{e.preventDefault();busy($('#sendForm button'),async()=>{await json('/api/send',{text:$('#text').value,link:$('#asLink').checked});notify('Sent. Open TV Bridge on your TV to view or confirm.');});};
function saveBlob(blob,name){const url=URL.createObjectURL(blob),a=document.createElement('a');a.href=url;a.download=name;document.body.append(a);a.click();a.remove();setTimeout(()=>URL.revokeObjectURL(url),60000);}
$('#diagnostics').onclick=()=>busy($('#diagnostics'),async()=>{const r=await api('/api/diagnostics');saveBlob(await r.blob(),'tv-bridge-diagnostics.json');notify('Diagnostics downloaded.');});
$('#disconnect').onclick=()=>busy($('#disconnect'),async()=>{try{await json('/api/logout',{});}finally{token='';sessionStorage.removeItem('tvbridge-token');showConnected(false);notify('Disconnected.');}});
$('#clearHistory').onclick=()=>{history=[];persist();notify('Phone transfer history cleared. TV files were kept.');};
document.querySelectorAll('[data-media]').forEach(button=>button.onclick=()=>busy(button,async()=>{await json('/api/media',{action:button.dataset.media});await new Promise(r=>setTimeout(r,300));await poll();}));
const time=n=>Math.floor(Math.max(0,n)/60000)+':'+String(Math.floor(Math.max(0,n)/1000)%60).padStart(2,'0');
async function poll(){if(!token||document.hidden)return;const d=await(await api('/api/status')).json();$('#badge').textContent='Connected';$('#subtitle').textContent=d.device+' · TV Bridge '+d.version;$('#sessionInfo').textContent=d.sessions+' paired phone(s) · Your session expires '+new Date(d.expires).toLocaleTimeString();const media=d.media||{};$('#mediaState').textContent=media.active?(media.playing?'Playing':'Paused')+' · '+time(media.position)+' / '+time(media.duration):'Open a video in TV Bridge to use playback controls.';document.querySelectorAll('[data-media]').forEach(b=>b.disabled=!media.active);const event=d.events?.at(-1)||'';if(event!==lastEvent){lastEvent=event;if(!working)await refresh();}}
setInterval(()=>poll().catch(()=>{$('#badge').textContent='Connection lost';}),5000);document.addEventListener('visibilitychange',()=>{if(!document.hidden)poll().catch(e=>notify(e.message,true));});
persist();showConnected(!!token);if(token)poll().catch(e=>notify(e.message,true));
