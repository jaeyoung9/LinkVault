// ===== Toast Notifications =====
function showToast(message, type) {
    type = type || 'success';
    var container = document.querySelector('.toast-container');
    if (!container) {
        container = document.createElement('div');
        container.className = 'toast-container';
        document.body.appendChild(container);
    }
    var toast = document.createElement('div');
    toast.className = 'toast ' + type;
    toast.textContent = message;
    container.appendChild(toast);
    setTimeout(function() { toast.remove(); }, 3000);
}

// ===== Utility =====
function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
              .replace(/"/g, '&quot;').replace(/'/g, '&#039;');
}

// ===== Post Compose (replaces Bookmark CRUD) =====
var pendingPhotos = [];
var existingPhotos = [];
var deletePhotoIds = [];

function openAddModal() {
    document.getElementById('bookmarkModal').classList.add('active');
    document.getElementById('modalTitle').textContent = 'Share a Story';
    document.getElementById('bookmarkForm').reset();
    document.getElementById('bookmarkId').value = '';
    document.getElementById('bmLatitude').value = '';
    document.getElementById('bmLongitude').value = '';
    pendingPhotos = [];
    existingPhotos = [];
    deletePhotoIds = [];
    renderPhotoPreview();
    var bmPrivate = document.getElementById('bmPrivate');
    if (bmPrivate) bmPrivate.checked = false;

    // Reset emoji picker
    var emojiInput = document.getElementById('bmMapEmoji');
    if (emojiInput) emojiInput.value = '';
    var emojiBtn = document.getElementById('emojiPickerBtn');
    if (emojiBtn) emojiBtn.innerHTML = '&#128205;';
    var emojiPanel = document.getElementById('emojiPickerPanel');
    if (emojiPanel) emojiPanel.style.display = 'none';

    // Reset location search
    var locSearch = document.getElementById('bmLocationSearch');
    if (locSearch) locSearch.value = '';

    // Init compose map after modal is visible
    setTimeout(function() {
        if (typeof initComposeMap === 'function') {
            initComposeMap();
        }
        // Reset marker
        if (typeof composeMarker !== 'undefined' && composeMarker) {
            if (typeof composeMap !== 'undefined' && composeMap) {
                composeMap.removeLayer(composeMarker);
            }
            composeMarker = null;
        }
    }, 200);
}

function openEditModal(id) {
    fetch('/api/bookmarks/' + id)
        .then(function(r) { return r.json(); })
        .then(function(b) {
            document.getElementById('bookmarkModal').classList.add('active');
            document.getElementById('modalTitle').textContent = 'Edit Post';
            document.getElementById('bookmarkId').value = b.id;
            document.getElementById('bmTitle').value = b.title || '';
            document.getElementById('bmUrl').value = b.url || '';
            document.getElementById('bmDescription').value = b.description || '';
            document.getElementById('bmTags').value = b.tagNames ? b.tagNames.join(', ') : '';
            document.getElementById('bmCaption').value = b.caption || '';
            document.getElementById('bmAddress').value = b.address || '';
            document.getElementById('bmLatitude').value = b.latitude || '';
            document.getElementById('bmLongitude').value = b.longitude || '';
            var folderSelect = document.getElementById('bmFolder');
            if (folderSelect) folderSelect.value = b.folderId || '';

            // Load existing photos
            pendingPhotos = [];
            deletePhotoIds = [];
            existingPhotos = b.photos || [];
            renderPhotoPreview();

            var bmPrivate = document.getElementById('bmPrivate');
            if (bmPrivate) bmPrivate.checked = !!b.privatePost;

            // Set emoji picker
            var emojiInput = document.getElementById('bmMapEmoji');
            if (emojiInput) emojiInput.value = b.mapEmoji || '';
            var emojiBtn = document.getElementById('emojiPickerBtn');
            if (emojiBtn) emojiBtn.textContent = b.mapEmoji || '📍';
            var emojiPanel = document.getElementById('emojiPickerPanel');
            if (emojiPanel) emojiPanel.style.display = 'none';

            // Reset location search
            var locSearch = document.getElementById('bmLocationSearch');
            if (locSearch) locSearch.value = '';

            // Init compose map and set marker
            setTimeout(function() {
                if (typeof initComposeMap === 'function') {
                    initComposeMap();
                }
                if (b.latitude && b.longitude && typeof composeMap !== 'undefined' && composeMap) {
                    var latlng = L.latLng(b.latitude, b.longitude);
                    composeMap.setView(latlng, 15);
                    if (typeof composeMarker !== 'undefined' && composeMarker) {
                        composeMarker.setLatLng(latlng);
                    } else {
                        composeMarker = L.marker(latlng).addTo(composeMap);
                    }
                }
            }, 200);
        });
}

function closeModal() {
    document.getElementById('bookmarkModal').classList.remove('active');
}

function openFolderModal() {
    document.getElementById('folderModal').classList.add('active');
    document.getElementById('folderForm').reset();
}

function closeFolderModal() {
    document.getElementById('folderModal').classList.remove('active');
}

function savePost(event) {
    event.preventDefault();
    var id = document.getElementById('bookmarkId').value;
    var tagsRaw = document.getElementById('bmTags').value;
    var tagNames = tagsRaw ? tagsRaw.split(',').map(function(t) { return t.trim(); }).filter(function(t) { return t; }) : [];
    var folderVal = document.getElementById('bmFolder').value;

    var bmPrivateEl = document.getElementById('bmPrivate');
    var data = {
        title: document.getElementById('bmTitle').value,
        url: document.getElementById('bmUrl').value || null,
        description: document.getElementById('bmDescription').value,
        tagNames: tagNames,
        folderId: folderVal ? parseInt(folderVal) : null,
        latitude: document.getElementById('bmLatitude').value ? parseFloat(document.getElementById('bmLatitude').value) : null,
        longitude: document.getElementById('bmLongitude').value ? parseFloat(document.getElementById('bmLongitude').value) : null,
        address: document.getElementById('bmAddress').value || null,
        caption: document.getElementById('bmCaption').value || null,
        mapEmoji: document.getElementById('bmMapEmoji').value || null,
        privatePost: bmPrivateEl ? bmPrivateEl.checked : false
    };

    var formData = new FormData();
    formData.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }));

    // Add pending photos
    pendingPhotos.forEach(function(file) {
        formData.append('photos', file);
    });

    // For edit: add deletePhotoIds
    if (id && deletePhotoIds.length > 0) {
        formData.append('deletePhotoIds', new Blob([JSON.stringify(deletePhotoIds)], { type: 'application/json' }));
    }

    // Send photo order from DOM
    if (id) {
        var orderContainer = document.getElementById('photoPreviewContainer');
        if (orderContainer) {
            var photoOrder = [];
            Array.from(orderContainer.children).forEach(function(item) {
                if (item.dataset.type === 'existing') {
                    photoOrder.push(parseInt(item.dataset.photoId));
                }
            });
            if (photoOrder.length > 0) {
                formData.append('photoOrder', new Blob([JSON.stringify(photoOrder)], { type: 'application/json' }));
            }
        }
    }

    var method = id ? 'PUT' : 'POST';
    var url = id ? '/api/bookmarks/' + id : '/api/bookmarks';

    fetch(url, {
        method: method,
        body: formData
    })
    .then(function(r) {
        if (!r.ok) return r.json().then(function(e) { throw e; });
        return r.json();
    })
    .then(function() {
        closeModal();
        showToast(id ? 'Post updated' : 'Post shared!');
        setTimeout(function() { location.reload(); }, 500);
    })
    .catch(function(err) {
        var msg = err.message || 'Error saving post';
        if (err.fieldErrors) msg = Object.values(err.fieldErrors).join(', ');
        showToast(msg, 'error');
    });
}

function openAddModalPrivate() {
    openAddModal();
    setTimeout(function() {
        var bmPrivate = document.getElementById('bmPrivate');
        if (bmPrivate) bmPrivate.checked = true;
    }, 50);
}

function switchSavedTab(tabName, btn) {
    document.getElementById('savedTab').style.display = tabName === 'saved' ? '' : 'none';
    document.getElementById('privateTab').style.display = tabName === 'private' ? '' : 'none';
    var tabs = document.querySelectorAll('.settings-tab');
    tabs.forEach(function(t) { t.classList.remove('active'); });
    if (btn) btn.classList.add('active');
}

// Legacy function for backward compatibility
function saveBookmark(event) {
    savePost(event);
}

function deleteBookmark(id) {
    if (!confirm('Delete this post?')) return;
    fetch('/api/bookmarks/' + id, { method: 'DELETE' })
        .then(function() {
            showToast('Post deleted');
            setTimeout(function() { location.reload(); }, 500);
        })
        .catch(function() { showToast('Error deleting post', 'error'); });
}

function recordAccess(id, url) {
    fetch('/api/bookmarks/' + id + '/access', { method: 'POST' });
    window.open(url, '_blank');
}

// ===== Photo Upload =====
function handlePhotoSelect(files) {
    for (var i = 0; i < files.length; i++) {
        if (pendingPhotos.length + existingPhotos.length >= 4) {
            showToast('Maximum 4 photos allowed', 'error');
            break;
        }
        if (files[i].type.startsWith('image/')) {
            pendingPhotos.push(files[i]);
        }
    }
    renderPhotoPreview();
}

function renderPhotoPreview() {
    var container = document.getElementById('photoPreviewContainer');
    if (!container) return;
    container.innerHTML = '';

    var orderNum = 1;

    // Existing photos (from edit)
    existingPhotos.forEach(function(photo) {
        var div = document.createElement('div');
        div.className = 'photo-preview-item';
        div.draggable = true;
        div.dataset.type = 'existing';
        div.dataset.photoId = photo.id;
        div.innerHTML = '<img src="' + photo.url + '" alt=""/>' +
            '<button type="button" class="photo-preview-remove" onclick="removeExistingPhoto(' + photo.id + ')">&times;</button>' +
            '<span class="photo-order-badge">' + orderNum + '</span>';
        orderNum++;
        initPhotoDrag(div);
        container.appendChild(div);
    });

    // Pending photos (newly selected)
    pendingPhotos.forEach(function(file, idx) {
        var div = document.createElement('div');
        div.className = 'photo-preview-item';
        div.draggable = true;
        div.dataset.type = 'pending';
        div.dataset.pendingIdx = idx;
        var img = document.createElement('img');
        img.alt = '';
        var reader = new FileReader();
        reader.onload = function(e) { img.src = e.target.result; };
        reader.readAsDataURL(file);
        div.appendChild(img);
        var btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'photo-preview-remove';
        btn.innerHTML = '&times;';
        btn.dataset.idx = idx;
        btn.onclick = function() { removePendingPhoto(parseInt(this.dataset.idx)); };
        div.appendChild(btn);
        var badge = document.createElement('span');
        badge.className = 'photo-order-badge';
        badge.textContent = orderNum;
        orderNum++;
        div.appendChild(badge);
        initPhotoDrag(div);
        container.appendChild(div);
    });
}

function initPhotoDrag(el) {
    el.addEventListener('dragstart', function(e) {
        el.classList.add('photo-dragging');
        e.dataTransfer.effectAllowed = 'move';
        e.dataTransfer.setData('text/plain', '');
        e.stopPropagation();
    });
    el.addEventListener('dragend', function() {
        el.classList.remove('photo-dragging');
        document.querySelectorAll('.photo-drag-over').forEach(function(d) { d.classList.remove('photo-drag-over'); });
    });
    el.addEventListener('dragover', function(e) {
        e.preventDefault();
        e.stopPropagation();
        var dragging = document.querySelector('.photo-dragging');
        if (dragging && dragging !== el) {
            el.classList.add('photo-drag-over');
        }
    });
    el.addEventListener('dragleave', function() {
        el.classList.remove('photo-drag-over');
    });
    el.addEventListener('drop', function(e) {
        e.preventDefault();
        e.stopPropagation();
        el.classList.remove('photo-drag-over');
        var dragging = document.querySelector('.photo-dragging');
        if (dragging && dragging !== el) {
            var container = el.parentNode;
            var items = Array.from(container.children);
            var fromIdx = items.indexOf(dragging);
            var toIdx = items.indexOf(el);
            if (fromIdx < toIdx) {
                container.insertBefore(dragging, el.nextSibling);
            } else {
                container.insertBefore(dragging, el);
            }
            syncPhotoOrderFromDom();
        }
    });
}

function syncPhotoOrderFromDom() {
    var container = document.getElementById('photoPreviewContainer');
    if (!container) return;
    var items = Array.from(container.children);
    var newExisting = [];
    var newPending = [];

    items.forEach(function(item, idx) {
        var badge = item.querySelector('.photo-order-badge');
        if (badge) badge.textContent = idx + 1;

        if (item.dataset.type === 'existing') {
            var photoId = parseInt(item.dataset.photoId);
            var found = existingPhotos.find(function(p) { return p.id === photoId; });
            if (found) newExisting.push(found);
        } else if (item.dataset.type === 'pending') {
            var pendingIdx = parseInt(item.dataset.pendingIdx);
            if (pendingPhotos[pendingIdx]) newPending.push(pendingPhotos[pendingIdx]);
        }
    });

    existingPhotos = newExisting;
    pendingPhotos = newPending;

    // Update pending indices
    items.forEach(function(item, idx) {
        if (item.dataset.type === 'pending') {
            var file = newPending.indexOf(pendingPhotos.find(function(f) {
                return f === newPending[Array.from(container.querySelectorAll('[data-type="pending"]')).indexOf(item)];
            }));
        }
    });
}

function removePendingPhoto(idx) {
    pendingPhotos.splice(idx, 1);
    renderPhotoPreview();
}

function removeExistingPhoto(photoId) {
    deletePhotoIds.push(photoId);
    existingPhotos = existingPhotos.filter(function(p) { return p.id !== photoId; });
    renderPhotoPreview();
}

// Photo drop zone
function initPhotoDropZone() {
    var zone = document.getElementById('photoDropZone');
    if (!zone) return;

    zone.addEventListener('dragover', function(e) {
        e.preventDefault();
        zone.classList.add('drag-active');
    });
    zone.addEventListener('dragleave', function() {
        zone.classList.remove('drag-active');
    });
    zone.addEventListener('drop', function(e) {
        e.preventDefault();
        zone.classList.remove('drag-active');
        handlePhotoSelect(e.dataTransfer.files);
    });
}

// ===== Emoji Picker =====
var EMOJI_LIST = ['📍','🏠','🏢','🏫','🏪','🏨','🏩','⛪','🕌','🕍','⛲','⛵','🏖️','🏔️','🏞️','🌊','🌳','🌸','🌻','🌿','🍔','🍕','☕','🍰','🍽️','🎨','🎵','📷','✈️','🚂','🚗','🚲','🎯','⭐','❤️','💡','🔥','🌟','🌈','🎉'];
var emojiPanelPopulated = false;

function toggleEmojiPicker() {
    var panel = document.getElementById('emojiPickerPanel');
    if (!panel) return;
    if (panel.style.display === 'none' || panel.style.display === '') {
        if (!emojiPanelPopulated) {
            var html = '';
            EMOJI_LIST.forEach(function(emoji) {
                html += '<button type="button" class="emoji-picker-item" onclick="selectEmoji(\'' + emoji + '\')">' + emoji + '</button>';
            });
            panel.innerHTML = html;
            emojiPanelPopulated = true;
        }
        panel.style.display = 'flex';
    } else {
        panel.style.display = 'none';
    }
}

function selectEmoji(emoji) {
    var input = document.getElementById('bmMapEmoji');
    if (input) input.value = emoji;
    var btn = document.getElementById('emojiPickerBtn');
    if (btn) btn.textContent = emoji;

    if (typeof composeMarker !== 'undefined' && composeMarker && composeMap) {
        var newIcon = createEmojiIcon(emoji);
        composeMarker.setIcon(newIcon);
    }

    var panel = document.getElementById('emojiPickerPanel');
    if (panel) panel.style.display = 'none';
}

function clearEmojiSelection() {
    var input = document.getElementById('bmMapEmoji');
    if (input) input.value = '';
    var btn = document.getElementById('emojiPickerBtn');
    if (btn) btn.innerHTML = '&#128205;';

    if (typeof composeMarker !== 'undefined' && composeMarker) {
        composeMarker.setIcon(createEmojiIcon('📍'));
    }

    var panel = document.getElementById('emojiPickerPanel');
    if (panel) panel.style.display = 'none';
}

// ===== Report Content =====
function reportContent(targetType, targetId) {
    var reason = prompt('Please describe why you are reporting this content:');
    if (!reason || !reason.trim()) return;

    fetch('/api/reports', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            targetType: targetType,
            targetId: targetId,
            reason: reason.trim()
        })
    })
    .then(function(r) {
        if (!r.ok) return r.json().then(function(e) { throw e; });
        return r.json();
    })
    .then(function() {
        showToast('Report submitted. Thank you.');
    })
    .catch(function(err) {
        var msg = err.message || 'Error submitting report';
        showToast(msg, 'error');
    });
}

// ===== Folder CRUD =====
function saveFolder(event) {
    event.preventDefault();
    var data = {
        name: document.getElementById('folderName').value,
        parentId: document.getElementById('folderParent').value || null
    };

    fetch('/api/folders', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
    })
    .then(function(r) {
        if (!r.ok) return r.json().then(function(e) { throw e; });
        return r.json();
    })
    .then(function() {
        closeFolderModal();
        showToast('Folder created');
        setTimeout(function() { location.reload(); }, 500);
    })
    .catch(function(err) { showToast(err.message || 'Error creating folder', 'error'); });
}

function deleteFolder(id) {
    if (!confirm('Delete this folder and all its contents?')) return;
    fetch('/api/folders/' + id, { method: 'DELETE' })
        .then(function() {
            showToast('Folder deleted');
            setTimeout(function() { location.reload(); }, 500);
        })
        .catch(function() { showToast('Error deleting folder', 'error'); });
}

// ===== Drag & Drop =====
var draggedBookmarkId = null;

function initDragDrop() {
    document.querySelectorAll('.bookmark-card[draggable]').forEach(function(card) {
        card.addEventListener('dragstart', function(e) {
            draggedBookmarkId = card.dataset.id;
            card.classList.add('dragging');
            e.dataTransfer.effectAllowed = 'move';
        });
        card.addEventListener('dragend', function() {
            card.classList.remove('dragging');
            draggedBookmarkId = null;
            document.querySelectorAll('.drag-over').forEach(function(el) { el.classList.remove('drag-over'); });
        });
    });

    document.querySelectorAll('.folder-item').forEach(function(item) {
        item.addEventListener('dragover', function(e) {
            e.preventDefault();
            e.dataTransfer.dropEffect = 'move';
            item.classList.add('drag-over');
        });
        item.addEventListener('dragleave', function() {
            item.classList.remove('drag-over');
        });
        item.addEventListener('drop', function(e) {
            e.preventDefault();
            item.classList.remove('drag-over');
            if (draggedBookmarkId) {
                var folderId = item.dataset.folderId;
                moveBookmarkToFolder(draggedBookmarkId, folderId);
            }
        });
    });

    var uncatZone = document.getElementById('uncategorizedDrop');
    if (uncatZone) {
        uncatZone.addEventListener('dragover', function(e) {
            e.preventDefault();
            uncatZone.classList.add('drag-over');
        });
        uncatZone.addEventListener('dragleave', function() { uncatZone.classList.remove('drag-over'); });
        uncatZone.addEventListener('drop', function(e) {
            e.preventDefault();
            uncatZone.classList.remove('drag-over');
            if (draggedBookmarkId) moveBookmarkToFolder(draggedBookmarkId, null);
        });
    }
}

function moveBookmarkToFolder(bookmarkId, folderId) {
    fetch('/api/bookmarks/' + bookmarkId + '/move', {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ folderId: folderId ? parseInt(folderId) : null })
    })
    .then(function(r) {
        if (!r.ok) throw new Error();
        showToast('Post moved');
        setTimeout(function() { location.reload(); }, 500);
    })
    .catch(function() { showToast('Error moving post', 'error'); });
}

// ===== URL Duplicate Check =====
function checkDuplicateUrl() {
    var urlInput = document.getElementById('bmUrl');
    if (!urlInput || !urlInput.value) return;
    fetch('/api/bookmarks/check-url?url=' + encodeURIComponent(urlInput.value))
        .then(function(r) { return r.json(); })
        .then(function(data) {
            if (data.exists && !document.getElementById('bookmarkId').value) {
                urlInput.style.borderColor = '#ef4444';
                showToast('This URL already exists!', 'error');
            } else {
                urlInput.style.borderColor = '';
            }
        });
}

// ===== Import =====
function importFile(format) {
    var input = document.getElementById(format + 'File');
    if (!input.files.length) {
        showToast('Please select a file', 'error');
        return;
    }
    var formData = new FormData();
    formData.append('file', input.files[0]);

    fetch('/api/data/import/' + format, { method: 'POST', body: formData })
        .then(function(r) { return r.json(); })
        .then(function(data) {
            showToast(data.message);
            setTimeout(function() { window.location.href = '/'; }, 1500);
        })
        .catch(function() { showToast('Import failed', 'error'); });
}

// ===== Feature 1: Sidebar Collapse =====
function toggleSidebar() {
    var layout = document.querySelector('.app-layout');
    layout.classList.toggle('mini-sidebar');
    localStorage.setItem('sidebarMini', layout.classList.contains('mini-sidebar'));
}

function initSidebarState() {
    if (localStorage.getItem('sidebarMini') === 'true') {
        var layout = document.querySelector('.app-layout');
        if (layout) layout.classList.add('mini-sidebar');
    }
}

function toggleMobileSidebar() {
    var sidebar = document.querySelector('.sidebar');
    var overlay = document.getElementById('mobileOverlay');
    if (sidebar) sidebar.classList.toggle('mobile-open');
    if (overlay) overlay.classList.toggle('active');
}

// ===== Feature 2: Global Search / Ctrl+K =====
var searchDebounceTimer = null;

function openSearchModal() {
    var modal = document.getElementById('searchModal');
    if (modal) {
        modal.classList.add('active');
        setTimeout(function() {
            var input = document.getElementById('globalSearchInput');
            if (input) { input.value = ''; input.focus(); }
        }, 100);
        document.getElementById('searchResults').innerHTML =
            '<div style="padding:24px;text-align:center;color:var(--text-muted);font-size:0.85rem;">Type to search...</div>';
    }
}

function closeSearchModal() {
    var modal = document.getElementById('searchModal');
    if (modal) modal.classList.remove('active');
}

function onGlobalSearchInput() {
    clearTimeout(searchDebounceTimer);
    var query = document.getElementById('globalSearchInput').value.trim();
    if (!query) {
        document.getElementById('searchResults').innerHTML =
            '<div style="padding:24px;text-align:center;color:var(--text-muted);font-size:0.85rem;">Type to search...</div>';
        return;
    }
    searchDebounceTimer = setTimeout(function() {
        fetch('/api/search?q=' + encodeURIComponent(query))
            .then(function(r) { return r.json(); })
            .then(function(data) { renderSearchResults(data); })
            .catch(function() {
                document.getElementById('searchResults').innerHTML =
                    '<div style="padding:24px;text-align:center;color:var(--text-muted);">Search error</div>';
            });
    }, 300);
}

function renderSearchResults(data) {
    var html = '';
    if (data.bookmarks && data.bookmarks.length > 0) {
        html += '<div class="search-result-group"><div class="search-result-group-title">Posts</div>';
        data.bookmarks.forEach(function(b) {
            html += '<a href="/bookmark/' + b.id + '" class="search-result-item">' +
                '<span class="search-result-icon">&#128279;</span>' +
                '<span>' + escapeHtml(b.title) + '</span></a>';
        });
        html += '</div>';
    }
    if (data.tags && data.tags.length > 0) {
        html += '<div class="search-result-group"><div class="search-result-group-title">Tags</div>';
        data.tags.forEach(function(t) {
            html += '<a href="/tag/' + encodeURIComponent(t.name) + '" class="search-result-item">' +
                '<span class="search-result-icon">&#127991;</span>' +
                '<span>' + escapeHtml(t.name) + ' (' + t.bookmarkCount + ')</span></a>';
        });
        html += '</div>';
    }
    if (data.folders && data.folders.length > 0) {
        html += '<div class="search-result-group"><div class="search-result-group-title">Folders</div>';
        data.folders.forEach(function(f) {
            html += '<a href="/folder/' + f.id + '" class="search-result-item">' +
                '<span class="search-result-icon">&#128193;</span>' +
                '<span>' + escapeHtml(f.name) + '</span></a>';
        });
        html += '</div>';
    }
    if (!html) {
        html = '<div style="padding:24px;text-align:center;color:var(--text-muted);font-size:0.85rem;">No results found</div>';
    }
    document.getElementById('searchResults').innerHTML = html;
}

// ===== Feature 3: Save Bookmark (Read Later) =====
function toggleSaveBookmark(id) {
    fetch('/api/bookmarks/' + id + '/save', { method: 'POST' })
        .then(function(r) { return r.json(); })
        .then(function(data) {
            var btns = document.querySelectorAll('.js-save-btn[data-id="' + id + '"]');
            btns.forEach(function(btn) {
                if (data.saved) {
                    btn.classList.add('save-active');
                } else {
                    btn.classList.remove('save-active');
                }
            });
            showToast(data.saved ? 'Post saved' : 'Post unsaved');
        })
        .catch(function() { showToast('Error saving post', 'error'); });
}

// ===== Feature 4: Favorites =====
function toggleFavoriteBookmark(id) {
    fetch('/api/bookmarks/' + id + '/favorite', { method: 'POST' })
        .then(function(r) { return r.json(); })
        .then(function(data) {
            var btns = document.querySelectorAll('.js-fav-btn[data-id="' + id + '"]');
            btns.forEach(function(btn) {
                if (data.favorited) {
                    btn.classList.add('fav-active');
                    btn.innerHTML = '&#9733;';
                } else {
                    btn.classList.remove('fav-active');
                    btn.innerHTML = '&#9734;';
                }
            });
            showToast(data.favorited ? 'Added to favorites' : 'Removed from favorites');
            setTimeout(function() { location.reload(); }, 800);
        })
        .catch(function() { showToast('Error updating favorite', 'error'); });
}

function initFavoritesDragDrop() {
    var container = document.getElementById('favoritesContainer');
    if (!container) return;
    var items = container.querySelectorAll('.favorite-item');
    var dragItem = null;

    items.forEach(function(item) {
        item.addEventListener('dragstart', function(e) {
            dragItem = item;
            item.classList.add('dragging');
            e.dataTransfer.effectAllowed = 'move';
            e.stopPropagation();
        });
        item.addEventListener('dragend', function() {
            item.classList.remove('dragging');
            dragItem = null;
            items.forEach(function(i) { i.classList.remove('drag-over-fav'); });
        });
        item.addEventListener('dragover', function(e) {
            e.preventDefault();
            e.stopPropagation();
            if (dragItem && dragItem !== item) {
                item.classList.add('drag-over-fav');
            }
        });
        item.addEventListener('dragleave', function() {
            item.classList.remove('drag-over-fav');
        });
        item.addEventListener('drop', function(e) {
            e.preventDefault();
            e.stopPropagation();
            item.classList.remove('drag-over-fav');
            if (dragItem && dragItem !== item) {
                container.insertBefore(dragItem, item);
                saveFavoritesOrder();
            }
        });
    });
}

function saveFavoritesOrder() {
    var container = document.getElementById('favoritesContainer');
    if (!container) return;
    var items = container.querySelectorAll('.favorite-item');
    var orderItems = [];
    items.forEach(function(item, index) {
        orderItems.push({
            bookmarkId: parseInt(item.dataset.bookmarkId),
            displayOrder: index
        });
    });
    fetch('/api/bookmarks/favorites/reorder', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ items: orderItems })
    }).catch(function() { showToast('Error saving order', 'error'); });
}

// ===== Feature 5: Notifications =====
function toggleNotificationPanel() {
    var panel = document.getElementById('notificationPanel');
    if (!panel) return;
    if (panel.style.display === 'none' || panel.style.display === '') {
        var bellBtn = document.querySelector('.notification-bell-btn');
        if (bellBtn) {
            var rect = bellBtn.getBoundingClientRect();
            panel.style.top = (rect.bottom + 8) + 'px';
            panel.style.left = rect.left + 'px';
            var panelWidth = 280;
            if (rect.left + panelWidth > window.innerWidth) {
                panel.style.left = (window.innerWidth - panelWidth - 12) + 'px';
            }
        }
        panel.style.display = 'flex';
        loadNotifications();
    } else {
        panel.style.display = 'none';
    }
}

function loadNotifications() {
    fetch('/api/notifications?page=0&size=20')
        .then(function(r) { return r.json(); })
        .then(function(data) {
            var list = document.getElementById('notificationList');
            if (!data.content || data.content.length === 0) {
                list.innerHTML = '<div class="empty-state" style="padding:20px;"><p style="font-size:0.82rem;">No notifications</p></div>';
                return;
            }
            var html = '';
            data.content.forEach(function(n) {
                var cls = n.read ? 'notification-item' : 'notification-item unread';
                var href = n.relatedBookmarkId ? '/bookmark/' + n.relatedBookmarkId : '#';
                html += '<a href="' + href + '" class="' + cls + '" onclick="markNotifRead(' + n.id + ')">' +
                    '<div>' + escapeHtml(n.message) + '</div>' +
                    '<div class="notification-time">' + formatNotifTime(n.createdAt) + '</div></a>';
            });
            list.innerHTML = html;
        })
        .catch(function() {});
}

function markNotifRead(id) {
    fetch('/api/notifications/' + id + '/read', { method: 'PUT' }).catch(function() {});
}

function markAllNotificationsRead() {
    fetch('/api/notifications/read-all', { method: 'PUT' })
        .then(function() {
            loadNotifications();
            var badge = document.querySelector('.notification-badge');
            if (badge) badge.remove();
            showToast('All notifications marked as read');
        })
        .catch(function() { showToast('Error marking notifications', 'error'); });
}

function pollUnreadCount() {
    fetch('/api/notifications/unread-count')
        .then(function(r) { return r.json(); })
        .then(function(data) {
            var bellBtn = document.querySelector('.notification-bell-btn');
            if (!bellBtn) return;
            var existing = bellBtn.querySelector('.notification-badge');
            if (data.count > 0) {
                if (existing) {
                    existing.textContent = data.count;
                } else {
                    var badge = document.createElement('span');
                    badge.className = 'notification-badge';
                    badge.textContent = data.count;
                    bellBtn.appendChild(badge);
                }
            } else {
                if (existing) existing.remove();
            }
        })
        .catch(function() {});
}

function formatNotifTime(dateStr) {
    if (!dateStr) return '';
    var d = new Date(dateStr);
    var now = new Date();
    var diffMs = now - d;
    var diffMins = Math.floor(diffMs / 60000);
    if (diffMins < 1) return 'just now';
    if (diffMins < 60) return diffMins + 'm ago';
    var diffHours = Math.floor(diffMins / 60);
    if (diffHours < 24) return diffHours + 'h ago';
    var diffDays = Math.floor(diffHours / 24);
    return diffDays + 'd ago';
}

// ===== Event Delegation for data-* attributes =====
function initEventDelegation() {
    document.addEventListener('click', function(e) {
        // Access link (bookmark title click or frequent chip)
        var accessLink = e.target.closest('.js-access-link');
        if (accessLink) {
            e.preventDefault();
            var id = accessLink.dataset.id;
            var url = accessLink.dataset.url;
            if (id && url) recordAccess(id, url);
            return;
        }

        // Save bookmark button
        var saveBtn = e.target.closest('.js-save-btn');
        if (saveBtn) {
            e.preventDefault();
            e.stopPropagation();
            var id = saveBtn.dataset.id;
            if (id) toggleSaveBookmark(id);
            return;
        }

        // Favorite bookmark button
        var favBtn = e.target.closest('.js-fav-btn');
        if (favBtn) {
            e.preventDefault();
            e.stopPropagation();
            var id = favBtn.dataset.id;
            if (id) toggleFavoriteBookmark(id);
            return;
        }

        // Edit button
        var editBtn = e.target.closest('.js-edit-btn');
        if (editBtn) {
            var id = editBtn.dataset.id;
            if (id) openEditModal(id);
            return;
        }

        // Delete bookmark button
        var deleteBtn = e.target.closest('.js-delete-btn');
        if (deleteBtn) {
            var id = deleteBtn.dataset.id;
            if (id) deleteBookmark(id);
            return;
        }

        // Delete folder button
        var deleteFolderBtn = e.target.closest('.js-delete-folder-btn');
        if (deleteFolderBtn) {
            var id = deleteFolderBtn.dataset.id;
            if (id) deleteFolder(id);
            return;
        }

        // Close notification panel on outside click
        var panel = document.getElementById('notificationPanel');
        if (panel && panel.style.display === 'flex') {
            if (!e.target.closest('.notification-panel') && !e.target.closest('.notification-bell-btn')) {
                panel.style.display = 'none';
            }
        }
    });

    // Ctrl+K shortcut for search
    document.addEventListener('keydown', function(e) {
        if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
            e.preventDefault();
            openSearchModal();
        }
        if (e.key === 'Escape') {
            closeSearchModal();
        }
    });
}

// ===== Theme =====
function applyTheme(theme) {
    if (theme === 'LIGHT') {
        document.body.classList.add('light-theme');
    } else {
        document.body.classList.remove('light-theme');
    }
    localStorage.setItem('theme', theme);
}

function initTheme() {
    var saved = localStorage.getItem('theme');
    if (saved === 'LIGHT') {
        document.body.classList.add('light-theme');
    }
}

// ===== Init =====
document.addEventListener('DOMContentLoaded', function() {
    initTheme();
    initSidebarState();
    initDragDrop();
    initEventDelegation();
    initFavoritesDragDrop();
    initPhotoDropZone();
    var urlInput = document.getElementById('bmUrl');
    if (urlInput) urlInput.addEventListener('blur', checkDuplicateUrl);
    pollUnreadCount();
    setInterval(pollUnreadCount, 60000);
});
