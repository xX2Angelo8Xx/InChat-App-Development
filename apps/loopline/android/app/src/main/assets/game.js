/* Loopline: deterministic puzzles, local progress, no network. */
(() => {
  'use strict';
  const $ = id => document.getElementById(id);
  const screens = ['home', 'choose', 'game', 'result'];
  const kinds = ['path', 'sum', 'memory'];
  const labels = {path:'Linie', sum:'Summe', memory:'Echo'};
  const titles = {path:'Finde den Weg.', sum:'Alles geht auf.', memory:'Halte den Takt.'};
  const today = () => {
    const d = new Date();
    return [d.getFullYear(), String(d.getMonth()+1).padStart(2,'0'), String(d.getDate()).padStart(2,'0')].join('-');
  };
  const hash = s => { let h=2166136261; for(const c of s) h=Math.imul(h^c.charCodeAt(0),16777619); return h>>>0; };
  const random = seed => { let x=seed>>>0; return () => { x=(x+0x6D2B79F5)|0; let t=Math.imul(x^(x>>>15),1|x); t^=t+Math.imul(t^(t>>>7),61|t); return ((t^(t>>>14))>>>0)/4294967296; }; };
  const shuffle = (a,r) => { a=a.slice(); for(let i=a.length-1;i>0;i--){const j=Math.floor(r()*(i+1));[a[i],a[j]]=[a[j],a[i]];} return a; };
  const neighbors = (i,n) => [i%n?i-1:-1,i%n<n-1?i+1:-1,i>=n?i-n:-1,i<n*(n-1)?i+n:-1].filter(x=>x>=0);
  let saved;
  try { saved=JSON.parse(localStorage.getItem('loopline-v1'))||{}; } catch (_) { saved={}; }
  saved.days ||= {};
  saved.freeModes ||= {path:Math.ceil((saved.free||0)/3),sum:Math.ceil(((saved.free||0)-1)/3),memory:Math.ceil(((saved.free||0)-2)/3)};
  kinds.forEach(k=>saved.freeModes[k]=Math.max(0,saved.freeModes[k]||0));
  saved.records ||= {};
  saved.stars ||= 0;
  const persist=()=>localStorage.setItem('loopline-v1',JSON.stringify(saved));
  let mode='daily', kindChoice='path', index=0, puzzle=null, run=0, busy=false, muted=!!saved.muted;
  let stats={hints:0,errors:0,resets:0,swaps:0,start:0};
  const show=id=>screens.forEach(s=>$(s).classList.toggle('hidden',s!==id));
  const soundIcon = off => off
    ? '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 9v6h4l5 4V5L8 9H4"/><path d="M3 3l18 18"/></svg>'
    : '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 9v6h4l5 4V5L8 9H4"/><path d="M17 9a4 4 0 0 1 0 6M19 6a8 8 0 0 1 0 12"/></svg>';
  function updateSound(){ $('sound').innerHTML=soundIcon(muted);$('sound').setAttribute('aria-label',muted?'Ton einschalten':'Ton ausschalten');$('sound').setAttribute('aria-pressed',String(muted)); }
  function beep(freq=480){if(muted)return;try{const C=window.AudioContext||window.webkitAudioContext,ctx=new C(),o=ctx.createOscillator(),g=ctx.createGain();o.type='sine';o.frequency.value=freq;g.gain.setValueAtTime(.055,ctx.currentTime);g.gain.exponentialRampToValueAtTime(.001,ctx.currentTime+.11);o.connect(g).connect(ctx.destination);o.start();o.stop(ctx.currentTime+.12);o.onended=()=>ctx.close();}catch(_){}}
  function streak(){let count=0,d=new Date();if(!saved.days[today()]?.complete)d.setDate(d.getDate()-1);for(let i=0;i<365;i++){const key=[d.getFullYear(),String(d.getMonth()+1).padStart(2,'0'),String(d.getDate()).padStart(2,'0')].join('-');if(!saved.days[key]?.complete)break;count++;d.setDate(d.getDate()-1);}return count;}
  function home(){run++;const d=new Date();$('today').textContent=new Intl.DateTimeFormat('de-AT',{day:'numeric',month:'long'}).format(d).toUpperCase();$('streak').textContent=streak()+' TAGE IN FOLGE';$('star-total').textContent=saved.stars+' STERNE';const step=saved.days[today()]?.step||0;$('daily-label').textContent=step>=3?'Heute abgeschlossen ✓':step?step+' / 3 geschafft':'Heute spielen';show('home');}
  function choose(){run++;kinds.forEach(k=>{$('count-'+k).textContent=(saved.freeModes[k]||0)+' GELÖST';$('best-'+k).textContent=saved.records[k]?'BESTE RUNDE '+saved.records[k].stars+' ★ · '+saved.records[k].seconds+' S':'NEU';});show('choose');}
  function pathPuzzle(seed,level){
    const r=random(seed),n=level<3?4:5,length=Math.min(n*n-2,level<3?13:18+Math.min(4,level-3));
    let route=[];
    for(let attempt=0;attempt<120;attempt++){
      const start=Math.floor(r()*n*n),used=new Set([start]),p=[start];
      const dfs=()=>{if(p.length===length)return true;for(const x of shuffle(neighbors(p[p.length-1],n),r)){if(used.has(x))continue;used.add(x);p.push(x);if(dfs())return true;p.pop();used.delete(x);}return false;};
      if(dfs()){route=p;break;}
    }
    if(!route.length)route=Array.from({length:n*n},(_,i)=>Math.floor(i/n)%2?n*Math.floor(i/n)+n-1-i%n:i).slice(0,length);
    const positions=[0],gap=level<3?4:5;
    for(let i=gap;i<route.length-2;i+=gap)positions.push(i);
    positions.push(route.length-1);
    const anchors=new Map(positions.map((pos,i)=>[route[pos],i+1]));
    const walls=new Set(Array.from({length:n*n},(_,i)=>i).filter(i=>!route.includes(i)));
    return {type:'path',n,route,anchors,walls,trace:[],level};
  }
  function swapDistance(a,b){const target=new Map(b.map((v,i)=>[v,i])),visited=new Set();let cycles=0;for(let i=0;i<a.length;i++){if(visited.has(i))continue;cycles++;let j=i;while(!visited.has(j)){visited.add(j);j=target.get(a[j]);}}return a.length-cycles;}
  function sumPuzzle(seed,level){
    const r=random(seed),solution=shuffle([1,2,3,4,5,6,7,8,9],r),scramble=Math.min(6,level<3?3:4+Math.floor((level-3)/3));
    let tiles,par;
    const rows=[0,1,2].map(y=>solution.slice(y*3,y*3+3).reduce((a,b)=>a+b,0));
    const cols=[0,1,2].map(x=>solution[x]+solution[x+3]+solution[x+6]);
    for(let attempt=0;attempt<50;attempt++){tiles=solution.slice();for(let i=0;i<scramble;i++){const a=Math.floor(r()*9),b=(a+1+Math.floor(r()*8))%9;[tiles[a],tiles[b]]=[tiles[b],tiles[a]];}par=swapDistance(tiles,solution);if(par>=Math.min(3,scramble))break;}
    const p={type:'sum',n:3,tiles,solution,par,rows,cols,selected:null,level};
    if(solvedCount(p)===6){[tiles[0],tiles[1]]=[tiles[1],tiles[0]];p.par=swapDistance(tiles,solution);}
    p.initial=tiles.slice();
    return p;
  }
  function memoryPuzzle(seed,level){
    const r=random(seed),n=level<3?3:4,count=Math.min(9,level<3?5:6+Math.floor((level-3)/2)),sequence=[];
    while(sequence.length<count){const i=Math.floor(r()*n*n);if(i!==sequence[sequence.length-1])sequence.push(i);}
    return {type:'memory',n,sequence,answer:0,phase:'ready',reverse:level>=3,level};
  }
  function make(){const kind=mode==='daily'?kinds[index]:kindChoice,seed=hash((mode==='daily'?today():'free')+':'+index+':'+kind+':v2'),level=mode==='daily'?4:Math.min(9,2+Math.floor(index/2));return kind==='path'?pathPuzzle(seed,level):kind==='sum'?sumPuzzle(seed,level):memoryPuzzle(seed,level);}
  function startDaily(){mode='daily';index=saved.days[today()]?.step||0;if(index>=3){result(true);return;}load();}
  function startFree(kind){mode='free';kindChoice=kind;index=saved.freeModes[kind]||0;load();}
  function load(){
    run++;busy=false;puzzle=make();stats={hints:0,errors:0,resets:0,swaps:0,start:Date.now()};
    $('kind').textContent=(mode==='daily'?String(index+1).padStart(2,'0')+' / ':'')+labels[puzzle.type].toUpperCase();
    $('title').textContent=titles[puzzle.type];
    $('instruction').textContent=puzzle.type==='path'?'Verbinde die Zahlen der Reihe nach. Am Ende muss jedes helle Feld Teil der Linie sein.':puzzle.type==='sum'?'Tausche Zahlen durch Antippen. Erfülle alle sechs Summen am Rand.':'Merke dir die Folge. Tippe die Felder danach '+(puzzle.reverse?'rückwärts.':'in derselben Reihenfolge.');
    $('round-label').textContent=mode==='daily'?'TAGESRUNDE · '+(index+1)+' / 3':labels[puzzle.type].toUpperCase()+' · LEVEL '+(index+1);
    $('twist').textContent=puzzle.type==='memory'&&puzzle.reverse?'↶ RÜCKWÄRTS':puzzle.type==='path'?puzzle.route.length+' FELDER FÜLLEN':puzzle.type==='sum'?'6 SUMMEN':'';
    $('progress').innerHTML=mode==='daily'?Array.from({length:3},(_,i)=>'<i class="'+(i<=index?'active':'')+'"></i>').join(''):'<span>LEVEL '+(index+1)+' · '+(saved.records[puzzle.type]?'PERSÖNLICHER REKORD '+saved.records[puzzle.type].stars+' ★':'DEIN ERSTER VERSUCH')+'</span>';
    render();show('game');
  }
  function board(n,extra=''){const el=document.createElement('div');el.className='board '+extra;el.style.gridTemplateColumns='repeat('+n+',1fr)';el.style.gridTemplateRows='repeat('+n+',1fr)';return el;}
  function render(){if(puzzle.type==='path')renderPath();else if(puzzle.type==='sum')renderSum();else renderMemory();}
  function renderPath(){
    const p=puzzle,wrap=document.createElement('div');wrap.className='board-wrap';const b=board(p.n);
    for(let i=0;i<p.n*p.n;i++){const c=document.createElement('button');c.className='cell'+(p.walls.has(i)?' wall':'')+(p.trace.includes(i)?' on':'')+(p.anchors.has(i)?' anchor':'');c.textContent=p.anchors.get(i)||'';c.dataset.cell=i;c.setAttribute('aria-label',p.walls.has(i)?'Blockiertes Feld':p.anchors.has(i)?'Zahl '+p.anchors.get(i):'Feld '+(i+1));b.append(c);}
    wrap.append(b);const svg=document.createElementNS('http://www.w3.org/2000/svg','svg');svg.setAttribute('viewBox','0 0 '+p.n*10+' '+p.n*10);svg.setAttribute('class','path-svg');const line=document.createElementNS('http://www.w3.org/2000/svg','polyline');svg.append(line);wrap.append(svg);$('play-area').replaceChildren(wrap);updatePath();
    let drawing=false;const handle=e=>{const c=document.elementFromPoint(e.clientX,e.clientY)?.closest('[data-cell]');if(c&&b.contains(c))stepPath(Number(c.dataset.cell));};
    b.onpointerdown=e=>{if(e.target.closest('.cell')){drawing=true;b.setPointerCapture(e.pointerId);handle(e);}};
    b.onpointermove=e=>{if(drawing)handle(e);};b.onpointerup=()=>drawing=false;b.onpointercancel=()=>drawing=false;
    $('feedback').textContent='0 / '+p.route.length+' helle Felder verbunden';
  }
  function updatePath(){const p=puzzle;$('play-area').querySelectorAll('.cell').forEach((c,i)=>c.classList.toggle('on',p.trace.includes(i)));$('play-area').querySelector('polyline').setAttribute('points',p.trace.map(i=>(i%p.n*10+5)+','+(Math.floor(i/p.n)*10+5)).join(' '));}
  function stepPath(i){
    const p=puzzle;if(p.type!=='path'||busy||p.walls.has(i))return;const t=p.trace,last=t[t.length-1];
    if(!t.length){if(p.anchors.get(i)!==1){$('feedback').textContent='Beginne bei der 1.';return;}}
    else {if(i===last)return;if(t.length>1&&i===t[t.length-2]){t.pop();updatePath();$('feedback').textContent=t.length+' / '+p.route.length+' helle Felder verbunden';return;}
      if(!neighbors(last,p.n).includes(i)||t.includes(i))return;
      const next=Math.max(0,...t.map(x=>p.anchors.get(x)||0))+1;
      if(p.anchors.has(i)&&p.anchors.get(i)!==next){stats.errors++;$('feedback').textContent='Zuerst zur '+next+'.';beep(210);return;}
    }
    t.push(i);beep(380+(p.anchors.get(i)||0)*55);updatePath();
    $('feedback').textContent=t.length+' / '+p.route.length+' helle Felder verbunden';
    if(p.anchors.get(i)===p.anchors.size){if(t.length===p.route.length)finish();else{$('feedback').textContent='Noch '+(p.route.length-t.length)+' helle Felder offen. Gehe ein Feld zurück.';beep(210);}}
  }
  const rowSum=(p,y)=>p.tiles.slice(y*3,y*3+3).reduce((a,b)=>a+b,0);
  const colSum=(p,x)=>p.tiles[x]+p.tiles[x+3]+p.tiles[x+6];
  const solvedCount=p=>p.rows.filter((v,y)=>v===rowSum(p,y)).length+p.cols.filter((v,x)=>v===colSum(p,x)).length;
  function renderSum(){
    const p=puzzle,layout=document.createElement('div');layout.className='sum-layout';const b=board(3,'sum-board');
    p.tiles.forEach((v,i)=>{const c=document.createElement('button');const rowDone=rowSum(p,Math.floor(i/3))===p.rows[Math.floor(i/3)],colDone=colSum(p,i%3)===p.cols[i%3];c.className='cell'+(p.selected===i?' selected':'')+(rowDone?' row-done':'')+(colDone?' col-done':'')+(p.hintCells?.includes(i)?' hint-cell':'');c.textContent=v;c.setAttribute('aria-label','Feld '+(i+1)+', Zahl '+v);c.onclick=()=>{if(busy)return;if(p.selected===null){p.selected=i;renderSum();return;}if(p.selected===i){p.selected=null;renderSum();return;}[p.tiles[p.selected],p.tiles[i]]=[p.tiles[i],p.tiles[p.selected]];p.selected=null;p.hintCells=null;stats.swaps++;beep(440+solvedCount(p)*30);renderSum();if(solvedCount(p)===6)finish();};b.append(c);});
    layout.append(b);const right=document.createElement('div');right.className='sum-targets right';right.style.gridTemplateRows='repeat(3,1fr)';
    p.rows.forEach((target,y)=>{const d=document.createElement('span'),current=rowSum(p,y);d.className='target'+(current===target?' done':'');d.innerHTML='<small>'+current+'</small><b>'+target+'</b>';d.setAttribute('aria-label','Zeile '+(y+1)+': '+current+' von '+target);right.append(d);});
    layout.append(right);const bottom=document.createElement('div');bottom.className='sum-targets bottom';bottom.style.gridTemplateColumns='repeat(3,1fr)';
    p.cols.forEach((target,x)=>{const d=document.createElement('span'),current=colSum(p,x);d.className='target'+(current===target?' done':'');d.innerHTML='<small>'+current+'</small><b>'+target+'</b>';d.setAttribute('aria-label','Spalte '+(x+1)+': '+current+' von '+target);bottom.append(d);});
    layout.append(bottom);$('play-area').replaceChildren(layout);$('feedback').textContent=solvedCount(p)+' / 6 Summen · '+stats.swaps+' Tauschzüge'+(p.selected===null?'':' · Zweite Zahl wählen');
  }
  function renderMemory(){
    const p=puzzle,b=board(p.n,'memory-board');
    for(let i=0;i<p.n*p.n;i++){const c=document.createElement('button');c.className='cell';c.setAttribute('aria-label','Feld '+(i%p.n+1)+', '+(Math.floor(i/p.n)+1));c.onclick=()=>{if(p.phase!=='input'||busy)return;const wanted=p.reverse?p.sequence[p.sequence.length-1-p.answer]:p.sequence[p.answer];if(i===wanted){p.answer++;c.classList.add('hit');beep(380+p.answer*50);$('feedback').textContent=p.answer+' / '+p.sequence.length+' richtig';if(p.answer===p.sequence.length)finish();}else{c.classList.add('wrong');stats.errors++;beep(180);p.phase='failed';$('feedback').textContent='Knapp daneben. Folge noch einmal ansehen.';renderMemory();}};b.append(c);}
    $('play-area').replaceChildren(b);
    if(p.phase==='ready'||p.phase==='failed'){const btn=document.createElement('button');btn.className='memory-start';btn.innerHTML='<span class="mini-mark">✳</span> '+(p.phase==='failed'?'Noch einmal ansehen':'Folge zeigen')+' <span>→</span>';btn.onclick=playMemory;$('play-area').append(btn);}
  }
  async function playMemory(){
    const p=puzzle,ticket=run;if(p.phase!=='ready'&&p.phase!=='failed')return;p.answer=0;p.phase='showing';renderMemory();$('feedback').textContent='Schau genau hin …';
    const pause=ms=>new Promise(resolve=>setTimeout(resolve,ms));await pause(420);
    for(const i of p.sequence){if(ticket!==run)return;const cells=$('play-area').querySelectorAll('.cell');cells[i].classList.add('lit');beep(450);await pause(p.level<3?620:510);if(ticket!==run)return;cells[i].classList.remove('lit');await pause(190);}
    if(ticket!==run)return;p.phase='input';$('feedback').textContent=p.reverse?'Jetzt rückwärts · 0 / '+p.sequence.length:'Jetzt du · 0 / '+p.sequence.length;
  }
  function finish(){
    if(busy)return;busy=true;run++;
    const seconds=Math.max(1,Math.round((Date.now()-stats.start)/1000)),penalty=stats.hints+stats.errors+stats.resets;
    const stars=Math.max(1,3-Math.min(2,penalty)),kind=puzzle.type,score={stars,seconds,swaps:stats.swaps,hints:stats.hints,errors:stats.errors};
    saved.stars+=stars;const old=saved.records[kind];if(!old||stars>old.stars||(stars===old.stars&&seconds<old.seconds))saved.records[kind]=score;
    if(mode==='daily'){const day=saved.days[today()]||{step:0,complete:false};day.step=Math.max(day.step,index+1);day.complete=day.step>=3;day.scores ||= {};day.scores[kind]=score;saved.days[today()]=day;}
    else saved.freeModes[kind]=Math.max(saved.freeModes[kind]||0,index+1);
    persist();beep(760);setTimeout(()=>result(false,score),330);
  }
  function result(already,score){
    const done=mode==='daily'&&(already||index>=2);
    $('result-title').innerHTML=done?'Heute<br><em>geschafft.</em>':'Gut<br><em>gedacht.</em>';
    $('result-copy').textContent=done?'Deine Tagesrunde ist vollständig. Morgen warten drei neue Rätsel.':'Noch eine Runde? Das nächste Level wartet schon.';
    $('result-score').innerHTML=already?'':('<strong>'+ '★'.repeat(score.stars)+'☆'.repeat(3-score.stars)+'</strong><span>'+score.seconds+' SEKUNDEN'+(puzzle?.type==='sum'?' · '+score.swaps+' ZÜGE':'')+'</span>');
    $('result-detail').textContent=already?'':score.stars===3?'Perfekt gelöst · ohne Fehler, Hinweis oder Neustart':score.stars===2?'Gut gelöst · ein kleiner Umweg':'Geschafft · morgen geht noch mehr';
    $('next-label').textContent=done?'Freies Training':mode==='daily'?'Nächstes Rätsel':'Nächstes Level';show('result');
  }
  function reset(){if(!puzzle||busy)return;run++;stats.resets++;if(puzzle.type==='path'){puzzle.trace=[];renderPath();}else if(puzzle.type==='sum'){puzzle.tiles=puzzle.initial.slice();puzzle.selected=null;puzzle.hintCells=null;stats.swaps=0;renderSum();}else{puzzle.answer=0;puzzle.phase='ready';renderMemory();$('feedback').textContent='Bereit? Starte die Folge.';}}
  function hint(){
    if(!puzzle||busy)return;const p=puzzle;
    if(p.type==='path'){const prefix=p.trace.every((v,i)=>p.route[i]===v);if(!prefix){$('feedback').textContent='Ein Teil deiner Linie führt anders. Gehe zurück und suche einen neuen Weg.';return;}const i=p.route[p.trace.length];if(i===undefined)return;stats.hints++;const c=$('play-area').querySelectorAll('.cell')[i];c.classList.add('hint-cell');setTimeout(()=>c.classList.remove('hint-cell'),1800);$('feedback').textContent='Das markierte Feld ist ein möglicher nächster Schritt.';}
    else if(p.type==='sum'){const i=p.tiles.findIndex((v,x)=>v!==p.solution[x]);if(i<0)return;const j=p.tiles.indexOf(p.solution[i]);stats.hints++;p.hintCells=[i,j];renderSum();setTimeout(()=>{if(puzzle===p){p.hintCells=null;$('play-area').querySelectorAll('.hint-cell').forEach(c=>c.classList.remove('hint-cell'));}},3200);$('feedback').textContent='Tausche die beiden markierten Zahlen.';}
    else if(p.phase==='failed'||p.phase==='ready'){stats.hints++;p.phase='ready';p.answer=0;renderMemory();$('feedback').textContent='Du kannst die Folge erneut ansehen.';}else $('feedback').textContent=p.reverse?'Denke an die letzte Markierung zuerst.':'Denke an die erste Markierung zuerst.';
  }
  $('daily').onclick=startDaily;$('free').onclick=choose;$('choose-back').onclick=home;
  kinds.forEach(k=>{ $('mode-'+k).onclick=()=>startFree(k); });
  $('back').onclick=()=>mode==='free'?choose():home();$('result-home').onclick=home;
  $('result-next').onclick=()=>{if(mode==='daily'&&index>=2)choose();else{index++;load();}};
  $('reset').onclick=reset;$('hint').onclick=hint;
  $('sound').onclick=()=>{muted=!muted;saved.muted=muted;persist();updateSound();};
  updateSound();home();
  if(typeof module!=='undefined')module.exports={hash,random,pathPuzzle,sumPuzzle,memoryPuzzle,neighbors,swapDistance,solvedCount};
})();
