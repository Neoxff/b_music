#!/usr/bin/env python3
"""
Audit des bindings Koin — B Music.

Detecte tout type que Koin doit resoudre mais qui n'a AUCUN binding :
chaque cas est un `NoDefinitionFoundException` au premier usage => CRASH.

Les types sont collectes par trois chemins :
  1. `get<T>()` / `by inject()` / `viewModel<T>()` explicites ;
  2. les PARAMETRES DE CONSTRUCTEUR des classes liees par Koin
     (`viewModel { DownloadViewModel(engine = get(), prefs = get()) }`)
     -> c'est ce chemin qui a laisse passer `Preferences` ;
  3. les methodes DAO (`factory { get<DB>().fooDao() }`).

Patterns de binding reconnus :
  - `single<Foo> { ... }` / `factory<Foo> { ... }`   -> Foo (explicite)
  - `single { Foo(...) }`                             -> Foo (constructeur)
  - `single { ... } bind Foo::class`                  -> Foo (bind)
  - `single { get<DB>().fooDao() }`                   -> FooDao (methode DAO)
  - `single { androidContext().contentResolver }`     -> ContentResolver (propriete)

Usage : python3 scripts/check-koin-bindings.py [racine_sources]
Sortie : 0 = aucun type non resolu ; 1 = binding(s) manquant(s).
"""
import re
import sys
from pathlib import Path

ROOT = Path(sys.argv[1] if len(sys.argv) > 1 else "app/src/main/java")
if not ROOT.exists():
    sys.exit(f"racine introuvable : {ROOT}")
FILES = sorted(ROOT.rglob("*.kt"))

DAO_METHODS = {
    "playlistDao", "downloadDao", "playCountDao", "historyDao", "queueDao",
    "inclExclDao", "searchHistoryDao", "customPlaylistDao", "albumDao",
    "artistDao", "trackDao", "folderDao", "lyricsDao", "autoEqDao",
}
PROP_ALIASES = {"contentResolver": "ContentResolver", "sharedPreferences": "SharedPreferences"}


def balanced(text, open_idx, o, c):
    """Retourne le contenu entre parenthèses/accolades, index de l'ouverture inclus."""
    depth, start = 0, open_idx
    for j in range(open_idx, min(len(text), open_idx + 8000)):
        if text[j] == o:
            depth += 1
            if depth == 1:
                start = j + 1
        elif text[j] == c:
            depth -= 1
            if depth == 0:
                return text[start:j]
    return ""


def split_top_level(text):
    out, depth, cur = [], 0, ""
    for ch in text:
        if ch in "([{<":
            depth += 1
        elif ch in ")]}>":
            depth -= 1
        if ch == "," and depth == 0:
            out.append(cur)
            cur = ""
        else:
            cur += ch
    if cur.strip():
        out.append(cur)
    return out


# --- 0. Signatures : classe -> {nom_param: type} ---
class_re = re.compile(r"\b(?:data\s+|open\s+|abstract\s+|sealed\s+)*class\s+([A-Z][A-Za-z0-9_]*)\s*[\(<]")
signatures = {}
for f in FILES:
    txt = f.read_text(encoding="utf-8", errors="replace")
    for m in class_re.finditer(txt):
        name = m.group(1)
        if txt[m.end() - 1] == "<":
            gt = txt.find(">", m.end())
            if gt < 0:
                continue
            args = balanced(txt, gt, "<", ">")
        else:
            args = balanced(txt, m.end() - 1, "(", ")")
        params = {}
        for p in split_top_level(args):
            p = p.strip()
            p = re.sub(r"^(?:(?:private|public|protected|internal|override|open|abstract|lateinit|const|final)\s+)*(?:val|var)\s+", "", p)
            pm = re.match(r"([A-Za-z0-9_]+)\s*:\s*([^=,\n]+)", p)
            if pm:
                t = pm.group(2).strip()
                t = re.sub(r"<.*>", "", t).strip()  # retire les generics
                params[pm.group(1)] = t.rsplit(".", 1)[-1]
        if params:
            signatures.setdefault(name, {}).update(params)

# --- 0bis. Fonctions top-level : nom -> type de retour (les factories Koin
#         `factory { jsonHttpClient(...) }` lie le TYPE DE RETOUR) ---
fun_re_colon = re.compile(
    r"\bfun\s+([a-z][A-Za-z0-9_]*)\s*(?:<[^>]*>)?\s*\(([^)]*)\)\s*:\s*([A-Za-z0-9_.<>?,\s]+?)\s*[{=]",
    re.M,
)
fun_re_eq = re.compile(
    r"\bfun\s+([a-z][A-Za-z0-9_]*)\s*(?:<[^>]*>)?\s*\(([^)]*)\)\s*=\s*([A-Z][A-Za-z0-9_]*)",
    re.M,
)
funcs, func_params = {}, {}
for f in FILES:
    txt = f.read_text(encoding="utf-8", errors="replace")
    parsed = []
    for m in fun_re_colon.finditer(txt):
        parsed.append((m.group(1), m.group(2), m.group(3).strip().rstrip("?").replace(" ", "").split(".")[-1]))
    for m in fun_re_eq.finditer(txt):
        parsed.append((m.group(1), m.group(2), m.group(3)))
    for fname, rawparams, rt in parsed:
        funcs.setdefault(fname, rt)
        params = {}
        for p in split_top_level(rawparams):
            p = p.strip()
            p = re.sub(r"^(?:(?:private|public|protected|internal|override|open|abstract|lateinit|const|final)\s+)*(?:val|var)\s+", "", p)
            pm2 = re.match(r"([A-Za-z0-9_]+)\s*:\s*([^=,\n]+)", p)
            if pm2:
                t = re.sub(r"<.*>", "", pm2.group(2).strip()).strip()
                params[pm2.group(1)] = t.rsplit(".", 1)[-1]
        if params:
            func_params.setdefault(fname, {}).update(params)

# --- 1. Bindings declares ---
bind_re = re.compile(
    r"\b(single|factory|viewModel|scoped)\s*(?:<\s*([A-Za-z0-9_.]+)\s*>)?\s*\{", re.M
)
bound, bound_where = set(), {}
ctor_calls = []  # (classe, args_nommes)


def register(name, where, how):
    if name:
        bound.add(name)
        bound_where.setdefault(name, (where, how))


for f in FILES:
    txt = f.read_text(encoding="utf-8", errors="replace")
    for m in re.finditer(r"\}\s*bind\s+([A-Za-z0-9_]+)\s*::\s*class", txt):
        register(m.group(1), f, "bind")

    for m in bind_re.finditer(txt):
        tparam = m.group(2)
        if tparam:
            register(tparam.rsplit(".", 1)[-1], f, "explicite")
            continue
        body = balanced(txt, m.end() - 1, "{", "}")
        found = None
        cm2 = re.search(r"([A-Z][A-Za-z0-9_]*)\s*::\s*class", body)
        if cm2:
            found = cm2.group(1)
        if not found and "getDefaultSharedPreferences" in body:
            found = "SharedPreferences"
        pm = re.search(r"\.\s*([a-z][A-Za-z0-9_]*)\s*$", body.strip())
        if not found and pm and pm.group(1) in PROP_ALIASES:
            found = PROP_ALIASES[pm.group(1)]
        # binding d'un simple identifiant : `single { Preferences }`
        if not found:
            idm = re.fullmatch(r"([A-Z][A-Za-z0-9_]*)", body.strip())
            if idm:
                found = idm.group(1)
        dm = re.search(r"\.\s*([a-z][A-Za-z0-9_]*)\s*\(\s*\)", body)
        if not found and dm and dm.group(1) in DAO_METHODS:
            found = dm.group(1)[0].upper() + dm.group(1)[1:]
        cm = re.search(r"(?:^|[^\w.])((?:[A-Za-z0-9_]+\.)*)([A-Z][A-Za-z0-9_]*)\s*\(", body)
        if not found and cm:
            found = cm.group(2)
            # memorise les arguments nommes pour tracer les dependances
            paren = cm.start(2) + len(cm.group(2))
            args = balanced(body, body.index("(", paren), "(", ")")
            named = {a.split("=")[0].strip(): a.split("=", 1)[1].strip()
                     for a in split_top_level(args) if "=" in a}
            ctor_calls.append((found, named, f))
        # factory par fonction : `factory { jsonHttpClient(...) }` lie le type de RETOUR
        if not found:
            fm = re.search(r"(?:^|[^\w.])([a-z][A-Za-z0-9_]*)\s*\(", body)
            if fm and fm.group(1) in funcs:
                found = funcs[fm.group(1)]
                args = balanced(body, body.index("(", fm.end() - 1), "(", ")")
                named = {a.split("=")[0].strip(): a.split("=", 1)[1].strip()
                         for a in split_top_level(args) if "=" in a}
                ctor_calls.append((fm.group(1), named, f))
        if not found:
            gm = re.search(r"\bget\s*<\s*([A-Za-z0-9_.]+)\s*>\s*\(\s*\)", body)
            if gm:
                found = gm.group(1).rsplit(".", 1)[-1]
        register(found, f, "infere")

# --- 2. Types resolus par Koin ---
used = {}


def use(t, where):
    if t:
        used.setdefault(t.rsplit(".", 1)[-1], []).append(where)


use_res = [
    re.compile(r"\bget\s*<\s*([A-Za-z0-9_.]+)\s*>\s*\("),
    re.compile(r"\bby\s+inject\s*(?:<\s*([A-Za-z0-9_.]+)\s*>)?\s*\(\s*\)"),
    re.compile(r"\binject\s*<\s*([A-Za-z0-9_.]+)\s*>\s*\(\s*\)"),
    re.compile(r"\bviewModel\s*<\s*([A-Za-z0-9_.]+)\s*>\s*\("),
]
for f in FILES:
    for idx, line in enumerate(f.read_text(encoding="utf-8", errors="replace").splitlines(), 1):
        for rx in use_res:
            for mm in rx.finditer(line):
                use(mm.group(1), (f, idx, line.strip()))

# --- 3. Dependances par constructeur des classes liees ---
for cls, named, f in ctor_calls:
    sig = signatures.get(cls) or func_params.get(cls, {})
    for pname, pval in named.items():
        if pval.strip() != "get()":
            continue  # valeur explicite : pas une resolution Koin
        ptype = sig.get(pname)
        if ptype:
            use(ptype, (f, 0, f"{cls}({pname} = get()) -> {ptype}"))

IMPLICIT_OK = {
    "Context", "Application", "Activity", "Fragment", "CoroutineScope",
    "ViewModel", "SavedStateHandle", "String", "Int", "Boolean", "Long", "Float",
    "Double", "Char", "Byte", "Short",
}

missing = {t: occ for t, occ in used.items() if t not in bound and t not in IMPLICIT_OK}

print(f"Fichiers scannes : {len(FILES)}")
print(f"Types resolus par Koin : {len(used)}")
print(f"Bindings Koin          : {len(bound)}")
print()
if not missing:
    print("OK : aucun type resolu par Koin sans binding (0 NoDefinitionFoundException possible).")
    sys.exit(0)

print(f"!! {len(missing)} type(s) resolu(s) SANS binding -> NoDefinitionFoundException :")
for t, occ in sorted(missing.items()):
    print(f"\n  - {t}   ({len(occ)} usage(s))")
    for ff, l, s in occ[:5]:
        loc = f"{ff}:{l}" if l else str(ff)
        print(f"      {loc}  {s[:110]}")
sys.exit(1)
