const fs=require('node:fs');
const vm=require('node:vm');
const assert=require('node:assert/strict');
const path=require('node:path');
const elements=new Map();
const node=id=>{if(!elements.has(id))elements.set(id,{textContent:'',innerHTML:'',classList:{toggle(){}},setAttribute(){}});return elements.get(id);};
const context={document:{getElementById:node},window:{},localStorage:{getItem:()=>null,setItem(){}},Date,Intl,Math,JSON,String,Array,Set,Map,console,module:{exports:{}}};
vm.runInNewContext(fs.readFileSync(path.join(__dirname,'../android/app/src/main/assets/game.js'),'utf8'),context);
const {pathPuzzle,sumPuzzle,memoryPuzzle,neighbors}=context.module.exports;
for(let seed=0;seed<300;seed++)for(let level=1;level<=9;level++){
  const p=pathPuzzle(seed,level);
  assert.equal(new Set(p.route).size,p.route.length);
  assert.ok(p.route.length<=p.limit);
  assert.equal(p.anchors.get(p.route[0]),1);
  assert.equal(p.anchors.get(p.route[p.route.length-1]),p.anchors.size);
  p.route.forEach((cell,i)=>{assert.ok(cell>=0&&cell<p.n*p.n);assert.ok(!p.wall.has(cell));if(i)assert.ok(neighbors(cell,p.n).includes(p.route[i-1]));});
  const s=sumPuzzle(seed,level);
  assert.equal(new Set(s.tiles).size,9);
  assert.equal(s.rows.reduce((a,b)=>a+b),45);
  assert.equal(s.cols.reduce((a,b)=>a+b),45);
  const m=memoryPuzzle(seed,level);
  assert.ok(m.sequence.length>=3&&m.sequence.every(i=>i>=0&&i<m.n*m.n));
}
console.log('2,700 seeds × 3 puzzle types: valid');
