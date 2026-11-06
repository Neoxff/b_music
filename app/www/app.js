// Données de démonstration (fichiers locaux simulés)
const TRACKS = [
  {t:"Akwaba",a:"Akossiwa",d:214},{t:"Horizon",a:"Akossiwa",d:187},
  {t:"Sankofa",a:"Akossiwa",d:243},{t:"Nocturne nº7",a:"Yao Doe",d:301},
  {t:"Kpalimé",a:"Akossiwa",d:172},{t:"Basse époque",a:"Groupe Anya",d:226}
];
const ALBUMS = [
  {t:"Minuit à Lomé",s:"Akossiwa",g:"linear-gradient(140deg,#7b4b1e,#e8a33d)",c:"M"},
  {t:"Pluie sur Adakpamé",s:"Yao Doe",g:"linear-gradient(140deg,#1e3a5f,#4cc2ff)",c:"P"},
  {t:"Racines",s:"Groupe Anya",g:"linear-gradient(140deg,#3f1d4e,#b06cf0)",c:"R"},
  {t:"Savane",s:"Akossiwa",g:"linear-gradient(140deg,#143a2a,#3ddc84)",c:"S"},
  {t:"Ferraille",s:"Groupe Anya",g:"linear-gradient(140deg,#4a4a4a,#c0c0c0)",c:"F"},
  {t:"Marée basse",s:"Yao Doe",g:"linear-gradient(140deg,#5f1e2e,#f0625f)",c:"M"}
];
const PLAYLISTS = ["Matin calme","Route Lomé–Kara","Concentration","Soirée palmier","Sport"];
const fmt = s => Math.floor(s/60)+":"+String(Math.floor(s%60)).padStart(2,"0");
const $ = s => document.querySelector(s), $$ = s => [...document.querySelectorAll(s)];

// Sidebar playlists
$("#playlistList").innerHTML = PLAYLISTS.map(p=>`<a href="#">${p}</a>`).join("");

// Grille d'accueil
$("#homeGrid").innerHTML = ALBUMS.map(al=>`
  <button class="card" data-goto-album aria-label="Lire l'album ${al.t}">
    <div class="cover" style="background:${al.g}">${al.c}<span class="play-fab" aria-hidden="true"><svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor"><path d="M8 5v14l11-7z"/></svg></span></div>
    <h3>${al.t}</h3><p>Album · ${al.s}</p>
  </button>`).join("");

// Lignes de piste
const row = (tr,i,current) => `
  <div class="track" role="listitem" aria-current="${current?'true':'false'}">
    <span>${current
      ? '<svg width="16" height="16" viewBox="0 0 24 24" fill="var(--accent)" aria-label="En écoute"><rect x="4" y="8" width="3" height="9" rx="1"/><rect x="10.5" y="4" width="3" height="17" rx="1"/><rect x="17" y="10" width="3" height="6" rx="1"/></svg>'
      : i+1}</span>
    <span><div class="t-title">${tr.t}</div><div class="t-artist">${tr.a}</div></span>
    <span class="t-time">${fmt(tr.d)}</span>
    <button aria-label="Ajouter ${tr.t} à la file" data-add-queue="${tr.t}">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
    </button>
  </div>`;
$("#albumTracks").innerHTML = TRACKS.map((t,i)=>row(t,i,i===0)).join("");

// Recherche locale
const renderSearch = q => {
  q = q.trim().toLowerCase();
  const hits = q ? TRACKS.filter(t=>(t.t+" "+t.a).toLowerCase().includes(q)) : TRACKS;
  $("#searchResults").innerHTML = hits.map((t,i)=>row(t,i,false)).join("");
  $("#searchEmpty").hidden = hits.length !== 0;
  $("#clearSearch").hidden = !q;
};
renderSearch("");
$("#searchInput").addEventListener("input", e=>renderSearch(e.target.value));
$("#clearSearch").addEventListener("click", ()=>{ $("#searchInput").value=""; renderSearch(""); $("#searchInput").focus(); });

// File d'attente
let queue = TRACKS.slice(1,5).map(t=>({...t}));
const renderQueue = () => {
  const total = queue.reduce((s,t)=>s+t.d,0);
  $("#queueSummary").textContent = `${queue.length} titre(s) · ${fmt(total)}`;
  $("#queueNow").innerHTML = `<h2 class="side-label" style="padding:0;margin-bottom:var(--space-2)">En écoute</h2>` + row(TRACKS[0],0,true);
  $("#queueList").innerHTML = queue.map((t,i)=>`
    <div class="queue-item">
      <span class="t-time">${i+1}</span>
      <span><div class="t-title">${t.t}</div><div class="t-artist">${t.a}</div></span>
      <span class="t-time">${fmt(t.d)}</span>
      <span>
        <button aria-label="Monter ${t.t}" data-up="${i}"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="18 15 12 9 6 15"/></svg></button>
        <button aria-label="Retirer ${t.t} de la file" data-del="${i}"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg></button>
      </span>
    </div>`).join("");
};
renderQueue();

// Navigation entre écrans
const go = name => {
  $$(".screen").forEach(s=>s.classList.toggle("visible", s.id==="screen-"+name));
  $$(".nav-item[data-screen]").forEach(n=>{
    const on = n.dataset.screen===name;
    n.classList.toggle("active", on);
    if(on) n.setAttribute("aria-current","page"); else n.removeAttribute("aria-current");
  });
  $("#main").scrollTop = 0;
};
$$("[data-screen]").forEach(el=>el.addEventListener("click",e=>{e.preventDefault();go(el.dataset.screen);}));
$("#homeGrid").addEventListener("click",e=>{ if(e.target.closest("[data-goto-album]")) go("album"); });

// Lecture simulée
let playing=true, cur=72, dur=TRACKS[0].d;
const playBtn=$("#playPauseBtn");
const setPlayIcon=()=>{ $("#iconPause").hidden=!playing; $("#iconPlay").hidden=playing; playBtn.setAttribute("aria-pressed",String(playing)); };
playBtn.addEventListener("click",()=>{playing=!playing;setPlayIcon();});
$("#albumPlayBtn").addEventListener("click",()=>{playing=true;setPlayIcon();});
const seek=$("#seekBar");
const renderSeek=()=>{ const pct=cur/dur*100; $("#seekFill").style.width=pct+"%"; $("#seekKnob").style.left=pct+"%"; $("#timeCur").textContent=fmt(cur); $("#timeDur").textContent=fmt(dur); seek.setAttribute("aria-valuenow",String(Math.floor(cur))); seek.setAttribute("aria-valuetext",fmt(cur)+" sur "+fmt(dur)); };
const vol=$("#volBar");
const renderVol=()=>{ const v=+vol.getAttribute("aria-valuenow"); $("#volFill").style.width=v+"%"; $("#volKnob").style.left=v+"%"; };
vol.addEventListener("click",e=>{ const r=vol.getBoundingClientRect(); vol.setAttribute("aria-valuenow",String(Math.round((e.clientX-r.left)/r.width*100))); renderVol(); });
vol.addEventListener("keydown",e=>{ let v=+vol.getAttribute("aria-valuenow"); if(e.key==="ArrowRight"||e.key==="ArrowUp")v=Math.min(100,v+5); else if(e.key==="ArrowLeft"||e.key==="ArrowDown")v=Math.max(0,v-5); else return; e.preventDefault(); vol.setAttribute("aria-valuenow",String(v)); renderVol(); });
seek.addEventListener("click",e=>{ const r=seek.getBoundingClientRect(); cur=Math.round((e.clientX-r.left)/r.width*dur); renderSeek(); });
seek.addEventListener("keydown",e=>{ if(e.key==="ArrowRight")cur=Math.min(dur,cur+5); else if(e.key==="ArrowLeft")cur=Math.max(0,cur-5); else return; e.preventDefault(); renderSeek(); });
setInterval(()=>{ if(playing){ cur=cur>=dur?0:cur+1; renderSeek(); } },1000);
setPlayIcon(); renderSeek();

// Toggles, ajout/retrait file
$("#shuffleBtn").addEventListener("click",e=>{const b=e.currentTarget;b.setAttribute("aria-pressed",b.getAttribute("aria-pressed")!=="true");});
$("#repeatBtn").addEventListener("click",e=>{const b=e.currentTarget;b.setAttribute("aria-pressed",b.getAttribute("aria-pressed")!=="true");});
["#likeBtn","#likeAlbumBtn"].forEach(s=>$(s).addEventListener("click",e=>{const b=e.currentTarget;b.setAttribute("aria-pressed",b.getAttribute("aria-pressed")!=="true");}));
document.addEventListener("click",e=>{
  const add=e.target.closest("[data-add-queue]");
  if(add){ const tr=TRACKS.find(t=>t.t===add.dataset.addQueue); if(tr){queue.push({...tr});renderQueue();} return; }
  const up=e.target.closest("[data-up]");
  if(up){ const i=+up.dataset.up; if(i>0){[queue[i-1],queue[i]]=[queue[i],queue[i-1]];renderQueue();} return; }
  const del=e.target.closest("[data-del]");
  if(del){ queue.splice(+del.dataset.del,1); renderQueue(); }
});

// Chips : bascule aria-pressed dans le même groupe
$$(".chips").forEach(group=>group.addEventListener("click",e=>{
  const chip=e.target.closest(".chip"); if(!chip)return;
  group.querySelectorAll(".chip").forEach(c=>c.setAttribute("aria-pressed",String(c===chip)));
}));
