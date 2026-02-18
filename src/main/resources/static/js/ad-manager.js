/**
 * LinkVault Ad Manager
 * Handles AdSense initialization, ad-blocker detection, and ad hide feedback.
 */
(function() {
    'use strict';

    // Session tracking in sessionStorage
    var SESSION_KEY = 'lv_ad_session';
    var PAGE_VIEW_KEY = 'lv_page_views';

    function getSessionId() {
        var id = sessionStorage.getItem(SESSION_KEY);
        if (!id) {
            id = 'gs_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
            sessionStorage.setItem(SESSION_KEY, id);
        }
        return id;
    }

    function getPageViewCount() {
        return parseInt(sessionStorage.getItem(PAGE_VIEW_KEY) || '0', 10);
    }

    function incrementPageViews() {
        var count = getPageViewCount() + 1;
        sessionStorage.setItem(PAGE_VIEW_KEY, count.toString());
        return count;
    }

    // Initialize on page load
    incrementPageViews();

    // Log guest page view event
    if (document.body.getAttribute('data-guest') === 'true') {
        fetch('/api/guest/event', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                sessionId: getSessionId(),
                eventType: 'PAGE_VIEW',
                pageUrl: window.location.pathname
            })
        }).catch(function() {});
    }

    // Initialize AdSense slots
    function initAdsense() {
        try {
            var adSlots = document.querySelectorAll('.adsbygoogle');
            adSlots.forEach(function(slot) {
                (adsbygoogle = window.adsbygoogle || []).push({});
            });
        } catch (e) {
            // AdSense not loaded (ad-blocker or not configured)
            showAdblockerNotices();
        }
    }

    // Detect ad-blocker
    function detectAdBlocker() {
        setTimeout(function() {
            var adSlots = document.querySelectorAll('.adsbygoogle');
            adSlots.forEach(function(slot) {
                if (slot.offsetHeight === 0 || slot.innerHTML.trim() === '') {
                    var notice = slot.parentElement.querySelector('.adblocker-notice');
                    if (notice) {
                        notice.style.display = 'block';
                        slot.style.display = 'none';
                    }
                }
            });
        }, 2000);
    }

    function showAdblockerNotices() {
        document.querySelectorAll('.adblocker-notice').forEach(function(el) {
            el.style.display = 'block';
        });
        document.querySelectorAll('.adsbygoogle').forEach(function(el) {
            el.style.display = 'none';
        });
    }

    // Check cookie consent before loading ads
    function shouldLoadAds() {
        var consent = localStorage.getItem('cookieConsent');
        if (document.body.getAttribute('data-guest') === 'true' && consent !== 'accepted') {
            return false;
        }
        return true;
    }

    // Wait for AdSense script to load
    if (shouldLoadAds() && document.querySelector('.adsbygoogle')) {
        if (window.adsbygoogle) {
            initAdsense();
            detectAdBlocker();
        } else {
            window.addEventListener('load', function() {
                setTimeout(function() {
                    initAdsense();
                    detectAdBlocker();
                }, 500);
            });
        }
    }

    // Ad hide modal
    window.showAdHideModal = function(btn) {
        var adCard = btn.closest('.ad-card');
        var modal = document.createElement('div');
        modal.className = 'ad-hide-modal';
        modal.innerHTML =
            '<div class="ad-hide-modal-content">' +
            '<h4>Why do you want to hide this ad?</h4>' +
            '<div class="ad-hide-reason-option" data-reason="NOT_INTERESTED">Not interested in this</div>' +
            '<div class="ad-hide-reason-option" data-reason="REPETITIVE">I keep seeing this ad</div>' +
            '<div class="ad-hide-reason-option" data-reason="OFFENSIVE">Inappropriate content</div>' +
            '<div class="ad-hide-reason-option" data-reason="OTHER">Other reason</div>' +
            '<div style="margin-top:12px;text-align:right;">' +
            '<button class="btn btn-sm btn-outline" onclick="this.closest(\'.ad-hide-modal\').remove()">Cancel</button>' +
            '</div>' +
            '</div>';

        modal.querySelectorAll('.ad-hide-reason-option').forEach(function(opt) {
            opt.addEventListener('click', function() {
                var reason = this.getAttribute('data-reason');
                submitAdHide(adCard, reason);
                modal.remove();
            });
        });

        modal.addEventListener('click', function(e) {
            if (e.target === modal) modal.remove();
        });

        document.body.appendChild(modal);
    };

    function submitAdHide(adCard, reason) {
        var adUnitId = adCard.getAttribute('data-ad-unit') || 'feed_ad';

        fetch('/api/ad/hide', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                adUnitId: adUnitId,
                reason: reason,
                sessionId: getSessionId()
            })
        }).catch(function() {});

        // Remove the ad card visually
        adCard.style.opacity = '0';
        adCard.style.transition = 'opacity 0.3s';
        setTimeout(function() {
            adCard.style.display = 'none';
        }, 300);

        if (typeof showToast === 'function') {
            showToast('Ad hidden. Thanks for your feedback.');
        }
    }

    // Export session helpers
    window.lvAdManager = {
        getSessionId: getSessionId,
        getPageViewCount: getPageViewCount
    };
})();
