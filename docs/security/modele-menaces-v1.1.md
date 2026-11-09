# Modèle de menaces — B Music v1.1 (téléchargeur)

> **Statut :** v1.1 — mise à jour de la v1.0.0. Date : 2026-11-09.
> **Périmètre :** mise à jour du modèle STRIDE v1.0.0 pour le module
> `com.bmusic.app.downloader` (`downloader/`).
> `INTERNET` est déjà déclaré dans la v1.0.0
> (`android.permission.INTERNET` dans `AndroidManifest.xml`) : cette
> permission n'est pas nouvelle. La mesure de protection propre au
> téléchargeur est **l'isolation réseau dans `downloader/`** : client HTTP
> dédié (`DownloaderHttpClient`, User-Agent fixe, timeouts, pas de cookies),
> jamais partagé avec les autres clients (Last.fm, paroles, mise à jour).

## 1. Actifs et surface

| Actif | Sensibilité | Entrée |
|---|---|---|
| Downloads (fichiers média), bibliothèque locale | Moyen | URL utilisateur, metadata réseau |
| Client HTTP dédié `DownloaderHttpClient` | Critique (vecteur SSRF) | URL suivant parsing/stricte |
| Stockage scopé `Music/B Music`, `Movies/B Music` | Moyen | Noms de fichiers dérivés du réseau |
| Intent de partage (lien hérité), presse-papier | Faible–Moyen | `content://`, `https://youtu.be/...` |

## 2. STRIDE v1.1 (réseau étendu, contenu non fiable, partage, stockage)

### Spoofing
- URL fournie par l'utilisateur apparemment légitime
  (« youtube.com » en texte, alors que l’hôte réel est un righthand).
- **Mesure :** `UrlValidator` normalise (lowercase hôte, port par défaut
  retiré), refuse les identifiants (`user:pass@host`) et les fragments.
  Aucune page HTML n’est interprétée par le module : seul le moteur
  d’extraction parle au réseau.

### Tampering
- Fichier réseau renommé (`../../etc/passwd`, `..\..\evil.exe`,
  noms avec caractères de contrôle ou octets nuls).
- **Mesure :** `FileNameSanitizer` (traversal retiré, extension blanche,
  troncature 100 caractères, noms réservés préfixés, invisibles/bidi
  supprimés, jamais de chemin dérivé du réseau sans filtrage).

### Repudiation
- Un `Content-Length` mensonger pour faire croire à un téléchargement
  complet ou à un fichier valide.
- **Mesure :** `DownloadLimits.evaluate` arrête dès que le flux dépasse
  la taille annoncée et ne présente jamais un transfert incohérent comme
  OK.

### Information disclosure
- Probe du réseau privé : voir si un poste `192.168.`, un routeur
  `169.254.169.254`, un ADB `127.0.0.1:5037` répond.
- **Mesure :** `SsrfGuard` refuse loopback + plages privées
  (`10/8`, `172.16/12`, `192.168/16`, `127/8`, `169.254/16`, `100.64/10`,
  `0.0.0.0`, `::1`, `fc00::/7`, `fe80::/10`, IPv4-mappées en IPv6).
  Périmètre réseau isolé dans `downloader/` (client dédié, pas de partage
  avec les autres modules).

### Denial of service
- Fichier de 2 Go annoncé, gzip bomb, redirection infini.
- **Mesure :** plafond dur audio 200 Mo (configurable pour vidéo),
  arrêt si flux > taille annoncée, redirections limitées à **5**
  avec re-validation complète à chaque saut, timeouts
  `connect 10 s / read 30 s / write 30 s / call 120 s`.

### Elevation of privilege
- Lien `intent://` / `javascript:` / `file://` exécuté si le système venait
  à le déléguer ; média circulaire qui invoque un autre composant avec
  les droits de l'app.
- **Mesure :** `UrlValidator` accepte **http/https uniquement**
  (refuse `file://`, `content://`, `intent://`, `javascript:`, `ftp:`,
  `data:`, etc.), limite 2 048 caractères, pas d’identifiants, pas de
  `Cookie` : `DownloadLimits.USE_COOKIES = false`, encharié par
  `CookieJar.NO_COOKIES`.

## 3. Points d’architecture à retenir

- **DNS rebinding :** `SsrfGuard.resolveAndValidate` vérifie l'IP
  réellement utilisée pour la connexion (résolution épinglée) :
  le client doit se connecter contre `ResolvedTarget.addresses`,
  jamais via une seconde résolution.
- **Redirections :** validation AVANT connexion ET après chaque saut
  (5 max). Une redirection vers une IP privée est bloquée même si la
  première URL était publique (chaîne `example.com` → `internal`).
- **Isolation :** tout le trafic non fiable passe par `downloader/`
  (client + module), jamais par les clients existants.

## 4. Hors périmètre (signalés, non explorés)

- Aucune chaîne DRM n’est ici (L16-Widevine refusé ailleurs).
- Aucune modification d’autorisation Android ou de manifest.
- Aucun moteur de rendu HTML/WebView.

## 5. Référence des garde-fous (implémentation)

| Classe | Fichier | Rôle |
|---|---|---|
| `UrlValidator` | `downloader/security/UrlValidator.kt` | schéma, longueur, credentials, normalisation |
| `SsrfGuard` | `downloader/security/SsrfGuard.kt` | anti-SSRF, redirection 5 max, épinglage DNS |
| `FileNameSanitizer` | `downloader/security/FileNameSanitizer.kt` | nom sûr, extension blanche, bidi/contrôle |
| `DownloadLimits` | `downloader/security/DownloadLimits.kt` | quota audio/vidéo, timeouts, pas de cookies |
