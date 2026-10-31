// Pet Flip Tracker — Cloudflare Worker (data-serving + page).
//
// This Worker is now intentionally thin: a GitHub Action (see ../scanner) does the
// heavy Hypixel auction-house scan on GitHub's IPs and writes the finished payload to
// KV. This Worker just serves the web page and returns that payload at /api/flips.
// No external API calls, no cron, no rate-limit exposure.

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);

    if (url.pathname === '/api/flips') {
      const payload = (await env.PETFLIPS.get('payload')) || '{"pets":[],"scanned":0,"total":0,"updated":null}';
      // Traffic-healed freshness: CF cron silently stopped firing on 2026-09-05, so any
      // visit that finds the payload stale (>16 min) re-dispatches the scan itself —
      // throttled through KV so a burst of visitors sends one dispatch, not dozens.
      ctx.waitUntil(healIfStale(env, payload));
      return new Response(payload, {
        headers: {
          'content-type': 'application/json',
          'cache-control': 'no-store',
          'access-control-allow-origin': '*',
        },
      });
    }

    return new Response(PAGE_HTML, { headers: { 'content-type': 'text/html; charset=utf-8' } });
  },

  // Reliable scan trigger. GitHub's own cron scheduler delays/drops runs under load
  // (observed firing ~hourly despite a */15 schedule); Cloudflare crons are punctual,
  // so this fires every 15 min and force-dispatches the scan workflow on GitHub.
  // Requires the GH_PAT secret (fine-grained PAT scoped to pet-flip-tracker with
  // Actions read+write): `wrangler secret put GH_PAT`. No-ops until it's set.
  async scheduled(event, env, ctx) {
    if (!env.GH_PAT) return;
    await dispatchScan(env, 'cron');
  },
};

/** Stale payload on a visit: dispatch a scan, at most once per 10 minutes. */
async function healIfStale(env, payload) {
  if (!env.GH_PAT) return;
  let updated = null;
  try {
    updated = JSON.parse(payload).updated;
  } catch (e) { /* unreadable payload: treat as stale */ }
  const age = updated ? Date.now() - Date.parse(updated) : Infinity;
  if (age < 16 * 60 * 1000) return;

  const last = Number(await env.PETFLIPS.get('healDispatchedAt')) || 0;
  if (Date.now() - last < 10 * 60 * 1000) return;
  await env.PETFLIPS.put('healDispatchedAt', String(Date.now()), { expirationTtl: 3600 });
  await dispatchScan(env, 'stale-visit');
}

async function dispatchScan(env, why) {
  const gh = (path, init = {}) => fetch(`https://api.github.com/repos/Rinity9801/pet-flip-tracker/actions/workflows/scan.yml${path}`, {
    ...init,
    headers: {
      authorization: `Bearer ${env.GH_PAT}`,
      accept: 'application/vnd.github+json',
      'user-agent': 'pet-flip-tracker-cron',
      'x-github-api-version': '2022-11-28',
    },
  });
  const dispatch = () => gh('/dispatches', { method: 'POST', body: JSON.stringify({ ref: 'main' }) });

  let r = await dispatch();
  if (r.status !== 204) {
    // GitHub auto-disables scheduled workflows after 60 days without repo commits
    // (state "disabled_inactivity"), which also rejects dispatches — this went stale
    // 2026-09-03 exactly that way. Re-enable and retry, so the trigger heals itself.
    await r.text(); // drain
    const enable = await gh('/enable', { method: 'PUT' });
    const retry = await dispatch();
    if (retry.status !== 204) {
      console.log(`scan dispatch (${why}) failed: HTTP ${retry.status} (enable: ${enable.status}) ${await retry.text()}`);
    } else {
      console.log(`scan workflow was disabled (enable: ${enable.status}); re-enabled and dispatched (${why})`);
    }
  }
}

/******** Web page ********/
const PAGE_HTML = `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Pet Flip Tracker</title>
<style>
  :root { color-scheme: dark; }
  * { box-sizing: border-box; }
  body { margin: 0; font: 14px/1.5 system-ui, sans-serif; background: #14141a; color: #e6e6ee; }
  header { padding: 18px 20px; border-bottom: 1px solid #2a2a36; display: flex; align-items: center; gap: 16px; flex-wrap: wrap; }
  h1 { font-size: 18px; margin: 0; }
  .muted { color: #9aa0ad; font-size: 12px; }
  button { background: #3a7bd5; color: #fff; border: 0; border-radius: 6px; padding: 7px 14px; cursor: pointer; font-weight: 600; }
  button:disabled { opacity: .5; cursor: default; }
  input.search { background: #11141c; border: 1px solid #2a2a36; border-radius: 6px; color: #e6e6ee; padding: 7px 10px; min-width: 180px; }
  main { padding: 20px; max-width: 1150px; margin: 0 auto; }
  .banner { background: #1f3a24; border: 1px solid #2f6b3a; border-radius: 8px; padding: 14px 16px; margin-bottom: 14px; font-size: 15px; }
  .banner a { color: #8fd0ff; }
  .progress { color: #9aa0ad; font-size: 12px; margin-bottom: 14px; }
  table { width: 100%; border-collapse: collapse; }
  th, td { text-align: right; padding: 8px 10px; border-bottom: 1px solid #24242e; white-space: nowrap; }
  th:nth-child(2), td:nth-child(2), th:nth-child(3), td:nth-child(3) { text-align: left; }
  th { color: #9aa0ad; font-weight: 600; font-size: 12px; text-transform: uppercase; letter-spacing: .03em; cursor: pointer; user-select: none; }
  tr.pet { cursor: pointer; }
  tr.pet:hover { background: #1c1c25; }
  tr.best td { background: #18301d; }
  .star { color: #ffd24a; }
  .tier { font-size: 11px; padding: 1px 6px; border-radius: 4px; background: #2a2a36; color: #c7b3ff; }
  select.rarity, select#basis { background: #2a2a36; color: #c7b3ff; border: 1px solid #3a3a48; border-radius: 4px; font-size: 11px; padding: 2px 4px; text-transform: capitalize; cursor: pointer; }
  a { color: #8fd0ff; text-decoration: none; }
  a:hover { text-decoration: underline; }
  .levels { background: #0f0f14; }
  .levels table { font-size: 13px; }
  .levels td, .levels th { border-bottom: 1px solid #1c1c24; }
  .hidden { display: none; }
  .pos { color: #6fcf7f; } .neg { color: #e07a7a; }
  .err { color: #e07a7a; }
  .note { color: #b9bfcc; font-size: 12px; padding: 6px 0 8px; white-space: normal; text-align: left; }
  .kat { font-size: 11px; color: #e0b46a; }
</style>
</head>
<body>
<header>
  <h1>🐾 Pet Flip Tracker</h1>
  <span class="muted" id="updated"></span>
  <input class="search" id="search" placeholder="filter pets…">
  <button id="refresh">Refresh</button>
  <label class="muted" title="Skip Kat's wait with Kat Flowers (1 day) / Bouquets (5 days) and count their cost"><input type="checkbox" id="rush"> Rush Kat with flowers</label>
  <label class="muted" title="Coins per Mining XP you earn, not per pet XP: mining pets (⛏) level much faster than the rest. Wisps only level by feeding, so they're hidden here.">Grinding by <select id="basis">
    <option value="equipped">mining, pet equipped</option>
    <option value="share">mining, pet share</option>
    <option value="pet">raw pet XP</option>
  </select></label>
  <span class="muted">All pets · best buy level &amp; resell, by coins per XP (after AH tax). Includes Kat rarity upgrades and Golden Dragons made at Lvl 1.</span>
</header>
<main>
  <div id="banner" class="banner">Loading…</div>
  <div id="progress" class="progress"></div>
  <table id="summary"><thead><tr>
    <th data-k="rank">#</th><th data-k="name">Pet</th><th data-k="tier">Rarity</th>
    <th data-k="listings">Listings</th><th data-k="buy">Buy Lvl</th><th data-k="price">Buy Price</th>
    <th data-k="sell">Sell Lvl</th><th data-k="net">Net After Tax</th><th data-k="xp">XP to Grind</th>
    <th data-k="profit">Profit</th><th data-k="perxp">Coins/XP</th><th data-k="liq" title="Number sold in the last 7 days at the sell (cap) level">Sell Sold/7d</th><th>Buy</th>
  </tr></thead><tbody id="rows"></tbody></table>
</main>
<script>
let DATA = null, sortKey = 'perxp', sortDir = -1, filter = '';
let rush = false, basis = 'equipped';
try { rush = localStorage.getItem('rushKat') === '1'; basis = localStorage.getItem('xpBasis') || basis; } catch (e) {}
// Pet XP earned per 1 Mining XP, measured in-game (equipped, and levelled through pet share).
const BASES = {
  equipped: { mining: 99769 / 47272.5, other: 0.7298 },
  share: { mining: 39 / 131.6, other: 0.1644 },
};
const xpName = () => BASES[basis] ? 'Mining XP' : 'XP';
const minesFast = p => p.skill === 'MINING' || p.skill === 'ALL';
// Pet XP per XP on the chosen basis: 1 for raw pet XP, 0 for wisps (they only level by feeding).
function xpRate(p) {
  const b = BASES[basis];
  if (!b) return 1;
  if (p.skill === 'GABAGOOL') return 0;
  return minesFast(p) ? b.mining : b.other;
}
const picked = {}; // tag -> chosen rarity (overrides the pet's default)
const fmt0 = n => n == null ? '' : Math.round(n).toLocaleString();
const fmt2 = n => n == null ? '' : Number(n).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const compact = n => n >= 1e9 ? (n/1e9).toFixed(2)+'B' : n >= 1e6 ? (n/1e6).toFixed(2)+'M' : n >= 1e3 ? (n/1e3).toFixed(1)+'k' : ''+n;
const auc = (uuid, text) => uuid ? '<a href="https://sky.coflnet.com/auction/'+uuid+'" target="_blank" rel="noopener">'+text+'</a>' : '';
// 7-day sold count, color-coded for liquidity: red=thin (hard to buy/sell), green=plenty.
const liqCell = n => { if (n == null) return '<span class="muted">–</span>'; const c = n === 0 ? 'neg' : n < 5 ? '' : 'pos'; return '<span class="'+c+'">'+n+'</span>'; };

// The rarities map + the currently-shown flip for a pet (defaults to its best rarity).
const raritiesOf = p => p.rarities || { [p.tier]: { tier: p.tier, sell: p.sell, sellNet: p.sellNet, best: p.best, levels: p.levels, perXp: p.perXp } };
const activeTier = p => picked[p.tag] && raritiesOf(p)[picked[p.tag]] ? picked[p.tag] : (p.defaultTier || p.tier);
// With "Rush Kat" on, a Kat option pays for flowers/bouquets too: every buy row's profit drops by
// the same amount, so coins/XP and even the best buy level are recomputed here.
function eff(v) {
  const rc = rush && v && v.upgrade && v.upgrade.rush ? v.upgrade.rush.cost : 0;
  if (!rc) return v;
  const levels = v.levels.map(l => Object.assign({}, l, { profit: l.profit - rc, perXp: l.xp > 0 ? (l.profit - rc) / l.xp : null }));
  let best = null;
  for (const l of levels) if (l.perXp !== null && (best === null || l.perXp > best.perXp)) best = l;
  if (!best) return v;
  return Object.assign({}, v, { levels, best, perXp: best.perXp, rushed: true });
}
// Restates a flip per Mining XP: the same coins, over the Mining XP that earns that much pet XP.
function rebase(v, r) {
  if (r === 1) return v;
  const conv = l => Object.assign({}, l, { xp: l.xp / r, perXp: l.perXp === null ? null : l.perXp * r });
  return Object.assign({}, v, { levels: v.levels.map(conv), best: conv(v.best), perXp: v.perXp * r });
}
const view = p => rebase(eff(raritiesOf(p)[activeTier(p)]), xpRate(p));
const petName = p => p.name + (minesFast(p) ? ' <span class="kat">⛏</span>' : '');
// Option keys can be "EPIC", "EPIC>LEGENDARY" (Kat upgrade) or "LEGENDARY@100"; the scanner sends a label for the composite ones.
const optLabel = (p, t) => (raritiesOf(p)[t] && raritiesOf(p)[t].label) || (t || '').toLowerCase();
const buyLink = (row, text) => row.crafted ? '<span class="kat">⚒ craft</span>' : auc(row.uuid, text);
const hrs = h => h >= 48 ? (h / 24).toFixed(h % 24 ? 1 : 0) + 'd' : h + 'h';
// Kat upgrade / Lvl 1 craft cost, spelled out under the level table.
function costNote(v) {
  const c = v.upgrade || v.craft;
  if (!c) return '';
  const mats = (c.materials || []).map(m => m.amount + '× ' + m.id.toLowerCase().replace(/_/g, ' ') + ' (' + compact(m.unit * m.amount) + ')');
  const parts = [];
  if (c.coins) parts.push(compact(c.coins) + ' coins');
  parts.push(...mats);
  let s = (v.upgrade ? 'Kat upgrade ' + v.upgrade.from.toLowerCase() + ' → ' + v.upgrade.to.toLowerCase() + ': '
    : 'Golden Dragon made at Lvl 1: ') + parts.join(' + ') + ' = <b>' + compact(c.total) + '</b>';
  if (v.upgrade && v.upgrade.hours) s += ' · takes ' + hrs(v.upgrade.hours) + (v.upgrade.steps > 1 ? ' over ' + v.upgrade.steps + ' upgrades' : '');
  const r = v.upgrade && v.upgrade.rush;
  if (r && r.days) {
    const items = [];
    if (r.bouquets) items.push(r.bouquets + '× Kat Bouquet');
    if (r.flowers) items.push(r.flowers + '× Kat Flower');
    s += '<br>Skip the ' + r.days + '-day wait: ' + items.join(' + ') + ' = <b>' + compact(r.cost) + '</b>'
      + (v.rushed ? ' <span class="kat">(included)</span>' : ' <span class="muted">(tick "Rush Kat" to include)</span>');
  }
  if (c.unpriced && c.unpriced.length) s += ' · not priced (counted as 0): ' + c.unpriced.map(x => x.toLowerCase().replace(/_/g, ' ')).join(', ');
  if (v.upgrade) s += ' · profit includes the upgrade cost';
  return '<div class="note">' + s + '</div>';
}

async function load() {
  const btn = document.getElementById('refresh');
  const orig = btn.textContent;
  btn.disabled = true; btn.textContent = 'Refreshing…';
  try {
    // Unique URL each time so we always read the current KV, never a cached copy.
    const res = await fetch('/api/flips?t=' + Date.now(), { cache: 'no-store' });
    DATA = await res.json();
    render();
  } catch (e) {
    document.getElementById('banner').innerHTML = '<span class="err">Failed to load: ' + e + '</span>';
  }
  btn.textContent = orig; btn.disabled = false;
}

function sortedPets() {
  let pets = DATA.pets.filter(p => xpRate(p) > 0);
  if (filter) pets = pets.filter(p => p.name.toLowerCase().includes(filter));
  pets.sort((a, b) => {
    const get = p => { const v = view(p); return ({ rank: v.perXp, name: p.name, tier: activeTier(p), listings: p.listings,
      buy: v.best.level, price: v.best.price, sell: v.sell.level, net: v.sellNet, xp: v.best.xp,
      profit: v.best.profit, perxp: v.perXp, liq: (v.sell.soldWeek ?? -1) }[sortKey]); };
    const av = get(a), bv = get(b);
    if (typeof av === 'string') return sortDir * av.localeCompare(bv);
    return sortDir * (av - bv);
  });
  return pets;
}

// The cells of a pet's main row (everything after the rarity selector cell).
function rowCells(p, i) {
  const v = view(p);
  return '<td>' + p.listings + '</td>' +
    '<td>' + v.best.level + '</td><td>' + fmt0(v.best.price) + (v.upgrade ? '<br><span class="kat">+ ' + compact(v.upgrade.total) + ' Kat</span>' : '') + '</td>' +
    '<td>' + v.sell.level + '</td><td>' + fmt0(v.sellNet) + '</td>' +
    '<td>' + fmt0(v.best.xp) + '</td>' +
    '<td class="' + (v.best.profit >= 0 ? 'pos' : 'neg') + '">' + fmt0(v.best.profit) + '</td>' +
    '<td><b>' + fmt2(v.perXp) + '</b></td>' +
    '<td>' + liqCell(v.sell.soldWeek) + '</td>' +
    '<td>' + buyLink(v.best, 'buy ↗') + '</td>';
}

function raritySelect(p) {
  const tiers = Object.keys(raritiesOf(p));
  if (tiers.length <= 1) return '<span class="tier">' + optLabel(p, activeTier(p)) + '</span>';
  const cur = activeTier(p);
  const opts = tiers.sort((a, b) => eff(raritiesOf(p)[b]).perXp - eff(raritiesOf(p)[a]).perXp)
    .map(t => '<option value="' + t + '"' + (t === cur ? ' selected' : '') + '>' + optLabel(p, t) + '</option>').join('');
  return '<select class="rarity" data-tag="' + p.tag + '">' + opts + '</select>';
}

function detailHtml(p) {
  const v = view(p);
  let inner = '<td></td><td colspan="12">' + costNote(v) + '<table><thead><tr>' +
    '<th>Buy Lvl</th><th>Cheapest</th><th>' + xpName() + ' to Grind</th><th>Profit</th><th>Coins/' + xpName() + '</th><th>Sold/7d</th><th></th></tr></thead><tbody>';
  v.levels.forEach(l => {
    inner += '<tr>' +
      '<td>' + (l === v.best || (l.level === v.best.level && !!l.crafted === !!v.best.crafted) ? '<span class="star">★</span> ' : '') + l.level + (l.crafted ? ' <span class="kat">made</span>' : '') + '</td>' +
      '<td>' + fmt0(l.price) + '</td><td>' + fmt0(l.xp) + '</td>' +
      '<td class="' + (l.profit >= 0 ? 'pos' : 'neg') + '">' + fmt0(l.profit) + '</td>' +
      '<td>' + fmt2(l.perXp) + '</td>' +
      '<td>' + liqCell(l.soldWeek) + '</td>' +
      '<td>' + buyLink(l, 'open ↗') + '</td></tr>';
  });
  return inner + '</tbody></table></td>';
}

function render() {
  document.getElementById('updated').textContent = 'updated ' + new Date(DATA.updated).toLocaleTimeString();
  document.getElementById('progress').textContent =
    'Scanned ' + DATA.scanned + ' / ' + DATA.total + ' pets'
    + (DATA.lastFullSweep ? ' · last full sweep ' + new Date(DATA.lastFullSweep).toLocaleTimeString() : ' · first sweep in progress…');
  document.querySelector('th[data-k="xp"]').textContent = xpName() + ' to Grind';
  document.querySelector('th[data-k="perxp"]').textContent = 'Coins/' + xpName();

  const list = sortedPets();
  const top = list[0];
  const banner = document.getElementById('banner');
  if (DATA.pets.length === 0) {
    banner.innerHTML = '<span class="muted">No flips computed yet — the first scan is still running, check back in a minute.</span>';
  } else if (top) {
    const v = view(top);
    const how = v.best.crafted ? '<b>Make ' + top.name + '</b> at Lvl 1 for ' + compact(v.best.price)
      : '<b>Buy ' + top.name + '</b> (' + optLabel(top, activeTier(top)) + ') at Lvl ' + v.best.level + ' for ' + compact(v.best.price)
        + (v.upgrade ? ' + ' + compact(v.upgrade.total) + ' Kat upgrade' : '');
    banner.innerHTML = '<span class="star">★</span> ' + how + ' → grind to Lvl ' + v.sell.level +
      ' = <b>' + fmt2(v.perXp) + ' coins/' + xpName() + '</b> &nbsp; ' + buyLink(v.best, 'open auction ↗');
  }

  const rows = document.getElementById('rows');
  rows.innerHTML = '';
  list.forEach((p, i) => {
    const tr = document.createElement('tr');
    tr.className = 'pet' + (i === 0 ? ' best' : '');
    tr.dataset.tag = p.tag;
    tr.innerHTML =
      '<td>' + (i === 0 ? '<span class="star">★</span>1' : i + 1) + '</td>' +
      '<td>' + petName(p) + '</td>' +
      '<td>' + raritySelect(p) + '</td>' +
      rowCells(p, i);
    rows.appendChild(tr);

    const det = document.createElement('tr');
    det.className = 'levels hidden';
    det.dataset.det = p.tag;
    det.innerHTML = detailHtml(p);
    rows.appendChild(det);

    // Clicking the row toggles the breakdown — but not when using the rarity dropdown.
    tr.addEventListener('click', e => { if (!e.target.closest('select')) det.classList.toggle('hidden'); });
  });
}

// Rarity change (delegated once, so it survives in-place row rebuilds): update just
// that pet's row + breakdown to the chosen rarity. No re-sort, so the row doesn't jump.
document.getElementById('rows').addEventListener('change', e => {
  if (!e.target.matches('select.rarity')) return;
  const tag = e.target.dataset.tag;
  picked[tag] = e.target.value;
  const p = DATA.pets.find(x => x.tag === tag);
  const rows = document.getElementById('rows');
  const tr = rows.querySelector('tr.pet[data-tag="' + tag + '"]');
  const det = rows.querySelector('tr[data-det="' + tag + '"]');
  const i = [...rows.querySelectorAll('tr.pet')].indexOf(tr);
  tr.innerHTML = '<td>' + (i === 0 ? '<span class="star">★</span>1' : i + 1) + '</td><td>' + petName(p) + '</td><td>' + raritySelect(p) + '</td>' + rowCells(p, i);
  det.innerHTML = detailHtml(p);
});

document.querySelectorAll('th[data-k]').forEach(th => th.addEventListener('click', () => {
  const k = th.dataset.k;
  if (sortKey === k) sortDir *= -1; else { sortKey = k; sortDir = (k === 'name' || k === 'tier') ? 1 : -1; }
  render();
}));
document.getElementById('search').addEventListener('input', e => { filter = e.target.value.toLowerCase(); render(); });
document.getElementById('refresh').addEventListener('click', load);
const rushBox = document.getElementById('rush');
rushBox.checked = rush;
rushBox.addEventListener('change', () => {
  rush = rushBox.checked;
  try { localStorage.setItem('rushKat', rush ? '1' : '0'); } catch (e) {}
  if (DATA) render();
});
const basisBox = document.getElementById('basis');
basisBox.value = basis;
basisBox.addEventListener('change', () => {
  basis = basisBox.value;
  try { localStorage.setItem('xpBasis', basis); } catch (e) {}
  if (DATA) render();
});
load();
setInterval(load, 60000);
</script>
</body>
</html>`;
