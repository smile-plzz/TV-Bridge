'use strict';
const $=s=>document.querySelector(s);
let token=sessionStorage.getItem('tvbridge-token')||'', activeUpload=null, cancelled=false;
function notify(message,error=false){$('#notice').textContent=message;$('#notice').classList.toggle('error',error);}
function showConnected(yes){$('#pair').hidden=yes;$('#connected').hidden=!yes;$('#badge').textContent=yes?'Paired with TV':'Local connection';}
async function api(path,options={}){
  const headers={...options.headers};if(token)headers.Authorization='Bearer '+token;
  let response;try{response=await fetch(path,{...options,headers,signal:options.signal||AbortSignal.timeout(path==='/api/file'?1800000:20000)});}catch(e){throw new Error('Cannot reach the TV. Keep TV Bridge open and check your Wi-Fi connection.');}
  if(response.status===401){token='';sessionStorage.removeItem('tvbridge-token');showConnected(false);}
  if(!response.ok){let d;try{d=await response.json();}catch(e){}throw new Error(d?.message||'Request failed. Please retry.');}
  return response;
}
const json=(path,data,method='POST')=>api(path,{method,headers:{'Content-Type':'application/json'},body:JSON.stringify(data)});
const size=n=>n<1024?n+' B':n<1048576?(n/1024).toFixed(1)+' KB':n<1073741824?(n/1048576).toFixed(1)+' MB':(n/1073741824).toFixed(2)+' GB';
function busy(button,work){button.disabled=true;return work().catch(e=>notify(e.message,true)).finally(()=>button.disabled=false);}
$('#pairForm').addEventListener('submit',e=>{e.preventDefault();busy($('#connect'),async()=>{
  const r=await json('/api/pair',{code:$('#code').value,name:$('#phoneName').value||'My phone'});const data=await r.json();notify('Approve this phone on your TV…');
  for(let i=0;i<30;i++){await new Promise(r=>setTimeout(r,1500));const r=await json('/api/pair/status',{request:data.request});const d=await r.json();if(d.token){token=d.token;sessionStorage.setItem('tvbridge-token',token);showConnected(true);notify('Connected. Ready when you are.');await refresh();return;}}
  throw new Error('Pairing timed out. Try again.');
});});
document.querySelectorAll('[data-tab]').forEach(b=>b.addEventListener('click',()=>{document.querySelectorAll('.tab').forEach(t=>t.hidden=t.id!==b.dataset.tab);document.querySelectorAll('[data-tab]').forEach(n=>n.setAttribute('aria-pressed',String(n===b)));}));
async function refresh(){
  const data=await(await api('/api/files')).json();$('#space').textContent=size(data.free)+' free on TV';
  const list=$('#fileList');list.replaceChildren();
  if(!data.files.length){const li=document.createElement('li');li.textContent='No files yet. Send your first one above.';list.append(li);}
  for(const f of data.files){const li=document.createElement('li'),name=document.createElement('div'),meta=document.createElement('div'),actions=document.createElement('div');name.className='filename';name.textContent=f.name;meta.className='muted';meta.textContent=size(f.size)+' · '+new Date(f.modified).toLocaleString();actions.className='file-actions';
    const download=document.createElement('button');download.textContent='Download';download.className='secondary';download.onclick=()=>busy(download,()=>downloadFile(f));
    const remove=document.createElement('button');remove.textContent='Delete';remove.className='secondary';remove.onclick=()=>{if(confirm('Delete '+f.name+' from the TV?'))busy(remove,async()=>{await json('/api/file',{name:f.name},'DELETE');await refresh();notify('File deleted.');});};
    actions.append(download,remove);li.append(name,meta,actions);list.append(li);
  }
}
function saveBlob(blob,name){const url=URL.createObjectURL(blob),a=document.createElement('a');a.href=url;a.download=name;document.body.append(a);a.click();a.remove();setTimeout(()=>URL.revokeObjectURL(url),60000);}
async function downloadFile(file){
  // A one-use form would expose credentials. Authenticated fetch keeps tokens out of URLs.
  if(file.size>200*1024*1024&&!confirm('This file is '+size(file.size)+'. Browser downloads may need substantial phone memory. Continue?'))return;
  notify('Downloading '+file.name+'… Keep this page open.');
  const r=await json('/api/file',{name:file.name});saveBlob(await r.blob(),file.name);notify('Download ready. Check your browser’s downloads.');
}
$('#refresh').onclick=()=>busy($('#refresh'),refresh);
function upload(file,index,count){return new Promise((resolve,reject)=>{
  const xhr=new XMLHttpRequest();activeUpload=xhr;xhr.open('POST','/api/upload');xhr.setRequestHeader('Authorization','Bearer '+token);xhr.setRequestHeader('X-File-Name',encodeURIComponent(file.name));xhr.timeout=30*60*1000;
  xhr.upload.onprogress=e=>{if(e.lengthComputable){const pc=Math.round(e.loaded/e.total*100);$('#progress').value=pc;$('#progressText').textContent=`${index}/${count} · ${file.name} · ${pc}%${pc===100?' · Finishing on TV…':''}`;}};
  xhr.onload=()=>{activeUpload=null;if(xhr.status===200)resolve();else{if(xhr.status===401){token='';sessionStorage.removeItem('tvbridge-token');showConnected(false);}let msg='Upload failed. Check the TV connection and retry.';try{msg=JSON.parse(xhr.responseText).message||msg;}catch(e){}reject(new Error(msg));}};
  xhr.onerror=()=>{activeUpload=null;reject(new Error('Transfer interrupted. Keep the TV app open, then retry.'));};xhr.ontimeout=xhr.onerror;xhr.onabort=()=>{activeUpload=null;reject(new Error('Transfer cancelled. You can retry from the start.'));};xhr.send(file);
});}
$('#upload').onchange=async()=>{
  const files=[...$('#upload').files];if(!files.length)return;
  cancelled=false;$('#upload').disabled=true;$('#transfer').hidden=false;
  try{for(let i=0;i<files.length;i++){if(cancelled)break;const file=files[i];if(!file.size)throw new Error('Empty files cannot be sent.');if(file.size>1073741824)throw new Error(file.name+' exceeds the 1 GiB limit.');await upload(file,i+1,files.length);}notify('Files sent. Open “Files on TV” to view them.');await refresh();}
  catch(e){notify(e.message,true);}finally{$('#upload').disabled=false;$('#upload').value='';$('#transfer').hidden=true;}
};
$('#cancel').onclick=()=>{cancelled=true;if(activeUpload)activeUpload.abort();};
$('#sendForm').onsubmit=e=>{e.preventDefault();busy($('#sendForm button'),async()=>{await json('/api/send',{text:$('#text').value,link:$('#asLink').checked});notify('Sent. Check your TV.');});};
$('#diagnostics').onclick=()=>busy($('#diagnostics'),async()=>{const r=await api('/api/diagnostics');saveBlob(await r.blob(),'tv-bridge-diagnostics.json');notify('Diagnostics downloaded.');});
$('#disconnect').onclick=()=>busy($('#disconnect'),async()=>{try{await json('/api/logout',{});}finally{token='';sessionStorage.removeItem('tvbridge-token');showConnected(false);notify('Disconnected.');}});
showConnected(!!token);if(token)refresh().catch(e=>notify(e.message,true));
