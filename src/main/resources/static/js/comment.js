// ===== Comment System =====

function loadComments() {
    fetch('/api/comments/bookmark/' + bookmarkId)
        .then(function(r) { return r.json(); })
        .then(function(comments) {
            var container = document.getElementById('commentsContainer');
            var noComments = document.getElementById('noComments');

            if (comments.length === 0) {
                container.innerHTML = '';
                container.appendChild(noComments);
                noComments.style.display = '';
                return;
            }

            container.innerHTML = '';
            comments.forEach(function(comment) {
                container.appendChild(renderComment(comment));
            });
        })
        .catch(function() {
            showToast('Error loading comments', 'error');
        });
}

function renderComment(comment) {
    var div = document.createElement('div');
    div.className = 'comment-item';
    div.style.marginLeft = (comment.depth * 24) + 'px';
    div.dataset.id = comment.id;

    var likeActive = comment.userVote === 'LIKE' ? ' vote-active' : '';
    var dislikeActive = comment.userVote === 'DISLIKE' ? ' vote-active' : '';

    div.innerHTML =
        '<div class="comment-header">' +
            '<strong>' + comment.username + '</strong>' +
            '<span class="comment-time">' + formatTime(comment.createdAt) + '</span>' +
            (comment.edited ? '<span class="comment-edited">(edited)</span>' : '') +
        '</div>' +
        '<div class="comment-content" id="content-' + comment.id + '">' + escapeHtml(comment.content) + '</div>' +
        '<div class="comment-actions">' +
            '<button class="vote-btn' + likeActive + '" onclick="voteComment(' + comment.id + ', \'LIKE\')">' +
                '&#9650; ' + comment.likeCount +
            '</button>' +
            '<button class="vote-btn' + dislikeActive + '" onclick="voteComment(' + comment.id + ', \'DISLIKE\')">' +
                '&#9660; ' + comment.dislikeCount +
            '</button>' +
            (comment.depth < 5 ? '<button class="comment-action-btn" onclick="toggleReplyForm(' + comment.id + ')">Reply</button>' : '') +
            (comment.canEdit ? '<button class="comment-action-btn" onclick="editComment(' + comment.id + ')">Edit</button>' : '') +
            (comment.canDelete ? '<button class="comment-action-btn" onclick="deleteComment(' + comment.id + ')">Delete</button>' : '') +
        '</div>' +
        '<div class="reply-form" id="reply-form-' + comment.id + '" style="display:none;">' +
            '<textarea class="form-control" id="reply-content-' + comment.id + '" placeholder="Write a reply..." rows="2"></textarea>' +
            '<div style="margin-top:6px;text-align:right;">' +
                '<button class="btn btn-sm btn-outline" onclick="toggleReplyForm(' + comment.id + ')">Cancel</button> ' +
                '<button class="btn btn-sm btn-primary" onclick="submitReply(' + comment.id + ', ' + comment.bookmarkId + ')">Reply</button>' +
            '</div>' +
        '</div>';

    // Render replies
    if (comment.replies && comment.replies.length > 0) {
        comment.replies.forEach(function(reply) {
            div.appendChild(renderComment(reply));
        });
    }

    return div;
}

function submitComment() {
    var content = document.getElementById('newCommentContent').value.trim();
    if (!content) {
        showToast('Please enter a comment', 'error');
        return;
    }

    fetch('/api/comments', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ content: content, bookmarkId: bookmarkId })
    })
    .then(function(r) {
        if (!r.ok) return r.json().then(function(e) { throw e; });
        return r.json();
    })
    .then(function() {
        document.getElementById('newCommentContent').value = '';
        showToast('Comment posted');
        loadComments();
    })
    .catch(function(err) {
        showToast(err.message || 'Error posting comment', 'error');
    });
}

function submitReply(parentId, bmId) {
    var content = document.getElementById('reply-content-' + parentId).value.trim();
    if (!content) {
        showToast('Please enter a reply', 'error');
        return;
    }

    fetch('/api/comments', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ content: content, bookmarkId: bmId, parentId: parentId })
    })
    .then(function(r) {
        if (!r.ok) return r.json().then(function(e) { throw e; });
        return r.json();
    })
    .then(function() {
        showToast('Reply posted');
        loadComments();
    })
    .catch(function(err) {
        showToast(err.message || 'Error posting reply', 'error');
    });
}

function editComment(commentId) {
    var contentEl = document.getElementById('content-' + commentId);
    var currentText = contentEl.textContent;

    contentEl.innerHTML =
        '<textarea class="form-control" id="edit-content-' + commentId + '" rows="2">' + currentText + '</textarea>' +
        '<div style="margin-top:6px;text-align:right;">' +
            '<button class="btn btn-sm btn-outline" onclick="loadComments()">Cancel</button> ' +
            '<button class="btn btn-sm btn-primary" onclick="saveEdit(' + commentId + ')">Save</button>' +
        '</div>';
}

function saveEdit(commentId) {
    var content = document.getElementById('edit-content-' + commentId).value.trim();
    if (!content) return;

    fetch('/api/comments/' + commentId, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ content: content })
    })
    .then(function(r) {
        if (!r.ok) return r.json().then(function(e) { throw e; });
        return r.json();
    })
    .then(function() {
        showToast('Comment updated');
        loadComments();
    })
    .catch(function(err) {
        showToast(err.message || 'Error updating comment', 'error');
    });
}

function deleteComment(commentId) {
    if (!confirm('Delete this comment?')) return;

    fetch('/api/comments/' + commentId, { method: 'DELETE' })
    .then(function(r) {
        if (!r.ok) return r.json().then(function(e) { throw e; });
        showToast('Comment deleted');
        loadComments();
    })
    .catch(function(err) {
        showToast(err.message || 'Error deleting comment', 'error');
    });
}

function voteComment(commentId, voteType) {
    fetch('/api/comments/' + commentId + '/vote', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ voteType: voteType })
    })
    .then(function(r) {
        if (!r.ok) return r.json().then(function(e) { throw e; });
        return r.json();
    })
    .then(function() {
        loadComments();
    })
    .catch(function(err) {
        showToast(err.message || 'Error voting', 'error');
    });
}

function toggleReplyForm(commentId) {
    var form = document.getElementById('reply-form-' + commentId);
    form.style.display = form.style.display === 'none' ? '' : 'none';
}

function formatTime(dateStr) {
    if (!dateStr) return '';
    var d = new Date(dateStr);
    var now = new Date();
    var diff = Math.floor((now - d) / 1000);

    if (diff < 60) return 'just now';
    if (diff < 3600) return Math.floor(diff / 60) + 'm ago';
    if (diff < 86400) return Math.floor(diff / 3600) + 'h ago';
    if (diff < 604800) return Math.floor(diff / 86400) + 'd ago';
    return d.toLocaleDateString();
}

function escapeHtml(text) {
    var div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// Load comments on page ready
document.addEventListener('DOMContentLoaded', loadComments);
