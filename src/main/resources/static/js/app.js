// ===== Toast Notifications =====
function showToast(message, type = 'success') {
    let container = document.querySelector('.toast-container');
    if (!container) {
        container = document.createElement('div');
        container.className = 'toast-container';
        document.body.appendChild(container);
    }
    const toast = document.createElement('div');
    toast.className = 'toast ' + type;
    toast.textContent = message;
    container.appendChild(toast);
    setTimeout(() => toast.remove(), 3000);
}

// ===== Utility =====
function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
              .replace(/"/g, '&quot;').replace(/'/g, '&#039;');
}

// ===== Bookmark CRUD =====
function openAddModal() {
    document.getElementById('bookmarkModal').classList.add('active');
    document.getElementById('modalTitle').textContent = 'Add Bookmark';
    document.getElementById('bookmarkForm').reset();
    document.getElementById('bookmarkId').value = '';
}

function openEditModal(id) {
    fetch('/api/bookmarks/' + id)
        .then(r => r.json())
        .then(b => {
            document.getElementById('bookmarkModal').classList.add('active');
            document.getElementById('modalTitle').textContent = 'Edit Bookmark';
            document.getElementById('bookmarkId').value = b.id;
            document.getElementById('bmTitle').value = b.title;
            document.getElementById('bmUrl').value = b.url;
            document.getElementById('bmDescription').value = b.description || '';
            document.getElementById('bmTags').value = b.tagNames ? b.tagNames.join(', ') : '';
            const folderSelect = document.getElementById('bmFolder');
            if (folderSelect) folderSelect.value = b.folderId || '';
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

function saveBookmark(event) {
    event.preventDefault();
    const id = document.getElementById('bookmarkId').value;
    const tagsRaw = document.getElementById('bmTags').value;
    const tagNames = tagsRaw ? tagsRaw.split(',').map(t => t.trim()).filter(t => t) : [];
    const folderVal = document.getElementById('bmFolder').value;

    const data = {
        title: document.getElementById('bmTitle').value,
        url: document.getElementById('bmUrl').value,
        description: document.getElementById('bmDescription').value,
        tagNames: tagNames,
        folderId: folderVal ? parseInt(folderVal) : null
    };

    const method = id ? 'PUT' : 'POST';
    const url = id ? '/api/bookmarks/' + id : '/api/bookmarks';

    fetch(url, {
        method: method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
    })
    .then(r => {
        if (!r.ok) return r.json().then(e => { throw e; });
        return r.json();
    })
    .then(() => {
        closeModal();
        showToast(id ? 'Bookmark updated' : 'Bookmark created');
        setTimeout(() => location.reload(), 500);
    })
    .catch(err => {
        const msg = err.message || err.fieldErrors ? Object.values(err.fieldErrors).join(', ') : 'Error saving bookmark';
        showToast(msg, 'error');
    });
}

function deleteBookmark(id) {
    if (!confirm('Delete this bookmark?')) return;
    fetch('/api/bookmarks/' + id, { method: 'DELETE' })
        .then(() => {
            showToast('Bookmark deleted');
            setTimeout(() => location.reload(), 500);
        })
        .catch(() => showToast('Error deleting bookmark', 'error'));
}

function recordAccess(id, url) {
    fetch('/api/bookmarks/' + id + '/access', { method: 'POST' });
    window.open(url, '_blank');
}

// ===== Folder CRUD =====
function saveFolder(event) {
    event.preventDefault();
    const data = {
        name: document.getElementById('folderName').value,
        parentId: document.getElementById('folderParent').value || null
    };

    fetch('/api/folders', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
    })
    .then(r => {
        if (!r.ok) return r.json().then(e => { throw e; });
        return r.json();
    })
    .then(() => {
        closeFolderModal();
        showToast('Folder created');
        setTimeout(() => location.reload(), 500);
    })
    .catch(err => showToast(err.message || 'Error creating folder', 'error'));
}

function deleteFolder(id) {
    if (!confirm('Delete this folder and all its contents?')) return;
    fetch('/api/folders/' + id, { method: 'DELETE' })
        .then(() => {
            showToast('Folder deleted');
            setTimeout(() => location.reload(), 500);
        })
        .catch(() => showToast('Error deleting folder', 'error'));
}

// ===== Drag & Drop =====
let draggedBookmarkId = null;

function initDragDrop() {
    // Bookmark cards are draggable
    document.querySelectorAll('.bookmark-card[draggable]').forEach(card => {
        card.addEventListener('dragstart', e => {
            draggedBookmarkId = card.dataset.id;
            card.classList.add('dragging');
            e.dataTransfer.effectAllowed = 'move';
        });
        card.addEventListener('dragend', () => {
            card.classList.remove('dragging');
            draggedBookmarkId = null;
            document.querySelectorAll('.drag-over').forEach(el => el.classList.remove('drag-over'));
        });
    });

    // Folder items in sidebar are drop targets
    document.querySelectorAll('.folder-item').forEach(item => {
        item.addEventListener('dragover', e => {
            e.preventDefault();
            e.dataTransfer.dropEffect = 'move';
            item.classList.add('drag-over');
        });
        item.addEventListener('dragleave', () => {
            item.classList.remove('drag-over');
        });
        item.addEventListener('drop', e => {
            e.preventDefault();
            item.classList.remove('drag-over');
            if (draggedBookmarkId) {
                const folderId = item.dataset.folderId;
                moveBookmarkToFolder(draggedBookmarkId, folderId);
            }
        });
    });

    // Uncategorized drop zone
    const uncatZone = document.getElementById('uncategorizedDrop');
    if (uncatZone) {
        uncatZone.addEventListener('dragover', e => {
            e.preventDefault();
            uncatZone.classList.add('drag-over');
        });
        uncatZone.addEventListener('dragleave', () => uncatZone.classList.remove('drag-over'));
        uncatZone.addEventListener('drop', e => {
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
    .then(r => {
        if (!r.ok) throw new Error();
        showToast('Bookmark moved');
        setTimeout(() => location.reload(), 500);
    })
    .catch(() => showToast('Error moving bookmark', 'error'));
}

// ===== URL Duplicate Check =====
function checkDuplicateUrl() {
    const urlInput = document.getElementById('bmUrl');
    if (!urlInput || !urlInput.value) return;
    fetch('/api/bookmarks/check-url?url=' + encodeURIComponent(urlInput.value))
        .then(r => r.json())
        .then(data => {
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
    const input = document.getElementById(format + 'File');
    if (!input.files.length) {
        showToast('Please select a file', 'error');
        return;
    }
    const formData = new FormData();
    formData.append('file', input.files[0]);

    fetch('/api/data/import/' + format, { method: 'POST', body: formData })
        .then(r => r.json())
        .then(data => {
            showToast(data.message);
            setTimeout(() => window.location.href = '/', 1500);
        })
        .catch(() => showToast('Import failed', 'error'));
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
        html += '<div class="search-result-group"><div class="search-result-group-title">Bookmarks</div>';
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
                    btn.innerHTML = '&#128278;';
                } else {
                    btn.classList.remove('save-active');
                    btn.innerHTML = '&#128278;';
                }
            });
            showToast(data.saved ? 'Bookmark saved' : 'Bookmark unsaved');
        })
        .catch(function() { showToast('Error saving bookmark', 'error'); });
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
            // Reload after a short delay to update sidebar favorites
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
        // Position the panel near the bell button
        var bellBtn = document.querySelector('.notification-bell-btn');
        if (bellBtn) {
            var rect = bellBtn.getBoundingClientRect();
            panel.style.top = (rect.bottom + 8) + 'px';
            panel.style.left = rect.left + 'px';
            // Ensure panel doesn't go off-screen to the right
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
            // Update badge
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
    // Server-injected script in sidebar fragment already applies the theme.
    // This is a fallback in case localStorage has a value not yet synced.
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
    var urlInput = document.getElementById('bmUrl');
    if (urlInput) urlInput.addEventListener('blur', checkDuplicateUrl);
    // Poll for unread notifications every 60 seconds
    pollUnreadCount();
    setInterval(pollUnreadCount, 60000);
});
