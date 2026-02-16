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

// ===== Event Delegation for data-* attributes =====
function initEventDelegation() {
    document.addEventListener('click', function(e) {
        // Access link (bookmark title click or frequent chip)
        const accessLink = e.target.closest('.js-access-link');
        if (accessLink) {
            e.preventDefault();
            const id = accessLink.dataset.id;
            const url = accessLink.dataset.url;
            if (id && url) recordAccess(id, url);
            return;
        }

        // Edit button
        const editBtn = e.target.closest('.js-edit-btn');
        if (editBtn) {
            const id = editBtn.dataset.id;
            if (id) openEditModal(id);
            return;
        }

        // Delete bookmark button
        const deleteBtn = e.target.closest('.js-delete-btn');
        if (deleteBtn) {
            const id = deleteBtn.dataset.id;
            if (id) deleteBookmark(id);
            return;
        }

        // Delete folder button
        const deleteFolderBtn = e.target.closest('.js-delete-folder-btn');
        if (deleteFolderBtn) {
            const id = deleteFolderBtn.dataset.id;
            if (id) deleteFolder(id);
            return;
        }
    });
}

// ===== Init =====
document.addEventListener('DOMContentLoaded', () => {
    initDragDrop();
    initEventDelegation();
    const urlInput = document.getElementById('bmUrl');
    if (urlInput) urlInput.addEventListener('blur', checkDuplicateUrl);
});
