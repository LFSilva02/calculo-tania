// ===== util =====
function $(sel){return document.querySelector(sel)}
function create(tag, cls){const e=document.createElement(tag); if(cls) e.className=cls; return e}
function fmt(v){ if(Number.isNaN(v)) return 'NaN'; const s=Number(v).toPrecision(12); return (''+parseFloat(s)) }

// Tabs
document.querySelectorAll('.tab').forEach(btn=>btn.addEventListener('click', e=>{
  document.querySelectorAll('.tab').forEach(t=>t.classList.remove('active'));
  document.querySelectorAll('.tab-panel').forEach(p=>p.classList.remove('active'));
  btn.classList.add('active');
  document.getElementById(btn.dataset.tab).classList.add('active');
}))

// ===== Bisseção =====
function normalizeExpr(expr){
  let s = expr.trim()
    .replace(/\s+/g,'')
    .toLowerCase()
    .replace(/\^/g,'**')
    .replace(/pi/g, 'Math.PI')
    .replace(/\be(?=[^a-zA-Z0-9_])/g, 'Math.E') // 'e' constante isolada
  // funções
  const funs = ['sin','cos','tan','asin','acos','atan','sqrt','abs','exp','ln','log','log10']
  for(const f of funs){
    if(f==='ln' || f==='log'){ s = s.replace(/\b(ln|log)\s*\(/g, 'Math.log(') }
    else s = s.replace(new RegExp('\\b'+f+'\\s*\\(','g'), 'Math.'+f+'(')
  }
  // multiplicação implícita simples: número seguido de x -> numero*x ; x seguido de número -> x*numero ; ) ( -> )*(
  s = s.replace(/(\d)(x)/g, '$1*$2')
       .replace(/(x)(\d)/g, '$1*$2')
       .replace(/\)\(/g, ')*(')
       .replace(/(x)(Math\.)/g, '$1*$2')
       .replace(/(Math\.[A-Za-z0-9_]+)\s*(x)/g, '$1*$2')
  return s
}
function compileExpr(expr){
  const norm = normalizeExpr(expr)
  try{
    // new Function com Math disponível via 'with'
    /* eslint no-new-func: "off" */
    const f = new Function('x', 'with(Math){ return ('+norm+'); }')
    // teste rápido
    const t = f(0)
    if(Number.isNaN(t)) throw new Error('expressão inválida')
    return f
  }catch(e){
    throw new Error('Expressão inválida: '+e.message)
  }
}
function bissecao(f, a, b, tol=1e-6, maxIt=100){
  const fa = f(a), fb = f(b)
  if(!isFinite(fa) || !isFinite(fb)) throw new Error('f(a) ou f(b) inválido (NaN/Inf)')
  if(fa*fb >= 0) throw new Error('f(a)*f(b) ≥ 0: intervalo não garante raiz única')
  let A=a, B=b, FA=fa, FB=fb, m = 0, fm = 0
  const rows = [['k','a','b','m=(a+b)/2','f(m)']]
  for(let k=1; k<=maxIt; k++){
    m = (A+B)/2; fm = f(m)
    rows.push([k, A, B, m, fm])
    if(Math.abs(fm) < tol || Math.abs(B-A)/2 < tol) break
    if(FA*fm < 0){ B = m; FB = fm } else { A = m; FA = fm }
  }
  return {rows, root:m, froot:fm}
}
$('#btnBissecao').addEventListener('click', ()=>{
  const out = $('#outBissecao'); out.textContent=''
  try{
    const f = compileExpr($('#fx').value)
    const a = parseFloat($('#a').value.replace(',','.'))
    const b = parseFloat($('#b').value.replace(',','.'))
    const tol = parseFloat($('#tol').value.replace(',','.'))
    const maxIt = parseInt($('#maxIt').value,10)
    const {rows, root, froot} = bissecao(f,a,b,tol,maxIt)
    const lines = rows.map(r=>r.map((c,i)=> i===0? (''+c).padStart(3): (typeof c==='number'? fmt(c): c).padStart(16)).join(' '))
    lines.push('', 'Aproximação da raiz: x* = '+fmt(root))
    lines.push('f(x*) = '+fmt(froot))
    out.textContent = lines.join('\n')
  }catch(e){
    out.textContent = 'Erro: '+e.message
  }
})

// ===== Gauss & LU =====
function buildGrid(n){
  const wrap = $('#gridWrap'); wrap.innerHTML=''
  const tip = create('div'); tip.textContent = 'Preencha a matriz A (n×n) e o vetor b (coluna à direita).'
  wrap.appendChild(tip)
  const grid = create('div','matrix-grid')
  grid.style.gridTemplateColumns = `repeat(${n+1}, minmax(80px,1fr))`
  for(let i=0;i<n;i++){
    for(let j=0;j<n;j++){
      const inp = create('input')
      inp.value = '0'
      inp.dataset.i=i; inp.dataset.j=j; inp.classList.add('a')
      grid.appendChild(inp)
    }
    const b = create('input')
    b.value = '0'; b.dataset.i=i; b.classList.add('b')
    b.style.background='#0b1e3a'
    grid.appendChild(b)
  }
  wrap.appendChild(grid)
}
function readA(n){
  const A = Array.from({length:n}, ()=>Array(n).fill(0))
  document.querySelectorAll('input.a').forEach(inp=>{
    const i = parseInt(inp.dataset.i,10), j = parseInt(inp.dataset.j,10)
    A[i][j] = parseFloat(inp.value.replace(',','.'))||0
  })
  return A
}
function readb(n){
  const b = Array(n).fill(0)
  document.querySelectorAll('input.b').forEach(inp=>{
    const i = parseInt(inp.dataset.i,10)
    b[i] = parseFloat(inp.value.replace(',','.'))||0
  })
  return b
}
function copyMat(A){ return A.map(row=>row.slice()) }
function swapRows(A,i,j){ const t=A[i]; A[i]=A[j]; A[j]=t }
function swapVec(v,i,j){ const t=v[i]; v[i]=v[j]; v[j]=t }
function gaussianSolve(Aorig, borig){
  const n = Aorig.length
  if(borig.length!==n) throw new Error('Dimensão de b incompatível com A')
  const A = copyMat(Aorig), b = borig.slice()
  for(let k=0;k<n-1;k++){
    // pivoteamento parcial
    let p=k, max=Math.abs(A[k][k])
    for(let i=k+1;i<n;i++){
      const v = Math.abs(A[i][k]); if(v>max){max=v; p=i}
    }
    if(Math.abs(A[p][k])<1e-15) throw new Error('Matriz singular ou quase singular')
    if(p!==k){ swapRows(A,k,p); swapVec(b,k,p) }
    for(let i=k+1;i<n;i++){
      const m = A[i][k]/A[k][k]
      A[i][k]=0
      for(let j=k+1;j<n;j++) A[i][j]-=m*A[k][j]
      b[i]-=m*b[k]
    }
  }
  if(Math.abs(A[n-1][n-1])<1e-15) throw new Error('Matriz singular ou quase singular')
  const x = Array(n).fill(0)
  for(let i=n-1;i>=0;i--){
    let s=b[i]; for(let j=i+1;j<n;j++) s-=A[i][j]*x[j]
    x[i]=s/A[i][i]
  }
  return x
}
// LU com pivoteamento parcial (Doolittle): retorna {P,L,U}
function luDecomposeWithPivot(Aorig){
  const n=Aorig.length, A=copyMat(Aorig)
  const L=Array.from({length:n},()=>Array(n).fill(0))
  const U=Array.from({length:n},()=>Array(n).fill(0))
  const piv = Array.from({length:n},(_,i)=>i)
  for(let k=0;k<n;k++){
    // pivot
    let p=k, max=Math.abs(A[k][k])
    for(let i=k+1;i<n;i++){ const v=Math.abs(A[i][k]); if(v>max){max=v; p=i} }
    if(Math.abs(A[p][k])<1e-15) throw new Error('Matriz singular ou quase singular (pivot ~ 0)')
    if(p!==k){
      swapRows(A,k,p); const tp=piv[k]; piv[k]=piv[p]; piv[p]=tp
      for(let j=0;j<k;j++){ const t=L[k][j]; L[k][j]=L[p][j]; L[p][j]=t }
    }
    // Doolittle
    for(let j=k;j<n;j++){
      let s=0; for(let t=0;t<k;t++) s+=L[k][t]*U[t][j]
      U[k][j]=A[k][j]-s
    }
    for(let i=k+1;i<n;i++){
      let s=0; for(let t=0;t<k;t++) s+=L[i][t]*U[t][k]
      L[i][k]=(A[i][k]-s)/U[k][k]
    }
    L[k][k]=1
  }
  // P a partir de piv
  const P = Array.from({length:n},()=>Array(n).fill(0))
  for(let i=0;i<n;i++) P[i][piv[i]]=1
  return {P,L,U}
}
function mulMatVec(M,v){ return M.map(row=>row.reduce((s,bij,j)=>s+bij*v[j],0)) }
function solveWithLU(P,L,U,b){
  const n=b.length
  const Pb = mulMatVec(P,b)
  const y = Array(n).fill(0)
  for(let i=0;i<n;i++){
    let s=Pb[i]; for(let j=0;j<i;j++) s-=L[i][j]*y[j]
    y[i]=s/L[i][i] // L[i][i]=1
  }
  const x = Array(n).fill(0)
  for(let i=n-1;i>=0;i--){
    let s=y[i]; for(let j=i+1;j<n;j++) s-=U[i][j]*x[j]
    x[i]=s/U[i][i]
  }
  return x
}
function matToString(M){ return M.map(r=>'[ '+r.map(v=>String(fmt(v)).padStart(12)).join(' ')+' ]').join('\n') }
function vecToString(v){ return '[ '+v.map(fmt).join(', ')+' ]' }

// UI bind
function gerar(){
  const n = Math.max(3, Math.min(10, parseInt($('#n').value,10)||3))
  $('#n').value = n
  buildGrid(n)
}
$('#btnGerar').addEventListener('click', gerar)
// gerar grid inicial
gerar()

$('#btnGauss').addEventListener('click', ()=>{
  const out = $('#outGauss'); out.textContent=''
  try{
    const n = parseInt($('#n').value,10)
    const A = readA(n), b = readb(n)
    const x = gaussianSolve(A,b)
    out.textContent = 'Solução (Gauss) – x:\n'+vecToString(x)+'\n'
  }catch(e){ out.textContent='Erro: '+e.message }
})
$('#btnLU').addEventListener('click', ()=>{
  const out = $('#outGauss'); out.textContent=''
  try{
    const n = parseInt($('#n').value,10)
    const A = readA(n), b = readb(n)
    const {P,L,U} = luDecomposeWithPivot(A)
    let s = 'Matriz de Permutação P:\n'+matToString(P)+'\n\n'
    s += 'L:\n'+matToString(L)+'\n\n'
    s += 'U:\n'+matToString(U)+'\n\n'
    const x = solveWithLU(P,L,U,b)
    s += 'Solução via LU (PA=LU) – x:\n'+vecToString(x)+'\n'
    out.textContent = s
  }catch(e){ out.textContent='Erro: '+e.message }
})
