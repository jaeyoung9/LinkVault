// ===== Toast (reuse pattern from app.js) =====
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

// ===== User Modal =====
function openUserModal() {
    document.getElementById('userModal').classList.add('active');
    document.getElementById('userModalTitle').textContent = 'New User';
    document.getElementById('userForm').reset();
    document.getElementById('userId').value = '';
    document.getElementById('userUsername').removeAttribute('readonly');
    document.getElementById('userEnabled').checked = true;
}

function closeUserModal() {
    document.getElementById('userModal').classList.remove('active');
}

function openEditUserModal(id, username, email, role, enabled) {
    document.getElementById('userModal').classList.add('active');
    document.getElementById('userModalTitle').textContent = 'Edit User';
    document.getElementById('userId').value = id;
    document.getElementById('userUsername').value = username;
    document.getElementById('userUsername').setAttribute('readonly', 'readonly');
    document.getElementById('userEmail').value = email;
    document.getElementById('userPassword').value = '';
    document.getElementById('userRole').value = role;
    document.getElementById('userEnabled').checked = (enabled === 'true' || enabled === true);
}

function saveUser(event) {
    event.preventDefault();
    var id = document.getElementById('userId').value;
    var data = {
        username: document.getElementById('userUsername').value,
        email: document.getElementById('userEmail').value,
        password: document.getElementById('userPassword').value || null,
        role: document.getElementById('userRole').value,
        enabled: document.getElementById('userEnabled').checked
    };

    var method = id ? 'PUT' : 'POST';
    var url = id ? '/api/admin/users/' + id : '/api/admin/users';

    fetch(url, {
        method: method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
    })
    .then(function(r) {
        if (!r.ok) return r.json().then(function(e) { throw e; });
        return r.json();
    })
    .then(function() {
        closeUserModal();
        showToast(id ? 'User updated' : 'User created');
        setTimeout(function() { location.reload(); }, 500);
    })
    .catch(function(err) {
        showToast(err.message || 'Error saving user', 'error');
    });
}

function deleteUser(id) {
    if (!confirm('Delete this user?')) return;
    fetch('/api/admin/users/' + id, { method: 'DELETE' })
        .then(function() {
            showToast('User deleted');
            setTimeout(function() { location.reload(); }, 500);
        })
        .catch(function() { showToast('Error deleting user', 'error'); });
}

// ===== Tag Management =====
function toggleAllTags(checkbox) {
    var checkboxes = document.querySelectorAll('.tag-checkbox');
    checkboxes.forEach(function(cb) { cb.checked = checkbox.checked; });
}

function mergeTags() {
    var targetName = document.getElementById('mergeTargetName').value.trim();
    if (!targetName) {
        showToast('Please enter a target tag name', 'error');
        return;
    }

    var sourceIds = [];
    document.querySelectorAll('.tag-checkbox:checked').forEach(function(cb) {
        sourceIds.push(parseInt(cb.value));
    });

    if (sourceIds.length === 0) {
        showToast('Please select tags to merge', 'error');
        return;
    }

    fetch('/api/admin/tags/merge', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ sourceTagIds: sourceIds, targetTagName: targetName })
    })
    .then(function(r) {
        if (!r.ok) return r.json().then(function(e) { throw e; });
        return r.json();
    })
    .then(function() {
        showToast('Tags merged successfully');
        setTimeout(function() { location.reload(); }, 500);
    })
    .catch(function(err) {
        showToast(err.message || 'Error merging tags', 'error');
    });
}

function cleanupUnusedTags() {
    if (!confirm('Delete all unused tags?')) return;
    fetch('/api/admin/tags/unused', { method: 'DELETE' })
        .then(function(r) { return r.json(); })
        .then(function(data) {
            showToast(data.message || 'Unused tags cleaned up');
            setTimeout(function() { location.reload(); }, 500);
        })
        .catch(function() { showToast('Error cleaning up tags', 'error'); });
}

function deleteTag(id) {
    if (!confirm('Delete this tag?')) return;
    fetch('/api/admin/tags/' + id, { method: 'DELETE' })
        .then(function() {
            showToast('Tag deleted');
            setTimeout(function() { location.reload(); }, 500);
        })
        .catch(function() { showToast('Error deleting tag', 'error'); });
}

// ===== Backup/Restore =====
function createBackup() {
    fetch('/api/admin/backup', { method: 'POST' })
        .then(function(r) { return r.json(); })
        .then(function(data) {
            showToast(data.message || 'Backup created');
            setTimeout(function() { location.reload(); }, 500);
        })
        .catch(function() { showToast('Error creating backup', 'error'); });
}

function restoreBackup(filename) {
    if (!confirm('Restore database from ' + filename + '? This will overwrite current data!')) return;
    fetch('/api/admin/restore', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ filename: filename })
    })
    .then(function(r) { return r.json(); })
    .then(function(data) {
        showToast(data.message || 'Database restored');
        setTimeout(function() { location.reload(); }, 1000);
    })
    .catch(function() { showToast('Error restoring backup', 'error'); });
}

// ===== Admin Bookmark Delete =====
function adminDeleteBookmark(id) {
    if (!confirm('Delete this bookmark?')) return;
    fetch('/api/admin/bookmarks/' + id, { method: 'DELETE' })
        .then(function() {
            showToast('Bookmark deleted');
            setTimeout(function() { location.reload(); }, 500);
        })
        .catch(function() { showToast('Error deleting bookmark', 'error'); });
}

// ===== Event Delegation =====
document.addEventListener('click', function(e) {
    // Edit user
    var editUser = e.target.closest('.js-edit-user');
    if (editUser) {
        openEditUserModal(
            editUser.dataset.id,
            editUser.dataset.username,
            editUser.dataset.email,
            editUser.dataset.role,
            editUser.dataset.enabled
        );
        return;
    }

    // Delete user
    var delUser = e.target.closest('.js-delete-user');
    if (delUser) {
        deleteUser(delUser.dataset.id);
        return;
    }

    // Delete tag
    var delTag = e.target.closest('.js-delete-tag');
    if (delTag) {
        deleteTag(delTag.dataset.id);
        return;
    }

    // Admin delete bookmark
    var adminDelBm = e.target.closest('.js-admin-delete-bookmark');
    if (adminDelBm) {
        adminDeleteBookmark(adminDelBm.dataset.id);
        return;
    }

    // Restore backup
    var restoreBtn = e.target.closest('.js-restore-backup');
    if (restoreBtn) {
        restoreBackup(restoreBtn.dataset.filename);
        return;
    }
});
