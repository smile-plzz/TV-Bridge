const assert=require('node:assert/strict');
const fs=require('node:fs');
const vm=require('node:vm');
const {File}=require('node:buffer');
const source=fs.readFileSync('app/src/main/assets/app.js','utf8');
const definition=source.split('\n').find(line=>line.startsWith('async function identity('));
const identity=vm.runInNewContext(definition+'; identity');
(async()=>{
  const original=new File(['same photo bytes'],'photo.jpg',{lastModified:10});
  const reselected=new File(['same photo bytes'],'photo.jpg',{lastModified:99});
  const value=await identity(original);
  assert.equal(await identity(reselected),value,'Photo re-export timestamps must not prevent recovery');
  assert.notEqual(await identity(new File(['other photo data'],'photo.jpg')),value,'Different content');
  assert.notEqual(await identity(new File(['same photo bytes'],'other.jpg')),value,'Different name');
  assert.notEqual(await identity(new File(['same photo bytes!'],'photo.jpg')),value,'Different size');
  console.log('4 browser file-identity checks passed');
})().catch(e=>{console.error(e);process.exit(1);});
