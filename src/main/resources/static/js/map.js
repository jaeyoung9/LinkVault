// ===== Leaflet Map Functions =====

var composeMap = null;
var composeMarker = null;

var CARTODB_TILES = 'https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png';
var CARTODB_ATTR = '&copy; <a href="https://www.openstreetmap.org/copyright">OSM</a> &copy; <a href="https://carto.com/">CARTO</a>';

// ===== Helper: Emoji DivIcon =====
function createEmojiIcon(emoji, isAdmin) {
    var className = 'emoji-marker' + (isAdmin ? ' admin-style' : '');
    return L.divIcon({
        className: className,
        html: emoji || '📍',
        iconSize: [32, 32],        // 전체 아이콘 크기
        iconAnchor: [16, 16],      // 닻 포인트를 정중앙 [너비/2, 높이/2]로 설정
        popupAnchor: [0, -16]      // 팝업이 아이콘 바로 위에서 뜨게 설정
    });
}

// ===== Helper: escapeHtml =====
function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
              .replace(/"/g, '&quot;').replace(/'/g, '&#039;');
}

// ===== Compose Map =====
function initComposeMap() {
    if (composeMap) {
        composeMap.invalidateSize();
        return;
    }

    var mapEl = document.getElementById('composeMap');
    if (!mapEl) return;

    composeMap = L.map('composeMap', {
        zoomControl: false,
        attributionControl: false
    }).setView([37.5665, 126.9780], 11);
    L.tileLayer(CARTODB_TILES, {
        attribution: CARTODB_ATTR,
        maxZoom: 19
    }).addTo(composeMap);

    composeMap.on('click', function(e) {
        var lat = e.latlng.lat;
        var lng = e.latlng.lng;

        document.getElementById('bmLatitude').value = lat;
        document.getElementById('bmLongitude').value = lng;

        var currentEmoji = document.getElementById('bmMapEmoji')?.value || '📍';
        var emojiIcon = createEmojiIcon(currentEmoji);

        if (composeMarker) {
            composeMarker.setLatLng(e.latlng);
            composeMarker.setIcon(emojiIcon); // 위치 이동 시 아이콘도 최신화
        } else {
            composeMarker = L.marker(e.latlng, { icon: emojiIcon }).addTo(composeMap);
        }

        // Reverse geocode to auto-fill address
        reverseGeocode(lat, lng, function(address) {
            if (address) {
                document.getElementById('bmAddress').value = address;
            }
        });
    });

    initComposeSearch();
}

// Reverse geocode using Nominatim
function reverseGeocode(lat, lng, callback) {
    fetch('https://nominatim.openstreetmap.org/reverse?format=json&lat=' + lat + '&lon=' + lng + '&zoom=18&addressdetails=1', {
        headers: { 'Accept-Language': 'ko,en' }
    })
    .then(function(r) { return r.json(); })
    .then(function(data) {
        callback(data.display_name || null);
    })
    .catch(function() { callback(null); });
}

// Compose location search (unified: bmLocationSearch + bmAddress both trigger search)
var composeSearchTimer = null;
function initComposeSearch() {
    var searchInput = document.getElementById('bmLocationSearch');
    var addressInput = document.getElementById('bmAddress');
    var resultsDiv = document.getElementById('composeSearchResults');
    if (!resultsDiv) return;

    function doComposeSearch(query) {
        clearTimeout(composeSearchTimer);
        if (!query || query.length < 2) {
            resultsDiv.style.display = 'none';
            return;
        }
        composeSearchTimer = setTimeout(function() {
            fetch('https://nominatim.openstreetmap.org/search?format=json&q=' + encodeURIComponent(query) + '&limit=5', {
                headers: { 'Accept-Language': 'ko,en' }
            })
            .then(function(r) { return r.json(); })
            .then(function(results) {
                if (!results || results.length === 0) {
                    resultsDiv.style.display = 'none';
                    return;
                }
                var html = '';
                results.forEach(function(r) {
                    html += '<div class="compose-search-item" onclick="selectComposeLocation(' +
                        r.lat + ',' + r.lon + ',\'' + escapeHtml(r.display_name).replace(/'/g, "\\'") + '\')">' +
                        '<span>📍</span> <span>' + escapeHtml(r.display_name) + '</span></div>';
                });
                resultsDiv.innerHTML = html;
                resultsDiv.style.display = 'block';
            })
            .catch(function() { resultsDiv.style.display = 'none'; });
        }, 150);
    }

    // Wire bmLocationSearch
    if (searchInput) {
        searchInput.addEventListener('input', function() {
            doComposeSearch(searchInput.value.trim());
        });
    }

    // Wire bmAddress — also triggers search so both inputs work seamlessly
    if (addressInput) {
        addressInput.addEventListener('input', function() {
            // Only trigger search from bmAddress if bmLocationSearch is empty
            var searchVal = searchInput ? searchInput.value.trim() : '';
            if (!searchVal) {
                doComposeSearch(addressInput.value.trim());
            }
        });
    }

    // Close on outside click
    document.addEventListener('click', function(e) {
        if (!e.target.closest('#bmLocationSearch') && !e.target.closest('#bmAddress') && !e.target.closest('#composeSearchResults')) {
            resultsDiv.style.display = 'none';
        }
    });
}

function selectComposeLocation(lat, lng, address) {
    document.getElementById('bmLatitude').value = lat;
    document.getElementById('bmLongitude').value = lng;
    document.getElementById('bmAddress').value = address;
    var searchInput = document.getElementById('bmLocationSearch');
    if (searchInput) searchInput.value = '';
    var resultsDiv = document.getElementById('composeSearchResults');
    if (resultsDiv) resultsDiv.style.display = 'none';

    if (composeMap) {
        var latlng = L.latLng(lat, lng);
        composeMap.setView(latlng, 15);

        var currentEmoji = document.getElementById('bmMapEmoji')?.value || '📍';
        var emojiIcon = createEmojiIcon(currentEmoji);

        if (composeMarker) {
            composeMarker.setLatLng(latlng);
            composeMarker.setIcon(emojiIcon); // 아이콘 업데이트
        } else {
            composeMarker = L.marker(latlng, { icon: emojiIcon }).addTo(composeMap);
        }
    }
}

// ===== Detail Map =====
function initDetailMap(lat, lng, emoji) {
    var mapEl = document.getElementById('detailMap');
    if (!mapEl || !lat || !lng) return;

    var map = L.map('detailMap', {
        scrollWheelZoom: false,
        dragging: false,
        zoomControl: false,
        attributionControl: false
    }).setView([lat, lng], 15);

    L.tileLayer(CARTODB_TILES, {
        attribution: CARTODB_ATTR,
        maxZoom: 19
    }).addTo(map);

    L.marker([lat, lng], { icon: createEmojiIcon(emoji || '📍') }).addTo(map);
}

// ===== Discovery Map =====
var discoveryMap = null;
var discoveryMarkers = [];
var discoveryPosts = [];
var currentMode = 'USER';

function initDiscoveryMap() {
    var mapEl = document.getElementById('discoveryMap');
    if (!mapEl) return;

    discoveryMap = L.map('discoveryMap', {
        zoomControl: false,
        attributionControl: false
    }).setView([37.5665, 126.9780], 7);

    L.tileLayer(CARTODB_TILES, {
        attribution: CARTODB_ATTR,
        maxZoom: 19
    }).addTo(discoveryMap);

    loadMapData();
    initMapSearch();
}

function loadMapData() {
    var url = '/api/bookmarks/map-data';
    if (currentMode === 'ADMIN' && typeof IS_ADMIN !== 'undefined' && IS_ADMIN) {
        url += '?admin=true';
    }

    fetch(url)
        .then(function(r) { return r.json(); })
        .then(function(posts) {
            discoveryPosts = posts;
            renderMarkers();
        })
        .catch(function() {
            console.error('Failed to load map data');
        });
}

function renderMarkers() {
    // Clear existing markers
    discoveryMarkers.forEach(function(m) {
        discoveryMap.removeLayer(m);
    });
    discoveryMarkers = [];

    discoveryPosts.forEach(function(post) {
        if (!post.latitude || !post.longitude) return;

        //var icon = post.mapEmoji ? createEmojiIcon(post.mapEmoji) : createEmojiIcon('📍');
        var isPrivateAlert = (currentMode === 'ADMIN' && post.privatePost === true);
        var icon = createEmojiIcon(post.mapEmoji || '📍', isPrivateAlert);
        var marker = L.marker([post.latitude, post.longitude], { icon: icon });

        // Glass-card popup
        var popupHtml = '<div class="glass-popup"><div class="glass-card">';
        if (post.leadPhotoUrl) {
            popupHtml += '<div class="glass-photo"><img src="' + escapeHtml(post.leadPhotoUrl) + '" alt=""/></div>';
        }
        popupHtml += '<div class="glass-card-body">';
        popupHtml += '<div style="font-weight:600;font-size:0.9rem;margin-bottom:4px;">' + escapeHtml(post.title) + '</div>';
        if (post.address) {
            popupHtml += '<div style="font-size:0.78rem;color:#888;margin-bottom:2px;">📍 ' + escapeHtml(post.address) + '</div>';
        }
        if (post.ownerUsername) {
            popupHtml += '<div style="font-size:0.72rem;color:#aaa;margin-bottom:6px;">by ' + escapeHtml(post.ownerUsername) + '</div>';
        }
        if (post.privatePost) {
            popupHtml += '<div style="font-size:0.7rem;color:var(--accent);margin-bottom:4px;">🔒 Private</div>';
        }
        popupHtml += '<a href="/bookmark/' + post.id + '" class="open-btn">Open</a>';
        popupHtml += '</div></div></div>';

        marker.bindPopup(popupHtml, { className: 'glass-popup-wrapper', maxWidth: 240 });
        marker.addTo(discoveryMap);
        discoveryMarkers.push(marker);
    });
}

// ===== Map Search =====
var mapSearchTimer = null;

function initMapSearch() {
    var input = document.getElementById('mapSearchInput');
    var resultsDiv = document.getElementById('mapSearchResults');
    if (!input || !resultsDiv) return;

    input.addEventListener('input', function() {
        clearTimeout(mapSearchTimer);
        var q = input.value.trim();
        if (q.length < 2) {
            resultsDiv.style.display = 'none';
            return;
        }
        mapSearchTimer = setTimeout(function() {
            handleMapSearch(q);
        }, 350);
    });

    input.addEventListener('focus', function() {
        if (input.value.trim().length >= 2) {
            resultsDiv.style.display = 'block';
        }
    });

    // Close on outside click
    document.addEventListener('click', function(e) {
        if (!e.target.closest('.map-search-wrapper')) {
            resultsDiv.style.display = 'none';
        }
    });
}

function handleMapSearch(query) {
    var resultsDiv = document.getElementById('mapSearchResults');
    var lowerQuery = query.toLowerCase();

    // Internal filter
    var internalResults = discoveryPosts.filter(function(p) {
        var titleMatch = p.title && p.title.toLowerCase().indexOf(lowerQuery) !== -1;
        var addressMatch = p.address && p.address.toLowerCase().indexOf(lowerQuery) !== -1;
        return titleMatch || addressMatch;
    }).slice(0, 5);

    // Nominatim external search
    fetch('https://nominatim.openstreetmap.org/search?format=json&q=' + encodeURIComponent(query) + '&limit=5', {
        headers: { 'Accept-Language': 'ko,en' }
    })
    .then(function(r) { return r.json(); })
    .then(function(osmResults) {
        var html = '';

        if (internalResults.length > 0) {
            html += '<div class="map-search-group-title">LinkVault Posts</div>';
            internalResults.forEach(function(p) {
                html += '<div class="map-search-item" onclick="flyToPost(' + p.latitude + ',' + p.longitude + ')">' +
                    '<span class="map-search-item-emoji">' + (p.mapEmoji || '📍') + '</span>' +
                    '<div><div class="map-search-item-title">' + escapeHtml(p.title) + '</div>' +
                    (p.address ? '<div class="map-search-item-sub">' + escapeHtml(p.address) + '</div>' : '') +
                    '</div></div>';
            });
        }

        if (osmResults && osmResults.length > 0) {
            html += '<div class="map-search-group-title">Places (OSM)</div>';
            osmResults.forEach(function(r) {
                html += '<div class="map-search-item" onclick="flyToPost(' + r.lat + ',' + r.lon + ')">' +
                    '<span class="map-search-item-emoji">🌍</span>' +
                    '<div><div class="map-search-item-title">' + escapeHtml(r.display_name.split(',')[0]) + '</div>' +
                    '<div class="map-search-item-sub">' + escapeHtml(r.display_name) + '</div>' +
                    '</div></div>';
            });
        }

        if (!html) {
            html = '<div style="padding:12px;text-align:center;color:#888;font-size:0.85rem;">No results found</div>';
        }

        resultsDiv.innerHTML = html;
        resultsDiv.style.display = 'block';
    })
    .catch(function() {
        // Show internal results only
        var html = '';
        if (internalResults.length > 0) {
            internalResults.forEach(function(p) {
                html += '<div class="map-search-item" onclick="flyToPost(' + p.latitude + ',' + p.longitude + ')">' +
                    '<span class="map-search-item-emoji">' + (p.mapEmoji || '📍') + '</span>' +
                    '<div><div class="map-search-item-title">' + escapeHtml(p.title) + '</div></div></div>';
            });
        }
        if (html) {
            resultsDiv.innerHTML = html;
            resultsDiv.style.display = 'block';
        }
    });
}

function flyToPost(lat, lng) {
    if (discoveryMap) {
        discoveryMap.flyTo([lat, lng], 16);
    }
    var resultsDiv = document.getElementById('mapSearchResults');
    if (resultsDiv) resultsDiv.style.display = 'none';
    var input = document.getElementById('mapSearchInput');
    if (input) input.value = '';
}

// ===== Mode Toggle =====
function setMode(mode) {
    currentMode = mode;
    var userBtn = document.getElementById('modeBtnUser');
    var adminBtn = document.getElementById('modeBtnAdmin');
    var glider = document.getElementById('modeGlider');

    if (mode === 'ADMIN') {
        if (userBtn) userBtn.classList.remove('active');
        if (adminBtn) adminBtn.classList.add('active');
        if (glider) glider.style.transform = 'translateX(100%)';
    } else {
        if (userBtn) userBtn.classList.add('active');
        if (adminBtn) adminBtn.classList.remove('active');
        if (glider) glider.style.transform = 'translateX(0)';
    }

    loadMapData();
}
